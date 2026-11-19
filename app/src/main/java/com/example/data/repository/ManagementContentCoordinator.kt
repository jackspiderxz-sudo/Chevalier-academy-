package com.example.data.repository

import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.TransportRouteEntity
import kotlinx.coroutines.flow.first

internal class ManagementContentCoordinator(
    private val database: ChevalierDatabase
) {
    suspend fun fetchLiveRecordSnapshot(
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean = false
    ): LiveRecordInfo? {
        return when (section) {
            ManagementSection.SCHOOL_PROFILE,
            ManagementSection.ABOUT_SCHOOL,
            ManagementSection.CONTACT_DETAILS -> {
                val p = database.schoolProfileDao().getProfileOnce() ?: return null
                LiveRecordInfo(
                    id = 1L,
                    title = p.shortName,
                    snapshotJson = ContentSnapshotSerializer.schoolProfileToJson(p),
                    isLivePublished = true
                )
            }
            ManagementSection.LOGO_BANNER -> {
                val item = database.campusHeroPhotoDao().observeAllForManagement().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = item.caption ?: "Campus Banner #${item.id}",
                    snapshotJson = ContentSnapshotSerializer.heroPhotoToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            ManagementSection.ACADEMICS -> {
                val item = database.academicInfoDao().observeAll().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = item.title,
                    snapshotJson = ContentSnapshotSerializer.academicInfoToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            ManagementSection.HIGHER_SECONDARY_COURSES -> {
                val item = database.higherSecondaryCourseDao().observeAll().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = "${item.groupCode} - ${item.streamTitle}",
                    snapshotJson = ContentSnapshotSerializer.courseToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            ManagementSection.FACILITIES -> {
                val item = database.facilityDao().observeAll().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = item.name,
                    snapshotJson = ContentSnapshotSerializer.facilityToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            ManagementSection.TRANSPORT -> {
                val item = database.transportDao().observeAll().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = "${item.routeNumber} - ${item.routeName}",
                    snapshotJson = ContentSnapshotSerializer.transportToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            ManagementSection.NEWS -> {
                val item = database.newsDao().observeAll().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = item.title,
                    snapshotJson = ContentSnapshotSerializer.newsToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            ManagementSection.ANNOUNCEMENTS -> {
                val item = database.announcementDao().observeAll().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = item.title,
                    snapshotJson = ContentSnapshotSerializer.announcementToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            ManagementSection.PHOTO_GALLERY -> {
                if (isGalleryMediaItem) {
                    val media = database.galleryDao().observeAllMedia().first().firstOrNull { it.id == recordId } ?: return null
                    LiveRecordInfo(
                        id = media.id,
                        title = media.title ?: "Gallery Item #${media.id}",
                        snapshotJson = ContentSnapshotSerializer.galleryMediaToJson(media),
                        isLivePublished = media.isPublished && media.isApproved && media.approvalStatus == ApprovalStatus.APPROVED
                    )
                } else {
                    val album = database.galleryDao().observeAlbumsWithMedia().first().firstOrNull { it.album.id == recordId }?.album ?: return null
                    LiveRecordInfo(
                        id = album.id,
                        title = album.title,
                        snapshotJson = ContentSnapshotSerializer.galleryAlbumToJson(album),
                        isLivePublished = album.isPublished && album.isApproved && album.approvalStatus == ApprovalStatus.APPROVED
                    )
                }
            }
            ManagementSection.SOCIAL_MEDIA -> {
                val item = database.socialMediaDao().observeAll().first().firstOrNull { it.id == recordId } ?: return null
                LiveRecordInfo(
                    id = item.id,
                    title = "${item.platformName}: ${item.handleOrLabel}",
                    snapshotJson = ContentSnapshotSerializer.socialToJson(item),
                    isLivePublished = item.isPublished && item.isApproved && item.approvalStatus == ApprovalStatus.APPROVED
                )
            }
            else -> null
        }
    }

    suspend fun applySnapshotJsonToEntityTable(
        section: ManagementSection,
        snapshotJson: String,
        targetRecordId: Long,
        publishImmediately: Boolean,
        forcedStatus: String? = null
    ): Long {
        val statusString = forcedStatus ?: if (publishImmediately) ApprovalStatus.APPROVED else ApprovalStatus.PENDING
        return when (section) {
            ManagementSection.SCHOOL_PROFILE,
            ManagementSection.ABOUT_SCHOOL,
            ManagementSection.CONTACT_DETAILS -> {
                val parsed = ContentSnapshotSerializer.schoolProfileFromJson(snapshotJson)
                val existing = database.schoolProfileDao().getProfileOnce() ?: ChevalierDatabase.verifiedDefaultProfile()
                val merged = mergeSchoolProfileSectionFields(existing, parsed, section)
                    .copy(id = 1, updatedAtMillis = System.currentTimeMillis())
                database.schoolProfileDao().upsertProfile(merged)
                1L
            }
            ManagementSection.LOGO_BANNER -> {
                val parsed = ContentSnapshotSerializer.heroPhotoFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString
                )
                database.campusHeroPhotoDao().upsert(updated)
            }
            ManagementSection.ACADEMICS -> {
                val parsed = ContentSnapshotSerializer.academicInfoFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    updatedAtMillis = System.currentTimeMillis()
                )
                database.academicInfoDao().upsert(updated)
            }
            ManagementSection.HIGHER_SECONDARY_COURSES -> {
                val parsed = ContentSnapshotSerializer.courseFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString
                )
                database.higherSecondaryCourseDao().upsert(updated)
            }
            ManagementSection.FACILITIES -> {
                val parsed = ContentSnapshotSerializer.facilityFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString
                )
                database.facilityDao().upsert(updated)
            }
            ManagementSection.TRANSPORT -> {
                val parsed = ContentSnapshotSerializer.transportFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString
                )
                database.transportDao().upsert(updated)
            }
            ManagementSection.NEWS -> {
                val parsed = ContentSnapshotSerializer.newsFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString
                )
                database.newsDao().upsert(updated)
            }
            ManagementSection.ANNOUNCEMENTS -> {
                val parsed = ContentSnapshotSerializer.announcementFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString
                )
                database.announcementDao().upsert(updated)
            }
            ManagementSection.PHOTO_GALLERY -> {
                val isMedia = org.json.JSONObject(snapshotJson).has("albumId")
                if (isMedia) {
                    val parsed = ContentSnapshotSerializer.galleryMediaFromJson(snapshotJson, publishImmediately)
                    val updated = parsed.copy(
                        id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                        isVerified = publishImmediately,
                        isPublished = publishImmediately,
                        isApproved = publishImmediately,
                        approvalStatus = statusString
                    )
                    database.galleryDao().upsertMedia(updated)
                } else {
                    val parsed = ContentSnapshotSerializer.galleryAlbumFromJson(snapshotJson, publishImmediately)
                    val updated = parsed.copy(
                        id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                        isVerified = publishImmediately,
                        isPublished = publishImmediately,
                        isApproved = publishImmediately,
                        approvalStatus = statusString
                    )
                    database.galleryDao().upsertAlbum(updated)
                }
            }
            ManagementSection.SOCIAL_MEDIA -> {
                val parsed = ContentSnapshotSerializer.socialFromJson(snapshotJson, publishImmediately)
                val updated = parsed.copy(
                    id = if (targetRecordId > 0L) targetRecordId else parsed.id,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString
                )
                database.socialMediaDao().upsert(updated)
            }
            else -> targetRecordId
        }
    }

    suspend fun deleteRecordFromEntityTable(section: ManagementSection, recordId: Long, isGalleryMediaItem: Boolean = false) {
        when (section) {
            ManagementSection.LOGO_BANNER -> database.campusHeroPhotoDao().deleteById(recordId)
            ManagementSection.ACADEMICS -> database.academicInfoDao().deleteById(recordId)
            ManagementSection.HIGHER_SECONDARY_COURSES -> database.higherSecondaryCourseDao().deleteById(recordId)
            ManagementSection.FACILITIES -> database.facilityDao().deleteById(recordId)
            ManagementSection.TRANSPORT -> database.transportDao().deleteById(recordId)
            ManagementSection.NEWS -> database.newsDao().deleteById(recordId)
            ManagementSection.ANNOUNCEMENTS -> database.announcementDao().deleteById(recordId)
            ManagementSection.PHOTO_GALLERY -> {
                if (isGalleryMediaItem) {
                    database.galleryDao().deleteMediaById(recordId)
                } else {
                    database.galleryDao().deleteAlbumById(recordId)
                }
            }
            ManagementSection.SOCIAL_MEDIA -> database.socialMediaDao().deleteById(recordId)
            else -> Unit
        }
    }

    data class LiveRecordInfo(
        val id: Long,
        val title: String,
        val snapshotJson: String,
        val isLivePublished: Boolean
    )
}

internal fun mergeSchoolProfileSectionFields(
    base: SchoolProfileEntity,
    source: SchoolProfileEntity,
    section: ManagementSection
): SchoolProfileEntity = when (section) {
    ManagementSection.SCHOOL_PROFILE -> base.copy(
        schoolName = source.schoolName,
        shortName = source.shortName,
        motto = source.motto,
        addressLine = source.addressLine,
        affiliationNumber = source.affiliationNumber,
        welcomeIntroduction = source.welcomeIntroduction,
        mediumAndGradesNote = source.mediumAndGradesNote,
        profileImageUri = source.profileImageUri
    )
    ManagementSection.ABOUT_SCHOOL -> base.copy(
        principalName = source.principalName,
        history = source.history,
        vision = source.vision,
        mission = source.mission,
        aboutImageUri = source.aboutImageUri
    )
    ManagementSection.CONTACT_DETAILS -> base.copy(
        addressLine = source.addressLine,
        phone = source.phone,
        email = source.email,
        website = source.website,
        officeHours = source.officeHours,
        mapCoordinatesOrQuery = source.mapCoordinatesOrQuery
    )
    else -> source
}
