package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.PinSecurityManager
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.SchoolRepository
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManagementCoreWorkflowTest {

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var schoolRepo: SchoolRepository
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        schoolRepo = SchoolRepository.fromDatabase(database)
        mgmtRepo = ManagementRepository(database, MediaStudioManager(context))
        prefsRepo = UserPreferencesRepository(context)
        backupManager = BackupRestoreManager(context, database, prefsRepo)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== A. AUTHENTICATION & SECURITY ====================

    @Test
    fun `master admin setup validates pin, hashes verifier, handles lockout and recovery`() = runTest {
        schoolRepo.ensureVerifiedProfileSeeded()

        // Short PIN (< 6) rejected
        assertTrue(mgmtRepo.setupMasterAdmin("Principal", "admin", "1234", "1234").isFailure)
        // Mismatched PIN rejected
        assertTrue(mgmtRepo.setupMasterAdmin("Principal", "admin", "123456", "654321").isFailure)

        // Valid Master Admin creation
        val setup = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        assertEquals(ManagementRole.MASTER_ADMIN, setup.account.account.role)
        assertNotEquals("246810", setup.account.account.pinVerifierBase64)
        assertFalse(setup.account.account.pinVerifierBase64.contains("246810"))
        assertTrue(setup.recoveryCode.isNotBlank())

        // Login fails on wrong PIN and triggers lockout after 3 attempts
        assertTrue(mgmtRepo.loginWithPin("admin", "000000").isFailure)
        assertTrue(mgmtRepo.loginWithPin("admin", "111111").isFailure)
        val thirdErr = mgmtRepo.loginWithPin("admin", "999999").exceptionOrNull()?.message.orEmpty()
        assertTrue(thirdErr.contains("locked", ignoreCase = true))

        // Locked account blocks even correct PIN until lockout clears or PIN is recovered
        val duringLock = mgmtRepo.loginWithPin("admin", "246810")
        assertTrue(duringLock.isFailure)

        // Recovery with wrong code fails cleanly
        assertTrue(mgmtRepo.recoverMasterAdminPin("WRONG-CODE", "135790", "135790").isFailure)

        // Recovery with issued one-time code resets PIN, clears lockout, and rotates recovery key
        val recovered = mgmtRepo.recoverMasterAdminPin(setup.recoveryCode, "135790", "135790").getOrThrow()
        assertNotEquals(setup.recoveryCode, recovered.recoveryCode)

        // New PIN authenticates cleanly; old recovery code no longer works
        assertTrue(mgmtRepo.loginWithPin("admin", "135790").isSuccess)
        assertTrue(mgmtRepo.recoverMasterAdminPin(setup.recoveryCode, "112233", "112233").isFailure)
    }

    @Test
    fun `deactivated staff rejected and viewmodel inactivity timer locks session`() = runTest {
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
        val staff = mgmtRepo.createStaffAccount(
            actor = master,
            username = "clerk",
            displayName = "Office Clerk",
            role = ManagementRole.STAFF,
            initialPin = "654321",
            permissions = listOf(
                StaffPermissionEntity(accountId = 0, sectionKey = ManagementSection.NEWS.key, canView = true)
            )
        ).getOrThrow()

        assertTrue(mgmtRepo.loginWithPin("clerk", "654321").isSuccess)

        // Deactivate staff -> login must fail with SecurityException
        mgmtRepo.updateStaffAccount(
            actor = master,
            targetAccountId = staff.account.id,
            displayName = "Office Clerk",
            isActive = false,
            newPinOrNull = null,
            permissions = staff.permissions
        ).getOrThrow()
        val inactiveLogin = mgmtRepo.loginWithPin("clerk", "654321")
        assertTrue(inactiveLogin.isFailure)
        assertTrue(inactiveLogin.exceptionOrNull() is SecurityException)

        // Test ViewModel inactivity auto-lock deterministically
        val store = androidx.lifecycle.ViewModelStore()
        try {
            val vm = androidx.lifecycle.ViewModelProvider(
                store,
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ManagementViewModel(mgmtRepo, backupManager, inactivityTimeoutMillis = 60_000L) as T
                    }
                }
            )[ManagementViewModel::class.java]
            val t0 = System.currentTimeMillis()
            vm.loginWithPin("admin", "246810")
            var spins = 0
            while (vm.authUiState.value !is ManagementAuthUiState.Authenticated && spins < 100) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                Thread.sleep(25)
                spins++
            }
            assertTrue(vm.authUiState.value is ManagementAuthUiState.Authenticated)
            vm.onUserInteraction(t0)
            assertFalse(vm.checkInactivityLock(t0 + 30_000L))
            assertTrue(vm.checkInactivityLock(t0 + 61_000L))
            assertTrue(vm.authUiState.value is ManagementAuthUiState.LoginRequired)
        } finally {
            store.clear()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    // ==================== B. PERMISSIONS, C. PUBLISHING, D. VERSIONING, E. LOGS ====================

    @Test
    fun `granular permissions, hybrid approval keeps live version intact, version restore preserves history, and logs are filtered`() = runTest {
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
        val editorPerms = listOf(
            StaffPermissionEntity(
                accountId = 0,
                sectionKey = ManagementSection.NEWS.key,
                canView = true,
                canCreate = true,
                canEdit = true,
                canDelete = false,
                canPublish = false
            )
        )
        val staff = mgmtRepo.createStaffAccount(
            actor = master,
            username = "editor1",
            displayName = "News Editor",
            role = ManagementRole.STAFF,
            initialPin = "112233",
            permissions = editorPerms
        ).getOrThrow()

        // Staff attempting unauthorized direct publish -> SecurityException
        val directPublishAttempt = mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.NEWS,
            recordId = 0L,
            recordTitle = "Unauthorized",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(title = "Unauthorized", summary = "S", fullBody = "B", publishedDateText = "12 Nov")
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Try publish"
        )
        assertTrue(directPublishAttempt.exceptionOrNull() is SecurityException)

        // Staff attempting Facilities create without permission -> SecurityException
        val unpermittedSection = mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.FACILITIES,
            recordId = 0L,
            recordTitle = "Lab",
            proposedJson = "{}",
            saveMode = PublishSaveMode.SAVE_DRAFT,
            changeSummary = "Try"
        )
        assertTrue(unpermittedSection.exceptionOrNull() is SecurityException)

        // Master Admin publishes v1 News article
        val v1Id = mgmtRepo.saveManagedContentRecord(
            actor = master,
            section = ManagementSection.NEWS,
            recordId = 0L,
            recordTitle = "Annual Sports Day",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(
                    title = "Annual Sports Day",
                    summary = "Verified v1 summary",
                    fullBody = "Verified v1 body",
                    publishedDateText = "12 Nov 2026"
                )
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Initial publish"
        ).getOrThrow()

        assertEquals(1, schoolRepo.newsItems.first().size)
        assertEquals("Verified v1 summary", schoolRepo.newsItems.first().first().summary)

        // Staff edits ALREADY PUBLISHED News article & submits for approval -> live public version MUST stay "Verified v1 summary"!
        mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.NEWS,
            recordId = v1Id,
            recordTitle = "Annual Sports Day Updated",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(
                    id = v1Id,
                    title = "Annual Sports Day Updated",
                    summary = "Proposed v2 summary",
                    fullBody = "Proposed v2 body",
                    publishedDateText = "13 Nov 2026"
                )
            ),
            saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
            changeSummary = "Updated date and summary"
        ).getOrThrow()

        // Verify live public view STILL shows v1 untouched
        val publicWhilePending = schoolRepo.newsItems.first()
        assertEquals(1, publicWhilePending.size)
        assertEquals("Annual Sports Day", publicWhilePending.first().title)
        assertEquals("Verified v1 summary", publicWhilePending.first().summary)

        // Verify Approval queue has 1 PENDING_APPROVAL item
        val pending = mgmtRepo.pendingApprovals.first()
        assertEquals(1, pending.size)
        val approvalId = pending.first().id

        // Rejection requires non-blank reason
        assertTrue(mgmtRepo.rejectSubmission(master, approvalId, "   ").isFailure)

        // Staff submits a new Draft/Pending article and Master Admin rejects it with reason -> hidden from public
        val draftId = mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.NEWS,
            recordId = 0L,
            recordTitle = "Unverified Rumor",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(title = "Unverified Rumor", summary = "S", fullBody = "B", publishedDateText = "12 Nov")
            ),
            saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
            changeSummary = "Draft item"
        ).getOrThrow()
        val rumorApproval = mgmtRepo.pendingApprovals.first().first { it.targetRecordId == draftId }
        mgmtRepo.rejectSubmission(master, rumorApproval.id, "Unverified details").getOrThrow()
        assertEquals(1, schoolRepo.newsItems.first().size) // still only v1

        // Master Admin approves v2 revision -> now public updates to "Annual Sports Day Updated"
        mgmtRepo.approveSubmission(master, approvalId, publishImmediately = true).getOrThrow()
        assertEquals("Annual Sports Day Updated", schoolRepo.newsItems.first().first().title)

        // Version History has v1 (PUBLISHED), v2 (PENDING_APPROVAL), v3 (PUBLISHED); restoring v1 creates v4 without deleting history!
        val newsVersions = database.versionHistoryDao().observeForRecord(ManagementSection.NEWS.key, v1Id).first()
        assertEquals(3, newsVersions.size)
        val v1Entry = newsVersions.last()
        assertEquals(1, v1Entry.versionNumber)
        mgmtRepo.restoreVersion(master, v1Entry.id, publishImmediately = true).getOrThrow()
        val afterRestoreVersions = database.versionHistoryDao().observeForRecord(ManagementSection.NEWS.key, v1Id).first()
        assertEquals(4, afterRestoreVersions.size)
        assertEquals("Annual Sports Day", schoolRepo.newsItems.first().first().title)

        // Unpublish immediately hides from public
        mgmtRepo.unpublishRecord(master, ManagementSection.NEWS, v1Id).getOrThrow()
        assertTrue(schoolRepo.newsItems.first().isEmpty())

        // Master Admin sees all logs (including STAFF_CREATED, BACKUP/STAFF); Staff does not see unpermitted BACKUP_RESTORE or Master-only STAFF logs by Master
        val masterLogs = mgmtRepo.observeVisibleActivityLogs(master).first()
        val staffLogs = mgmtRepo.observeVisibleActivityLogs(staff).first()
        assertTrue(masterLogs.size > staffLogs.size)
        assertTrue(staffLogs.all { it.sectionKey == ManagementSection.NEWS.key })

        // Permission revocation blocks subsequent edits by staff
        mgmtRepo.updateStaffAccount(
            actor = master,
            targetAccountId = staff.account.id,
            displayName = staff.account.displayName,
            isActive = true,
            newPinOrNull = null,
            permissions = emptyList()
        ).getOrThrow()
        val updatedStaff = database.managementAccountDao().findWithPermissionsById(staff.account.id)!!
        assertFalse(mgmtRepo.hasPermission(updatedStaff, ManagementSection.NEWS, PermissionAction.EDIT))
    }

    @Test
    fun `deleting news record or gallery album closes pending approvals`() = runTest {
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
        val editorPerms = listOf(
            StaffPermissionEntity(
                accountId = 0,
                sectionKey = ManagementSection.NEWS.key,
                canView = true,
                canCreate = true,
                canEdit = true
            ),
            StaffPermissionEntity(
                accountId = 0,
                sectionKey = ManagementSection.PHOTO_GALLERY.key,
                canView = true,
                canCreate = true,
                canEdit = true
            )
        )
        val staff = mgmtRepo.createStaffAccount(
            actor = master,
            username = "content_staff",
            displayName = "Content Staff",
            role = ManagementRole.STAFF,
            initialPin = "112233",
            permissions = editorPerms
        ).getOrThrow()

        val newsId = mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.NEWS,
            recordId = 0L,
            recordTitle = "Pending News",
            proposedJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(title = "Pending News", summary = "S", fullBody = "B", publishedDateText = "12 Nov")
            ),
            saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
            changeSummary = "Submit news"
        ).getOrThrow()

        val albumId = mgmtRepo.saveManagedContentRecord(
            actor = master,
            section = ManagementSection.PHOTO_GALLERY,
            recordId = 0L,
            recordTitle = "Campus Album",
            proposedJson = ContentSnapshotSerializer.galleryAlbumToJson(
                com.example.data.local.entity.GalleryAlbumEntity(title = "Campus Album", category = "Events")
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Publish album"
        ).getOrThrow()

        val mediaId = mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.PHOTO_GALLERY,
            recordId = 0L,
            recordTitle = "Photo 1",
            proposedJson = ContentSnapshotSerializer.galleryMediaToJson(
                com.example.data.local.entity.GalleryMediaEntity(
                    albumId = albumId,
                    mediaType = "PHOTO",
                    mediaUri = "file:///tmp/photo1.jpg",
                    caption = "Photo 1"
                )
            ),
            saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
            changeSummary = "Submit photo"
        ).getOrThrow()

        assertTrue(mgmtRepo.pendingApprovals.first().any { it.sectionKey == ManagementSection.NEWS.key && it.targetRecordId == newsId })
        assertTrue(mgmtRepo.pendingApprovals.first().any { it.sectionKey == ManagementSection.PHOTO_GALLERY.key && it.targetRecordId == mediaId })

        mgmtRepo.deleteRecord(master, ManagementSection.NEWS, newsId, "Pending News").getOrThrow()
        mgmtRepo.deleteRecord(master, ManagementSection.PHOTO_GALLERY, albumId, "Campus Album", isGalleryMediaItem = false).getOrThrow()

        val remainingPending = mgmtRepo.pendingApprovals.first()
        assertFalse(remainingPending.any { it.sectionKey == ManagementSection.NEWS.key && it.targetRecordId == newsId })
        assertFalse(remainingPending.any { it.sectionKey == ManagementSection.PHOTO_GALLERY.key && it.targetRecordId == mediaId })
    }
}
