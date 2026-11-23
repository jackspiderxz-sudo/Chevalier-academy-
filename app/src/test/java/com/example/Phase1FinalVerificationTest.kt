package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.data.repository.SchoolRepository
import com.example.ui.navigation.AppRoute
import com.example.ui.navigation.ChevalierAppShell
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.management.ManagementPortalScreen
import com.example.ui.screens.management.MediaStudioScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.PreferencesUiState
import com.example.ui.viewmodel.SchoolDataViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class Phase1FinalVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var schoolRepo: SchoolRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager
    private val viewModelStores = mutableListOf<ViewModelStore>()
    private lateinit var managementViewModel: ManagementViewModel
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = (context.applicationContext as ChevalierApplication).container.mediaStudioManager
        mgmtRepo = ManagementRepository(database, mediaManager)
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
        prefsRepo = UserPreferencesRepository(context)
        backupManager = BackupRestoreManager(context, database, prefsRepo)
        val store = ViewModelStore().also { viewModelStores.add(it) }
        mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(prefsRepo)
        )[MainViewModel::class.java]
        managementViewModel = ViewModelProvider(
            store,
            ManagementViewModel.Factory(mgmtRepo, backupManager)
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        composeTestRule.mainClock.autoAdvance = true
        viewModelStores.forEach { it.clear() }
        viewModelStores.clear()
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

    @Test
    fun `gallery category selection clears album filter and lightbox navigation stays within filtered photos`() {
        val album1 = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 10L,
                title = "Annual Sports",
                category = "Sports",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(
                GalleryMediaEntity(
                    id = 101L,
                    albumId = 10L,
                    title = "Relay Start",
                    caption = "Track event",
                    mediaUri = "https://example.com/relay1.jpg",
                    mediaType = "IMAGE",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true
                ),
                GalleryMediaEntity(
                    id = 102L,
                    albumId = 10L,
                    title = "Relay Finish",
                    caption = "Medal ceremony",
                    mediaUri = "https://example.com/relay2.jpg",
                    mediaType = "IMAGE",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true
                )
            )
        )
        val album2 = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 20L,
                title = "Science Fair",
                category = "Academics",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(
                GalleryMediaEntity(
                    id = 201L,
                    albumId = 20L,
                    title = "Solar Model",
                    caption = "Physics booth",
                    mediaUri = "https://example.com/solar.jpg",
                    mediaType = "IMAGE",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    GalleryScreen(albums = listOf(album1, album2))
                }
            }
        }
        composeTestRule.waitForIdle()

        // Select Album 1 from Albums tab
        composeTestRule.onNodeWithTag("gallery_tab_albums").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_album_card_10").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("gallery_selected_album_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_102").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_201").assertDoesNotExist()

        // Open first photo in lightbox: counter and prev/next stay strictly within the 2 filtered photos
        composeTestRule.onNodeWithTag("gallery_photo_item_101").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_lightbox_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Relay Start").assertIsDisplayed()

        composeTestRule.onNodeWithTag("lightbox_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 / 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Relay Finish").assertIsDisplayed()

        // Next from index 1 wraps to index 0 (never enters album2 photo 201)
        composeTestRule.onNodeWithTag("lightbox_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Relay Start").assertIsDisplayed()

        composeTestRule.onNodeWithTag("lightbox_prev_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 / 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Relay Finish").assertIsDisplayed()

        composeTestRule.onNodeWithTag("lightbox_close_button").performClick()
        composeTestRule.waitForIdle()

        // Selecting a category chip clears the selected album filter and shows matching category items
        composeTestRule.onNodeWithTag("gallery_category_chip_academics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_selected_album_header").assertDoesNotExist()
        composeTestRule.onNodeWithTag("gallery_photo_item_201").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_101").assertDoesNotExist()
    }

    @Test
    fun `media studio hides Edit Details button when staff lacks MEDIA_STUDIO EDIT permission`() {
        val file = File(context.cacheDir, "phase1_edit_perm_ui.jpg")
        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.rgb(122, 24, 34))
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()

        val mediaId = runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
            val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
            val imported = mgmtRepo.importImageToMediaStudio(
                actor = master,
                sourceUri = Uri.fromFile(file),
                title = "Read Only Media",
                caption = "No edit rights",
                categoryOrAlbum = "Campus",
                assignedSections = emptyList(),
                isFeatured = false
            ).getOrThrow()
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "media_no_edit",
                displayName = "Media Reader",
                role = ManagementRole.STAFF,
                initialPin = "223344",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        canView = true,
                        canCreate = true,
                        canEdit = false,
                        canDelete = false,
                        canPublish = false
                    )
                )
            ).getOrThrow()
            imported.id
        }

        managementViewModel.loginWithPin("media_no_edit", "223344")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    MediaStudioScreen(
                        managementViewModel = managementViewModel,
                        mStrings = managementStringsFor(AppLanguage.ENGLISH)
                    )
                }
            }
        }

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("media_item_title_$mediaId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("media_edit_metadata_btn_$mediaId").assertDoesNotExist()
        file.delete()
    }

    @Test
    fun `parent portal top bar bookmarks shortcut navigates to BOOKMARKS route in ChevalierAppShell`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT
            )
        }
        val store = ViewModelStore().also { viewModelStores.add(it) }
        val appMainVm = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataVm = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository)
        )[SchoolDataViewModel::class.java]
        val mgmtVm = ViewModelProvider(
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
            val prefsState by appMainVm.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val lang = ready?.selectedLanguage ?: AppLanguage.ENGLISH
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = appMainVm,
                        schoolDataViewModel = schoolDataVm,
                        managementViewModel = mgmtVm,
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

        composeTestRule.onNodeWithTag("bottom_nav_parent").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_portal_top_header").assertIsDisplayed()

        composeTestRule.onNodeWithTag("top_bar_more_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_menu_bookmarks").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bookmarks_screen_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_portal_top_header").assertIsDisplayed()
    }

    @Test
    fun `super owner searchable section count reflects granted VIEW permissions and admin language change persists`() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
            val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "vice_principal",
                displayName = "Vice Principal",
                role = ManagementRole.SUPER_OWNER,
                initialPin = "654321",
                permissions = listOf(
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.SCHOOL_PROFILE.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.NEWS.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.ANNOUNCEMENTS.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.PHOTO_GALLERY.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.SCHOOL_POSTS.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.APP_SETTINGS.key, canView = true)
                )
            ).getOrThrow()
            prefsRepo.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT
            )
        }

        managementViewModel.loginWithPin("vice_principal", "654321")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }
        assertEquals(5, managementViewModel.searchableSectionCount())

        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val currentLang = ready?.selectedLanguage ?: AppLanguage.ENGLISH
            val navController = rememberNavController()
            CompositionLocalProvider(LocalAppStrings provides stringsFor(currentLang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.MANAGEMENT_ENTRY
                    ) {
                        composable(AppRoute.MANAGEMENT_ENTRY) {
                            ManagementPortalScreen(
                                managementViewModel = managementViewModel,
                                mainViewModel = mainViewModel,
                                currentLanguage = currentLang,
                                currentThemeMode = AppThemeMode.LIGHT,
                                onExitToPublicApp = {},
                                onOpenLanguageSelection = {
                                    navController.navigate(AppRoute.LANGUAGE_SELECTION)
                                }
                            )
                        }
                        composable(AppRoute.LANGUAGE_SELECTION) {
                            val previousRoute = navController.previousBackStackEntry?.destination?.route
                            val openedFromSettings =
                                previousRoute == AppRoute.SETTINGS ||
                                    previousRoute == AppRoute.MANAGEMENT_ENTRY
                            LanguageSelectionScreen(
                                selectedLanguage = currentLang,
                                onLanguagePreviewSelected = { lang ->
                                    mainViewModel.selectLanguage(lang, markOnboarded = false)
                                },
                                onConfirmLanguage = { lang ->
                                    mainViewModel.selectLanguage(lang, markOnboarded = true)
                                    if (openedFromSettings && navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("management_dashboard_grid").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Navigate to Common App -> APP_SETTINGS -> Change Language -> choose Tamil -> Continue
        composeTestRule.onNodeWithTag("management_dashboard_grid")
            .performScrollToNode(hasTestTag("admin_dash_card_common_app"))
        composeTestRule.onNodeWithTag("admin_dash_card_common_app").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_hub_open_app_settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_change_language_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("language_selection_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_option_ta").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("language_continue_button").performScrollTo().performClick()

        // Restores Management Portal APP_SETTINGS screen with Tamil applied and persisted
        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("language_selection_screen").assertDoesNotExist()
                composeTestRule.onNodeWithTag("management_dashboard_shell").assertIsDisplayed()
                composeTestRule.onNodeWithTag("settings_screen_list").assertIsDisplayed()
                composeTestRule.onAllNodesWithText(managementStringsFor(AppLanguage.TAMIL).cardAppSettings)
                    .onFirst()
                    .assertIsDisplayed()
                runBlocking { prefsRepo.preferencesFlow.first().selectedLanguage } == AppLanguage.TAMIL
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("management_dashboard_shell").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_screen_list").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(managementStringsFor(AppLanguage.TAMIL).cardAppSettings)
            .onFirst()
            .assertIsDisplayed()
        val persisted = runBlocking { prefsRepo.preferencesFlow.first() }
        assertEquals(AppLanguage.TAMIL, persisted.selectedLanguage)
    }
}
