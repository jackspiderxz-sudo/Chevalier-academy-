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
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
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
    }.toString()

    fun schoolProfileFromJson(jsonStr: String): SchoolProfileEntity {
        val o = JSONObject(jsonStr)
        return SchoolProfileEntity(
            id = o.optInt("id", 1),
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
            aboutImageUri = o.optNullableString("aboutImageUri")
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
    }.toString()

    fun academicInfoFromJson(jsonStr: String, publishNow: Boolean): AcademicInfoEntity {
        val o = JSONObject(jsonStr)
        return AcademicInfoEntity(
            id = o.optLong("id", 0L),
            title = o.getString("title"),
            category = o.getString("category"),
            description = o.getString("description"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            updatedAtMillis = System.currentTimeMillis()
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
    }.toString()

    fun courseFromJson(jsonStr: String, publishNow: Boolean): HigherSecondaryCourseEntity {
        val o = JSONObject(jsonStr)
        return HigherSecondaryCourseEntity(
            id = o.optLong("id", 0L),
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
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING)
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
    }.toString()

    fun facilityFromJson(jsonStr: String, publishNow: Boolean): FacilityEntity {
        val o = JSONObject(jsonStr)
        return FacilityEntity(
            id = o.optLong("id", 0L),
            name = o.getString("name"),
            category = o.getString("category"),
            description = o.getString("description"),
            capacityOrSpec = o.optNullableString("capacityOrSpec"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            imageUri = o.optNullableString("imageUri")
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
    }.toString()

    fun transportFromJson(jsonStr: String, publishNow: Boolean): TransportRouteEntity {
        val o = JSONObject(jsonStr)
        return TransportRouteEntity(
            id = o.optLong("id", 0L),
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
            displayOrder = o.optInt("displayOrder", 0)
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
    }.toString()

    fun newsFromJson(jsonStr: String, publishNow: Boolean): NewsItemEntity {
        val o = JSONObject(jsonStr)
        return NewsItemEntity(
            id = o.optLong("id", 0L),
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
            displayOrder = o.optInt("displayOrder", 0)
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
    }.toString()

    fun announcementFromJson(jsonStr: String, publishNow: Boolean): AnnouncementEntity {
        val o = JSONObject(jsonStr)
        return AnnouncementEntity(
            id = o.optLong("id", 0L),
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
            displayOrder = o.optInt("displayOrder", 0)
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
    }.toString()

    fun socialFromJson(jsonStr: String, publishNow: Boolean): SocialMediaLinkEntity {
        val o = JSONObject(jsonStr)
        return SocialMediaLinkEntity(
            id = o.optLong("id", 0L),
            platformName = o.getString("platformName"),
            handleOrLabel = o.getString("handleOrLabel"),
            url = o.getString("url"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING)
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
    }.toString()

    fun heroPhotoFromJson(jsonStr: String, publishNow: Boolean): CampusHeroPhotoEntity {
        val o = JSONObject(jsonStr)
        return CampusHeroPhotoEntity(
            id = o.optLong("id", 0L),
            imageUri = o.getString("imageUri"),
            caption = o.optNullableString("caption"),
            displayOrder = o.optInt("displayOrder", 0),
            isVerified = publishNow || o.optBoolean("isVerified", false),
            isPublished = publishNow || o.optBoolean("isPublished", false),
            isApproved = publishNow || o.optBoolean("isApproved", false),
            approvalStatus = if (publishNow) ApprovalStatus.APPROVED else o.optString("approvalStatus", ApprovalStatus.PENDING),
            addedAtMillis = o.optLong("addedAtMillis", System.currentTimeMillis())
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
    }.toString()

    fun galleryAlbumFromJson(jsonStr: String, publishNow: Boolean): GalleryAlbumEntity {
        val o = JSONObject(jsonStr)
        return GalleryAlbumEntity(
            id = o.optLong("id", 0L),
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
            displayOrder = o.optInt("displayOrder", 0)
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
    }.toString()

    fun galleryMediaFromJson(jsonStr: String, publishNow: Boolean): GalleryMediaEntity {
        val o = JSONObject(jsonStr)
        return GalleryMediaEntity(
            id = o.optLong("id", 0L),
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
            addedAtMillis = o.optLong("addedAtMillis", System.currentTimeMillis())
        )
    }

    fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return v.takeIf { it.isNotBlank() && it != "null" }
    }
}
