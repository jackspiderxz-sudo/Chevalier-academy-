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
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.SyncState
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.media.YouTubeMediaHelper
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
            ManagementSection.SCHOOL_POSTS -> {
                val item = database.schoolPostDao().getById(recordId) ?: return null
                val displayTitle = item.title.ifBlank {
                    YouTubeMediaHelper.extractVideoId(item.youtubeUrl)?.let { "YouTube Video ($it)" }
                        ?: "School Post #${item.id}"
                }
                LiveRecordInfo(
                    id = item.id,
                    title = displayTitle,
                    snapshotJson = ContentSnapshotSerializer.schoolPostToJson(item),
                    isLivePublished = item.status == SchoolPostStatus.PUBLISHED
                )
            }
            ManagementSection.SCHOOL_HIGHLIGHTS -> {
                val item = database.schoolHighlightDao().getById(recordId) ?: return null
                val displayTitle = item.title.ifBlank {
                    YouTubeMediaHelper.extractVideoId(item.youtubeShortsUrl)?.let { "Highlight ($it)" }
                        ?: "Highlight #${item.id}"
                }
                LiveRecordInfo(
                    id = item.id,
                    title = displayTitle,
                    snapshotJson = ContentSnapshotSerializer.schoolHighlightToJson(item),
                    isLivePublished = item.status == SchoolPostStatus.PUBLISHED
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
        fun nextSyncState(existingSyncState: String?, fallback: String): String = when (existingSyncState) {
            SyncState.SYNCED -> SyncState.PENDING_UPLOAD
            null -> fallback
            else -> existingSyncState
        }
        return when (section) {
            ManagementSection.SCHOOL_PROFILE,
            ManagementSection.ABOUT_SCHOOL,
            ManagementSection.CONTACT_DETAILS -> {
                val parsed = ContentSnapshotSerializer.schoolProfileFromJson(snapshotJson)
                val existing = database.schoolProfileDao().getProfileOnce() ?: ChevalierDatabase.verifiedDefaultProfile()
                val merged = mergeSchoolProfileSectionFields(existing, parsed, section)
                    .copy(
                        id = 1,
                        updatedAtMillis = System.currentTimeMillis(),
                        remoteId = existing.remoteId,
                        tenantId = existing.tenantId,
                        syncState = nextSyncState(existing.syncState, existing.syncState),
                        serverUpdatedAtMillis = existing.serverUpdatedAtMillis
                    )
                database.schoolProfileDao().upsertProfile(merged)
                1L
            }
            ManagementSection.LOGO_BANNER -> {
                val parsed = ContentSnapshotSerializer.heroPhotoFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.campusHeroPhotoDao().observeAllForManagement().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.campusHeroPhotoDao().upsert(updated)
            }
            ManagementSection.ACADEMICS -> {
                val parsed = ContentSnapshotSerializer.academicInfoFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.academicInfoDao().observeAll().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    updatedAtMillis = System.currentTimeMillis(),
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.academicInfoDao().upsert(updated)
            }
            ManagementSection.HIGHER_SECONDARY_COURSES -> {
                val parsed = ContentSnapshotSerializer.courseFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.higherSecondaryCourseDao().observeAll().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.higherSecondaryCourseDao().upsert(updated)
            }
            ManagementSection.FACILITIES -> {
                val parsed = ContentSnapshotSerializer.facilityFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.facilityDao().observeAll().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.facilityDao().upsert(updated)
            }
            ManagementSection.TRANSPORT -> {
                val parsed = ContentSnapshotSerializer.transportFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.transportDao().observeAll().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.transportDao().upsert(updated)
            }
            ManagementSection.NEWS -> {
                val parsed = ContentSnapshotSerializer.newsFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.newsDao().observeAll().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.newsDao().upsert(updated)
            }
            ManagementSection.ANNOUNCEMENTS -> {
                val parsed = ContentSnapshotSerializer.announcementFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.announcementDao().observeAll().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.announcementDao().upsert(updated)
            }
            ManagementSection.PHOTO_GALLERY -> {
                val isMedia = org.json.JSONObject(snapshotJson).has("albumId")
                if (isMedia) {
                    val parsed = ContentSnapshotSerializer.galleryMediaFromJson(snapshotJson, publishImmediately)
                    val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                    val existing = if (resolvedId > 0L) {
                        database.galleryDao().observeAllMedia().first().firstOrNull { it.id == resolvedId }
                    } else null
                    val updated = parsed.copy(
                        id = existing?.id ?: resolvedId,
                        isVerified = publishImmediately,
                        isPublished = publishImmediately,
                        isApproved = publishImmediately,
                        approvalStatus = statusString,
                        remoteId = existing?.remoteId ?: parsed.remoteId,
                        tenantId = existing?.tenantId ?: parsed.tenantId,
                        syncState = nextSyncState(existing?.syncState, parsed.syncState),
                        serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                        deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                    )
                    database.galleryDao().upsertMedia(updated)
                } else {
                    val parsed = ContentSnapshotSerializer.galleryAlbumFromJson(snapshotJson, publishImmediately)
                    val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                    val existing = if (resolvedId > 0L) {
                        database.galleryDao().observeAlbumsWithMedia().first().firstOrNull { it.album.id == resolvedId }?.album
                    } else null
                    val updated = parsed.copy(
                        id = existing?.id ?: resolvedId,
                        isVerified = publishImmediately,
                        isPublished = publishImmediately,
                        isApproved = publishImmediately,
                        approvalStatus = statusString,
                        remoteId = existing?.remoteId ?: parsed.remoteId,
                        tenantId = existing?.tenantId ?: parsed.tenantId,
                        syncState = nextSyncState(existing?.syncState, parsed.syncState),
                        serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                        deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                    )
                    database.galleryDao().upsertAlbum(updated)
                }
            }
            ManagementSection.SOCIAL_MEDIA -> {
                val parsed = ContentSnapshotSerializer.socialFromJson(snapshotJson, publishImmediately)
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) {
                    database.socialMediaDao().observeAll().first().firstOrNull { it.id == resolvedId }
                } else null
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    isVerified = publishImmediately,
                    isPublished = publishImmediately,
                    isApproved = publishImmediately,
                    approvalStatus = statusString,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.socialMediaDao().upsert(updated)
            }
            ManagementSection.SCHOOL_POSTS -> {
                val parsed = ContentSnapshotSerializer.schoolPostFromJson(
                    jsonStr = snapshotJson,
                    publishNow = publishImmediately,
                    forcedStatus = forcedStatus
                )
                require(parsed.postType in SchoolPostType.all) {
                    "Unsupported school post type: ${parsed.postType}"
                }
                val cleanTitle = parsed.title.trim()
                val cleanYoutube = parsed.youtubeUrl?.trim()?.takeIf { it.isNotEmpty() }
                val photos = parsed.photoUris()
                when (parsed.postType) {
                    SchoolPostType.YOUTUBE_DIRECT -> {
                        require(YouTubeMediaHelper.isValidYouTubeUrl(cleanYoutube)) {
                            "A valid YouTube URL is required for YouTube Direct Posts."
                        }
                    }
                    SchoolPostType.ADMIN_CUSTOM_VIDEO -> {
                        require(YouTubeMediaHelper.isValidYouTubeUrl(cleanYoutube)) {
                            "A valid YouTube URL is required for Admin Custom Video Posts."
                        }
                        require(cleanTitle.isNotEmpty()) {
                            "Title is required for Admin Custom Video Posts."
                        }
                    }
                    SchoolPostType.PHOTO -> {
                        require(cleanTitle.isNotEmpty()) {
                            "Title is required for Photo Posts."
                        }
                        require(photos.isNotEmpty()) {
                            "Photo Posts require at least 1 photo."
                        }
                        require(photos.size <= SchoolPostEntity.MAX_PHOTO_COUNT) {
                            "Photo Posts allow a maximum of ${SchoolPostEntity.MAX_PHOTO_COUNT} photos."
                        }
                    }
                }
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) database.schoolPostDao().getById(resolvedId) else null
                val now = System.currentTimeMillis()
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    title = cleanTitle,
                    description = parsed.description.trim(),
                    youtubeUrl = if (parsed.postType == SchoolPostType.PHOTO) null else cleanYoutube,
                    customThumbnailUri = if (parsed.postType == SchoolPostType.ADMIN_CUSTOM_VIDEO) parsed.customThumbnailUri else null,
                    photoUrisJson = if (parsed.postType == SchoolPostType.PHOTO) {
                        SchoolPostEntity.encodePhotoUris(photos)
                    } else {
                        "[]"
                    },
                    createdAtMillis = existing?.createdAtMillis ?: parsed.createdAtMillis,
                    publishedAtMillis = if (parsed.status == SchoolPostStatus.PUBLISHED) {
                        existing?.publishedAtMillis ?: parsed.publishedAtMillis ?: now
                    } else {
                        existing?.publishedAtMillis ?: parsed.publishedAtMillis
                    },
                    updatedAtMillis = now,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.schoolPostDao().upsert(updated)
            }
            ManagementSection.SCHOOL_HIGHLIGHTS -> {
                val parsed = ContentSnapshotSerializer.schoolHighlightFromJson(
                    jsonStr = snapshotJson,
                    publishNow = publishImmediately,
                    forcedStatus = forcedStatus
                )
                val ytParsed = YouTubeMediaHelper.parse(parsed.youtubeShortsUrl)
                    ?: throw IllegalArgumentException("A valid YouTube Shorts URL is required.")
                require(ytParsed.isShort) {
                    "Highlight URL must be a YouTube Shorts link (youtube.com/shorts/...)."
                }
                val resolvedId = if (targetRecordId > 0L) targetRecordId else parsed.id
                val existing = if (resolvedId > 0L) database.schoolHighlightDao().getById(resolvedId) else null
                val now = System.currentTimeMillis()
                val transitioningToPublished = publishImmediately && existing != null && existing.status != SchoolPostStatus.PUBLISHED
                val finalPublishedAt = when {
                    existing != null && existing.status == SchoolPostStatus.PUBLISHED -> existing.publishedAtMillis
                    transitioningToPublished -> now
                    else -> parsed.publishedAtMillis
                }
                val finalExpiresAt = when {
                    existing != null && existing.status == SchoolPostStatus.PUBLISHED -> existing.expiresAtMillis
                    transitioningToPublished -> SchoolHighlightEntity.computeExpiryMillis(now)
                    else -> parsed.expiresAtMillis
                }
                val updated = parsed.copy(
                    id = existing?.id ?: resolvedId,
                    title = parsed.title.trim(),
                    youtubeShortsUrl = ytParsed.canonicalWatchUrl,
                    createdAtMillis = existing?.createdAtMillis ?: parsed.createdAtMillis,
                    publishedAtMillis = finalPublishedAt,
                    expiresAtMillis = finalExpiresAt,
                    remoteId = existing?.remoteId ?: parsed.remoteId,
                    tenantId = existing?.tenantId ?: parsed.tenantId,
                    syncState = nextSyncState(existing?.syncState, parsed.syncState),
                    serverUpdatedAtMillis = existing?.serverUpdatedAtMillis ?: parsed.serverUpdatedAtMillis,
                    deletedAtMillis = existing?.deletedAtMillis ?: parsed.deletedAtMillis
                )
                database.schoolHighlightDao().upsert(updated)
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
            ManagementSection.SCHOOL_POSTS -> database.schoolPostDao().deleteById(recordId)
            ManagementSection.SCHOOL_HIGHLIGHTS -> database.schoolHighlightDao().deleteById(recordId)
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
