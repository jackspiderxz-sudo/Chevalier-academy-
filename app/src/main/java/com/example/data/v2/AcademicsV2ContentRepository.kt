package com.example.data.v2

import android.content.Context
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.PermissionAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Unified Version 2 content repository connecting Admin Management -> Academics (13 categories)
 * and the Public Academics Page (13 cards) through one consistent data structure,
 * role-based access control, publication workflow, and atomic JSON persistence.
 *
 * Strictly zero fake, demo, or placeholder school content is seeded.
 */
class AcademicsV2ContentRepository(
    context: Context,
    private val storageFile: File = File(File(context.filesDir, "chevalier_v2"), "academics_v2_store.json")
) {
    private val mutex = Mutex()
    private val _snapshot = MutableStateFlow(loadSnapshotSafely())
    val snapshot: StateFlow<AcademicsV2Snapshot> = _snapshot.asStateFlow()

    fun observePublicRecords(
        category: AcademicsV2Category,
        nowMillisProvider: () -> Long = { System.currentTimeMillis() }
    ): Flow<List<AcademicsV2Record>> = snapshot.map { current ->
        val now = nowMillisProvider()
        current.records
            .filter { it.category == category && it.isVisiblePublicly(now) }
            .sortedByDescending { it.updatedAtMillis }
    }

    fun observePublicGalleryAlbums(): Flow<List<V2GalleryAlbumRecord>> = snapshot.map { current ->
        current.galleryAlbums
            .filter { it.isVisiblePublicly }
            .sortedByDescending { it.updatedAtMillis }
    }

    fun observePublicGalleryPhotosForAlbum(albumId: String): Flow<List<V2GalleryPhotoRecord>> = snapshot.map { current ->
        val albumPublished = current.galleryAlbums.any { it.id == albumId && it.isVisiblePublicly }
        if (!albumPublished) {
            emptyList()
        } else {
            current.galleryPhotos
                .filter { it.isVisiblePublicly && albumId in it.albumIds }
                .sortedByDescending { it.updatedAtMillis }
        }
    }

    fun observePublicFacultyProfiles(): Flow<List<PublicFacultyProfile>> = snapshot.map { current ->
        current.facultyProfiles
            .mapNotNull { it.toPublicProfileOrNull() }
            .sortedBy { it.teacherName.lowercase() }
    }

    fun observePublicFacultyProfileById(profileId: String): Flow<PublicFacultyProfile?> = snapshot.map { current ->
        current.facultyProfiles
            .firstOrNull { it.id == profileId }
            ?.toPublicProfileOrNull()
    }

    fun getPublicFacultyProfileById(profileId: String): PublicFacultyProfile? =
        _snapshot.value.facultyProfiles
            .firstOrNull { it.id == profileId }
            ?.toPublicProfileOrNull()

    /**
     * Create or edit a unified Academics V2 record.
     * Creating or editing NEVER automatically publishes content:
     * - New items default to DRAFT (or PENDING_APPROVAL if submitted for approval).
     * - Editing existing content keeps it unpublished (DRAFT or PENDING_APPROVAL) so edits
     *   cannot unintentionally go live without explicit publication / approval.
     */
    suspend fun saveRecord(
        actor: AccountWithPermissions,
        existingId: String?,
        category: AcademicsV2Category,
        sectionKey: String,
        title: String,
        description: String,
        imageUri: String? = null,
        attachmentUri: String? = null,
        attachmentLabel: String? = null,
        publishDateText: String? = null,
        expiryDateMillis: Long? = null,
        submitForApproval: Boolean = false
    ): Result<AcademicsV2Record> = runMutation {
        val action = if (existingId == null) PermissionAction.CREATE else PermissionAction.EDIT
        AcademicsV2AccessControl.requireCategoryPermission(actor, category, action)

        val section = category.findSection(sectionKey)
            ?: throw IllegalArgumentException("Invalid section '$sectionKey' for category '${category.title}'.")
        val cleanTitle = title.trim()
        val cleanDescription = description.trim()
        require(cleanTitle.isNotEmpty()) { "Title is required." }
        require(cleanDescription.isNotEmpty()) { "Description is required." }

        val cleanImage = imageUri?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanImage != null) {
            validatePhotoOnlyUri(cleanImage)
        }

        val current = _snapshot.value
        val existing = existingId?.let { id -> current.records.firstOrNull { it.id == id } }
        val now = System.currentTimeMillis()
        val targetStatus = if (submitForApproval) {
            AcademicsV2RecordStatus.PENDING_APPROVAL
        } else {
            AcademicsV2RecordStatus.DRAFT
        }

        val saved = AcademicsV2Record(
            id = existing?.id ?: UUID.randomUUID().toString(),
            category = category,
            sectionKey = section.key,
            title = cleanTitle,
            description = cleanDescription,
            imageUri = cleanImage,
            attachmentUri = attachmentUri?.trim()?.takeIf { it.isNotEmpty() },
            attachmentLabel = attachmentLabel?.trim()?.takeIf { it.isNotEmpty() },
            publishDateText = publishDateText?.trim()?.takeIf { it.isNotEmpty() },
            expiryDateMillis = expiryDateMillis,
            status = targetStatus,
            isExamResultAuthorized = false,
            updatedByAccountId = actor.account.id,
            updatedByName = actor.account.displayName,
            updatedAtMillis = now,
            approvedByName = null,
            approvedAtMillis = null
        )

        val updatedRecords = if (existing == null) {
            listOf(saved) + current.records
        } else {
            current.records.map { if (it.id == existing.id) saved else it }
        }

        val auditAction = if (existing == null) "CREATE_V2_RECORD" else "EDIT_V2_RECORD"
        val next = current.copy(
            records = updatedRecords,
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = auditAction,
                actor = actor,
                targetRef = "${category.routeId}/${section.key}/${saved.id}",
                details = "Saved '${saved.title}' as ${saved.status.displayLabel}"
            )
        )
        persistSnapshotLocked(next)
        saved
    }

    suspend fun submitRecordForOwnerApproval(
        actor: AccountWithPermissions,
        recordId: String
    ): Result<AcademicsV2Record> = runMutation {
        val current = _snapshot.value
        val existing = current.records.firstOrNull { it.id == recordId }
            ?: throw IllegalArgumentException("Record not found.")
        AcademicsV2AccessControl.requireCategoryPermission(actor, existing.category, PermissionAction.EDIT)

        val updated = existing.copy(
            status = AcademicsV2RecordStatus.PENDING_APPROVAL,
            updatedByAccountId = actor.account.id,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val next = current.copy(
            records = current.records.map { if (it.id == recordId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "SUBMIT_FOR_APPROVAL",
                actor = actor,
                targetRef = "${existing.category.routeId}/${existing.sectionKey}/${existing.id}",
                details = "Submitted '${existing.title}' for School Owner final approval"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    /**
     * Standard Admin/Staff publication for categories they hold PUBLISH permission on.
     * Exam Results strictly require School Owner authorization and are blocked here for non-owners.
     */
    suspend fun publishRecord(
        actor: AccountWithPermissions,
        recordId: String,
        authorizeExamResult: Boolean = false
    ): Result<AcademicsV2Record> = runMutation {
        val current = _snapshot.value
        val existing = current.records.firstOrNull { it.id == recordId }
            ?: throw IllegalArgumentException("Record not found.")

        val section = existing.category.findSection(existing.sectionKey)
        if (section?.requiresOwnerAuthorizationToPublish == true) {
            AcademicsV2AccessControl.requireSchoolOwner(actor, "Exam Results publication")
            if (!authorizeExamResult) {
                throw SecurityException("Exam Results require explicit School Owner authorization before publishing.")
            }
        } else {
            AcademicsV2AccessControl.requireCategoryPermission(actor, existing.category, PermissionAction.PUBLISH)
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = AcademicsV2RecordStatus.PUBLISHED,
            isExamResultAuthorized = if (section?.requiresOwnerAuthorizationToPublish == true) true else existing.isExamResultAuthorized,
            updatedByAccountId = actor.account.id,
            updatedByName = actor.account.displayName,
            updatedAtMillis = now,
            approvedByName = if (AcademicsV2AccessControl.isSchoolOwner(actor)) actor.account.displayName else existing.approvedByName,
            approvedAtMillis = if (AcademicsV2AccessControl.isSchoolOwner(actor)) now else existing.approvedAtMillis
        )

        val next = current.copy(
            records = current.records.map { if (it.id == recordId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "PUBLISH_V2_RECORD",
                actor = actor,
                targetRef = "${existing.category.routeId}/${existing.sectionKey}/${existing.id}",
                details = "Published '${existing.title}'"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun ownerApproveAndPublishRecord(
        actor: AccountWithPermissions,
        recordId: String,
        authorizeExamResult: Boolean = true
    ): Result<AcademicsV2Record> = runMutation {
        AcademicsV2AccessControl.requireSchoolOwner(actor, "Final Preview & Approval")
        val current = _snapshot.value
        val existing = current.records.firstOrNull { it.id == recordId }
            ?: throw IllegalArgumentException("Record not found.")

        val isExamResult = existing.category == AcademicsV2Category.EXAMINATIONS &&
            existing.sectionKey == "exam_results"
        if (isExamResult && !authorizeExamResult) {
            throw SecurityException("Exam Results must be explicitly authorized by the School Owner.")
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = AcademicsV2RecordStatus.PUBLISHED,
            isExamResultAuthorized = if (isExamResult) true else existing.isExamResultAuthorized,
            updatedByAccountId = actor.account.id,
            updatedByName = actor.account.displayName,
            updatedAtMillis = now,
            approvedByName = actor.account.displayName,
            approvedAtMillis = now
        )
        val next = current.copy(
            records = current.records.map { if (it.id == recordId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "OWNER_APPROVE_PUBLISH",
                actor = actor,
                targetRef = "${existing.category.routeId}/${existing.sectionKey}/${existing.id}",
                details = "School Owner approved and published '${existing.title}'"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun updateRecordStatus(
        actor: AccountWithPermissions,
        recordId: String,
        targetStatus: AcademicsV2RecordStatus
    ): Result<AcademicsV2Record> {
        if (targetStatus == AcademicsV2RecordStatus.PUBLISHED) {
            return publishRecord(actor, recordId, authorizeExamResult = false)
        }
        return runMutation {
            val current = _snapshot.value
            val existing = current.records.firstOrNull { it.id == recordId }
                ?: throw IllegalArgumentException("Record not found.")
            val requiredAction = when (targetStatus) {
                AcademicsV2RecordStatus.UNPUBLISHED, AcademicsV2RecordStatus.ARCHIVED -> PermissionAction.PUBLISH
                else -> PermissionAction.EDIT
            }
            if (!AcademicsV2AccessControl.hasCategoryPermission(actor, existing.category, requiredAction) &&
                !AcademicsV2AccessControl.hasCategoryPermission(actor, existing.category, PermissionAction.EDIT)
            ) {
                throw SecurityException("Permission denied to change status for ${existing.category.title}.")
            }

            val updated = existing.copy(
                status = targetStatus,
                isExamResultAuthorized = if (targetStatus == AcademicsV2RecordStatus.DRAFT || targetStatus == AcademicsV2RecordStatus.UNPUBLISHED) {
                    false
                } else {
                    existing.isExamResultAuthorized
                },
                updatedByAccountId = actor.account.id,
                updatedByName = actor.account.displayName,
                updatedAtMillis = System.currentTimeMillis()
            )
            val next = current.copy(
                records = current.records.map { if (it.id == recordId) updated else it },
                accessAuditEvents = appendAudit(
                    current.accessAuditEvents,
                    action = "UPDATE_V2_RECORD_STATUS",
                    actor = actor,
                    targetRef = "${existing.category.routeId}/${existing.sectionKey}/${existing.id}",
                    details = "Changed '${existing.title}' status to ${targetStatus.displayLabel}"
                )
            )
            persistSnapshotLocked(next)
            updated
        }
    }

    suspend fun deleteRecord(
        actor: AccountWithPermissions,
        recordId: String
    ): Result<Unit> = runMutation {
        val current = _snapshot.value
        val existing = current.records.firstOrNull { it.id == recordId }
            ?: throw IllegalArgumentException("Record not found.")
        AcademicsV2AccessControl.requireCategoryPermission(actor, existing.category, PermissionAction.DELETE)

        val next = current.copy(
            records = current.records.filterNot { it.id == recordId },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "DELETE_V2_RECORD",
                actor = actor,
                targetRef = "${existing.category.routeId}/${existing.sectionKey}/${existing.id}",
                details = "Deleted '${existing.title}'"
            )
        )
        persistSnapshotLocked(next)
    }

    // -------------------------------------------------------------------------
    // Card 07: Photo-Only Gallery Management (multi-album reuse, NO video!)
    // -------------------------------------------------------------------------

    suspend fun saveGalleryAlbum(
        actor: AccountWithPermissions,
        existingId: String?,
        sectionKey: String,
        title: String,
        description: String,
        dateText: String
    ): Result<V2GalleryAlbumRecord> = runMutation {
        val action = if (existingId == null) PermissionAction.CREATE else PermissionAction.EDIT
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.GALLERY, action)
        require(AcademicsV2Category.GALLERY.findSection(sectionKey) != null) {
            "Invalid Gallery section: $sectionKey"
        }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Album title is required." }

        val current = _snapshot.value
        val existing = existingId?.let { id -> current.galleryAlbums.firstOrNull { it.id == id } }
        val saved = V2GalleryAlbumRecord(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sectionKey = sectionKey,
            title = cleanTitle,
            description = description.trim(),
            dateText = dateText.trim(),
            status = AcademicsV2RecordStatus.DRAFT,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val updatedAlbums = if (existing == null) {
            listOf(saved) + current.galleryAlbums
        } else {
            current.galleryAlbums.map { if (it.id == existing.id) saved else it }
        }
        val next = current.copy(
            galleryAlbums = updatedAlbums,
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = if (existing == null) "CREATE_V2_ALBUM" else "EDIT_V2_ALBUM",
                actor = actor,
                targetRef = "gallery/album/${saved.id}",
                details = "Saved photo album '${saved.title}' as Draft (Unpublished)"
            )
        )
        persistSnapshotLocked(next)
        saved
    }

    suspend fun updateGalleryAlbumStatus(
        actor: AccountWithPermissions,
        albumId: String,
        targetStatus: AcademicsV2RecordStatus
    ): Result<V2GalleryAlbumRecord> = runMutation {
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.GALLERY, PermissionAction.PUBLISH)
        val current = _snapshot.value
        val existing = current.galleryAlbums.firstOrNull { it.id == albumId }
            ?: throw IllegalArgumentException("Album not found.")
        val updated = existing.copy(
            status = targetStatus,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val next = current.copy(
            galleryAlbums = current.galleryAlbums.map { if (it.id == albumId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "UPDATE_V2_ALBUM_STATUS",
                actor = actor,
                targetRef = "gallery/album/$albumId",
                details = "Set album '${existing.title}' status to ${targetStatus.displayLabel}"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun deleteGalleryAlbum(
        actor: AccountWithPermissions,
        albumId: String
    ): Result<Unit> = runMutation {
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.GALLERY, PermissionAction.DELETE)
        val current = _snapshot.value
        val existing = current.galleryAlbums.firstOrNull { it.id == albumId }
            ?: throw IllegalArgumentException("Album not found.")
        val cleanedPhotos = current.galleryPhotos.map { photo ->
            photo.copy(albumIds = photo.albumIds.filterNot { it == albumId })
        }
        val next = current.copy(
            galleryAlbums = current.galleryAlbums.filterNot { it.id == albumId },
            galleryPhotos = cleanedPhotos,
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "DELETE_V2_ALBUM",
                actor = actor,
                targetRef = "gallery/album/$albumId",
                details = "Deleted photo album '${existing.title}'"
            )
        )
        persistSnapshotLocked(next)
    }

    /**
     * Save or edit a Gallery Photo. Strictly rejects video URIs/links.
     * Supports assigning a single photo across multiple albums via [albumIds].
     */
    suspend fun saveGalleryPhoto(
        actor: AccountWithPermissions,
        existingId: String?,
        title: String,
        description: String,
        dateText: String,
        imageUri: String,
        albumIds: List<String>
    ): Result<V2GalleryPhotoRecord> = runMutation {
        val action = if (existingId == null) PermissionAction.CREATE else PermissionAction.EDIT
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.GALLERY, action)

        val cleanTitle = title.trim()
        val cleanImageUri = imageUri.trim()
        require(cleanTitle.isNotEmpty()) { "Photo title is required." }
        require(cleanImageUri.isNotEmpty()) { "Photo image is required." }
        validatePhotoOnlyUri(cleanImageUri)

        val current = _snapshot.value
        val validAlbumIds = albumIds.distinct().filter { id -> current.galleryAlbums.any { it.id == id } }
        require(validAlbumIds.isNotEmpty()) { "Select at least one photo album." }

        val existing = existingId?.let { id -> current.galleryPhotos.firstOrNull { it.id == id } }
        val saved = V2GalleryPhotoRecord(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = cleanTitle,
            description = description.trim(),
            dateText = dateText.trim(),
            imageUri = cleanImageUri,
            albumIds = validAlbumIds,
            status = AcademicsV2RecordStatus.DRAFT,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val updatedPhotos = if (existing == null) {
            listOf(saved) + current.galleryPhotos
        } else {
            current.galleryPhotos.map { if (it.id == existing.id) saved else it }
        }
        val next = current.copy(
            galleryPhotos = updatedPhotos,
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = if (existing == null) "UPLOAD_V2_PHOTO" else "EDIT_V2_PHOTO",
                actor = actor,
                targetRef = "gallery/photo/${saved.id}",
                details = "Saved photo '${saved.title}' assigned to ${validAlbumIds.size} album(s) as Draft"
            )
        )
        persistSnapshotLocked(next)
        saved
    }

    suspend fun assignGalleryPhotoToAlbums(
        actor: AccountWithPermissions,
        photoId: String,
        albumIds: List<String>
    ): Result<V2GalleryPhotoRecord> = runMutation {
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.GALLERY, PermissionAction.EDIT)
        val current = _snapshot.value
        val existing = current.galleryPhotos.firstOrNull { it.id == photoId }
            ?: throw IllegalArgumentException("Photo not found.")
        val validAlbumIds = albumIds.distinct().filter { id -> current.galleryAlbums.any { it.id == id } }
        require(validAlbumIds.isNotEmpty()) { "Select at least one photo album." }

        val updated = existing.copy(
            albumIds = validAlbumIds,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val next = current.copy(
            galleryPhotos = current.galleryPhotos.map { if (it.id == photoId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "REUSE_V2_PHOTO_ALBUMS",
                actor = actor,
                targetRef = "gallery/photo/$photoId",
                details = "Assigned photo '${existing.title}' to ${validAlbumIds.size} album(s)"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun updateGalleryPhotoStatus(
        actor: AccountWithPermissions,
        photoId: String,
        targetStatus: AcademicsV2RecordStatus
    ): Result<V2GalleryPhotoRecord> = runMutation {
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.GALLERY, PermissionAction.PUBLISH)
        val current = _snapshot.value
        val existing = current.galleryPhotos.firstOrNull { it.id == photoId }
            ?: throw IllegalArgumentException("Photo not found.")
        val updated = existing.copy(
            status = targetStatus,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val next = current.copy(
            galleryPhotos = current.galleryPhotos.map { if (it.id == photoId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "UPDATE_V2_PHOTO_STATUS",
                actor = actor,
                targetRef = "gallery/photo/$photoId",
                details = "Set photo '${existing.title}' status to ${targetStatus.displayLabel}"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun deleteGalleryPhoto(
        actor: AccountWithPermissions,
        photoId: String
    ): Result<Unit> = runMutation {
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.GALLERY, PermissionAction.DELETE)
        val current = _snapshot.value
        val existing = current.galleryPhotos.firstOrNull { it.id == photoId }
            ?: throw IllegalArgumentException("Photo not found.")
        val next = current.copy(
            galleryPhotos = current.galleryPhotos.filterNot { it.id == photoId },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "DELETE_V2_PHOTO",
                actor = actor,
                targetRef = "gallery/photo/$photoId",
                details = "Deleted photo '${existing.title}'"
            )
        )
        persistSnapshotLocked(next)
    }

    // -------------------------------------------------------------------------
    // Card 11: Faculty & Staff (strict 8-field public projection + approval & consent)
    // -------------------------------------------------------------------------

    suspend fun saveFacultyProfile(
        actor: AccountWithPermissions,
        existingId: String?,
        sectionKey: String,
        teacherName: String,
        approvedProfilePhotoUri: String?,
        designation: String,
        department: String,
        teachingSubjects: String,
        educationalQualifications: String,
        teachingExperience: String,
        professionalIntroduction: String,
        isSchoolApproved: Boolean,
        hasStaffConsent: Boolean
    ): Result<V2FacultyProfileRecord> = runMutation {
        val action = if (existingId == null) PermissionAction.CREATE else PermissionAction.EDIT
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.FACULTY_STAFF, action)
        require(AcademicsV2Category.FACULTY_STAFF.findSection(sectionKey) != null) {
            "Invalid Faculty & Staff section: $sectionKey"
        }

        val cleanName = teacherName.trim()
        require(cleanName.isNotEmpty()) { "Teacher / Staff name is required." }
        require(designation.trim().isNotEmpty()) { "Designation is required." }
        val cleanPhoto = approvedProfilePhotoUri?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanPhoto != null) {
            validatePhotoOnlyUri(cleanPhoto)
        }

        val current = _snapshot.value
        val existing = existingId?.let { id -> current.facultyProfiles.firstOrNull { it.id == id } }
        val saved = V2FacultyProfileRecord(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sectionKey = sectionKey,
            teacherName = cleanName,
            approvedProfilePhotoUri = cleanPhoto,
            designation = designation.trim(),
            department = department.trim(),
            teachingSubjects = teachingSubjects.trim(),
            educationalQualifications = educationalQualifications.trim(),
            teachingExperience = teachingExperience.trim(),
            professionalIntroduction = professionalIntroduction.trim(),
            isSchoolApproved = isSchoolApproved,
            hasStaffConsent = hasStaffConsent,
            status = AcademicsV2RecordStatus.DRAFT,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val updatedProfiles = if (existing == null) {
            listOf(saved) + current.facultyProfiles
        } else {
            current.facultyProfiles.map { if (it.id == existing.id) saved else it }
        }
        val next = current.copy(
            facultyProfiles = updatedProfiles,
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = if (existing == null) "CREATE_V2_FACULTY" else "EDIT_V2_FACULTY",
                actor = actor,
                targetRef = "faculty_staff/${saved.id}",
                details = "Saved staff profile '${saved.teacherName}' as Draft (schoolApproved=$isSchoolApproved, consent=$hasStaffConsent)"
            )
        )
        persistSnapshotLocked(next)
        saved
    }

    suspend fun updateFacultyProfileStatus(
        actor: AccountWithPermissions,
        profileId: String,
        targetStatus: AcademicsV2RecordStatus
    ): Result<V2FacultyProfileRecord> = runMutation {
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.FACULTY_STAFF, PermissionAction.PUBLISH)
        val current = _snapshot.value
        val existing = current.facultyProfiles.firstOrNull { it.id == profileId }
            ?: throw IllegalArgumentException("Staff profile not found.")
        if (targetStatus == AcademicsV2RecordStatus.PUBLISHED) {
            if (!existing.isSchoolApproved || !existing.hasStaffConsent) {
                throw SecurityException(
                    "Staff profile requires both School Approval and Staff Consent before public publication."
                )
            }
        }
        val updated = existing.copy(
            status = targetStatus,
            updatedByName = actor.account.displayName,
            updatedAtMillis = System.currentTimeMillis()
        )
        val next = current.copy(
            facultyProfiles = current.facultyProfiles.map { if (it.id == profileId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "UPDATE_V2_FACULTY_STATUS",
                actor = actor,
                targetRef = "faculty_staff/$profileId",
                details = "Set staff profile '${existing.teacherName}' status to ${targetStatus.displayLabel}"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun deleteFacultyProfile(
        actor: AccountWithPermissions,
        profileId: String
    ): Result<Unit> = runMutation {
        AcademicsV2AccessControl.requireCategoryPermission(actor, AcademicsV2Category.FACULTY_STAFF, PermissionAction.DELETE)
        val current = _snapshot.value
        val existing = current.facultyProfiles.firstOrNull { it.id == profileId }
            ?: throw IllegalArgumentException("Staff profile not found.")
        val next = current.copy(
            facultyProfiles = current.facultyProfiles.filterNot { it.id == profileId },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "DELETE_V2_FACULTY",
                actor = actor,
                targetRef = "faculty_staff/$profileId",
                details = "Deleted staff profile '${existing.teacherName}'"
            )
        )
        persistSnapshotLocked(next)
    }

    // -------------------------------------------------------------------------
    // Card 08: School Owner – Control Center (Private Owner Profile, Staff Access, Audit)
    // -------------------------------------------------------------------------

    fun getOwnerPrivateProfile(actor: AccountWithPermissions): Result<V2OwnerPrivateProfile> = runCatching {
        AcademicsV2AccessControl.requireSchoolOwner(actor, "View Owner Private Profile")
        _snapshot.value.ownerPrivateProfile
    }

    suspend fun updateOwnerPrivateProfile(
        actor: AccountWithPermissions,
        ownerName: String,
        designation: String,
        privateContactPhone: String,
        privateContactEmail: String,
        governanceNote: String
    ): Result<V2OwnerPrivateProfile> = runMutation {
        AcademicsV2AccessControl.requireSchoolOwner(actor, "Update Owner Private Profile")
        val updated = V2OwnerPrivateProfile(
            ownerName = ownerName.trim(),
            designation = designation.trim().ifBlank { "School Owner / Correspondent" },
            privateContactPhone = privateContactPhone.trim(),
            privateContactEmail = privateContactEmail.trim(),
            governanceNote = governanceNote.trim(),
            updatedAtMillis = System.currentTimeMillis()
        )
        val current = _snapshot.value
        val next = current.copy(
            ownerPrivateProfile = updated,
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "UPDATE_OWNER_PRIVATE_PROFILE",
                actor = actor,
                targetRef = "owner/profile",
                details = "Updated private School Owner profile (never exposed publicly)"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun submitStaffAccessRequest(
        actor: AccountWithPermissions,
        requestedRole: String,
        requestedCategoryKeys: List<String>,
        reason: String
    ): Result<V2StaffAccessRequest> = runMutation {
        if (!actor.account.isActive) {
            throw SecurityException("Inactive accounts cannot submit access requests.")
        }
        require(requestedCategoryKeys.isNotEmpty()) { "Select at least one requested responsibility." }
        require(reason.trim().isNotEmpty()) { "Reason for access request is required." }

        val request = V2StaffAccessRequest(
            id = UUID.randomUUID().toString(),
            staffAccountId = actor.account.id,
            staffUsername = actor.account.username,
            staffDisplayName = actor.account.displayName,
            requestedRole = requestedRole.trim().ifBlank {
                AcademicsV2AccessControl.roleDisplayLabel(actor.account.role)
            },
            requestedCategoryKeys = requestedCategoryKeys.distinct(),
            reason = reason.trim(),
            status = V2StaffAccessRequestStatus.PENDING,
            createdAtMillis = System.currentTimeMillis()
        )
        val current = _snapshot.value
        val next = current.copy(
            staffAccessRequests = listOf(request) + current.staffAccessRequests,
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "SUBMIT_STAFF_ACCESS_REQUEST",
                actor = actor,
                targetRef = "access_request/${request.id}",
                details = "${actor.account.displayName} requested access to ${request.requestedCategoryKeys.joinToString()}"
            )
        )
        persistSnapshotLocked(next)
        request
    }

    suspend fun reviewStaffAccessRequest(
        actor: AccountWithPermissions,
        requestId: String,
        approve: Boolean,
        reviewNote: String
    ): Result<V2StaffAccessRequest> = runMutation {
        AcademicsV2AccessControl.requireSchoolOwner(actor, "Staff Access Approval")
        val current = _snapshot.value
        val existing = current.staffAccessRequests.firstOrNull { it.id == requestId }
            ?: throw IllegalArgumentException("Staff access request not found.")
        val targetStatus = if (approve) V2StaffAccessRequestStatus.APPROVED else V2StaffAccessRequestStatus.REJECTED
        val updated = existing.copy(
            status = targetStatus,
            reviewedByName = actor.account.displayName,
            reviewedAtMillis = System.currentTimeMillis(),
            reviewNote = reviewNote.trim().takeIf { it.isNotEmpty() }
        )
        val next = current.copy(
            staffAccessRequests = current.staffAccessRequests.map { if (it.id == requestId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = if (approve) "APPROVE_STAFF_ACCESS" else "REJECT_STAFF_ACCESS",
                actor = actor,
                targetRef = "access_request/$requestId",
                details = "${if (approve) "Approved" else "Rejected"} request for ${existing.staffDisplayName}"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun revokeStaffAccessRequest(
        actor: AccountWithPermissions,
        requestId: String,
        reviewNote: String
    ): Result<V2StaffAccessRequest> = runMutation {
        AcademicsV2AccessControl.requireSchoolOwner(actor, "Revoke Staff Access")
        val current = _snapshot.value
        val existing = current.staffAccessRequests.firstOrNull { it.id == requestId }
            ?: throw IllegalArgumentException("Staff access request not found.")
        val updated = existing.copy(
            status = V2StaffAccessRequestStatus.REVOKED,
            reviewedByName = actor.account.displayName,
            reviewedAtMillis = System.currentTimeMillis(),
            reviewNote = reviewNote.trim().takeIf { it.isNotEmpty() }
        )
        val next = current.copy(
            staffAccessRequests = current.staffAccessRequests.map { if (it.id == requestId) updated else it },
            accessAuditEvents = appendAudit(
                current.accessAuditEvents,
                action = "REVOKE_STAFF_ACCESS",
                actor = actor,
                targetRef = "access_request/$requestId",
                details = "Revoked access for ${existing.staffDisplayName}"
            )
        )
        persistSnapshotLocked(next)
        updated
    }

    suspend fun logAuditEvent(
        actor: AccountWithPermissions,
        action: String,
        targetRef: String,
        details: String
    ) {
        runMutation {
            val current = _snapshot.value
            val next = current.copy(
                accessAuditEvents = appendAudit(
                    current.accessAuditEvents,
                    action = action,
                    actor = actor,
                    targetRef = targetRef,
                    details = details
                )
            )
            persistSnapshotLocked(next)
        }
    }

    private suspend fun <T> runMutation(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching(block)
        }
    }

    private fun validatePhotoOnlyUri(uri: String) {
        val lower = uri.lowercase()
        val forbiddenIndicators = listOf(
            "youtube.com",
            "youtu.be",
            "vimeo.com",
            ".mp4",
            ".mov",
            ".mkv",
            ".webm",
            ".avi",
            ".3gp",
            "video/"
        )
        if (forbiddenIndicators.any { lower.contains(it) }) {
            throw IllegalArgumentException("Video upload and video gallery links are not permitted. Only photo images are allowed.")
        }
    }

    private fun appendAudit(
        existing: List<V2AccessAuditEvent>,
        action: String,
        actor: AccountWithPermissions,
        targetRef: String,
        details: String
    ): List<V2AccessAuditEvent> {
        val event = V2AccessAuditEvent(
            id = UUID.randomUUID().toString(),
            action = action,
            actorName = actor.account.displayName,
            actorRole = AcademicsV2AccessControl.roleDisplayLabel(actor.account.role),
            targetRef = targetRef,
            details = details,
            timestampMillis = System.currentTimeMillis()
        )
        return (listOf(event) + existing).take(MAX_AUDIT_EVENTS)
    }

    private fun persistSnapshotLocked(next: AcademicsV2Snapshot) {
        storageFile.parentFile?.mkdirs()
        val tempFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
        val payload = serializeSnapshot(next).toString()
        tempFile.writeText(payload, Charsets.UTF_8)
        if (!tempFile.renameTo(storageFile)) {
            tempFile.copyTo(storageFile, overwrite = true)
            tempFile.delete()
        }
        _snapshot.value = next
    }

    private fun loadSnapshotSafely(): AcademicsV2Snapshot {
        if (!storageFile.exists()) return AcademicsV2Snapshot()
        return runCatching {
            val raw = storageFile.readText(Charsets.UTF_8)
            if (raw.isBlank()) AcademicsV2Snapshot() else deserializeSnapshot(JSONObject(raw))
        }.getOrElse { AcademicsV2Snapshot() }
    }

    private fun serializeSnapshot(s: AcademicsV2Snapshot): JSONObject {
        val root = JSONObject()
        root.put("version", 2)

        val recordsArr = JSONArray()
        s.records.forEach { r ->
            recordsArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("category", r.category.routeId)
                    put("sectionKey", r.sectionKey)
                    put("title", r.title)
                    put("description", r.description)
                    put("imageUri", r.imageUri ?: JSONObject.NULL)
                    put("attachmentUri", r.attachmentUri ?: JSONObject.NULL)
                    put("attachmentLabel", r.attachmentLabel ?: JSONObject.NULL)
                    put("publishDateText", r.publishDateText ?: JSONObject.NULL)
                    put("expiryDateMillis", r.expiryDateMillis ?: JSONObject.NULL)
                    put("status", r.status.key)
                    put("isExamResultAuthorized", r.isExamResultAuthorized)
                    put("updatedByAccountId", r.updatedByAccountId ?: JSONObject.NULL)
                    put("updatedByName", r.updatedByName)
                    put("updatedAtMillis", r.updatedAtMillis)
                    put("approvedByName", r.approvedByName ?: JSONObject.NULL)
                    put("approvedAtMillis", r.approvedAtMillis ?: JSONObject.NULL)
                }
            )
        }
        root.put("records", recordsArr)

        val albumsArr = JSONArray()
        s.galleryAlbums.forEach { a ->
            albumsArr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("sectionKey", a.sectionKey)
                    put("title", a.title)
                    put("description", a.description)
                    put("dateText", a.dateText)
                    put("status", a.status.key)
                    put("updatedByName", a.updatedByName)
                    put("updatedAtMillis", a.updatedAtMillis)
                }
            )
        }
        root.put("galleryAlbums", albumsArr)

        val photosArr = JSONArray()
        s.galleryPhotos.forEach { p ->
            photosArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("description", p.description)
                    put("dateText", p.dateText)
                    put("imageUri", p.imageUri)
                    put("albumIds", JSONArray(p.albumIds))
                    put("status", p.status.key)
                    put("updatedByName", p.updatedByName)
                    put("updatedAtMillis", p.updatedAtMillis)
                }
            )
        }
        root.put("galleryPhotos", photosArr)

        val facultyArr = JSONArray()
        s.facultyProfiles.forEach { f ->
            facultyArr.put(
                JSONObject().apply {
                    put("id", f.id)
                    put("sectionKey", f.sectionKey)
                    put("teacherName", f.teacherName)
                    put("approvedProfilePhotoUri", f.approvedProfilePhotoUri ?: JSONObject.NULL)
                    put("designation", f.designation)
                    put("department", f.department)
                    put("teachingSubjects", f.teachingSubjects)
                    put("educationalQualifications", f.educationalQualifications)
                    put("teachingExperience", f.teachingExperience)
                    put("professionalIntroduction", f.professionalIntroduction)
                    put("isSchoolApproved", f.isSchoolApproved)
                    put("hasStaffConsent", f.hasStaffConsent)
                    put("status", f.status.key)
                    put("updatedByName", f.updatedByName)
                    put("updatedAtMillis", f.updatedAtMillis)
                }
            )
        }
        root.put("facultyProfiles", facultyArr)

        root.put(
            "ownerPrivateProfile",
            JSONObject().apply {
                put("ownerName", s.ownerPrivateProfile.ownerName)
                put("designation", s.ownerPrivateProfile.designation)
                put("privateContactPhone", s.ownerPrivateProfile.privateContactPhone)
                put("privateContactEmail", s.ownerPrivateProfile.privateContactEmail)
                put("governanceNote", s.ownerPrivateProfile.governanceNote)
                put("updatedAtMillis", s.ownerPrivateProfile.updatedAtMillis)
            }
        )

        val requestsArr = JSONArray()
        s.staffAccessRequests.forEach { req ->
            requestsArr.put(
                JSONObject().apply {
                    put("id", req.id)
                    put("staffAccountId", req.staffAccountId)
                    put("staffUsername", req.staffUsername)
                    put("staffDisplayName", req.staffDisplayName)
                    put("requestedRole", req.requestedRole)
                    put("requestedCategoryKeys", JSONArray(req.requestedCategoryKeys))
                    put("reason", req.reason)
                    put("status", req.status.key)
                    put("reviewedByName", req.reviewedByName ?: JSONObject.NULL)
                    put("reviewedAtMillis", req.reviewedAtMillis ?: JSONObject.NULL)
                    put("reviewNote", req.reviewNote ?: JSONObject.NULL)
                    put("createdAtMillis", req.createdAtMillis)
                }
            )
        }
        root.put("staffAccessRequests", requestsArr)

        val auditArr = JSONArray()
        s.accessAuditEvents.forEach { ev ->
            auditArr.put(
                JSONObject().apply {
                    put("id", ev.id)
                    put("action", ev.action)
                    put("actorName", ev.actorName)
                    put("actorRole", ev.actorRole)
                    put("targetRef", ev.targetRef)
                    put("details", ev.details)
                    put("timestampMillis", ev.timestampMillis)
                }
            )
        }
        root.put("accessAuditEvents", auditArr)
        return root
    }

    private fun deserializeSnapshot(root: JSONObject): AcademicsV2Snapshot {
        val records = mutableListOf<AcademicsV2Record>()
        val recordsArr = root.optJSONArray("records") ?: JSONArray()
        for (i in 0 until recordsArr.length()) {
            val o = recordsArr.optJSONObject(i) ?: continue
            val cat = AcademicsV2Category.fromRouteId(o.optString("category")) ?: continue
            records += AcademicsV2Record(
                id = o.optString("id"),
                category = cat,
                sectionKey = o.optString("sectionKey"),
                title = o.optString("title"),
                description = o.optString("description"),
                imageUri = o.optNullableString("imageUri"),
                attachmentUri = o.optNullableString("attachmentUri"),
                attachmentLabel = o.optNullableString("attachmentLabel"),
                publishDateText = o.optNullableString("publishDateText"),
                expiryDateMillis = o.optNullableLong("expiryDateMillis"),
                status = AcademicsV2RecordStatus.fromKey(o.optString("status")),
                isExamResultAuthorized = o.optBoolean("isExamResultAuthorized", false),
                updatedByAccountId = o.optNullableLong("updatedByAccountId"),
                updatedByName = o.optString("updatedByName"),
                updatedAtMillis = o.optLong("updatedAtMillis", 0L),
                approvedByName = o.optNullableString("approvedByName"),
                approvedAtMillis = o.optNullableLong("approvedAtMillis")
            )
        }

        val albums = mutableListOf<V2GalleryAlbumRecord>()
        val albumsArr = root.optJSONArray("galleryAlbums") ?: JSONArray()
        for (i in 0 until albumsArr.length()) {
            val o = albumsArr.optJSONObject(i) ?: continue
            albums += V2GalleryAlbumRecord(
                id = o.optString("id"),
                sectionKey = o.optString("sectionKey"),
                title = o.optString("title"),
                description = o.optString("description"),
                dateText = o.optString("dateText"),
                status = AcademicsV2RecordStatus.fromKey(o.optString("status")),
                updatedByName = o.optString("updatedByName"),
                updatedAtMillis = o.optLong("updatedAtMillis", 0L)
            )
        }

        val photos = mutableListOf<V2GalleryPhotoRecord>()
        val photosArr = root.optJSONArray("galleryPhotos") ?: JSONArray()
        for (i in 0 until photosArr.length()) {
            val o = photosArr.optJSONObject(i) ?: continue
            photos += V2GalleryPhotoRecord(
                id = o.optString("id"),
                title = o.optString("title"),
                description = o.optString("description"),
                dateText = o.optString("dateText"),
                imageUri = o.optString("imageUri"),
                albumIds = o.optJSONArray("albumIds").toStringList(),
                status = AcademicsV2RecordStatus.fromKey(o.optString("status")),
                updatedByName = o.optString("updatedByName"),
                updatedAtMillis = o.optLong("updatedAtMillis", 0L)
            )
        }

        val faculty = mutableListOf<V2FacultyProfileRecord>()
        val facultyArr = root.optJSONArray("facultyProfiles") ?: JSONArray()
        for (i in 0 until facultyArr.length()) {
            val o = facultyArr.optJSONObject(i) ?: continue
            faculty += V2FacultyProfileRecord(
                id = o.optString("id"),
                sectionKey = o.optString("sectionKey"),
                teacherName = o.optString("teacherName"),
                approvedProfilePhotoUri = o.optNullableString("approvedProfilePhotoUri"),
                designation = o.optString("designation"),
                department = o.optString("department"),
                teachingSubjects = o.optString("teachingSubjects"),
                educationalQualifications = o.optString("educationalQualifications"),
                teachingExperience = o.optString("teachingExperience"),
                professionalIntroduction = o.optString("professionalIntroduction"),
                isSchoolApproved = o.optBoolean("isSchoolApproved", false),
                hasStaffConsent = o.optBoolean("hasStaffConsent", false),
                status = AcademicsV2RecordStatus.fromKey(o.optString("status")),
                updatedByName = o.optString("updatedByName"),
                updatedAtMillis = o.optLong("updatedAtMillis", 0L)
            )
        }

        val ownerObj = root.optJSONObject("ownerPrivateProfile")
        val ownerProfile = if (ownerObj != null) {
            V2OwnerPrivateProfile(
                ownerName = ownerObj.optString("ownerName"),
                designation = ownerObj.optString("designation", "School Owner / Correspondent"),
                privateContactPhone = ownerObj.optString("privateContactPhone"),
                privateContactEmail = ownerObj.optString("privateContactEmail"),
                governanceNote = ownerObj.optString("governanceNote"),
                updatedAtMillis = ownerObj.optLong("updatedAtMillis", 0L)
            )
        } else {
            V2OwnerPrivateProfile()
        }

        val requests = mutableListOf<V2StaffAccessRequest>()
        val requestsArr = root.optJSONArray("staffAccessRequests") ?: JSONArray()
        for (i in 0 until requestsArr.length()) {
            val o = requestsArr.optJSONObject(i) ?: continue
            requests += V2StaffAccessRequest(
                id = o.optString("id"),
                staffAccountId = o.optLong("staffAccountId"),
                staffUsername = o.optString("staffUsername"),
                staffDisplayName = o.optString("staffDisplayName"),
                requestedRole = o.optString("requestedRole"),
                requestedCategoryKeys = o.optJSONArray("requestedCategoryKeys").toStringList(),
                reason = o.optString("reason"),
                status = V2StaffAccessRequestStatus.fromKey(o.optString("status")),
                reviewedByName = o.optNullableString("reviewedByName"),
                reviewedAtMillis = o.optNullableLong("reviewedAtMillis"),
                reviewNote = o.optNullableString("reviewNote"),
                createdAtMillis = o.optLong("createdAtMillis", 0L)
            )
        }

        val audits = mutableListOf<V2AccessAuditEvent>()
        val auditArr = root.optJSONArray("accessAuditEvents") ?: JSONArray()
        for (i in 0 until auditArr.length()) {
            val o = auditArr.optJSONObject(i) ?: continue
            audits += V2AccessAuditEvent(
                id = o.optString("id"),
                action = o.optString("action"),
                actorName = o.optString("actorName"),
                actorRole = o.optString("actorRole"),
                targetRef = o.optString("targetRef"),
                details = o.optString("details"),
                timestampMillis = o.optLong("timestampMillis", 0L)
            )
        }

        return AcademicsV2Snapshot(
            records = records,
            galleryAlbums = albums,
            galleryPhotos = photos,
            facultyProfiles = faculty,
            ownerPrivateProfile = ownerProfile,
            staffAccessRequests = requests,
            accessAuditEvents = audits
        )
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val value = optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    companion object {
        private const val MAX_AUDIT_EVENTS = 250
    }
}
