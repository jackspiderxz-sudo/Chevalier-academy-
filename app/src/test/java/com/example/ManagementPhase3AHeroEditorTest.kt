package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.app.ActivityOptionsCompat
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
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.SchoolRepository
import com.example.ui.screens.management.HeroBannerEditorScreen
import com.example.ui.screens.management.MediaStudioScreen
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
class ManagementPhase3AHeroEditorTest {

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

    private fun createTempImageFile(name: String, width: Int, height: Int): File {
        val file = File(context.cacheDir, name)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(141, 32, 41))
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bmp.recycle()
        return file
    }

    private fun setupAndLoginMasterAdmin() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        }
        authenticateUser("admin", "246810")
    }

    private fun authenticateUser(username: String, pin: String) {
        managementViewModel.loginWithPin(username, pin)
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
    fun `HeroBannerEditorScreen uses UniversalContentEditorScaffold, validates, crops 16_9 with rotation preserving original, previews and publishes`() {
        setupAndLoginMasterAdmin()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        val sampleFile = createTempImageFile("hero_picker_3a.jpg", 1200, 900)
        val pickerOwner = fakePhotoPickerRegistryOwner { Uri.fromFile(sampleFile) }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                LocalActivityResultRegistryOwner provides pickerOwner
            ) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HeroBannerEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_logo_banner")
        composeTestRule.onNodeWithTag("mgmt_fab_add_logo_banner").performClick()

        // Opens full-screen UniversalContentEditorScaffold
        awaitTagDisplayed("universal_content_editor_scaffold")
        awaitTagDisplayed("hero_media_empty_hint")

        // Inline validation fails cleanly when no image is selected
        composeTestRule.onNodeWithTag("hero_editor_caption").performScrollTo().performTextInput("Morning Assembly")
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitTagDisplayed("mgmt_error_banner")
        composeTestRule.onNodeWithText(mStrings.heroImageRequiredError).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Assembly").performScrollTo().assertIsDisplayed()

        // Dismiss error banner and pick image from device Photo Picker
        composeTestRule.onNodeWithText(mStrings.dismissBtn).performScrollTo().performClick()
        composeTestRule.onNodeWithTag("hero_media_pick_device_btn").performScrollTo().performClick()
        awaitTagDisplayed("hero_media_preview", scrollTo = true)
        awaitTagDisplayed("hero_media_status_badge", scrollTo = true)
        composeTestRule.onNodeWithTag("hero_media_status_badge")
            .assertTextContains(mStrings.mediaUi.badgeOriginalLabel)

        // Locate imported original file to confirm it remains untouched after crop/rotation
        val origDir = File(context.filesDir, "chevalier_media/originals")
        val origFile = origDir.listFiles()?.firstOrNull()
        assertNotNull(origFile)
        val origBytesBefore = origFile!!.readBytes()

        // Open UniversalImageCropDialog from UniversalMediaField -> defaults to 16:9
        composeTestRule.onNodeWithTag("hero_media_crop_btn").performScrollTo().performClick()
        awaitTagDisplayed("universal_image_crop_dialog")
        composeTestRule.onNodeWithTag("crop_aspect_ratio_16_9").performScrollTo().assertIsSelected()

        // Rotate 90 degrees and apply
        composeTestRule.onNodeWithTag("crop_rotate_right_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_rotation_label").performScrollTo().assertTextContains("90°")
        composeTestRule.onNodeWithTag("crop_apply_btn").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_image_crop_dialog")

        // Field switches to Adjusted badge & original file bytes stay untouched
        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("hero_media_status_badge")
                    .performScrollTo()
                    .assertTextContains(mStrings.mediaUi.badgeAdjustedLabel)
                true
            }.getOrDefault(false)
        }
        assertTrue(origBytesBefore.contentEquals(origFile.readBytes()))

        // Update display order and switch to Preview tab
        composeTestRule.onNodeWithTag("hero_editor_order").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("hero_editor_order").performScrollTo().performTextInput("2")
        composeTestRule.onNodeWithTag("editor_unsaved_indicator").assertIsDisplayed()

        composeTestRule.onNodeWithTag("editor_tab_preview").performClick()
        awaitTagDisplayed("hero_editor_live_preview", scrollTo = true)
        awaitTagDisplayed("hero_preview_image", scrollTo = true)
        composeTestRule.onNodeWithText("Morning Assembly").performScrollTo().assertIsDisplayed()

        // Publish live
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitTagDoesNotExist("universal_content_editor_scaffold")
        awaitIdleNotBusy()

        val saved = runBlocking {
            database.campusHeroPhotoDao().observeAllForManagement().first()
                .firstOrNull { it.caption == "Morning Assembly" }
        }
        assertNotNull(saved)
        assertTrue(saved!!.imageUri.contains("chevalier_media/adjusted"))
        assertEquals(2, saved.displayOrder)
        assertTrue(saved.isPublished && saved.isApproved)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(Uri.parse(saved.imageUri).path!!, bounds)
        val ratio = bounds.outWidth.toFloat() / bounds.outHeight.toFloat()
        assertTrue("Expected ~16:9 hero banner crop, got $ratio", ratio in 1.65f..1.88f)
    }

    @Test
    fun `HeroBannerEditorScreen handles unavailable media and replaces via Media Studio Picker`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
        val studioFile = createTempImageFile("hero_studio_valid.jpg", 1280, 720)
        val studioItem = runBlocking {
            mgmtRepo.importImageToMediaStudio(
                actor = master,
                sourceUri = Uri.fromFile(studioFile),
                title = "Main Quadrangle",
                caption = "Campus Quadrangle",
                categoryOrAlbum = "Campus",
                assignedSections = emptyList(),
                isFeatured = false
            ).getOrThrow()
        }
        val brokenHeroId = runBlocking {
            mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.LOGO_BANNER,
                recordId = 0L,
                recordTitle = "Broken Banner",
                proposedJson = ContentSnapshotSerializer.heroPhotoToJson(
                    CampusHeroPhotoEntity(
                        imageUri = "file:///nonexistent/deleted_hero_banner.jpg",
                        caption = "Broken Banner",
                        displayOrder = 1
                    )
                ),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Seed broken hero"
            ).getOrThrow()
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HeroBannerEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        val editTag = "managed_edit_btn_${"Broken Banner".lowercase().take(24)}"
        awaitTagDisplayed(editTag, scrollTo = true)
        composeTestRule.onNodeWithTag(editTag).performClick()

        awaitTagDisplayed("universal_content_editor_scaffold")
        awaitTagDisplayed("hero_media_status_badge", scrollTo = true)
        composeTestRule.onNodeWithTag("hero_media_status_badge")
            .assertTextContains(mStrings.mediaUi.badgeUnavailableLabel)
        awaitTagDisplayed("hero_media_error", scrollTo = true)
        composeTestRule.onNodeWithTag("hero_media_crop_btn").assertIsNotEnabled()

        // Attempting to save broken image fails validation cleanly (top scaffold banner + inline field banner)
        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitCondition {
            composeTestRule.onAllNodesWithTag("mgmt_error_banner").fetchSemanticsNodes().size == 2
        }
        composeTestRule.onAllNodesWithTag("mgmt_error_banner")[0].performScrollTo().assertIsDisplayed()

        // Replace using Media Studio Picker
        composeTestRule.onNodeWithTag("hero_media_pick_studio_btn").performScrollTo().performClick()
        awaitTagDisplayed("universal_media_studio_picker_dialog")
        scrollLazyListToItem("studio_picker_list", "studio_picker_item_${studioItem.id}")
        composeTestRule.onNodeWithTag("studio_picker_select_${studioItem.id}_original").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_media_studio_picker_dialog")

        composeTestRule.onNodeWithTag("hero_media_status_badge")
            .performScrollTo()
            .assertTextContains(mStrings.mediaUi.badgeOriginalLabel)

        composeTestRule.onNodeWithTag("editor_publish_now_btn").performClick()
        awaitTagDoesNotExist("universal_content_editor_scaffold")
        awaitIdleNotBusy()

        val updated = runBlocking {
            database.campusHeroPhotoDao().observeAllForManagement().first().first { it.id == brokenHeroId }
        }
        assertEquals(studioItem.originalUri, updated.imageUri)
    }

    @Test
    fun `HeroBannerEditorScreen resumes Draft and Pending revisions and resets cleanly to Live`() {
        setupAndLoginMasterAdmin()
        val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
        val liveImg = createTempImageFile("hero_live_res.jpg", 1280, 720)
        val imported = runBlocking { mediaManager.importAndOptimizeImage(Uri.fromFile(liveImg)).getOrThrow() }
        val liveEntity = CampusHeroPhotoEntity(
            imageUri = imported.optimizedUri,
            caption = "Live Campus View",
            displayOrder = 1
        )

        val heroId = runBlocking {
            val id = mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.LOGO_BANNER,
                recordId = 0L,
                recordTitle = "Live Campus View",
                proposedJson = ContentSnapshotSerializer.heroPhotoToJson(liveEntity),
                saveMode = PublishSaveMode.PUBLISH_NOW,
                changeSummary = "Initial publish"
            ).getOrThrow()

            val draft = liveEntity.copy(id = id, caption = "Draft Sunset Banner", displayOrder = 5)
            mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.LOGO_BANNER,
                recordId = id,
                recordTitle = "Draft Sunset Banner",
                proposedJson = ContentSnapshotSerializer.heroPhotoToJson(draft),
                saveMode = PublishSaveMode.SAVE_DRAFT,
                changeSummary = "Draft edit"
            ).getOrThrow()
            id
        }

        // Live record remains unchanged while draft exists
        runBlocking {
            val liveNow = database.campusHeroPhotoDao().observeAllForManagement().first().first { it.id == heroId }
            assertEquals("Live Campus View", liveNow.caption)
            assertEquals(1, liveNow.displayOrder)
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HeroBannerEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        val editTag = "managed_edit_btn_${"Live Campus View".lowercase().take(24)}"
        awaitTagDisplayed(editTag, scrollTo = true)
        composeTestRule.onNodeWithTag(editTag).performClick()

        // Resumes draft values and displays revision banner
        awaitTagDisplayed("editor_revision_banner")
        composeTestRule.onNodeWithTag("hero_editor_caption").performScrollTo().assertTextContains("Draft Sunset Banner")
        composeTestRule.onNodeWithTag("hero_editor_order").performScrollTo().assertTextContains("5")

        // Reset to Live reverts fields & marks draft discarded without touching live row
        awaitTagDisplayed("editor_reset_to_live_btn", scrollTo = true)
        composeTestRule.onNodeWithTag("editor_reset_to_live_btn").performClick()
        awaitTagDoesNotExist("editor_revision_banner")
        composeTestRule.onNodeWithTag("hero_editor_caption").performScrollTo().assertTextContains("Live Campus View")
        composeTestRule.onNodeWithTag("hero_editor_order").performScrollTo().assertTextContains("1")

        runBlocking {
            val latest = database.versionHistoryDao().getLatestForRecord(ManagementSection.LOGO_BANNER.key, heroId)
            assertEquals(ContentWorkflowStatus.DRAFT_DISCARDED, latest?.approvalStatus)
        }

        // Close clean editor, save a PENDING revision, and verify reopening resumes Pending Approval
        composeTestRule.onNodeWithTag("editor_close_btn").performClick()
        awaitTagDoesNotExist("universal_content_editor_scaffold")

        runBlocking {
            val pending = liveEntity.copy(id = heroId, caption = "Pending Panorama", displayOrder = 8)
            mgmtRepo.saveManagedContentRecord(
                actor = master,
                section = ManagementSection.LOGO_BANNER,
                recordId = heroId,
                recordTitle = "Pending Panorama",
                proposedJson = ContentSnapshotSerializer.heroPhotoToJson(pending),
                saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
                changeSummary = "Pending banner update"
            ).getOrThrow()
        }

        composeTestRule.onNodeWithTag(editTag).performClick()
        awaitTagDisplayed("editor_revision_banner")
        composeTestRule.onNodeWithTag("hero_editor_caption").performScrollTo().assertTextContains("Pending Panorama")
        composeTestRule.onNodeWithTag("hero_editor_order").performScrollTo().assertTextContains("8")

        runBlocking {
            val liveNow = database.campusHeroPhotoDao().observeAllForManagement().first().first { it.id == heroId }
            assertEquals("Live Campus View", liveNow.caption)
        }
    }

    @Test
    fun `HeroBannerEditorScreen enforces unsaved-change confirmation on Cancel and Close`() {
        setupAndLoginMasterAdmin()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HeroBannerEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_logo_banner")
        composeTestRule.onNodeWithTag("mgmt_fab_add_logo_banner").performClick()
        awaitTagDisplayed("universal_content_editor_scaffold")

        composeTestRule.onNodeWithTag("hero_editor_caption").performScrollTo().performTextInput("Unsaved Hero Caption")
        awaitTagDisplayed("editor_unsaved_indicator")

        // Cancel triggers confirmation; Keep Editing preserves input
        composeTestRule.onNodeWithTag("editor_cancel_btn").performClick()
        awaitTagDisplayed("editor_discard_confirm_dialog")
        composeTestRule.onNodeWithTag("editor_keep_editing_btn").performClick()
        awaitTagDoesNotExist("editor_discard_confirm_dialog")
        composeTestRule.onNodeWithText("Unsaved Hero Caption").assertIsDisplayed()

        // Close triggers confirmation; Discard closes without saving
        composeTestRule.onNodeWithTag("editor_close_btn").performClick()
        awaitTagDisplayed("editor_discard_confirm_dialog")
        composeTestRule.onNodeWithTag("editor_confirm_discard_btn").performClick()
        awaitTagDoesNotExist("universal_content_editor_scaffold")

        runBlocking {
            assertTrue(database.campusHeroPhotoDao().observeAllForManagement().first().isEmpty())
        }
    }

    @Test
    fun `RBAC governs HeroBannerEditorScreen and MediaStudioScreen Assign-to-Hero approval vs publish flows`() {
        val sampleFile = createTempImageFile("rbac_studio_hero.jpg", 1200, 800)
        var studioItemId = 0L

        runBlocking {
            val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account
            val masterSession = mgmtRepo.loginWithPin("admin", "246810").getOrThrow()
            studioItemId = mgmtRepo.importImageToMediaStudio(
                actor = masterSession,
                sourceUri = Uri.fromFile(sampleFile),
                title = "RBAC Auditorium",
                caption = "Auditorium",
                categoryOrAlbum = "Campus",
                assignedSections = emptyList(),
                isFeatured = false
            ).getOrThrow().id

            // User 1: LOGO_BANNER (V/C/E, NO Publish) + MEDIA_STUDIO (V/C/E, NO Publish)
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "banner_author",
                displayName = "Banner Author",
                role = ManagementRole.STAFF,
                initialPin = "112233",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.LOGO_BANNER.key,
                        canView = true, canCreate = true, canEdit = true, canDelete = false, canPublish = false
                    ),
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        canView = true, canCreate = true, canEdit = true, canDelete = false, canPublish = false
                    )
                )
            ).getOrThrow()

            // User 2: LOGO_BANNER Publish + MEDIA_STUDIO View
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "banner_publisher",
                displayName = "Banner Publisher",
                role = ManagementRole.STAFF,
                initialPin = "445566",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.LOGO_BANNER.key,
                        canView = true, canCreate = true, canEdit = true, canDelete = false, canPublish = true
                    ),
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        canView = true, canCreate = false, canEdit = false, canDelete = false, canPublish = false
                    )
                )
            ).getOrThrow()

            // User 3: MEDIA_STUDIO View only, NO LOGO_BANNER Create
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "media_viewer",
                displayName = "Media Viewer",
                role = ManagementRole.STAFF,
                initialPin = "778899",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0L,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        canView = true, canCreate = false, canEdit = false, canDelete = false, canPublish = false
                    )
                )
            ).getOrThrow()
        }

        val studioOrigFile = File(context.filesDir, "chevalier_media/originals").listFiles()!!.first()
        val studioOrigBytesBefore = studioOrigFile.readBytes()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        val pickerFile = createTempImageFile("rbac_picker_hero.jpg", 1200, 800)
        val pickerOwner = fakePhotoPickerRegistryOwner { Uri.fromFile(pickerFile) }

        var showMediaStudio by androidx.compose.runtime.mutableStateOf(false)

        // 1. Non-publisher uses HeroBannerEditorScreen & MediaStudioScreen Assign -> both submit PENDING approval
        authenticateUser("banner_author", "112233")
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                LocalActivityResultRegistryOwner provides pickerOwner
            ) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    if (!showMediaStudio) {
                        HeroBannerEditorScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                    } else {
                        MediaStudioScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                    }
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_logo_banner")
        composeTestRule.onNodeWithTag("mgmt_fab_add_logo_banner").performClick()
        awaitTagDisplayed("universal_content_editor_scaffold")
        awaitTagDoesNotExist("editor_publish_now_btn")

        composeTestRule.onNodeWithTag("hero_media_pick_device_btn").performScrollTo().performClick()
        awaitTagDisplayed("hero_media_preview", scrollTo = true)
        composeTestRule.onNodeWithTag("hero_editor_caption").performScrollTo().performTextInput("Author Submitted Banner")
        composeTestRule.onNodeWithTag("editor_submit_approval_btn").performClick()
        awaitTagDoesNotExist("universal_content_editor_scaffold")
        awaitIdleNotBusy()

        runBlocking {
            val pendingBanner = database.campusHeroPhotoDao().observeAllForManagement().first()
                .single { it.caption == "Author Submitted Banner" }
            assertFalse(pendingBanner.isPublished)
            assertEquals(ApprovalStatus.PENDING, pendingBanner.approvalStatus)
            assertEquals(1, database.contentApprovalDao().observePending().first().count { it.sectionKey == ManagementSection.LOGO_BANNER.key })
        }

        // Now switch to MediaStudioScreen as non-publisher and tap Assign Banner
        composeTestRule.runOnIdle {
            showMediaStudio = true
        }

        awaitTagDisplayed("media_assign_banner_btn_$studioItemId", scrollTo = true)
        composeTestRule.onNodeWithTag("media_assign_banner_btn_$studioItemId").performClick()

        // Opens UniversalImageCropDialog (NOT legacy HeroPhotoAdjustmentDialog)
        awaitTagDisplayed("universal_image_crop_dialog")
        awaitTagDoesNotExist("photo_adj_apply_crop_btn")
        composeTestRule.onNodeWithTag("crop_rotate_right_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("crop_apply_btn").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_image_crop_dialog")
        awaitIdleNotBusy()

        runBlocking {
            val banners = database.campusHeroPhotoDao().observeAllForManagement().first()
            assertEquals(2, banners.size)
            assertTrue(banners.none { it.isPublished })
            val studioAssigned = banners.first { it.caption != "Author Submitted Banner" }
            assertEquals(ApprovalStatus.PENDING, studioAssigned.approvalStatus)
            assertTrue(studioAssigned.imageUri.contains("chevalier_media/adjusted"))
            assertEquals(2, database.contentApprovalDao().observePending().first().count { it.sectionKey == ManagementSection.LOGO_BANNER.key })
            // Original studio asset & bytes preserved
            assertTrue(studioOrigBytesBefore.contentEquals(studioOrigFile.readBytes()))
            assertNotNull(database.mediaStudioDao().observeAll().first().firstOrNull { it.id == studioItemId })
        }

        // 2. Publisher uses MediaStudioScreen Assign -> publishes immediately live
        authenticateUser("banner_publisher", "445566")
        awaitCondition {
            managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, com.example.data.local.entity.PermissionAction.PUBLISH)
        }
        awaitTagDisplayed("media_assign_banner_btn_$studioItemId", scrollTo = true)
        composeTestRule.onNodeWithTag("media_assign_banner_btn_$studioItemId").performClick()
        awaitTagDisplayed("universal_image_crop_dialog")
        composeTestRule.onNodeWithTag("crop_apply_btn").performScrollTo().performClick()
        awaitTagDoesNotExist("universal_image_crop_dialog")
        awaitIdleNotBusy()

        runBlocking {
            val publishedBanners = database.campusHeroPhotoDao().observeAllForManagement().first()
                .filter { it.isPublished && it.isApproved && it.approvalStatus == ApprovalStatus.APPROVED }
            assertEquals(1, publishedBanners.size)
        }

        // 3. Viewer without LOGO_BANNER CREATE does not see Assign Banner button
        authenticateUser("media_viewer", "778899")
        awaitTagDoesNotExist("media_assign_banner_btn_$studioItemId")
    }

    @Test
    fun `HeroBannerEditorScreen localizes labels and validation errors across English, Tamil, and Hindi`() {
        setupAndLoginMasterAdmin()
        val en = managementStringsFor(AppLanguage.ENGLISH)
        val ta = managementStringsFor(AppLanguage.TAMIL)
        val hi = managementStringsFor(AppLanguage.HINDI)

        listOf(en, ta, hi).forEach { bundle ->
            assertTrue(bundle.mediaUi.fieldHeroBannerPhotoLabel.isNotBlank())
            assertTrue(bundle.heroAddDialogTitle.isNotBlank())
            assertTrue(bundle.heroEditDialogTitle.isNotBlank())
            assertTrue(bundle.heroImageRequiredError.isNotBlank())
        }
        assertNotEquals(en.mediaUi.fieldHeroBannerPhotoLabel, ta.mediaUi.fieldHeroBannerPhotoLabel)
        assertNotEquals(en.mediaUi.fieldHeroBannerPhotoLabel, hi.mediaUi.fieldHeroBannerPhotoLabel)
        assertNotEquals(ta.mediaUi.fieldHeroBannerPhotoLabel, hi.mediaUi.fieldHeroBannerPhotoLabel)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.TAMIL)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HeroBannerEditorScreen(managementViewModel = managementViewModel, mStrings = ta)
                }
            }
        }

        awaitTagDisplayed("mgmt_fab_add_logo_banner")
        composeTestRule.onNodeWithTag("mgmt_fab_add_logo_banner").performClick()
        awaitTagDisplayed("universal_content_editor_scaffold")
        assertTrue(composeTestRule.onAllNodesWithText(ta.mediaUi.fieldHeroBannerPhotoLabel).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodesWithText(ta.editorEditTab).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodesWithText(ta.editorPreviewTab).fetchSemanticsNodes().isNotEmpty())
    }
}
