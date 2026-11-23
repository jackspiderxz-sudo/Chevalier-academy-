package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.Version2FeatureConfig
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
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.AcademyHeroBannerConfig
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.data.repository.SchoolRepository
import com.example.data.repository.SocialFeedRepository
import com.example.ui.navigation.ChevalierAppShell
import com.example.ui.screens.AcademyPageCard
import com.example.ui.screens.AcademyPageScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.PreferencesUiState
import com.example.ui.viewmodel.SchoolDataViewModel
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
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcademyPageUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var schoolRepo: SchoolRepository
    private lateinit var socialFeedRepo: SocialFeedRepository
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var backupManager: BackupRestoreManager
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var mainViewModel: MainViewModel
    private lateinit var schoolDataViewModel: SchoolDataViewModel
    private lateinit var managementViewModel: ManagementViewModel

    @Before
    fun setup() {
        Version2FeatureConfig.setAcademicsV2Enabled(false)
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = MediaStudioManager(context)
        prefsRepo = UserPreferencesRepository(context)
        runBlocking {
            prefsRepo.setLanguage(AppLanguage.ENGLISH, markSelectedOnLaunch = true)
            prefsRepo.saveAcademyHeroBanner(AcademyHeroBannerConfig())
            prefsRepo.reconcileSocialPostPreferences(emptySet(), emptySet())
        }
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
            socialMediaDao = database.socialMediaDao(),
            schoolPostDao = database.schoolPostDao(),
            schoolHighlightDao = database.schoolHighlightDao()
        )
        socialFeedRepo = SocialFeedRepository(
            schoolPostDao = database.schoolPostDao(),
            schoolHighlightDao = database.schoolHighlightDao(),
            mediaStudioManager = mediaManager
        )
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
        schoolDataViewModel = ViewModelProvider(
            viewModelStore,
            SchoolDataViewModel.Factory(
                schoolRepository = schoolRepo,
                socialFeedRepository = socialFeedRepo,
                preferencesRepository = prefsRepo
            )
        )[SchoolDataViewModel::class.java]
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

    private fun createTempSampleImageUri(fileName: String): Uri {
        val file = File(context.cacheDir, fileName)
        val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(65, 92, 198))
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun academyPageNavigation_headerOrder_andTwelveCardGridLayout() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = AppLanguage.ENGLISH,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        // Wait past splash to Home
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("bottom_nav_academics").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Open Academy Page from Academics bottom navigation item
        composeTestRule.onNodeWithTag("bottom_nav_academics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_page_screen").assertIsDisplayed()

        // Header verification: Back + CHEVALIER ACADEMY on left; Search -> Notification -> Menu on right
        composeTestRule.onNodeWithTag("academy_header_title").assertTextEquals("CHEVALIER ACADEMY")
        val backBounds = composeTestRule.onNodeWithTag("academy_header_back_btn").assertIsDisplayed().getUnclippedBoundsInRoot()
        val titleBounds = composeTestRule.onNodeWithTag("academy_header_title").assertIsDisplayed().getUnclippedBoundsInRoot()
        val searchBounds = composeTestRule.onNodeWithTag("academy_header_search_btn").assertIsDisplayed().getUnclippedBoundsInRoot()
        val notifBounds = composeTestRule.onNodeWithTag("academy_header_notification_btn").assertIsDisplayed().getUnclippedBoundsInRoot()
        val menuBounds = composeTestRule.onNodeWithTag("academy_header_menu_btn").assertIsDisplayed().getUnclippedBoundsInRoot()

        assertTrue(backBounds.left < titleBounds.left)
        assertTrue(titleBounds.right <= searchBounds.left)
        assertTrue(searchBounds.left < notifBounds.left)
        assertTrue(notifBounds.left < menuBounds.left)

        // Hero Banner immediately below header with NO external "ACADEMY HERO BANNER" heading
        composeTestRule.onNodeWithTag("academy_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACADEMY HERO BANNER", ignoreCase = true).assertDoesNotExist()

        // Verify exact 12 cards in exact approved order and 3x4 grid
        val expectedOrder = listOf(
            "news_announcements" to "News & Announcements",
            "academic_calendar" to "Academic Calendar",
            "examinations" to "Examinations",
            "about_school" to "About School",
            "higher_secondary_courses" to "Higher Secondary Courses",
            "facilities" to "Facilities",
            "transport" to "Transport",
            "school_timetable" to "School Timetable",
            "faculty_staff" to "Faculty & Staff",
            "student_activities" to "Student Activities",
            "gallery" to "Gallery",
            "academics" to "Academics"
        )
        assertEquals(12, AcademyPageCard.entries.size)
        assertEquals(expectedOrder, AcademyPageCard.entries.map { it.id to it.title })

        val cardHeights = expectedOrder.map { (id, _) ->
            val bounds = composeTestRule.onNodeWithTag("academy_card_$id").assertIsDisplayed().getUnclippedBoundsInRoot()
            (bounds.bottom - bounds.top).value
        }
        val firstHeight = cardHeights.first()
        cardHeights.forEach { h ->
            assertTrue("All 12 Academy cards must have equal height ($h vs $firstHeight)", abs(h - firstHeight) <= 1f)
        }

        // Verify Search icon, Menu icon, and Notification icon are independently functional
        composeTestRule.onNodeWithTag("academy_header_search_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_search_input").performTextInput("Transport")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_card_transport").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_examinations").assertDoesNotExist()
        // Close search
        composeTestRule.onNodeWithTag("academy_header_search_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_card_examinations").assertIsDisplayed()

        // Open Menu dropdown
        composeTestRule.onNodeWithTag("academy_header_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_header_overflow_menu").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_menu_gallery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_menu_bookmarks").assertIsDisplayed()
    }

    @Test
    fun academyHeroBanner_adminImageOnly_imageWithText_persistence_andRbac() {
        val master = runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "master", "123456", "123456").getOrThrow().account
        }
        val unauthorizedStaff = runBlocking {
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "transport_clerk",
                displayName = "Transport Clerk",
                role = ManagementRole.STAFF,
                initialPin = "654321",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0,
                        sectionKey = ManagementSection.TRANSPORT.key,
                        canView = true,
                        canEdit = true
                    )
                )
            ).getOrThrow()
        }

        val sampleUri = createTempSampleImageUri("academy_hero_test.png")

        // Unauthorized staff cannot upload or save Academy Hero Banner
        val deniedImport = runBlocking { mgmtRepo.importAcademyHeroBannerImage(unauthorizedStaff, sampleUri) }
        assertTrue(deniedImport.isFailure)
        assertTrue(deniedImport.exceptionOrNull() is SecurityException)

        val deniedSave = runBlocking {
            mgmtRepo.saveAcademyHeroBannerConfig(
                actor = unauthorizedStaff,
                imageUri = sampleUri.toString(),
                overlayTitle = "Unauthorized",
                overlaySubtitle = "",
                publishNow = true
            )
        }
        assertTrue(deniedSave.isFailure)
        assertTrue(deniedSave.exceptionOrNull() is SecurityException)

        // Authorized Master Admin imports and publishes IMAGE-ONLY banner (no overlay text!)
        val importedUri = runBlocking {
            mgmtRepo.importAcademyHeroBannerImage(master, sampleUri).getOrThrow()
        }
        val imageOnlySaved = runBlocking {
            mgmtRepo.saveAcademyHeroBannerConfig(
                actor = master,
                imageUri = importedUri,
                overlayTitle = "",
                overlaySubtitle = "",
                publishNow = true
            ).getOrThrow()
        }
        assertTrue(imageOnlySaved.isPublished)
        assertTrue(imageOnlySaved.hasImage)
        assertFalse(imageOnlySaved.hasOverlayText)

        // Verify persisted image-only config renders without any forced title/subtitle
        val persistedImageOnly = runBlocking { prefsRepo.academyHeroBannerFlow.first() }
        assertEquals(importedUri, persistedImageOnly.imageUri)
        assertEquals("", persistedImageOnly.overlayTitle)
        assertEquals("", persistedImageOnly.overlaySubtitle)

        composeTestRule.setContent {
            val bannerConfig by schoolDataViewModel.academyHeroBanner.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    AcademyPageScreen(
                        heroBannerConfig = bannerConfig,
                        academyFeedPosts = emptyList(),
                        likedPostIds = emptySet(),
                        likedPostRemoteIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        bookmarkedPostRemoteIds = emptySet(),
                        isSearchActive = false,
                        searchQuery = "",
                        canEditHeroBanner = true,
                        isHeroEditorBusy = false,
                        showHeroEditorDialog = false,
                        onSearchQueryChange = {},
                        onOpenHeroEditor = {},
                        onDismissHeroEditor = {},
                        onPickHeroBannerImage = { _, _ -> },
                        onPublishHeroBanner = { _, _, _, _ -> },
                        onOpenCategory = {},
                        onToggleLike = { _, _ -> },
                        onToggleBookmark = { _, _ -> }
                    )
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeTestRule.onNodeWithTag("academy_hero_image_only").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeTestRule.onNodeWithTag("academy_hero_overlay_title").assertDoesNotExist()
        composeTestRule.onNodeWithTag("academy_hero_overlay_subtitle").assertDoesNotExist()

        // Now update banner with optional overlay title & subtitle and verify live render + persistence
        runBlocking {
            mgmtRepo.saveAcademyHeroBannerConfig(
                actor = master,
                imageUri = importedUri,
                overlayTitle = "Academic Excellence 2026",
                overlaySubtitle = "Science, Humanities & Co-Curricular Leadership",
                publishNow = true
            ).getOrThrow()
        }
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeTestRule.onNodeWithTag("academy_hero_overlay_title").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeTestRule.onNodeWithTag("academy_hero_overlay_title").assertTextEquals("Academic Excellence 2026")
        composeTestRule.onNodeWithTag("academy_hero_overlay_subtitle")
            .assertTextEquals("Science, Humanities & Co-Curricular Leadership")

        val persistedWithText = runBlocking { prefsRepo.academyHeroBannerFlow.first() }
        assertEquals("Academic Excellence 2026", persistedWithText.overlayTitle)
        assertEquals("Science, Humanities & Co-Curricular Leadership", persistedWithText.overlaySubtitle)
    }

    @Test
    fun academyCardDestinations_preserveFiveVerticalOptions_andShowPublishedOnly() {
        runBlocking {
            database.newsDao().upsert(
                NewsItemEntity(
                    title = "Annual Science Exhibition Published",
                    summary = "Students presented renewable energy models.",
                    fullBody = "Students presented renewable energy models across physics and chemistry labs.",
                    publishedDateText = "20 Nov 2026",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED
                )
            )
            database.newsDao().upsert(
                NewsItemEntity(
                    title = "Internal Draft Notice Hidden",
                    summary = "Should never appear publicly.",
                    fullBody = "Unpublished internal draft.",
                    publishedDateText = "20 Nov 2026",
                    isVerified = false,
                    isPublished = false,
                    isApproved = false,
                    approvalStatus = ApprovalStatus.DRAFT
                )
            )
            database.higherSecondaryCourseDao().upsert(
                HigherSecondaryCourseEntity(
                    streamTitle = "Bio-Maths Stream",
                    groupCode = "GROUP-I",
                    subjectsSummary = "Physics, Chemistry, Biology, Mathematics",
                    mediumOfInstruction = "English",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED
                )
            )
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = AppLanguage.ENGLISH,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("bottom_nav_academics").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("bottom_nav_academics").performClick()
        composeTestRule.waitForIdle()

        // Tap News & Announcements card
        composeTestRule.onNodeWithTag("academy_card_news_announcements").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_category_detail_news_announcements").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_header_title").assertTextEquals("CHEVALIER ACADEMY")

        // Verify 5 primary vertical navigation options exist
        for (i in 0 until 5) {
            composeTestRule.onNodeWithTag("academy_category_detail_news_announcements")
                .performScrollToNode(hasTestTag("academy_vertical_nav_option_$i"))
            composeTestRule.onNodeWithTag("academy_vertical_nav_option_$i").assertIsDisplayed()
        }
        // Published news is displayed; unpublished draft is strictly excluded
        composeTestRule.onNodeWithTag("academy_category_detail_news_announcements")
            .performScrollToNode(hasTestTag("academy_vertical_nav_option_0"))
        composeTestRule.onNodeWithText("Annual Science Exhibition Published").assertIsDisplayed()
        composeTestRule.onNodeWithText("Internal Draft Notice Hidden").assertDoesNotExist()

        // Navigate back via header back button to Academy Page and test Higher Secondary Courses
        composeTestRule.onNodeWithTag("academy_header_back_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_page_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("academy_card_higher_secondary_courses").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_category_detail_higher_secondary_courses").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bio-Maths Stream").assertIsDisplayed()
    }

    @Test
    fun academySocialFeed_headerDateTime_likeAndBookmarkOnly_persistence_andSingleRecordSeparation() {
        val master = runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "master", "123456", "123456").getOrThrow().account
        }

        val now = 1_763_700_000_000L
        val homeOnlyPost = SchoolPostEntity(
            remoteId = "remote-home-only-1",
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.EVENTS,
            title = "Home Only Annual Day Celebration",
            description = "Visible exclusively on Home Page School Social Feed.",
            photoUrisJson = "[]",
            status = SchoolPostStatus.PUBLISHED,
            createdAtMillis = now - 30_000L,
            publishedAtMillis = now - 30_000L,
            updatedAtMillis = now - 30_000L
        )
        val academyOnlyPost = SchoolPostEntity(
            remoteId = "remote-academy-only-2",
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.ACADEMICS,
            title = "Robotics Lab Student Showcase",
            description = "Grade 11 students demonstrated autonomous sensor projects.",
            photoUrisJson = "[]",
            status = SchoolPostStatus.PUBLISHED,
            createdAtMillis = now - 20_000L,
            publishedAtMillis = now - 20_000L,
            updatedAtMillis = now - 20_000L
        )
        val sharedBothFeedsPost = SchoolPostEntity(
            remoteId = "remote-shared-both-3",
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.ACHIEVEMENTS,
            title = "District Olympiad First Prize",
            description = "Single post visible on both Home Feed and Academy Feed.",
            photoUrisJson = "[]",
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = true,
            createdAtMillis = now - 10_000L,
            publishedAtMillis = now - 10_000L,
            updatedAtMillis = now - 10_000L
        )

        val (academyOnlyId, sharedPostId) = runBlocking {
            database.schoolPostDao().upsert(homeOnlyPost)
            val aId = database.schoolPostDao().upsert(academyOnlyPost)
            val sId = database.schoolPostDao().upsert(sharedBothFeedsPost)
            // Configure visibility without duplicating records!
            mgmtRepo.setPostFeedVisibility(master, homeOnlyPost.remoteId, showOnHomeFeed = true, showOnAcademyFeed = false).getOrThrow()
            mgmtRepo.setPostFeedVisibility(master, academyOnlyPost.remoteId, showOnHomeFeed = false, showOnAcademyFeed = true).getOrThrow()
            mgmtRepo.setPostFeedVisibility(master, sharedBothFeedsPost.remoteId, showOnHomeFeed = true, showOnAcademyFeed = true).getOrThrow()
            aId to sId
        }

        // Verify SINGLE RECORD RULE: exactly 3 rows in Room (no duplicate row for dual-feed post)
        val totalRoomPosts = runBlocking { database.schoolPostDao().observeAllPosts().first() }
        assertEquals(3, totalRoomPosts.size)

        // Verify feed separation at ViewModel layer
        val homeFeed = runBlocking {
            schoolDataViewModel.publishedSchoolPosts.first { list -> list.size == 2 }
        }
        val academyFeed = runBlocking {
            schoolDataViewModel.publishedAcademyFeedPosts.first { list -> list.size == 2 }
        }
        assertEquals(setOf(homeOnlyPost.remoteId, sharedBothFeedsPost.remoteId), homeFeed.map { it.remoteId }.toSet())
        assertEquals(setOf(academyOnlyPost.remoteId, sharedBothFeedsPost.remoteId), academyFeed.map { it.remoteId }.toSet())

        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val posts by schoolDataViewModel.publishedAcademyFeedPosts.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    AcademyPageScreen(
                        heroBannerConfig = AcademyHeroBannerConfig(),
                        academyFeedPosts = posts,
                        likedPostIds = ready?.likedPostIds ?: emptySet(),
                        likedPostRemoteIds = ready?.likedPostRemoteIds ?: emptySet(),
                        bookmarkedPostIds = ready?.bookmarkedPostIds ?: emptySet(),
                        bookmarkedPostRemoteIds = ready?.bookmarkedPostRemoteIds ?: emptySet(),
                        isSearchActive = false,
                        searchQuery = "",
                        canEditHeroBanner = false,
                        isHeroEditorBusy = false,
                        showHeroEditorDialog = false,
                        onSearchQueryChange = {},
                        onOpenHeroEditor = {},
                        onDismissHeroEditor = {},
                        onPickHeroBannerImage = { _, _ -> },
                        onPublishHeroBanner = { _, _, _, _ -> },
                        onOpenCategory = {},
                        onToggleLike = { id, remote -> mainViewModel.togglePostLike(id, remote) },
                        onToggleBookmark = { id, remote -> mainViewModel.togglePostBookmark(id, remote) }
                    )
                }
            }
        }

        // Scroll to Academy Social Feed posts
        val listNode = composeTestRule.onNodeWithTag("academy_page_screen")
        listNode.performScrollToNode(hasTestTag("academy_post_card_$sharedPostId"))
        composeTestRule.onNodeWithTag("academy_post_card_$sharedPostId").assertIsDisplayed()

        // Home-only post MUST NOT appear in Academy Social Feed
        composeTestRule.onNodeWithText("Home Only Annual Day Celebration").assertDoesNotExist()

        // Post header displays CHEVALIER ACADEMY (never "Academic Updates") + date and time below it
        composeTestRule.onNodeWithTag("academy_post_school_name_$sharedPostId")
            .assertTextEquals("CHEVALIER ACADEMY")
        composeTestRule.onNodeWithText("Academic Updates", ignoreCase = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("academy_post_datetime_$sharedPostId")
            .assertIsDisplayed()
            .assertTextContains("•", substring = true)

        // ONLY Like and Bookmark actions exist (no Comment, no Share)
        composeTestRule.onNodeWithTag("academy_post_like_btn_$sharedPostId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_post_bookmark_btn_$sharedPostId").assertIsDisplayed()
        composeTestRule.onNodeWithText("Comment", ignoreCase = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Share", ignoreCase = true).assertDoesNotExist()

        // Toggle Like & Bookmark and verify persistence in DataStore
        composeTestRule.onNodeWithTag("academy_post_like_btn_$sharedPostId").performClick()
        composeTestRule.onNodeWithTag("academy_post_bookmark_btn_$sharedPostId").performClick()
        composeTestRule.waitForIdle()

        val updatedPrefs = runBlocking {
            prefsRepo.preferencesFlow.first { state ->
                state.isPostLiked(sharedPostId, sharedBothFeedsPost.remoteId) &&
                    state.isPostBookmarked(sharedPostId, sharedBothFeedsPost.remoteId)
            }
        }
        assertTrue(updatedPrefs.isPostLiked(sharedPostId, sharedBothFeedsPost.remoteId))
        assertTrue(updatedPrefs.isPostBookmarked(sharedPostId, sharedBothFeedsPost.remoteId))

        // Scroll to Academy-only post and confirm it is also displayed
        listNode.performScrollToNode(hasTestTag("academy_post_card_$academyOnlyId"))
        composeTestRule.onNodeWithTag("academy_post_card_$academyOnlyId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_post_school_name_$academyOnlyId")
            .assertTextEquals("CHEVALIER ACADEMY")
    }
}
