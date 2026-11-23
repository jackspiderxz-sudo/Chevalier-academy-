package com.example.data.v2

import com.example.data.local.entity.ManagementSection

/**
 * Exact 13 Public Academics categories and their agreed sub-sections for Version 2.
 * Shared identically by the Public Academics Page (13 cards) and Admin Management -> Academics (13 categories).
 */
data class AcademicsV2SectionSpec(
    val key: String,
    val title: String,
    val requiresOwnerAuthorizationToPublish: Boolean = false
)

enum class AcademicsV2Category(
    val cardNumber: Int,
    val routeId: String,
    val title: String,
    val permissionKey: String,
    val legacySection: ManagementSection?,
    val sections: List<AcademicsV2SectionSpec>
) {
    ACADEMICS(
        cardNumber = 1,
        routeId = "academics",
        title = "Academics",
        permissionKey = ManagementSection.ACADEMICS.key,
        legacySection = ManagementSection.ACADEMICS,
        sections = listOf(
            AcademicsV2SectionSpec("classes", "Classes"),
            AcademicsV2SectionSpec("subjects", "Subjects"),
            AcademicsV2SectionSpec("curriculum", "Curriculum"),
            AcademicsV2SectionSpec("academic_structure", "Academic Structure"),
            AcademicsV2SectionSpec("learning_resources", "Learning Resources")
        )
    ),
    HIGHER_SECONDARY_COURSES(
        cardNumber = 2,
        routeId = "higher_secondary_courses",
        title = "Higher Secondary Courses",
        permissionKey = ManagementSection.HIGHER_SECONDARY_COURSES.key,
        legacySection = ManagementSection.HIGHER_SECONDARY_COURSES,
        sections = listOf(
            AcademicsV2SectionSpec("available_groups_streams", "Available Groups / Streams"),
            AcademicsV2SectionSpec("group_wise_subjects", "Group-wise Subjects"),
            AcademicsV2SectionSpec("course_details", "Course Details"),
            AcademicsV2SectionSpec("eligibility_criteria", "Eligibility Criteria"),
            AcademicsV2SectionSpec("career_opportunities", "Career Opportunities")
        )
    ),
    ABOUT_SCHOOL(
        cardNumber = 3,
        routeId = "about_school",
        title = "About School",
        permissionKey = ManagementSection.ABOUT_SCHOOL.key,
        legacySection = ManagementSection.ABOUT_SCHOOL,
        sections = listOf(
            AcademicsV2SectionSpec("school_profile", "School Profile"),
            AcademicsV2SectionSpec("history_background", "History & Background"),
            AcademicsV2SectionSpec("vision_mission", "Vision & Mission"),
            AcademicsV2SectionSpec("motto_core_values", "Motto & Core Values"),
            AcademicsV2SectionSpec("leadership_management", "Leadership & Management"),
            AcademicsV2SectionSpec("recognitions_affiliations", "Recognitions & Affiliations")
        )
    ),
    FACILITIES(
        cardNumber = 4,
        routeId = "facilities",
        title = "Facilities",
        permissionKey = ManagementSection.FACILITIES.key,
        legacySection = ManagementSection.FACILITIES,
        sections = listOf(
            AcademicsV2SectionSpec("campus_infrastructure", "Campus & Infrastructure"),
            AcademicsV2SectionSpec("classrooms", "Classrooms"),
            AcademicsV2SectionSpec("laboratories", "Laboratories"),
            AcademicsV2SectionSpec("library", "Library"),
            AcademicsV2SectionSpec("sports_playground", "Sports & Playground"),
            AcademicsV2SectionSpec("safety_hygiene", "Safety & Hygiene"),
            AcademicsV2SectionSpec("other_facilities", "Other Facilities")
        )
    ),
    TRANSPORT(
        cardNumber = 5,
        routeId = "transport",
        title = "Transport",
        permissionKey = ManagementSection.TRANSPORT.key,
        legacySection = ManagementSection.TRANSPORT,
        sections = listOf(
            AcademicsV2SectionSpec("transport_overview", "Transport Overview"),
            AcademicsV2SectionSpec("bus_routes", "Bus Routes"),
            AcademicsV2SectionSpec("bus_stops", "Bus Stops"),
            AcademicsV2SectionSpec("timings", "Timings"),
            AcademicsV2SectionSpec("transport_guidelines", "Transport Guidelines"),
            AcademicsV2SectionSpec("contact_support", "Contact & Support")
        )
    ),
    NEWS_ANNOUNCEMENTS(
        cardNumber = 6,
        routeId = "news_announcements",
        title = "News & Announcements",
        permissionKey = ManagementSection.NEWS.key,
        legacySection = ManagementSection.NEWS,
        sections = listOf(
            AcademicsV2SectionSpec("latest_news", "Latest News"),
            AcademicsV2SectionSpec("announcements", "Announcements"),
            AcademicsV2SectionSpec("circulars_notices", "Circulars & Notices"),
            AcademicsV2SectionSpec("events_programs", "Events & Programs"),
            AcademicsV2SectionSpec("achievements", "Achievements"),
            AcademicsV2SectionSpec("important_updates", "Important Updates")
        )
    ),
    GALLERY(
        cardNumber = 7,
        routeId = "gallery",
        title = "Gallery",
        permissionKey = ManagementSection.PHOTO_GALLERY.key,
        legacySection = ManagementSection.PHOTO_GALLERY,
        sections = listOf(
            AcademicsV2SectionSpec("photo_gallery", "Photo Gallery"),
            AcademicsV2SectionSpec("events_celebrations", "Events & Celebrations"),
            AcademicsV2SectionSpec("sports_competitions", "Sports & Competitions"),
            AcademicsV2SectionSpec("academic_activities", "Academic Activities"),
            AcademicsV2SectionSpec("campus_infrastructure", "Campus & Infrastructure")
        )
    ),
    SCHOOL_TIMETABLE(
        cardNumber = 8,
        routeId = "school_timetable",
        title = "School Timetable",
        permissionKey = "V2_SCHOOL_TIMETABLE",
        legacySection = null,
        sections = listOf(
            AcademicsV2SectionSpec("class_wise_timetable", "Class-wise Timetable"),
            AcademicsV2SectionSpec("section_wise_timetable", "Section-wise Timetable"),
            AcademicsV2SectionSpec("subject_period_details", "Subject & Period Details"),
            AcademicsV2SectionSpec("teacher_assignment", "Teacher Assignment"),
            AcademicsV2SectionSpec("working_hours_period_schedule", "Working Hours & Period Schedule"),
            AcademicsV2SectionSpec("break_lunch_schedule", "Break Time & Lunch Schedule"),
            AcademicsV2SectionSpec("timetable_updates", "Timetable Updates")
        )
    ),
    EXAMINATIONS(
        cardNumber = 9,
        routeId = "examinations",
        title = "Examinations",
        permissionKey = "V2_EXAMINATIONS",
        legacySection = null,
        sections = listOf(
            AcademicsV2SectionSpec("exam_schedule", "Exam Schedule"),
            AcademicsV2SectionSpec("exam_timetable", "Exam Timetable"),
            AcademicsV2SectionSpec("exam_instructions", "Exam Instructions"),
            AcademicsV2SectionSpec(
                key = "exam_results",
                title = "Exam Results",
                requiresOwnerAuthorizationToPublish = true
            ),
            AcademicsV2SectionSpec("exam_reports", "Exam Reports"),
            AcademicsV2SectionSpec("assessment_grading", "Assessment & Grading"),
            AcademicsV2SectionSpec("important_exam_notifications", "Important Exam Notifications")
        )
    ),
    ACADEMIC_CALENDAR(
        cardNumber = 10,
        routeId = "academic_calendar",
        title = "Academic Calendar",
        permissionKey = "V2_ACADEMIC_CALENDAR",
        legacySection = null,
        sections = listOf(
            AcademicsV2SectionSpec("academic_year", "Academic Year"),
            AcademicsV2SectionSpec("school_events", "School Events"),
            AcademicsV2SectionSpec("holidays", "Holidays"),
            AcademicsV2SectionSpec("working_days", "Working Days"),
            AcademicsV2SectionSpec("examination_dates", "Examination Dates"),
            AcademicsV2SectionSpec("important_dates", "Important Dates"),
            AcademicsV2SectionSpec("calendar_updates", "Calendar Updates")
        )
    ),
    FACULTY_STAFF(
        cardNumber = 11,
        routeId = "faculty_staff",
        title = "Faculty & Staff",
        permissionKey = "V2_FACULTY_STAFF",
        legacySection = null,
        sections = listOf(
            AcademicsV2SectionSpec("teaching_staff", "Teaching Staff"),
            AcademicsV2SectionSpec("non_teaching_staff", "Non-Teaching Staff"),
            AcademicsV2SectionSpec("staff_profiles", "Staff Profiles"),
            AcademicsV2SectionSpec("department_subject_allocation", "Department & Subject Allocation"),
            AcademicsV2SectionSpec("leadership_team", "Leadership Team"),
            AcademicsV2SectionSpec("contact_information", "Contact Information")
        )
    ),
    STUDENT_ACTIVITIES(
        cardNumber = 12,
        routeId = "student_activities",
        title = "Student Activities",
        permissionKey = "V2_STUDENT_ACTIVITIES",
        legacySection = null,
        sections = listOf(
            AcademicsV2SectionSpec("cultural_activities", "Cultural Activities"),
            AcademicsV2SectionSpec("sports_activities", "Sports Activities"),
            AcademicsV2SectionSpec("competitions_awards", "Competitions & Awards"),
            AcademicsV2SectionSpec("clubs_associations", "Clubs & Associations"),
            AcademicsV2SectionSpec("student_projects_innovations", "Student Projects & Innovations"),
            AcademicsV2SectionSpec("student_participation_achievements", "Student Participation & Achievements")
        )
    ),
    ADMISSIONS(
        cardNumber = 13,
        routeId = "admissions",
        title = "Admissions",
        permissionKey = "V2_ADMISSIONS",
        legacySection = null,
        sections = listOf(
            AcademicsV2SectionSpec("admission_overview", "Admission Overview"),
            AcademicsV2SectionSpec("available_classes", "Available Classes"),
            AcademicsV2SectionSpec("eligibility_criteria", "Eligibility Criteria"),
            AcademicsV2SectionSpec("admission_process", "Admission Process"),
            AcademicsV2SectionSpec("required_documents", "Required Documents"),
            AcademicsV2SectionSpec("important_dates_notifications", "Important Dates & Notifications"),
            AcademicsV2SectionSpec("admission_enquiry_contact", "Admission Enquiry / Contact")
        )
    );

    fun findSection(sectionKey: String): AcademicsV2SectionSpec? =
        sections.firstOrNull { it.key == sectionKey }

    companion object {
        fun fromRouteId(routeId: String?): AcademicsV2Category? =
            entries.firstOrNull { it.routeId == routeId }

        fun fromPermissionKey(key: String?): AcademicsV2Category? =
            entries.firstOrNull { it.permissionKey == key }
    }
}

