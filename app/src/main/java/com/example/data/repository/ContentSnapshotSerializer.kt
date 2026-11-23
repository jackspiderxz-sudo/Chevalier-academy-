package com.example.data.repository

import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.SyncMetadataDefaults
import com.example.data.local.entity.TransportRouteEntity
import org.json.JSONObject

object ContentSnapshotSerializer {

    fun schoolProfileToJson(p: SchoolProfileEntity): String = JSONObject().apply {
        put("id", p.id)
        put("schoolName", p.schoolName)
        put("shortName", p.shortName)
        put("motto", p.motto)
        put("addressLine", p.addressLine)
        put("emblemMark", p.emblemMark)
        put("poweredBy", p.poweredBy)
        putOpt("welcomeIntroduction", p.welcomeIntroduction)
        putOpt("mediumAndGradesNote", p.mediumAndGradesNote)
        putOpt("mapCoordinatesOrQuery", p.mapCoordinatesOrQuery)
        putOpt("phone", p.phone)
        putOpt("email", p.email)
        putOpt("website", p.website)
        putOpt("officeHours", p.officeHours)
        putOpt("principalName", p.principalName)
        putOpt("history", p.history)
        putOpt("vision", p.vision)
        putOpt("mission", p.mission)
        putOpt("affiliationNumber", p.affiliationNumber)
        put("updatedAtMillis", p.updatedAtMillis)
        putOpt("profileImageUri", p.profileImageUri)
        putOpt("aboutImageUri", p.aboutImageUri)
        putSyncMetadata(
            remoteId = p.remoteId,
            tenantId = p.tenantId,
            syncState = p.syncState,
            serverUpdatedAtMillis = p.serverUpdatedAtMillis,
            includeSoftDelete = false
        )
    }.toString()

