package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.management.MediaStudioScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class Phase1Part1GalleryAndMediaMetadataTest {

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

    private fun setupAndLoginMasterAdmin() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        }
        managementViewModel.loginWithPin("admin", "246810")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }
    }

    @Test
    fun `gallery album selection filters strictly by album id even when two albums share the same category`() {
        val albumA = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 10L,
                title = "Annual Day 2026",
                category = "Campus Events",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(
                GalleryMediaEntity(
                    id = 101L,
                    albumId = 10L,
                    title = "Annual Stage",
                    caption = "Annual Day opening",
                    mediaUri = "https://example.com/annual.jpg",
                    mediaType = "IMAGE",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true
                )
            )
        )
        val albumB = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 20L,
                title = "Science Expo 2026",
                category = "Campus Events",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(
                GalleryMediaEntity(
                    id = 202L,
                    albumId = 20L,
                    title = "Robotics Booth",
                    caption = "Science Expo showcase",
                    mediaUri = "https://example.com/science.jpg",
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
                    GalleryScreen(albums = listOf(albumA, albumB))
                }
            }
        }
        composeTestRule.waitForIdle()

        // Initially on All Photos: both photos are shown
        composeTestRule.onNodeWithTag("gallery_photo_item_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_202").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_selected_album_header").assertDoesNotExist()

        // Switch to Albums tab and tap Album A
        composeTestRule.onNodeWithTag("gallery_tab_albums").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_album_card_10").performClick()
        composeTestRule.waitForIdle()

        // Must show selected-album header and ONLY Album A's photo (not Album B despite identical category)
        composeTestRule.onNodeWithTag("gallery_selected_album_header").assertIsDisplayed()
        composeTestRule.onNodeWithText("Annual Day 2026").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_202").assertDoesNotExist()

        // Clear filter returns cleanly to All Photos
        composeTestRule.onNodeWithTag("gallery_clear_album_filter_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_selected_album_header").assertDoesNotExist()
        composeTestRule.onNodeWithTag("gallery_photo_item_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_202").assertIsDisplayed()
    }

    @Test
    fun `selecting an empty gallery album shows localized empty feedback across English, Tamil, and Hindi`() {
        val populatedAlbum = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 1L,
                title = "Sports Meet",
                category = "Sports",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(
                GalleryMediaEntity(
                    id = 11L,
                    albumId = 1L,
                    title = "Track Finish",
                    mediaUri = "https://example.com/track.jpg",
                    mediaType = "IMAGE",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true
                )
            )
        )
        val emptyAlbum = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 2L,
                title = "Upcoming Convocation",
                category = "Sports",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = emptyList()
        )

        // Verify all 3 language resource sets provide non-blank localized empty-album strings
        AppLanguage.entries.forEach { lang ->
            val s = stringsFor(lang)
            assertTrue(s.galleryViewingAlbumLabel.isNotBlank())
            assertTrue(s.galleryClearAlbumFilterLabel.isNotBlank())
            assertTrue(s.galleryEmptyAlbumTitle.isNotBlank())
            assertTrue(s.galleryEmptyAlbumDesc.isNotBlank())
        }

        // Render in Tamil to verify localized empty-album card end-to-end
        val tamilStrings = stringsFor(AppLanguage.TAMIL)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides tamilStrings) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    GalleryScreen(albums = listOf(populatedAlbum, emptyAlbum))
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("gallery_tab_albums").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_album_card_2").performClick()
        composeTestRule.waitForIdle()

        // Selected album header + Tamil empty-album message must appear; populated album's photo must NOT leak in
        composeTestRule.onNodeWithTag("gallery_selected_album_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_empty_album_card").assertIsDisplayed()
        composeTestRule.onNodeWithText(tamilStrings.galleryEmptyAlbumTitle).assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_11").assertDoesNotExist()

        // Clearing filter from the empty-state button returns to All Photos
        composeTestRule.onNodeWithTag("gallery_empty_album_clear_btn")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_empty_album_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("gallery_photo_item_11").assertIsDisplayed()
    }

    @Test
    fun `media studio edit metadata dialog validates inputs and saves updated title caption and category`() {
        setupAndLoginMasterAdmin()
        val actor = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
        val imgFile = createTempImageFile("media_meta_edit.jpg")
        val imported = runBlocking {
            mgmtRepo.importImageToMediaStudio(
                actor = actor,
                sourceUri = Uri.fromFile(imgFile),
                title = "Initial Photo",
                caption = "Old caption",
                categoryOrAlbum = "Campus",
                assignedSections = emptyList(),
                isFeatured = false
            ).getOrThrow()
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    MediaStudioScreen(
                        managementViewModel = managementViewModel,
                        mStrings = mStrings
                    )
                }
            }
        }

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("media_edit_metadata_btn_${imported.id}").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Open Edit Metadata dialog
        composeTestRule.onNodeWithTag("media_edit_metadata_btn_${imported.id}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("media_edit_metadata_dialog").assertIsDisplayed()

        // Validation: clearing title blocks saving and shows error without substituting defaults
        composeTestRule.onNodeWithTag("media_edit_title_input").performTextClearance()
        composeTestRule.onNodeWithTag("media_edit_metadata_save_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(mStrings.mediaTitleRequiredError).assertIsDisplayed()

        // Enter new Title, Caption, Category and save
        composeTestRule.onNodeWithTag("media_edit_title_input").performTextInput("Main Auditorium Hall")
        composeTestRule.onNodeWithTag("media_edit_caption_input").performTextClearance()
        composeTestRule.onNodeWithTag("media_edit_caption_input").performTextInput("Newly renovated cultural hall")
        composeTestRule.onNodeWithTag("media_edit_category_input").performTextClearance()
        composeTestRule.onNodeWithTag("media_edit_category_input").performTextInput("Facilities")
        composeTestRule.onNodeWithTag("media_edit_metadata_save_btn").performClick()

        // Wait for dialog dismissal and Room persistence
        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("media_edit_metadata_dialog").assertDoesNotExist()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("media_item_title_${imported.id}").assertTextContains("Main Auditorium Hall")
        composeTestRule.onNodeWithTag("media_item_category_${imported.id}").assertTextContains("Facilities")
        composeTestRule.onNodeWithTag("media_item_caption_${imported.id}").assertTextContains("Newly renovated cultural hall")

        val persisted = runBlocking { database.mediaStudioDao().getById(imported.id) }
        assertNotNull(persisted)
        assertEquals("Main Auditorium Hall", persisted!!.title)
        assertEquals("Newly renovated cultural hall", persisted.caption)
        assertEquals("Facilities", persisted.categoryOrAlbum)
        assertEquals(imported.originalUri, persisted.originalUri)
        assertEquals(imported.optimizedUri, persisted.optimizedUri)

        val logs = runBlocking { database.activityLogDao().getAllOnce() }
        assertTrue(logs.any { it.actionType == "MEDIA_METADATA_UPDATED" && it.recordRef.contains("#${imported.id}") })
    }

    @Test
    fun `updateMediaItemMetadata enforces MEDIA_STUDIO EDIT permission and rejects blank title or category`() = runBlocking {
        mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        val master = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
        val imgFile = createTempImageFile("media_perm_check.jpg")
        val imported = mgmtRepo.importImageToMediaStudio(
            actor = master,
            sourceUri = Uri.fromFile(imgFile),
            title = "Library Wing",
            caption = null,
            categoryOrAlbum = "Academics",
            assignedSections = emptyList(),
            isFeatured = false
        ).getOrThrow()

        // Blank title / category must fail without silently falling back to defaults
        assertTrue(mgmtRepo.updateMediaItemMetadata(master, imported.id, "   ", null, "Academics").isFailure)
        assertTrue(mgmtRepo.updateMediaItemMetadata(master, imported.id, "Library Wing", null, "   ").isFailure)

        // Staff with VIEW-only permission on MEDIA_STUDIO cannot edit metadata
        val staffAcc = mgmtRepo.createStaffAccount(
            actor = master,
            username = "viewer",
            displayName = "Viewer Staff",
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
                )
            )
        ).getOrThrow()
        val staffSession = mgmtRepo.loginWithPin("viewer", "112233").getOrThrow()
        val unauthorizedResult = mgmtRepo.updateMediaItemMetadata(
            actor = staffSession,
            mediaId = imported.id,
            title = "Unauthorized Rename",
            caption = "Should fail",
            categoryOrAlbum = "Other"
        )
        assertTrue(unauthorizedResult.isFailure)
        assertEquals("Library Wing", database.mediaStudioDao().getById(imported.id)?.title)
    }
}
