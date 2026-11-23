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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
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
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.data.repository.SchoolRepository
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.ParentPortalTopBar
import com.example.ui.screens.management.ManagementPortalScreen
import com.example.ui.screens.management.MediaStudioScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase1Part2PermissionsAndNavTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var schoolRepo: SchoolRepository
    private lateinit var prefsRepo: UserPreferencesRepository
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
        viewModelStore = ViewModelStore()
        mainViewModel = ViewModelProvider(
            viewModelStore,
            MainViewModel.Factory(prefsRepo)
        )[MainViewModel::class.java]
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(mgmtRepo, backupManager)
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        database.close()
        File(context.filesDir, "chevalier_media").deleteRecursively()
    }

    private fun createTempImageFile(name: String): File {
        val file = File(context.cacheDir, name)
        val bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.rgb(141, 32, 41))
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bmp.recycle()
        return file
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
        assertTrue(
            "Timed out waiting for condition (vmError=${managementViewModel.errorMessage.value})",
            condition()
        )
    }

    private fun seedMasterMediaAndAlbum(): Triple<Long, Long, Long> = runBlocking {
        mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
        val imgFile = createTempImageFile("phase1_part2_gallery_perm.jpg")
        val imported = mgmtRepo.importImageToMediaStudio(
            actor = master,
            sourceUri = Uri.fromFile(imgFile),
            title = "Science Lab Inauguration",
            caption = "New physics wing",
            categoryOrAlbum = "Campus",
            assignedSections = emptyList(),
            isFeatured = false
        ).getOrThrow()
        val albumId = database.galleryDao().upsertAlbum(
            GalleryAlbumEntity(
                title = "Campus Events 2026",
                category = "Events",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        Triple(master.account.id, imported.id, albumId)
    }

    @Test
    fun `media studio hides Assign to Album when staff lacks PHOTO_GALLERY CREATE permission`() {
        val (_, mediaId, _) = seedMasterMediaAndAlbum()
        runBlocking {
            val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "media_viewer",
                displayName = "Media Viewer",
                role = ManagementRole.STAFF,
                initialPin = "112233",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        canView = true,
                        canCreate = false,
                        canEdit = false,
                        canDelete = false,
                        canPublish = false
                    ),
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.PHOTO_GALLERY.key,
                        canView = true,
                        canCreate = false,
                        canEdit = false,
                        canDelete = false,
                        canPublish = false
                    )
                )
            ).getOrThrow()
        }

        managementViewModel.loginWithPin("media_viewer", "112233")
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

        composeTestRule.onNodeWithTag("media_assign_album_btn_$mediaId").assertDoesNotExist()
    }

    @Test
    fun `media studio routes Assign to Album to approval queue when staff has CREATE without PUBLISH`() {
        val (_, mediaId, albumId) = seedMasterMediaAndAlbum()
        runBlocking {
            val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "gallery_creator",
                displayName = "Gallery Creator",
                role = ManagementRole.STAFF,
                initialPin = "334455",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        canView = true,
                        canCreate = false,
                        canEdit = false,
                        canDelete = false,
                        canPublish = false
                    ),
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.PHOTO_GALLERY.key,
                        canView = true,
                        canCreate = true,
                        canEdit = false,
                        canDelete = false,
                        canPublish = false
                    )
                )
            ).getOrThrow()
        }

        managementViewModel.loginWithPin("gallery_creator", "334455")
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
                composeTestRule.onNodeWithTag("media_assign_album_btn_$mediaId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("media_assign_album_btn_$mediaId").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("media_assign_album_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("media_assign_album_option_$albumId").performClick()

        awaitCondition {
            val pending = runBlocking { database.contentApprovalDao().observePending().first() }
            pending.any { it.sectionKey == ManagementSection.PHOTO_GALLERY.key } &&
                !managementViewModel.isBusy.value &&
                !managementViewModel.statusMessage.value.isNullOrBlank()
        }

        // Must NOT be auto-published in live public gallery
        val albumWithMedia = runBlocking {
            database.galleryDao().observeAlbumsWithMedia().first().first { it.album.id == albumId }
        }
        assertTrue(
            "Assigned gallery item must remain unpublished until approved",
            albumWithMedia.mediaItems.none { it.isPublished && it.isApproved }
        )
        assertTrue(
            "Expected approval feedback message, got: ${managementViewModel.statusMessage.value}",
            managementViewModel.statusMessage.value.orEmpty().contains("approval", ignoreCase = true)
        )
    }

    @Test
    fun `media studio publishes Assign to Album directly when user has PHOTO_GALLERY CREATE and PUBLISH`() {
        val (_, mediaId, albumId) = seedMasterMediaAndAlbum()

        managementViewModel.loginWithPin("admin", "246810")
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
                composeTestRule.onNodeWithTag("media_assign_album_btn_$mediaId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("media_assign_album_btn_$mediaId").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("media_assign_album_option_$albumId").performClick()

        awaitCondition {
            val albumWithMedia = runBlocking {
                database.galleryDao().observeAlbumsWithMedia().first().first { it.album.id == albumId }
            }
            albumWithMedia.mediaItems.any { it.title == "Science Lab Inauguration" && it.isPublished && it.isApproved }
        }

        val pending = runBlocking { database.contentApprovalDao().observePending().first() }
        assertTrue(pending.none { it.sectionKey == ManagementSection.PHOTO_GALLERY.key })
    }

    @Test
    fun `parent portal top bar overflow menu includes Bookmarks shortcut and triggers callback`() {
        var bookmarksClicked = false
        var galleryClicked = false
        var drawerClicked = false
        var settingsClicked = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ParentPortalTopBar(
                        isSearchActive = false,
                        hasNotifications = true,
                        onToggleSearch = {},
                        onNotificationsClick = {},
                        onGalleryClick = { galleryClicked = true },
                        onBookmarksClick = { bookmarksClicked = true },
                        onOpenDrawerClick = { drawerClicked = true },
                        onSettingsClick = { settingsClicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("top_bar_more_menu_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_overflow_menu").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_menu_gallery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_menu_bookmarks").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_menu_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_menu_settings").assertIsDisplayed()

        composeTestRule.onNodeWithTag("top_bar_menu_bookmarks").performClick()
        composeTestRule.waitForIdle()

        assertTrue(bookmarksClicked)
        assertFalse(galleryClicked)
        assertFalse(drawerClicked)
        assertFalse(settingsClicked)
    }

    @Test
    fun `global search dashboard badge dynamically reflects searchable sections for master admin and restricted staff`() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
            val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "limited_staff",
                displayName = "Limited Staff",
                role = ManagementRole.STAFF,
                initialPin = "556677",
                permissions = listOf(
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.NEWS.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.ANNOUNCEMENTS.key, canView = true),
                    StaffPermissionEntity(accountId = 0L, sectionKey = ManagementSection.PHOTO_GALLERY.key, canView = true)
                )
            ).getOrThrow()
        }

        // 1. Master Admin sees full dynamic searchable section count (not hardcoded "11")
        managementViewModel.loginWithPin("admin", "246810")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }

        val expectedMasterCount = ManagementRepository.SEARCHABLE_SECTIONS.size
        assertNotEquals(11, expectedMasterCount)
        assertEquals(expectedMasterCount, managementViewModel.searchableSectionCount())

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

        composeTestRule.onNodeWithTag("mgmt_top_search_btn").assertIsDisplayed()

        // 2. Switch to restricted Staff (only 3 searchable sections permitted)
        managementViewModel.logout()
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.LoginRequired
        }
        managementViewModel.loginWithPin("limited_staff", "556677")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }
        assertEquals(3, managementViewModel.searchableSectionCount())
        composeTestRule.onNodeWithTag("mgmt_top_search_btn").assertIsDisplayed()
    }

    @Test
    fun `admin APP_SETTINGS exposes Change Language full screen action and returns cleanly to management portal`() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        }
        managementViewModel.loginWithPin("admin", "246810")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }

        var currentLang = AppLanguage.ENGLISH

        composeTestRule.setContent {
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
                                    currentLang = lang
                                    mainViewModel.selectLanguage(lang, markOnboarded = false)
                                },
                                onConfirmLanguage = { lang ->
                                    currentLang = lang
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

        // Open Admin APP_SETTINGS via Common App card
        composeTestRule.onNodeWithTag("management_dashboard_grid")
            .performScrollToNode(hasTestTag("admin_dash_card_common_app"))
        composeTestRule.onNodeWithTag("admin_dash_card_common_app").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_hub_open_app_settings").performClick()
        composeTestRule.waitForIdle()

        // Existing inline language radios remain preserved alongside Change Language button
        composeTestRule.onNodeWithTag("settings_language_en").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_language_ta").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_language_hi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_change_language_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        // Full-screen Language Selection opens cleanly
        composeTestRule.onNodeWithTag("language_selection_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_continue_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Confirming pops cleanly back to Management Portal's APP_SETTINGS screen
        composeTestRule.onNodeWithTag("management_dashboard_shell").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_screen_list").assertIsDisplayed()
    }
}