/**
 * Exact 8 Admin Management cards for Version 2.
 */
enum class AdminV2Card(
    val cardNumber: Int,
    val title: String,
    val subtitle: String,
    val testTag: String,
    val isOwnerCard: Boolean = false
) {
    HOME_PAGE(
        cardNumber = 1,
        title = "Home Page",
        subtitle = "Hero banner, feed posts, highlights & preview",
        testTag = "admin_v2_card_home_page"
    ),
    PARENT(
        cardNumber = 2,
        title = "Parent",
        subtitle = "Approved public parent info & circulars",
        testTag = "admin_v2_card_parent"
    ),
    ACADEMICS(
        cardNumber = 3,
        title = "Academics",
        subtitle = "Manage all 13 Academics categories",
        testTag = "admin_v2_card_academics"
    ),
    CONTACT(
        cardNumber = 4,
        title = "Contact",
        subtitle = "Address, official phone, email & map",
        testTag = "admin_v2_card_contact"
    ),
    ADMIN(
        cardNumber = 5,
        title = "Admin",
        subtitle = "Staff, roles, responsibilities & access",
        testTag = "admin_v2_card_admin"
    ),
    SCHOOL_SETTINGS(
        cardNumber = 6,
        title = "School Settings",
        subtitle = "School profile, logo, website & social links",
        testTag = "admin_v2_card_school_settings"
    ),
    PREVIEW_PUBLISH(
        cardNumber = 7,
        title = "Preview & Publish",
        subtitle = "Public previews, review & publish controls",
        testTag = "admin_v2_card_preview_publish"
    ),
    SCHOOL_OWNER_CONTROL_CENTER(
        cardNumber = 8,
        title = "School Owner – Control Center",
        subtitle = "Final approvals, access control & audit history",
        testTag = "admin_v2_card_school_owner",
        isOwnerCard = true
    )
}

