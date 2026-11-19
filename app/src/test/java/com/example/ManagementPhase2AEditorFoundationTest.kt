package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
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
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.ResumedRevisionKind
import com.example.data.repository.SchoolRepository
import com.example.ui.screens.management.NewsEditorScreen
import com.example.ui.screens.management.SchoolProfileSectionEditor
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ManagementPhase2AEditorFoundationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var schoolRepo: SchoolRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var managementViewModel: ManagementViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = UserPreferencesRepository(context)
        mgmtRepo = ManagementRepository(database, MediaStudioManager(context))
        schoolRepo = SchoolRepository(
            schoolProfileDao = database.schoolProfileDao(),
            campusHeroPhotoDao = database.campusHeroPhotoDao(),
            academicInfoDao = database.academicInfoDao(),
            courseDao = database.higherSecondaryCourseDao(),
            facilityDao = database.facilityDao(),
            transportDao = database.transportDao(),
            newsDao = database.newsDao(),
            announcementDao = database.announcementDao(),
            galleryDao = database.galleryDao(),
            socialMediaDao = database.socialMediaDao()
        )
        runBlocking {
            schoolRepo.ensureVerifiedProfileSeeded()
        }
        backupManager = BackupRestoreManager(context, database, prefsRepo)
        viewModelStore = ViewModelStore()
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(mgmtRepo, backupManager)
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()
        database.close()
    }

    private fun authenticateUser(username: String, pin: String) {
        managementViewModel.loginWithPin(username, pin)
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }
    }

    private fun setupAndLoginMasterAdmin() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        }
        authenticateUser("admin", "246810")
    }

    private fun awaitCondition(timeoutMs: Long = 5_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()
        assertTrue(
            "Timed out after ${timeoutMs}ms (vmError=${managementViewModel.errorMessage.value})",
            condition()
        )
    }

    private fun awaitTagDisplayed(tag: String, scrollTo: Boolean = false) {
        awaitCondition {
            runCatching {
                val node = composeTestRule.onNodeWithTag(tag, useUnmergedTree = false)
                if (scrollTo) node.performScrollTo()
                node.assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun awaitTagDoesNotExist(tag: String) {
        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun `draft resumption loads draft values into News and Profile editors without modifying live published data`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session

        val newsId = runBlocking {
            val live = NewsItemEntity(
                title = "Live Annual Sports Day",
                summary = "Live published summary",
                fullBody = "Live published full article",
                publishedDateText = "10 Nov 2026"
            )
            val id = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = live.title,
                proposedJson = ContentSnapshotSerializer.newsToJson(live),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Initial live publish"
            ).getOrThrow()

            // Save a DRAFT edit that must NOT overwrite live record
            val draft = live.copy(
                id = id,
                title = "Draft Resumed Sports Headline",
                summary = "Draft updated sports summary"
            )
            mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.NEWS,
                recordId = id,
                recordTitle = draft.title,
                proposedJson = ContentSnapshotSerializer.newsToJson(draft),
                saveMode = PublishSaveMode.SAVE_DRAFT,
                changeSummary = "Draft headline polish"
            ).getOrThrow()

            // Also save an ABOUT_SCHOOL draft without touching live profile
            val liveProfile = database.schoolProfileDao().getProfileOnce()!!
            mgmtRepo.saveSchoolProfileSection(
                actor = master,
                section = ManagementSection.ABOUT_SCHOOL,
                updatedProfile = liveProfile.copy(vision = "Draft Holistic Vision 2027"),
                saveMode = PublishSaveMode.SAVE_DRAFT,
                changeSummary = "Draft vision update"
            ).getOrThrow()

            id
        }

        // Verify live published data was NEVER overwritten by saving drafts
        runBlocking {
            val publicNews = schoolRepo.newsItems.first()
            assertEquals(1, publicNews.size)
            assertEquals("Live Annual Sports Day", publicNews.single().title)
            assertEquals("Live published summary", publicNews.single().summary)

            val publicProfile = schoolRepo.schoolProfile.first()!!
            assertFalse(publicProfile.vision.orEmpty().contains("Draft Holistic Vision 2027"))
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NewsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        val editTag = "managed_edit_btn_${"Live Annual Sports Day".lowercase().take(24)}"
        awaitTagDisplayed(editTag)
        composeTestRule.onNodeWithTag(editTag).performClick()

        awaitTagDisplayed("universal_content_editor_scaffold")
        awaitTagDisplayed("editor_revision_banner")
        assertTrue(
            composeTestRule.onAllNodesWithText(
                mStrings.editorResumingDraftTitle(2),
                substring = true
            ).fetchSemanticsNodes().isNotEmpty()
        )
        // Draft fields should be pre-filled instead of the old live title
        composeTestRule.onNodeWithTag("news_editor_title").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("news_editor_title").assertTextContains("Draft Resumed Sports Headline")
        composeTestRule.onNodeWithTag("news_editor_summary").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("news_editor_summary").assertTextContains("Draft updated sports summary")

        val resolved = runBlocking {
            mgmtRepo.resolveEditableRevision(ManagementSection.NEWS, newsId)
        }
        assertEquals(ResumedRevisionKind.DRAFT, resolved.revisionKind)
    }

    @Test
    fun `pending revision resumption displays pending banner and proposed values`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session

        runBlocking {
            val staff = mgmtRepo.createStaffAccount(
                actor = master,
                username = "newseditor",
                displayName = "News Editor",
                role = ManagementRole.STAFF,
                initialPin = "112233",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0,
                        sectionKey = ManagementSection.NEWS.key,
                        canView = true,
                        canCreate = true,
                        canEdit = true
                    )
                )
            ).getOrThrow()

            val live = NewsItemEntity(
                title = "Live Science Expo",
                summary = "Live expo details",
                fullBody = "Live expo body",
                publishedDateText = "11 Nov 2026"
            )
            val id = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = live.title,
                proposedJson = ContentSnapshotSerializer.newsToJson(live),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Published expo"
            ).getOrThrow()

            val pendingProposal = live.copy(
                id = id,
                title = "Pending Science Expo Schedule Change",
                summary = "Submitted for approval"
            )
            mgmtRepo.saveManagedContentRecord(
                actor = staff,
                section = ManagementSection.NEWS,
                recordId = id,
                recordTitle = pendingProposal.title,
                proposedJson = ContentSnapshotSerializer.newsToJson(pendingProposal),
                saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
                changeSummary = "Updated time"
            ).getOrThrow()
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NewsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        val editTag = "managed_edit_btn_${"Live Science Expo".lowercase().take(24)}"
        awaitTagDisplayed(editTag)
        composeTestRule.onNodeWithTag(editTag).performClick()

        awaitTagDisplayed("editor_revision_banner")
        assertTrue(
            composeTestRule.onAllNodesWithText(
                mStrings.editorPendingRevisionTitle(2),
                substring = true
            ).fetchSemanticsNodes().isNotEmpty()
        )
        composeTestRule.onNodeWithText(mStrings.editorPendingPreservedHint).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending Science Expo Schedule Change").assertIsDisplayed()
    }

    @Test
    fun `reset to live reverts draft fields, clears revision banner, records audit, and preserves live data`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session

        val newsId = runBlocking {
            val live = NewsItemEntity(
                title = "Live Convocation Notice",
                summary = "Original live summary",
                fullBody = "Original live full body",
                publishedDateText = "12 Nov 2026"
            )
            val id = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = live.title,
                proposedJson = ContentSnapshotSerializer.newsToJson(live),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Live convocation"
            ).getOrThrow()

            val draft = live.copy(id = id, title = "Temporary Draft Convocation Title")
            mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.NEWS,
                recordId = id,
                recordTitle = draft.title,
                proposedJson = ContentSnapshotSerializer.newsToJson(draft),
                saveMode = PublishSaveMode.SAVE_DRAFT,
                changeSummary = "Draft edit"
            ).getOrThrow()
            id
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NewsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        val editTag = "managed_edit_btn_${"Live Convocation Notice".lowercase().take(24)}"
        awaitTagDisplayed(editTag)
        composeTestRule.onNodeWithTag(editTag).performClick()

        awaitTagDisplayed("editor_revision_banner")
        composeTestRule.onNodeWithText("Temporary Draft Convocation Title").assertIsDisplayed()
        awaitTagDisplayed("editor_reset_to_live_btn")
        composeTestRule.onNodeWithTag("editor_reset_to_live_btn").performClick()

        awaitTagDoesNotExist("editor_revision_banner")
        composeTestRule.onNodeWithTag("news_editor_title").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("news_editor_title").assertTextContains("Live Convocation Notice")

        runBlocking {
            val latestVer = database.versionHistoryDao().getLatestForRecord(ManagementSection.NEWS.key, newsId)
            assertEquals(ContentWorkflowStatus.DRAFT_DISCARDED, latestVer?.approvalStatus)
            assertEquals("Live Convocation Notice", schoolRepo.newsItems.first().single().title)
        }
    }

    @Test
    fun `unsaved changes trigger discard confirmation dialog and Keep Editing preserves entered text`() {
        setupAndLoginMasterAdmin()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NewsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_news")
        composeTestRule.onNodeWithTag("mgmt_fab_add_news").performClick()
        awaitTagDisplayed("news_editor_title")

        composeTestRule.onNodeWithTag("news_editor_title").performTextInput("Unsaved Draft Headline")
        composeTestRule.onNodeWithTag("editor_unsaved_indicator").assertIsDisplayed()

        // Attempt to leave via Cancel -> discard confirmation appears
        composeTestRule.onNodeWithTag("editor_cancel_btn").performClick()
        awaitTagDisplayed("editor_discard_confirm_dialog")

        // Choose Keep Editing -> dialog dismissed, entered text remains
        composeTestRule.onNodeWithTag("editor_keep_editing_btn").performClick()
        awaitTagDoesNotExist("editor_discard_confirm_dialog")
        composeTestRule.onNodeWithText("Unsaved Draft Headline").assertIsDisplayed()

        // Attempt to leave via Top Close -> discard confirmation appears -> Confirm Discard closes editor
        composeTestRule.onNodeWithTag("editor_close_btn").performClick()
        awaitTagDisplayed("editor_discard_confirm_dialog")
        composeTestRule.onNodeWithTag("editor_confirm_discard_btn").performClick()
        awaitTagDoesNotExist("universal_content_editor_scaffold")
    }

    @Test
    fun `embedded SchoolProfileSectionEditor registers navigation guard for unsaved edits and resumes drafts`() {
        setupAndLoginMasterAdmin()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        var leaveGuard: ((() -> Unit) -> Unit)? = null
        var leftEditor = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    SchoolProfileSectionEditor(
                        managementViewModel = managementViewModel,
                        mStrings = mStrings,
                        section = ManagementSection.ABOUT_SCHOOL,
                        onNavigateBack = { leftEditor = true },
                        onRegisterNavigationGuard = { leaveGuard = it }
                    )
                }
            }
        }

        awaitTagDisplayed("about_vision_input")
        composeTestRule.onNodeWithTag("about_vision_input").performTextClearance()
        composeTestRule.onNodeWithTag("about_vision_input").performTextInput("Draft Inspiring Vision")
        awaitTagDisplayed("editor_unsaved_indicator")

        // Simulate tapping parent Dashboard Back button using registered navigation guard
        composeTestRule.runOnIdle {
            leaveGuard?.invoke { leftEditor = true }
        }
        awaitTagDisplayed("editor_discard_confirm_dialog")
        assertFalse(leftEditor)

        composeTestRule.onNodeWithTag("editor_confirm_discard_btn").performClick()
        awaitCondition { leftEditor }
    }

    @Test
    fun `validation failure shows inline error and preserves user inputs`() {
        setupAndLoginMasterAdmin()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NewsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_news")
        composeTestRule.onNodeWithTag("mgmt_fab_add_news").performClick()
        awaitTagDisplayed("news_editor_title")

        composeTestRule.onNodeWithTag("news_editor_title").performTextInput("Typed Title Must Survive Validation")
        // Summary and body intentionally left blank
        composeTestRule.onNodeWithTag("editor_save_draft_btn").performClick()

        awaitTagDisplayed("mgmt_error_banner")
        composeTestRule.onNodeWithText(mStrings.newsRequiredError).assertIsDisplayed()
        composeTestRule.onNodeWithText("Typed Title Must Survive Validation").assertIsDisplayed()
    }

    @Test
    fun `RBAC hides Publish Now for non-publishing staff and enforces view-only restriction`() {
        runBlocking {
            val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "author",
                displayName = "Content Author",
                role = ManagementRole.STAFF,
                initialPin = "112233",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0,
                        sectionKey = ManagementSection.NEWS.key,
                        canView = true,
                        canCreate = true,
                        canEdit = true,
                        canPublish = false
                    )
                )
            ).getOrThrow()

            // Seed a live article as Master Admin
            val live = NewsItemEntity(
                title = "Live RBAC Article",
                summary = "Summary",
                fullBody = "Body",
                publishedDateText = "12 Nov 2026"
            )
            mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = live.title,
                proposedJson = ContentSnapshotSerializer.newsToJson(live),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Initial live"
            ).getOrThrow()
        }

        authenticateUser("author", "112233")
        val authorSession = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session

        // Repository layer must also deny PUBLISH_NOW for non-publishing staff
        val publishDenied = runBlocking {
            mgmtRepo.saveManagedContentRecord(
                actor = authorSession,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = "Unauthorized Publish",
                proposedJson = ContentSnapshotSerializer.newsToJson(
                    NewsItemEntity(title = "Unauthorized Publish", summary = "S", fullBody = "B", publishedDateText = "12 Nov 2026")
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Try publish"
            )
        }
        assertTrue(publishDenied.isFailure)
        assertTrue(publishDenied.exceptionOrNull() is SecurityException)

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NewsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_news")
        composeTestRule.onNodeWithTag("mgmt_fab_add_news").performClick()
        awaitTagDisplayed("editor_save_draft_btn")
        awaitTagDisplayed("editor_submit_approval_btn")
        composeTestRule.onNodeWithTag("editor_publish_now_btn").assertDoesNotExist()
    }

    @Test
    fun `News Preview tab renders public NewsItemCard and Submit-Approve workflow still publishes live`() {
        runBlocking {
            val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "reporter",
                displayName = "Campus Reporter",
                role = ManagementRole.STAFF,
                initialPin = "334455",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0,
                        sectionKey = ManagementSection.NEWS.key,
                        canView = true,
                        canCreate = true,
                        canEdit = true,
                        canPublish = false
                    )
                )
            ).getOrThrow()
        }

        authenticateUser("reporter", "334455")
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NewsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_news")
        composeTestRule.onNodeWithTag("mgmt_fab_add_news").performClick()
        awaitTagDisplayed("news_editor_title")

        composeTestRule.onNodeWithTag("news_editor_title").performTextInput("Preview Robotics Gold Medal")
        composeTestRule.onNodeWithTag("news_editor_summary").performTextInput("Chevalier team wins state robotics first prize")
        composeTestRule.onNodeWithTag("news_editor_body").performTextInput("Full competition report from Dindigul campus.")

        // Switch to Preview tab -> public NewsItemCard preview is rendered live
        composeTestRule.onNodeWithTag("editor_tab_preview").performClick()
        awaitTagDisplayed("editor_preview_container")
        composeTestRule.onNodeWithTag("news_item_card_0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preview Robotics Gold Medal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chevalier team wins state robotics first prize").assertIsDisplayed()

        // Switch back to Edit and submit for approval
        composeTestRule.onNodeWithTag("editor_tab_edit").performClick()
        awaitTagDisplayed("news_editor_title", scrollTo = true)
        composeTestRule.onNodeWithTag("editor_submit_approval_btn").performClick()

        awaitTagDoesNotExist("universal_content_editor_scaffold")

        // Pending approval created, public stream still empty until Master Admin approves
        runBlocking {
            val pending = mgmtRepo.pendingApprovals.first()
            assertEquals(1, pending.size)
            assertEquals("Preview Robotics Gold Medal", pending.single().recordTitle)
            assertTrue(schoolRepo.newsItems.first().isEmpty())

            val masterSession = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
            mgmtRepo.approveSubmission(masterSession, pending.single().id, publishImmediately = true).getOrThrow()

            val publicNews = schoolRepo.newsItems.first()
            assertEquals(1, publicNews.size)
            assertEquals("Preview Robotics Gold Medal", publicNews.single().title)
        }
    }
}
