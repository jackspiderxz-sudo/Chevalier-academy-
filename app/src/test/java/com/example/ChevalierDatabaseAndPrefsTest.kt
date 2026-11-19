package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.SchoolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChevalierDatabaseAndPrefsTest {

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var repository: SchoolRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SchoolRepository.fromDatabase(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verified seed profile leaves unverified fields null and other tables empty`() = runTest {
        repository.ensureVerifiedProfileSeeded()

        val profile = repository.schoolProfile.first()
        assertEquals("CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL", profile?.schoolName)
        assertEquals("Learn to Live & Love", profile?.motto)
        assertEquals("Siluvathur Road, Dindigul - 624 306", profile?.addressLine)
        assertEquals("POWERED BY JK LABS", profile?.poweredBy)
        assertNull(profile?.welcomeIntroduction)
        assertNull(profile?.mediumAndGradesNote)
        assertNull(profile?.phone)
        assertNull(profile?.email)
        assertNull(profile?.principalName)

        assertTrue(repository.campusHeroPhotos.first().isEmpty())
        assertTrue(repository.academicInfo.first().isEmpty())
        assertTrue(repository.higherSecondaryCourses.first().isEmpty())
        assertTrue(repository.facilities.first().isEmpty())
        assertTrue(repository.transportRoutes.first().isEmpty())
        assertTrue(repository.newsItems.first().isEmpty())
        assertTrue(repository.announcements.first().isEmpty())
        assertTrue(repository.galleryAlbumsWithMedia.first().isEmpty())
        assertTrue(repository.socialMediaLinks.first().isEmpty())
    }

    @Test
    fun `public flows exclude draft, pending, rejected, and unpublished records`() = runTest {
        // Unpublished / pending / rejected records must stay hidden from public UI
        repository.upsertNewsItem(
            NewsItemEntity(
                title = "Draft News",
                summary = "Draft",
                fullBody = "Draft body",
                publishedDateText = "12 Nov 2026",
                isVerified = true,
                isPublished = false,
                isApproved = true,
                approvalStatus = ApprovalStatus.DRAFT
            )
        )
        repository.upsertNewsItem(
            NewsItemEntity(
                title = "Pending News",
                summary = "Pending",
                fullBody = "Pending body",
                publishedDateText = "12 Nov 2026",
                isVerified = true,
                isPublished = true,
                isApproved = false,
                approvalStatus = ApprovalStatus.PENDING
            )
        )
        repository.upsertNewsItem(
            NewsItemEntity(
                title = "Rejected News",
                summary = "Rejected",
                fullBody = "Rejected body",
                publishedDateText = "12 Nov 2026",
                isVerified = true,
                isPublished = true,
                isApproved = false,
                approvalStatus = ApprovalStatus.REJECTED
            )
        )
        repository.upsertCampusHeroPhoto(
            CampusHeroPhotoEntity(
                imageUri = "content://unverified/1",
                isVerified = false,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        repository.upsertCourse(
            HigherSecondaryCourseEntity(
                streamTitle = "Unapproved Bio-Maths",
                groupCode = "GROUP_1",
                subjectsSummary = "Pending",
                mediumOfInstruction = "English",
                isVerified = false,
                isPublished = false,
                isApproved = false,
                approvalStatus = ApprovalStatus.PENDING
            )
        )
        repository.upsertSocialMediaLink(
            SocialMediaLinkEntity(
                platformName = "Instagram",
                handleOrLabel = "@pending",
                url = "https://instagram.com",
                isVerified = false,
                isPublished = false,
                isApproved = false,
                approvalStatus = ApprovalStatus.PENDING
            )
        )

        assertTrue(repository.newsItems.first().isEmpty())
        assertTrue(repository.campusHeroPhotos.first().isEmpty())
        assertTrue(repository.higherSecondaryCourses.first().isEmpty())
        assertTrue(repository.socialMediaLinks.first().isEmpty())
    }

    @Test
    fun `public flows expose strictly verified, published, and approved records`() = runTest {
        repository.upsertCourse(
            HigherSecondaryCourseEntity(
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
        )
        repository.upsertAnnouncement(
            AnnouncementEntity(
                title = "Verified Circular",
                message = "Management verified update.",
                issuedDateText = "12 Nov 2026",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        val albumId = repository.upsertGalleryAlbum(
            GalleryAlbumEntity(
                title = "Campus",
                category = "Campus",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        repository.upsertGalleryMedia(
            GalleryMediaEntity(
                albumId = albumId,
                mediaUri = "content://verified/1",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED
            )
        )
        repository.upsertGalleryMedia(
            GalleryMediaEntity(
                albumId = albumId,
                mediaUri = "content://pending/2",
                isVerified = false,
                isPublished = false,
                isApproved = false,
                approvalStatus = ApprovalStatus.PENDING
            )
        )

        assertEquals(1, repository.higherSecondaryCourses.first().size)
        assertEquals(1, repository.announcements.first().size)
        val albums = repository.galleryAlbumsWithMedia.first()
        assertEquals(1, albums.size)
        assertEquals(1, albums.first().mediaItems.size)
        assertEquals("content://verified/1", albums.first().mediaItems.first().mediaUri)
    }

    @Test
    fun `user preferences persist language and theme mode in DataStore`() = runTest {
        val prefsRepo = UserPreferencesRepository(context)
        prefsRepo.restorePreferences(
            language = AppLanguage.ENGLISH,
            isOnboarded = false,
            themeMode = AppThemeMode.SYSTEM
        )
        // Previewing a language before tapping Continue must not skip onboarding yet
        prefsRepo.setLanguage(AppLanguage.HINDI, markSelectedOnLaunch = false)
        val previewState = prefsRepo.preferencesFlow.first()
        assertEquals(AppLanguage.HINDI, previewState.selectedLanguage)
        assertEquals(false, previewState.isLanguageSelectedOnLaunch)

        // Confirming selection marks onboarding complete and persists language code
        prefsRepo.setLanguage(AppLanguage.TAMIL, markSelectedOnLaunch = true)
        prefsRepo.setThemeMode(AppThemeMode.DARK)

        val recreatedRepo = UserPreferencesRepository(context)
        val restoredState = recreatedRepo.preferencesFlow.first()
        assertEquals(AppLanguage.TAMIL, restoredState.selectedLanguage)
        assertTrue(restoredState.isLanguageSelectedOnLaunch)
        assertEquals(AppThemeMode.DARK, restoredState.themeMode)
    }
}