enum class SchoolOwnerSection(val index: Int, val title: String, val testTag: String) {
    OWNER_PROFILE_CONTACT(1, "Owner Profile & Contact", "owner_section_profile"),
    STAFF_ACCESS_APPROVAL(2, "Staff Access Approval", "owner_section_staff_access"),
    ROLE_PERMISSION_CONTROL(3, "Role & Permission Control", "owner_section_roles"),
    APPROVAL_REQUESTS(4, "Approval Requests", "owner_section_approvals"),
    FINAL_PREVIEW_APPROVAL(5, "Final Preview & Approval", "owner_section_final_preview"),
    ACCESS_HISTORY_SECURITY(6, "Access History & Security", "owner_section_audit")
}

enum class AcademicsV2RecordStatus(val key: String, val displayLabel: String) {
    DRAFT("DRAFT", "Draft (Unpublished)"),
    PENDING_APPROVAL("PENDING_APPROVAL", "Pending Final Approval"),
    PUBLISHED("PUBLISHED", "Published"),
    UNPUBLISHED("UNPUBLISHED", "Unpublished"),
    ARCHIVED("ARCHIVED", "Archived");

    companion object {
        fun fromKey(key: String?): AcademicsV2RecordStatus =
            entries.firstOrNull { it.key == key } ?: DRAFT
    }
}

