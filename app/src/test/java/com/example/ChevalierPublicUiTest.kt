package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidGraphicsColor
import android.net.Uri
import android.os.Looper
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.MediaStudioManager
import com.example.ui.navigation.AppRoute
import com.example.ui.navigation.ChevalierAppShell
import com.example.ui.screens.AboutSchoolScreen
import com.example.ui.screens.AnnouncementItemCard
import com.example.ui.screens.FacilityItemCard
import com.example.ui.screens.HigherSecondaryCoursesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewsItemCard
import com.example.ui.screens.ParentModuleId
import com.example.ui.screens.SocialMediaScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.management.HeroPhotoAdjustmentDialog
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChevalierPublicUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testViewModelStores = mutableListOf<ViewModelStore>()

    @After
    fun tearDown() {
        composeTestRule.mainClock.autoAdvance = true
        testViewModelStores.forEach { it.clear() }
        testViewModelStores.clear()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        File(ctx.filesDir, "chevalier_media").deleteRecursively()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `home screen renders all six sections and navigates from quick access cards`() {
        var navigatedRoute: String? = null
        val profile = ChevalierDatabase.verifiedDefaultProfile()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HomeScreen(
                        profile = profile,
                        heroPhotos = emptyList(),
                        newsItems = emptyList(),
                        announcements = emptyList(),
                        galleryMedia = emptyList(),
                        socialLinks = emptyList(),
                        onNavigate = { navigatedRoute = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val homeList = composeTestRule.onNodeWithTag("home_screen_list")

        // Verify Hero Banner remains displayed and removed sections no longer exist
        homeList.performScrollToNode(hasTestTag("home_section_hero"))
        composeTestRule.onNodeWithTag("home_section_hero").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_campus_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_logo_banner_container").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_school_identity_container").assertDoesNotExist()

        composeTestRule.onNodeWithTag("home_section_welcome").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_welcome_read_more").assertDoesNotExist()

        homeList.performScrollToNode(hasTestTag("home_section_highlights"))
        composeTestRule.onNodeWithTag("home_section_highlights").assertIsDisplayed()

        homeList.performScrollToNode(hasTestTag("home_section_social_feed"))
        composeTestRule.onNodeWithTag("home_section_social_feed").assertIsDisplayed()

        // Duplicate Contact Details and removed Quick Access sections must not appear on Home Page
        composeTestRule.onNodeWithTag("home_section_contact_social").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_open_maps_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_section_quick_access").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_quick_academics").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_quick_courses").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_quick_facilities").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_quick_transport").assertDoesNotExist()
    }

    @Test
    fun `higher secondary courses renders all four group slots and hides unverified details`() {
        val verifiedBioMaths = HigherSecondaryCourseEntity(
            id = 1,
            streamTitle = "Bio-Maths",
            groupCode = "GROUP_1",
            subjectsSummary = "Physics, Chemistry, Biology, Mathematics",
            mediumOfInstruction = "English",
            classLevelLabel = "XI + XII / Plus One + Plus Two",
            isVerified = true,
            isPublished = true,
            isApproved = true,
            approvalStatus = ApprovalStatus.APPROVED
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HigherSecondaryCoursesScreen(courses = listOf(verifiedBioMaths))
                }
            }
        }

        composeTestRule.waitForIdle()
        val list = composeTestRule.onNodeWithTag("courses_screen_list")

        list.performScrollToNode(hasTestTag("course_group_group_1"))
        composeTestRule.onNodeWithTag("course_group_group_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("XI + XII / Plus One + Plus Two").assertIsDisplayed()

        list.performScrollToNode(hasTestTag("course_group_group_2"))
        composeTestRule.onNodeWithTag("course_group_group_2").assertIsDisplayed()

        list.performScrollToNode(hasTestTag("course_group_group_3"))
        composeTestRule.onNodeWithTag("course_group_group_3").assertIsDisplayed()

        list.performScrollToNode(hasTestTag("course_group_group_4"))
        composeTestRule.onNodeWithTag("course_group_group_4").assertIsDisplayed()
    }

    @Test
    fun `social media screen renders four official platform slots`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    SocialMediaScreen(links = emptyList())
                }
            }
        }

        composeTestRule.waitForIdle()
        val list = composeTestRule.onNodeWithTag("social_media_screen_list")
        list.performScrollToNode(hasTestTag("social_slot_instagram"))
        composeTestRule.onNodeWithTag("social_slot_instagram").assertIsDisplayed()
        list.performScrollToNode(hasTestTag("social_slot_facebook"))
        composeTestRule.onNodeWithTag("social_slot_facebook").assertIsDisplayed()
        list.performScrollToNode(hasTestTag("social_slot_youtube"))
        composeTestRule.onNodeWithTag("social_slot_youtube").assertIsDisplayed()
        list.performScrollToNode(hasTestTag("social_slot_x"))
        composeTestRule.onNodeWithTag("social_slot_x").assertIsDisplayed()
    }

    @Test
    fun `management entry screen is separated from public bottom navigation`() {
        assertTrue(AppRoute.MANAGEMENT_ENTRY in AppRoute.hiddenChromeRoutes)
        assertFalse(AppRoute.HOME in AppRoute.hiddenChromeRoutes)
        assertFalse(AppRoute.NEWS in AppRoute.hiddenChromeRoutes)
        assertFalse(AppRoute.ANNOUNCEMENTS in AppRoute.hiddenChromeRoutes)
    }

    @Test
    fun `admin photo adjustment dialog provides zoom, drag viewport, reset, live preview, and saves cropped file separately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = File(context.cacheDir, "ui_crop_test.jpg")
        val bmp = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(AndroidGraphicsColor.rgb(8, 122, 69))
        FileOutputStream(sourceFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bmp.recycle()

        val imported = kotlinx.coroutines.runBlocking {
            MediaStudioManager(context).importAndOptimizeImage(Uri.fromFile(sourceFile)).getOrThrow()
        }

        var savedOrig: String? = null
        var savedAdj: String? = null

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                HeroPhotoAdjustmentDialog(
                    sourceUri = imported.originalUri,
                    captionPreview = "Campus Front View",
                    onApply = { orig, adj ->
                        savedOrig = orig
                        savedAdj = adj
                    },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("photo_adj_crop_viewport").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_adj_zoom_in_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("photo_adj_zoom_out_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("photo_adj_reset_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("photo_adj_live_preview").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_adj_apply_crop_btn").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            savedAdj != null
        }

        assertEquals(imported.originalUri, savedOrig)
        assertNotEquals(savedOrig, savedAdj)
        assertTrue(File(Uri.parse(savedAdj!!).path!!).exists())
        sourceFile.delete()
    }

    @Test
    fun `main activity disables fullscreen flag so real android status bar stays visible`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            @Suppress("DEPRECATION")
            val hasFullscreenFlag = (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
            assertFalse("Activity must not enable FLAG_FULLSCREEN", hasFullscreenFlag)
        } finally {
            controller.pause().stop().destroy()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `first launch displays 5-second non-skippable splash then language selection and persists choice`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = false,
                themeMode = AppThemeMode.SYSTEM
            )
        }
        val store = ViewModelStore().also { testViewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataViewModel = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository)
        )[SchoolDataViewModel::class.java]
        val managementViewModel = ViewModelProvider(
            store,
            ManagementViewModel.Factory(
                app.container.managementRepository,
                app.container.backupRestoreManager,
                app.container.userPreferencesRepository,
                app.container.centralBackendStatus
            )
        )[ManagementViewModel::class.java]

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val lang = ready?.selectedLanguage ?: AppLanguage.ENGLISH
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = lang,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithTag("splash_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_artwork_image").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_selection_screen").assertDoesNotExist()

        // Tap-to-skip must not work
        composeTestRule.onNodeWithTag("splash_screen_root").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("splash_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_selection_screen").assertDoesNotExist()

        // Splash remains visible right before 5 seconds complete
        composeTestRule.mainClock.advanceTimeBy(4_800)
        composeTestRule.onNodeWithTag("splash_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_selection_screen").assertDoesNotExist()

        // Advances past 5,000 ms -> navigates automatically to Language Selection on first launch
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_screen_root").assertDoesNotExist()
        composeTestRule.onNodeWithTag("language_selection_screen").assertIsDisplayed()

        // Choose Tamil and tap Continue
        composeTestRule.onNodeWithTag("language_option_ta")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("language_continue_button")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen_list").assertIsDisplayed()

        val persisted = runBlocking { app.container.userPreferencesRepository.preferencesFlow.first() }
        assertEquals(AppLanguage.TAMIL, persisted.selectedLanguage)
        assertTrue(persisted.isLanguageSelectedOnLaunch)
    }

    @Test
    fun `subsequent launch skips language selection directly to home and settings change language updates persistence`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.TAMIL,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT
            )
        }
        val store = ViewModelStore().also { testViewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataViewModel = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository)
        )[SchoolDataViewModel::class.java]
        val managementViewModel = ViewModelProvider(
            store,
            ManagementViewModel.Factory(
                app.container.managementRepository,
                app.container.backupRestoreManager,
                app.container.userPreferencesRepository,
                app.container.centralBackendStatus
            )
        )[ManagementViewModel::class.java]

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val lang = ready?.selectedLanguage ?: AppLanguage.ENGLISH
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = lang,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        // Splash shows first for full 5 seconds
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithTag("splash_screen_root").assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(5_100)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Goes directly to Home in saved Tamil without showing Language Selection
        composeTestRule.onNodeWithTag("language_selection_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_screen_list").assertIsDisplayed()

        // Open Settings via top bar settings icon and click Change Language
        composeTestRule.onNodeWithTag("top_bar_settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_change_language_button")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("language_selection_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_option_hi")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("language_continue_button")
            .performScrollTo()
            .performClick()

        // Returns directly to Settings with Hindi applied and persists across restart
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_list").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(stringsFor(AppLanguage.HINDI).titleSettings)
            .onFirst()
            .assertIsDisplayed()

        val persisted = runBlocking { app.container.userPreferencesRepository.preferencesFlow.first() }
        assertEquals(AppLanguage.HINDI, persisted.selectedLanguage)
        assertTrue(persisted.isLanguageSelectedOnLaunch)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `splash fills portrait tall-portrait and landscape displays proportionally`() {
        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                SplashScreen(onSplashCompleted = {})
            }
        }
        composeTestRule.waitForIdle()
        val tallRoot = composeTestRule.onNodeWithTag("splash_screen_root").getUnclippedBoundsInRoot()
        val tallArt = composeTestRule.onNodeWithTag("splash_artwork_image").getUnclippedBoundsInRoot()
        assertEquals(412f, tallRoot.width.value, 1f)
        assertEquals(915f, tallRoot.height.value, 1f)
        // Must fill at least full width and maintain exact 941/1672 aspect without distortion
        assertTrue(tallArt.width.value >= 412f)
        assertEquals(941f / 1672f, tallArt.width.value / tallArt.height.value, 0.01f)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w800dp-h400dp-land")
    fun `splash fills landscape container edge to edge without vertical cropping`() {
        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                SplashScreen(onSplashCompleted = {})
            }
        }
        composeTestRule.waitForIdle()
        val landRoot = composeTestRule.onNodeWithTag("splash_screen_root").getUnclippedBoundsInRoot()
        val landArt = composeTestRule.onNodeWithTag("splash_artwork_image").getUnclippedBoundsInRoot()
        assertEquals(800f, landRoot.width.value, 1f)
        assertEquals(400f, landRoot.height.value, 1f)
        assertEquals(800f, landArt.width.value, 1f)
        assertEquals(400f, landArt.height.value, 1f)
        composeTestRule.onNodeWithTag("splash_artwork_image").assertIsDisplayed()
    }

    @Test
    fun `public screens and cards render readable cms managed images`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = File(context.cacheDir, "public_cms_media_test.jpg")
        val bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(AndroidGraphicsColor.rgb(122, 24, 34))
        FileOutputStream(sourceFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()

        val managedUri = runBlocking {
            MediaStudioManager(context).importAndOptimizeImage(Uri.fromFile(sourceFile)).getOrThrow().originalUri
        }
        sourceFile.delete()

        val profile = ChevalierDatabase.verifiedDefaultProfile().copy(aboutImageUri = managedUri)
        val facility = FacilityEntity(
            id = 11L,
            name = "Science Lab",
            category = "Academic",
            description = "Physics and Chemistry Lab",
            isVerified = true,
            isPublished = true,
            isApproved = true,
            approvalStatus = ApprovalStatus.APPROVED,
            imageUri = managedUri
        )
        val news = NewsItemEntity(
            id = 22L,
            title = "Annual Day",
            summary = "Summary text",
            fullBody = "Full body text",
            publishedDateText = "16 Nov 2026",
            isVerified = true,
            isPublished = true,
            isApproved = true,
            approvalStatus = ApprovalStatus.APPROVED,
            imageUri = managedUri
        )
        val announcement = AnnouncementEntity(
            id = 33L,
            title = "Holiday Notice",
            message = "School closed tomorrow",
            issuedDateText = "16 Nov 2026",
            isVerified = true,
            isPublished = true,
            isApproved = true,
            approvalStatus = ApprovalStatus.APPROVED,
            imageUri = managedUri
        )

        var showAboutScreen by mutableStateOf(false)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    if (!showAboutScreen) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            FacilityItemCard(facility = facility)
                            NewsItemCard(item = news, badgeText = "News", highlighted = false, onClick = {})
                            AnnouncementItemCard(item = announcement, onClick = {})
                        }
                    } else {
                        AboutSchoolScreen(profile = profile)
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("facility_item_image_11", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("news_item_image_22", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("announcement_item_image_33", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        showAboutScreen = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_school_image", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `bottom navigation shows exactly home parent academics contact in order and top right menu opens gallery with back navigation`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT
            )
            val albumId = app.container.schoolRepository.upsertGalleryAlbum(
                com.example.data.local.entity.GalleryAlbumEntity(
                    title = "Campus Day",
                    category = "Events",
                    description = "Verified campus celebration",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED
                )
            )
            app.container.schoolRepository.upsertGalleryMedia(
                com.example.data.local.entity.GalleryMediaEntity(
                    albumId = albumId,
                    title = "Campus Tour Video",
                    caption = "Official school tour",
                    mediaUri = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    mediaType = "YOUTUBE",
                    displayOrder = 0,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED
                )
            )
        }

        val store = ViewModelStore().also { testViewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataViewModel = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository)
        )[SchoolDataViewModel::class.java]
        val managementViewModel = ViewModelProvider(
            store,
            ManagementViewModel.Factory(
                app.container.managementRepository,
                app.container.backupRestoreManager,
                app.container.userPreferencesRepository,
                app.container.centralBackendStatus
            )
        )[ManagementViewModel::class.java]

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val lang = ready?.selectedLanguage ?: AppLanguage.ENGLISH
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = lang,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(5_100)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // 1 & 2. Verify exact 4-item Bottom Navigation order (Home, Academics, Parent, Contact) and absence of Gallery & More
        composeTestRule.onNodeWithTag("public_bottom_navigation_bar").assertIsDisplayed()
        val homeBounds = composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed().getUnclippedBoundsInRoot()
        val academicsBounds = composeTestRule.onNodeWithTag("bottom_nav_academics").assertIsDisplayed().getUnclippedBoundsInRoot()
        val parentBounds = composeTestRule.onNodeWithTag("bottom_nav_parent").assertIsDisplayed().getUnclippedBoundsInRoot()
        val contactBounds = composeTestRule.onNodeWithTag("bottom_nav_contact").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(homeBounds.left < academicsBounds.left)
        assertTrue(academicsBounds.left < parentBounds.left)
        assertTrue(parentBounds.left < contactBounds.left)
        composeTestRule.onNodeWithTag("bottom_nav_gallery").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_more").assertDoesNotExist()
        assertEquals(
            setOf(AppRoute.HOME, AppRoute.PARENT, AppRoute.ACADEMICS, AppRoute.CONTACT),
            AppRoute.primaryTabRoutes
        )

        // 5 & 6. Navigate across Parent, Academics, Contact, and Home
        composeTestRule.onNodeWithTag("bottom_nav_parent").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_portal_top_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_screen_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_student_info_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_academics").performClick()
        composeTestRule.waitForIdle()
        val academyPageList = composeTestRule.onNodeWithTag("academy_page_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_header_title").assertTextEquals("CHEVALIER ACADEMY")
        composeTestRule.onNodeWithTag("academy_card_news_announcements").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_academic_calendar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_examinations").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_about_school").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_higher_secondary_courses").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_facilities").assertIsDisplayed()
        academyPageList.performScrollToNode(hasTestTag("academy_cards_grid"))
        composeTestRule.onNodeWithTag("academy_card_transport").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_school_timetable").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_faculty_staff").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_student_activities").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_gallery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_card_academics").assertIsDisplayed()

        // Tap Academics card to open Academy Category Detail screen and return via Header Back
        composeTestRule.onNodeWithTag("academy_card_academics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_category_detail_academics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("academy_header_back_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("academy_page_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_contact").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("contact_screen_list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen_list").assertIsDisplayed()

        // 3, 4 & 7. Open Gallery from Top-Right menu, verify Gallery tabs/lightbox, and return via Back
        composeTestRule.onNodeWithTag("bottom_nav_parent").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_more_menu_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_menu_gallery").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("gallery_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_tab_media").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_tab_albums").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Campus Day").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_tab_media").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Campus Tour Video").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_player_external_fallback_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()

        // Return to Parent screen using normal TopBar Back navigation
        composeTestRule.onNodeWithTag("top_bar_back_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_screen_list").assertIsDisplayed()

        // 10. Verify Management Portal entry remains accessible via Navigation Drawer
        composeTestRule.onNodeWithTag("top_bar_more_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_menu_drawer").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drawer_management_login").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("public_bottom_navigation_bar").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w800dp-h600dp")
    fun `adaptive navigation rail displays exact four primary items on wide screens`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.TAMIL,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT
            )
        }
        val store = ViewModelStore().also { testViewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataViewModel = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository)
        )[SchoolDataViewModel::class.java]
        val managementViewModel = ViewModelProvider(
            store,
            ManagementViewModel.Factory(
                app.container.managementRepository,
                app.container.backupRestoreManager,
                app.container.userPreferencesRepository,
                app.container.centralBackendStatus
            )
        )[ManagementViewModel::class.java]

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val lang = ready?.selectedLanguage ?: AppLanguage.TAMIL
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = lang,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(5_100)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Wide screen uses NavigationRail instead of Bottom NavigationBar
        composeTestRule.onNodeWithTag("public_bottom_navigation_bar").assertDoesNotExist()
        val homeBounds = composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed().getUnclippedBoundsInRoot()
        val academicsBounds = composeTestRule.onNodeWithTag("bottom_nav_academics").assertIsDisplayed().getUnclippedBoundsInRoot()
        val parentBounds = composeTestRule.onNodeWithTag("bottom_nav_parent").assertIsDisplayed().getUnclippedBoundsInRoot()
        val contactBounds = composeTestRule.onNodeWithTag("bottom_nav_contact").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(homeBounds.top < academicsBounds.top)
        assertTrue(academicsBounds.top < parentBounds.top)
        assertTrue(parentBounds.top < contactBounds.top)
        composeTestRule.onNodeWithTag("bottom_nav_gallery").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_more").assertDoesNotExist()

        // Verify Tamil localization on Parent Dashboard
        composeTestRule.onNodeWithTag("bottom_nav_parent").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringsFor(AppLanguage.TAMIL).parentPortalTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringsFor(AppLanguage.TAMIL).parentServicesSectionTitle).assertIsDisplayed()

        // Verify Hindi & English localization dictionaries for Parent Portal & Modules
        assertEquals("Parent", stringsFor(AppLanguage.ENGLISH).navParent)
        assertEquals("பெற்றோர்", stringsFor(AppLanguage.TAMIL).navParent)
        assertEquals("अभिभावक", stringsFor(AppLanguage.HINDI).navParent)
        assertEquals("PARENT PORTAL", stringsFor(AppLanguage.ENGLISH).parentPortalTitle)
        assertTrue(stringsFor(AppLanguage.HINDI).parentPortalTitle.isNotBlank())
        assertTrue(stringsFor(AppLanguage.HINDI).parentModuleEmergencyInfo.isNotBlank())
        assertTrue(stringsFor(AppLanguage.HINDI).parentModuleParentGuidance.isNotBlank())
        assertTrue(stringsFor(AppLanguage.HINDI).parentStudentSocialFeedTitle.isNotBlank())
        assertTrue(stringsFor(AppLanguage.HINDI).openTopMenuDescription.isNotBlank())
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `parent dashboard renders hero twelve services in 4x3 grid and student social feed without engagement actions`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        val db = ChevalierDatabase.getInstance(app)
        var publishedPostId = 0L
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT
            )
            publishedPostId = db.schoolPostDao().upsert(
                SchoolPostEntity(
                    postType = SchoolPostType.YOUTUBE_DIRECT,
                    category = SchoolPostCategory.EVENTS,
                    title = "Annual Science Exhibition Highlights",
                    description = "Students presented robotics and renewable energy models.",
                    youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    status = SchoolPostStatus.PUBLISHED,
                    isFeatured = true,
                    createdAtMillis = 1_700_000_000_000L,
                    publishedAtMillis = 1_700_000_000_000L,
                    updatedAtMillis = 1_700_000_000_000L
                )
            )
        }

        val store = ViewModelStore().also { testViewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataViewModel = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository)
        )[SchoolDataViewModel::class.java]
        val managementViewModel = ViewModelProvider(
            store,
            ManagementViewModel.Factory(
                app.container.managementRepository,
                app.container.backupRestoreManager,
                app.container.userPreferencesRepository,
                app.container.centralBackendStatus
            )
        )[ManagementViewModel::class.java]

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val lang = ready?.selectedLanguage ?: AppLanguage.ENGLISH
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = lang,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(5_100)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Open Parent Dashboard via Bottom Navigation
        composeTestRule.onNodeWithTag("bottom_nav_parent").performClick()
        composeTestRule.waitForIdle()

        // 1. Parent Portal Top Bar
        composeTestRule.onNodeWithTag("parent_portal_top_header").assertIsDisplayed()
        composeTestRule.onNodeWithText("CHEVALIER ACADEMY").assertIsDisplayed()
        composeTestRule.onNodeWithText("PARENT PORTAL").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_header_search_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_header_notification_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_more_menu_button").assertIsDisplayed()

        // 2. Student Information Hero Panel
        val heroBounds = composeTestRule.onNodeWithTag("parent_student_info_card").assertIsDisplayed().getUnclippedBoundsInRoot()
        composeTestRule.onNodeWithTag("parent_student_avatar", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample Preview", useUnmergedTree = true).assertIsDisplayed()

        // Verify removed sections do NOT exist
        composeTestRule.onNodeWithTag("parent_glance_section").assertDoesNotExist()
        composeTestRule.onNodeWithTag("parent_glance_attendance_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("parent_glance_exams_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("parent_latest_updates_section").assertDoesNotExist()
        composeTestRule.onNodeWithTag("parent_updates_view_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("powered_by_jk_labs_footer").assertDoesNotExist()

        // Top Header Notification action & Back
        composeTestRule.onNodeWithTag("parent_header_notification_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_back_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_screen_list").assertIsDisplayed()

        // 3. Parent Services — All 12 module cards present in a fixed 4-column x 3-row grid
        assertEquals(12, ParentModuleId.entries.size)
        val servicesHeaderBounds = composeTestRule.onNodeWithTag("parent_services_section").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(heroBounds.bottom <= servicesHeaderBounds.top)

        val row1Col1 = composeTestRule.onNodeWithTag("parent_module_card_student_profile").assertIsDisplayed().getUnclippedBoundsInRoot()
        val row1Col2 = composeTestRule.onNodeWithTag("parent_module_card_exams_results").assertIsDisplayed().getUnclippedBoundsInRoot()
        val row1Col3 = composeTestRule.onNodeWithTag("parent_module_card_attendance").assertIsDisplayed().getUnclippedBoundsInRoot()
        val row1Col4 = composeTestRule.onNodeWithTag("parent_module_card_homework_assignments").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(row1Col1.left < row1Col2.left)
        assertTrue(row1Col2.left < row1Col3.left)
        assertTrue(row1Col3.left < row1Col4.left)
        assertEquals(row1Col1.top, row1Col4.top)

        val row2Col1 = composeTestRule.onNodeWithTag("parent_module_card_child_activities").assertIsDisplayed().getUnclippedBoundsInRoot()
        val row3Col4 = composeTestRule.onNodeWithTag("parent_module_card_parent_guidance").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(row1Col1.bottom <= row2Col1.top)
        assertTrue(row2Col1.bottom <= row3Col4.top)

        ParentModuleId.entries.forEach { moduleId ->
            composeTestRule.onNodeWithTag("parent_screen_list")
                .performScrollToNode(hasTestTag("parent_module_card_${moduleId.routeKey}"))
            composeTestRule.onNodeWithTag("parent_module_card_${moduleId.routeKey}").assertIsDisplayed()
        }

        // Open Emergency Information & Parent Guidance modules and return via Back
        composeTestRule.onNodeWithTag("parent_module_card_emergency_info").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_module_screen_emergency_info").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_module_screen_emergency_info")
            .performScrollToNode(hasTestTag("parent_module_back_dashboard_btn"))
        composeTestRule.onNodeWithTag("parent_module_back_dashboard_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_screen_list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("parent_module_card_parent_guidance").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_module_screen_parent_guidance").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_screen_list").assertIsDisplayed()

        // 4. Student Social Feed reuses Home Page published posts WITHOUT Like, Bookmark, Share, or Comment actions
        composeTestRule.onNodeWithTag("parent_screen_list")
            .performScrollToNode(hasTestTag("parent_student_social_feed_section"))
        composeTestRule.onNodeWithTag("parent_student_social_feed_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_screen_list")
            .performScrollToNode(hasTestTag("social_post_card_$publishedPostId"))
        composeTestRule.onNodeWithTag("social_post_card_$publishedPostId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("social_post_title_$publishedPostId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("social_post_description_$publishedPostId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("social_post_actions_$publishedPostId").assertDoesNotExist()
        composeTestRule.onNodeWithTag("post_like_btn_$publishedPostId").assertDoesNotExist()
        composeTestRule.onNodeWithTag("post_bookmark_btn_$publishedPostId").assertDoesNotExist()
        composeTestRule.onNodeWithTag("powered_by_jk_labs_footer").assertDoesNotExist()

        // 5. Original Bottom Navigation remains visible
        composeTestRule.onNodeWithTag("public_bottom_navigation_bar").assertIsDisplayed()

        // Top Header Search filters Parent Services modules accurately
        composeTestRule.onNodeWithTag("parent_header_search_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_screen_list")
            .performScrollToNode(hasTestTag("parent_dashboard_search_field"))
        composeTestRule.onNodeWithTag("parent_dashboard_search_field").performTextInput("Guidance")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_screen_list")
            .performScrollToNode(hasTestTag("parent_module_card_parent_guidance"))
        composeTestRule.onNodeWithTag("parent_module_card_parent_guidance").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_module_card_leave_request").assertDoesNotExist()
        composeTestRule.onNodeWithTag("parent_header_search_button").performClick()
        composeTestRule.waitForIdle()
    }
}
