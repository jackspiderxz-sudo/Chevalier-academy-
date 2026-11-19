package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object ApprovalStatus {
    const val APPROVED = "APPROVED"
    const val PENDING = "PENDING"
    const val DRAFT = "DRAFT"
    const val REJECTED = "REJECTED"
}

@Entity(tableName = "school_profile")
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
    val aboutImageUri: String? = null
)

@Entity(tableName = "academic_info")
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
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "higher_secondary_courses")
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
    val approvalStatus: String = ApprovalStatus.PENDING
)

@Entity(tableName = "facilities")
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
    val imageUri: String? = null
)

@Entity(tableName = "transport_routes")
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
    val displayOrder: Int = 0
)

@Entity(tableName = "news_items")
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
    val displayOrder: Int = 0
)

@Entity(tableName = "announcements")
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
    val displayOrder: Int = 0
)