    fun schoolProfileFromJson(jsonStr: String): SchoolProfileEntity {
        val o = JSONObject(jsonStr)
        val id = o.optInt("id", 1)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return SchoolProfileEntity(
            id = id,
            schoolName = o.getString("schoolName"),
            shortName = o.getString("shortName"),
            motto = o.getString("motto"),
            addressLine = o.getString("addressLine"),
            emblemMark = o.optString("emblemMark", "M S C"),
            poweredBy = o.optString("poweredBy", "POWERED BY JK LABS"),
            welcomeIntroduction = o.optNullableString("welcomeIntroduction"),
            mediumAndGradesNote = o.optNullableString("mediumAndGradesNote"),
            mapCoordinatesOrQuery = o.optNullableString("mapCoordinatesOrQuery"),
            phone = o.optNullableString("phone"),
            email = o.optNullableString("email"),
            website = o.optNullableString("website"),
            officeHours = o.optNullableString("officeHours"),
            principalName = o.optNullableString("principalName"),
            history = o.optNullableString("history"),
            vision = o.optNullableString("vision"),
            mission = o.optNullableString("mission"),
            affiliationNumber = o.optNullableString("affiliationNumber"),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis()),
            profileImageUri = o.optNullableString("profileImageUri"),
            aboutImageUri = o.optNullableString("aboutImageUri"),
            remoteId = SyncMetadataDefaults.resolveRemoteId(
                candidate = o.optNullableString("remoteId"),
                tableName = "school_profile",
                localId = id.toLong(),
                tenantId = tenantId
            ),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis")
        )
    }

    fun academicInfoToJson(a: AcademicInfoEntity): String = JSONObject().apply {
        put("id", a.id)
        put("title", a.title)
        put("category", a.category)
        put("description", a.description)
        put("displayOrder", a.displayOrder)
        put("isVerified", a.isVerified)
        put("isPublished", a.isPublished)
        put("isApproved", a.isApproved)
        put("approvalStatus", a.approvalStatus)
        put("updatedAtMillis", a.updatedAtMillis)
        putSyncMetadata(a.remoteId, a.tenantId, a.syncState, a.serverUpdatedAtMillis, a.deletedAtMillis)
    }.toString()

    fun academicInfoFromJson(jsonStr: String, publishNow: Boolean): AcademicInfoEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return AcademicInfoEntity(
            id = id,
            title = o.getString("title"),
            category = o.getString("category"),
            description = o.getString("description"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            updatedAtMillis = System.currentTimeMillis(),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "academic_info", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun courseToJson(c: HigherSecondaryCourseEntity): String = JSONObject().apply {
        put("id", c.id)
        put("streamTitle", c.streamTitle)
        put("groupCode", c.groupCode)
        put("subjectsSummary", c.subjectsSummary)
        put("mediumOfInstruction", c.mediumOfInstruction)
        putOpt("classLevelLabel", c.classLevelLabel)
        putOpt("eligibilityNote", c.eligibilityNote)
        put("displayOrder", c.displayOrder)
        put("isVerified", c.isVerified)
        put("isPublished", c.isPublished)
        put("isApproved", c.isApproved)
        put("approvalStatus", c.approvalStatus)
        putSyncMetadata(c.remoteId, c.tenantId, c.syncState, c.serverUpdatedAtMillis, c.deletedAtMillis)
    }.toString()

    fun courseFromJson(jsonStr: String, publishNow: Boolean): HigherSecondaryCourseEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return HigherSecondaryCourseEntity(
            id = id,
            streamTitle = o.getString("streamTitle"),
            groupCode = o.getString("groupCode"),
            subjectsSummary = o.getString("subjectsSummary"),
            mediumOfInstruction = o.getString("mediumOfInstruction"),
            classLevelLabel = o.optNullableString("classLevelLabel"),
            eligibilityNote = o.optNullableString("eligibilityNote"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "higher_secondary_courses", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun facilityToJson(f: FacilityEntity): String = JSONObject().apply {
        put("id", f.id)
        put("name", f.name)
        put("category", f.category)
        put("description", f.description)
        putOpt("capacityOrSpec", f.capacityOrSpec)
        put("displayOrder", f.displayOrder)
        put("isVerified", f.isVerified)
        put("isPublished", f.isPublished)
        put("isApproved", f.isApproved)
        put("approvalStatus", f.approvalStatus)
        putOpt("imageUri", f.imageUri)
        putSyncMetadata(f.remoteId, f.tenantId, f.syncState, f.serverUpdatedAtMillis, f.deletedAtMillis)
    }.toString()

    fun facilityFromJson(jsonStr: String, publishNow: Boolean): FacilityEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return FacilityEntity(
            id = id,
            name = o.getString("name"),
            category = o.getString("category"),
            description = o.getString("description"),
            capacityOrSpec = o.optNullableString("capacityOrSpec"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            imageUri = o.optNullableString("imageUri"),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "facilities", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun transportToJson(t: TransportRouteEntity): String = JSONObject().apply {
        put("id", t.id)
        put("routeNumber", t.routeNumber)
        put("routeName", t.routeName)
        put("stopsSummary", t.stopsSummary)
        putOpt("morningTiming", t.morningTiming)
        putOpt("afternoonTiming", t.afternoonTiming)
        putOpt("coordinatorContact", t.coordinatorContact)
        put("isVerified", t.isVerified)
        put("isPublished", t.isPublished)
        put("isApproved", t.isApproved)
        put("approvalStatus", t.approvalStatus)
        put("displayOrder", t.displayOrder)
        putSyncMetadata(t.remoteId, t.tenantId, t.syncState, t.serverUpdatedAtMillis, t.deletedAtMillis)
    }.toString()

    fun transportFromJson(jsonStr: String, publishNow: Boolean): TransportRouteEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return TransportRouteEntity(
            id = id,
            routeNumber = o.getString("routeNumber"),
            routeName = o.getString("routeName"),
            stopsSummary = o.getString("stopsSummary"),
            morningTiming = o.optNullableString("morningTiming"),
            afternoonTiming = o.optNullableString("afternoonTiming"),
            coordinatorContact = o.optNullableString("coordinatorContact"),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            displayOrder = o.optInt("displayOrder", 0),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "transport_routes", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun newsToJson(n: NewsItemEntity): String = JSONObject().apply {
        put("id", n.id)
        put("title", n.title)
        put("summary", n.summary)
        put("fullBody", n.fullBody)
        put("publishedDateText", n.publishedDateText)
        put("publishedAtMillis", n.publishedAtMillis)
        put("isFeatured", n.isFeatured)
        put("isVerified", n.isVerified)
        put("isPublished", n.isPublished)
        put("isApproved", n.isApproved)
        put("approvalStatus", n.approvalStatus)
        putOpt("imageUri", n.imageUri)
        put("displayOrder", n.displayOrder)
        putSyncMetadata(n.remoteId, n.tenantId, n.syncState, n.serverUpdatedAtMillis, n.deletedAtMillis)
    }.toString()

    fun newsFromJson(jsonStr: String, publishNow: Boolean): NewsItemEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return NewsItemEntity(
            id = id,
            title = o.getString("title"),
            summary = o.getString("summary"),
            fullBody = o.getString("fullBody"),
            publishedDateText = o.getString("publishedDateText"),
            publishedAtMillis = o.optLong("publishedAtMillis", System.currentTimeMillis()),
            isFeatured = o.optBoolean("isFeatured", false),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            imageUri = o.optNullableString("imageUri"),
            displayOrder = o.optInt("displayOrder", 0),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "news_items", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun announcementToJson(a: AnnouncementEntity): String = JSONObject().apply {
        put("id", a.id)
        put("title", a.title)
        put("message", a.message)
        putOpt("priorityLabel", a.priorityLabel)
        put("issuedDateText", a.issuedDateText)
        put("issuedAtMillis", a.issuedAtMillis)
        put("isVerified", a.isVerified)
        put("isPublished", a.isPublished)
        put("isApproved", a.isApproved)
        put("approvalStatus", a.approvalStatus)
        putOpt("imageUri", a.imageUri)
        put("displayOrder", a.displayOrder)
        putSyncMetadata(a.remoteId, a.tenantId, a.syncState, a.serverUpdatedAtMillis, a.deletedAtMillis)
    }.toString()

    fun announcementFromJson(jsonStr: String, publishNow: Boolean): AnnouncementEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return AnnouncementEntity(
            id = id,
            title = o.getString("title"),
            message = o.getString("message"),
            priorityLabel = o.optNullableString("priorityLabel"),
            issuedDateText = o.getString("issuedDateText"),
            issuedAtMillis = o.optLong("issuedAtMillis", System.currentTimeMillis()),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            imageUri = o.optNullableString("imageUri"),
            displayOrder = o.optInt("displayOrder", 0),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "announcements", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun socialToJson(s: SocialMediaLinkEntity): String = JSONObject().apply {
        put("id", s.id)
        put("platformName", s.platformName)
        put("handleOrLabel", s.handleOrLabel)
        put("url", s.url)
        put("displayOrder", s.displayOrder)
        put("isVerified", s.isVerified)
        put("isPublished", s.isPublished)
        put("isApproved", s.isApproved)
        put("approvalStatus", s.approvalStatus)
        putSyncMetadata(s.remoteId, s.tenantId, s.syncState, s.serverUpdatedAtMillis, s.deletedAtMillis)
    }.toString()

    fun socialFromJson(jsonStr: String, publishNow: Boolean): SocialMediaLinkEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return SocialMediaLinkEntity(
            id = id,
            platformName = o.getString("platformName"),
            handleOrLabel = o.getString("handleOrLabel"),
            url = o.getString("url"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "social_media_links", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun heroPhotoToJson(h: CampusHeroPhotoEntity): String = JSONObject().apply {
        put("id", h.id)
        put("imageUri", h.imageUri)
        putOpt("caption", h.caption)
        put("displayOrder", h.displayOrder)
        put("isVerified", h.isVerified)
        put("isPublished", h.isPublished)
        put("isApproved", h.isApproved)
        put("approvalStatus", h.approvalStatus)
        put("addedAtMillis", h.addedAtMillis)
        putSyncMetadata(h.remoteId, h.tenantId, h.syncState, h.serverUpdatedAtMillis, h.deletedAtMillis)
    }.toString()

    fun heroPhotoFromJson(jsonStr: String, publishNow: Boolean): CampusHeroPhotoEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return CampusHeroPhotoEntity(
            id = id,
            imageUri = o.getString("imageUri"),
            caption = o.optNullableString("caption"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            addedAtMillis = o.optLong("addedAtMillis", System.currentTimeMillis()),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "campus_hero_photos", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun galleryAlbumToJson(a: GalleryAlbumEntity): String = JSONObject().apply {
        put("id", a.id)
        put("title", a.title)
        put("category", a.category)
        putOpt("description", a.description)
        putOpt("eventDateText", a.eventDateText)
        putOpt("coverMediaUri", a.coverMediaUri)
        put("createdAtMillis", a.createdAtMillis)
        put("isVerified", a.isVerified)
        put("isPublished", a.isPublished)
        put("isApproved", a.isApproved)
        put("approvalStatus", a.approvalStatus)
        put("displayOrder", a.displayOrder)
        putSyncMetadata(a.remoteId, a.tenantId, a.syncState, a.serverUpdatedAtMillis, a.deletedAtMillis)
    }.toString()

    fun galleryAlbumFromJson(jsonStr: String, publishNow: Boolean): GalleryAlbumEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return GalleryAlbumEntity(
            id = id,
            title = o.getString("title"),
            category = o.optString("category", "Campus"),
            description = o.optNullableString("description"),
            eventDateText = o.optNullableString("eventDateText"),
            coverMediaUri = o.optNullableString("coverMediaUri"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            displayOrder = o.optInt("displayOrder", 0),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "gallery_albums", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun galleryMediaToJson(m: GalleryMediaEntity): String = JSONObject().apply {
        put("id", m.id)
        put("albumId", m.albumId)
        putOpt("title", m.title)
        putOpt("caption", m.caption)
        put("mediaUri", m.mediaUri)
        put("mediaType", m.mediaType)
        put("displayOrder", m.displayOrder)
        put("isVerified", m.isVerified)
        put("isPublished", m.isPublished)
        put("isApproved", m.isApproved)
        put("approvalStatus", m.approvalStatus)
        put("addedAtMillis", m.addedAtMillis)
        putSyncMetadata(m.remoteId, m.tenantId, m.syncState, m.serverUpdatedAtMillis, m.deletedAtMillis)
    }.toString()

    fun galleryMediaFromJson(jsonStr: String, publishNow: Boolean): GalleryMediaEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        return GalleryMediaEntity(
            id = id,
            albumId = o.getLong("albumId"),
            title = o.optNullableString("title"),
            caption = o.optNullableString("caption"),
            mediaUri = o.getString("mediaUri"),
            mediaType = o.optString("mediaType", "IMAGE"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            addedAtMillis = o.optLong("addedAtMillis", System.currentTimeMillis()),
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "gallery_media", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun schoolPostToJson(p: SchoolPostEntity): String = JSONObject().apply {
        put("id", p.id)
        put("postType", p.postType)
        put("category", p.category)
        put("title", p.title)
        put("description", p.description)
        putOpt("youtubeUrl", p.youtubeUrl)
        putOpt("customThumbnailUri", p.customThumbnailUri)
        put("photoUrisJson", p.photoUrisJson)
        put("status", p.status)
        put("isFeatured", p.isFeatured)
        put("createdAtMillis", p.createdAtMillis)
        if (p.publishedAtMillis != null) put("publishedAtMillis", p.publishedAtMillis) else put("publishedAtMillis", JSONObject.NULL)
        put("updatedAtMillis", p.updatedAtMillis)
        putSyncMetadata(p.remoteId, p.tenantId, p.syncState, p.serverUpdatedAtMillis, p.deletedAtMillis)
    }.toString()

    fun schoolPostFromJson(
        jsonStr: String,
        publishNow: Boolean,
        forcedStatus: String? = null
    ): SchoolPostEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        val now = System.currentTimeMillis()
        val existingPub = if (o.has("publishedAtMillis") && !o.isNull("publishedAtMillis")) {
            o.optLong("publishedAtMillis", 0L).takeIf { it > 0L }
        } else null
        val resolvedStatus = when {
            publishNow -> SchoolPostStatus.PUBLISHED
            forcedStatus == SchoolPostStatus.ARCHIVED -> SchoolPostStatus.ARCHIVED
            forcedStatus == SchoolPostStatus.UNPUBLISHED -> SchoolPostStatus.UNPUBLISHED
            else -> SchoolPostStatus.UNPUBLISHED
        }
        return SchoolPostEntity(
            id = id,
            postType = o.optString("postType", SchoolPostType.PHOTO),
            category = o.optString("category", SchoolPostCategory.GENERAL).ifBlank { SchoolPostCategory.GENERAL },
            title = o.optString("title", ""),
            description = o.optString("description", ""),
            youtubeUrl = o.optNullableString("youtubeUrl"),
            customThumbnailUri = o.optNullableString("customThumbnailUri"),
            photoUrisJson = o.optString("photoUrisJson", "[]").ifBlank { "[]" },
            status = resolvedStatus,
            isFeatured = o.optBoolean("isFeatured", false),
            createdAtMillis = o.optLong("createdAtMillis", now).takeIf { it > 0L } ?: now,
            publishedAtMillis = if (resolvedStatus == SchoolPostStatus.PUBLISHED) (existingPub ?: now) else existingPub,
            updatedAtMillis = now,
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "school_posts", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun schoolHighlightToJson(h: SchoolHighlightEntity): String = JSONObject().apply {
        put("id", h.id)
        put("title", h.title)
        put("youtubeShortsUrl", h.youtubeShortsUrl)
        put("status", h.status)
        put("createdAtMillis", h.createdAtMillis)
        put("publishedAtMillis", h.publishedAtMillis)
        put("expiresAtMillis", h.expiresAtMillis)
        putSyncMetadata(h.remoteId, h.tenantId, h.syncState, h.serverUpdatedAtMillis, h.deletedAtMillis)
    }.toString()

    fun schoolHighlightFromJson(
        jsonStr: String,
        publishNow: Boolean,
        forcedStatus: String? = null
    ): SchoolHighlightEntity {
        val o = JSONObject(jsonStr)
        val id = o.optLong("id", 0L)
        val tenantId = SyncMetadataDefaults.resolveTenantId(o.optNullableString("tenantId"))
        val now = System.currentTimeMillis()
        val createdAt = o.optLong("createdAtMillis", now).takeIf { it > 0L } ?: now
        val publishedAt = o.optLong("publishedAtMillis", now).takeIf { it > 0L } ?: now
        val expiresAt = o.optLong("expiresAtMillis", SchoolHighlightEntity.computeExpiryMillis(publishedAt))
            .takeIf { it > 0L } ?: SchoolHighlightEntity.computeExpiryMillis(publishedAt)
        val resolvedStatus = when {
            publishNow -> SchoolPostStatus.PUBLISHED
            forcedStatus == SchoolPostStatus.ARCHIVED -> SchoolPostStatus.ARCHIVED
            else -> SchoolPostStatus.UNPUBLISHED
        }
        return SchoolHighlightEntity(
            id = id,
            title = o.optString("title", ""),
            youtubeShortsUrl = o.getString("youtubeShortsUrl"),
            status = resolvedStatus,
            createdAtMillis = createdAt,
            publishedAtMillis = publishedAt,
            expiresAtMillis = expiresAt,
            remoteId = SyncMetadataDefaults.resolveRemoteId(o.optNullableString("remoteId"), "school_highlights", id, tenantId),
            tenantId = tenantId,
            syncState = SyncMetadataDefaults.resolveSyncState(o.optNullableString("syncState")),
            serverUpdatedAtMillis = o.optNullableLong("serverUpdatedAtMillis"),
            deletedAtMillis = o.optNullableLong("deletedAtMillis")
        )
    }

    fun JSONObject.putSyncMetadata(
        remoteId: String,
        tenantId: String,
        syncState: String,
        serverUpdatedAtMillis: Long?,
        deletedAtMillis: Long? = null,
        includeSoftDelete: Boolean = true
    ) {
        put("remoteId", remoteId)
        put("tenantId", tenantId)
        put("syncState", syncState)
        if (serverUpdatedAtMillis != null) {
            put("serverUpdatedAtMillis", serverUpdatedAtMillis)
        } else {
            put("serverUpdatedAtMillis", JSONObject.NULL)
        }
        if (includeSoftDelete) {
            if (deletedAtMillis != null) {
                put("deletedAtMillis", deletedAtMillis)
            } else {
                put("deletedAtMillis", JSONObject.NULL)
            }
        }
    }

    fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getLong(key) }.getOrNull()
    }

    fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return v.takeIf { it.isNotBlank() && it != "null" }
    }
}
