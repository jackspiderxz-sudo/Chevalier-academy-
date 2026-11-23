package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.AdminHeroBannerConfig
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AdminDashboardCard
import com.example.data.repository.ManagementRepository
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.management.ManagementPortalScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
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
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminDashboardHomeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var backupManager: BackupRestoreManager
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var mainViewModel: MainViewModel
    private lateinit var managementViewModel: ManagementViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = MediaStudioManager(context)
        prefsRepo = UserPreferencesRepository(context)
        runBlocking {
            prefsRepo.saveAdminHeroBanner(AdminHeroBannerConfig())
        }
        mgmtRepo = ManagementRepository(
            database = database,
            mediaStudioManager = mediaManager,
            preferencesRepository = prefsRepo
        )
        backupManager = BackupRestoreManager(context, database, prefsRepo)
        viewModelStore = ViewModelStore()
        mainViewModel = ViewModelProvider(
            viewModelStore,
            MainViewModel.Factory(prefsRepo)
        )[MainViewModel::class.java]
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(
                managementRepository = mgmtRepo,
                backupRestoreManager = backupManager,
                preferencesRepository = prefsRepo
            )
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        database.close()
        File(context.filesDir, "chevalier_media").deleteRecursively()
    }

    private fun awaitCondition(timeoutMs: Long = 6_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()
        assertTrue("Timed out waiting for condition", condition())
    }

    private fun loginAsMasterAdmin(displayName: String = "Dr. Principal") {
        runBlocking {
            mgmtRepo.setupMasterAdmin(displayName, "admin", "246810", "246810").getOrThrow()
        }
        managementViewModel.loginWithPin("admin", "246810")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }
    }

    @Test
    fun `admin header, compact landscape hero banner, exact 8 cards in 2x4 order, and reused bottom navigation render accurately`() {
        loginAsMasterAdmin("Dr. Chevalier Principal")
        var lastPublicTabSelected: String? = null
        var backExitedToPublic = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ManagementPortalScreen(
                        managementViewModel = managementViewModel,
                        mainViewModel = mainViewModel,
                        currentLanguage = AppLanguage.ENGLISH,
                        currentThemeMode = AppThemeMode.LIGHT,
                        onExitToPublicApp = { backExitedToPublic = true },
                        onSelectPublicTab = { route -> lastPublicTabSelected = route }
                    )
                }
            }
        }

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("management_dashboard_grid").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // 1. Header layout: Left (Back + CHEVALIER ACADEMY), Right (Search -> Notification -> Menu)
        val backBounds = composeTestRule.onNodeWithTag("mgmt_header_back_btn").getUnclippedBoundsInRoot()
        val titleBounds = composeTestRule.onNodeWithTag("mgmt_header_title").getUnclippedBoundsInRoot()
        val searchBounds = composeTestRule.onNodeWithTag("mgmt_top_search_btn").getUnclippedBoundsInRoot()
        val notifyBounds = composeTestRule.onNodeWithTag("mgmt_top_notifications_btn").getUnclippedBoundsInRoot()
        val menuBounds = composeTestRule.onNodeWithTag("mgmt_top_menu_btn").getUnclippedBoundsInRoot()

        composeTestRule.onNodeWithTag("mgmt_header_title").assertTextEquals("CHEVALIER ACADEMY")
        assertTrue("Back button must be left of title", backBounds.left < titleBounds.left)
        assertTrue("Search icon must precede Notification bell", searchBounds.left < notifyBounds.left)
        assertTrue("Notification bell must precede Menu icon", notifyBounds.left < menuBounds.left)

        // 2. Compact landscape rectangular Hero Banner directly below header with 3 editable text lines
        val heroBounds = composeTestRule.onNodeWithTag("admin_hero_banner_card").getUnclippedBoundsInRoot()
        val heroWidth = (heroBounds.right - heroBounds.left).value
        val heroHeight = (heroBounds.bottom - heroBounds.top).value
        assertTrue(
            "Hero banner must be compact landscape rectangle (width=$heroWidth, height=$heroHeight)",
            heroWidth > heroHeight * 1.8f && heroHeight <= 170f
        )
        composeTestRule.onNodeWithTag("admin_hero_line_1").assertTextEquals("Admin Dashboard")
        composeTestRule.onNodeWithTag("admin_hero_line_2").assertTextEquals("Welcome Back, Dr. Chevalier Principal")
        composeTestRule.onNodeWithTag("admin_hero_line_3").assertTextEquals("School Administration Panel")

        // 3. Exact 8 Admin Management Cards in exact 2x4 order
        val expectedOrder = listOf(
            AdminDashboardCard.HOME_PAGE to "Home Page",
            AdminDashboardCard.ACADEMY to "Academy",
            AdminDashboardCard.PARENT to "Parent",
            AdminDashboardCard.CONTACT to "Contact",
            AdminDashboardCard.STAFF_MANAGEMENT to "Staff Management",
            AdminDashboardCard.COMMON_APP to "Common App",
            AdminDashboardCard.MANAGEMENT_TOOLS to "Management Tools",
            AdminDashboardCard.UPDATES to "Updates"
        )
        assertEquals(8, AdminDashboardCard.entries.size)
        expectedOrder.forEachIndexed { index, (card, expectedLabel) ->
            assertEquals(card, AdminDashboardCard.entries[index])
            assertEquals(expectedLabel, card.title)
            composeTestRule.onNodeWithTag("management_dashboard_grid")
                .performScrollToNode(hasTestTag("admin_dash_card_${card.id}"))
            composeTestRule.onNode(
                hasTestTag("admin_dash_card_${card.id}") and hasText(expectedLabel)
            ).assertIsDisplayed()
        }

        // Verify 2-column x 4-row grid alignment and equal card dimensions across Row 1 and Row 2
        composeTestRule.onNodeWithTag("management_dashboard_grid")
            .performScrollToNode(hasTestTag("admin_dash_card_home_page"))
        val homeCardBounds = composeTestRule.onNodeWithTag("admin_dash_card_home_page").getUnclippedBoundsInRoot()
        val academyCardBounds = composeTestRule.onNodeWithTag("admin_dash_card_academy").getUnclippedBoundsInRoot()
        assertTrue("Home Page and Academy must sit side-by-side in row 1", homeCardBounds.left < academyCardBounds.left)
        assertTrue(
            "Row 1 cards must share top alignment",
            kotlin.math.abs((homeCardBounds.top - academyCardBounds.top).value) < 2f
        )

        composeTestRule.onNodeWithTag("management_dashboard_grid")
            .performScrollToNode(hasTestTag("admin_dash_card_parent"))
        val parentCardBounds = composeTestRule.onNodeWithTag("admin_dash_card_parent").getUnclippedBoundsInRoot()
        val contactCardBounds = composeTestRule.onNodeWithTag("admin_dash_card_contact").getUnclippedBoundsInRoot()
        assertTrue("Parent and Contact must sit side-by-side in row 2", parentCardBounds.left < contactCardBounds.left)
        assertTrue(
            "Row 2 cards must share top alignment",
            kotlin.math.abs((parentCardBounds.top - contactCardBounds.top).value) < 2f
        )
        assertTrue(
            "Parent must align in the left column under Home Page",
            kotlin.math.abs((parentCardBounds.left - homeCardBounds.left).value) < 2f
        )
        assertTrue(
            "Cards must have equal height",
            kotlin.math.abs(((homeCardBounds.bottom - homeCardBounds.top) - (parentCardBounds.bottom - parentCardBounds.top)).value) < 2f
        )

        // 4. Reused public bottom navigation order: Home -> Academics -> Parent -> Contact
        composeTestRule.onNodeWithTag("public_bottom_navigation_bar").assertIsDisplayed()
        val navHome = composeTestRule.onNodeWithTag("bottom_nav_home").getUnclippedBoundsInRoot()
        val navAcad = composeTestRule.onNodeWithTag("bottom_nav_academics").getUnclippedBoundsInRoot()
        val navParent = composeTestRule.onNodeWithTag("bottom_nav_parent").getUnclippedBoundsInRoot()
        val navContact = composeTestRule.onNodeWithTag("bottom_nav_contact").getUnclippedBoundsInRoot()
        assertTrue(navHome.left < navAcad.left && navAcad.left < navParent.left && navParent.left < navContact.left)

        composeTestRule.onNodeWithTag("bottom_nav_parent").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppRoute.PARENT, lastPublicTabSelected)

        composeTestRule.onNodeWithTag("mgmt_header_back_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(backExitedToPublic)
    }

    @Test
    fun `admin can edit, preview, save, and persist hero banner text and background image across restart`() {
        loginAsMasterAdmin("Principal Mary")

        // Also test image import persistence into internal managed storage via repository
        val tempFile = File(context.cacheDir, "admin_hero_bg_test.jpg")
        val bmp = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.rgb(20, 40, 90))
        FileOutputStream(tempFile).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        bmp.recycle()

        val importedUri = runBlocking {
            val actor = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
            mgmtRepo.importAdminHeroBackgroundImage(actor, Uri.fromFile(tempFile)).getOrThrow()
        }
        assertTrue("Imported hero image must be stored inside app-private chevalier_media", importedUri.contains("chevalier_media"))
        assertTrue("Stored hero image file must exist and be readable", mediaManager.isManagedMediaFileReadable(importedUri))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ManagementPortalScreen(
                        managementViewModel = managementViewModel,
                        mainViewModel = mainViewModel,
                        currentLanguage = AppLanguage.ENGLISH,
                        currentThemeMode = AppThemeMode.LIGHT,
                        onExitToPublicApp = {}
                    )
                }
            }
        }

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("admin_hero_edit_btn").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Open Edit Hero Banner dialog, edit all 3 lines, toggle Preview, then Save
        composeTestRule.onNodeWithTag("admin_hero_edit_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_hero_editor_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("admin_hero_input_line_1").performTextClearance()
        composeTestRule.onNodeWithTag("admin_hero_input_line_1").performTextInput("Chevalier Control Hub")
        composeTestRule.onNodeWithTag("admin_hero_input_line_2").performTextClearance()
        composeTestRule.onNodeWithTag("admin_hero_input_line_2").performTextInput("Welcome Back, {Admin Name}")
        composeTestRule.onNodeWithTag("admin_hero_input_line_3").performTextClearance()
        composeTestRule.onNodeWithTag("admin_hero_input_line_3").performTextInput("Academic Year 2026-27")

        composeTestRule.onNodeWithTag("admin_hero_preview_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_hero_preview_card").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("admin_hero_save_btn").performClick()

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("admin_hero_editor_dialog").assertDoesNotExist()
                composeTestRule.onNodeWithTag("admin_hero_line_1").assertTextEquals("Chevalier Control Hub")
                composeTestRule.onNodeWithTag("admin_hero_line_3").assertTextEquals("Academic Year 2026-27")
                true
            }.getOrDefault(false)
        }

        // Verify persisted in DataStore across fresh read and logged in ActivityLog
        val persisted = runBlocking { prefsRepo.adminHeroBannerFlow.first() }
        assertEquals("Chevalier Control Hub", persisted.titleLine1)
        assertEquals("Welcome Back, {Admin Name}", persisted.welcomeLine2)
        assertEquals("Academic Year 2026-27", persisted.subtitleLine3)

        val logs = runBlocking { database.activityLogDao().observeAll().first() }
        assertTrue(logs.any { it.actionType == "ADMIN_HERO_BANNER_UPDATED" })
    }

    @Test
    fun `rbac enforces same 8-card layout for staff while blocking unauthorized cards, hero edits, and scoped search leaks`() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
            val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()

            // Seed public Home post and public Academy course to prove Admin Search never exposes them
            database.schoolPostDao().upsert(
                SchoolPostEntity(
                    title = "PublicFestivalSecretToken",
                    description = "Public campus festival post",
                    category = "Events",
                    postType = "PHOTO",
                    status = "PUBLISHED"
                )
            )
            database.higherSecondaryCourseDao().upsert(
                HigherSecondaryCourseEntity(
                    groupCode = "CS101",
                    streamTitle = "PublicAcademyCourseSecretToken",
                    subjectsSummary = "Computer Science",
                    mediumOfInstruction = "English",
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED
                )
            )

            // Create restricted staff who can ONLY view Updates (News) and Contact
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "news_staff",
                displayName = "News Coordinator",
                role = ManagementRole.STAFF,
                initialPin = "334455",
                permissions = listOf(
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.NEWS.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.CONTACT_DETAILS.key, canView = true)
                )
            ).getOrThrow()
        }

        // Repository-level RBAC check: unauthorized staff cannot edit Hero Banner
        runBlocking {
            val staff = mgmtRepo.loginWithPin("news_staff", "334455").getOrThrow()
            assertFalse(mgmtRepo.canEditAdminHeroBanner(staff))
            val saveAttempt = mgmtRepo.saveAdminHeroBannerConfig(
                actor = staff,
                titleLine1 = "Unauthorized",
                welcomeLine2 = "Unauthorized",
                subtitleLine3 = "Unauthorized",
                backgroundImageUri = null
            )
            assertTrue("Repository must reject unauthorized Hero Banner update", saveAttempt.isFailure)
            assertTrue(saveAttempt.exceptionOrNull() is SecurityException)

            // Scoped Admin Dashboard search: must NOT return public Home post, public Academy course, or restricted Staff Management
            val publicPostResults = mgmtRepo.searchAdminDashboard(staff, "PublicFestivalSecretToken")
            assertTrue("Admin Search must never expose public Home posts", publicPostResults.isEmpty())

            val publicCourseResults = mgmtRepo.searchAdminDashboard(staff, "PublicAcademyCourseSecretToken")
            assertTrue("Admin Search must never expose public Academy courses", publicCourseResults.isEmpty())

            val staffSectionResults = mgmtRepo.searchAdminDashboard(staff, "Staff Management")
            assertTrue("Restricted staff must not see Staff Management in Admin Search", staffSectionResults.isEmpty())

            val contactResults = mgmtRepo.searchAdminDashboard(staff, "Contact")
            assertTrue("Authorized Contact section must appear in Admin Search", contactResults.any { it.targetCard == AdminDashboardCard.CONTACT })
        }

        // UI-level RBAC check: restricted staff sees the exact same 8-card layout, with lock on restricted cards
        managementViewModel.loginWithPin("news_staff", "334455")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ManagementPortalScreen(
                        managementViewModel = managementViewModel,
                        mainViewModel = mainViewModel,
                        currentLanguage = AppLanguage.ENGLISH,
                        currentThemeMode = AppThemeMode.LIGHT,
                        onExitToPublicApp = {}
                    )
                }
            }
        }

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("management_dashboard_grid").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // All 8 cards remain rendered in the same Dashboard layout
        AdminDashboardCard.entries.forEach { card ->
            composeTestRule.onNodeWithTag("management_dashboard_grid")
                .performScrollToNode(hasTestTag("admin_dash_card_${card.id}"))
            composeTestRule.onNodeWithTag("admin_dash_card_${card.id}").assertIsDisplayed()
        }

        // Hero edit button is hidden for unauthorized staff
        composeTestRule.onNodeWithTag("admin_hero_edit_btn").assertDoesNotExist()

        // Staff Management card is locked and tapping it is blocked with an error banner
        composeTestRule.onNodeWithTag("management_dashboard_grid")
            .performScrollToNode(hasTestTag("admin_dash_card_staff_management"))
        composeTestRule.onNodeWithTag("admin_dash_card_lock_staff_management", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_dash_card_staff_management").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mgmt_error_banner").assertIsDisplayed()
        // Remains on Dashboard grid, never enters Staff Management
        composeTestRule.onNodeWithTag("management_dashboard_grid").assertIsDisplayed()
    }
}