/**
 * Unified content record connecting Admin Academics Management (13 categories)
 * to Public Academics Page (13 cards).
 */
data class AcademicsV2Record(
    val id: String,
    val category: AcademicsV2Category,
    val sectionKey: String,
    val title: String,
    val description: String,
    val imageUri: String? = null,
    val attachmentUri: String? = null,
    val attachmentLabel: String? = null,
    val publishDateText: String? = null,
    val expiryDateMillis: Long? = null,
    val status: AcademicsV2RecordStatus = AcademicsV2RecordStatus.DRAFT,
    val isExamResultAuthorized: Boolean = false,
    val updatedByAccountId: Long? = null,
    val updatedByName: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val approvedByName: String? = null,
    val approvedAtMillis: Long? = null
) {
    fun isVisiblePublicly(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (status != AcademicsV2RecordStatus.PUBLISHED) return false
        if (expiryDateMillis != null && expiryDateMillis < nowMillis) return false
        if (category == AcademicsV2Category.EXAMINATIONS &&
            sectionKey == "exam_results" &&
            !isExamResultAuthorized
        ) {
            return false
        }
        return true
    }
}

/**
 * Photo-only Gallery album record for Card 07 (strictly no video gallery support).
 */
data class V2GalleryAlbumRecord(
    val id: String,
    val sectionKey: String,
    val title: String,
    val description: String = "",
    val dateText: String = "",
    val status: AcademicsV2RecordStatus = AcademicsV2RecordStatus.DRAFT,
    val updatedByName: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val isVisiblePublicly: Boolean
        get() = status == AcademicsV2RecordStatus.PUBLISHED
}

/**
 * Photo-only Gallery item supporting reuse across multiple albums via [albumIds].
 */
