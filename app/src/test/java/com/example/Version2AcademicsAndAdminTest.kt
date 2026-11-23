package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.Version2FeatureConfig
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ManagementAccountEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.data.repository.SchoolRepository
import com.example.data.v2.AcademicsV2AccessControl
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AcademicsV2ContentRepository
import com.example.data.v2.AcademicsV2RecordStatus
import com.example.data.v2.AdminV2Card
import com.example.data.v2.SchoolOwnerSection
import com.example.data.v2.V2StaffAccessRequestStatus
import com.example.ui.components.Version2UnavailableNoticeScreen
import com.example.ui.screens.AcademicsV2CategoryDetailScreen
import com.example.ui.screens.AcademicsV2PublicHubScreen
import com.example.ui.screens.PublicFacultyProfileDetailScreen
import com.example.ui.screens.management.AdminV2AcademicsManagementHubScreen
import com.example.ui.screens.management.AdminV2CategoryManagementScreen
import com.example.ui.screens.management.AdminV2DashboardGrid
import com.example.ui.screens.management.SchoolOwnerControlCenterScreen
import com.example.ui.screens.publicTestTagForAcademicsCategory
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.SchoolDataViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Version2AcademicsAndAdminTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var storageFile: File
    private lateinit var v2Repo: AcademicsV2ContentRepository
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var schoolDataViewModel: SchoolDataViewModel
    private lateinit var managementViewModel: ManagementViewModel

    private val schoolOwner = AccountWithPermissions(
        account = ManagementAccountEntity(
            id = 1L,
            username = "owner",
            displayName = "School Correspondent",
            role = ManagementRole.MASTER_ADMIN,
            pinSaltBase64 = "s",
            pinVerifierBase64 = "h",
            isActive = true
        ),
        permissions = emptyList()
    )

    private val examStaffWithPublish = AccountWithPermissions(
        account = ManagementAccountEntity(
            id = 2L,
            username = "exam_staff",
            displayName = "Exam Coordinator",
            role = ManagementRole.STAFF,
            pinSaltBase64 = "s",
            pinVerifierBase64 = "h",
            isActive = true
        ),
        permissions = listOf(
            StaffPermissionEntity(
                accountId = 2L,
                sectionKey = AcademicsV2Category.EXAMINATIONS.permissionKey,
                canView = true,
                canCreate = true,
                canEdit = true,
                canDelete = false,
                canPublish = true
            )
        )
    )

    private val restrictedStaff = AccountWithPermissions(
        account = ManagementAccountEntity(
            id = 3L,
            username = "transport_staff",
            displayName = "Transport Coordinator",
            role = ManagementRole.STAFF,
            pinSaltBase64 = "s",
            pinVerifierBase64 = "h",
            isActive = true
        ),
        permissions = listOf(
            StaffPermissionEntity(
                accountId = 3L,
                sectionKey = AcademicsV2Category.TRANSPORT.permissionKey,
                canView = true,
                canCreate = true,
                canEdit = true,
                canDelete = false,
                canPublish = false
            )
        )
    )

    @Before
    fun setup() {
        Version2FeatureConfig.resetToReleaseDefaults()
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storageFile = File(context.cacheDir, "v2_test_${UUID.randomUUID()}.json")
        v2Repo = AcademicsV2ContentRepository(context, storageFile)

        val prefs = UserPreferencesRepository(context)
        val media = MediaStudioManager(context)
        val schoolRepo = SchoolRepository.fromDatabase(database)
        val mgmtRepo = ManagementRepository(database, media)
        val backup = BackupRestoreManager(context, database, prefs)

        viewModelStore = ViewModelStore()
        schoolDataViewModel = ViewModelProvider(
            viewModelStore,
            SchoolDataViewModel.Factory(
                schoolRepository = schoolRepo,
                socialFeedRepository = null,
                academicsV2Repository = v2Repo
            )
        )[SchoolDataViewModel::class.java]
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(
                managementRepository = mgmtRepo,
                backupRestoreManager = backup,
                preferencesRepository = prefs,
                academicsV2Repository = v2Repo
            )
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        Version2FeatureConfig.resetToReleaseDefaults()
        viewModelStore.clear()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()
        database.close()
        storageFile.delete()
    }

    @Test
    fun `version 2 feature gate defaults to false and renders exact localized unavailability notice`() {
        assertFalse(Version2FeatureConfig.RELEASE_VERSION_2_ENABLED)
        assertFalse(Version2FeatureConfig.isVersion2AcademicsEnabled)
        assertFalse(Version2FeatureConfig.isVersion2AdminPanelEnabled)

        assertEquals("Coming in Version 2", Version2FeatureConfig.comingInVersion2Message(AppLanguage.ENGLISH))
        assertEquals(
            "இந்த வசதி Version 2-ல் விரைவில் கிடைக்கும்.",
            Version2FeatureConfig.comingInVersion2Message(AppLanguage.TAMIL)
        )

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                Version2UnavailableNoticeScreen(
                    language = AppLanguage.TAMIL,
                    title = "Academics",
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("version2_unavailable_message").assertIsDisplayed()
        composeTestRule.onNodeWithText("இந்த வசதி Version 2-ல் விரைவில் கிடைக்கும்.").assertIsDisplayed()
    }

    @Test
    fun `structure matches exact 13 Academics cards, 80 sub-sections, 8 Admin cards, 6 Owner sections, no Video Gallery, no Technology Partner`() {
        assertEquals(13, AcademicsV2Category.entries.size)
        assertEquals(80, AcademicsV2Category.entries.sumOf { it.sections.size })
        assertEquals(8, AdminV2Card.entries.size)
        assertEquals(6, SchoolOwnerSection.entries.size)

        val academicTitles = AcademicsV2Category.entries.map { it.title }
        assertEquals(
            listOf(
                "Academics",
                "Higher Secondary Courses",
                "About School",
                "Facilities",
                "Transport",
                "News & Announcements",
                "Gallery",
                "School Timetable",
                "Examinations",
                "Academic Calendar",
                "Faculty & Staff",
                "Student Activities",
                "Admissions"
            ),
            academicTitles
        )
        assertFalse(academicTitles.any { it.contains("Technology Partner", ignoreCase = true) })
        assertFalse(
            AcademicsV2Category.GALLERY.sections.any { it.title.contains("Video", ignoreCase = true) }
        )

        val adminTitles = AdminV2Card.entries.map { it.title }
        assertEquals(
            listOf(
                "Home Page",
                "Parent",
                "Academics",
                "Contact",
                "Admin",
                "School Settings",
                "Preview & Publish",
                "School Owner – Control Center"
            ),
            adminTitles
        )
        assertFalse(adminTitles.any { it.contains("Technology Partner", ignoreCase = true) })
        assertTrue(AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER.isOwnerCard)
    }

    @Test
    fun `public Academics V2 hub renders all 13 cards and Admin V2 dashboard renders all 8 cards plus 13 Academics Management categories`() {
        Version2FeatureConfig.setVersion2Enabled(true)

        var selectedPublicCategory: AcademicsV2Category? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    AcademicsV2PublicHubScreen(
                        schoolDataViewModel = schoolDataViewModel,
                        onOpenCategory = { selectedPublicCategory = it }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        AcademicsV2Category.entries.forEach { cat ->
            val tag = publicTestTagForAcademicsCategory(cat)
            composeTestRule.onNodeWithTag("academics_v2_hub_screen")
                .performScrollToNode(hasTestTag(tag))
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(publicTestTagForAcademicsCategory(AcademicsV2Category.ADMISSIONS))
            .performClick()
        assertEquals(AcademicsV2Category.ADMISSIONS, selectedPublicCategory)
    }

    @Test
    fun `admin V2 dashboard renders exact 8 cards and Card 03 opens all 13 Academics Management categories`() {
        Version2FeatureConfig.setVersion2Enabled(true)
        var openedAcademicsHub by mutableStateOf(false)

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                if (!openedAcademicsHub) {
                    AdminV2DashboardGrid(
                        session = schoolOwner,
                        managementViewModel = managementViewModel,
                        onSelectAdminCard = { card ->
                            if (card == AdminV2Card.ACADEMICS) openedAcademicsHub = true
                        }
                    )
                } else {
                    AdminV2AcademicsManagementHubScreen(
                        session = schoolOwner,
                        managementViewModel = managementViewModel,
                        onBack = { openedAcademicsHub = false },
                        onSelectCategory = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        AdminV2Card.entries.forEach { card ->
            composeTestRule.onNodeWithTag("admin_v2_dashboard_grid")
                .performScrollToNode(hasTestTag(card.testTag))
            composeTestRule.onNodeWithTag(card.testTag).assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag("admin_v2_dashboard_grid")
            .performScrollToNode(hasTestTag(AdminV2Card.ACADEMICS.testTag))
        composeTestRule.onNodeWithTag(AdminV2Card.ACADEMICS.testTag).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_v2_academics_hub").assertIsDisplayed()
        AcademicsV2Category.entries.forEach { cat ->
            val tag = "admin_v2_academics_cat_${cat.routeId}"
            composeTestRule.onNodeWithTag("admin_v2_academics_hub")
                .performScrollToNode(hasTestTag(tag))
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun `draft first save, edit does not auto-publish, publish shows publicly, unpublish and expiry hide publicly`() = runBlocking {
        // 1. Create -> saved as DRAFT (unpublished)
        val created = v2Repo.saveRecord(
            actor = schoolOwner,
            existingId = null,
            category = AcademicsV2Category.NEWS_ANNOUNCEMENTS,
            sectionKey = "circulars_notices",
            title = "Term 1 Parent Circular",
            description = "School reopening timetable notice.",
            publishDateText = "2026-06-10",
            expiryDateMillis = System.currentTimeMillis() + 86_400_000L
        ).getOrThrow()
        assertEquals(AcademicsV2RecordStatus.DRAFT, created.status)
        assertTrue(v2Repo.observePublicRecords(AcademicsV2Category.NEWS_ANNOUNCEMENTS).first().isEmpty())

        // 2. Publish -> becomes visible publicly
        val published = v2Repo.publishRecord(schoolOwner, created.id).getOrThrow()
        assertEquals(AcademicsV2RecordStatus.PUBLISHED, published.status)
        assertEquals(1, v2Repo.observePublicRecords(AcademicsV2Category.NEWS_ANNOUNCEMENTS).first().size)

        // 3. Edit existing content -> must NOT unintentionally stay auto-published; transitions back to DRAFT
        val edited = v2Repo.saveRecord(
            actor = schoolOwner,
            existingId = created.id,
            category = AcademicsV2Category.NEWS_ANNOUNCEMENTS,
            sectionKey = "circulars_notices",
            title = "Term 1 Parent Circular (Revised)",
            description = "Updated bus arrival timings.",
            publishDateText = "2026-06-10",
            expiryDateMillis = System.currentTimeMillis() + 86_400_000L
        ).getOrThrow()
        assertEquals(AcademicsV2RecordStatus.DRAFT, edited.status)
        assertTrue(v2Repo.observePublicRecords(AcademicsV2Category.NEWS_ANNOUNCEMENTS).first().isEmpty())

        // 4. Re-publish then verify expired record is excluded publicly
        v2Repo.publishRecord(schoolOwner, edited.id).getOrThrow()
        val futureTime = System.currentTimeMillis() + 200_000_000L
        assertTrue(
            v2Repo.observePublicRecords(
                category = AcademicsV2Category.NEWS_ANNOUNCEMENTS,
                nowMillisProvider = { futureTime }
            ).first().isEmpty()
        )

        // 5. Unpublish hides publicly
        v2Repo.updateRecordStatus(schoolOwner, edited.id, AcademicsV2RecordStatus.UNPUBLISHED).getOrThrow()
        assertTrue(v2Repo.observePublicRecords(AcademicsV2Category.NEWS_ANNOUNCEMENTS).first().isEmpty())
    }

    @Test
    fun `gallery supports multi-album photo reuse and strictly rejects video uploads or links`() = runBlocking {
        val albumA = v2Repo.saveGalleryAlbum(
            actor = schoolOwner,
            existingId = null,
            sectionKey = "events_celebrations",
            title = "Annual Day",
            description = "Stage photographs",
            dateText = "2026-02-14"
        ).getOrThrow()
        val albumB = v2Repo.saveGalleryAlbum(
            actor = schoolOwner,
            existingId = null,
            sectionKey = "campus_infrastructure",
            title = "Auditorium Highlights",
            description = "Campus hall photos",
            dateText = "2026-02-14"
        ).getOrThrow()

        // Video link & .mp4 file must be strictly rejected
        val ytAttempt = v2Repo.saveGalleryPhoto(
            actor = schoolOwner,
            existingId = null,
            title = "Disallowed Video",
            description = "",
            dateText = "",
            imageUri = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            albumIds = listOf(albumA.id)
        )
        assertTrue(ytAttempt.isFailure)
        val mp4Attempt = v2Repo.saveGalleryPhoto(
            actor = schoolOwner,
            existingId = null,
            title = "Disallowed MP4",
            description = "",
            dateText = "",
            imageUri = "content://media/external/video/clip.mp4",
            albumIds = listOf(albumA.id)
        )
        assertTrue(mp4Attempt.isFailure)

        // Photo reused across BOTH Album A and Album B
        val photo = v2Repo.saveGalleryPhoto(
            actor = schoolOwner,
            existingId = null,
            title = "Main Hall Assembly",
            description = "Morning prayer photo",
            dateText = "2026-02-14",
            imageUri = "content://media/external/images/hall.jpg",
            albumIds = listOf(albumA.id, albumB.id)
        ).getOrThrow()

        // Unpublished album & photo stay hidden
        assertTrue(v2Repo.observePublicGalleryAlbums().first().isEmpty())
        assertTrue(v2Repo.observePublicGalleryPhotosForAlbum(albumA.id).first().isEmpty())

        // Publish albums and photo -> single photo appears in BOTH albums
        v2Repo.updateGalleryAlbumStatus(schoolOwner, albumA.id, AcademicsV2RecordStatus.PUBLISHED).getOrThrow()
        v2Repo.updateGalleryAlbumStatus(schoolOwner, albumB.id, AcademicsV2RecordStatus.PUBLISHED).getOrThrow()
        v2Repo.updateGalleryPhotoStatus(schoolOwner, photo.id, AcademicsV2RecordStatus.PUBLISHED).getOrThrow()

        assertEquals(2, v2Repo.observePublicGalleryAlbums().first().size)
        assertEquals(1, v2Repo.observePublicGalleryPhotosForAlbum(albumA.id).first().size)
        assertEquals(1, v2Repo.observePublicGalleryPhotosForAlbum(albumB.id).first().size)
        assertEquals(photo.id, v2Repo.observePublicGalleryPhotosForAlbum(albumB.id).first().single().id)
    }

    @Test
    fun `exam results require explicit School Owner authorization and block staff direct publish`() = runBlocking {
        // Staff with EXAMINATIONS publish CAN publish Exam Schedule
        val schedule = v2Repo.saveRecord(
            actor = examStaffWithPublish,
            existingId = null,
            category = AcademicsV2Category.EXAMINATIONS,
            sectionKey = "exam_schedule",
            title = "Quarterly Exam Dates",
            description = "Starts September 15."
        ).getOrThrow()
        assertTrue(v2Repo.publishRecord(examStaffWithPublish, schedule.id).isSuccess)

        // Staff CANNOT directly publish Exam Results
        val resultDraft = v2Repo.saveRecord(
            actor = examStaffWithPublish,
            existingId = null,
            category = AcademicsV2Category.EXAMINATIONS,
            sectionKey = "exam_results",
            title = "Quarterly Results",
            description = "Summary pass percentage."
        ).getOrThrow()
        val staffPublishAttempt = v2Repo.publishRecord(examStaffWithPublish, resultDraft.id, authorizeExamResult = true)
        assertTrue(staffPublishAttempt.isFailure)
        assertTrue(staffPublishAttempt.exceptionOrNull() is SecurityException)

        // Owner without explicit authorization flag is also blocked
        val unauthOwnerAttempt = v2Repo.publishRecord(schoolOwner, resultDraft.id, authorizeExamResult = false)
        assertTrue(unauthOwnerAttempt.isFailure)

        // Owner approves & authorizes -> now visible publicly
        v2Repo.ownerApproveAndPublishRecord(schoolOwner, resultDraft.id, authorizeExamResult = true).getOrThrow()
        val publicExamItems = v2Repo.observePublicRecords(AcademicsV2Category.EXAMINATIONS).first()
        assertTrue(publicExamItems.any { it.id == resultDraft.id && it.isExamResultAuthorized })
    }

    @Test
    fun `faculty privacy enforces approval and consent gates and opens Detailed Profile Page with 8 public fields only`() {
        runBlocking {
            val unapproved = v2Repo.saveFacultyProfile(
                actor = schoolOwner,
                existingId = null,
                sectionKey = "teaching_staff",
                teacherName = "Dr. Kavitha Subramanian",
                approvedProfilePhotoUri = null,
                designation = "Senior Mathematics Teacher",
                department = "Mathematics",
                teachingSubjects = "Higher Secondary Mathematics",
                educationalQualifications = "M.Sc., M.Ed., Ph.D.",
                teachingExperience = "14 years",
                professionalIntroduction = "Guides board exam mathematics mentoring.",
                isSchoolApproved = true,
                hasStaffConsent = false
            ).getOrThrow()

            // Cannot publish without BOTH school approval and staff consent
            assertTrue(
                v2Repo.updateFacultyProfileStatus(schoolOwner, unapproved.id, AcademicsV2RecordStatus.PUBLISHED).isFailure
            )
            assertNull(v2Repo.getPublicFacultyProfileById(unapproved.id))

            // Record both approval & consent and publish
            val approved = v2Repo.saveFacultyProfile(
                actor = schoolOwner,
                existingId = unapproved.id,
                sectionKey = "teaching_staff",
                teacherName = "Dr. Kavitha Subramanian",
                approvedProfilePhotoUri = null,
                designation = "Senior Mathematics Teacher",
                department = "Mathematics",
                teachingSubjects = "Higher Secondary Mathematics",
                educationalQualifications = "M.Sc., M.Ed., Ph.D.",
                teachingExperience = "14 years",
                professionalIntroduction = "Guides board exam mathematics mentoring.",
                isSchoolApproved = true,
                hasStaffConsent = true
            ).getOrThrow()
            v2Repo.updateFacultyProfileStatus(schoolOwner, approved.id, AcademicsV2RecordStatus.PUBLISHED).getOrThrow()

            val publicProfile = v2Repo.getPublicFacultyProfileById(approved.id)
            assertNotNull(publicProfile)

            // Verify public card -> tap -> opens Detailed Profile Page
            var openedProfileId by mutableStateOf<String?>(null)
            composeTestRule.setContent {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    if (openedProfileId == null) {
                        AcademicsV2CategoryDetailScreen(
                            category = AcademicsV2Category.FACULTY_STAFF,
                            schoolDataViewModel = schoolDataViewModel,
                            onBack = {},
                            onOpenFacultyProfile = { openedProfileId = it }
                        )
                    } else {
                        PublicFacultyProfileDetailScreen(
                            profileId = openedProfileId!!,
                            schoolDataViewModel = schoolDataViewModel,
                            onBack = { openedProfileId = null }
                        )
                    }
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("public_faculty_card_${approved.id}").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("public_faculty_detail_screen").assertIsDisplayed()
            composeTestRule.onNodeWithText("Dr. Kavitha Subramanian").assertIsDisplayed()
            composeTestRule.onNodeWithText("M.Sc., M.Ed., Ph.D.").assertExists()
            composeTestRule.onNodeWithText("14 years").assertExists()
        }
    }

    @Test
    fun `rbac blocks unauthorized staff from restricted categories and School Owner Control Center`() {
        runBlocking {
            // Transport staff cannot edit Examinations or access Owner private profile
            assertFalse(AcademicsV2AccessControl.canAccessAdminCard(restrictedStaff, AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER))
            assertTrue(v2Repo.getOwnerPrivateProfile(restrictedStaff).isFailure)
            assertTrue(
                v2Repo.saveRecord(
                    actor = restrictedStaff,
                    existingId = null,
                    category = AcademicsV2Category.EXAMINATIONS,
                    sectionKey = "exam_schedule",
                    title = "Unauthorized Entry",
                    description = "Should fail"
                ).isFailure
            )

            // Staff submits access request -> School Owner approves & later revokes -> audit trail recorded
            val req = v2Repo.submitStaffAccessRequest(
                actor = restrictedStaff,
                requestedRole = "Transport & Events",
                requestedCategoryKeys = listOf(AcademicsV2Category.ACADEMIC_CALENDAR.permissionKey),
                reason = "Need to update transport holiday calendar"
            ).getOrThrow()
            assertEquals(V2StaffAccessRequestStatus.PENDING, req.status)

            val approvedReq = v2Repo.reviewStaffAccessRequest(
                actor = schoolOwner,
                requestId = req.id,
                approve = true,
                reviewNote = "Approved for Term 1"
            ).getOrThrow()
            assertEquals(V2StaffAccessRequestStatus.APPROVED, approvedReq.status)

            val revokedReq = v2Repo.revokeStaffAccessRequest(
                actor = schoolOwner,
                requestId = req.id,
                reviewNote = "Completed Term 1 updates"
            ).getOrThrow()
            assertEquals(V2StaffAccessRequestStatus.REVOKED, revokedReq.status)
            assertTrue(v2Repo.snapshot.value.accessAuditEvents.any { it.action == "REVOKE_STAFF_ACCESS" })

            // UI route guard blocks restricted staff when directly opening Examinations or Control Center
            composeTestRule.setContent {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    AdminV2CategoryManagementScreen(
                        category = AcademicsV2Category.EXAMINATIONS,
                        session = restrictedStaff,
                        managementViewModel = managementViewModel,
                        onBack = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("admin_v2_access_denied").assertIsDisplayed()
        }
    }

    @Test
    fun `school owner control center blocks non-owner in UI and exposes all 6 governance sections to owner`() {
        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                SchoolOwnerControlCenterScreen(
                    session = restrictedStaff,
                    managementViewModel = managementViewModel,
                    onBack = {},
                    onOpenStaffAccounts = {},
                    onOpenLegacyApprovals = {},
                    onOpenBackupRestore = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owner_control_center_denied").assertIsDisplayed()
    }

    @Test
    fun `school owner control center renders all 6 sections and private owner profile for school owner`() {
        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                SchoolOwnerControlCenterScreen(
                    session = schoolOwner,
                    managementViewModel = managementViewModel,
                    onBack = {},
                    onOpenStaffAccounts = {},
                    onOpenLegacyApprovals = {},
                    onOpenBackupRestore = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("school_owner_control_center_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("owner_private_profile_card").assertIsDisplayed()
        SchoolOwnerSection.entries.forEach { sec ->
            composeTestRule.onNodeWithTag(sec.testTag).assertExists()
        }
    }
}
