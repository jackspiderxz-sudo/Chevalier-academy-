package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.AppLanguage
import com.example.core.model.SchoolTenantContext
import com.example.core.notifications.LocalAlertNotifier
import com.example.data.backend.BackendConfigDetector
import com.example.data.backend.IntegrationState
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.SchoolRepository
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase4BackendSecurityTest {

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var schoolRepo: SchoolRepository
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var notifier: LocalAlertNotifier
    private lateinit var backupManager: BackupRestoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        schoolRepo = SchoolRepository.fromDatabase(database)
        prefsRepo = UserPreferencesRepository(context)
        notifier = LocalAlertNotifier(context)
        mgmtRepo = ManagementRepository(
            database = database,
            mediaStudioManager = MediaStudioManager(context),
            preferencesRepository = prefsRepo,
            localAlertNotifier = notifier
        )
        backupManager = BackupRestoreManager(context, database, prefsRepo)
    }

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "chevalier_media").deleteRecursively()
    }

    @Test
    fun `rbac filters visible approvals, versions, notifications, and enforces notification actions`() = runBlocking {
        schoolRepo.ensureVerifiedProfileSeeded()
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account

        // Create two staff members:
        // 1) newsViewer: only NEWS VIEW and NOTIFICATIONS VIEW (cannot markAllRead or clearAll)
        val newsViewer = mgmtRepo.createStaffAccount(
            actor = master,
            username = "newsviewer",
            displayName = "News Viewer",
            role = ManagementRole.STAFF,
            initialPin = "112233",
            permissions = listOf(
                StaffPermissionEntity(accountId = 0, sectionKey = ManagementSection.NEWS.key, canView = true),
                StaffPermissionEntity(accountId = 0, sectionKey = ManagementSection.NOTIFICATIONS.key, canView = true)
            )
        ).getOrThrow()

        // 2) silentStaff: only FACILITIES VIEW (no NEWS, no NOTIFICATIONS)
        val silentStaff = mgmtRepo.createStaffAccount(
            actor = master,
            username = "facviewer",
            displayName = "Facilities Staff",
            role = ManagementRole.STAFF,
            initialPin = "445566",
            permissions = listOf(
                StaffPermissionEntity(accountId = 0, sectionKey = ManagementSection.FACILITIES.key, canView = true)
            )
        ).getOrThrow()

        // Trigger a NEWS publish + a BACKUP_CREATED event + a STAFF event
        mgmtRepo.saveManagedContentRecord(
            actor = master,
            section = ManagementSection.NEWS,
            recordId = 0L,
            recordTitle = "Annual Day",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(title = "Annual Day", summary = "Celebration", fullBody = "Full details", publishedDateText = "2026-11-16")
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Publish Annual Day news"
        ).getOrThrow()

        val backupBytes = backupManager.createEncryptedBackupBytes("Secret#123").getOrThrow()
        val backupPreview = backupManager.inspectEncryptedBackup(backupBytes, "Secret#123").getOrThrow()
        mgmtRepo.recordBackupCreated(master, backupPreview)

        // Verify newsViewer sees NEWS notifications & version history, NOT STAFF_MANAGEMENT or BACKUP_RESTORE
        val viewerNotifications = mgmtRepo.observeVisibleNotifications(newsViewer).first()
        assertTrue(viewerNotifications.any { it.sectionKey == ManagementSection.NEWS.key })
        assertFalse(viewerNotifications.any { it.sectionKey == ManagementSection.STAFF_MANAGEMENT.key })
        assertFalse(viewerNotifications.any { it.sectionKey == ManagementSection.BACKUP_RESTORE.key })

        val viewerVersions = mgmtRepo.observeVisibleVersionHistory(newsViewer).first()
        assertTrue(viewerVersions.any { it.sectionKey == ManagementSection.NEWS.key })
        assertFalse(viewerVersions.any { it.sectionKey == ManagementSection.STAFF_MANAGEMENT.key })

        // silentStaff has NO NOTIFICATIONS VIEW -> sees zero notifications and zero unread count
        assertTrue(mgmtRepo.observeVisibleNotifications(silentStaff).first().isEmpty())
        assertEquals(0, mgmtRepo.observeVisibleUnreadNotificationsCount(silentStaff).first())

        // Enforce RBAC on notification mutations:
        // silentStaff cannot even mark a single notification read
        val firstNoteId = viewerNotifications.first().id
        assertTrue(mgmtRepo.markNotificationRead(silentStaff, firstNoteId).exceptionOrNull() is SecurityException)
        // newsViewer has VIEW only -> can mark single read, but markAllRead & clearNotifications throw SecurityException
        assertTrue(mgmtRepo.markNotificationRead(newsViewer, firstNoteId).isSuccess)
        assertTrue(mgmtRepo.markAllNotificationsRead(newsViewer).exceptionOrNull() is SecurityException)
        assertTrue(mgmtRepo.clearNotifications(newsViewer).exceptionOrNull() is SecurityException)

        // Master Admin CAN clear notifications, and NOTIFICATIONS_CLEARED audit log is recorded
        assertTrue(mgmtRepo.clearNotifications(master).isSuccess)
        assertTrue(mgmtRepo.allNotifications.first().isEmpty())
        val logs = mgmtRepo.observeVisibleActivityLogs(master).first()
        assertTrue(logs.any { it.actionType == "NOTIFICATIONS_CLEARED" && it.actorRole == ManagementRole.MASTER_ADMIN })
    }

    @Test
    fun `viewmodel clears governance streams on logout and audits backup failures safely`() {
        runBlocking {
            schoolRepo.ensureVerifiedProfileSeeded()
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        }

        val status = BackendConfigDetector.inspect(context)
        val store = ViewModelStore()
        val collectorScope = CoroutineScope(Dispatchers.Main.immediate + Job())
        val exportFile = File(context.cacheDir, "phase4_export.chvbak")
        try {
            val vm = ViewModelProvider(
                store,
                ManagementViewModel.Factory(
                    managementRepository = mgmtRepo,
                    backupRestoreManager = backupManager,
                    preferencesRepository = prefsRepo,
                    centralBackendStatus = status
                )
            )[ManagementViewModel::class.java]

            // Keep StateFlows subscribed just like active Compose UI
            collectorScope.launch { vm.visibleActivityLogs.collect {} }
            collectorScope.launch { vm.allNotifications.collect {} }

            vm.loginWithPin("admin", "246810")
            awaitMainCondition { vm.authUiState.value is ManagementAuthUiState.Authenticated }

            // Create and verify invalid backup URI audit logging without leaking secrets
            val badUri = Uri.fromFile(File(context.cacheDir, "nonexistent_phase4_backup.chvbak"))
            vm.inspectBackupUri(badUri, "TopSecretPass#99")
            awaitMainCondition { vm.errorMessage.value != null && !vm.isBusy.value }

            val session = vm.currentSession()!!
            val logsBeforeLogout = runBlocking { mgmtRepo.observeVisibleActivityLogs(session).first() }
            val failLog = logsBeforeLogout.firstOrNull { it.actionType == "BACKUP_INSPECT_FAILED" }
            assertNotNull(failLog)
            assertFalse(failLog!!.details.contains("TopSecretPass"))

            // Perform valid backup export to file URI -> BACKUP_CREATED audited
            vm.createBackupToUri(Uri.fromFile(exportFile), "ValidPass1", "ValidPass1")
            awaitMainCondition {
                vm.statusMessage.value?.contains("Encrypted backup saved") == true && !vm.isBusy.value
            }
            assertTrue(exportFile.exists())

            awaitMainCondition {
                vm.visibleActivityLogs.value.any { it.actionType == "BACKUP_CREATED" } &&
                    vm.allNotifications.value.isNotEmpty()
            }
            assertFalse(vm.visibleActivityLogs.value.any { it.details.contains("ValidPass1") })

            // Verify streams clear immediately on logout
            vm.logout()
            awaitMainCondition { vm.authUiState.value is ManagementAuthUiState.LoginRequired }
            awaitMainCondition {
                vm.visibleActivityLogs.value.isEmpty() && vm.allNotifications.value.isEmpty()
            }
        } finally {
            collectorScope.cancel()
            store.clear()
            exportFile.delete()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `backup snapshot integrity check detects tampering and records accurate actorRole on restore`() = runBlocking {
        schoolRepo.ensureVerifiedProfileSeeded()
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
        val superOwner = mgmtRepo.createStaffAccount(
            actor = master,
            username = "vp",
            displayName = "Vice Principal",
            role = ManagementRole.SUPER_OWNER,
            initialPin = "778899",
            permissions = ManagementSection.entries.map {
                StaffPermissionEntity(accountId = 0, sectionKey = it.key, canView = true, canCreate = true, canEdit = true, canDelete = true, canPublish = true)
            }
        ).getOrThrow()

        val encryptedBytes = backupManager.createEncryptedBackupBytes("ArchivePass123").getOrThrow()
        val previewInfo = backupManager.inspectEncryptedBackup(encryptedBytes, "ArchivePass123").getOrThrow()
        assertEquals(SchoolTenantContext.DEFAULT_SCHOOL_ID, previewInfo.schoolId)
        assertFalse(previewInfo.snapshotSha256.isNullOrBlank())

        // Decrypt inner zip, tamper with database_snapshot.json while keeping old manifest digest, re-encrypt with same key
        val tamperedArchive = tamperSnapshotInEncryptedArchive(encryptedBytes, "ArchivePass123")
        val inspectTampered = backupManager.inspectEncryptedBackup(tamperedArchive, "ArchivePass123")
        assertTrue(inspectTampered.isFailure)
        assertTrue(
            inspectTampered.exceptionOrNull()?.message?.contains("integrity", ignoreCase = true) == true
        )

        // Valid archive restores cleanly and logs Super Owner's actual role on BACKUP_RESTORED
        val restored = backupManager.restoreEncryptedBackupBytes(
            bytes = encryptedBytes,
            password = "ArchivePass123",
            actorName = superOwner.account.displayName,
            actorAccountId = superOwner.account.id,
            actorRole = superOwner.account.role
        )
        assertTrue(restored.isSuccess)
        val restoreLog = database.activityLogDao().observeAll().first()
            .firstOrNull { it.actionType == "BACKUP_RESTORED" }
        assertNotNull(restoreLog)
        assertEquals(ManagementRole.SUPER_OWNER, restoreLog!!.actorRole)
    }

    @Test
    fun `zip slip traversal archive entry is rejected`() = runBlocking {
        val maliciousZip = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(ZipEntry("../../etc/passwd"))
                zos.write("bad".toByteArray())
                zos.closeEntry()
            }
            baos.toByteArray()
        }
        val encryptedMalicious = encryptZipBytesForTest(maliciousZip, "ArchivePass123")
        val result = backupManager.inspectEncryptedBackup(encryptedMalicious, "ArchivePass123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid entry path inside backup archive") == true)
    }

    @Test
    fun `backend status reports honest local vs unconfigured state, allowBackup false, and alerts preference`() = runBlocking {
        val status = BackendConfigDetector.inspect(context)
        assertEquals(SchoolTenantContext.DEFAULT_SCHOOL_ID, status.tenantId)
        assertEquals(IntegrationState.ACTIVE_LOCAL, status.remoteAuth.state)
        assertEquals(IntegrationState.UNCONFIGURED, status.remoteCloudSync.state)
        assertEquals(IntegrationState.UNCONFIGURED, status.pushMessaging.state)
        assertFalse(status.isRemoteSyncConfigured)

        // Manifest allowBackup="false" verification
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)

        // Opt-in notification alerts preference default false -> toggle true -> persists
        assertFalse(prefsRepo.preferencesFlow.first().deviceAlertsEnabled)
        prefsRepo.setDeviceAlertsEnabled(true)
        assertTrue(prefsRepo.preferencesFlow.first().deviceAlertsEnabled)

        // LocalAlertNotifier returns false cleanly when alertsEnabled=false
        assertFalse(notifier.notifyLocalEventIfPermitted(101, "Test", "Body", alertsEnabled = false))
    }

    @Test
    fun `restore rejects backups without active master admin and preserves existing db media and preferences`() = runBlocking {
        schoolRepo.ensureVerifiedProfileSeeded()
        prefsRepo.setLanguage(AppLanguage.TAMIL, markSelectedOnLaunch = true)
        val master = mgmtRepo.setupMasterAdmin("Initial Principal", "admin_orig", "246810", "246810").getOrThrow().account
        mgmtRepo.createStaffAccount(
            actor = master,
            username = "staff_only",
            displayName = "Staff User",
            role = ManagementRole.STAFF,
            initialPin = "112233",
            permissions = listOf(
                StaffPermissionEntity(accountId = 0, sectionKey = ManagementSection.NEWS.key, canView = true)
            )
        ).getOrThrow()
        mgmtRepo.saveManagedContentRecord(
            actor = master,
            section = ManagementSection.NEWS,
            recordId = 0L,
            recordTitle = "Original Live News",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(title = "Original Live News", summary = "Original Summary", fullBody = "Body", publishedDateText = "2026-11-20")
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Seed live news"
        ).getOrThrow()

        val liveMediaDir = File(context.filesDir, "chevalier_media").apply { mkdirs() }
        val sentinelMedia = File(liveMediaDir, "sentinel_photo.txt").apply { writeText("live-media-bytes") }

        val validArchive = backupManager.createEncryptedBackupBytes("ArchivePass123").getOrThrow()

        // Variant A: empty managementAccounts array (no MASTER_ADMIN)
        val emptyAccountsArchive = rewriteSnapshotWithValidDigest(validArchive, "ArchivePass123") { snapshot ->
            snapshot.put("managementAccounts", JSONArray())
        }
        // Variant B: only STAFF account (no MASTER_ADMIN)
        val staffOnlyArchive = rewriteSnapshotWithValidDigest(validArchive, "ArchivePass123") { snapshot ->
            val accounts = snapshot.getJSONArray("managementAccounts")
            val filtered = JSONArray()
            for (i in 0 until accounts.length()) {
                val acc = accounts.getJSONObject(i)
                if (acc.getString("role") != ManagementRole.MASTER_ADMIN) {
                    filtered.put(acc)
                }
            }
            snapshot.put("managementAccounts", filtered)
        }
        // Variant C: MASTER_ADMIN present but inactive (isActive = false)
        val inactiveMasterArchive = rewriteSnapshotWithValidDigest(validArchive, "ArchivePass123") { snapshot ->
            val accounts = snapshot.getJSONArray("managementAccounts")
            for (i in 0 until accounts.length()) {
                val acc = accounts.getJSONObject(i)
                if (acc.getString("role") == ManagementRole.MASTER_ADMIN) {
                    acc.put("isActive", false)
                }
            }
        }

        for (badArchive in listOf(emptyAccountsArchive, staffOnlyArchive, inactiveMasterArchive)) {
            val inspectRes = backupManager.inspectEncryptedBackup(badArchive, "ArchivePass123")
            assertTrue("Expected inspectEncryptedBackup to reject invalid governance archive", inspectRes.isFailure)
            assertTrue(
                inspectRes.exceptionOrNull()?.message?.contains("active Master Admin", ignoreCase = true) == true
            )

            val restoreRes = backupManager.restoreEncryptedBackupBytes(
                bytes = badArchive,
                password = "ArchivePass123",
                actorName = master.account.displayName,
                actorAccountId = master.account.id,
                actorRole = master.account.role
            )
            assertTrue("Expected restoreEncryptedBackupBytes to reject invalid governance archive", restoreRes.isFailure)
            assertTrue(
                restoreRes.exceptionOrNull()?.message?.contains("active Master Admin", ignoreCase = true) == true
            )
        }

        // Verify existing DB, media files, and DataStore preferences were NOT modified
        val accountsAfter = database.managementAccountDao().getAllAccountsWithPermissionsOnce()
        assertEquals(2, accountsAfter.size)
        assertTrue(accountsAfter.any { it.account.username == "admin_orig" && it.account.isActive })
        val newsAfter = database.newsDao().observeAll().first()
        assertEquals(1, newsAfter.size)
        assertEquals("Original Live News", newsAfter.single().title)
        assertTrue(sentinelMedia.exists())
        assertEquals("live-media-bytes", sentinelMedia.readText())
        assertEquals(AppLanguage.TAMIL, prefsRepo.preferencesFlow.first().selectedLanguage)
    }

    @Test
    fun `viewmodel invalidates session on restore even with matching numeric id and prevents conflicting auth states`() {
        val backupFile = File(context.cacheDir, "bug03_valid_restore.chvbak")
        val invalidBackupFile = File(context.cacheDir, "bug03_invalid_restore.chvbak")
        val restoredMasterId: Long
        runBlocking {
            schoolRepo.ensureVerifiedProfileSeeded()
            prefsRepo.setLanguage(AppLanguage.HINDI, markSelectedOnLaunch = true)
            val restoredMaster = mgmtRepo.setupMasterAdmin("Restored Principal", "restored_admin", "778899", "778899").getOrThrow().account
            restoredMasterId = restoredMaster.account.id
            mgmtRepo.saveManagedContentRecord(
                actor = restoredMaster,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = "Restored Campus Notice",
                proposedJson = ContentSnapshotSerializer.newsToJson(
                    NewsItemEntity(title = "Restored Campus Notice", summary = "Restored", fullBody = "Restored body", publishedDateText = "2026-11-20")
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Add restored notice"
            ).getOrThrow()

            val validBytes = backupManager.createEncryptedBackupBytes("BackupPass123").getOrThrow()
            backupFile.writeBytes(validBytes)
            val invalidBytes = rewriteSnapshotWithValidDigest(validBytes, "BackupPass123") { snapshot ->
                val accounts = snapshot.getJSONArray("managementAccounts")
                for (i in 0 until accounts.length()) {
                    val acc = accounts.getJSONObject(i)
                    if (acc.getString("role") == ManagementRole.MASTER_ADMIN) {
                        acc.put("isActive", false)
                    }
                }
            }
            invalidBackupFile.writeBytes(invalidBytes)

            // Reset DB and sequence so active pre-restore user shares the exact same numeric account.id
            database.clearAllTables()
            database.openHelper.writableDatabase.execSQL("DELETE FROM sqlite_sequence")
            prefsRepo.setLanguage(AppLanguage.ENGLISH, markSelectedOnLaunch = true)
            val oldMaster = mgmtRepo.setupMasterAdmin("Previous Principal", "old_admin", "246810", "246810").getOrThrow().account
            assertEquals(restoredMasterId, oldMaster.account.id)
            mgmtRepo.saveManagedContentRecord(
                actor = oldMaster,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = "Pre-Restore Local News",
                proposedJson = ContentSnapshotSerializer.newsToJson(
                    NewsItemEntity(title = "Pre-Restore Local News", summary = "Local", fullBody = "Local body", publishedDateText = "2026-11-20")
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Seed local news"
            ).getOrThrow()
        }

        val store = ViewModelStore()
        val collectorScope = CoroutineScope(Dispatchers.Main.immediate + Job())
        val observedAuthStates = mutableListOf<ManagementAuthUiState>()
        try {
            val vm = ViewModelProvider(
                store,
                ManagementViewModel.Factory(
                    managementRepository = mgmtRepo,
                    backupRestoreManager = backupManager,
                    preferencesRepository = prefsRepo
                )
            )[ManagementViewModel::class.java]

            collectorScope.launch {
                vm.authUiState.collect { observedAuthStates.add(it) }
            }

            vm.loginWithPin("old_admin", "246810")
            awaitMainCondition {
                (vm.authUiState.value as? ManagementAuthUiState.Authenticated)?.session?.account?.username == "old_admin"
            }

            // Failed restore of invalid backup preserves current Authenticated session & existing data
            vm.confirmRestoreFromUri(Uri.fromFile(invalidBackupFile), "BackupPass123")
            awaitMainCondition { !vm.isBusy.value && vm.errorMessage.value != null }
            assertEquals(
                "old_admin",
                (vm.authUiState.value as? ManagementAuthUiState.Authenticated)?.session?.account?.username
            )
            assertEquals(
                "Pre-Restore Local News",
                runBlocking { database.newsDao().observeAll().first().single().title }
            )

            // Populate transient search & preview state before valid restore
            vm.performGlobalSearch("Local")
            awaitMainCondition { vm.searchResults.value.isNotEmpty() }
            vm.inspectBackupUri(Uri.fromFile(backupFile), "BackupPass123")
            awaitMainCondition { !vm.isBusy.value && vm.backupPreview.value != null }

            observedAuthStates.clear()
            vm.confirmRestoreFromUri(Uri.fromFile(backupFile), "BackupPass123")
            awaitMainCondition {
                !vm.isBusy.value && vm.authUiState.value is ManagementAuthUiState.LoginRequired
            }
            // Allow any pending Room invalidations to settle cleanly
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(100L)
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            // Must transition cleanly to LoginRequired with notice; NEVER keep old session or bind new account by ID
            val finalState = vm.authUiState.value as? ManagementAuthUiState.LoginRequired
            assertNotNull("Expected LoginRequired after restore", finalState)
            assertTrue(finalState!!.noticeMessage?.contains("sign in again", ignoreCase = true) == true)
            assertNull(vm.backupPreview.value)
            assertTrue(vm.searchResults.value.isEmpty())
            assertNull(vm.errorMessage.value)

            // Assert zero conflicting transitions during/after restore
            assertFalse(
                "Must never emit FirstTimeSetup during/after valid restore",
                observedAuthStates.any { it is ManagementAuthUiState.FirstTimeSetup }
            )
            assertFalse(
                "Must never emit Authenticated(restored_admin) automatically after restore",
                observedAuthStates.any {
                    it is ManagementAuthUiState.Authenticated && it.session.account.username == "restored_admin"
                }
            )
            val loginStates = observedAuthStates.filterIsInstance<ManagementAuthUiState.LoginRequired>()
            assertEquals(1, loginStates.size)

            // Old overwritten credentials must be rejected; restored credentials must succeed
            vm.loginWithPin("old_admin", "246810")
            awaitMainCondition { !vm.isBusy.value && vm.errorMessage.value != null }
            assertTrue(vm.authUiState.value is ManagementAuthUiState.LoginRequired)

            vm.loginWithPin("restored_admin", "778899")
            awaitMainCondition {
                !vm.isBusy.value &&
                    (vm.authUiState.value as? ManagementAuthUiState.Authenticated)?.session?.account?.username == "restored_admin"
            }
            assertEquals(
                "Restored Campus Notice",
                runBlocking { database.newsDao().observeAll().first().single().title }
            )
            assertEquals(AppLanguage.HINDI, runBlocking { prefsRepo.preferencesFlow.first().selectedLanguage })
        } finally {
            collectorScope.cancel()
            store.clear()
            backupFile.delete()
            invalidBackupFile.delete()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun awaitMainCondition(timeoutMs: Long = 6_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(25L)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Condition timed out after ${timeoutMs}ms", condition())
    }

    private fun rewriteSnapshotWithValidDigest(
        encryptedBytes: ByteArray,
        password: String,
        mutator: (JSONObject) -> Unit
    ): ByteArray {
        val buf = ByteBuffer.wrap(encryptedBytes)
        buf.position(12)
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
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }
        val snapshotJson = JSONObject(String(entries.getValue("database_snapshot.json"), Charsets.UTF_8))
        mutator(snapshotJson)
        val mutatedSnapshotStr = snapshotJson.toString()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(mutatedSnapshotStr.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val manifestJson = JSONObject(String(entries.getValue("manifest.json"), Charsets.UTF_8))
        manifestJson.put("snapshotSha256", digest)
        entries["database_snapshot.json"] = mutatedSnapshotStr.toByteArray(Charsets.UTF_8)
        entries["manifest.json"] = manifestJson.toString().toByteArray(Charsets.UTF_8)

        val updatedZip = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                entries.forEach { (name, bytes) ->
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            baos.toByteArray()
        }
        return encryptZipBytesForTest(updatedZip, password)
    }

    private fun tamperSnapshotInEncryptedArchive(encryptedBytes: ByteArray, password: String): ByteArray {
        val buf = ByteBuffer.wrap(encryptedBytes)
        buf.position(12) // skip header(8) + formatVersion(4)
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
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }
        val originalSnapshot = JSONObject(String(entries.getValue("database_snapshot.json"), Charsets.UTF_8))
        originalSnapshot.put("tamperedField", "unauthorized")
        entries["database_snapshot.json"] = originalSnapshot.toString().toByteArray(Charsets.UTF_8)

        val tamperedZip = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                entries.forEach { (name, bytes) ->
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            baos.toByteArray()
        }
        return encryptZipBytesForTest(tamperedZip, password)
    }

    private fun encryptZipBytesForTest(zipBytes: ByteArray, password: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
        keySpec.clearPassword()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(zipBytes)
        return ByteBuffer.allocate(8 + 4 + 16 + 12 + encrypted.size)
            .put("CHV_BAK1".toByteArray(Charsets.US_ASCII))
            .putInt(1)
            .put(salt)
            .put(iv)
            .put(encrypted)
            .array()
    }
}
