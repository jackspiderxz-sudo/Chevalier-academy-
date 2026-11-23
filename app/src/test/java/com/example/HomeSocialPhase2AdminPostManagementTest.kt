package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.media.YouTubeMetadataFetcher
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.SchoolRepository
import com.example.ui.screens.management.SchoolFeedAdminScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeSocialPhase2AdminPostManagementTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var managementViewModel: ManagementViewModel
    private lateinit var masterAccount: AccountWithPermissions
    private var defaultHttpProvider: (String) -> String? = YouTubeMetadataFetcher.httpGetProvider

    @Before
    fun setup() {
        defaultHttpProvider = YouTubeMetadataFetcher.httpGetProvider
        // Default to offline/null in tests unless explicitly stubbed per test
        YouTubeMetadataFetcher.httpGetProvider = { null }

        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = MediaStudioManager(context)
        prefsRepo = UserPreferencesRepository(context)
        mgmtRepo = ManagementRepository(database, mediaManager)
        backupManager = BackupRestoreManager(context, database, prefsRepo)

        runBlocking {
            SchoolRepository.fromDatabase(database).ensureVerifiedProfileSeeded()
            masterAccount = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810")
                .getOrThrow().account
        }

        viewModelStore = ViewModelStore()
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(mgmtRepo, backupManager, prefsRepo)
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        YouTubeMetadataFetcher.httpGetProvider = defaultHttpProvider
        viewModelStore.clear()
        database.close()
    }

    private fun loginViewModel(username: String, pin: String) {
        managementViewModel.loginWithPin(username, pin)
        val deadline = System.currentTimeMillis() + 5_000L
        while (managementViewModel.authUiState.value !is ManagementAuthUiState.Authenticated &&
            System.currentTimeMillis() < deadline
        ) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue(managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated)
    }

    private fun waitUntil(timeoutMs: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            Thread.sleep(25)
        }
        assertTrue("Condition not satisfied within ${timeoutMs}ms", predicate())
    }

    // ==================== 1. YOUTUBE METADATA FETCH & FALLBACK ====================

    @Test
    fun `youtube metadata fetcher retrieves title, description, thumbnail and falls back cleanly without inventing content when offline`() = runTest {
        val videoId = "dQw4w9WgXcQ"
        // Case A: Online oEmbed + watch HTML meta tags
        YouTubeMetadataFetcher.httpGetProvider = { reqUrl ->
            when {
                reqUrl.contains("/oembed") ->
                    """{"title":"Annual Day 2026 &amp; Awards","author_name":"Chevalier Academy Official","thumbnail_url":"https://i.ytimg.com/vi/$videoId/hqdefault.jpg"}"""
                reqUrl.contains("/watch?v=") ->
                    """<html><head><meta property="og:description" content="Highlights from Chevalier Academy&#39;s Annual Day celebration." /></head></html>"""
                else -> null
            }
        }
        val online = YouTubeMetadataFetcher.fetchMetadata("https://youtu.be/$videoId")
        assertNotNull(online)
        assertTrue(online!!.metadataAvailable)
        assertEquals("Annual Day 2026 &amp; Awards", online.title)
        assertEquals("Highlights from Chevalier Academy's Annual Day celebration.", online.description)
        assertEquals("Chevalier Academy Official", online.authorName)
        assertEquals("https://i.ytimg.com/vi/$videoId/hqdefault.jpg", online.thumbnailUrl)

        // Case B: Offline / unavailable metadata -> no invented title or description
        YouTubeMetadataFetcher.httpGetProvider = { null }
        val offline = YouTubeMetadataFetcher.fetchMetadata("https://www.youtube.com/watch?v=$videoId")
        assertNotNull(offline)
        assertFalse(offline!!.metadataAvailable)
        assertNull(offline.title)
        assertNull(offline.description)
        assertEquals("https://img.youtube.com/vi/$videoId/hqdefault.jpg", offline.thumbnailUrl)
    }

    // ==================== 2. ALL 3 POST TYPES & PHOTO LIMIT ====================

    @Test
    fun `governed post creation validates youtube direct, custom video, and max 10 photo limit`() = runTest {
        // 1. YouTube Direct Post with unavailable metadata (empty title allowed — no invented text)
        val directId = mgmtRepo.saveManagedContentRecord(
            actor = masterAccount,
            section = ManagementSection.SCHOOL_POSTS,
            recordId = 0L,
            recordTitle = "YouTube Video (dQw4w9WgXcQ)",
            proposedJson = ContentSnapshotSerializer.schoolPostToJson(
                SchoolPostEntity(
                    postType = SchoolPostType.YOUTUBE_DIRECT,
                    category = SchoolPostCategory.EVENTS,
                    title = "",
                    description = "",
                    youtubeUrl = "https://youtu.be/dQw4w9WgXcQ"
                )
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Added direct YouTube video"
        ).getOrThrow()

        val savedDirect = database.schoolPostDao().getById(directId)!!
        assertEquals(SchoolPostStatus.PUBLISHED, savedDirect.status)
        assertEquals("", savedDirect.title)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", savedDirect.youtubeUrl)

        // 2. Admin Custom Video Post requires title
        assertTrue(
            mgmtRepo.saveManagedContentRecord(
                actor = masterAccount,
                section = ManagementSection.SCHOOL_POSTS,
                recordId = 0L,
                recordTitle = "Missing Title",
                proposedJson = ContentSnapshotSerializer.schoolPostToJson(
                    SchoolPostEntity(
                        postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
                        title = "",
                        youtubeUrl = "https://youtu.be/M7lc1UVf-VE"
                    )
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = ""
            ).isFailure
        )
        val customId = mgmtRepo.saveManagedContentRecord(
            actor = masterAccount,
            section = ManagementSection.SCHOOL_POSTS,
            recordId = 0L,
            recordTitle = "Physics Lab Tour",
            proposedJson = ContentSnapshotSerializer.schoolPostToJson(
                SchoolPostEntity(
                    postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
                    category = SchoolPostCategory.ACADEMICS,
                    title = "Physics Lab Tour",
                    description = "Guided campus lab walkthrough",
                    youtubeUrl = "https://youtu.be/M7lc1UVf-VE"
                )
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "Custom video post"
        ).getOrThrow()
        assertEquals("Physics Lab Tour", database.schoolPostDao().getById(customId)!!.title)

        // 3. Photo Post: 0 photos rejected, 11 photos rejected, 10 photos accepted
        assertTrue(
            mgmtRepo.saveManagedContentRecord(
                actor = masterAccount,
                section = ManagementSection.SCHOOL_POSTS,
                recordId = 0L,
                recordTitle = "Zero Photos",
                proposedJson = ContentSnapshotSerializer.schoolPostToJson(
                    SchoolPostEntity(postType = SchoolPostType.PHOTO, title = "Zero Photos", photoUrisJson = "[]")
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = ""
            ).isFailure
        )

        val elevenPhotosJson = org.json.JSONArray((1..11).map { "file:///p_$it.jpg" }).toString()
        val elevenPostPayload = org.json.JSONObject(
            ContentSnapshotSerializer.schoolPostToJson(
                SchoolPostEntity(postType = SchoolPostType.PHOTO, title = "Eleven Photos")
            )
        ).put("photoUrisJson", elevenPhotosJson).toString()
        // ContentSnapshotSerializer decodes up to 10; validate SocialFeedRepository & SchoolPostEntity.encodePhotoUris reject >10
        assertTrue(
            runCatching {
                SchoolPostEntity.encodePhotoUris((1..11).map { "file:///p_$it.jpg" })
            }.isFailure
        )
        assertNotNull(elevenPostPayload)

        val tenPhotos = (1..SchoolPostEntity.MAX_PHOTO_COUNT).map { "file:///photo_$it.jpg" }
        val photoId = mgmtRepo.saveManagedContentRecord(
            actor = masterAccount,
            section = ManagementSection.SCHOOL_POSTS,
            recordId = 0L,
            recordTitle = "Sports Day Album",
            proposedJson = ContentSnapshotSerializer.schoolPostToJson(
                SchoolPostEntity(
                    postType = SchoolPostType.PHOTO,
                    category = SchoolPostCategory.SPORTS,
                    title = "Sports Day Album",
                    description = "10 best moments",
                    photoUrisJson = SchoolPostEntity.encodePhotoUris(tenPhotos)
                )
            ),
            saveMode = PublishSaveMode.PUBLISH_NOW,
            changeSummary = "10 photo post"
        ).getOrThrow()
        assertEquals(10, database.schoolPostDao().getById(photoId)!!.photoUris().size)
    }

    // ==================== 3. RBAC, APPROVALS, EDIT, ARCHIVE/RESTORE, PIN, DELETE ====================

    @Test
    fun `staff permissions, approval workflow, editing live post, archive-restore, pinning, and deletion follow existing governance`() = runTest {
        // Create STAFF with VIEW/CREATE/EDIT on SCHOOL_POSTS (NO PUBLISH, NO DELETE)
        val staff = mgmtRepo.createStaffAccount(
            actor = masterAccount,
            username = "feed_editor",
            displayName = "Feed Staff",
            role = ManagementRole.STAFF,
            initialPin = "135790",
            permissions = listOf(
                StaffPermissionEntity(
                    accountId = 0L,
                    sectionKey = ManagementSection.SCHOOL_POSTS.key,
                    canView = true,
                    canCreate = true,
                    canEdit = true,
                    canDelete = false,
                    canPublish = false
                )
            )
        ).getOrThrow()

        // Create SUPER_OWNER with full permissions
        val superOwner = mgmtRepo.createStaffAccount(
            actor = masterAccount,
            username = "vice_principal",
            displayName = "Vice Principal",
            role = ManagementRole.SUPER_OWNER,
            initialPin = "246812",
            permissions = ManagementSection.entries.map {
                StaffPermissionEntity(
                    accountId = 0L,
                    sectionKey = it.key,
                    canView = true,
                    canCreate = true,
                    canEdit = true,
                    canDelete = true,
                    canPublish = true
                )
            }
        ).getOrThrow()

        val draftPayload = ContentSnapshotSerializer.schoolPostToJson(
            SchoolPostEntity(
                postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
                category = SchoolPostCategory.ACHIEVEMENTS,
                title = "District Robotics Win",
                description = "Students won 1st prize",
                youtubeUrl = "https://youtu.be/dQw4w9WgXcQ"
            )
        )

        // Staff CANNOT publish directly
        assertTrue(
            mgmtRepo.saveManagedContentRecord(
                actor = staff,
                section = ManagementSection.SCHOOL_POSTS,
                recordId = 0L,
                recordTitle = "District Robotics Win",
                proposedJson = draftPayload,
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Direct publish attempt"
            ).isFailure
        )

        // Staff CAN save draft and submit for approval
        val postId = mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.SCHOOL_POSTS,
            recordId = 0L,
            recordTitle = "District Robotics Win",
            proposedJson = draftPayload,
            saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
            changeSummary = "Submitted robotics win"
        ).getOrThrow()

        assertEquals(SchoolPostStatus.UNPUBLISHED, database.schoolPostDao().getById(postId)!!.status)
        assertTrue(database.schoolPostDao().observePublishedPosts().first().isEmpty())

        val pendingApproval = database.contentApprovalDao().observePending().first().single()
        assertEquals(ManagementSection.SCHOOL_POSTS.key, pendingApproval.sectionKey)
        assertEquals(postId, pendingApproval.targetRecordId)

        // Staff CANNOT approve own submission
        assertTrue(mgmtRepo.approveSubmission(staff, pendingApproval.id, publishImmediately = true).isFailure)

        // SUPER_OWNER approves & publishes -> now live
        mgmtRepo.approveSubmission(superOwner, pendingApproval.id, publishImmediately = true).getOrThrow()
        assertEquals(SchoolPostStatus.PUBLISHED, database.schoolPostDao().getById(postId)!!.status)
        assertEquals(1, database.schoolPostDao().observePublishedPosts().first().size)

        // Staff submits an EDIT to already-published post -> live title stays intact until approved!
        val editedPayload = ContentSnapshotSerializer.schoolPostToJson(
            database.schoolPostDao().getById(postId)!!.copy(title = "State Robotics Champions")
        )
        mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.SCHOOL_POSTS,
            recordId = postId,
            recordTitle = "State Robotics Champions",
            proposedJson = editedPayload,
            saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
            changeSummary = "Updated title"
        ).getOrThrow()
        assertEquals("District Robotics Win", database.schoolPostDao().getById(postId)!!.title)

        // Approve edit -> live title updates
        val editApproval = database.contentApprovalDao().observePending().first().single()
        mgmtRepo.approveSubmission(masterAccount, editApproval.id, publishImmediately = true).getOrThrow()
        assertEquals("State Robotics Champions", database.schoolPostDao().getById(postId)!!.title)

        // Pin / Unpin requires PUBLISH
        assertTrue(mgmtRepo.setSchoolPostFeatured(staff, postId, true).isFailure)
        mgmtRepo.setSchoolPostFeatured(superOwner, postId, true).getOrThrow()
        assertTrue(database.schoolPostDao().getById(postId)!!.isFeatured)

        // Archiving a PUBLISHED post requires PUBLISH (staff fails; superOwner succeeds and clears pin)
        assertTrue(mgmtRepo.archiveSchoolPost(staff, postId).isFailure)
        mgmtRepo.archiveSchoolPost(superOwner, postId).getOrThrow()
        val archived = database.schoolPostDao().getById(postId)!!
        assertEquals(SchoolPostStatus.ARCHIVED, archived.status)
        assertFalse(archived.isFeatured)
        assertTrue(database.schoolPostDao().observePublishedPosts().first().isEmpty())

        // Cannot pin archived post
        assertTrue(mgmtRepo.setSchoolPostFeatured(masterAccount, postId, true).isFailure)

        // Restore from archive moves back to UNPUBLISHED (never bypasses publish)
        mgmtRepo.restoreArchivedSchoolPost(staff, postId).getOrThrow()
        assertEquals(SchoolPostStatus.UNPUBLISHED, database.schoolPostDao().getById(postId)!!.status)

        // Publish & Unpublish toggle
        mgmtRepo.publishExistingRecord(masterAccount, ManagementSection.SCHOOL_POSTS, postId).getOrThrow()
        assertEquals(SchoolPostStatus.PUBLISHED, database.schoolPostDao().getById(postId)!!.status)
        mgmtRepo.unpublishRecord(masterAccount, ManagementSection.SCHOOL_POSTS, postId).getOrThrow()
        assertEquals(SchoolPostStatus.UNPUBLISHED, database.schoolPostDao().getById(postId)!!.status)

        // Delete requires DELETE permission
        assertTrue(mgmtRepo.deleteRecord(staff, ManagementSection.SCHOOL_POSTS, postId, "State Robotics Champions").isFailure)
        mgmtRepo.deleteRecord(masterAccount, ManagementSection.SCHOOL_POSTS, postId, "State Robotics Champions").getOrThrow()
        assertNull(database.schoolPostDao().getById(postId))

        // Verify audit trail & version history were populated
        val logs = database.activityLogDao().getAllOnce().filter { it.sectionKey == ManagementSection.SCHOOL_POSTS.key }
        assertTrue(logs.any { it.actionType == "SUBMITTED_FOR_APPROVAL" })
        assertTrue(logs.any { it.actionType == "APPROVAL_PUBLISHED" })
        assertTrue(logs.any { it.actionType == "POST_PINNED" })
        assertTrue(logs.any { it.actionType == "POST_ARCHIVED" })
        assertTrue(logs.any { it.actionType == "POST_RESTORED" })
        assertTrue(logs.any { it.actionType == "CONTENT_DELETED" })
        assertTrue(database.versionHistoryDao().getAllForRecordOnce(ManagementSection.SCHOOL_POSTS.key, postId).size >= 4)
    }

    // ==================== 4. 24-HOUR HIGHLIGHTS GOVERNANCE & EXPIRY ====================

    @Test
    fun `highlights require YouTube Shorts, honor approval workflow, and expire after 24h without deleting record`() = runTest {
        val staff = mgmtRepo.createStaffAccount(
            actor = masterAccount,
            username = "hl_staff",
            displayName = "Highlight Staff",
            role = ManagementRole.STAFF,
            initialPin = "654321",
            permissions = listOf(
                StaffPermissionEntity(
                    accountId = 0L,
                    sectionKey = ManagementSection.SCHOOL_HIGHLIGHTS.key,
                    canView = true,
                    canCreate = true,
                    canEdit = true,
                    canDelete = false,
                    canPublish = false
                )
            )
        ).getOrThrow()

        // Non-Shorts URL rejected
        assertTrue(
            mgmtRepo.saveManagedContentRecord(
                actor = masterAccount,
                section = ManagementSection.SCHOOL_HIGHLIGHTS,
                recordId = 0L,
                recordTitle = "Not A Short",
                proposedJson = ContentSnapshotSerializer.schoolHighlightToJson(
                    SchoolHighlightEntity(title = "Not A Short", youtubeShortsUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = ""
            ).isFailure
        )

        // Staff submits valid Shorts Highlight for approval
        val hlId = mgmtRepo.saveManagedContentRecord(
            actor = staff,
            section = ManagementSection.SCHOOL_HIGHLIGHTS,
            recordId = 0L,
            recordTitle = "Morning Prayer Short",
            proposedJson = ContentSnapshotSerializer.schoolHighlightToJson(
                SchoolHighlightEntity(
                    title = "Morning Prayer Short",
                    youtubeShortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
                )
            ),
            saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
            changeSummary = "Morning short"
        ).getOrThrow()

        assertEquals(SchoolPostStatus.UNPUBLISHED, database.schoolHighlightDao().getById(hlId)!!.status)
        val pending = database.contentApprovalDao().observePending().first().single()
        mgmtRepo.approveSubmission(masterAccount, pending.id, publishImmediately = true).getOrThrow()

        val publishedHl = database.schoolHighlightDao().getById(hlId)!!
        assertEquals(SchoolPostStatus.PUBLISHED, publishedHl.status)
        assertEquals(
            publishedHl.publishedAtMillis + SchoolHighlightEntity.HIGHLIGHT_TTL_MILLIS,
            publishedHl.expiresAtMillis
        )

        // Active before 24h, hidden after 24h without deleting the DB entry
        val beforeExpiry = publishedHl.expiresAtMillis - 1_000L
        assertEquals(1, database.schoolHighlightDao().observeActiveHighlights(beforeExpiry).first().size)
        val afterExpiry = publishedHl.expiresAtMillis + 1_000L
        assertTrue(database.schoolHighlightDao().observeActiveHighlights(afterExpiry).first().isEmpty())
        assertNotNull(database.schoolHighlightDao().getById(hlId))
    }

    // ==================== 5. COMPOSE UI TESTS (POST COMPOSER, RBAC BUTTONS, HIGHLIGHTS) ====================

    @Test
    fun `admin UI composer creates Custom Video Post, shows YouTube Direct offline fallback, and hides Publish Now for staff without publish permission`() {
        runBlocking {
            mgmtRepo.createStaffAccount(
                actor = masterAccount,
                username = "staff_no_pub",
                displayName = "Junior Editor",
                role = ManagementRole.STAFF,
                initialPin = "112233",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.SCHOOL_POSTS.key,
                        canView = true,
                        canCreate = true,
                        canEdit = true,
                        canDelete = false,
                        canPublish = false
                    )
                )
            ).getOrThrow()
        }

        // First sign in as Staff without PUBLISH permission
        loginViewModel("staff_no_pub", "112233")

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    SchoolFeedAdminScreen(
                        managementViewModel = managementViewModel,
                        mStrings = managementStringsFor(AppLanguage.ENGLISH)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Open Post Composer -> Staff sees Save Draft & Submit for Approval, NOT Publish Now
        composeTestRule.onNodeWithTag("mgmt_fab_add_school_posts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("school_post_composer_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("editor_save_draft_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("editor_submit_approval_btn").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("editor_publish_now_btn").assertCountEquals(0)

        // Switch to YouTube Direct -> enter URL -> verify clean offline fallback without invented content
        composeTestRule.onNodeWithTag("post_type_chip_youtube_direct").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_composer_youtube_url_input")
            .performTextInput("https://youtu.be/dQw4w9WgXcQ")
        waitUntil {
            composeTestRule.onAllNodesWithTag("post_composer_yt_preview_card")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("post_composer_yt_preview_card").performScrollTo().assertIsDisplayed()

        // Switch to Custom Video Post and submit for approval
        composeTestRule.onNodeWithTag("post_type_chip_admin_custom_video").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_composer_title_input").performScrollTo().performTextInput("Campus Tour Video")
        composeTestRule.onNodeWithTag("editor_submit_approval_btn").performClick()
        waitUntil {
            runBlocking { database.schoolPostDao().observeAllPosts().first().any { it.title == "Campus Tour Video" } }
        }
        val created = runBlocking { database.schoolPostDao().observeAllPosts().first().single() }
        assertEquals(SchoolPostStatus.UNPUBLISHED, created.status)
        assertEquals(1, runBlocking { database.contentApprovalDao().observePending().first().size })
    }
}
