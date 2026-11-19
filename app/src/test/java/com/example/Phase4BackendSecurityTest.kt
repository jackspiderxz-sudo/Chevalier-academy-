package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
