package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.model.SchoolTenantContext
import com.example.data.backend.BackendConfigDetector
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.ui.screens.HomeSocialFeedScreen
import com.example.ui.screens.management.BackupRestoreScreen
import com.example.ui.screens.management.SchoolFeedAdminScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase3LocalizationTest {

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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = (context.applicationContext as ChevalierApplication).container.mediaStudioManager
        mgmtRepo = ManagementRepository(database, mediaManager)
        prefsRepo = UserPreferencesRepository(context)
        backupManager = BackupRestoreManager(context, database, prefsRepo)
        viewModelStore = ViewModelStore()
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(
                managementRepository = mgmtRepo,
                backupRestoreManager = backupManager,
                preferencesRepository = prefsRepo,
                centralBackendStatus = BackendConfigDetector.inspect(context)
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

    private fun containsTamilScript(value: String): Boolean =
        value.any { it in '\u0B80'..'\u0BFF' }

    private fun containsDevanagariScript(value: String): Boolean =
        value.any { it in '\u0900'..'\u097F' }

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

    private fun loginMasterAdmin() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        }
        managementViewModel.loginWithPin("admin", "246810")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }
    }

    // ==================== 1. CENTRALIZED TRANSLATION COMPLETENESS ====================

    @Test
    fun `social category labels translate all defaults in EN TA HI and preserve custom category keys`() {
        val en = stringsFor(AppLanguage.ENGLISH)
        val ta = stringsFor(AppLanguage.TAMIL)
        val hi = stringsFor(AppLanguage.HINDI)

        val enMgmt = managementStringsFor(AppLanguage.ENGLISH).schoolFeed
        val taMgmt = managementStringsFor(AppLanguage.TAMIL).schoolFeed
        val hiMgmt = managementStringsFor(AppLanguage.HINDI).schoolFeed

        SchoolPostCategory.defaults.forEach { categoryKey ->
            val enLabel = en.socialCategoryLabel(categoryKey)
            val taLabel = ta.socialCategoryLabel(categoryKey)
            val hiLabel = hi.socialCategoryLabel(categoryKey)

            assertEquals(categoryKey, enLabel)
            assertTrue("Expected Tamil script for $categoryKey, got '$taLabel'", containsTamilScript(taLabel))
            assertTrue("Expected Devanagari script for $categoryKey, got '$hiLabel'", containsDevanagariScript(hiLabel))
            assertNotEquals(enLabel, taLabel)
            assertNotEquals(enLabel, hiLabel)

            // Management school feed categories stay aligned with public AppStrings
            assertEquals(enLabel, enMgmt.categoryLabel(categoryKey))
            assertEquals(taLabel, taMgmt.categoryLabel(categoryKey))
            assertEquals(hiLabel, hiMgmt.categoryLabel(categoryKey))
        }

        // Case-insensitive lookup and custom category passthrough
        assertEquals(ta.socialCategories.campusLife, ta.socialCategoryLabel("campus life"))
        assertEquals(hi.socialCategories.sports, hi.socialCategoryLabel("SPORTS"))
        assertEquals("Science Club", en.socialCategoryLabel("Science Club"))
        assertEquals("Science Club", ta.socialCategoryLabel("Science Club"))
        assertEquals("Science Club", hi.socialCategoryLabel("Science Club"))
    }

    @Test
    fun `backend status tenant ID label localizes in EN TA HI while preserving exact tenantId value`() {
        val tenantId = SchoolTenantContext.DEFAULT_SCHOOL_ID
        val en = managementStringsFor(AppLanguage.ENGLISH).backendStatusTenantId(tenantId)
        val ta = managementStringsFor(AppLanguage.TAMIL).backendStatusTenantId(tenantId)
        val hi = managementStringsFor(AppLanguage.HINDI).backendStatusTenantId(tenantId)

        assertEquals("Tenant ID: $tenantId", en)
        assertTrue("Tamil tenant label should contain Tamil script: $ta", containsTamilScript(ta))
        assertTrue("Hindi tenant label should contain Devanagari script: $hi", containsDevanagariScript(hi))
        assertTrue("Tamil tenant label must preserve raw tenantId verbatim", ta.contains(tenantId))
        assertTrue("Hindi tenant label must preserve raw tenantId verbatim", hi.contains(tenantId))
    }

    // ==================== 2. HOME SOCIAL FEED CATEGORY LOCALIZATION (TASK C) ====================

    @Test
    fun `HomeSocialFeedScreen displays Tamil category chips and post badge while filtering by raw category ID`() {
        val taStrings = stringsFor(AppLanguage.TAMIL)
        val sampleProfile = SchoolProfileEntity(
            schoolName = "CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL",
            shortName = "Chevalier Academy",
            motto = "Learn to Live & Love",
            addressLine = "Siluvathur Road, Dindigul - 624 306",
            emblemMark = "CA",
            poweredBy = "POWERED BY JK LABS",
            welcomeIntroduction = "Welcome to Chevalier Academy."
        )
        val sportsPost = SchoolPostEntity(
            id = 101L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.SPORTS,
            title = "Annual Sports Day 2026",
            description = "Track & Field Finals",
            youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = 1_750_000_000_000L
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides taStrings) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HomeSocialFeedScreen(
                        profile = sampleProfile,
                        heroPhotos = emptyList(),
                        publishedPosts = listOf(sportsPost),
                        publishedHighlights = emptyList(),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Scroll to social feed category filter row
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_social_feed"))
        composeTestRule.waitForIdle()

        // Verify category chips show Tamil labels, keyed on raw IDs
        composeTestRule.onNodeWithTag("feed_filter_general")
            .assertTextEquals(taStrings.socialCategories.general)
        composeTestRule.onNodeWithTag("feed_filter_academics")
            .assertTextEquals(taStrings.socialCategories.academics)

        // Scroll category LazyRow to Sports chip, verify Tamil text, click to filter by raw "Sports"
        composeTestRule.onNodeWithTag("home_feed_category_row")
            .performScrollToNode(hasTestTag("feed_filter_sports"))
        composeTestRule.onNodeWithTag("feed_filter_sports")
            .assertTextEquals(taStrings.socialCategories.sports)
            .performClick()
        composeTestRule.waitForIdle()

        // Verify post card remains visible and displays Tamil category pill (not raw "Sports")
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("social_post_card_101"))
        composeTestRule.onNodeWithTag("social_post_card_101").assertIsDisplayed()
        composeTestRule.onNode(
            hasText(taStrings.socialCategories.sports) and hasAnyAncestor(hasTestTag("social_post_card_101"))
        ).assertIsDisplayed()
        assertTrue(
            "Raw English 'Sports' label should not appear on Tamil HomeSocialFeedScreen",
            composeTestRule.onAllNodesWithText("Sports").fetchSemanticsNodes().isEmpty()
        )

        // Switch to empty category ("Events") -> empty state title uses localized Tamil category name
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_social_feed"))
        composeTestRule.onNodeWithTag("home_feed_category_row")
            .performScrollToNode(hasTestTag("feed_filter_events"))
        composeTestRule.onNodeWithTag("feed_filter_events")
            .assertTextEquals(taStrings.socialCategories.events)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_feed_empty_card"))
        composeTestRule.onNode(
            hasText(taStrings.formatCategoryEmptyTitle(taStrings.socialCategories.events)) and
                hasAnyAncestor(hasTestTag("home_feed_empty_card"))
        ).assertIsDisplayed()
    }

    @Test
    fun `HomeSocialFeedScreen displays Hindi category chips and post badge while filtering by raw category ID`() {
        val hiStrings = stringsFor(AppLanguage.HINDI)
        val sampleProfile = SchoolProfileEntity(
            schoolName = "CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL",
            shortName = "Chevalier Academy",
            motto = "Learn to Live & Love",
            addressLine = "Siluvathur Road, Dindigul - 624 306",
            emblemMark = "CA",
            poweredBy = "POWERED BY JK LABS",
            welcomeIntroduction = "Welcome to Chevalier Academy."
        )
        val academicsPost = SchoolPostEntity(
            id = 202L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.ACADEMICS,
            title = "Science Exhibition Winners",
            description = "Senior secondary showcase",
            youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = 1_750_000_000_000L
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides hiStrings) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HomeSocialFeedScreen(
                        profile = sampleProfile,
                        heroPhotos = emptyList(),
                        publishedPosts = listOf(academicsPost),
                        publishedHighlights = emptyList(),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_social_feed"))
        composeTestRule.onNodeWithTag("feed_filter_academics")
            .assertTextEquals(hiStrings.socialCategories.academics)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("social_post_card_202"))
        composeTestRule.onNodeWithTag("social_post_card_202").assertIsDisplayed()
        composeTestRule.onNode(
            hasText(hiStrings.socialCategories.academics) and hasAnyAncestor(hasTestTag("social_post_card_202"))
        ).assertIsDisplayed()
        assertTrue(
            "Raw English 'Academics' label should not appear on Hindi HomeSocialFeedScreen",
            composeTestRule.onAllNodesWithText("Academics").fetchSemanticsNodes().isEmpty()
        )
    }

    // ==================== 3. SCHOOL FEED ADMIN & COMPOSER LOCALIZATION (TASKS A & B) ====================

    @Test
    fun `SchoolFeedAdminScreen and SchoolFeedPostComposer render Tamil labels and validation errors`() {
        loginMasterAdmin()
        val taAppStrings = stringsFor(AppLanguage.TAMIL)
        val taMgmtStrings = managementStringsFor(AppLanguage.TAMIL)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides taAppStrings) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    SchoolFeedAdminScreen(
                        managementViewModel = managementViewModel,
                        mStrings = taMgmtStrings
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Admin screen tabs, filters, empty state in Tamil
        composeTestRule.onNodeWithTag("school_feed_tab_posts")
            .assertTextContains(taMgmtStrings.schoolFeed.postsTabLabel(0))
        composeTestRule.onNodeWithTag("school_feed_tab_highlights")
            .assertTextContains(taMgmtStrings.schoolFeed.highlightsTabLabel(0))
        composeTestRule.onNodeWithTag("post_filter_all")
            .assertTextEquals(taMgmtStrings.schoolFeed.filterAll)
        composeTestRule.onNodeWithTag("post_filter_archived")
            .assertTextEquals(taMgmtStrings.schoolFeed.filterArchived)
        composeTestRule.onNodeWithText(taMgmtStrings.schoolFeed.createFirstPostBtn)
            .assertIsDisplayed()

        // Open Post Composer dialog -> verify Tamil Post Type chips, Category chips & localized validation error
        composeTestRule.onNodeWithTag("mgmt_fab_add_school_posts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("school_post_composer_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText(taMgmtStrings.schoolFeed.newPostTitle).assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_type_chip_youtube_direct")
            .assertTextEquals(taMgmtStrings.schoolFeed.postTypeYouTubeDirect)
        composeTestRule.onNodeWithTag("post_type_chip_admin_custom_video")
            .assertTextEquals(taMgmtStrings.schoolFeed.postTypeCustomVideo)
        composeTestRule.onNodeWithTag("post_type_chip_photo")
            .assertTextEquals(taMgmtStrings.schoolFeed.postTypePhoto)
        composeTestRule.onNodeWithTag("post_category_chip_sports")
            .performScrollTo()
            .assertTextEquals(taMgmtStrings.schoolFeed.categorySports)

        // Trigger validation error on empty Photo Post title -> must be localized in Tamil
        composeTestRule.onNodeWithTag("editor_save_draft_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(taMgmtStrings.schoolFeed.titleRequiredPhotoPostError)
            .assertIsDisplayed()
    }

    @Test
    fun `SchoolFeedAdminScreen Highlights tab and Highlight Composer render Hindi labels and validation errors`() {
        loginMasterAdmin()
        val hiAppStrings = stringsFor(AppLanguage.HINDI)
        val hiMgmtStrings = managementStringsFor(AppLanguage.HINDI)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides hiAppStrings) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    SchoolFeedAdminScreen(
                        managementViewModel = managementViewModel,
                        mStrings = hiMgmtStrings,
                        initialTab = 1
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("school_feed_tab_highlights")
            .assertTextContains(hiMgmtStrings.schoolFeed.highlightsTabLabel(0))
        composeTestRule.onNodeWithTag("highlight_filter_active")
            .assertTextEquals(hiMgmtStrings.schoolFeed.filterActive24h)
        composeTestRule.onNodeWithTag("highlight_filter_expired")
            .assertTextEquals(hiMgmtStrings.schoolFeed.filterExpired)

        // Open Highlight Composer -> verify Hindi title, info banner, and validation error
        composeTestRule.onNodeWithTag("mgmt_fab_add_school_highlights").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("school_highlight_composer_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText(hiMgmtStrings.schoolFeed.newHighlightTitle).assertIsDisplayed()

        // Submit empty Shorts URL -> verify Hindi validation error
        composeTestRule.onNodeWithTag("editor_save_draft_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(hiMgmtStrings.schoolFeed.invalidShortsUrlError)
            .assertIsDisplayed()
    }

    // ==================== 4. BACKUP SCREEN TENANT ID LOCALIZATION (TASK D) ====================

    @Test
    fun `BackupRestoreScreen localizes Tenant ID and Auth labels in Tamil while preserving raw tenantId`() {
        loginMasterAdmin()
        val taAppStrings = stringsFor(AppLanguage.TAMIL)
        val taMgmtStrings = managementStringsFor(AppLanguage.TAMIL)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides taAppStrings) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    BackupRestoreScreen(
                        managementViewModel = managementViewModel,
                        mStrings = taMgmtStrings
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("backend_status_tenant_id")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(taMgmtStrings.backendStatusTenantId(SchoolTenantContext.DEFAULT_SCHOOL_ID))

        composeTestRule.onNode(
            hasText(taMgmtStrings.backendStatusAuthModeLabel, substring = true) and
                hasAnyAncestor(hasTestTag("backend_status_card"))
        ).assertIsDisplayed()
        assertTrue(
            "Hardcoded English 'Tenant ID:' should not appear in Tamil BackupRestoreScreen",
            composeTestRule.onAllNodesWithText("Tenant ID:", substring = true).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun `BackupRestoreScreen localizes Tenant ID and Auth labels in Hindi while preserving raw tenantId`() {
        loginMasterAdmin()
        val hiAppStrings = stringsFor(AppLanguage.HINDI)
        val hiMgmtStrings = managementStringsFor(AppLanguage.HINDI)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides hiAppStrings) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    BackupRestoreScreen(
                        managementViewModel = managementViewModel,
                        mStrings = hiMgmtStrings
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("backend_status_tenant_id")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(hiMgmtStrings.backendStatusTenantId(SchoolTenantContext.DEFAULT_SCHOOL_ID))

        composeTestRule.onNode(
            hasText(hiMgmtStrings.backendStatusAuthModeLabel, substring = true) and
                hasAnyAncestor(hasTestTag("backend_status_card"))
        ).assertIsDisplayed()
    }
}