data class V2GalleryPhotoRecord(
    val id: String,
    val title: String,
    val description: String = "",
    val dateText: String = "",
    val imageUri: String,
    val albumIds: List<String>,
    val status: AcademicsV2RecordStatus = AcademicsV2RecordStatus.DRAFT,
    val updatedByName: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val isVisiblePublicly: Boolean
        get() = status == AcademicsV2RecordStatus.PUBLISHED && imageUri.isNotBlank()
}

/**
 * Admin-managed faculty/staff profile record with explicit school approval & consent gates.
 * Private personal information is intentionally omitted from the model so it cannot leak publicly.
 */
data class V2FacultyProfileRecord(
    val id: String,
    val sectionKey: String,
    val teacherName: String,
    val approvedProfilePhotoUri: String? = null,
    val designation: String,
    val department: String,
    val teachingSubjects: String,
    val educationalQualifications: String,
    val teachingExperience: String,
    val professionalIntroduction: String,
    val isSchoolApproved: Boolean = false,
    val hasStaffConsent: Boolean = false,
    val status: AcademicsV2RecordStatus = AcademicsV2RecordStatus.DRAFT,
    val updatedByName: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toPublicProfileOrNull(): PublicFacultyProfile? {
        if (status != AcademicsV2RecordStatus.PUBLISHED || !isSchoolApproved || !hasStaffConsent) {
            return null
        }
        return PublicFacultyProfile(
            id = id,
            sectionKey = sectionKey,
            teacherName = teacherName,
            approvedProfilePhotoUri = approvedProfilePhotoUri,
            designation = designation,
            department = department,
            teachingSubjects = teachingSubjects,
            educationalQualifications = educationalQualifications,
            teachingExperience = teachingExperience,
            professionalIntroduction = professionalIntroduction
        )
    }
}

/**
 * Strict public projection for Card 11 (Faculty & Staff) containing ONLY the 8 approved fields.
 * Private mobile numbers, personal emails, home addresses, dates of birth, ID documents,
 * and family details are structurally absent from this type.
 */
data class PublicFacultyProfile(
    val id: String,
    val sectionKey: String,
    val teacherName: String,
    val approvedProfilePhotoUri: String?,
    val designation: String,
    val department: String,
    val teachingSubjects: String,
    val educationalQualifications: String,
    val teachingExperience: String,
    val professionalIntroduction: String
)

/**
 * School Owner private profile & contact details.
 * Accessible ONLY inside School Owner – Control Center; never surfaced on public screens.
 */
data class V2OwnerPrivateProfile(
    val ownerName: String = "",
    val designation: String = "School Owner / Correspondent",
    val privateContactPhone: String = "",
    val privateContactEmail: String = "",
    val governanceNote: String = "",
    val updatedAtMillis: Long = 0L
)

enum class V2StaffAccessRequestStatus(val key: String) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    REVOKED("REVOKED");

    companion object {
        fun fromKey(key: String?): V2StaffAccessRequestStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

data class V2StaffAccessRequest(
    val id: String,
    val staffAccountId: Long,
    val staffUsername: String,
    val staffDisplayName: String,
    val requestedRole: String,
    val requestedCategoryKeys: List<String>,
    val reason: String,
    val status: V2StaffAccessRequestStatus = V2StaffAccessRequestStatus.PENDING,
    val reviewedByName: String? = null,
    val reviewedAtMillis: Long? = null,
    val reviewNote: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class V2AccessAuditEvent(
    val id: String,
    val action: String,
    val actorName: String,
    val actorRole: String,
    val targetRef: String,
    val details: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class AcademicsV2Snapshot(
    val records: List<AcademicsV2Record> = emptyList(),
    val galleryAlbums: List<V2GalleryAlbumRecord> = emptyList(),
    val galleryPhotos: List<V2GalleryPhotoRecord> = emptyList(),
    val facultyProfiles: List<V2FacultyProfileRecord> = emptyList(),
    val ownerPrivateProfile: V2OwnerPrivateProfile = V2OwnerPrivateProfile(),
    val staffAccessRequests: List<V2StaffAccessRequest> = emptyList(),
    val accessAuditEvents: List<V2AccessAuditEvent> = emptyList()
)
