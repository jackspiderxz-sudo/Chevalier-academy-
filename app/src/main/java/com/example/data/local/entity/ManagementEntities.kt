package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object ManagementRole {
    const val MASTER_ADMIN = "MASTER_ADMIN"
    const val SUPER_OWNER = "SUPER_OWNER"
    const val STAFF = "STAFF"
}

enum class ManagementSection(val key: String, val supportsPublish: Boolean = true) {
    SCHOOL_PROFILE("SCHOOL_PROFILE", supportsPublish = false),
    LOGO_BANNER("LOGO_BANNER", supportsPublish = true),
    ABOUT_SCHOOL("ABOUT_SCHOOL", supportsPublish = false),
    ACADEMICS("ACADEMICS", supportsPublish = true),
    HIGHER_SECONDARY_COURSES("HIGHER_SECONDARY_COURSES", supportsPublish = true),
    FACILITIES("FACILITIES", supportsPublish = true),
    TRANSPORT("TRANSPORT", supportsPublish = true),
    NEWS("NEWS", supportsPublish = true),
    ANNOUNCEMENTS("ANNOUNCEMENTS", supportsPublish = true),
    PHOTO_GALLERY("PHOTO_GALLERY", supportsPublish = true),
    CONTACT_DETAILS("CONTACT_DETAILS", supportsPublish = false),
    SOCIAL_MEDIA("SOCIAL_MEDIA", supportsPublish = true),
    SCHOOL_POSTS("SCHOOL_POSTS", supportsPublish = true),
    SCHOOL_HIGHLIGHTS("SCHOOL_HIGHLIGHTS", supportsPublish = true),
    MEDIA_STUDIO("MEDIA_STUDIO", supportsPublish = true),
    STAFF_MANAGEMENT("STAFF_MANAGEMENT", supportsPublish = false),
    NOTIFICATIONS("NOTIFICATIONS", supportsPublish = false),
    ACTIVITY_LOG("ACTIVITY_LOG", supportsPublish = false),
    BACKUP_RESTORE("BACKUP_RESTORE", supportsPublish = false),
    APP_SETTINGS("APP_SETTINGS", supportsPublish = false);

    companion object {
        fun fromKey(key: String): ManagementSection? = entries.firstOrNull { it.key == key }
    }
}

enum class PermissionAction {
    VIEW,
    CREATE,
    EDIT,
    DELETE,
    PUBLISH
}

object ContentWorkflowStatus {
    const val DRAFT = "DRAFT"
    const val DRAFT_DISCARDED = "DRAFT_DISCARDED"
    const val PENDING_APPROVAL = "PENDING_APPROVAL"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val PUBLISHED = "PUBLISHED"
    const val UNPUBLISHED = "UNPUBLISHED"
}

@Entity(
    tableName = "management_accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class ManagementAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val role: String,
    val pinSaltBase64: String,
    val pinVerifierBase64: String,
    val recoverySaltBase64: String? = null,
    val recoveryVerifierBase64: String? = null,
    val isActive: Boolean = true,
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "staff_permissions",
    foreignKeys = [
        ForeignKey(
            entity = ManagementAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "sectionKey"], unique = true)
    ]
)
data class StaffPermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val sectionKey: String,
    val canView: Boolean = false,
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canPublish: Boolean = false
)

data class AccountWithPermissions(
    @Embedded val account: ManagementAccountEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "accountId"
    )
    val permissions: List<StaffPermissionEntity>
)

@Entity(
    tableName = "content_approvals",
    indices = [
        Index(value = ["sectionKey"]),
        Index(value = ["status"]),
        Index(value = ["submittedByAccountId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class ContentApprovalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionKey: String,
    val targetRecordId: Long = 0L,
    val recordTitle: String,
    val previousSnapshotJson: String? = null,
    val proposedPayloadJson: String,
    val changeSummary: String,
    val submittedByAccountId: Long,
    val submittedByName: String,
    val submittedAtMillis: Long = System.currentTimeMillis(),
    val status: String = ContentWorkflowStatus.PENDING_APPROVAL,
    val reviewedByAccountId: Long? = null,
    val reviewedByName: String? = null,
    val reviewedAtMillis: Long? = null,
    val rejectionReason: String? = null,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Entity(
    tableName = "version_history",
    indices = [
        Index(value = ["sectionKey", "recordId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class VersionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionKey: String,
    val recordId: Long,
    val versionNumber: Int,
    val recordTitle: String,
    val previousContentJson: String? = null,
    val contentSnapshotJson: String,
    val changedByAccountId: Long,
    val changedByName: String,
    val changedAtMillis: Long = System.currentTimeMillis(),
    val changeSummary: String,
    val approvalStatus: String,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null
)

@Entity(
    tableName = "media_studio_items",
    indices = [
        Index(value = ["mediaKind"]),
        Index(value = ["approvalStatus"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class MediaStudioItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaKind: String, // "IMAGE" or "YOUTUBE"
    val title: String,
    val caption: String? = null,
    val categoryOrAlbum: String = "Campus",
    val originalUri: String,
    val optimizedUri: String? = null,
    val mimeType: String,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val fileSizeBytes: Long = 0L,
    val optimizedSizeBytes: Long = 0L,
    val assignedSectionsCsv: String = "",
    val isFeatured: Boolean = false,
    val displayOrder: Int = 0,
    val approvalStatus: String = ContentWorkflowStatus.PUBLISHED,
    val createdByName: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Entity(
    tableName = "activity_logs",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["actorAccountId"]),
        Index(value = ["sectionKey"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val actorAccountId: Long? = null,
    val actorName: String,
    val actorRole: String,
    val sectionKey: String,
    val recordRef: String,
    val details: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null
)

@Entity(
    tableName = "local_notifications",
    indices = [Index(value = ["isRead"]), Index(value = ["createdAtMillis"])]
)
data class LocalNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val title: String,
    val message: String,
    val sectionKey: String,
    val isRead: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
