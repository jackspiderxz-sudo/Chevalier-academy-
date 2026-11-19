package com.example.data.repository

import com.example.data.local.ChevalierDatabase
import com.example.data.local.dao.AcademicInfoDao
import com.example.data.local.dao.AnnouncementDao
import com.example.data.local.dao.CampusHeroPhotoDao
import com.example.data.local.dao.FacilityDao
import com.example.data.local.dao.GalleryDao
import com.example.data.local.dao.HigherSecondaryCourseDao
import com.example.data.local.dao.NewsDao
import com.example.data.local.dao.SchoolProfileDao
import com.example.data.local.dao.SocialMediaDao
import com.example.data.local.dao.TransportDao
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.TransportRouteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SchoolRepository(
    private val schoolProfileDao: SchoolProfileDao,
    private val campusHeroPhotoDao: CampusHeroPhotoDao,
    private val academicInfoDao: AcademicInfoDao,
    private val courseDao: HigherSecondaryCourseDao,
    private val facilityDao: FacilityDao,
    private val transportDao: TransportDao,
    private val newsDao: NewsDao,
    private val announcementDao: AnnouncementDao,
    private val galleryDao: GalleryDao,
    private val socialMediaDao: SocialMediaDao
) {
    // Public-facing reactive streams (strictly verified, published & approved records only)
    val schoolProfile: Flow<SchoolProfileEntity?> = schoolProfileDao.observeProfile()
    val campusHeroPhotos: Flow<List<CampusHeroPhotoEntity>> = campusHeroPhotoDao.observePublicPhotos()
    val academicInfo: Flow<List<AcademicInfoEntity>> = academicInfoDao.observePublic()
    val higherSecondaryCourses: Flow<List<HigherSecondaryCourseEntity>> = courseDao.observePublic()
    val facilities: Flow<List<FacilityEntity>> = facilityDao.observePublic()
    val transportRoutes: Flow<List<TransportRouteEntity>> = transportDao.observePublic()
    val newsItems: Flow<List<NewsItemEntity>> = newsDao.observePublic()
    val announcements: Flow<List<AnnouncementEntity>> = announcementDao.observePublic()
    val galleryAlbumsWithMedia: Flow<List<GalleryAlbumWithMedia>> =
        galleryDao.observePublicAlbumsWithMedia().map { albums ->
            albums.map { albumWithMedia ->
                albumWithMedia.copy(
                    mediaItems = albumWithMedia.mediaItems
                        .filter {
                            it.isVerified &&
                                it.isPublished &&
                                it.isApproved &&
                                it.approvalStatus == ApprovalStatus.APPROVED
                        }
                        .sortedBy { it.displayOrder }
                )
            }
        }
    val galleryMedia: Flow<List<GalleryMediaEntity>> = galleryDao.observePublicMedia()
    val socialMediaLinks: Flow<List<SocialMediaLinkEntity>> = socialMediaDao.observePublic()

    // Management streams reserved for Prompt 3
    val allNewsForManagement: Flow<List<NewsItemEntity>> = newsDao.observeAll()
    val allAnnouncementsForManagement: Flow<List<AnnouncementEntity>> = announcementDao.observeAll()

    suspend fun ensureVerifiedProfileSeeded() {
        if (schoolProfileDao.getProfileOnce() == null) {
            schoolProfileDao.upsertProfile(ChevalierDatabase.verifiedDefaultProfile())
        }
    }

    // Write operations prepared for Prompt 3 management module
    suspend fun updateSchoolProfile(profile: SchoolProfileEntity) =
        schoolProfileDao.upsertProfile(profile)

    suspend fun upsertCampusHeroPhoto(photo: CampusHeroPhotoEntity): Long =
        campusHeroPhotoDao.upsert(photo)

    suspend fun deleteCampusHeroPhoto(id: Long) =
        campusHeroPhotoDao.deleteById(id)

    suspend fun upsertAcademicInfo(item: AcademicInfoEntity): Long =
        academicInfoDao.upsert(item)

    suspend fun deleteAcademicInfo(id: Long) =
        academicInfoDao.deleteById(id)

    suspend fun upsertCourse(course: HigherSecondaryCourseEntity): Long =
        courseDao.upsert(course)

    suspend fun deleteCourse(id: Long) =
        courseDao.deleteById(id)

    suspend fun upsertFacility(facility: FacilityEntity): Long =
        facilityDao.upsert(facility)

    suspend fun deleteFacility(id: Long) =
        facilityDao.deleteById(id)

    suspend fun upsertTransportRoute(route: TransportRouteEntity): Long =
        transportDao.upsert(route)

    suspend fun deleteTransportRoute(id: Long) =
        transportDao.deleteById(id)

    suspend fun upsertNewsItem(newsItem: NewsItemEntity): Long =
        newsDao.upsert(newsItem)

    suspend fun deleteNewsItem(id: Long) =
        newsDao.deleteById(id)

    suspend fun upsertAnnouncement(announcement: AnnouncementEntity): Long =
        announcementDao.upsert(announcement)

    suspend fun deleteAnnouncement(id: Long) =
        announcementDao.deleteById(id)

    suspend fun upsertGalleryAlbum(album: GalleryAlbumEntity): Long =
        galleryDao.upsertAlbum(album)

    suspend fun upsertGalleryMedia(media: GalleryMediaEntity): Long =
        galleryDao.upsertMedia(media)

    suspend fun deleteGalleryAlbum(albumId: Long) =
        galleryDao.deleteAlbumById(albumId)

    suspend fun upsertSocialMediaLink(link: SocialMediaLinkEntity): Long =
        socialMediaDao.upsert(link)

    suspend fun deleteSocialMediaLink(id: Long) =
        socialMediaDao.deleteById(id)

    companion object {
        fun fromDatabase(db: ChevalierDatabase): SchoolRepository = SchoolRepository(
            schoolProfileDao = db.schoolProfileDao(),
            campusHeroPhotoDao = db.campusHeroPhotoDao(),
            academicInfoDao = db.academicInfoDao(),
            courseDao = db.higherSecondaryCourseDao(),
            facilityDao = db.facilityDao(),
            transportDao = db.transportDao(),
            newsDao = db.newsDao(),
            announcementDao = db.announcementDao(),
            galleryDao = db.galleryDao(),
            socialMediaDao = db.socialMediaDao()
        )
    }
}
