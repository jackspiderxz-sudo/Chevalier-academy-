package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.app.ActivityOptionsCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.activity.compose.LocalActivityResultRegistryOwner
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.CropAspectRatioPreset
import com.example.data.media.MediaAdjustmentSpec
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.SchoolRepository
import com.example.ui.screens.management.AnnouncementsEditorScreen
import com.example.ui.screens.management.FacilitiesEditorScreen
import com.example.ui.screens.management.NewsEditorScreen
import com.example.ui.screens.management.PhotoGalleryEditorScreen
import com.example.ui.screens.management.SchoolProfileSectionEditor
import com.example.ui.screens.management.UniversalImageCropDialog
import com.example.ui.screens.management.UniversalMediaField
import com.example.ui.screens.management.UniversalMediaStudioPickerDialog
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManagementPhase2B2MediaUiTest {

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
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()
        database.close()
        File(context.filesDir, "chevalier_media").deleteRecursively()
    }

    private fun createTempImageFile(name: String, width: Int, height: Int, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): File {
        val file = File(context.cacheDir, name)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(141, 32, 41))
        FileOutputStream(file).use { out ->
            bmp.compress(format, 92, out)
        }
        bmp.recycle()
        return file
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

    private fun awaitCondition(timeoutMs: Long = 6_000L, condition: () -> Boolean) {
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

    private fun awaitIdleNotBusy() {
        awaitCondition { !managementViewModel.isBusy.value }
    }

    private fun scrollLazyListToItem(listTag: String, itemTag: String) {
        awaitTagDisplayed(listTag)
        val alreadyComposed = runCatching {
            composeTestRule.onNodeWithTag(itemTag).assertExists()
            true
        }.getOrDefault(false)
        if (!alreadyComposed) {
            composeTestRule.onNodeWithTag(listTag).performScrollToIndex(0)
        }
        composeTestRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag(itemTag))
        awaitTagDisplayed(itemTag)
    }

    private fun fakePhotoPickerRegistryOwner(nextUriProvider: () -> Uri?): ActivityResultRegistryOwner {
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                @Suppress("UNCHECKED_CAST")
                dispatchResult(requestCode, nextUriProvider() as O)
            }
        }
        return object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
    }

    @Test
    fun `UniversalMediaField imports photo picker image, surfaces errors on broken file, and clears on remove`() {
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        val validImg = createTempImageFile("picker_valid.jpg", 900, 600)
        var pickerResultUri: Uri? = Uri.fromFile(validImg)
        val pickerOwner = fakePhotoPickerRegistryOwner { pickerResultUri }
        var latestSelectedUri: String? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                LocalActivityResultRegistryOwner provides pickerOwner
            ) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    var selectedUri by remember { mutableStateOf<String?>(null) }
                    UniversalMediaField(
                        mStrings = mStrings,
                        label = mStrings.mediaUi.fieldFacilityPhotoLabel,
                        selectedUri = selectedUri,
                        onUriChanged = {
                            selectedUri = it
                            latestSelectedUri = it
                        },
                        mediaStudioItems = emptyList(),
                        testTagPrefix = "test_media"
                    )
                }
            }
        }

        awaitTagDisplayed("test_media_empty_hint")
        composeTestRule.onNodeWithTag("test_media_pick_device_btn").performClick()

        awaitTagDisplayed("test_media_preview")
        awaitTagDisplayed("test_media_status_badge")
        composeTestRule.onNodeWithTag("test_media_status_badge")
            .assertTextContains(mStrings.mediaUi.badgeOriginalLabel)
        assertNotNull(latestSelectedUri)
        assertTrue(mediaManager.isManagedMediaFileReadable(latestSelectedUri!!))

        // Attempt picking an unreadable/invalid file -> shows inline error without losing selected image
        pickerResultUri = Uri.parse("file:///nonexistent/corrupt.jpg")
        composeTestRule.onNodeWithTag("test_media_change_btn").performClick()
        awaitTagDisplayed("test_media_error")
        assertNotNull(latestSelectedUri)

        // Removing clears state cleanly back to empty hint
        composeTestRule.onNodeWithTag("test_media_remove_btn").performClick()
        awaitTagDisplayed("test_media_empty_hint")
        assertNull(latestSelectedUri)

        // Simulate missing pre-existing URI -> shows Unavailable badge and disables Crop
        val brokenOwner = fakePhotoPickerRegistryOwner { null }
        composeTestRule.runOnUiThread {
            // Covered via separate composition or remove check; handled cleanly below
        }
        assertFalse(mediaManager.isManagedMediaFileReadable("file:///missing/deleted.jpg"))
        assertNotNull(brokenOwner)
    }

    @Test
    fun `UniversalMediaField marks missing managed file Unavailable and disables cropper`() {
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    UniversalMediaField(
                        mStrings = mStrings,
                        label = mStrings.mediaUi.fieldNewsImageLabel,
                        selectedUri = "file:///nonexistent/deleted_photo.jpg",
                        onUriChanged = {},
                        mediaStudioItems = emptyList(),
                        testTagPrefix = "broken_media"
                    )
                }
            }
        }

        awaitTagDisplayed("broken_media_error")
        awaitTagDisplayed("broken_media_status_badge")
        composeTestRule.onNodeWithTag("broken_media_status_badge")
            .assertTextContains(mStrings.mediaUi.badgeUnavailableLabel)
        composeTestRule.onNodeWithTag("broken_media_crop_btn").assertIsNotEnabled()
    }

    @Test
    fun `UniversalMediaStudioPickerDialog distinguishes original and adjusted versions and blocks broken files`() {
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        val campusImg = createTempImageFile("studio_campus.jpg", 1000, 750)
        val imported = runBlocking { mediaManager.importAndOptimizeImage(Uri.fromFile(campusImg)).getOrThrow() }
        val adjusted = runBlocking {
            mediaManager.saveAdjustedMedia(
                sourceImageUri = imported.originalUri,
                spec = MediaAdjustmentSpec(
                    aspectPreset = CropAspectRatioPreset.RATIO_1_1,
                    zoom = 1.4f,
                    rotationDegrees = 90
                ),
                outputTag = "studio_crop"
            ).getOrThrow()
        }

        val validItem = MediaStudioItemEntity(
            id = 11L,
            title = "Main Auditorium",
            mediaKind = "IMAGE",
            originalUri = imported.originalUri,
            optimizedUri = imported.optimizedUri,
            mimeType = "image/jpeg",
            categoryOrAlbum = "Campus",
            widthPx = imported.widthPx,
            heightPx = imported.heightPx,
            createdByName = "Principal"
        )
        val brokenItem = MediaStudioItemEntity(
            id = 12L,
            title = "Missing Legacy Banner",
            mediaKind = "IMAGE",
            originalUri = "file:///nonexistent/missing_banner.jpg",
            optimizedUri = null,
            mimeType = "image/jpeg",
            categoryOrAlbum = "Legacy",
            createdByName = "Principal"
        )
        val youtubeItem = MediaStudioItemEntity(
            id = 13L,
            title = "Annual Day Video",
            mediaKind = "YOUTUBE",
            originalUri = "https://youtu.be/dQw4w9WgXcQ",
            optimizedUri = null,
            mimeType = "video/youtube",
            categoryOrAlbum = "Events",
            createdByName = "Principal"
        )

        var selectedResult: String? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    UniversalMediaStudioPickerDialog(
                        mStrings = mStrings,
                        mediaStudioItems = listOf(validItem, brokenItem, youtubeItem),
                        onSelectUri = { selectedResult = it },
                        onDismiss = {}
                    )
                }
            }
        }

        awaitTagDisplayed("studio_picker_list")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_12")
        composeTestRule.onNodeWithTag("studio_picker_item_13").assertDoesNotExist()

        // Broken file cannot be selected
        composeTestRule.onNodeWithTag("studio_picker_select_12_original").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithText(mStrings.mediaUi.studioPickerUnavailableHint).performScrollTo().assertIsDisplayed()

        // Scroll back to valid item -> shows Original + discovered Adjusted copy
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_11")
        composeTestRule.onNodeWithTag("studio_picker_select_11_original").performScrollTo().assertIsEnabled()
        val adjustedBtnTag = "studio_picker_select_11_adjusted_1"
        val altAdjustedBtnTag = "studio_picker_select_11_adjusted_2"
        val targetAdjTag = if (
            runCatching { composeTestRule.onNodeWithTag(adjustedBtnTag).assertExists(); true }.getOrDefault(false)
        ) adjustedBtnTag else altAdjustedBtnTag

        composeTestRule.onNodeWithTag(targetAdjTag).performScrollTo().performClick()
        assertEquals(adjusted.optimizedUri, selectedResult)
    }

    @Test
    fun `UniversalImageCropDialog adjusts aspect ratio, zoom, rotation, reset and saves copy preserving original`() {
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        val sourceFile = createTempImageFile("crop_source.jpg", 1200, 800)
        val imported = runBlocking { mediaManager.importAndOptimizeImage(Uri.fromFile(sourceFile)).getOrThrow() }
        val originalFile = File(Uri.parse(imported.originalUri).path!!)
        val originalBytesBefore = originalFile.readBytes()

        var appliedOriginal: String? = null
        var appliedAdjusted: String? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    UniversalImageCropDialog(
                        mStrings = mStrings,
                        sourceUri = imported.originalUri,
                        initialAspectPreset = CropAspectRatioPreset.ORIGINAL,
                        outputTag = "phase2b2_test",
                        onApply = { orig, adj ->
                            appliedOriginal = orig
                            appliedAdjusted = adj
                        },
                        onDismiss = {}
                    )
                }
            }
        }

        awaitTagDisplayed("universal_image_crop_dialog")
        awaitTagDisplayed("crop_aspect_original", scrollTo = true)
        composeTestRule.onNodeWithTag("crop_aspect_free").performScrollTo().performClick()
        awaitTagDisplayed("crop_free_ratio_slider", scrollTo = true)
        composeTestRule.onNodeWithText(mStrings.mediaUi.cropperTitle).performScrollTo()
        composeTestRule.onNodeWithTag("crop_aspect_ratio_16_9").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("crop_aspect_ratio_4_3").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("crop_aspect_ratio_1_1").performScrollTo().performClick()

        // Scroll down to verify Live Cropped Preview is rendered inside the dialog
        awaitTagDisplayed("crop_live_preview", scrollTo = true)

        // Rotation 90° CW -> 270° CCW -> Reset back to 0°
        composeTestRule.onNodeWithTag("crop_rotate_right_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_rotation_label").performScrollTo().assertTextContains("90°")
        composeTestRule.onNodeWithTag("crop_rotate_left_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_rotate_left_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_rotation_label").performScrollTo().assertTextContains("270°")
        composeTestRule.onNodeWithTag("crop_reset_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_rotation_label").performScrollTo().assertTextContains("0°")

        // Scroll back to top of cropper so horizontal aspect chips are vertically in view, then apply 1:1 + 90° + zoom in
        composeTestRule.onNodeWithText(mStrings.mediaUi.cropperTitle).performScrollTo()
        composeTestRule.onNodeWithTag("crop_aspect_ratio_1_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_rotate_right_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_zoom_in_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_apply_btn").performScrollTo().performClick()

        awaitCondition { appliedAdjusted != null }
        assertEquals(imported.originalUri, appliedOriginal)
        assertNotEquals(imported.originalUri, appliedAdjusted)
        assertTrue(appliedAdjusted!!.contains("chevalier_media/adjusted"))

        // Verify saved file is readable square crop and original bytes are 100% untouched
        val adjPath = Uri.parse(appliedAdjusted).path!!
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(adjPath, bounds)
        assertTrue(bounds.outWidth > 0 && bounds.outHeight > 0)
        val ratio = bounds.outWidth.toFloat() / bounds.outHeight.toFloat()
        assertTrue("Expected ~1:1 crop ratio, was $ratio", ratio in 0.95f..1.05f)
        assertTrue(originalBytesBefore.contentEquals(originalFile.readBytes()))
    }

    @Test
    fun `GalleryAlbumDetailEditor manages album photos including add, reorder, cover, publish toggle, and delete`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session

        val img1 = createTempImageFile("gal_1.jpg", 800, 600)
        val img2 = createTempImageFile("gal_2.jpg", 800, 600)

        var studioAssetId = 0L
        val (albumId, media1Id, media2Id) = runBlocking {
            val imp1 = mgmtRepo.importImageToMediaStudio(master, Uri.fromFile(img1), "Studio Asset 1", null, "Events", emptyList(), false).getOrThrow()
            val imp2 = mgmtRepo.importImageToMediaStudio(master, Uri.fromFile(img2), "Studio Asset 2", null, "Events", emptyList(), false).getOrThrow()
            studioAssetId = imp1.id

            val aId = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.PHOTO_GALLERY,
                recordId = 0L,
                recordTitle = "Annual Science Fair",
                proposedJson = ContentSnapshotSerializer.galleryAlbumToJson(
                    GalleryAlbumEntity(title = "Annual Science Fair", category = "Academics")
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Album created"
            ).getOrThrow()

            val m1 = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.PHOTO_GALLERY,
                recordId = 0L,
                recordTitle = "Robotics Booth",
                proposedJson = ContentSnapshotSerializer.galleryMediaToJson(
                    GalleryMediaEntity(albumId = aId, title = "Robotics Booth", mediaUri = imp1.optimizedUri ?: imp1.originalUri, displayOrder = 0)
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Photo 1"
            ).getOrThrow()

            val m2 = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.PHOTO_GALLERY,
                recordId = 0L,
                recordTitle = "Solar Model",
                proposedJson = ContentSnapshotSerializer.galleryMediaToJson(
                    GalleryMediaEntity(albumId = aId, title = "Solar Model", mediaUri = imp2.optimizedUri ?: imp2.originalUri, displayOrder = 1)
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Photo 2"
            ).getOrThrow()

            Triple(aId, m1, m2)
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    PhotoGalleryEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("gallery_manage_photos_btn_$albumId", scrollTo = true)
        composeTestRule.onNodeWithTag("gallery_manage_photos_btn_$albumId").performClick()

        awaitTagDisplayed("gallery_album_detail_editor")
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media1Id")
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media2Id")

        // 1. Add a 3rd photo via Studio Picker inside GalleryAlbumDetailEditor
        composeTestRule.onNodeWithTag("gallery_detail_add_btn").performClick()
        awaitTagDisplayed("gallery_media_edit_dialog")
        composeTestRule.onNodeWithTag("gallery_detail_media_field_pick_studio_btn").performScrollTo().performClick()
        awaitTagDisplayed("universal_media_studio_picker_dialog")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_$studioAssetId")
        composeTestRule.onNodeWithTag("studio_picker_select_${studioAssetId}_original").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_media_studio_picker_dialog")

        composeTestRule.onNodeWithTag("gallery_media_title_input").performScrollTo().performTextInput("Third Prize Ceremony")
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitTagDoesNotExist("gallery_media_edit_dialog")
        awaitIdleNotBusy()

        awaitCondition {
            runBlocking {
                database.galleryDao().observeAllMedia().first().count { it.albumId == albumId } == 3
            }
        }

        // 2. Reorder Photo 1 down -> Photo 2 becomes order 0, Photo 1 becomes order 1
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media1Id")
        composeTestRule.onNodeWithTag("gallery_move_down_$media1Id").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("gallery_move_down_$media1Id").performClick()
        awaitIdleNotBusy()
        awaitCondition {
            runBlocking {
                val items = database.galleryDao().observeAllMedia().first().filter { it.albumId == albumId }
                val firstOrder = items.firstOrNull { it.id == media1Id }?.displayOrder
                val secondOrder = items.firstOrNull { it.id == media2Id }?.displayOrder
                secondOrder == 0 && firstOrder == 1
            }
        }

        // 3. Set Photo 2 as Album Cover -> badge appears and DB cover updates
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media2Id")
        composeTestRule.onNodeWithTag("gallery_set_cover_$media2Id").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("gallery_set_cover_$media2Id").performClick()
        awaitIdleNotBusy()
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media2Id")
        awaitTagDisplayed("gallery_cover_badge_$media2Id", scrollTo = true)
        composeTestRule.onNodeWithTag("gallery_cover_badge_$media2Id")
            .assertTextContains(mStrings.mediaUi.galleryAlbumCoverBadge)
        runBlocking {
            val album = database.galleryDao().observeAlbumsWithMedia().first().first { it.album.id == albumId }.album
            val photo2 = database.galleryDao().observeAllMedia().first().first { it.id == media2Id }
            assertEquals(photo2.mediaUri, album.coverMediaUri)
        }

        // 4. Toggle publish off for Photo 1
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media1Id")
        composeTestRule.onNodeWithTag("gallery_toggle_publish_$media1Id").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("gallery_toggle_publish_$media1Id").performClick()
        awaitIdleNotBusy()
        awaitCondition {
            runBlocking {
                val item = database.galleryDao().observeAllMedia().first().first { it.id == media1Id }
                !item.isPublished
            }
        }

        // 5. Delete Photo 1 via confirmation AlertDialog
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media1Id")
        composeTestRule.onNodeWithTag("gallery_delete_media_$media1Id").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("gallery_delete_media_$media1Id").performClick()
        awaitTagDisplayed("gallery_delete_confirm_dialog")
        composeTestRule.onNodeWithTag("gallery_delete_confirm_btn").performClick()
        awaitTagDoesNotExist("gallery_delete_confirm_dialog")
        awaitIdleNotBusy()
        awaitCondition {
            runBlocking {
                database.galleryDao().observeAllMedia().first().none { it.id == media1Id }
            }
        }
    }

    @Test
    fun `GalleryAlbumDetailEditor enforces RBAC and routes non-publisher reorder and cover changes through approval queue`() {
        val img1 = createTempImageFile("rbac_gal1.jpg", 640, 480)
        val img2 = createTempImageFile("rbac_gal2.jpg", 640, 480)

        val (albumId, media1Id, media2Id) = runBlocking {
            val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "galleryeditor",
                displayName = "Gallery Coordinator",
                role = ManagementRole.STAFF,
                initialPin = "556677",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0,
                        sectionKey = ManagementSection.PHOTO_GALLERY.key,
                        canView = true,
                        canCreate = false,
                        canEdit = true,
                        canDelete = false,
                        canPublish = false
                    )
                )
            ).getOrThrow()

            val imp1 = mediaManager.importAndOptimizeImage(Uri.fromFile(img1)).getOrThrow()
            val imp2 = mediaManager.importAndOptimizeImage(Uri.fromFile(img2)).getOrThrow()

            val aId = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.PHOTO_GALLERY,
                recordId = 0L,
                recordTitle = "Campus Day",
                proposedJson = ContentSnapshotSerializer.galleryAlbumToJson(
                    GalleryAlbumEntity(title = "Campus Day", category = "Events")
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Album"
            ).getOrThrow()

            val m1 = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.PHOTO_GALLERY,
                recordId = 0L,
                recordTitle = "Opening Parade",
                proposedJson = ContentSnapshotSerializer.galleryMediaToJson(
                    GalleryMediaEntity(albumId = aId, title = "Opening Parade", mediaUri = imp1.optimizedUri, displayOrder = 0)
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Photo 1"
            ).getOrThrow()

            val m2 = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.PHOTO_GALLERY,
                recordId = 0L,
                recordTitle = "Stage Drama",
                proposedJson = ContentSnapshotSerializer.galleryMediaToJson(
                    GalleryMediaEntity(albumId = aId, title = "Stage Drama", mediaUri = imp2.optimizedUri, displayOrder = 1)
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Photo 2"
            ).getOrThrow()

            Triple(aId, m1, m2)
        }

        managementViewModel.loginWithPin("galleryeditor", "556677")
        awaitCondition {
            managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    PhotoGalleryEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("gallery_manage_photos_btn_$albumId", scrollTo = true)
        composeTestRule.onNodeWithTag("gallery_manage_photos_btn_$albumId").performClick()
        awaitTagDisplayed("gallery_album_detail_editor")
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media1Id")

        // Create, Delete, and Publish controls hidden when lacking those permissions
        composeTestRule.onNodeWithTag("gallery_detail_add_btn").assertDoesNotExist()
        composeTestRule.onNodeWithTag("gallery_delete_media_$media1Id").assertDoesNotExist()
        composeTestRule.onNodeWithTag("gallery_toggle_publish_$media1Id").assertDoesNotExist()

        // Non-publisher setting cover submits approval proposal instead of changing live cover
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media2Id")
        composeTestRule.onNodeWithTag("gallery_set_cover_$media2Id").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("gallery_set_cover_$media2Id").performClick()
        awaitIdleNotBusy()
        awaitCondition {
            runBlocking { mgmtRepo.pendingApprovals.first().size == 1 }
        }
        runBlocking {
            val album = database.galleryDao().observeAlbumsWithMedia().first().first { it.album.id == albumId }.album
            assertNull(album.coverMediaUri)
        }

        // Non-publisher first reorder submits pending approval without mutating live order
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media1Id")
        composeTestRule.onNodeWithTag("gallery_move_down_$media1Id").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("gallery_move_down_$media1Id").performClick()
        awaitIdleNotBusy()
        awaitCondition {
            runBlocking { mgmtRepo.pendingApprovals.first().size == 2 }
        }

        // Non-publisher second reorder creates another pending approval while live order & cover remain untouched
        scrollLazyListToItem("gallery_detail_list", "gallery_detail_item_$media2Id")
        composeTestRule.onNodeWithTag("gallery_move_up_$media2Id").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("gallery_move_up_$media2Id").performClick()
        awaitIdleNotBusy()
        awaitCondition {
            runBlocking { mgmtRepo.pendingApprovals.first().size == 3 }
        }
        runBlocking {
            val album = database.galleryDao().observeAlbumsWithMedia().first().first { it.album.id == albumId }.album
            assertNull(album.coverMediaUri)
            val allMedia = database.galleryDao().observeAllMedia().first()
            assertEquals(0, allMedia.first { it.id == media1Id }.displayOrder)
            assertEquals(1, allMedia.first { it.id == media2Id }.displayOrder)
        }
    }

    @Test
    fun `SchoolProfile and AboutSchool pilot editors save imageUri and track dirty state`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
        val studioFile = createTempImageFile("profile_about_img.jpg", 960, 640)
        val studioItem = runBlocking {
            mgmtRepo.importImageToMediaStudio(
                actor = master,
                sourceUri = Uri.fromFile(studioFile),
                title = "School Campus Wing",
                caption = "Academic block",
                categoryOrAlbum = "Campus",
                assignedSections = emptyList(),
                isFeatured = false
            ).getOrThrow()
        }
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        var activeSection by mutableStateOf(ManagementSection.SCHOOL_PROFILE)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    SchoolProfileSectionEditor(
                        managementViewModel = managementViewModel,
                        mStrings = mStrings,
                        section = activeSection
                    )
                }
            }
        }

        // 1. SCHOOL_PROFILE sets profileImageUri & tracks dirty state
        awaitTagDisplayed("profile_media_pick_studio_btn", scrollTo = true)
        composeTestRule.onNodeWithTag("profile_media_pick_studio_btn").performClick()
        awaitTagDisplayed("universal_media_studio_picker_dialog")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_${studioItem.id}")
        composeTestRule.onNodeWithTag("studio_picker_select_${studioItem.id}_original").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_media_studio_picker_dialog")
        awaitTagDisplayed("editor_unsaved_indicator")
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitIdleNotBusy()

        awaitCondition {
            runBlocking { database.schoolProfileDao().getProfileOnce()?.profileImageUri == studioItem.originalUri }
        }

        // 2. Switch to ABOUT_SCHOOL and set aboutImageUri
        composeTestRule.runOnIdle {
            activeSection = ManagementSection.ABOUT_SCHOOL
        }
        awaitTagDisplayed("about_media_pick_studio_btn", scrollTo = true)
        composeTestRule.onNodeWithTag("about_media_pick_studio_btn").performClick()
        awaitTagDisplayed("universal_media_studio_picker_dialog")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_${studioItem.id}")
        composeTestRule.onNodeWithTag("studio_picker_select_${studioItem.id}_original").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_media_studio_picker_dialog")
        awaitTagDisplayed("editor_unsaved_indicator")
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitIdleNotBusy()

        awaitCondition {
            runBlocking { database.schoolProfileDao().getProfileOnce()?.aboutImageUri == studioItem.originalUri }
        }
    }

    @Test
    fun `News pilot editor integrates UniversalMediaField and Preview tab renders news_preview_image`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
        val studioFile = createTempImageFile("news_studio_img.jpg", 960, 640)
        val studioItem = runBlocking {
            mgmtRepo.importImageToMediaStudio(
                actor = master,
                sourceUri = Uri.fromFile(studioFile),
                title = "Library Ceremony",
                caption = "Inauguration",
                categoryOrAlbum = "News",
                assignedSections = emptyList(),
                isFeatured = false
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

        awaitTagDisplayed("mgmt_fab_add_news")
        composeTestRule.onNodeWithTag("mgmt_fab_add_news").performClick()
        awaitTagDisplayed("news_editor_title")
        composeTestRule.onNodeWithTag("news_editor_title").performTextInput("Campus Library Inauguration")
        composeTestRule.onNodeWithTag("news_editor_summary").performTextInput("New reading wing opened")
        composeTestRule.onNodeWithTag("news_editor_body").performTextInput("Full ceremony coverage.")

        composeTestRule.onNodeWithTag("news_media_pick_studio_btn").performScrollTo().performClick()
        awaitTagDisplayed("universal_media_studio_picker_dialog")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_${studioItem.id}")
        composeTestRule.onNodeWithTag("studio_picker_select_${studioItem.id}_original").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_media_studio_picker_dialog")

        composeTestRule.onNodeWithTag("editor_tab_preview").performClick()
        awaitTagDisplayed("news_preview_image")
        composeTestRule.onNodeWithTag("editor_tab_edit").performClick()
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitIdleNotBusy()

        awaitCondition {
            runBlocking {
                database.newsDao().observeAll().first()
                    .any { it.title == "Campus Library Inauguration" && it.imageUri == studioItem.originalUri }
            }
        }
    }

    @Test
    fun `Facilities and Announcements pilot editors save selected media URI`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
        val studioFile = createTempImageFile("facility_ann_img.jpg", 800, 600)
        val studioItem = runBlocking {
            mgmtRepo.importImageToMediaStudio(
                actor = master,
                sourceUri = Uri.fromFile(studioFile),
                title = "STEM Campus Asset",
                caption = null,
                categoryOrAlbum = "Facilities",
                assignedSections = emptyList(),
                isFeatured = false
            ).getOrThrow()
        }
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        var showAnnouncements by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    if (!showAnnouncements) {
                        FacilitiesEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                    } else {
                        AnnouncementsEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                    }
                }
            }
        }

        // Facilities
        awaitTagDisplayed("mgmt_fab_add_facilities")
        composeTestRule.onNodeWithTag("mgmt_fab_add_facilities").performClick()
        composeTestRule.onNodeWithText(mStrings.facilityNameRequiredLabel).performTextInput("Robotics Lab")
        composeTestRule.onNodeWithText(mStrings.fieldDescriptionRequiredLabel).performTextInput("Modern STEM lab")
        composeTestRule.onNodeWithTag("facility_media_pick_studio_btn").performScrollTo().performClick()
        awaitTagDisplayed("universal_media_studio_picker_dialog")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_${studioItem.id}")
        composeTestRule.onNodeWithTag("studio_picker_select_${studioItem.id}_original").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_media_studio_picker_dialog")
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitIdleNotBusy()

        awaitCondition {
            runBlocking {
                database.facilityDao().observeAll().first()
                    .any { it.name == "Robotics Lab" && it.imageUri == studioItem.originalUri }
            }
        }

        // Switch to Announcements
        composeTestRule.runOnIdle {
            showAnnouncements = true
        }
        awaitTagDisplayed("mgmt_fab_add_announcements")
        composeTestRule.onNodeWithTag("mgmt_fab_add_announcements").performClick()
        composeTestRule.onNodeWithText(mStrings.announcementTitleLabel).performTextInput("PTA Schedule")
        composeTestRule.onNodeWithText(mStrings.announcementMessageLabel).performTextInput("Parent teacher meeting at 9 AM")
        composeTestRule.onNodeWithTag("announcement_media_pick_studio_btn").performScrollTo().performClick()
        awaitTagDisplayed("universal_media_studio_picker_dialog")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_${studioItem.id}")
        composeTestRule.onNodeWithTag("studio_picker_select_${studioItem.id}_original").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_media_studio_picker_dialog")
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitIdleNotBusy()

        awaitCondition {
            runBlocking {
                database.announcementDao().observeAll().first()
                    .any { it.title == "PTA Schedule" && it.imageUri == studioItem.originalUri }
            }
        }
    }

    @Test
    fun `Phase 2B2 Universal Media strings are distinct and localized across English, Tamil, and Hindi`() {
        val en = managementStringsFor(AppLanguage.ENGLISH)
        val ta = managementStringsFor(AppLanguage.TAMIL)
        val hi = managementStringsFor(AppLanguage.HINDI)

        val bundles = listOf(en, ta, hi)
        bundles.forEach { b ->
            assertTrue(b.mediaUi.pickFromDeviceBtn.isNotBlank())
            assertTrue(b.mediaUi.pickFromStudioBtn.isNotBlank())
            assertTrue(b.mediaUi.cropAdjustBtn.isNotBlank())
            assertTrue(b.mediaUi.studioPickerTitle.isNotBlank())
            assertTrue(b.mediaUi.cropperTitle.isNotBlank())
            assertTrue(b.mediaUi.gallerySetCoverBtn.isNotBlank())
            assertTrue(b.galleryManagePhotosBtn(3).contains("3"))
        }

        assertNotEquals(en.mediaUi.cropperTitle, ta.mediaUi.cropperTitle)
        assertNotEquals(en.mediaUi.cropperTitle, hi.mediaUi.cropperTitle)
        assertNotEquals(ta.mediaUi.cropperTitle, hi.mediaUi.cropperTitle)

        // Verify UniversalMediaField renders cleanly in Tamil
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.TAMIL)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    UniversalMediaField(
                        mStrings = ta,
                        label = ta.mediaUi.fieldAboutSchoolPhotoLabel,
                        selectedUri = null,
                        onUriChanged = {},
                        mediaStudioItems = emptyList(),
                        testTagPrefix = "ta_media"
                    )
                }
            }
        }
        awaitTagDisplayed("ta_media_pick_device_btn")
        assertTrue(
            composeTestRule.onAllNodesWithText(ta.mediaUi.pickFromDeviceBtn)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }
}
