package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object ApprovalStatus {
    const val APPROVED = "APPROVED"
    const val PENDING = "PENDING"
    const val DRAFT = "DRAFT"
    const val REJECTED = "REJECTED"
}

@Entity(
    tableName = "school_profile",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class SchoolProfileEntity(
    @PrimaryKey val id: Int = 1,
    val schoolName: String,
    val shortName: String,
    val motto: String,
    val addressLine: String,
    val emblemMark: String,
    val poweredBy: String,
    val welcomeIntroduction: String? = null,
    val mediumAndGradesNote: String? = null,
    val mapCoordinatesOrQuery: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val officeHours: String? = null,
    val principalName: String? = null,
    val history: String? = null,
    val vision: String? = null,
    val mission: String? = null,
    val affiliationNumber: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val profileImageUri: String? = null,
    val aboutImageUri: String? = null,
    val remoteId: String = SyncMetadataDefaults.deterministicRemoteId("school_profile", id.toLong()),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null
)

@Entity(
    tableName = "academic_info",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class AcademicInfoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val description: String,
    val displayOrder: Int = 0,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Entity(
    tableName = "higher_secondary_courses",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class HigherSecondaryCourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val streamTitle: String,
    val groupCode: String,
    val subjectsSummary: String,
    val mediumOfInstruction: String,
    val classLevelLabel: String? = null,
    val eligibilityNote: String? = null,
    val displayOrder: Int = 0,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Entity(
    tableName = "facilities",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class FacilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val description: String,
    val capacityOrSpec: String? = null,
    val displayOrder: Int = 0,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val imageUri: String? = null,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Entity(
    tableName = "transport_routes",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class TransportRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeNumber: String,
    val routeName: String,
    val stopsSummary: String,
    val morningTiming: String? = null,
    val afternoonTiming: String? = null,
    val coordinatorContact: String? = null,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val displayOrder: Int = 0,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Entity(
    tableName = "news_items",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class NewsItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String,
    val fullBody: String,
    val publishedDateText: String,
    val publishedAtMillis: Long = System.currentTimeMillis(),
    val isFeatured: Boolean = false,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val imageUri: String? = null,
    val displayOrder: Int = 0,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Entity(
    tableName = "announcements",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val priorityLabel: String? = null,
    val issuedDateText: String,
    val issuedAtMillis: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val imageUri: String? = null,
    val displayOrder: Int = 0,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)
