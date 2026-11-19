package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.media.CropAspectRatioPreset
import com.example.data.media.MediaAdjustmentSpec
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.SchoolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManagementMediaBackupMigrationTest {

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = MediaStudioManager(context)
        mgmtRepo = ManagementRepository(database, mediaManager)
        prefsRepo = UserPreferencesRepository(context)
        backupManager = BackupRestoreManager(context, database, prefsRepo)
    }

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "chevalier_media").deleteRecursively()
    }

    private fun createTempImageFile(name: String, width: Int, height: Int, format: Bitmap.CompressFormat): File {
        val file = File(context.cacheDir, name)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(141, 32, 41))
        FileOutputStream(file).use { out ->
            bmp.compress(format, 95, out)
        }
        bmp.recycle()
        return file
    }

    private fun decryptBackupZipEntriesForTest(encryptedBytes: ByteArray, password: String): LinkedHashMap<String, ByteArray> {
        val buf = ByteBuffer.wrap(encryptedBytes)
        val header = ByteArray(8).also { buf.get(it) }
        assertEquals("CHV_BAK1", String(header, Charsets.US_ASCII))
        assertEquals(1, buf.getInt())
        val salt = ByteArray(16).also { buf.get(it) }
        val iv = ByteArray(12).also { buf.get(it) }
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
        keySpec.clearPassword()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val zipBytes = cipher.doFinal(ciphertext)

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return entries
    }

    private fun encryptBackupZipEntriesForTest(entries: Map<String, ByteArray>, password: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, data) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        val zipBytes = baos.toByteArray()
        val rng = SecureRandom()
        val salt = ByteArray(16).also { rng.nextBytes(it) }
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
        keySpec.clearPassword()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(zipBytes)

        val out = ByteBuffer.allocate(8 + 4 + salt.size + iv.size + ciphertext.size)
        out.put("CHV_BAK1".toByteArray(Charsets.US_ASCII))
        out.putInt(1)
        out.put(salt)
        out.put(iv)
        out.put(ciphertext)
        return out.array()
    }

    // ==================== F. MEDIA STUDIO ====================

    @Test
    fun `media studio optimizes oversized images while preserving original, validates youtube, and warns on delete`() = runTest {
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account

        val largeJpg = createTempImageFile("campus_large.jpg", 2400, 1600, Bitmap.CompressFormat.JPEG)
        val media = mgmtRepo.importImageToMediaStudio(
            actor = master,
            sourceUri = Uri.fromFile(largeJpg),
            title = "Main Campus Gate",
            caption = "Official campus entrance",
            categoryOrAlbum = "Campus",
            assignedSections = listOf(ManagementSection.PHOTO_GALLERY.key),
            isFeatured = true
        ).getOrThrow()

        assertTrue(media.widthPx <= 1600)
        assertTrue(media.heightPx <= 1600)
        assertTrue(mediaManager.isManagedMediaFileReadable(media.originalUri))
        assertTrue(mediaManager.isManagedMediaFileReadable(media.optimizedUri!!))
        assertNotEquals(media.originalUri, media.optimizedUri) // resized optimized file created alongside original

        // Assign to Hero Banner and Album -> usage warning detects assignments
        val albumId = mgmtRepo.saveManagedContentRecord(
            actor = master,
            section = ManagementSection.PHOTO_GALLERY,
            recordId = 0L,
            recordTitle = "Campus Tour",
            proposedJson = ContentSnapshotSerializer.galleryAlbumToJson(GalleryAlbumEntity(title = "Campus Tour")),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "New album"
        ).getOrThrow()
        mgmtRepo.assignMediaToHeroBanner(master, media.id, "Hero", PublishSaveMode.PUBLISH_NOW).getOrThrow()
        mgmtRepo.assignMediaToGalleryAlbum(master, media.id, albumId, PublishSaveMode.PUBLISH_NOW).getOrThrow()

        val warnings = mgmtRepo.getMediaUsageWarnings(media)
        assertTrue(warnings.any { it.contains("Hero") })
        assertTrue(warnings.any { it.contains("Gallery") })

        // Missing file detection
        assertFalse(mediaManager.isManagedMediaFileReadable("file:///nonexistent/missing_file.png"))

        // YouTube URL validation
        assertTrue(MediaStudioManager.isValidYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(MediaStudioManager.isValidYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(mgmtRepo.addYouTubeVideoToMediaStudio(master, "https://not-youtube.example.com/v", "Bad", null, "Videos", false).isFailure)
        assertTrue(mgmtRepo.addYouTubeVideoToMediaStudio(master, "https://youtu.be/dQw4w9WgXcQ", "Annual Day", null, "Events", false).isSuccess)
    }

    @Test
    fun `hero photo adjustment saves cropped 16 by 9 copy separately and preserves original untouched`() = runTest {
        val portraitOrSquareSource = createTempImageFile("campus_raw.jpg", 1200, 1200, Bitmap.CompressFormat.JPEG)
        val imported = mediaManager.importAndOptimizeImage(Uri.fromFile(portraitOrSquareSource)).getOrThrow()
        val originalFile = File(Uri.parse(imported.originalUri).path!!)
        val originalBytesBefore = originalFile.readBytes()

        val adjusted = mediaManager.saveAdjustedHeroCrop(
            sourceImageUri = imported.originalUri,
            zoom = 1.75f,
            panFractionX = 0.25f,
            panFractionY = -0.35f,
            viewportAspectRatio = MediaStudioManager.HERO_BANNER_ASPECT_RATIO
        ).getOrThrow()

        assertEquals(imported.originalUri, adjusted.originalUri)
        assertNotEquals(imported.originalUri, adjusted.optimizedUri)
        assertTrue(adjusted.optimizedUri.contains("/adjusted/"))
        assertTrue(mediaManager.isManagedMediaFileReadable(adjusted.originalUri))
        assertTrue(mediaManager.isManagedMediaFileReadable(adjusted.optimizedUri))
        assertEquals(imported.originalUri, mediaManager.resolveOriginalUri(adjusted.optimizedUri))

        // Verify adjusted bitmap respects 16:9 hero proportions and original bytes stayed 100% untouched
        val ratio = adjusted.widthPx.toFloat() / adjusted.heightPx.toFloat()
        assertTrue("Expected ~16:9 ratio, was $ratio", ratio in 1.70f..1.85f)
        assertTrue(originalBytesBefore.contentEquals(originalFile.readBytes()))
    }

    @Test
    fun `universal media engine supports all aspect ratios, zoom, pan, 90 degree rotations, and preserves original`() = runTest {
        val landscapeSource = createTempImageFile("universal_src.jpg", 1200, 800, Bitmap.CompressFormat.JPEG)
        val imported = mediaManager.importAndOptimizeImage(Uri.fromFile(landscapeSource)).getOrThrow()
        val originalFile = File(Uri.parse(imported.originalUri).path!!)
        val originalBytesBefore = originalFile.readBytes()

        // 1. ORIGINAL preserves 1.5 ratio
        val originalRatioRes = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.ORIGINAL)
        ).getOrThrow()
        val origRatio = originalRatioRes.widthPx.toFloat() / originalRatioRes.heightPx.toFloat()
        assertTrue("Expected ~1.5 ratio for ORIGINAL, got $origRatio", origRatio in 1.45f..1.55f)
        assertEquals(imported.originalUri, mediaManager.resolveOriginalUri(originalRatioRes.optimizedUri))

        // 2. FREE custom aspect ratio
        val freeRes = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(
                aspectPreset = CropAspectRatioPreset.FREE,
                customAspectRatio = 2.2f,
                zoom = 1.5f,
                panFractionX = 0.4f,
                panFractionY = -0.25f
            )
        ).getOrThrow()
        val freeRatio = freeRes.widthPx.toFloat() / freeRes.heightPx.toFloat()
        assertTrue("Expected ~2.2 ratio for FREE, got $freeRatio", freeRatio in 2.10f..2.30f)

        // 3. RATIO_16_9
        val ratio169Res = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.RATIO_16_9)
        ).getOrThrow()
        val r169 = ratio169Res.widthPx.toFloat() / ratio169Res.heightPx.toFloat()
        assertTrue("Expected ~16:9 ratio, got $r169", r169 in 1.70f..1.85f)

        // 4. RATIO_4_3
        val ratio43Res = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.RATIO_4_3)
        ).getOrThrow()
        val r43 = ratio43Res.widthPx.toFloat() / ratio43Res.heightPx.toFloat()
        assertTrue("Expected ~4:3 ratio, got $r43", r43 in 1.28f..1.38f)

        // 5. RATIO_1_1
        val ratio11Res = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.RATIO_1_1, zoom = 2.0f, panFractionX = -0.5f, panFractionY = 0.5f)
        ).getOrThrow()
        val r11 = ratio11Res.widthPx.toFloat() / ratio11Res.heightPx.toFloat()
        assertTrue("Expected ~1:1 ratio, got $r11", r11 in 0.96f..1.04f)

        // 6. 90 degree clockwise and anticlockwise rotations flip orientation under ORIGINAL preset
        val cwDegrees = MediaStudioManager.rotateQuarterTurns(0, clockwise = true)
        val ccwDegrees = MediaStudioManager.rotateQuarterTurns(0, clockwise = false)
        assertEquals(90, cwDegrees)
        assertEquals(270, ccwDegrees)
        assertEquals(0, MediaStudioManager.rotateQuarterTurns(270, clockwise = true))
        assertEquals(180, MediaStudioManager.rotateQuarterTurns(270, clockwise = false))

        val rotatedCwRes = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.ORIGINAL, rotationDegrees = cwDegrees)
        ).getOrThrow()
        assertTrue("CW rotation should swap landscape to portrait", rotatedCwRes.heightPx > rotatedCwRes.widthPx)
        val rotatedCwRatio = rotatedCwRes.widthPx.toFloat() / rotatedCwRes.heightPx.toFloat()
        assertTrue("Expected ~0.667 portrait ratio, got $rotatedCwRatio", rotatedCwRatio in 0.62f..0.72f)

        val rotatedCcwRes = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.ORIGINAL, rotationDegrees = -90)
        ).getOrThrow()
        assertTrue("CCW rotation should swap landscape to portrait", rotatedCcwRes.heightPx > rotatedCcwRes.widthPx)

        // Re-adjusting an already-adjusted URI resolves back to the untouched original
        val reEditedRes = mediaManager.saveAdjustedMedia(
            sourceImageUri = rotatedCwRes.optimizedUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.RATIO_4_3, rotationDegrees = 180)
        ).getOrThrow()
        assertEquals(imported.originalUri, reEditedRes.originalUri)
        assertTrue(reEditedRes.optimizedUri.contains("/adjusted/"))

        // Original file bytes stayed 100% untouched across all crops and rotations
        assertTrue(originalBytesBefore.contentEquals(originalFile.readBytes()))
    }

    // ==================== G. ENCRYPTED BACKUP & RESTORE ====================

    @Test
    fun `encrypted backup restores db and media, rejects bad password or corruption, and rolls back on failure`() = runTest {
        SchoolRepository.fromDatabase(database).ensureVerifiedProfileSeeded()
        prefsRepo.setLanguage(AppLanguage.TAMIL, markSelectedOnLaunch = true)
        prefsRepo.setThemeMode(AppThemeMode.DARK)

        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
        val pngFile = createTempImageFile("emblem_copy.png", 600, 600, Bitmap.CompressFormat.PNG)
        val importedMedia = mgmtRepo.importImageToMediaStudio(
            actor = master,
            sourceUri = Uri.fromFile(pngFile),
            title = "Library Hall",
            caption = null,
            categoryOrAlbum = "Facilities",
            assignedSections = emptyList(),
            isFeatured = false
        ).getOrThrow()

        mgmtRepo.saveManagedContentRecord(
            actor = master,
            section = ManagementSection.NEWS,
            recordId = 0L,
            recordTitle = "Verified Science Fair",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(title = "Verified Science Fair", summary = "S", fullBody = "B", publishedDateText = "12 Nov")
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Added fair"
        ).getOrThrow()

        // Create encrypted archive
        val encryptedBytes = backupManager.createEncryptedBackupBytes("StrongPass123").getOrThrow()
        assertFalse(String(encryptedBytes, Charsets.UTF_8).contains("StrongPass123"))

        // Inspect with wrong password -> fails safely
        assertTrue(backupManager.inspectEncryptedBackup(encryptedBytes, "WrongPassword").isFailure)

        // Inspect corrupted archive -> fails safely
        val corrupted = encryptedBytes.copyOf().also { it[it.size - 10] = (it[it.size - 10].toInt() xor 0xFF).toByte() }
        assertTrue(backupManager.inspectEncryptedBackup(corrupted, "StrongPass123").isFailure)

        // Clear/mutate live database & delete local media to prove restore recovers everything cleanly
        database.newsDao().observeAll().first().forEach { database.newsDao().deleteById(it.id) }
        File(Uri.parse(importedMedia.originalUri).path!!).delete()
        prefsRepo.setLanguage(AppLanguage.ENGLISH, markSelectedOnLaunch = false)

        // Preview succeeds with accurate counts
        val preview = backupManager.inspectEncryptedBackup(encryptedBytes, "StrongPass123").getOrThrow()
        assertEquals(4, preview.schemaVersion)
        assertEquals(1, preview.accountCount)
        assertEquals(1, preview.mediaStudioCount)
        assertTrue(preview.mediaFileCount >= 1)

        // Restore with simulated failure mid-transaction -> rolls back DB + preferences without leaving partial state!
        database.newsDao().upsert(
            NewsItemEntity(
                title = "PreRestoreSentinel",
                summary = "Sentinel",
                fullBody = "Body",
                publishedDateText = "Now",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        val failedRestore = backupManager.restoreEncryptedBackupBytes(
            bytes = encryptedBytes,
            password = "StrongPass123",
            actorName = master.account.displayName,
            simulateFailureAfterClearForTest = true
        )
        assertTrue(failedRestore.isFailure)
        assertEquals("PreRestoreSentinel", database.newsDao().observeAll().first().single().title)

        // Genuine restore succeeds and restores News, Accounts, Media files & Preferences
        val restored = backupManager.restoreEncryptedBackupBytes(
            bytes = encryptedBytes,
            password = "StrongPass123",
            actorName = master.account.displayName
        ).getOrThrow()
        assertEquals(1, restored.accountCount)
        assertEquals("Verified Science Fair", database.newsDao().observeAll().first().single().title)
        assertTrue(mediaManager.isManagedMediaFileReadable(importedMedia.originalUri))
        assertEquals(AppLanguage.TAMIL, prefsRepo.preferencesFlow.first().selectedLanguage)
    }

    @Test
    fun `v4 backup preserves new media uris and displayOrder fields and remaps foreign device paths including sidecars`() = runTest {
        SchoolRepository.fromDatabase(database).ensureVerifiedProfileSeeded()
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account

        val rawImg = createTempImageFile("v4_entity_photo.jpg", 1000, 800, Bitmap.CompressFormat.JPEG)
        val imported = mediaManager.importAndOptimizeImage(Uri.fromFile(rawImg)).getOrThrow()
        val adjusted = mediaManager.saveAdjustedMedia(
            sourceImageUri = imported.originalUri,
            spec = MediaAdjustmentSpec(aspectPreset = CropAspectRatioPreset.RATIO_4_3, rotationDegrees = 90)
        ).getOrThrow()

        val currentProfile = database.schoolProfileDao().getProfileOnce()!!
        database.schoolProfileDao().upsertProfile(
            currentProfile.copy(
                profileImageUri = imported.originalUri,
                aboutImageUri = adjusted.optimizedUri
            )
        )
        database.facilityDao().upsert(
            FacilityEntity(
                name = "STEM Lab",
                category = "Labs",
                description = "Robotics & Science",
                imageUri = adjusted.optimizedUri,
                displayOrder = 2,
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        database.newsDao().upsert(
            NewsItemEntity(
                title = "Second By Order",
                summary = "S2",
                fullBody = "B2",
                publishedDateText = "12 Nov",
                publishedAtMillis = 9_999L,
                imageUri = imported.originalUri,
                displayOrder = 5,
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        database.newsDao().upsert(
            NewsItemEntity(
                title = "First By Order",
                summary = "S1",
                fullBody = "B1",
                publishedDateText = "10 Nov",
                publishedAtMillis = 1_000L,
                imageUri = adjusted.optimizedUri,
                displayOrder = 1,
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        database.announcementDao().upsert(
            AnnouncementEntity(
                title = "Exam Circular",
                message = "Timetable released",
                issuedDateText = "13 Nov",
                imageUri = imported.originalUri,
                displayOrder = 3,
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        database.transportDao().upsert(
            TransportRouteEntity(
                routeNumber = "R-08",
                routeName = "West Corridor",
                stopsSummary = "Main Stop",
                displayOrder = 4,
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        database.galleryDao().upsertAlbum(
            GalleryAlbumEntity(
                title = "Sports Meet",
                category = "Sports",
                coverMediaUri = adjusted.optimizedUri,
                displayOrder = 7,
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )

        // Export v4 backup, then rewrite local file URI prefix inside snapshot & sidecar to simulate a different device/app path
        val v4Encrypted = backupManager.createEncryptedBackupBytes("PassV4!").getOrThrow()
        val entries = decryptBackupZipEntriesForTest(v4Encrypted, "PassV4!")
        val currentRootPrefix = Uri.fromFile(File(context.filesDir, "chevalier_media")).toString()
        val foreignPrefix = "file:///data/user/0/com.other.device.install/files/chevalier_media"

        val rewrittenEntries = entries.mapValues { (key, value) ->
            when {
                key == "database_snapshot.json" || (key.startsWith("media_files/adjusted/") && key.endsWith(".orig")) ->
                    String(value, Charsets.UTF_8).replace(currentRootPrefix, foreignPrefix).toByteArray(Charsets.UTF_8)
                else -> value
            }
        }
        val foreignEncrypted = encryptBackupZipEntriesForTest(rewrittenEntries, "PassV4!")

        // Wipe local DB and media before restoring
        database.clearAllTables()
        File(context.filesDir, "chevalier_media").deleteRecursively()

        val restoredPreview = backupManager.restoreEncryptedBackupBytes(
            bytes = foreignEncrypted,
            password = "PassV4!",
            actorName = master.account.displayName
        ).getOrThrow()
        assertEquals(4, restoredPreview.schemaVersion)

        val restoredProfile = database.schoolProfileDao().getProfileOnce()!!
        val restoredFacility = database.facilityDao().observeAll().first().single()
        val restoredNews = database.newsDao().observePublic().first()
        val restoredAnnouncement = database.announcementDao().observeAll().first().single()
        val restoredRoute = database.transportDao().observeAll().first().single()
        val restoredAlbum = database.galleryDao().observeAlbumsWithMedia().first().single().album

        // Verify all new URI fields were remapped back to current device directory & readable
        assertEquals(imported.originalUri, restoredProfile.profileImageUri)
        assertEquals(adjusted.optimizedUri, restoredProfile.aboutImageUri)
        assertEquals(adjusted.optimizedUri, restoredFacility.imageUri)
        assertEquals(imported.originalUri, restoredAnnouncement.imageUri)
        assertTrue(mediaManager.isManagedMediaFileReadable(restoredProfile.profileImageUri!!))
        assertTrue(mediaManager.isManagedMediaFileReadable(restoredProfile.aboutImageUri!!))

        // Sidecar .orig file was also remapped so resolveOriginalUri recovers originalUri
        assertEquals(imported.originalUri, mediaManager.resolveOriginalUri(restoredProfile.aboutImageUri!!))

        // Verify displayOrder preserved and public query respects displayOrder ASC
        assertEquals(listOf("First By Order", "Second By Order"), restoredNews.map { it.title })
        assertEquals(listOf(1, 5), restoredNews.map { it.displayOrder })
        assertEquals(3, restoredAnnouncement.displayOrder)
        assertEquals(4, restoredRoute.displayOrder)
        assertEquals(7, restoredAlbum.displayOrder)
    }

    @Test
    fun `restore accepts legacy schema v1, v2, and v3 backups without new v4 fields`() = runTest {
        SchoolRepository.fromDatabase(database).ensureVerifiedProfileSeeded()
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
        database.facilityDao().upsert(
            FacilityEntity(
                name = "Auditorium",
                category = "Campus",
                description = "Main hall",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        database.newsDao().upsert(
            NewsItemEntity(
                title = "Legacy News Item",
                summary = "Summary",
                fullBody = "Body",
                publishedDateText = "01 Nov",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )

        val baseBytes = backupManager.createEncryptedBackupBytes("LegacyPass9").getOrThrow()
        val baseEntries = decryptBackupZipEntriesForTest(baseBytes, "LegacyPass9")
        val snapshotJson = JSONObject(String(baseEntries["database_snapshot.json"]!!, Charsets.UTF_8))

        // Strip all v4-added keys to authentically mirror a v1/v2/v3 backup snapshot
        snapshotJson.getJSONObject("schoolProfile").apply {
            remove("profileImageUri")
            remove("aboutImageUri")
        }
        fun JSONArray?.stripKey(key: String) {
            if (this == null) return
            for (i in 0 until length()) {
                getJSONObject(i).remove(key)
            }
        }
        snapshotJson.optJSONArray("facilities").stripKey("imageUri")
        snapshotJson.optJSONArray("newsItems").apply {
            stripKey("imageUri")
            stripKey("displayOrder")
        }
        snapshotJson.optJSONArray("announcements").apply {
            stripKey("imageUri")
            stripKey("displayOrder")
        }
        snapshotJson.optJSONArray("transportRoutes").stripKey("displayOrder")
        snapshotJson.optJSONArray("galleryAlbums").stripKey("displayOrder")

        for (legacySchema in listOf(1, 2, 3)) {
            val manifestJson = JSONObject(String(baseEntries["manifest.json"]!!, Charsets.UTF_8)).apply {
                put("schemaVersion", legacySchema)
            }
            val legacyEntries = LinkedHashMap(baseEntries).apply {
                put("manifest.json", manifestJson.toString().toByteArray(Charsets.UTF_8))
                put("database_snapshot.json", snapshotJson.toString().toByteArray(Charsets.UTF_8))
            }
            val legacyBytes = encryptBackupZipEntriesForTest(legacyEntries, "LegacyPass9")

            database.clearAllTables()
            val preview = backupManager.inspectEncryptedBackup(legacyBytes, "LegacyPass9").getOrThrow()
            assertEquals(legacySchema, preview.schemaVersion)

            val restored = backupManager.restoreEncryptedBackupBytes(
                bytes = legacyBytes,
                password = "LegacyPass9",
                actorName = master.account.displayName
            ).getOrThrow()
            assertEquals(legacySchema, restored.schemaVersion)

            val profile = database.schoolProfileDao().getProfileOnce()!!
            assertNull(profile.profileImageUri)
            assertNull(profile.aboutImageUri)
            val news = database.newsDao().observeAll().first().single()
            assertEquals("Legacy News Item", news.title)
            assertNull(news.imageUri)
            assertEquals(0, news.displayOrder)
        }
    }

    // ==================== H. NON-DESTRUCTIVE ROOM MIGRATIONS ====================

    @Test
    fun `room migration 2 to 4 preserves all prompt 2 tables and enables prompt 3 and phase 2b1 tables`() = runTest {
        val dbName = "migration_test_v2_to_v4.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()

        // Create authentic Version 2 SQLite DB directly matching Prompt 2 schema
        val v2Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v2Db.version = 2
        v2Db.execSQL(
            """
            CREATE TABLE school_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                schoolName TEXT NOT NULL,
                shortName TEXT NOT NULL,
                motto TEXT NOT NULL,
                addressLine TEXT NOT NULL,
                emblemMark TEXT NOT NULL,
                poweredBy TEXT NOT NULL,
                welcomeIntroduction TEXT,
                mediumAndGradesNote TEXT,
                mapCoordinatesOrQuery TEXT,
                phone TEXT,
                email TEXT,
                website TEXT,
                officeHours TEXT,
                principalName TEXT,
                history TEXT,
                vision TEXT,
                mission TEXT,
                affiliationNumber TEXT,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v2Db.execSQL("CREATE TABLE campus_hero_photos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, imageUri TEXT NOT NULL, caption TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL)")
        v2Db.execSQL("CREATE TABLE academic_info (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)")
        v2Db.execSQL("CREATE TABLE higher_secondary_courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, streamTitle TEXT NOT NULL, groupCode TEXT NOT NULL, subjectsSummary TEXT NOT NULL, mediumOfInstruction TEXT NOT NULL, classLevelLabel TEXT, eligibilityNote TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v2Db.execSQL("CREATE TABLE facilities (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, capacityOrSpec TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v2Db.execSQL("CREATE TABLE transport_routes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeNumber TEXT NOT NULL, routeName TEXT NOT NULL, stopsSummary TEXT NOT NULL, morningTiming TEXT, afternoonTiming TEXT, coordinatorContact TEXT, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v2Db.execSQL("CREATE TABLE news_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, fullBody TEXT NOT NULL, publishedDateText TEXT NOT NULL, publishedAtMillis INTEGER NOT NULL, isFeatured INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v2Db.execSQL("CREATE TABLE announcements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, priorityLabel TEXT, issuedDateText TEXT NOT NULL, issuedAtMillis INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v2Db.execSQL("CREATE TABLE gallery_albums (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT, eventDateText TEXT, coverMediaUri TEXT, createdAtMillis INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v2Db.execSQL("CREATE TABLE gallery_media (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, albumId INTEGER NOT NULL, title TEXT, caption TEXT, mediaUri TEXT NOT NULL, mediaType TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL, FOREIGN KEY(albumId) REFERENCES gallery_albums(id) ON DELETE CASCADE)")
        v2Db.execSQL("CREATE INDEX index_gallery_media_albumId ON gallery_media(albumId)")
        v2Db.execSQL("CREATE TABLE social_media_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, platformName TEXT NOT NULL, handleOrLabel TEXT NOT NULL, url TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")

        v2Db.execSQL(
            """
            INSERT INTO school_profile (id, schoolName, shortName, motto, addressLine, emblemMark, poweredBy, updatedAtMillis)
            VALUES (1, 'CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL', 'Chevalier Academy', 'Learn to Live & Love', 'Siluvathur Road, Dindigul - 624 306', 'M S C', 'POWERED BY JK LABS', 1000)
            """.trimIndent()
        )
        v2Db.execSQL(
            """
            INSERT INTO higher_secondary_courses (streamTitle, groupCode, subjectsSummary, mediumOfInstruction, classLevelLabel, displayOrder, isVerified, isPublished, isApproved, approvalStatus)
            VALUES ('Bio-Maths', 'GROUP_1', 'Physics, Chemistry, Biology, Mathematics', 'English', 'XI + XII', 1, 1, 1, 1, 'APPROVED')
            """.trimIndent()
        )
        v2Db.close()

        // Open with Room v4 using MIGRATION_2_3 + MIGRATION_3_4 (no destructive migration!)
        val migratedDb = Room.databaseBuilder(context, ChevalierDatabase::class.java, dbName)
            .addMigrations(ChevalierDatabase.MIGRATION_2_3, ChevalierDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val migratedProfile = migratedDb.schoolProfileDao().getProfileOnce()
        val migratedCourses = migratedDb.higherSecondaryCourseDao().observeAll().first()
        assertEquals("Learn to Live & Love", migratedProfile?.motto)
        assertNull(migratedProfile?.profileImageUri)
        assertNull(migratedProfile?.aboutImageUri)
        assertEquals(1, migratedCourses.size)
        assertEquals("Bio-Maths", migratedCourses.first().streamTitle)

        // Verify new Prompt 3 tables work immediately after migration
        val migratedMgmtRepo = ManagementRepository(migratedDb, mediaManager)
        val setup = migratedMgmtRepo.setupMasterAdmin("Admin", "admin", "246810", "246810").getOrThrow()
        assertEquals("admin", setup.account.account.username)
        assertTrue(migratedDb.activityLogDao().getAllOnce().isNotEmpty())

        migratedDb.close()
        dbFile.delete()
    }

    @Test
    fun `room migration 3 to 4 safely preserves all existing records and adds media uri and displayOrder columns`() = runTest {
        val dbName = "migration_test_v3_to_v4.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()

        // Build Version 2 base tables then run MIGRATION_2_3 to produce an authentic Version 3 SQLite DB
        val v3Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v3Db.version = 3
        v3Db.execSQL(
            """
            CREATE TABLE school_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                schoolName TEXT NOT NULL,
                shortName TEXT NOT NULL,
                motto TEXT NOT NULL,
                addressLine TEXT NOT NULL,
                emblemMark TEXT NOT NULL,
                poweredBy TEXT NOT NULL,
                welcomeIntroduction TEXT,
                mediumAndGradesNote TEXT,
                mapCoordinatesOrQuery TEXT,
                phone TEXT,
                email TEXT,
                website TEXT,
                officeHours TEXT,
                principalName TEXT,
                history TEXT,
                vision TEXT,
                mission TEXT,
                affiliationNumber TEXT,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v3Db.execSQL("CREATE TABLE campus_hero_photos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, imageUri TEXT NOT NULL, caption TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL)")
        v3Db.execSQL("CREATE TABLE academic_info (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)")
        v3Db.execSQL("CREATE TABLE higher_secondary_courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, streamTitle TEXT NOT NULL, groupCode TEXT NOT NULL, subjectsSummary TEXT NOT NULL, mediumOfInstruction TEXT NOT NULL, classLevelLabel TEXT, eligibilityNote TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v3Db.execSQL("CREATE TABLE facilities (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, capacityOrSpec TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v3Db.execSQL("CREATE TABLE transport_routes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeNumber TEXT NOT NULL, routeName TEXT NOT NULL, stopsSummary TEXT NOT NULL, morningTiming TEXT, afternoonTiming TEXT, coordinatorContact TEXT, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v3Db.execSQL("CREATE TABLE news_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, fullBody TEXT NOT NULL, publishedDateText TEXT NOT NULL, publishedAtMillis INTEGER NOT NULL, isFeatured INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v3Db.execSQL("CREATE TABLE announcements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, priorityLabel TEXT, issuedDateText TEXT NOT NULL, issuedAtMillis INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v3Db.execSQL("CREATE TABLE gallery_albums (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT, eventDateText TEXT, coverMediaUri TEXT, createdAtMillis INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v3Db.execSQL("CREATE TABLE gallery_media (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, albumId INTEGER NOT NULL, title TEXT, caption TEXT, mediaUri TEXT NOT NULL, mediaType TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL, FOREIGN KEY(albumId) REFERENCES gallery_albums(id) ON DELETE CASCADE)")
        v3Db.execSQL("CREATE INDEX index_gallery_media_albumId ON gallery_media(albumId)")
        v3Db.execSQL("CREATE TABLE social_media_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, platformName TEXT NOT NULL, handleOrLabel TEXT NOT NULL, url TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")

        // Prompt 3 v3 tables
        v3Db.execSQL("CREATE TABLE management_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT NOT NULL, displayName TEXT NOT NULL, role TEXT NOT NULL, pinSaltBase64 TEXT NOT NULL, pinVerifierBase64 TEXT NOT NULL, recoverySaltBase64 TEXT, recoveryVerifierBase64 TEXT, isActive INTEGER NOT NULL, failedAttempts INTEGER NOT NULL, lockoutUntilMillis INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL)")
        v3Db.execSQL("CREATE UNIQUE INDEX index_management_accounts_username ON management_accounts (username)")
        v3Db.execSQL("CREATE TABLE staff_permissions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, sectionKey TEXT NOT NULL, canView INTEGER NOT NULL, canCreate INTEGER NOT NULL, canEdit INTEGER NOT NULL, canDelete INTEGER NOT NULL, canPublish INTEGER NOT NULL, FOREIGN KEY(accountId) REFERENCES management_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        v3Db.execSQL("CREATE INDEX index_staff_permissions_accountId ON staff_permissions (accountId)")
        v3Db.execSQL("CREATE UNIQUE INDEX index_staff_permissions_accountId_sectionKey ON staff_permissions (accountId, sectionKey)")
        v3Db.execSQL("CREATE TABLE content_approvals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionKey TEXT NOT NULL, targetRecordId INTEGER NOT NULL, recordTitle TEXT NOT NULL, previousSnapshotJson TEXT, proposedPayloadJson TEXT NOT NULL, changeSummary TEXT NOT NULL, submittedByAccountId INTEGER NOT NULL, submittedByName TEXT NOT NULL, submittedAtMillis INTEGER NOT NULL, status TEXT NOT NULL, reviewedByAccountId INTEGER, reviewedByName TEXT, reviewedAtMillis INTEGER, rejectionReason TEXT)")
        v3Db.execSQL("CREATE INDEX index_content_approvals_sectionKey ON content_approvals (sectionKey)")
        v3Db.execSQL("CREATE INDEX index_content_approvals_status ON content_approvals (status)")
        v3Db.execSQL("CREATE INDEX index_content_approvals_submittedByAccountId ON content_approvals (submittedByAccountId)")
        v3Db.execSQL("CREATE TABLE version_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionKey TEXT NOT NULL, recordId INTEGER NOT NULL, versionNumber INTEGER NOT NULL, recordTitle TEXT NOT NULL, previousContentJson TEXT, contentSnapshotJson TEXT NOT NULL, changedByAccountId INTEGER NOT NULL, changedByName TEXT NOT NULL, changedAtMillis INTEGER NOT NULL, changeSummary TEXT NOT NULL, approvalStatus TEXT NOT NULL)")
        v3Db.execSQL("CREATE INDEX index_version_history_sectionKey_recordId ON version_history (sectionKey, recordId)")
        v3Db.execSQL("CREATE TABLE media_studio_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mediaKind TEXT NOT NULL, title TEXT NOT NULL, caption TEXT, categoryOrAlbum TEXT NOT NULL, originalUri TEXT NOT NULL, optimizedUri TEXT, mimeType TEXT NOT NULL, widthPx INTEGER NOT NULL, heightPx INTEGER NOT NULL, fileSizeBytes INTEGER NOT NULL, optimizedSizeBytes INTEGER NOT NULL, assignedSectionsCsv TEXT NOT NULL, isFeatured INTEGER NOT NULL, displayOrder INTEGER NOT NULL, approvalStatus TEXT NOT NULL, createdByName TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
        v3Db.execSQL("CREATE INDEX index_media_studio_items_mediaKind ON media_studio_items (mediaKind)")
        v3Db.execSQL("CREATE INDEX index_media_studio_items_approvalStatus ON media_studio_items (approvalStatus)")
        v3Db.execSQL("CREATE TABLE activity_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, actionType TEXT NOT NULL, actorAccountId INTEGER, actorName TEXT NOT NULL, actorRole TEXT NOT NULL, sectionKey TEXT NOT NULL, recordRef TEXT NOT NULL, details TEXT NOT NULL, timestampMillis INTEGER NOT NULL)")
        v3Db.execSQL("CREATE INDEX index_activity_logs_timestampMillis ON activity_logs (timestampMillis)")
        v3Db.execSQL("CREATE INDEX index_activity_logs_actorAccountId ON activity_logs (actorAccountId)")
        v3Db.execSQL("CREATE INDEX index_activity_logs_sectionKey ON activity_logs (sectionKey)")
        v3Db.execSQL("CREATE TABLE local_notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, eventType TEXT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, sectionKey TEXT NOT NULL, isRead INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL)")
        v3Db.execSQL("CREATE INDEX index_local_notifications_isRead ON local_notifications (isRead)")
        v3Db.execSQL("CREATE INDEX index_local_notifications_createdAtMillis ON local_notifications (createdAtMillis)")

        // Seed existing v3 records across all migrated tables (both approved & pending visibility)
        v3Db.execSQL(
            """
            INSERT INTO school_profile (id, schoolName, shortName, motto, addressLine, emblemMark, poweredBy, updatedAtMillis)
            VALUES (1, 'CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL', 'Chevalier Academy', 'Learn to Live & Love', 'Siluvathur Road, Dindigul - 624 306', 'M S C', 'POWERED BY JK LABS', 5000)
            """.trimIndent()
        )
        v3Db.execSQL(
            """
            INSERT INTO facilities (name, category, description, capacityOrSpec, displayOrder, isVerified, isPublished, isApproved, approvalStatus)
            VALUES ('Computer Lab', 'Academic', 'Modern lab', '40 Systems', 1, 1, 1, 1, 'APPROVED')
            """.trimIndent()
        )
        v3Db.execSQL(
            """
            INSERT INTO transport_routes (routeNumber, routeName, stopsSummary, isVerified, isPublished, isApproved, approvalStatus)
            VALUES ('R-01', 'Dindigul Bus Stand', 'Stops A-Z', 1, 1, 1, 'APPROVED')
            """.trimIndent()
        )
        v3Db.execSQL(
            """
            INSERT INTO news_items (title, summary, fullBody, publishedDateText, publishedAtMillis, isFeatured, isVerified, isPublished, isApproved, approvalStatus)
            VALUES ('Published News', 'S', 'B', '10 Nov', 10000, 0, 1, 1, 1, 'APPROVED'),
                   ('Pending Draft News', 'S2', 'B2', '11 Nov', 20000, 0, 0, 0, 0, 'PENDING')
            """.trimIndent()
        )
        v3Db.execSQL(
            """
            INSERT INTO announcements (title, message, issuedDateText, issuedAtMillis, isVerified, isPublished, isApproved, approvalStatus)
            VALUES ('Holiday Notice', 'School closed tomorrow', '12 Nov', 15000, 1, 1, 1, 'APPROVED')
            """.trimIndent()
        )
        v3Db.execSQL(
            """
            INSERT INTO gallery_albums (title, category, createdAtMillis, isVerified, isPublished, isApproved, approvalStatus)
            VALUES ('Annual Day', 'Events', 12000, 1, 1, 1, 'APPROVED')
            """.trimIndent()
        )
        v3Db.close()

        // Open with Room v4 using MIGRATION_3_4
        val migratedDb = Room.databaseBuilder(context, ChevalierDatabase::class.java, dbName)
            .addMigrations(ChevalierDatabase.MIGRATION_2_3, ChevalierDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        // Verify all existing records & public approval rules survived intact
        val profile = migratedDb.schoolProfileDao().getProfileOnce()!!
        assertEquals("Learn to Live & Love", profile.motto)
        assertNull(profile.profileImageUri)
        assertNull(profile.aboutImageUri)

        val facility = migratedDb.facilityDao().observePublic().first().single()
        assertEquals("Computer Lab", facility.name)
        assertNull(facility.imageUri)

        val route = migratedDb.transportDao().observePublic().first().single()
        assertEquals("R-01", route.routeNumber)
        assertEquals(0, route.displayOrder)

        val allNews = migratedDb.newsDao().observeAll().first()
        val publicNews = migratedDb.newsDao().observePublic().first()
        assertEquals(2, allNews.size)
        assertEquals(listOf("Published News"), publicNews.map { it.title })
        assertTrue(allNews.all { it.imageUri == null && it.displayOrder == 0 })

        val announcement = migratedDb.announcementDao().observePublic().first().single()
        assertEquals("Holiday Notice", announcement.title)
        assertNull(announcement.imageUri)
        assertEquals(0, announcement.displayOrder)

        val album = migratedDb.galleryDao().observePublicAlbumsWithMedia().first().single().album
        assertEquals("Annual Day", album.title)
        assertEquals(0, album.displayOrder)

        // Verify newly added v4 columns are writable & queryable after migration
        migratedDb.schoolProfileDao().upsertProfile(
            profile.copy(
                profileImageUri = "file:///data/user/0/com.example/files/chevalier_media/originals/p.jpg",
                aboutImageUri = "file:///data/user/0/com.example/files/chevalier_media/adjusted/a.jpg"
            )
        )
        val updatedProfile = migratedDb.schoolProfileDao().getProfileOnce()!!
        assertTrue(updatedProfile.profileImageUri!!.endsWith("/p.jpg"))
        assertTrue(updatedProfile.aboutImageUri!!.endsWith("/a.jpg"))

        migratedDb.close()
        dbFile.delete()
    }
}
