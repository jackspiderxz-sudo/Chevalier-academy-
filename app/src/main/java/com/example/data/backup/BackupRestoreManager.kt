package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.BuildConfig
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.model.SchoolTenantContext
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ContentApprovalEntity
import com.example.data.local.entity.LocalNotificationEntity
import com.example.data.local.entity.ManagementAccountEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.local.entity.VersionHistoryEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserPreferencesState
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ContentSnapshotSerializer.optNullableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupPreviewInfo(
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersionName: String,
    val exportedAtMillis: Long,
    val accountCount: Int,
    val contentRecordCount: Int,
    val galleryItemCount: Int,
    val mediaStudioCount: Int,
    val mediaFileCount: Int,
    val historyAndLogCount: Int,
    val schoolId: String = SchoolTenantContext.DEFAULT_SCHOOL_ID,
    val snapshotSha256: String? = null
)

class BackupRestoreManager(
    private val context: Context,
    private val database: ChevalierDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    companion object {
        private val MAGIC_HEADER = "CHV_BAK1".toByteArray(Charsets.US_ASCII)
        private const val FORMAT_VERSION = 1
        private const val CURRENT_SCHEMA_VERSION = 4
        private const val PBKDF2_ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val MAX_ZIP_ENTRIES = 2000
        private const val MAX_ZIP_UNCOMPRESSED_BYTES = 128L * 1024L * 1024L
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val SNAPSHOT_ENTRY = "database_snapshot.json"
        private const val MEDIA_PREFIX = "media_files/"
        private val SNAPSHOT_MEDIA_JSON_KEYS = setOf(
            "profileImageUri",
            "aboutImageUri",
            "imageUri",
            "coverMediaUri",
            "mediaUri",
            "originalUri",
            "optimizedUri"
        )
    }

    private val secureRandom = SecureRandom()

    suspend fun createEncryptedBackupToUri(uri: Uri, password: String): Result<BackupPreviewInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encrypted = createEncryptedBackupBytes(password).getOrThrow()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(encrypted)
                    out.flush()
                } ?: throw IllegalStateException("Unable to open destination file for backup.")
                inspectEncryptedBackup(encrypted, password).getOrThrow()
            }
        }

    suspend fun createEncryptedBackupBytes(password: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(password.length >= 6) {
                    "Backup password must be at least 6 characters."
                }
                val (snapshotJson, preview) = buildSnapshotJsonAndPreview()
                val manifestJson = buildManifestJson(preview)
                val zipBytes = packageZip(manifestJson, snapshotJson)
                encryptBytes(zipBytes, password)
            }
        }

    suspend fun inspectEncryptedBackupFromUri(uri: Uri, password: String): Result<BackupPreviewInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Unable to read selected backup file.")
                inspectEncryptedBackup(bytes, password).getOrThrow()
            }
        }

    suspend fun inspectEncryptedBackup(bytes: ByteArray, password: String): Result<BackupPreviewInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(password.isNotEmpty()) { "Backup password is required." }
                val zipBytes = decryptBytes(bytes, password)
                val entries = unpackZipInMemory(zipBytes)
                val manifestStr = entries[MANIFEST_ENTRY]?.toString(Charsets.UTF_8)
                    ?: throw IllegalArgumentException("Backup manifest is missing or corrupted.")
                val snapshotStr = entries[SNAPSHOT_ENTRY]?.toString(Charsets.UTF_8)
                    ?: throw IllegalArgumentException("Backup database snapshot is missing or corrupted.")

                val manifest = JSONObject(manifestStr)
                val schemaVersion = manifest.getInt("schemaVersion")
                if (schemaVersion > CURRENT_SCHEMA_VERSION || schemaVersion < 1) {
                    throw IllegalArgumentException("Incompatible backup schema version: $schemaVersion.")
                }
                val verifiedDigest = verifySnapshotDigestIfPresent(manifest, snapshotStr, schemaVersion)
                // Validate database snapshot JSON parses properly
                val snapshot = JSONObject(snapshotStr)
                require(snapshot.has("schoolProfile") && snapshot.has("managementAccounts")) {
                    "Backup snapshot is incomplete or corrupted."
                }

                BackupPreviewInfo(
                    formatVersion = manifest.optInt("formatVersion", FORMAT_VERSION),
                    schemaVersion = schemaVersion,
                    appVersionName = manifest.optString("appVersionName", "unknown"),
                    exportedAtMillis = manifest.optLong("exportedAtMillis", 0L),
                    accountCount = manifest.optInt("accountCount", 0),
                    contentRecordCount = manifest.optInt("contentRecordCount", 0),
                    galleryItemCount = manifest.optInt("galleryItemCount", 0),
                    mediaStudioCount = manifest.optInt("mediaStudioCount", 0),
                    mediaFileCount = entries.keys.count { it.startsWith(MEDIA_PREFIX) && !it.endsWith("/") },
                    historyAndLogCount = manifest.optInt("historyAndLogCount", 0),
                    schoolId = manifest.optString("schoolId", SchoolTenantContext.DEFAULT_SCHOOL_ID),
                    snapshotSha256 = verifiedDigest
                )
            }
        }

    suspend fun restoreEncryptedBackupFromUri(
        uri: Uri,
        password: String,
        actorName: String,
        actorAccountId: Long? = null,
        actorRole: String = ManagementRole.MASTER_ADMIN
    ): Result<BackupPreviewInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Unable to read selected backup file.")
            restoreEncryptedBackupBytes(bytes, password, actorName, actorAccountId, actorRole = actorRole).getOrThrow()
        }
    }

    suspend fun restoreEncryptedBackupBytes(
        bytes: ByteArray,
        password: String,
        actorName: String,
        actorAccountId: Long? = null,
        simulateFailureAfterClearForTest: Boolean = false,
        actorRole: String = ManagementRole.MASTER_ADMIN
    ): Result<BackupPreviewInfo> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.isNotEmpty()) { "Backup password is required." }
            val zipBytes = decryptBytes(bytes, password)
            val entries = unpackZipInMemory(zipBytes)
            val manifestStr = entries[MANIFEST_ENTRY]?.toString(Charsets.UTF_8)
                ?: throw IllegalArgumentException("Backup manifest missing.")
            val snapshotStr = entries[SNAPSHOT_ENTRY]?.toString(Charsets.UTF_8)
                ?: throw IllegalArgumentException("Database snapshot missing.")

            val manifest = JSONObject(manifestStr)
            val schemaVersion = manifest.getInt("schemaVersion")
            if (schemaVersion > CURRENT_SCHEMA_VERSION || schemaVersion < 1) {
                throw IllegalArgumentException("Incompatible backup schema version: $schemaVersion.")
            }
            val verifiedDigest = verifySnapshotDigestIfPresent(manifest, snapshotStr, schemaVersion)

            val parsedSnapshot = parseSnapshot(JSONObject(snapshotStr))
            val stagingMediaDir = File(context.filesDir, "chevalier_media_staging")
            val liveMediaDir = File(context.filesDir, "chevalier_media")
            val rollbackMediaDir = File(context.filesDir, "chevalier_media_rollback")

            stagingMediaDir.deleteRecursively()
            stagingMediaDir.mkdirs()

            // Stage media files safely before touching live data
            entries.forEach { (name, data) ->
                if (name.startsWith(MEDIA_PREFIX) && !name.endsWith("/")) {
                    val rel = name.removePrefix(MEDIA_PREFIX)
                    val target = File(stagingMediaDir, rel).canonicalFile
                    if (!target.path.startsWith(stagingMediaDir.canonicalPath)) {
                        throw SecurityException("Invalid path inside backup archive.")
                    }
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { it.write(data) }
                }
            }

            val previousPrefs = preferencesRepository.preferencesFlow.first()
            if (rollbackMediaDir.exists()) rollbackMediaDir.deleteRecursively()
            var movedLiveToRollback = false

            try {
                if (liveMediaDir.exists()) {
                    movedLiveToRollback = liveMediaDir.renameTo(rollbackMediaDir)
                    if (!movedLiveToRollback) {
                        liveMediaDir.copyRecursively(rollbackMediaDir, overwrite = true)
                        liveMediaDir.deleteRecursively()
                        movedLiveToRollback = true
                    }
                }
                if (!stagingMediaDir.renameTo(liveMediaDir)) {
                    stagingMediaDir.copyRecursively(liveMediaDir, overwrite = true)
                    stagingMediaDir.deleteRecursively()
                }

                remapAdjustedSidecars(liveMediaDir)

                database.withTransaction {
                    applySnapshotInTransaction(parsedSnapshot, liveMediaDir, actorName, actorAccountId, actorRole)
                    if (simulateFailureAfterClearForTest) {
                        throw IllegalStateException("Simulated restore failure for rollback verification.")
                    }
                }
                database.invalidationTracker.refreshVersionsAsync()

                preferencesRepository.restorePreferences(
                    language = AppLanguage.fromCode(parsedSnapshot.languageCode),
                    isOnboarded = parsedSnapshot.isLanguageSelectedOnLaunch,
                    themeMode = AppThemeMode.fromKey(parsedSnapshot.themeModeKey),
                    deviceAlertsEnabled = parsedSnapshot.deviceAlertsEnabled ?: previousPrefs.deviceAlertsEnabled
                )

                rollbackMediaDir.deleteRecursively()

                BackupPreviewInfo(
                    formatVersion = manifest.optInt("formatVersion", FORMAT_VERSION),
                    schemaVersion = schemaVersion,
                    appVersionName = manifest.optString("appVersionName", "unknown"),
                    exportedAtMillis = manifest.optLong("exportedAtMillis", 0L),
                    accountCount = parsedSnapshot.accounts.size,
                    contentRecordCount = parsedSnapshot.academics.size +
                        parsedSnapshot.courses.size +
                        parsedSnapshot.facilities.size +
                        parsedSnapshot.transportRoutes.size +
                        parsedSnapshot.news.size +
                        parsedSnapshot.announcements.size +
                        parsedSnapshot.socialLinks.size,
                    galleryItemCount = parsedSnapshot.galleryAlbums.size + parsedSnapshot.galleryMedia.size + parsedSnapshot.heroPhotos.size,
                    mediaStudioCount = parsedSnapshot.mediaStudioItems.size,
                    mediaFileCount = entries.keys.count { it.startsWith(MEDIA_PREFIX) && !it.endsWith("/") },
                    historyAndLogCount = parsedSnapshot.versionHistory.size + parsedSnapshot.activityLogs.size,
                    schoolId = manifest.optString("schoolId", SchoolTenantContext.DEFAULT_SCHOOL_ID),
                    snapshotSha256 = verifiedDigest
                )
            } catch (t: Throwable) {
                // Rollback media directory & preferences completely
                stagingMediaDir.deleteRecursively()
                if (movedLiveToRollback && rollbackMediaDir.exists()) {
                    liveMediaDir.deleteRecursively()
                    if (!rollbackMediaDir.renameTo(liveMediaDir)) {
                        rollbackMediaDir.copyRecursively(liveMediaDir, overwrite = true)
                        rollbackMediaDir.deleteRecursively()
                    }
                }
                preferencesRepository.restorePreferences(
                    language = previousPrefs.selectedLanguage,
                    isOnboarded = previousPrefs.isLanguageSelectedOnLaunch,
                    themeMode = previousPrefs.themeMode,
                    deviceAlertsEnabled = previousPrefs.deviceAlertsEnabled
                )
                throw t
            }
        }
    }

    private suspend fun buildSnapshotJsonAndPreview(): Pair<String, BackupPreviewInfo> {
        val prefs: UserPreferencesState = preferencesRepository.preferencesFlow.first()
        val profile = database.schoolProfileDao().getProfileOnce() ?: ChevalierDatabase.verifiedDefaultProfile()
        val accountsWithPerms = database.managementAccountDao().getAllAccountsWithPermissionsOnce()
        val accounts = accountsWithPerms.map { it.account }
        val permissions = accountsWithPerms.flatMap { it.permissions }
        val heroPhotos = database.campusHeroPhotoDao().observeAllForManagement().first()
        val academics = database.academicInfoDao().observeAll().first()
        val courses = database.higherSecondaryCourseDao().observeAll().first()
        val facilities = database.facilityDao().observeAll().first()
        val routes = database.transportDao().observeAll().first()
        val news = database.newsDao().observeAll().first()
        val announcements = database.announcementDao().observeAll().first()
        val albumsWithMedia = database.galleryDao().observeAlbumsWithMedia().first()
        val albums = albumsWithMedia.map { it.album }
        val galleryMedia = database.galleryDao().observeAllMedia().first()
        val socialLinks = database.socialMediaDao().observeAll().first()
        val approvals = database.contentApprovalDao().getAllOnce()
        val versionHistory = database.versionHistoryDao().getAllOnce()
        val mediaStudio = database.mediaStudioDao().getAllOnce()
        val activityLogs = database.activityLogDao().getAllOnce()
        val notifications = database.localNotificationDao().getAllOnce()

        val root = JSONObject().apply {
            put("preferences", JSONObject().apply {
                put("selectedLanguage", prefs.selectedLanguage.code)
                put("isLanguageSelectedOnLaunch", prefs.isLanguageSelectedOnLaunch)
                put("themeMode", prefs.themeMode.storageKey)
                put("deviceAlertsEnabled", prefs.deviceAlertsEnabled)
            })
            put("tenant", JSONObject().apply {
                put("schoolId", SchoolTenantContext.DEFAULT_SCHOOL_ID)
                put("schoolCode", SchoolTenantContext.DEFAULT_SCHOOL_CODE)
            })
            put("schoolProfile", JSONObject(ContentSnapshotSerializer.schoolProfileToJson(profile)))
            put("heroPhotos", JSONArray().apply { heroPhotos.forEach { put(JSONObject(ContentSnapshotSerializer.heroPhotoToJson(it))) } })
            put("academicInfo", JSONArray().apply { academics.forEach { put(JSONObject(ContentSnapshotSerializer.academicInfoToJson(it))) } })
            put("courses", JSONArray().apply { courses.forEach { put(JSONObject(ContentSnapshotSerializer.courseToJson(it))) } })
            put("facilities", JSONArray().apply { facilities.forEach { put(JSONObject(ContentSnapshotSerializer.facilityToJson(it))) } })
            put("transportRoutes", JSONArray().apply { routes.forEach { put(JSONObject(ContentSnapshotSerializer.transportToJson(it))) } })
            put("newsItems", JSONArray().apply { news.forEach { put(JSONObject(ContentSnapshotSerializer.newsToJson(it))) } })
            put("announcements", JSONArray().apply { announcements.forEach { put(JSONObject(ContentSnapshotSerializer.announcementToJson(it))) } })
            put("galleryAlbums", JSONArray().apply { albums.forEach { put(JSONObject(ContentSnapshotSerializer.galleryAlbumToJson(it))) } })
            put("galleryMedia", JSONArray().apply { galleryMedia.forEach { put(JSONObject(ContentSnapshotSerializer.galleryMediaToJson(it))) } })
            put("socialLinks", JSONArray().apply { socialLinks.forEach { put(JSONObject(ContentSnapshotSerializer.socialToJson(it))) } })
            put("managementAccounts", JSONArray().apply { accounts.forEach { put(accountToJson(it)) } })
            put("staffPermissions", JSONArray().apply { permissions.forEach { put(permissionToJson(it)) } })
            put("contentApprovals", JSONArray().apply { approvals.forEach { put(approvalToJson(it)) } })
            put("versionHistory", JSONArray().apply { versionHistory.forEach { put(versionToJson(it)) } })
            put("mediaStudioItems", JSONArray().apply { mediaStudio.forEach { put(mediaStudioToJson(it)) } })
            put("activityLogs", JSONArray().apply { activityLogs.forEach { put(activityLogToJson(it)) } })
            put("localNotifications", JSONArray().apply { notifications.forEach { put(notificationToJson(it)) } })
        }

        val snapshotString = root.toString()
        val digestHex = computeSha256Hex(snapshotString.toByteArray(Charsets.UTF_8))
        val mediaFiles = collectManagedMediaFiles()
        val preview = BackupPreviewInfo(
            formatVersion = FORMAT_VERSION,
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appVersionName = BuildConfig.VERSION_NAME,
            exportedAtMillis = System.currentTimeMillis(),
            accountCount = accounts.size,
            contentRecordCount = academics.size + courses.size + facilities.size + routes.size + news.size + announcements.size + socialLinks.size,
            galleryItemCount = albums.size + galleryMedia.size + heroPhotos.size,
            mediaStudioCount = mediaStudio.size,
            mediaFileCount = mediaFiles.size,
            historyAndLogCount = versionHistory.size + activityLogs.size,
            schoolId = SchoolTenantContext.DEFAULT_SCHOOL_ID,
            snapshotSha256 = digestHex
        )
        return snapshotString to preview
    }

    private fun buildManifestJson(preview: BackupPreviewInfo): String = JSONObject().apply {
        put("formatVersion", preview.formatVersion)
        put("schemaVersion", preview.schemaVersion)
        put("appVersionName", preview.appVersionName)
        put("exportedAtMillis", preview.exportedAtMillis)
        put("schoolId", preview.schoolId)
        putOpt("snapshotSha256", preview.snapshotSha256)
        put("accountCount", preview.accountCount)
        put("contentRecordCount", preview.contentRecordCount)
        put("galleryItemCount", preview.galleryItemCount)
        put("mediaStudioCount", preview.mediaStudioCount)
        put("mediaFileCount", preview.mediaFileCount)
        put("historyAndLogCount", preview.historyAndLogCount)
    }.toString()

    private fun collectManagedMediaFiles(): List<Pair<String, File>> {
        val baseDir = File(context.filesDir, "chevalier_media")
        if (!baseDir.exists()) return emptyList()
        return baseDir.walkTopDown()
            .filter { it.isFile }
            .map { file ->
                val rel = file.relativeTo(baseDir).invariantSeparatorsPath
                rel to file
            }
            .toList()
    }

    private fun packageZip(manifestJson: String, snapshotJson: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(SNAPSHOT_ENTRY))
            zos.write(snapshotJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            collectManagedMediaFiles().forEach { (relPath, file) ->
                zos.putNextEntry(ZipEntry(MEDIA_PREFIX + relPath))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun encryptBytes(plainZipBytes: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val secretKey = try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
        } finally {
            keySpec.clearPassword()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainZipBytes)

        val out = ByteBuffer.allocate(MAGIC_HEADER.size + 4 + salt.size + iv.size + ciphertext.size)
        out.put(MAGIC_HEADER)
        out.putInt(FORMAT_VERSION)
        out.put(salt)
        out.put(iv)
        out.put(ciphertext)
        return out.array()
    }

    private fun decryptBytes(encryptedBytes: ByteArray, password: String): ByteArray {
        val minSize = MAGIC_HEADER.size + 4 + SALT_BYTES + IV_BYTES + 16
        if (encryptedBytes.size < minSize) {
            throw IllegalArgumentException("Corrupted or invalid backup archive.")
        }
        val buf = ByteBuffer.wrap(encryptedBytes)
        val header = ByteArray(MAGIC_HEADER.size)
        buf.get(header)
        if (!header.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("Unsupported backup file format.")
        }
        val version = buf.getInt()
        if (version != FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported encrypted backup version: $version.")
        }
        val salt = ByteArray(SALT_BYTES).also { buf.get(it) }
        val iv = ByteArray(IV_BYTES).also { buf.get(it) }
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val secretKey = try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
        } finally {
            keySpec.clearPassword()
        }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (t: Throwable) {
            throw IllegalArgumentException("Incorrect backup password or corrupted archive.", t)
        }
    }

    private fun unpackZipInMemory(zipBytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        var entryCount = 0
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw SecurityException("Backup archive exceeds safe entry count limit.")
                }
                val entryName = entry.name
                if (entryName.startsWith("/") || entryName.contains("..")) {
                    throw SecurityException("Invalid entry path inside backup archive.")
                }
                if (!entry.isDirectory) {
                    val data = zis.readBytes()
                    totalBytes += data.size
                    if (totalBytes > MAX_ZIP_UNCOMPRESSED_BYTES) {
                        throw SecurityException("Backup archive exceeds safe uncompressed size limit.")
                    }
                    result[entryName] = data
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return result
    }

    private fun computeSha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun verifySnapshotDigestIfPresent(
        manifest: JSONObject,
        snapshotStr: String,
        schemaVersion: Int
    ): String {
        val actualDigest = computeSha256Hex(snapshotStr.toByteArray(Charsets.UTF_8))
        val expectedDigest = if (schemaVersion >= CURRENT_SCHEMA_VERSION) {
            manifest.optNullableString("snapshotSha256")
        } else {
            null
        }
        if (expectedDigest != null) {
            val matches = MessageDigest.isEqual(
                expectedDigest.lowercase().toByteArray(Charsets.US_ASCII),
                actualDigest.toByteArray(Charsets.US_ASCII)
            )
            if (!matches) {
                throw IllegalArgumentException("Backup integrity verification failed (snapshot SHA-256 mismatch).")
            }
        }
        return actualDigest
    }

    private data class ParsedSnapshot(
        val languageCode: String,
        val isLanguageSelectedOnLaunch: Boolean,
        val themeModeKey: String,
        val deviceAlertsEnabled: Boolean?,
        val profile: com.example.data.local.entity.SchoolProfileEntity,
        val heroPhotos: List<com.example.data.local.entity.CampusHeroPhotoEntity>,
        val academics: List<com.example.data.local.entity.AcademicInfoEntity>,
        val courses: List<com.example.data.local.entity.HigherSecondaryCourseEntity>,
        val facilities: List<com.example.data.local.entity.FacilityEntity>,
        val transportRoutes: List<com.example.data.local.entity.TransportRouteEntity>,
        val news: List<com.example.data.local.entity.NewsItemEntity>,
        val announcements: List<com.example.data.local.entity.AnnouncementEntity>,
        val galleryAlbums: List<com.example.data.local.entity.GalleryAlbumEntity>,
        val galleryMedia: List<com.example.data.local.entity.GalleryMediaEntity>,
        val socialLinks: List<com.example.data.local.entity.SocialMediaLinkEntity>,
        val accounts: List<ManagementAccountEntity>,
        val permissions: List<StaffPermissionEntity>,
        val approvals: List<ContentApprovalEntity>,
        val versionHistory: List<VersionHistoryEntity>,
        val mediaStudioItems: List<MediaStudioItemEntity>,
        val activityLogs: List<ActivityLogEntity>,
        val notifications: List<LocalNotificationEntity>
    )

    private fun parseSnapshot(root: JSONObject): ParsedSnapshot {
        val prefsObj = root.optJSONObject("preferences")
        return ParsedSnapshot(
            languageCode = prefsObj?.optString("selectedLanguage", "en") ?: "en",
            isLanguageSelectedOnLaunch = prefsObj?.optBoolean("isLanguageSelectedOnLaunch", true) ?: true,
            themeModeKey = prefsObj?.optString("themeMode", "system") ?: "system",
            deviceAlertsEnabled = if (prefsObj != null && prefsObj.has("deviceAlertsEnabled")) {
                prefsObj.optBoolean("deviceAlertsEnabled", false)
            } else {
                null
            },
            profile = ContentSnapshotSerializer.schoolProfileFromJson(root.getJSONObject("schoolProfile").toString()),
            heroPhotos = root.optJSONArray("heroPhotos").mapObjects { ContentSnapshotSerializer.heroPhotoFromJson(it.toString(), publishNow = false) },
            academics = root.optJSONArray("academicInfo").mapObjects { ContentSnapshotSerializer.academicInfoFromJson(it.toString(), publishNow = false) },
            courses = root.optJSONArray("courses").mapObjects { ContentSnapshotSerializer.courseFromJson(it.toString(), publishNow = false) },
            facilities = root.optJSONArray("facilities").mapObjects { ContentSnapshotSerializer.facilityFromJson(it.toString(), publishNow = false) },
            transportRoutes = root.optJSONArray("transportRoutes").mapObjects { ContentSnapshotSerializer.transportFromJson(it.toString(), publishNow = false) },
            news = root.optJSONArray("newsItems").mapObjects { ContentSnapshotSerializer.newsFromJson(it.toString(), publishNow = false) },
            announcements = root.optJSONArray("announcements").mapObjects { ContentSnapshotSerializer.announcementFromJson(it.toString(), publishNow = false) },
            galleryAlbums = root.optJSONArray("galleryAlbums").mapObjects { ContentSnapshotSerializer.galleryAlbumFromJson(it.toString(), publishNow = false) },
            galleryMedia = root.optJSONArray("galleryMedia").mapObjects { ContentSnapshotSerializer.galleryMediaFromJson(it.toString(), publishNow = false) },
            socialLinks = root.optJSONArray("socialLinks").mapObjects { ContentSnapshotSerializer.socialFromJson(it.toString(), publishNow = false) },
            accounts = root.optJSONArray("managementAccounts").mapObjects { accountFromJson(it) },
            permissions = root.optJSONArray("staffPermissions").mapObjects { permissionFromJson(it) },
            approvals = root.optJSONArray("contentApprovals").mapObjects { approvalFromJson(it) },
            versionHistory = root.optJSONArray("versionHistory").mapObjects { versionFromJson(it) },
            mediaStudioItems = root.optJSONArray("mediaStudioItems").mapObjects { mediaStudioFromJson(it) },
            activityLogs = root.optJSONArray("activityLogs").mapObjects { activityLogFromJson(it) },
            notifications = root.optJSONArray("localNotifications").mapObjects { notificationFromJson(it) }
        )
    }

    private suspend fun applySnapshotInTransaction(
        s: ParsedSnapshot,
        liveMediaDir: File,
        actorName: String,
        actorAccountId: Long?,
        actorRole: String
    ) {
        val db = database.openHelper.writableDatabase
        // Clear child tables before parent tables
        db.execSQL("DELETE FROM staff_permissions")
        db.execSQL("DELETE FROM management_accounts")
        db.execSQL("DELETE FROM gallery_media")
        db.execSQL("DELETE FROM gallery_albums")
        db.execSQL("DELETE FROM campus_hero_photos")
        db.execSQL("DELETE FROM academic_info")
        db.execSQL("DELETE FROM higher_secondary_courses")
        db.execSQL("DELETE FROM facilities")
        db.execSQL("DELETE FROM transport_routes")
        db.execSQL("DELETE FROM news_items")
        db.execSQL("DELETE FROM announcements")
        db.execSQL("DELETE FROM social_media_links")
        db.execSQL("DELETE FROM content_approvals")
        db.execSQL("DELETE FROM version_history")
        db.execSQL("DELETE FROM media_studio_items")
        db.execSQL("DELETE FROM activity_logs")
        db.execSQL("DELETE FROM local_notifications")

        database.schoolProfileDao().upsertProfile(
            s.profile.copy(
                profileImageUri = s.profile.profileImageUri?.let { remapMediaUri(it, liveMediaDir) },
                aboutImageUri = s.profile.aboutImageUri?.let { remapMediaUri(it, liveMediaDir) }
            )
        )
        s.heroPhotos.forEach {
            database.campusHeroPhotoDao().upsert(it.copy(imageUri = remapMediaUri(it.imageUri, liveMediaDir)))
        }
        if (s.academics.isNotEmpty()) database.academicInfoDao().insertAll(s.academics)
        s.courses.forEach { database.higherSecondaryCourseDao().upsert(it) }
        s.facilities.forEach {
            database.facilityDao().upsert(
                it.copy(imageUri = it.imageUri?.let { uri -> remapMediaUri(uri, liveMediaDir) })
            )
        }
        s.transportRoutes.forEach { database.transportDao().upsert(it) }
        s.news.forEach {
            database.newsDao().upsert(
                it.copy(imageUri = it.imageUri?.let { uri -> remapMediaUri(uri, liveMediaDir) })
            )
        }
        s.announcements.forEach {
            database.announcementDao().upsert(
                it.copy(imageUri = it.imageUri?.let { uri -> remapMediaUri(uri, liveMediaDir) })
            )
        }
        s.galleryAlbums.forEach {
            database.galleryDao().upsertAlbum(
                it.copy(coverMediaUri = it.coverMediaUri?.let { uri -> remapMediaUri(uri, liveMediaDir) })
            )
        }
        s.galleryMedia.forEach {
            database.galleryDao().upsertMedia(
                it.copy(mediaUri = remapMediaUri(it.mediaUri, liveMediaDir))
            )
        }
        s.socialLinks.forEach { database.socialMediaDao().upsert(it) }

        s.accounts.forEach { database.managementAccountDao().insertAccount(it) }
        if (s.permissions.isNotEmpty()) database.managementAccountDao().upsertPermissions(s.permissions)
        s.approvals.forEach { approval ->
            database.contentApprovalDao().upsert(
                approval.copy(
                    previousSnapshotJson = approval.previousSnapshotJson?.let {
                        remapSnapshotJsonMediaUris(it, liveMediaDir)
                    },
                    proposedPayloadJson = remapSnapshotJsonMediaUris(
                        approval.proposedPayloadJson,
                        liveMediaDir
                    )
                )
            )
        }
        s.versionHistory.forEach { version ->
            database.versionHistoryDao().insert(
                version.copy(
                    previousContentJson = version.previousContentJson?.let {
                        remapSnapshotJsonMediaUris(it, liveMediaDir)
                    },
                    contentSnapshotJson = remapSnapshotJsonMediaUris(
                        version.contentSnapshotJson,
                        liveMediaDir
                    )
                )
            )
        }
        s.mediaStudioItems.forEach {
            database.mediaStudioDao().upsert(
                it.copy(
                    originalUri = remapMediaUri(it.originalUri, liveMediaDir),
                    optimizedUri = it.optimizedUri?.let { opt -> remapMediaUri(opt, liveMediaDir) }
                )
            )
        }
        s.activityLogs.forEach { database.activityLogDao().appendLog(it) }
        s.notifications.forEach { database.localNotificationDao().insert(it) }

        // Append restore audit trail entry & notification with accurate actorRole
        database.activityLogDao().appendLog(
            ActivityLogEntity(
                actionType = "BACKUP_RESTORED",
                actorAccountId = actorAccountId,
                actorName = actorName,
                actorRole = actorRole,
                sectionKey = ManagementSection.BACKUP_RESTORE.key,
                recordRef = "EncryptedBackup",
                details = "Restored school database and media archive safely."
            )
        )
        database.localNotificationDao().insert(
            LocalNotificationEntity(
                eventType = "BACKUP_RESTORED",
                title = "Backup Restored",
                message = "Encrypted archive restored by $actorName.",
                sectionKey = ManagementSection.BACKUP_RESTORE.key
            )
        )
    }

    private fun remapMediaUri(uriString: String, liveMediaDir: File): String {
        val normalized = uriString.replace('\\', '/')
        val marker = "chevalier_media/"
        val idx = normalized.indexOf(marker)
        if (idx < 0) return uriString
        val rel = normalized.substring(idx + marker.length).trimStart('/')
        val localFile = File(liveMediaDir, rel).canonicalFile
        if (!localFile.path.startsWith(liveMediaDir.canonicalPath)) {
            return uriString
        }
        return Uri.fromFile(localFile).toString()
    }

    private fun remapSnapshotJsonMediaUris(rawJson: String, liveMediaDir: File): String {
        if (!rawJson.contains("chevalier_media")) return rawJson
        return runCatching {
            val trimmed = rawJson.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(rawJson)
                if (remapJsonMediaFieldsInArray(arr, liveMediaDir)) arr.toString() else rawJson
            } else {
                val obj = JSONObject(rawJson)
                if (remapJsonMediaFieldsInObject(obj, liveMediaDir)) obj.toString() else rawJson
            }
        }.getOrDefault(rawJson)
    }

    private fun remapJsonMediaFieldsInObject(obj: JSONObject, liveMediaDir: File): Boolean {
        var changed = false
        val keys = ArrayList<String>(obj.length())
        val iter = obj.keys()
        while (iter.hasNext()) {
            keys.add(iter.next())
        }
        for (key in keys) {
            val value = obj.opt(key) ?: continue
            when (value) {
                is JSONObject -> {
                    if (remapJsonMediaFieldsInObject(value, liveMediaDir)) changed = true
                }
                is JSONArray -> {
                    if (remapJsonMediaFieldsInArray(value, liveMediaDir)) changed = true
                }
                is String -> {
                    if (key in SNAPSHOT_MEDIA_JSON_KEYS &&
                        !value.startsWith("http://", ignoreCase = true) &&
                        !value.startsWith("https://", ignoreCase = true) &&
                        value.contains("chevalier_media")
                    ) {
                        val remapped = remapMediaUri(value, liveMediaDir)
                        if (remapped != value) {
                            obj.put(key, remapped)
                            changed = true
                        }
                    }
                }
            }
        }
        return changed
    }

    private fun remapJsonMediaFieldsInArray(arr: JSONArray, liveMediaDir: File): Boolean {
        var changed = false
        for (i in 0 until arr.length()) {
            when (val item = arr.opt(i)) {
                is JSONObject -> if (remapJsonMediaFieldsInObject(item, liveMediaDir)) changed = true
                is JSONArray -> if (remapJsonMediaFieldsInArray(item, liveMediaDir)) changed = true
            }
        }
        return changed
    }

    private fun remapAdjustedSidecars(liveMediaDir: File) {
        val adjustedDir = File(liveMediaDir, "adjusted")
        if (!adjustedDir.exists() || !adjustedDir.isDirectory) return
        adjustedDir.listFiles { file -> file.isFile && file.name.endsWith(".orig") }?.forEach { sidecar ->
            val current = sidecar.readText().trim()
            if (current.isNotEmpty()) {
                val remapped = remapMediaUri(current, liveMediaDir)
                if (remapped != current) {
                    sidecar.writeText(remapped)
                }
            }
        }
    }

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val list = ArrayList<T>(length())
        for (i in 0 until length()) {
            list.add(transform(getJSONObject(i)))
        }
        return list
    }

    private fun accountToJson(a: ManagementAccountEntity): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("username", a.username)
        put("displayName", a.displayName)
        put("role", a.role)
        put("pinSaltBase64", a.pinSaltBase64)
        put("pinVerifierBase64", a.pinVerifierBase64)
        putOpt("recoverySaltBase64", a.recoverySaltBase64)
        putOpt("recoveryVerifierBase64", a.recoveryVerifierBase64)
        put("isActive", a.isActive)
        put("failedAttempts", a.failedAttempts)
        put("lockoutUntilMillis", a.lockoutUntilMillis)
        put("createdAtMillis", a.createdAtMillis)
        put("updatedAtMillis", a.updatedAtMillis)
    }

    private fun accountFromJson(o: JSONObject): ManagementAccountEntity = ManagementAccountEntity(
        id = o.getLong("id"),
        username = o.getString("username"),
        displayName = o.getString("displayName"),
        role = o.getString("role"),
        pinSaltBase64 = o.getString("pinSaltBase64"),
        pinVerifierBase64 = o.getString("pinVerifierBase64"),
        recoverySaltBase64 = o.optNullableString("recoverySaltBase64"),
        recoveryVerifierBase64 = o.optNullableString("recoveryVerifierBase64"),
        isActive = o.optBoolean("isActive", true),
        failedAttempts = o.optInt("failedAttempts", 0),
        lockoutUntilMillis = o.optLong("lockoutUntilMillis", 0L),
        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
    )

    private fun permissionToJson(p: StaffPermissionEntity): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("accountId", p.accountId)
        put("sectionKey", p.sectionKey)
        put("canView", p.canView)
        put("canCreate", p.canCreate)
        put("canEdit", p.canEdit)
        put("canDelete", p.canDelete)
        put("canPublish", p.canPublish)
    }

    private fun permissionFromJson(o: JSONObject): StaffPermissionEntity = StaffPermissionEntity(
        id = o.getLong("id"),
        accountId = o.getLong("accountId"),
        sectionKey = o.getString("sectionKey"),
        canView = o.optBoolean("canView", false),
        canCreate = o.optBoolean("canCreate", false),
        canEdit = o.optBoolean("canEdit", false),
        canDelete = o.optBoolean("canDelete", false),
        canPublish = o.optBoolean("canPublish", false)
    )

    private fun approvalToJson(a: ContentApprovalEntity): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("sectionKey", a.sectionKey)
        put("targetRecordId", a.targetRecordId)
        put("recordTitle", a.recordTitle)
        putOpt("previousSnapshotJson", a.previousSnapshotJson)
        put("proposedPayloadJson", a.proposedPayloadJson)
        put("changeSummary", a.changeSummary)
        put("submittedByAccountId", a.submittedByAccountId)
        put("submittedByName", a.submittedByName)
        put("submittedAtMillis", a.submittedAtMillis)
        put("status", a.status)
        putOpt("reviewedByAccountId", a.reviewedByAccountId)
        putOpt("reviewedByName", a.reviewedByName)
        putOpt("reviewedAtMillis", a.reviewedAtMillis)
        putOpt("rejectionReason", a.rejectionReason)
    }

    private fun approvalFromJson(o: JSONObject): ContentApprovalEntity = ContentApprovalEntity(
        id = o.getLong("id"),
        sectionKey = o.getString("sectionKey"),
        targetRecordId = o.optLong("targetRecordId", 0L),
        recordTitle = o.getString("recordTitle"),
        previousSnapshotJson = o.optNullableString("previousSnapshotJson"),
        proposedPayloadJson = o.getString("proposedPayloadJson"),
        changeSummary = o.getString("changeSummary"),
        submittedByAccountId = o.getLong("submittedByAccountId"),
        submittedByName = o.getString("submittedByName"),
        submittedAtMillis = o.optLong("submittedAtMillis", 0L),
        status = o.getString("status"),
        reviewedByAccountId = if (o.has("reviewedByAccountId") && !o.isNull("reviewedByAccountId")) o.getLong("reviewedByAccountId") else null,
        reviewedByName = o.optNullableString("reviewedByName"),
        reviewedAtMillis = if (o.has("reviewedAtMillis") && !o.isNull("reviewedAtMillis")) o.getLong("reviewedAtMillis") else null,
        rejectionReason = o.optNullableString("rejectionReason")
    )

    private fun versionToJson(v: VersionHistoryEntity): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("sectionKey", v.sectionKey)
        put("recordId", v.recordId)
        put("versionNumber", v.versionNumber)
        put("recordTitle", v.recordTitle)
        putOpt("previousContentJson", v.previousContentJson)
        put("contentSnapshotJson", v.contentSnapshotJson)
        put("changedByAccountId", v.changedByAccountId)
        put("changedByName", v.changedByName)
        put("changedAtMillis", v.changedAtMillis)
        put("changeSummary", v.changeSummary)
        put("approvalStatus", v.approvalStatus)
    }

    private fun versionFromJson(o: JSONObject): VersionHistoryEntity = VersionHistoryEntity(
        id = o.getLong("id"),
        sectionKey = o.getString("sectionKey"),
        recordId = o.getLong("recordId"),
        versionNumber = o.getInt("versionNumber"),
        recordTitle = o.getString("recordTitle"),
        previousContentJson = o.optNullableString("previousContentJson"),
        contentSnapshotJson = o.getString("contentSnapshotJson"),
        changedByAccountId = o.getLong("changedByAccountId"),
        changedByName = o.getString("changedByName"),
        changedAtMillis = o.optLong("changedAtMillis", 0L),
        changeSummary = o.getString("changeSummary"),
        approvalStatus = o.getString("approvalStatus")
    )

    private fun mediaStudioToJson(m: MediaStudioItemEntity): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("mediaKind", m.mediaKind)
        put("title", m.title)
        putOpt("caption", m.caption)
        put("categoryOrAlbum", m.categoryOrAlbum)
        put("originalUri", m.originalUri)
        putOpt("optimizedUri", m.optimizedUri)
        put("mimeType", m.mimeType)
        put("widthPx", m.widthPx)
        put("heightPx", m.heightPx)
        put("fileSizeBytes", m.fileSizeBytes)
        put("optimizedSizeBytes", m.optimizedSizeBytes)
        put("assignedSectionsCsv", m.assignedSectionsCsv)
        put("isFeatured", m.isFeatured)
        put("displayOrder", m.displayOrder)
        put("approvalStatus", m.approvalStatus)
        put("createdByName", m.createdByName)
        put("createdAtMillis", m.createdAtMillis)
    }

    private fun mediaStudioFromJson(o: JSONObject): MediaStudioItemEntity = MediaStudioItemEntity(
        id = o.getLong("id"),
        mediaKind = o.getString("mediaKind"),
        title = o.getString("title"),
        caption = o.optNullableString("caption"),
        categoryOrAlbum = o.optString("categoryOrAlbum", "Campus"),
        originalUri = o.getString("originalUri"),
        optimizedUri = o.optNullableString("optimizedUri"),
        mimeType = o.getString("mimeType"),
        widthPx = o.optInt("widthPx", 0),
        heightPx = o.optInt("heightPx", 0),
        fileSizeBytes = o.optLong("fileSizeBytes", 0L),
        optimizedSizeBytes = o.optLong("optimizedSizeBytes", 0L),
        assignedSectionsCsv = o.optString("assignedSectionsCsv", ""),
        isFeatured = o.optBoolean("isFeatured", false),
        displayOrder = o.optInt("displayOrder", 0),
        approvalStatus = o.optString("approvalStatus", "PUBLISHED"),
        createdByName = o.optString("createdByName", "Admin"),
        createdAtMillis = o.optLong("createdAtMillis", 0L)
    )

    private fun activityLogToJson(l: ActivityLogEntity): JSONObject = JSONObject().apply {
        put("id", l.id)
        put("actionType", l.actionType)
        putOpt("actorAccountId", l.actorAccountId)
        put("actorName", l.actorName)
        put("actorRole", l.actorRole)
        put("sectionKey", l.sectionKey)
        put("recordRef", l.recordRef)
        put("details", l.details)
        put("timestampMillis", l.timestampMillis)
    }

    private fun activityLogFromJson(o: JSONObject): ActivityLogEntity = ActivityLogEntity(
        id = o.getLong("id"),
        actionType = o.getString("actionType"),
        actorAccountId = if (o.has("actorAccountId") && !o.isNull("actorAccountId")) o.getLong("actorAccountId") else null,
        actorName = o.getString("actorName"),
        actorRole = o.getString("actorRole"),
        sectionKey = o.getString("sectionKey"),
        recordRef = o.getString("recordRef"),
        details = o.getString("details"),
        timestampMillis = o.optLong("timestampMillis", 0L)
    )

    private fun notificationToJson(n: LocalNotificationEntity): JSONObject = JSONObject().apply {
        put("id", n.id)
        put("eventType", n.eventType)
        put("title", n.title)
        put("message", n.message)
        put("sectionKey", n.sectionKey)
        put("isRead", n.isRead)
        put("createdAtMillis", n.createdAtMillis)
    }

    private fun notificationFromJson(o: JSONObject): LocalNotificationEntity = LocalNotificationEntity(
        id = o.getLong("id"),
        eventType = o.getString("eventType"),
        title = o.getString("title"),
        message = o.getString("message"),
        sectionKey = o.getString("sectionKey"),
        isRead = o.optBoolean("isRead", false),
        createdAtMillis = o.optLong("createdAtMillis", 0L)
    )
}
