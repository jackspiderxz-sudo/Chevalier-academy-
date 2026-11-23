package com.example.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.example.core.notifications.LocalAlertNotifier
import com.example.core.security.PinSecurityManager
import com.example.data.backup.BackupPreviewInfo
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.ContentApprovalEntity
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.LocalNotificationEntity
import com.example.data.local.entity.ManagementAccountEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.local.entity.VersionHistoryEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.AcademyHeroBannerConfig
import com.example.data.preferences.AdminHeroBannerConfig
import com.example.data.preferences.PostFeedVisibility
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class PublishSaveMode {
    SAVE_DRAFT,
    SUBMIT_FOR_APPROVAL,
    PUBLISH_NOW
}

data class MasterSetupResult(
    val account: AccountWithPermissions,
    val recoveryCode: String
)

enum class AdminDashboardCard(
    val id: String,
    val title: String,
    val keywords: List<String>,
    val requiredViewSections: List<ManagementSection>
) {
    HOME_PAGE(
        id = "home_page",
        title = "Home Page",
        keywords = listOf("home", "banner", "hero", "posts", "highlights", "feed"),
        requiredViewSections = listOf(
            ManagementSection.LOGO_BANNER,
            ManagementSection.SCHOOL_POSTS,
            ManagementSection.SCHOOL_HIGHLIGHTS,
            ManagementSection.MEDIA_STUDIO
        )
    ),
    ACADEMY(
        id = "academy",
        title = "Academy",
        keywords = listOf("academy", "academics", "courses", "facilities", "transport", "gallery", "about"),
        requiredViewSections = listOf(
            ManagementSection.ACADEMICS,
            ManagementSection.HIGHER_SECONDARY_COURSES,
            ManagementSection.FACILITIES,
            ManagementSection.TRANSPORT,
            ManagementSection.PHOTO_GALLERY,
            ManagementSection.ABOUT_SCHOOL
        )
    ),
    PARENT(
        id = "parent",
        title = "Parent",
        keywords = listOf("parent", "portal", "circulars", "communication", "family"),
        requiredViewSections = listOf(
            ManagementSection.ANNOUNCEMENTS,
            ManagementSection.NOTIFICATIONS,
            ManagementSection.SCHOOL_PROFILE
        )
    ),
    CONTACT(
        id = "contact",
        title = "Contact",
        keywords = listOf("contact", "phone", "email", "address", "office"),
        requiredViewSections = listOf(ManagementSection.CONTACT_DETAILS)
    ),
    STAFF_MANAGEMENT(
        id = "staff_management",
        title = "Staff Management",
        keywords = listOf("staff", "team", "roles", "permissions", "accounts", "owner", "admin"),
        requiredViewSections = listOf(ManagementSection.STAFF_MANAGEMENT)
    ),
    COMMON_APP(
        id = "common_app",
        title = "Common App",
        keywords = listOf("common", "app", "settings", "language", "theme", "profile", "social", "logo"),
        requiredViewSections = listOf(
            ManagementSection.APP_SETTINGS,
            ManagementSection.SCHOOL_PROFILE,
            ManagementSection.SOCIAL_MEDIA,
            ManagementSection.LOGO_BANNER
        )
    ),
    MANAGEMENT_TOOLS(
        id = "management_tools",
        title = "Management Tools",
        keywords = listOf("tools", "approval", "approvals", "version", "history", "activity", "log", "backup", "restore", "media"),
        requiredViewSections = listOf(
            ManagementSection.ACTIVITY_LOG,
            ManagementSection.BACKUP_RESTORE,
            ManagementSection.MEDIA_STUDIO
        )
    ),
    UPDATES(
        id = "updates",
        title = "Updates",
        keywords = listOf("updates", "news", "announcements", "notifications", "alerts"),
        requiredViewSections = listOf(
            ManagementSection.NEWS,
            ManagementSection.ANNOUNCEMENTS,
            ManagementSection.NOTIFICATIONS
        )
    )
}

enum class AdminDashboardSearchKind {
    DASHBOARD_CARD,
    MANAGEMENT_SECTION,
    MANAGEMENT_RECORD
}

data class AdminDashboardSearchResult(
    val id: String,
    val kind: AdminDashboardSearchKind,
    val title: String,
    val subtitle: String,
    val badge: String,
    val targetCard: AdminDashboardCard? = null,
    val targetSection: ManagementSection? = null,
    val targetToolKey: String? = null
)

data class ManagementSearchResult(
    val section: ManagementSection,
    val areaLabel: String,
    val recordId: Long,
    val title: String,
    val snippet: String,
    val statusLabel: String
)

enum class ResumedRevisionKind {
    NONE,
    DRAFT,
    PENDING_APPROVAL
}

data class EditableRecordResolution(
    val section: ManagementSection,
    val recordId: Long,
    val liveSnapshotJson: String?,
    val editableSnapshotJson: String?,
    val revisionKind: ResumedRevisionKind,
    val versionNumber: Int? = null,
    val revisionAuthor: String? = null,
    val revisionTimestampMillis: Long? = null,
    val hasLivePublishedVersion: Boolean = false
)

class ManagementRepository(
    private val database: ChevalierDatabase,
    private val mediaStudioManager: MediaStudioManager,
    private val preferencesRepository: UserPreferencesRepository? = null,
    private val localAlertNotifier: LocalAlertNotifier? = null
) {
    private val accountDao = database.managementAccountDao()
    private val approvalDao = database.contentApprovalDao()
    private val versionDao = database.versionHistoryDao()
    private val mediaDao = database.mediaStudioDao()
    private val logDao = database.activityLogDao()
    private val notificationDao = database.localNotificationDao()
    private val coordinator = ManagementContentCoordinator(database)

    // Reactive streams
    val masterAdminExists: Flow<Boolean> = accountDao.observeMasterAdmin().map { it != null }
    val accountsWithPermissions: Flow<List<AccountWithPermissions>> = accountDao.observeAllAccountsWithPermissions()
    val allApprovals: Flow<List<ContentApprovalEntity>> = approvalDao.observeAll()
    val pendingApprovals: Flow<List<ContentApprovalEntity>> = approvalDao.observePending()
    val pendingApprovalsCount: Flow<Int> = approvalDao.observePendingCount()
    val allVersionHistory: Flow<List<VersionHistoryEntity>> = versionDao.observeAll()
    val allMediaStudioItems: Flow<List<MediaStudioItemEntity>> = mediaDao.observeAll()
    val allNotifications: Flow<List<LocalNotificationEntity>> = notificationDao.observeAll()
    val unreadNotificationsCount: Flow<Int> = notificationDao.observeUnreadCount()

    // Management views of school content (includes drafts, pending, rejected, unpublished, published)
    val allHeroPhotos = database.campusHeroPhotoDao().observeAllForManagement()
    val allAcademics = database.academicInfoDao().observeAll()
    val allCourses = database.higherSecondaryCourseDao().observeAll()
    val allFacilities = database.facilityDao().observeAll()
    val allTransportRoutes = database.transportDao().observeAll()
    val allNews = database.newsDao().observeAll()
    val allAnnouncements = database.announcementDao().observeAll()
    val allGalleryAlbumsWithMedia = database.galleryDao().observeAlbumsWithMedia()
    val allGalleryMedia = database.galleryDao().observeAllMedia()
    val allSocialLinks = database.socialMediaDao().observeAll()
    val allSchoolPosts = database.schoolPostDao().observeAllPosts()
    val allSchoolHighlights = database.schoolHighlightDao().observeAllHighlights()
    val schoolProfile = database.schoolProfileDao().observeProfile()

    fun observeVisibleActivityLogs(actor: AccountWithPermissions): Flow<List<ActivityLogEntity>> =
        logDao.observeAll().map { logs ->
            if (actor.account.role == ManagementRole.MASTER_ADMIN) {
                logs
            } else if (!hasPermission(actor, ManagementSection.ACTIVITY_LOG, PermissionAction.VIEW)) {
                emptyList()
            } else {
                logs.filter { log ->
                    val section = ManagementSection.fromKey(log.sectionKey)
                    val allowedSection = section == null || hasPermission(actor, section, PermissionAction.VIEW)
                    allowedSection && (log.actorAccountId == actor.account.id ||
                        (log.sectionKey != ManagementSection.STAFF_MANAGEMENT.key &&
                            log.sectionKey != ManagementSection.BACKUP_RESTORE.key))
                }
            }
        }

    fun observeVisibleApprovals(actor: AccountWithPermissions): Flow<List<ContentApprovalEntity>> =
        approvalDao.observeAll().map { approvals ->
            if (actor.account.role == ManagementRole.MASTER_ADMIN) {
                approvals
            } else {
                approvals.filter { item ->
                    val section = ManagementSection.fromKey(item.sectionKey)
                    item.submittedByAccountId == actor.account.id ||
                        (section != null && (
                            hasPermission(actor, section, PermissionAction.VIEW) ||
                                hasPermission(actor, section, PermissionAction.PUBLISH)
                            ))
                }
            }
        }

    fun observeVisiblePendingApprovalsCount(actor: AccountWithPermissions): Flow<Int> =
        observeVisibleApprovals(actor).map { items ->
            items.count { it.status == ContentWorkflowStatus.PENDING_APPROVAL }
        }

    fun observeVisibleVersionHistory(actor: AccountWithPermissions): Flow<List<VersionHistoryEntity>> =
        versionDao.observeAll().map { versions ->
            if (actor.account.role == ManagementRole.MASTER_ADMIN) {
                versions
            } else {
                versions.filter { version ->
                    val section = ManagementSection.fromKey(version.sectionKey)
                    section != null &&
                        hasPermission(actor, section, PermissionAction.VIEW) &&
                        version.sectionKey != ManagementSection.STAFF_MANAGEMENT.key &&
                        version.sectionKey != ManagementSection.BACKUP_RESTORE.key
                }
            }
        }

    fun observeVisibleNotifications(actor: AccountWithPermissions): Flow<List<LocalNotificationEntity>> =
        notificationDao.observeAll().map { notifications ->
            if (actor.account.role == ManagementRole.MASTER_ADMIN) {
                notifications
            } else if (!hasPermission(actor, ManagementSection.NOTIFICATIONS, PermissionAction.VIEW)) {
                emptyList()
            } else {
                notifications.filter { note ->
                    val section = ManagementSection.fromKey(note.sectionKey)
                    val isRestrictedSecuritySection =
                        note.sectionKey == ManagementSection.STAFF_MANAGEMENT.key ||
                            note.sectionKey == ManagementSection.BACKUP_RESTORE.key
                    if (isRestrictedSecuritySection) {
                        section != null && hasPermission(actor, section, PermissionAction.VIEW)
                    } else {
                        section == null || hasPermission(actor, section, PermissionAction.VIEW)
                    }
                }
            }
        }

    fun observeVisibleUnreadNotificationsCount(actor: AccountWithPermissions): Flow<Int> =
        observeVisibleNotifications(actor).map { items ->
            items.count { !it.isRead }
        }

    fun hasPermission(
        actor: AccountWithPermissions,
        section: ManagementSection,
        action: PermissionAction
    ): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true
        val perm = actor.permissions.firstOrNull { it.sectionKey == section.key } ?: return false
        return when (action) {
            PermissionAction.VIEW -> perm.canView
            PermissionAction.CREATE -> perm.canCreate
            PermissionAction.EDIT -> perm.canEdit
            PermissionAction.DELETE -> perm.canDelete
            PermissionAction.PUBLISH -> perm.canPublish
        }
    }

    fun requirePermission(
        actor: AccountWithPermissions,
        section: ManagementSection,
        action: PermissionAction
    ) {
        if (!hasPermission(actor, section, action)) {
            throw SecurityException(
                "Permission denied: ${actor.account.displayName} lacks ${action.name} permission for ${section.key}."
            )
        }
    }

    fun canAccessApprovalCenter(actor: AccountWithPermissions): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true
        return ManagementSection.entries.any { section ->
            hasPermission(actor, section, PermissionAction.PUBLISH) ||
                hasPermission(actor, section, PermissionAction.EDIT)
        }
    }

    fun canAccessVersionHistory(actor: AccountWithPermissions): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true
        return ManagementSection.entries.any { section ->
            hasPermission(actor, section, PermissionAction.VIEW)
        }
    }

    fun canAccessAdminDashboardCard(
        actor: AccountWithPermissions,
        card: AdminDashboardCard
    ): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true
        if (card == AdminDashboardCard.MANAGEMENT_TOOLS) {
            return card.requiredViewSections.any { hasPermission(actor, it, PermissionAction.VIEW) } ||
                canAccessApprovalCenter(actor)
        }
        return card.requiredViewSections.any { section ->
            hasPermission(actor, section, PermissionAction.VIEW)
        }
    }

    fun canEditAdminHeroBanner(actor: AccountWithPermissions): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true
        return hasPermission(actor, ManagementSection.LOGO_BANNER, PermissionAction.EDIT) ||
            hasPermission(actor, ManagementSection.SCHOOL_PROFILE, PermissionAction.EDIT)
    }

    suspend fun importAdminHeroBackgroundImage(
        actor: AccountWithPermissions,
        sourceUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!canEditAdminHeroBanner(actor)) {
                throw SecurityException(
                    "Permission denied: ${actor.account.displayName} is not authorized to update Admin Hero Banner media."
                )
            }
            val processed = mediaStudioManager.importAndOptimizeImage(sourceUri).getOrThrow()
            processed.optimizedUri
        }
    }

    suspend fun saveAdminHeroBannerConfig(
        actor: AccountWithPermissions,
        titleLine1: String,
        welcomeLine2: String,
        subtitleLine3: String,
        backgroundImageUri: String?
    ): Result<AdminHeroBannerConfig> = withContext(Dispatchers.IO) {
        runCatching {
            if (!canEditAdminHeroBanner(actor)) {
                throw SecurityException(
                    "Permission denied: ${actor.account.displayName} is not authorized to edit the Admin Hero Banner."
                )
            }
            val cleanLine1 = titleLine1.trim().take(80)
                .ifEmpty { AdminHeroBannerConfig.DEFAULT_TITLE_LINE_1 }
            val cleanLine2 = welcomeLine2.trim().take(100)
                .ifEmpty { AdminHeroBannerConfig.DEFAULT_WELCOME_LINE_2 }
            val cleanLine3 = subtitleLine3.trim().take(100)
                .ifEmpty { AdminHeroBannerConfig.DEFAULT_SUBTITLE_LINE_3 }
            val cleanImageUri = backgroundImageUri?.trim()?.takeIf { it.isNotEmpty() }
            val now = System.currentTimeMillis()
            val updated = AdminHeroBannerConfig(
                titleLine1 = cleanLine1,
                welcomeLine2 = cleanLine2,
                subtitleLine3 = cleanLine3,
                backgroundImageUri = cleanImageUri,
                updatedAtMillis = now,
                updatedByName = actor.account.displayName
            )
            preferencesRepository?.saveAdminHeroBanner(updated)
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = "ADMIN_HERO_BANNER_UPDATED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.LOGO_BANNER.key,
                    recordRef = "AdminHeroBanner",
                    details = "Updated Admin Dashboard Hero Banner ('$cleanLine1' / '$cleanLine3')."
                )
            )
            updated
        }
    }

    fun canEditAcademyHeroBanner(actor: AccountWithPermissions): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true
        return hasPermission(actor, ManagementSection.LOGO_BANNER, PermissionAction.EDIT) ||
            hasPermission(actor, ManagementSection.ACADEMICS, PermissionAction.EDIT)
    }

    suspend fun importAcademyHeroBannerImage(
        actor: AccountWithPermissions,
        sourceUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!canEditAcademyHeroBanner(actor)) {
                throw SecurityException(
                    "Permission denied: ${actor.account.displayName} is not authorized to update Academy Hero Banner media."
                )
            }
            val processed = mediaStudioManager.importAndOptimizeImage(sourceUri).getOrThrow()
            processed.optimizedUri
        }
    }

    suspend fun saveAcademyHeroBannerConfig(
        actor: AccountWithPermissions,
        imageUri: String?,
        overlayTitle: String,
        overlaySubtitle: String,
        publishNow: Boolean
    ): Result<AcademyHeroBannerConfig> = withContext(Dispatchers.IO) {
        runCatching {
            if (!canEditAcademyHeroBanner(actor)) {
                throw SecurityException(
                    "Permission denied: ${actor.account.displayName} is not authorized to edit the Academy Hero Banner."
                )
            }
            val cleanImageUri = imageUri?.trim()?.takeIf { it.isNotEmpty() }
            if (publishNow) {
                require(cleanImageUri != null) {
                    "Upload a banner image before publishing the Academy Hero Banner."
                }
            }
            // Optional overlay text: NEVER force default text; image-only is explicitly supported.
            val cleanTitle = overlayTitle.trim().take(90)
            val cleanSubtitle = overlaySubtitle.trim().take(160)
            val now = System.currentTimeMillis()
            val updated = AcademyHeroBannerConfig(
                imageUri = cleanImageUri,
                overlayTitle = cleanTitle,
                overlaySubtitle = cleanSubtitle,
                isPublished = publishNow && cleanImageUri != null,
                updatedAtMillis = now,
                updatedByName = actor.account.displayName
            )
            preferencesRepository?.saveAcademyHeroBanner(updated)
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = "ACADEMY_HERO_BANNER_UPDATED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.ACADEMICS.key,
                    recordRef = "AcademyHeroBanner",
                    details = buildString {
                        append(if (updated.isPublished) "Published" else "Saved")
                        append(" Academy Hero Banner (")
                        append(if (updated.hasOverlayText) "image with text" else "image only")
                        append(").")
                    }
                )
            )
            updated
        }
    }

    suspend fun setPostFeedVisibility(
        actor: AccountWithPermissions,
        postRemoteId: String,
        showOnHomeFeed: Boolean,
        showOnAcademyFeed: Boolean
    ): Result<PostFeedVisibility> = withContext(Dispatchers.IO) {
        runCatching {
            val allowed = actor.account.isActive && (
                actor.account.role == ManagementRole.MASTER_ADMIN ||
                    hasPermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.CREATE) ||
                    hasPermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.EDIT) ||
                    hasPermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.PUBLISH)
                )
            if (!allowed) {
                throw SecurityException(
                    "Permission denied: ${actor.account.displayName} is not authorized to change post feed visibility."
                )
            }
            val cleanRemoteId = postRemoteId.trim()
            require(cleanRemoteId.isNotEmpty()) { "Post identifier is required." }
            require(showOnHomeFeed || showOnAcademyFeed) {
                "Select at least one feed (Home Page School Feed or Academy Social Feed)."
            }
            preferencesRepository?.setPostFeedVisibility(
                postRemoteId = cleanRemoteId,
                showOnHomeFeed = showOnHomeFeed,
                showOnAcademyFeed = showOnAcademyFeed
            )
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = "POST_FEED_VISIBILITY_UPDATED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.SCHOOL_POSTS.key,
                    recordRef = cleanRemoteId,
                    details = "Updated feed visibility (home=$showOnHomeFeed, academy=$showOnAcademyFeed)."
                )
            )
            PostFeedVisibility(
                showOnHomeFeed = showOnHomeFeed,
                showOnAcademyFeed = showOnAcademyFeed
            )
        }
    }

    // ==================== AUTHENTICATION & MASTER ADMIN ====================

    suspend fun setupMasterAdmin(
        displayName: String,
        username: String,
        pin: String,
        confirmPin: String
    ): Result<MasterSetupResult> = withContext(Dispatchers.Default) {
        runCatching {
            val cleanName = displayName.trim().ifEmpty { "Master Administrator" }
            val cleanUser = username.trim().lowercase().ifEmpty { "admin" }
            require(accountDao.getMasterAdminOnce() == null) {
                "Master Admin has already been configured on this device."
            }
            require(PinSecurityManager.isValidPinFormat(pin)) {
                "PIN must be at least 6 numeric digits."
            }
            require(pin == confirmPin) {
                "PIN and Confirm PIN do not match."
            }
            val recoveryCode = PinSecurityManager.generateRecoveryCode()
            val pinSalt = PinSecurityManager.generateSaltBase64()
            val pinVerifier = PinSecurityManager.deriveVerifierBase64(pin, pinSalt)
            val recSalt = PinSecurityManager.generateSaltBase64()
            val recVerifier = PinSecurityManager.deriveVerifierBase64(recoveryCode, recSalt)
            val now = System.currentTimeMillis()
            val created = database.withTransaction {
                require(accountDao.getMasterAdminOnce() == null) {
                    "Master Admin has already been configured on this device."
                }
                val id = accountDao.insertAccount(
                    ManagementAccountEntity(
                        username = cleanUser,
                        displayName = cleanName,
                        role = ManagementRole.MASTER_ADMIN,
                        pinSaltBase64 = pinSalt,
                        pinVerifierBase64 = pinVerifier,
                        recoverySaltBase64 = recSalt,
                        recoveryVerifierBase64 = recVerifier,
                        isActive = true,
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                )
                val loaded = accountDao.findWithPermissionsById(id)
                    ?: throw IllegalStateException("Failed to load Master Admin account after creation.")

                appendAuditAndNotify(
                    actionType = "MASTER_ADMIN_SETUP",
                    actor = loaded,
                    sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                    recordRef = cleanUser,
                    details = "Initial Master Admin account '$cleanName' configured locally.",
                    notifyTitle = "Master Admin Configured",
                    notifyMessage = "Master Admin '$cleanName' was initialized. Keep your recovery key offline."
                )
                loaded
            }
            dispatchSystemAlertIfEnabled(
                eventId = created.account.id.toInt(),
                title = "Master Admin Configured",
                message = "Master Admin '$cleanName' was initialized. Keep your recovery key offline."
            )
            MasterSetupResult(created, recoveryCode)
        }
    }

    suspend fun loginWithPin(username: String, pin: String): Result<AccountWithPermissions> = withContext(Dispatchers.Default) {
        runCatching {
            val cleanUser = username.trim().lowercase()
            require(cleanUser.isNotEmpty()) { "Username is required." }
            require(pin.isNotEmpty()) { "PIN is required." }

            val account = accountDao.findByUsername(cleanUser)
                ?: throw IllegalArgumentException("No management account found for '$cleanUser'.")

            if (!account.isActive) {
                logDao.appendLog(
                    ActivityLogEntity(
                        actionType = "LOGIN_DENIED_DEACTIVATED",
                        actorAccountId = account.id,
                        actorName = account.displayName,
                        actorRole = account.role,
                        sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                        recordRef = account.username,
                        details = "Blocked login attempt for deactivated account."
                    )
                )
                throw SecurityException("This staff account is deactivated. Contact Master Admin.")
            }

            val now = System.currentTimeMillis()
            if (account.lockoutUntilMillis > now) {
                val secondsRemaining = ((account.lockoutUntilMillis - now) / 1000L).coerceAtLeast(1L)
                throw IllegalStateException(
                    "Too many failed attempts. Try again in $secondsRemaining seconds."
                )
            }

            val valid = PinSecurityManager.verifySecretConstantTime(
                inputSecret = pin,
                saltBase64 = account.pinSaltBase64,
                expectedVerifierBase64 = account.pinVerifierBase64
            )
            if (!valid) {
                val nextFailures = account.failedAttempts + 1
                val lockDuration = PinSecurityManager.computeLockoutDurationMillis(nextFailures)
                val lockUntil = if (lockDuration > 0L) now + lockDuration else 0L
                database.withTransaction {
                    accountDao.updateAccount(
                        account.copy(
                            failedAttempts = nextFailures,
                            lockoutUntilMillis = lockUntil,
                            updatedAtMillis = now
                        )
                    )
                    logDao.appendLog(
                        ActivityLogEntity(
                            actionType = "FAILED_LOGIN",
                            actorAccountId = account.id,
                            actorName = account.displayName,
                            actorRole = account.role,
                            sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                            recordRef = account.username,
                            details = if (lockDuration > 0L) {
                                "Invalid PIN ($nextFailures failed attempts). Locked for ${lockDuration / 1000}s."
                            } else {
                                "Invalid PIN ($nextFailures failed attempts)."
                            }
                        )
                    )
                }
                if (lockDuration > 0L) {
                    throw IllegalArgumentException("Incorrect PIN. Account locked for ${lockDuration / 1000} seconds.")
                } else {
                    throw IllegalArgumentException("Incorrect PIN. Failed attempts: $nextFailures.")
                }
            }

            database.withTransaction {
                accountDao.updateAccount(
                    account.copy(
                        failedAttempts = 0,
                        lockoutUntilMillis = 0L,
                        updatedAtMillis = now
                    )
                )
                val loaded = accountDao.findWithPermissionsById(account.id)
                    ?: throw IllegalStateException("Unable to load authenticated session.")

                logDao.appendLog(
                    ActivityLogEntity(
                        actionType = "LOGIN",
                        actorAccountId = loaded.account.id,
                        actorName = loaded.account.displayName,
                        actorRole = loaded.account.role,
                        sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                        recordRef = loaded.account.username,
                        details = "Authenticated into local Management Dashboard."
                    )
                )
                loaded
            }
        }
    }

    suspend fun recoverMasterAdminPin(
        recoveryCode: String,
        newPin: String,
        confirmPin: String
    ): Result<MasterSetupResult> = withContext(Dispatchers.Default) {
        runCatching {
            val master = accountDao.getMasterAdminOnce()
                ?: throw IllegalStateException("Master Admin account is not configured.")
            val recSalt = master.recoverySaltBase64
                ?: throw IllegalStateException("No recovery key available.")
            val recVerifier = master.recoveryVerifierBase64
                ?: throw IllegalStateException("No recovery verifier available.")

            val normalizedInput = recoveryCode.trim().uppercase()
            val ok = PinSecurityManager.verifySecretConstantTime(normalizedInput, recSalt, recVerifier)
            if (!ok) {
                logDao.appendLog(
                    ActivityLogEntity(
                        actionType = "PIN_RECOVERY_FAILED",
                        actorAccountId = master.id,
                        actorName = master.displayName,
                        actorRole = master.role,
                        sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                        recordRef = master.username,
                        details = "Invalid Master Admin recovery code submitted."
                    )
                )
                throw SecurityException("Invalid Master Admin recovery code.")
            }

            require(PinSecurityManager.isValidPinFormat(newPin)) {
                "New PIN must be at least 6 numeric digits."
            }
            require(newPin == confirmPin) {
                "New PIN and confirmation do not match."
            }

            val newPinSalt = PinSecurityManager.generateSaltBase64()
            val newPinVerifier = PinSecurityManager.deriveVerifierBase64(newPin, newPinSalt)
            val rotatedRecoveryCode = PinSecurityManager.generateRecoveryCode()
            val newRecSalt = PinSecurityManager.generateSaltBase64()
            val newRecVerifier = PinSecurityManager.deriveVerifierBase64(rotatedRecoveryCode, newRecSalt)

            val refreshed = database.withTransaction {
                accountDao.updateAccount(
                    master.copy(
                        pinSaltBase64 = newPinSalt,
                        pinVerifierBase64 = newPinVerifier,
                        recoverySaltBase64 = newRecSalt,
                        recoveryVerifierBase64 = newRecVerifier,
                        failedAttempts = 0,
                        lockoutUntilMillis = 0L,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                val reloaded = accountDao.findWithPermissionsById(master.id)!!
                appendAuditAndNotify(
                    actionType = "MASTER_PIN_RECOVERED",
                    actor = reloaded,
                    sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                    recordRef = master.username,
                    details = "Master Admin PIN reset using offline recovery key.",
                    notifyTitle = "Master PIN Reset",
                    notifyMessage = "Master Admin PIN was reset via recovery code."
                )
                reloaded
            }
            dispatchSystemAlertIfEnabled(
                eventId = 2001,
                title = "Master PIN Reset",
                message = "Master Admin PIN was reset via offline recovery key."
            )
            MasterSetupResult(refreshed, rotatedRecoveryCode)
        }
    }

    suspend fun recordLogout(actor: AccountWithPermissions) {
        logDao.appendLog(
            ActivityLogEntity(
                actionType = "LOGOUT",
                actorAccountId = actor.account.id,
                actorName = actor.account.displayName,
                actorRole = actor.account.role,
                sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                recordRef = actor.account.username,
                details = "Signed out of Management Dashboard."
            )
        )
    }

    // ==================== STAFF & SUPER OWNER MANAGEMENT ====================

    suspend fun createStaffAccount(
        actor: AccountWithPermissions,
        username: String,
        displayName: String,
        role: String,
        initialPin: String,
        permissions: List<StaffPermissionEntity>
    ): Result<AccountWithPermissions> = runCatching {
        if (actor.account.role != ManagementRole.MASTER_ADMIN) {
            throw SecurityException("Only Master Admin can create staff or super-owner accounts.")
        }
        val cleanUser = username.trim().lowercase()
        val cleanName = displayName.trim()
        require(cleanUser.isNotEmpty() && cleanName.isNotEmpty()) {
            "Username and display name are required."
        }
        require(role == ManagementRole.STAFF || role == ManagementRole.SUPER_OWNER) {
            "Invalid management role: $role."
        }
        if (role == ManagementRole.SUPER_OWNER) {
            require(accountDao.countSuperOwners() < 2) {
                "Maximum of 2 Super Owner accounts allowed."
            }
        }
        require(accountDao.findByUsername(cleanUser) == null) {
            "An account with username '$cleanUser' already exists."
        }
        require(PinSecurityManager.isValidPinFormat(initialPin)) {
            "PIN must be at least 6 numeric digits."
        }

        val salt = PinSecurityManager.generateSaltBase64()
        val verifier = PinSecurityManager.deriveVerifierBase64(initialPin, salt)
        val now = System.currentTimeMillis()
        val created = database.withTransaction {
            if (role == ManagementRole.SUPER_OWNER) {
                require(accountDao.countSuperOwners() < 2) {
                    "Maximum of 2 Super Owner accounts allowed."
                }
            }
            require(accountDao.findByUsername(cleanUser) == null) {
                "An account with username '$cleanUser' already exists."
            }
            val newId = accountDao.insertAccount(
                ManagementAccountEntity(
                    username = cleanUser,
                    displayName = cleanName,
                    role = role,
                    pinSaltBase64 = salt,
                    pinVerifierBase64 = verifier,
                    isActive = true,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            accountDao.replacePermissions(
                newId,
                permissions.map { it.copy(id = 0, accountId = newId) }
            )
            val loaded = accountDao.findWithPermissionsById(newId)!!
            appendAuditAndNotify(
                actionType = "STAFF_CREATED",
                actor = actor,
                sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                recordRef = cleanUser,
                details = "Created $role account '$cleanName' ($cleanUser).",
                notifyTitle = "New Account Added",
                notifyMessage = "$cleanName ($role) added by ${actor.account.displayName}."
            )
            loaded
        }
        dispatchSystemAlertIfEnabled(
            eventId = 2002 + created.account.id.toInt(),
            title = "New Account Added",
            message = "$cleanName ($role) added by ${actor.account.displayName}."
        )
        created
    }

    suspend fun updateStaffAccount(
        actor: AccountWithPermissions,
        targetAccountId: Long,
        displayName: String,
        isActive: Boolean,
        newPinOrNull: String?,
        permissions: List<StaffPermissionEntity>
    ): Result<AccountWithPermissions> = runCatching {
        if (actor.account.role != ManagementRole.MASTER_ADMIN) {
            throw SecurityException("Only Master Admin can modify staff accounts and permissions.")
        }
        val target = accountDao.findById(targetAccountId)
            ?: throw IllegalArgumentException("Target account not found.")
        if (target.role == ManagementRole.MASTER_ADMIN) {
            throw SecurityException("Master Admin credentials cannot be altered through staff management.")
        }
        val cleanName = displayName.trim()
        require(cleanName.isNotEmpty()) { "Display name is required." }

        var salt = target.pinSaltBase64
        var verifier = target.pinVerifierBase64
        if (!newPinOrNull.isNullOrBlank()) {
            require(PinSecurityManager.isValidPinFormat(newPinOrNull)) {
                "PIN must be at least 6 numeric digits."
            }
            salt = PinSecurityManager.generateSaltBase64()
            verifier = PinSecurityManager.deriveVerifierBase64(newPinOrNull, salt)
        }

        database.withTransaction {
            accountDao.updateAccount(
                target.copy(
                    displayName = cleanName,
                    isActive = isActive,
                    pinSaltBase64 = salt,
                    pinVerifierBase64 = verifier,
                    failedAttempts = 0,
                    lockoutUntilMillis = 0L,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            accountDao.replacePermissions(
                targetAccountId,
                permissions.map { it.copy(id = 0, accountId = targetAccountId) }
            )
            val refreshed = accountDao.findWithPermissionsById(targetAccountId)!!
            appendAuditAndNotify(
                actionType = "STAFF_UPDATED",
                actor = actor,
                sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                recordRef = target.username,
                details = "Updated account '$cleanName' (active=$isActive${if (!newPinOrNull.isNullOrBlank()) ", PIN reset" else ""}).",
                notifyTitle = "Staff Permissions Updated",
                notifyMessage = "${actor.account.displayName} updated $cleanName."
            )
            refreshed
        }
    }

    suspend fun deleteStaffAccount(
        actor: AccountWithPermissions,
        targetAccountId: Long
    ): Result<Unit> = runCatching {
        if (actor.account.role != ManagementRole.MASTER_ADMIN) {
            throw SecurityException("Only Master Admin can remove staff accounts.")
        }
        val target = accountDao.findById(targetAccountId)
            ?: throw IllegalArgumentException("Target account not found.")
        if (target.role == ManagementRole.MASTER_ADMIN) {
            throw SecurityException("Master Admin account cannot be deleted.")
        }
        database.withTransaction {
            accountDao.deleteNonMasterAccount(targetAccountId)
            appendAuditAndNotify(
                actionType = "STAFF_DELETED",
                actor = actor,
                sectionKey = ManagementSection.STAFF_MANAGEMENT.key,
                recordRef = target.username,
                details = "Removed account '${target.displayName}'.",
                notifyTitle = "Staff Account Removed",
                notifyMessage = "${target.displayName} was removed by ${actor.account.displayName}."
            )
        }
    }

    // ==================== CONTENT GOVERNANCE & WORKFLOW ====================

    suspend fun saveSchoolProfileSection(
        actor: AccountWithPermissions,
        section: ManagementSection,
        updatedProfile: SchoolProfileEntity,
        saveMode: PublishSaveMode,
        changeSummary: String
    ): Result<String> = runCatching {
        requirePermission(actor, section, PermissionAction.EDIT)
        if (saveMode == PublishSaveMode.PUBLISH_NOW) {
            requirePermission(actor, section, PermissionAction.PUBLISH)
        }

        val outcome = database.withTransaction {
            val existing = database.schoolProfileDao().getProfileOnce() ?: ChevalierDatabase.verifiedDefaultProfile()
            val prevJson = ContentSnapshotSerializer.schoolProfileToJson(existing)
            val nextSnapshot = mergeSchoolProfileSectionFields(existing, updatedProfile, section)
                .copy(id = 1, updatedAtMillis = System.currentTimeMillis())
            val nextJson = ContentSnapshotSerializer.schoolProfileToJson(nextSnapshot)

            when (saveMode) {
                PublishSaveMode.PUBLISH_NOW -> {
                    database.schoolProfileDao().upsertProfile(nextSnapshot)
                    insertVersionRow(
                        section = section,
                        recordId = 1L,
                        recordTitle = nextSnapshot.shortName,
                        previousJson = prevJson,
                        nextJson = nextJson,
                        actor = actor,
                        summary = changeSummary.ifBlank { "Updated ${section.key}" },
                        workflowStatus = ContentWorkflowStatus.PUBLISHED
                    )
                    appendAuditAndNotify(
                        actionType = "CONTENT_PUBLISHED",
                        actor = actor,
                        sectionKey = section.key,
                        recordRef = "profile_1",
                        details = "Published update to ${section.key}: $changeSummary",
                        notifyTitle = "Profile Updated",
                        notifyMessage = "${actor.account.displayName} published changes to ${section.key}."
                    )
                    "Published changes to public school profile."
                }
                PublishSaveMode.SUBMIT_FOR_APPROVAL -> {
                    // Keep live published profile untouched until approved
                    approvalDao.upsert(
                        ContentApprovalEntity(
                            sectionKey = section.key,
                            targetRecordId = 1L,
                            recordTitle = nextSnapshot.shortName,
                            previousSnapshotJson = prevJson,
                            proposedPayloadJson = nextJson,
                            changeSummary = changeSummary.ifBlank { "Proposed update to ${section.key}" },
                            submittedByAccountId = actor.account.id,
                            submittedByName = actor.account.displayName,
                            status = ContentWorkflowStatus.PENDING_APPROVAL
                        )
                    )
                    insertVersionRow(
                        section = section,
                        recordId = 1L,
                        recordTitle = nextSnapshot.shortName,
                        previousJson = prevJson,
                        nextJson = nextJson,
                        actor = actor,
                        summary = changeSummary.ifBlank { "Submitted ${section.key} for approval" },
                        workflowStatus = ContentWorkflowStatus.PENDING_APPROVAL
                    )
                    appendAuditAndNotify(
                        actionType = "SUBMITTED_FOR_APPROVAL",
                        actor = actor,
                        sectionKey = section.key,
                        recordRef = "profile_1",
                        details = "Submitted ${section.key} revision for approval (live profile preserved).",
                        notifyTitle = "Pending Approval",
                        notifyMessage = "${actor.account.displayName} submitted ${section.key} edits for review."
                    )
                    "Submitted for approval. Live public profile remains unchanged until approved."
                }
                PublishSaveMode.SAVE_DRAFT -> {
                    insertVersionRow(
                        section = section,
                        recordId = 1L,
                        recordTitle = nextSnapshot.shortName,
                        previousJson = prevJson,
                        nextJson = nextJson,
                        actor = actor,
                        summary = changeSummary.ifBlank { "Saved draft of ${section.key}" },
                        workflowStatus = ContentWorkflowStatus.DRAFT
                    )
                    logDao.appendLog(
                        ActivityLogEntity(
                            actionType = "DRAFT_SAVED",
                            actorAccountId = actor.account.id,
                            actorName = actor.account.displayName,
                            actorRole = actor.account.role,
                            sectionKey = section.key,
                            recordRef = "profile_1",
                            details = "Saved draft snapshot for ${section.key}."
                        )
                    )
                    "Draft snapshot saved."
                }
            }
        }
        if (saveMode == PublishSaveMode.SUBMIT_FOR_APPROVAL) {
            dispatchSystemAlertIfEnabled(
                eventId = section.ordinal + 3100,
                title = "Pending Approval",
                message = "${actor.account.displayName} submitted ${section.key} edits for review."
            )
        }
        outcome
    }

    suspend fun saveManagedContentRecord(
        actor: AccountWithPermissions,
        section: ManagementSection,
        recordId: Long,
        recordTitle: String,
        proposedJson: String,
        saveMode: PublishSaveMode,
        changeSummary: String
    ): Result<Long> = runCatching {
        val actionRequired = if (recordId == 0L) PermissionAction.CREATE else PermissionAction.EDIT
        requirePermission(actor, section, actionRequired)
        if (saveMode == PublishSaveMode.PUBLISH_NOW) {
            requirePermission(actor, section, PermissionAction.PUBLISH)
        }

        val isGalleryMedia = section == ManagementSection.PHOTO_GALLERY &&
            org.json.JSONObject(proposedJson).has("albumId")
        val summary = changeSummary.ifBlank {
            if (recordId == 0L) "Created $recordTitle" else "Updated $recordTitle"
        }

        val savedRecordId = database.withTransaction {
            val existingLive = if (recordId > 0L) {
                coordinator.fetchLiveRecordSnapshot(section, recordId, isGalleryMediaItem = isGalleryMedia)
            } else {
                null
            }

            when (saveMode) {
                PublishSaveMode.PUBLISH_NOW -> {
                    val savedId = coordinator.applySnapshotJsonToEntityTable(
                        section = section,
                        snapshotJson = proposedJson,
                        targetRecordId = recordId,
                        publishImmediately = true,
                        forcedStatus = ApprovalStatus.APPROVED
                    )
                    insertVersionRow(
                        section = section,
                        recordId = savedId,
                        recordTitle = recordTitle,
                        previousJson = existingLive?.snapshotJson,
                        nextJson = proposedJson,
                        actor = actor,
                        summary = summary,
                        workflowStatus = ContentWorkflowStatus.PUBLISHED
                    )
                    appendAuditAndNotify(
                        actionType = "CONTENT_PUBLISHED",
                        actor = actor,
                        sectionKey = section.key,
                        recordRef = "$recordTitle#$savedId",
                        details = "Published '$recordTitle' in ${section.key}.",
                        notifyTitle = "Content Published",
                        notifyMessage = "${actor.account.displayName} published '$recordTitle'."
                    )
                    savedId
                }

                PublishSaveMode.SAVE_DRAFT -> {
                    if (existingLive != null && existingLive.isLivePublished) {
                        // Do NOT overwrite currently published live item; record draft in version history
                        insertVersionRow(
                            section = section,
                            recordId = existingLive.id,
                            recordTitle = recordTitle,
                            previousJson = existingLive.snapshotJson,
                            nextJson = proposedJson,
                            actor = actor,
                            summary = "Draft edit: $summary",
                            workflowStatus = ContentWorkflowStatus.DRAFT
                        )
                        logDao.appendLog(
                            ActivityLogEntity(
                                actionType = "DRAFT_SAVED",
                                actorAccountId = actor.account.id,
                                actorName = actor.account.displayName,
                                actorRole = actor.account.role,
                                sectionKey = section.key,
                                recordRef = "$recordTitle#${existingLive.id}",
                                details = "Saved draft revision while preserving live published item."
                            )
                        )
                        existingLive.id
                    } else {
                        val savedId = coordinator.applySnapshotJsonToEntityTable(
                            section = section,
                            snapshotJson = proposedJson,
                            targetRecordId = recordId,
                            publishImmediately = false,
                            forcedStatus = ApprovalStatus.DRAFT
                        )
                        insertVersionRow(
                            section = section,
                            recordId = savedId,
                            recordTitle = recordTitle,
                            previousJson = existingLive?.snapshotJson,
                            nextJson = proposedJson,
                            actor = actor,
                            summary = summary,
                            workflowStatus = ContentWorkflowStatus.DRAFT
                        )
                        logDao.appendLog(
                            ActivityLogEntity(
                                actionType = "DRAFT_SAVED",
                                actorAccountId = actor.account.id,
                                actorName = actor.account.displayName,
                                actorRole = actor.account.role,
                                sectionKey = section.key,
                                recordRef = "$recordTitle#$savedId",
                                details = "Saved draft '$recordTitle'."
                            )
                        )
                        savedId
                    }
                }

                PublishSaveMode.SUBMIT_FOR_APPROVAL -> {
                    val targetId = if (existingLive != null && existingLive.isLivePublished) {
                        // Live published version stays intact publicly! Stage revision in approval queue.
                        existingLive.id
                    } else {
                        coordinator.applySnapshotJsonToEntityTable(
                            section = section,
                            snapshotJson = proposedJson,
                            targetRecordId = recordId,
                            publishImmediately = false,
                            forcedStatus = ApprovalStatus.PENDING
                        )
                    }

                    approvalDao.upsert(
                        ContentApprovalEntity(
                            sectionKey = section.key,
                            targetRecordId = targetId,
                            recordTitle = recordTitle,
                            previousSnapshotJson = existingLive?.snapshotJson,
                            proposedPayloadJson = proposedJson,
                            changeSummary = summary,
                            submittedByAccountId = actor.account.id,
                            submittedByName = actor.account.displayName,
                            status = ContentWorkflowStatus.PENDING_APPROVAL
                        )
                    )
                    insertVersionRow(
                        section = section,
                        recordId = targetId,
                        recordTitle = recordTitle,
                        previousJson = existingLive?.snapshotJson,
                        nextJson = proposedJson,
                        actor = actor,
                        summary = summary,
                        workflowStatus = ContentWorkflowStatus.PENDING_APPROVAL
                    )
                    appendAuditAndNotify(
                        actionType = "SUBMITTED_FOR_APPROVAL",
                        actor = actor,
                        sectionKey = section.key,
                        recordRef = "$recordTitle#$targetId",
                        details = "Submitted '$recordTitle' in ${section.key} for approval.",
                        notifyTitle = "Approval Requested",
                        notifyMessage = "${actor.account.displayName} submitted '$recordTitle' for approval."
                    )
                    targetId
                }
            }
        }
        if (saveMode == PublishSaveMode.SUBMIT_FOR_APPROVAL) {
            dispatchSystemAlertIfEnabled(
                eventId = 3200 + savedRecordId.toInt(),
                title = "Approval Requested",
                message = "${actor.account.displayName} submitted '$recordTitle' for approval."
            )
        }
        savedRecordId
    }

    suspend fun approveSubmission(
        actor: AccountWithPermissions,
        approvalId: Long,
        publishImmediately: Boolean = true
    ): Result<Long> = runCatching {
        val (approvalTitle, finalId) = database.withTransaction {
            val approval = approvalDao.getById(approvalId)
                ?: throw IllegalArgumentException("Approval item not found.")
            val section = ManagementSection.fromKey(approval.sectionKey)
                ?: throw IllegalArgumentException("Unknown section ${approval.sectionKey}.")
            requirePermission(actor, section, PermissionAction.PUBLISH)

            val savedId = coordinator.applySnapshotJsonToEntityTable(
                section = section,
                snapshotJson = approval.proposedPayloadJson,
                targetRecordId = approval.targetRecordId,
                publishImmediately = publishImmediately,
                forcedStatus = if (publishImmediately) ApprovalStatus.APPROVED else ContentWorkflowStatus.APPROVED
            )

            approvalDao.update(
                approval.copy(
                    targetRecordId = savedId,
                    status = if (publishImmediately) ContentWorkflowStatus.PUBLISHED else ContentWorkflowStatus.APPROVED,
                    reviewedByAccountId = actor.account.id,
                    reviewedByName = actor.account.displayName,
                    reviewedAtMillis = System.currentTimeMillis(),
                    rejectionReason = null
                )
            )
            insertVersionRow(
                section = section,
                recordId = savedId,
                recordTitle = approval.recordTitle,
                previousJson = approval.previousSnapshotJson,
                nextJson = approval.proposedPayloadJson,
                actor = actor,
                summary = if (publishImmediately) "Approved & Published: ${approval.changeSummary}" else "Approved (Unpublished): ${approval.changeSummary}",
                workflowStatus = if (publishImmediately) ContentWorkflowStatus.PUBLISHED else ContentWorkflowStatus.APPROVED
            )
            appendAuditAndNotify(
                actionType = if (publishImmediately) "APPROVAL_PUBLISHED" else "APPROVED",
                actor = actor,
                sectionKey = section.key,
                recordRef = "${approval.recordTitle}#$savedId",
                details = "${actor.account.displayName} approved submission '${approval.recordTitle}' (published=$publishImmediately).",
                notifyTitle = "Submission Approved",
                notifyMessage = "'${approval.recordTitle}' was approved by ${actor.account.displayName}."
            )
            approval.recordTitle to savedId
        }
        dispatchSystemAlertIfEnabled(
            eventId = 3300 + approvalId.toInt(),
            title = "Submission Approved",
            message = "'$approvalTitle' was approved by ${actor.account.displayName}."
        )
        finalId
    }

    suspend fun rejectSubmission(
        actor: AccountWithPermissions,
        approvalId: Long,
        reason: String
    ): Result<Unit> = runCatching {
        val cleanReason = reason.trim()
        require(cleanReason.isNotEmpty()) { "Rejection reason is required." }
        val rejectedTitle = database.withTransaction {
            val approval = approvalDao.getById(approvalId)
                ?: throw IllegalArgumentException("Approval item not found.")
            val section = ManagementSection.fromKey(approval.sectionKey)
                ?: throw IllegalArgumentException("Unknown section ${approval.sectionKey}.")
            requirePermission(actor, section, PermissionAction.PUBLISH)

            val isGalleryMedia = section == ManagementSection.PHOTO_GALLERY &&
                org.json.JSONObject(approval.proposedPayloadJson).has("albumId")
            val existingLive = if (approval.targetRecordId > 0L) {
                coordinator.fetchLiveRecordSnapshot(section, approval.targetRecordId, isGalleryMediaItem = isGalleryMedia)
            } else null

            // If target record is NOT live published, mark it REJECTED in entity table; never touch already-live records!
            if (existingLive != null && !existingLive.isLivePublished) {
                coordinator.applySnapshotJsonToEntityTable(
                    section = section,
                    snapshotJson = existingLive.snapshotJson,
                    targetRecordId = existingLive.id,
                    publishImmediately = false,
                    forcedStatus = ApprovalStatus.REJECTED
                )
            }

            approvalDao.update(
                approval.copy(
                    status = ContentWorkflowStatus.REJECTED,
                    reviewedByAccountId = actor.account.id,
                    reviewedByName = actor.account.displayName,
                    reviewedAtMillis = System.currentTimeMillis(),
                    rejectionReason = cleanReason
                )
            )
            insertVersionRow(
                section = section,
                recordId = approval.targetRecordId,
                recordTitle = approval.recordTitle,
                previousJson = approval.previousSnapshotJson,
                nextJson = approval.proposedPayloadJson,
                actor = actor,
                summary = "Rejected: $cleanReason",
                workflowStatus = ContentWorkflowStatus.REJECTED
            )
            appendAuditAndNotify(
                actionType = "REJECTED",
                actor = actor,
                sectionKey = section.key,
                recordRef = "${approval.recordTitle}#${approval.targetRecordId}",
                details = "Rejected '${approval.recordTitle}': $cleanReason",
                notifyTitle = "Submission Rejected",
                notifyMessage = "'${approval.recordTitle}' was rejected: $cleanReason"
            )
            approval.recordTitle
        }
        dispatchSystemAlertIfEnabled(
            eventId = 3400 + approvalId.toInt(),
            title = "Submission Rejected",
            message = "'$rejectedTitle' was rejected: $cleanReason"
        )
    }

    suspend fun unpublishRecord(
        actor: AccountWithPermissions,
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean = false
    ): Result<Unit> = runCatching {
        requirePermission(actor, section, PermissionAction.PUBLISH)
        database.withTransaction {
            val live = coordinator.fetchLiveRecordSnapshot(section, recordId, isGalleryMediaItem)
                ?: throw IllegalArgumentException("Record not found.")
            coordinator.applySnapshotJsonToEntityTable(
                section = section,
                snapshotJson = live.snapshotJson,
                targetRecordId = recordId,
                publishImmediately = false,
                forcedStatus = ContentWorkflowStatus.UNPUBLISHED
            )
            insertVersionRow(
                section = section,
                recordId = recordId,
                recordTitle = live.title,
                previousJson = live.snapshotJson,
                nextJson = live.snapshotJson,
                actor = actor,
                summary = "Unpublished from public view",
                workflowStatus = ContentWorkflowStatus.UNPUBLISHED
            )
            appendAuditAndNotify(
                actionType = "UNPUBLISHED",
                actor = actor,
                sectionKey = section.key,
                recordRef = "${live.title}#$recordId",
                details = "Unpublished '${live.title}' from public app.",
                notifyTitle = "Content Unpublished",
                notifyMessage = "'${live.title}' is now hidden from public view."
            )
        }
    }

    suspend fun publishExistingRecord(
        actor: AccountWithPermissions,
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean = false
    ): Result<Unit> = runCatching {
        requirePermission(actor, section, PermissionAction.PUBLISH)
        database.withTransaction {
            val live = coordinator.fetchLiveRecordSnapshot(section, recordId, isGalleryMediaItem)
                ?: throw IllegalArgumentException("Record not found.")
            coordinator.applySnapshotJsonToEntityTable(
                section = section,
                snapshotJson = live.snapshotJson,
                targetRecordId = recordId,
                publishImmediately = true,
                forcedStatus = ApprovalStatus.APPROVED
            )
            insertVersionRow(
                section = section,
                recordId = recordId,
                recordTitle = live.title,
                previousJson = live.snapshotJson,
                nextJson = live.snapshotJson,
                actor = actor,
                summary = "Published to public view",
                workflowStatus = ContentWorkflowStatus.PUBLISHED
            )
            appendAuditAndNotify(
                actionType = "CONTENT_PUBLISHED",
                actor = actor,
                sectionKey = section.key,
                recordRef = "${live.title}#$recordId",
                details = "Published '${live.title}' to public view.",
                notifyTitle = "Content Published",
                notifyMessage = "'${live.title}' is now live on the public app."
            )
        }
    }

    suspend fun archiveSchoolPost(
        actor: AccountWithPermissions,
        postId: Long
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val existing = database.schoolPostDao().getById(postId)
                ?: throw IllegalArgumentException("School post #$postId not found.")
            if (existing.status == com.example.data.local.entity.SchoolPostStatus.PUBLISHED) {
                requirePermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.PUBLISH)
            } else {
                requirePermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.EDIT)
            }
            val now = System.currentTimeMillis()
            val previousJson = ContentSnapshotSerializer.schoolPostToJson(existing)
            val archived = existing.copy(
                status = com.example.data.local.entity.SchoolPostStatus.ARCHIVED,
                isFeatured = false,
                updatedAtMillis = now
            )
            database.schoolPostDao().upsert(archived)
            val nextJson = ContentSnapshotSerializer.schoolPostToJson(archived)
            val label = existing.title.ifBlank { "School Post #$postId" }
            insertVersionRow(
                section = ManagementSection.SCHOOL_POSTS,
                recordId = postId,
                recordTitle = label,
                previousJson = previousJson,
                nextJson = nextJson,
                actor = actor,
                summary = "Archived post",
                workflowStatus = com.example.data.local.entity.SchoolPostStatus.ARCHIVED
            )
            appendAuditAndNotify(
                actionType = "POST_ARCHIVED",
                actor = actor,
                sectionKey = ManagementSection.SCHOOL_POSTS.key,
                recordRef = "$label#$postId",
                details = "Archived school post '$label'.",
                notifyTitle = "Post Archived",
                notifyMessage = "${actor.account.displayName} archived '$label'."
            )
        }
    }

    suspend fun restoreArchivedSchoolPost(
        actor: AccountWithPermissions,
        postId: Long
    ): Result<Unit> = runCatching {
        requirePermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.EDIT)
        database.withTransaction {
            val existing = database.schoolPostDao().getById(postId)
                ?: throw IllegalArgumentException("School post #$postId not found.")
            require(existing.status == com.example.data.local.entity.SchoolPostStatus.ARCHIVED) {
                "Only archived posts can be restored."
            }
            val now = System.currentTimeMillis()
            val previousJson = ContentSnapshotSerializer.schoolPostToJson(existing)
            val restored = existing.copy(
                status = com.example.data.local.entity.SchoolPostStatus.UNPUBLISHED,
                updatedAtMillis = now
            )
            database.schoolPostDao().upsert(restored)
            val nextJson = ContentSnapshotSerializer.schoolPostToJson(restored)
            val label = existing.title.ifBlank { "School Post #$postId" }
            insertVersionRow(
                section = ManagementSection.SCHOOL_POSTS,
                recordId = postId,
                recordTitle = label,
                previousJson = previousJson,
                nextJson = nextJson,
                actor = actor,
                summary = "Restored post from archive (unpublished)",
                workflowStatus = ContentWorkflowStatus.UNPUBLISHED
            )
            appendAuditAndNotify(
                actionType = "POST_RESTORED",
                actor = actor,
                sectionKey = ManagementSection.SCHOOL_POSTS.key,
                recordRef = "$label#$postId",
                details = "Restored school post '$label' from archive as unpublished.",
                notifyTitle = "Post Restored",
                notifyMessage = "${actor.account.displayName} restored '$label' from archive."
            )
        }
    }

    suspend fun setSchoolPostFeatured(
        actor: AccountWithPermissions,
        postId: Long,
        isFeatured: Boolean
    ): Result<Unit> = runCatching {
        requirePermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.PUBLISH)
        database.withTransaction {
            val existing = database.schoolPostDao().getById(postId)
                ?: throw IllegalArgumentException("School post #$postId not found.")
            if (isFeatured) {
                require(existing.status != com.example.data.local.entity.SchoolPostStatus.ARCHIVED) {
                    "Archived posts cannot be pinned. Restore the post first."
                }
            }
            val now = System.currentTimeMillis()
            val previousJson = ContentSnapshotSerializer.schoolPostToJson(existing)
            val updated = existing.copy(isFeatured = isFeatured, updatedAtMillis = now)
            database.schoolPostDao().upsert(updated)
            val nextJson = ContentSnapshotSerializer.schoolPostToJson(updated)
            val label = existing.title.ifBlank { "School Post #$postId" }
            insertVersionRow(
                section = ManagementSection.SCHOOL_POSTS,
                recordId = postId,
                recordTitle = label,
                previousJson = previousJson,
                nextJson = nextJson,
                actor = actor,
                summary = if (isFeatured) "Pinned/Featured post" else "Unpinned post",
                workflowStatus = existing.status
            )
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = if (isFeatured) "POST_PINNED" else "POST_UNPINNED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.SCHOOL_POSTS.key,
                    recordRef = "$label#$postId",
                    details = if (isFeatured) "Pinned '$label' as featured post." else "Unpinned '$label'."
                )
            )
        }
    }

    suspend fun deleteRecord(
        actor: AccountWithPermissions,
        section: ManagementSection,
        recordId: Long,
        recordTitle: String,
        isGalleryMediaItem: Boolean = false
    ): Result<Unit> = runCatching {
        requirePermission(actor, section, PermissionAction.DELETE)
        database.withTransaction {
            val childMediaIds = if (section == ManagementSection.PHOTO_GALLERY && !isGalleryMediaItem) {
                database.galleryDao().getMediaIdsForAlbumOnce(recordId)
            } else {
                emptyList()
            }
            coordinator.deleteRecordFromEntityTable(section, recordId, isGalleryMediaItem)
            val reviewedAt = System.currentTimeMillis()
            val pendingForTarget = approvalDao.getPendingForRecordOnce(section.key, recordId)
                .filter { approval ->
                    if (section != ManagementSection.PHOTO_GALLERY) {
                        true
                    } else {
                        val isMediaApproval = runCatching {
                            org.json.JSONObject(approval.proposedPayloadJson).has("albumId")
                        }.getOrDefault(false)
                        isMediaApproval == isGalleryMediaItem
                    }
                }
            val pendingForChildren = childMediaIds.flatMap { mediaId ->
                approvalDao.getPendingForRecordOnce(ManagementSection.PHOTO_GALLERY.key, mediaId)
                    .filter { approval ->
                        runCatching {
                            org.json.JSONObject(approval.proposedPayloadJson).has("albumId")
                        }.getOrDefault(false)
                    }
            }
            (pendingForTarget + pendingForChildren).distinctBy { it.id }.forEach { pending ->
                approvalDao.update(
                    pending.copy(
                        status = ContentWorkflowStatus.DRAFT_DISCARDED,
                        reviewedByAccountId = actor.account.id,
                        reviewedByName = actor.account.displayName,
                        reviewedAtMillis = reviewedAt,
                        rejectionReason = "Cancelled: target record deleted"
                    )
                )
            }
            appendAuditAndNotify(
                actionType = "CONTENT_DELETED",
                actor = actor,
                sectionKey = section.key,
                recordRef = "$recordTitle#$recordId",
                details = "Deleted '$recordTitle' from ${section.key}.",
                notifyTitle = "Record Deleted",
                notifyMessage = "${actor.account.displayName} deleted '$recordTitle'."
            )
        }
    }

    suspend fun restoreVersion(
        actor: AccountWithPermissions,
        versionId: Long,
        publishImmediately: Boolean = false
    ): Result<Long> = runCatching {
        val version = versionDao.getAllOnce().firstOrNull { it.id == versionId }
            ?: throw IllegalArgumentException("Version record not found.")
        val section = ManagementSection.fromKey(version.sectionKey)
            ?: throw IllegalArgumentException("Unknown section ${version.sectionKey}.")
        requirePermission(actor, section, PermissionAction.EDIT)
        if (publishImmediately) {
            requirePermission(actor, section, PermissionAction.PUBLISH)
        }

        val mode = if (publishImmediately) PublishSaveMode.PUBLISH_NOW else PublishSaveMode.SUBMIT_FOR_APPROVAL
        if (section == ManagementSection.SCHOOL_PROFILE ||
            section == ManagementSection.ABOUT_SCHOOL ||
            section == ManagementSection.CONTACT_DETAILS
        ) {
            val restoredProfile = ContentSnapshotSerializer.schoolProfileFromJson(version.contentSnapshotJson)
            saveSchoolProfileSection(
                actor = actor,
                section = section,
                updatedProfile = restoredProfile,
                saveMode = mode,
                changeSummary = "Restored from v${version.versionNumber}"
            ).getOrThrow()
            1L
        } else {
            saveManagedContentRecord(
                actor = actor,
                section = section,
                recordId = version.recordId,
                recordTitle = version.recordTitle,
                proposedJson = version.contentSnapshotJson,
                saveMode = mode,
                changeSummary = "Restored from v${version.versionNumber}"
            ).getOrThrow()
        }
    }

    suspend fun resolveEditableRevision(
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean = false
    ): EditableRecordResolution {
        val isProfileSingleton = section == ManagementSection.SCHOOL_PROFILE ||
            section == ManagementSection.ABOUT_SCHOOL ||
            section == ManagementSection.CONTACT_DETAILS
        val targetId = if (isProfileSingleton) 1L else recordId

        if (!isProfileSingleton && targetId <= 0L) {
            return EditableRecordResolution(
                section = section,
                recordId = targetId,
                liveSnapshotJson = null,
                editableSnapshotJson = null,
                revisionKind = ResumedRevisionKind.NONE,
                hasLivePublishedVersion = false
            )
        }

        val liveInfo = if (isProfileSingleton) {
            val profile = database.schoolProfileDao().getProfileOnce() ?: ChevalierDatabase.verifiedDefaultProfile()
            ManagementContentCoordinator.LiveRecordInfo(
                id = 1L,
                title = profile.shortName,
                snapshotJson = ContentSnapshotSerializer.schoolProfileToJson(profile),
                isLivePublished = true
            )
        } else {
            coordinator.fetchLiveRecordSnapshot(section, targetId, isGalleryMediaItem)
        }

        val latestVer = getLatestVersionForRecord(section, targetId, isGalleryMediaItem)
        val pendingApproval = getLatestPendingApprovalForRecord(section, targetId, isGalleryMediaItem)

        return when {
            latestVer != null && latestVer.approvalStatus == ContentWorkflowStatus.DRAFT -> {
                EditableRecordResolution(
                    section = section,
                    recordId = targetId,
                    liveSnapshotJson = liveInfo?.snapshotJson,
                    editableSnapshotJson = latestVer.contentSnapshotJson,
                    revisionKind = ResumedRevisionKind.DRAFT,
                    versionNumber = latestVer.versionNumber,
                    revisionAuthor = latestVer.changedByName,
                    revisionTimestampMillis = latestVer.changedAtMillis,
                    hasLivePublishedVersion = liveInfo?.isLivePublished == true
                )
            }
            latestVer != null && latestVer.approvalStatus == ContentWorkflowStatus.PENDING_APPROVAL -> {
                EditableRecordResolution(
                    section = section,
                    recordId = targetId,
                    liveSnapshotJson = liveInfo?.snapshotJson,
                    editableSnapshotJson = pendingApproval?.proposedPayloadJson ?: latestVer.contentSnapshotJson,
                    revisionKind = ResumedRevisionKind.PENDING_APPROVAL,
                    versionNumber = latestVer.versionNumber,
                    revisionAuthor = pendingApproval?.submittedByName ?: latestVer.changedByName,
                    revisionTimestampMillis = pendingApproval?.submittedAtMillis ?: latestVer.changedAtMillis,
                    hasLivePublishedVersion = liveInfo?.isLivePublished == true
                )
            }
            latestVer == null && pendingApproval != null -> {
                EditableRecordResolution(
                    section = section,
                    recordId = targetId,
                    liveSnapshotJson = liveInfo?.snapshotJson,
                    editableSnapshotJson = pendingApproval.proposedPayloadJson,
                    revisionKind = ResumedRevisionKind.PENDING_APPROVAL,
                    versionNumber = null,
                    revisionAuthor = pendingApproval.submittedByName,
                    revisionTimestampMillis = pendingApproval.submittedAtMillis,
                    hasLivePublishedVersion = liveInfo?.isLivePublished == true
                )
            }
            latestVer == null && liveInfo != null && !liveInfo.isLivePublished -> {
                val rawStatus = runCatching {
                    org.json.JSONObject(liveInfo.snapshotJson).optString("approvalStatus", "")
                }.getOrDefault("")
                val fallbackKind = when (rawStatus) {
                    ApprovalStatus.DRAFT -> ResumedRevisionKind.DRAFT
                    ApprovalStatus.PENDING -> ResumedRevisionKind.PENDING_APPROVAL
                    else -> ResumedRevisionKind.NONE
                }
                EditableRecordResolution(
                    section = section,
                    recordId = targetId,
                    liveSnapshotJson = liveInfo.snapshotJson,
                    editableSnapshotJson = liveInfo.snapshotJson,
                    revisionKind = fallbackKind,
                    hasLivePublishedVersion = false
                )
            }
            else -> {
                EditableRecordResolution(
                    section = section,
                    recordId = targetId,
                    liveSnapshotJson = liveInfo?.snapshotJson,
                    editableSnapshotJson = liveInfo?.snapshotJson,
                    revisionKind = ResumedRevisionKind.NONE,
                    hasLivePublishedVersion = liveInfo?.isLivePublished == true
                )
            }
        }
    }

    suspend fun resetDraftRevisionToLive(
        actor: AccountWithPermissions,
        section: ManagementSection,
        recordId: Long,
        recordTitle: String,
        isGalleryMediaItem: Boolean = false
    ): Result<String?> = runCatching {
        requirePermission(actor, section, PermissionAction.EDIT)
        val isProfileSingleton = section == ManagementSection.SCHOOL_PROFILE ||
            section == ManagementSection.ABOUT_SCHOOL ||
            section == ManagementSection.CONTACT_DETAILS
        val targetId = if (isProfileSingleton) 1L else recordId
        val liveJson = database.withTransaction {
            val liveInfo = if (isProfileSingleton) {
                val profile = database.schoolProfileDao().getProfileOnce() ?: ChevalierDatabase.verifiedDefaultProfile()
                ManagementContentCoordinator.LiveRecordInfo(
                    id = 1L,
                    title = profile.shortName,
                    snapshotJson = ContentSnapshotSerializer.schoolProfileToJson(profile),
                    isLivePublished = true
                )
            } else {
                coordinator.fetchLiveRecordSnapshot(section, targetId, isGalleryMediaItem)
            }

            val resolvedLiveJson = liveInfo?.snapshotJson
            val latestVer = getLatestVersionForRecord(section, targetId, isGalleryMediaItem)
            if (latestVer != null &&
                latestVer.approvalStatus == ContentWorkflowStatus.DRAFT &&
                resolvedLiveJson != null &&
                liveInfo.isLivePublished
            ) {
                insertVersionRow(
                    section = section,
                    recordId = targetId,
                    recordTitle = recordTitle.ifBlank { liveInfo.title },
                    previousJson = latestVer.contentSnapshotJson,
                    nextJson = resolvedLiveJson,
                    actor = actor,
                    summary = "Reset draft revision to live version",
                    workflowStatus = ContentWorkflowStatus.DRAFT_DISCARDED
                )
                logDao.appendLog(
                    ActivityLogEntity(
                        actionType = "DRAFT_DISCARDED",
                        actorAccountId = actor.account.id,
                        actorName = actor.account.displayName,
                        actorRole = actor.account.role,
                        sectionKey = section.key,
                        recordRef = "${recordTitle.ifBlank { liveInfo.title }}#$targetId",
                        details = "Discarded draft revision and reverted editor to live published version."
                    )
                )
            }
            resolvedLiveJson
        }
        liveJson
    }

    // ==================== MEDIA STUDIO ====================

    suspend fun importImageToMediaStudio(
        actor: AccountWithPermissions,
        sourceUri: Uri,
        title: String,
        caption: String?,
        categoryOrAlbum: String,
        assignedSections: List<String>,
        isFeatured: Boolean
    ): Result<MediaStudioItemEntity> = runCatching {
        requirePermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.CREATE)
        val cleanTitle = title.trim().ifEmpty { "Campus Image" }
        val processed = mediaStudioManager.importAndOptimizeImage(sourceUri).getOrThrow()
        try {
            database.withTransaction {
                val item = MediaStudioItemEntity(
                    mediaKind = "IMAGE",
                    title = cleanTitle,
                    caption = caption?.trim()?.takeIf { it.isNotEmpty() },
                    categoryOrAlbum = categoryOrAlbum.trim().ifEmpty { "Campus" },
                    originalUri = processed.originalUri,
                    optimizedUri = processed.optimizedUri,
                    mimeType = processed.mimeType,
                    widthPx = processed.widthPx,
                    heightPx = processed.heightPx,
                    fileSizeBytes = processed.originalSizeBytes,
                    optimizedSizeBytes = processed.optimizedSizeBytes,
                    assignedSectionsCsv = assignedSections.joinToString(","),
                    isFeatured = isFeatured,
                    approvalStatus = if (hasPermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.PUBLISH)) {
                        ContentWorkflowStatus.PUBLISHED
                    } else {
                        ContentWorkflowStatus.PENDING_APPROVAL
                    },
                    createdByName = actor.account.displayName
                )
                val savedId = mediaDao.upsert(item)
                val saved = item.copy(id = savedId)
                logDao.appendLog(
                    ActivityLogEntity(
                        actionType = "MEDIA_IMPORTED",
                        actorAccountId = actor.account.id,
                        actorName = actor.account.displayName,
                        actorRole = actor.account.role,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        recordRef = "$cleanTitle#$savedId",
                        details = "Imported ${processed.mimeType} (${processed.widthPx}x${processed.heightPx}, orig=${processed.originalSizeBytes}B, opt=${processed.optimizedSizeBytes}B)."
                    )
                )
                saved
            }
        } catch (t: Throwable) {
            mediaStudioManager.deleteManagedMediaFiles(processed.originalUri, processed.optimizedUri)
            throw t
        }
    }

    suspend fun addYouTubeVideoToMediaStudio(
        actor: AccountWithPermissions,
        youtubeUrl: String,
        title: String,
        caption: String?,
        categoryOrAlbum: String,
        isFeatured: Boolean
    ): Result<MediaStudioItemEntity> = runCatching {
        requirePermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.CREATE)
        val cleanUrl = youtubeUrl.trim()
        require(MediaStudioManager.isValidYouTubeUrl(cleanUrl)) {
            "Enter a valid YouTube video URL (youtube.com or youtu.be)."
        }
        val cleanTitle = title.trim().ifEmpty { "School Video" }
        database.withTransaction {
            val item = MediaStudioItemEntity(
                mediaKind = "YOUTUBE",
                title = cleanTitle,
                caption = caption?.trim()?.takeIf { it.isNotEmpty() },
                categoryOrAlbum = categoryOrAlbum.trim().ifEmpty { "Videos" },
                originalUri = cleanUrl,
                optimizedUri = cleanUrl,
                mimeType = "video/youtube",
                assignedSectionsCsv = ManagementSection.PHOTO_GALLERY.key,
                isFeatured = isFeatured,
                approvalStatus = ContentWorkflowStatus.PUBLISHED,
                createdByName = actor.account.displayName
            )
            val id = mediaDao.upsert(item)
            val saved = item.copy(id = id)
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = "VIDEO_LINK_ADDED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.MEDIA_STUDIO.key,
                    recordRef = "$cleanTitle#$id",
                    details = "Added YouTube media reference '$cleanUrl'."
                )
            )
            saved
        }
    }

    suspend fun replaceMediaImage(
        actor: AccountWithPermissions,
        mediaId: Long,
        newUri: Uri
    ): Result<MediaStudioItemEntity> = runCatching {
        requirePermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.EDIT)
        val existing = mediaDao.getById(mediaId) ?: throw IllegalArgumentException("Media item not found.")
        val processed = mediaStudioManager.importAndOptimizeImage(newUri).getOrThrow()
        try {
            database.withTransaction {
                val updated = existing.copy(
                    originalUri = processed.originalUri,
                    optimizedUri = processed.optimizedUri,
                    mimeType = processed.mimeType,
                    widthPx = processed.widthPx,
                    heightPx = processed.heightPx,
                    fileSizeBytes = processed.originalSizeBytes,
                    optimizedSizeBytes = processed.optimizedSizeBytes
                )
                mediaDao.upsert(updated)
                logDao.appendLog(
                    ActivityLogEntity(
                        actionType = "MEDIA_REPLACED",
                        actorAccountId = actor.account.id,
                        actorName = actor.account.displayName,
                        actorRole = actor.account.role,
                        sectionKey = ManagementSection.MEDIA_STUDIO.key,
                        recordRef = "${updated.title}#$mediaId",
                        details = "Replaced media image with optimized ${processed.widthPx}x${processed.heightPx} asset."
                    )
                )
                updated
            }
        } catch (t: Throwable) {
            mediaStudioManager.deleteManagedMediaFiles(processed.originalUri, processed.optimizedUri)
            throw t
        }
    }

    suspend fun updateMediaItemMetadata(
        actor: AccountWithPermissions,
        mediaId: Long,
        title: String,
        caption: String?,
        categoryOrAlbum: String
    ): Result<MediaStudioItemEntity> = runCatching {
        requirePermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.EDIT)
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Media title is required." }
        val cleanCategory = categoryOrAlbum.trim()
        require(cleanCategory.isNotEmpty()) { "Media category is required." }
        val cleanCaption = caption?.trim()?.takeIf { it.isNotEmpty() }

        database.withTransaction {
            val existing = mediaDao.getById(mediaId)
                ?: throw IllegalArgumentException("Media item not found.")
            val updated = existing.copy(
                title = cleanTitle,
                caption = cleanCaption,
                categoryOrAlbum = cleanCategory
            )
            mediaDao.upsert(updated)
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = "MEDIA_METADATA_UPDATED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.MEDIA_STUDIO.key,
                    recordRef = "${updated.title}#$mediaId",
                    details = "Updated metadata (title='${updated.title}', category='${updated.categoryOrAlbum}')."
                )
            )
            updated
        }
    }

    suspend fun assignMediaToHeroBanner(
        actor: AccountWithPermissions,
        mediaId: Long,
        captionOverride: String?,
        saveMode: PublishSaveMode,
        adjustedUriOverride: String? = null
    ): Result<Long> = runCatching {
        database.withTransaction {
            val media = mediaDao.getById(mediaId) ?: throw IllegalArgumentException("Media item not found.")
            val displayUri = adjustedUriOverride?.takeIf { it.isNotBlank() } ?: media.optimizedUri ?: media.originalUri
            val hero = CampusHeroPhotoEntity(
                imageUri = displayUri,
                caption = captionOverride?.trim()?.takeIf { it.isNotEmpty() } ?: media.caption ?: media.title
            )
            val id = saveManagedContentRecord(
                actor = actor,
                section = ManagementSection.LOGO_BANNER,
                recordId = 0L,
                recordTitle = hero.caption ?: media.title,
                proposedJson = ContentSnapshotSerializer.heroPhotoToJson(hero),
                saveMode = saveMode,
                changeSummary = "Assigned media '${media.title}' to Campus Hero Banner"
            ).getOrThrow()
            val updatedCsv = (media.assignedSectionsCsv.split(",").filter { it.isNotBlank() } + ManagementSection.LOGO_BANNER.key)
                .distinct().joinToString(",")
            mediaDao.upsert(media.copy(assignedSectionsCsv = updatedCsv))
            id
        }
    }

    suspend fun assignMediaToGalleryAlbum(
        actor: AccountWithPermissions,
        mediaId: Long,
        albumId: Long,
        saveMode: PublishSaveMode
    ): Result<Long> = runCatching {
        database.withTransaction {
            val media = mediaDao.getById(mediaId) ?: throw IllegalArgumentException("Media item not found.")
            val displayUri = media.optimizedUri ?: media.originalUri
            val entry = GalleryMediaEntity(
                albumId = albumId,
                title = media.title,
                caption = media.caption,
                mediaUri = displayUri,
                mediaType = if (media.mediaKind == "YOUTUBE") "YOUTUBE" else "IMAGE"
            )
            val id = saveManagedContentRecord(
                actor = actor,
                section = ManagementSection.PHOTO_GALLERY,
                recordId = 0L,
                recordTitle = media.title,
                proposedJson = ContentSnapshotSerializer.galleryMediaToJson(entry),
                saveMode = saveMode,
                changeSummary = "Added '${media.title}' to gallery album #$albumId"
            ).getOrThrow()
            val updatedCsv = (media.assignedSectionsCsv.split(",").filter { it.isNotBlank() } + ManagementSection.PHOTO_GALLERY.key)
                .distinct().joinToString(",")
            mediaDao.upsert(media.copy(assignedSectionsCsv = updatedCsv))
            id
        }
    }

    suspend fun setGalleryAlbumCover(
        actor: AccountWithPermissions,
        albumId: Long,
        coverMediaUri: String?
    ): Result<PublishSaveMode> = runCatching {
        requirePermission(actor, ManagementSection.PHOTO_GALLERY, PermissionAction.EDIT)
        val album = database.galleryDao().observeAlbumsWithMedia().first()
            .firstOrNull { it.album.id == albumId }?.album
            ?: throw IllegalArgumentException("Gallery album not found.")

        val cleanCover = coverMediaUri?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanCover != null) {
            require(mediaStudioManager.isManagedMediaFileReadable(cleanCover)) {
                "Selected cover image file is unavailable or unreadable."
            }
        }

        val updatedAlbum = album.copy(coverMediaUri = cleanCover)
        val canPublish = hasPermission(actor, ManagementSection.PHOTO_GALLERY, PermissionAction.PUBLISH)
        val mode = when {
            !canPublish -> PublishSaveMode.SUBMIT_FOR_APPROVAL
            album.isPublished && album.isApproved && album.approvalStatus == ApprovalStatus.APPROVED -> PublishSaveMode.PUBLISH_NOW
            else -> PublishSaveMode.SAVE_DRAFT
        }
        saveManagedContentRecord(
            actor = actor,
            section = ManagementSection.PHOTO_GALLERY,
            recordId = album.id,
            recordTitle = album.title,
            proposedJson = ContentSnapshotSerializer.galleryAlbumToJson(updatedAlbum),
            saveMode = mode,
            changeSummary = if (cleanCover != null) {
                "Set album cover for '${album.title}'"
            } else {
                "Cleared album cover for '${album.title}'"
            }
        ).getOrThrow()
        mode
    }

    suspend fun swapGalleryMediaOrder(
        actor: AccountWithPermissions,
        albumId: Long,
        mediaId: Long,
        moveUp: Boolean
    ): Result<PublishSaveMode> = runCatching {
        requirePermission(actor, ManagementSection.PHOTO_GALLERY, PermissionAction.EDIT)
        database.withTransaction {
            val sorted = database.galleryDao().observeAllMedia().first()
                .filter { it.albumId == albumId }
                .sortedWith(
                    compareBy<GalleryMediaEntity> { it.displayOrder }
                        .thenByDescending { it.addedAtMillis }
                        .thenBy { it.id }
                )
                .toMutableList()
            val index = sorted.indexOfFirst { it.id == mediaId }
            require(index >= 0) { "Gallery item not found in album." }
            val targetIndex = if (moveUp) index - 1 else index + 1
            require(targetIndex in sorted.indices) { "Cannot reorder item past album boundary." }

            val movingItem = sorted[index]
            sorted[index] = sorted[targetIndex]
            sorted[targetIndex] = movingItem

            val canPublish = hasPermission(actor, ManagementSection.PHOTO_GALLERY, PermissionAction.PUBLISH)
            if (canPublish) {
                sorted.forEachIndexed { idx, entry ->
                    if (entry.displayOrder != idx) {
                        database.galleryDao().upsertMedia(entry.copy(displayOrder = idx))
                    }
                }
                val title = movingItem.title ?: "Gallery Item #${movingItem.id}"
                appendAuditAndNotify(
                    actionType = "GALLERY_MEDIA_REORDERED",
                    actor = actor,
                    sectionKey = ManagementSection.PHOTO_GALLERY.key,
                    recordRef = "$title#${movingItem.id}",
                    details = "Reordered '$title' in album #$albumId to position ${targetIndex + 1}.",
                    notifyTitle = "Gallery Reordered",
                    notifyMessage = "${actor.account.displayName} reordered '$title' to position ${targetIndex + 1}."
                )
                PublishSaveMode.PUBLISH_NOW
            } else {
                val proposedMoved = movingItem.copy(displayOrder = targetIndex)
                val title = movingItem.title ?: "Gallery Item #${movingItem.id}"
                saveManagedContentRecord(
                    actor = actor,
                    section = ManagementSection.PHOTO_GALLERY,
                    recordId = movingItem.id,
                    recordTitle = title,
                    proposedJson = ContentSnapshotSerializer.galleryMediaToJson(proposedMoved),
                    saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
                    changeSummary = "Move '$title' to display order ${targetIndex + 1}"
                ).getOrThrow()
                PublishSaveMode.SUBMIT_FOR_APPROVAL
            }
        }
    }

    suspend fun getMediaUsageWarnings(media: MediaStudioItemEntity): List<String> {
        val uris = listOfNotNull(media.originalUri, media.optimizedUri).toSet()
        val warnings = mutableListOf<String>()
        val heroMatches = database.campusHeroPhotoDao().observeAllForManagement().first()
            .count { it.imageUri in uris }
        if (heroMatches > 0) warnings += "Campus Hero Banner ($heroMatches)"

        val galleryMatches = database.galleryDao().observeAllMedia().first()
            .count { it.mediaUri in uris }
        if (galleryMatches > 0) warnings += "Photo Gallery ($galleryMatches)"

        val coverMatches = database.galleryDao().observeAlbumsWithMedia().first()
            .count { it.album.coverMediaUri in uris }
        if (coverMatches > 0) warnings += "Album Cover ($coverMatches)"

        if (media.assignedSectionsCsv.isNotBlank() && warnings.isEmpty()) {
            warnings += media.assignedSectionsCsv.split(",").filter { it.isNotBlank() }
        }
        return warnings
    }

    suspend fun deleteMediaItem(actor: AccountWithPermissions, mediaId: Long): Result<Unit> = runCatching {
        requirePermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.DELETE)
        val item = database.withTransaction {
            val found = mediaDao.getById(mediaId) ?: throw IllegalArgumentException("Media item not found.")
            mediaDao.deleteById(mediaId)
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = "MEDIA_DELETED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.MEDIA_STUDIO.key,
                    recordRef = "${found.title}#$mediaId",
                    details = "Deleted media studio asset '${found.title}'."
                )
            )
            found
        }
        if (item.mediaKind == "IMAGE") {
            mediaStudioManager.deleteManagedMediaFiles(item.originalUri, item.optimizedUri)
        }
    }

    // ==================== NOTIFICATIONS & BACKUP AUDIT ====================

    suspend fun markNotificationRead(actor: AccountWithPermissions, id: Long): Result<Unit> = runCatching {
        requirePermission(actor, ManagementSection.NOTIFICATIONS, PermissionAction.VIEW)
        notificationDao.markRead(id)
    }

    suspend fun markAllNotificationsRead(actor: AccountWithPermissions): Result<Unit> = runCatching {
        requirePermission(actor, ManagementSection.NOTIFICATIONS, PermissionAction.EDIT)
        notificationDao.markAllRead()
    }

    suspend fun clearNotifications(actor: AccountWithPermissions): Result<Unit> = runCatching {
        requirePermission(actor, ManagementSection.NOTIFICATIONS, PermissionAction.DELETE)
        database.withTransaction {
            notificationDao.clearAll()
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = "NOTIFICATIONS_CLEARED",
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.NOTIFICATIONS.key,
                    recordRef = "LocalInbox",
                    details = "Cleared management notification inbox."
                )
            )
        }
    }

    suspend fun recordBackupCreated(actor: AccountWithPermissions, info: BackupPreviewInfo) {
        database.withTransaction {
            appendAuditAndNotify(
                actionType = "BACKUP_CREATED",
                actor = actor,
                sectionKey = ManagementSection.BACKUP_RESTORE.key,
                recordRef = "EncryptedBackup",
                details = "Created AES-256-GCM encrypted backup (${info.contentRecordCount} records, ${info.mediaFileCount} media files, schema v${info.schemaVersion}).",
                notifyTitle = "Encrypted Backup Created",
                notifyMessage = "Archive exported by ${actor.account.displayName} (${info.contentRecordCount} records, ${info.mediaFileCount} media files)."
            )
        }
        dispatchSystemAlertIfEnabled(
            eventId = 4001,
            title = "Encrypted Backup Created",
            message = "Archive exported cleanly (${info.contentRecordCount} records, ${info.mediaFileCount} media files)."
        )
    }

    suspend fun recordBackupOperationFailed(
        actor: AccountWithPermissions,
        actionType: String,
        safeSummary: String
    ) {
        database.withTransaction {
            logDao.appendLog(
                ActivityLogEntity(
                    actionType = actionType,
                    actorAccountId = actor.account.id,
                    actorName = actor.account.displayName,
                    actorRole = actor.account.role,
                    sectionKey = ManagementSection.BACKUP_RESTORE.key,
                    recordRef = "EncryptedBackup",
                    details = safeSummary
                )
            )
        }
    }

    // ==================== PERMISSION-FILTERED GLOBAL SEARCH ====================

    suspend fun searchManagement(
        actor: AccountWithPermissions,
        query: String
    ): List<ManagementSearchResult> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val results = mutableListOf<ManagementSearchResult>()
        var cachedProfile: SchoolProfileEntity? = null
        var profileLoaded = false

        suspend fun loadProfile(): SchoolProfileEntity? {
            if (!profileLoaded) {
                cachedProfile = database.schoolProfileDao().getProfileOnce()
                profileLoaded = true
            }
            return cachedProfile
        }

        for (section in SEARCHABLE_SECTIONS) {
            if (!hasPermission(actor, section, PermissionAction.VIEW)) continue
            when (section) {
                ManagementSection.SCHOOL_PROFILE -> {
                    val schoolProfile = loadProfile()
                    if (schoolProfile != null &&
                        listOfNotNull(
                            schoolProfile.schoolName,
                            schoolProfile.shortName,
                            schoolProfile.motto,
                            schoolProfile.affiliationNumber
                        ).any { it.lowercase().contains(q) }
                    ) {
                        results += ManagementSearchResult(
                            ManagementSection.SCHOOL_PROFILE, "School Profile", 1L,
                            schoolProfile.shortName, schoolProfile.schoolName, "Official"
                        )
                    }
                }

                ManagementSection.ABOUT_SCHOOL -> {
                    val schoolProfile = loadProfile()
                    if (schoolProfile != null &&
                        listOfNotNull(
                            schoolProfile.principalName,
                            schoolProfile.history,
                            schoolProfile.vision,
                            schoolProfile.mission
                        ).any { it.lowercase().contains(q) }
                    ) {
                        results += ManagementSearchResult(
                            ManagementSection.ABOUT_SCHOOL, "About School", 1L,
                            schoolProfile.principalName ?: "About School",
                            schoolProfile.vision ?: schoolProfile.history ?: "",
                            "Verified"
                        )
                    }
                }

                ManagementSection.ACADEMICS -> {
                    allAcademics.first().filter {
                        it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.description.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.ACADEMICS, "Academics", it.id, it.title, it.category, it.approvalStatus)
                    }
                }

                ManagementSection.HIGHER_SECONDARY_COURSES -> {
                    allCourses.first().filter {
                        it.streamTitle.lowercase().contains(q) || it.groupCode.lowercase().contains(q) || it.subjectsSummary.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.HIGHER_SECONDARY_COURSES, "Courses", it.id, "${it.groupCode} - ${it.streamTitle}", it.subjectsSummary, it.approvalStatus)
                    }
                }

                ManagementSection.FACILITIES -> {
                    allFacilities.first().filter {
                        it.name.lowercase().contains(q) || it.category.lowercase().contains(q) || it.description.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.FACILITIES, "Facilities", it.id, it.name, it.category, it.approvalStatus)
                    }
                }

                ManagementSection.TRANSPORT -> {
                    allTransportRoutes.first().filter {
                        it.routeNumber.lowercase().contains(q) || it.routeName.lowercase().contains(q) || it.stopsSummary.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.TRANSPORT, "Transport", it.id, "${it.routeNumber}: ${it.routeName}", it.stopsSummary, it.approvalStatus)
                    }
                }

                ManagementSection.NEWS -> {
                    allNews.first().filter {
                        it.title.lowercase().contains(q) || it.summary.lowercase().contains(q) || it.fullBody.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.NEWS, "News", it.id, it.title, it.summary, it.approvalStatus)
                    }
                }

                ManagementSection.ANNOUNCEMENTS -> {
                    allAnnouncements.first().filter {
                        it.title.lowercase().contains(q) || it.message.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.ANNOUNCEMENTS, "Announcements", it.id, it.title, it.message, it.approvalStatus)
                    }
                }

                ManagementSection.PHOTO_GALLERY -> {
                    allGalleryAlbumsWithMedia.first().filter {
                        it.album.title.lowercase().contains(q) || it.album.category.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.PHOTO_GALLERY, "Gallery", it.album.id, it.album.title, it.album.category, it.album.approvalStatus)
                    }
                }

                ManagementSection.MEDIA_STUDIO -> {
                    allMediaStudioItems.first().filter {
                        it.title.lowercase().contains(q) || (it.caption?.lowercase()?.contains(q) == true) || it.categoryOrAlbum.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.MEDIA_STUDIO, "Media Studio", it.id, it.title, it.mediaKind, it.approvalStatus)
                    }
                }

                ManagementSection.SCHOOL_POSTS -> {
                    allSchoolPosts.first().filter {
                        it.title.lowercase().contains(q) ||
                            it.description.lowercase().contains(q) ||
                            it.category.lowercase().contains(q) ||
                            (it.youtubeUrl?.lowercase()?.contains(q) == true)
                    }.forEach {
                        results += ManagementSearchResult(
                            ManagementSection.SCHOOL_POSTS,
                            "School Feed",
                            it.id,
                            it.title.ifBlank { "YouTube Video" },
                            "${it.category} • ${it.postType}",
                            it.status
                        )
                    }
                }

                ManagementSection.SCHOOL_HIGHLIGHTS -> {
                    allSchoolHighlights.first().filter {
                        it.title.lowercase().contains(q) || it.youtubeShortsUrl.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(
                            ManagementSection.SCHOOL_HIGHLIGHTS,
                            "Highlights",
                            it.id,
                            it.title.ifBlank { it.youtubeShortsUrl },
                            "24h YouTube Short",
                            it.status
                        )
                    }
                }

                ManagementSection.STAFF_MANAGEMENT -> {
                    accountsWithPermissions.first().filter {
                        it.account.displayName.lowercase().contains(q) || it.account.username.lowercase().contains(q) || it.account.role.lowercase().contains(q)
                    }.forEach {
                        results += ManagementSearchResult(ManagementSection.STAFF_MANAGEMENT, "Staff", it.account.id, it.account.displayName, "@${it.account.username} (${it.account.role})", if (it.account.isActive) "Active" else "Inactive")
                    }
                }

                else -> Unit
            }
        }

        return results
    }

    /**
     * Scoped Admin Dashboard search: searches ONLY authorized Admin Dashboard cards,
     * authorized management sections, and authorized administrative management records
     * (Staff, Approvals, Activity Logs, Admin Notifications, Version History, Media Studio).
     * Never exposes public Home feed content, public Academy catalog content, or private
     * Parent/student records.
     */
    suspend fun searchAdminDashboard(
        actor: AccountWithPermissions,
        query: String
    ): List<AdminDashboardSearchResult> {
        if (!actor.account.isActive) return emptyList()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val results = mutableListOf<AdminDashboardSearchResult>()

        // 1. Authorized Admin Dashboard Cards (exact 8 cards)
        AdminDashboardCard.entries.forEach { card ->
            if (canAccessAdminDashboardCard(actor, card)) {
                val matches = card.title.lowercase().contains(q) ||
                    card.keywords.any { it.contains(q) }
                if (matches) {
                    results += AdminDashboardSearchResult(
                        id = "card_${card.id}",
                        kind = AdminDashboardSearchKind.DASHBOARD_CARD,
                        title = card.title,
                        subtitle = "Admin Dashboard Section",
                        badge = "Section",
                        targetCard = card
                    )
                }
            }
        }

        // 2. Authorized Admin Management Sub-Sections
        if (canAccessApprovalCenter(actor) &&
            ("approval center".contains(q) || "approvals".contains(q) || "pending".contains(q))
        ) {
            results += AdminDashboardSearchResult(
                id = "tool_approval_center",
                kind = AdminDashboardSearchKind.MANAGEMENT_SECTION,
                title = "Approval Center",
                subtitle = "Review pending management submissions",
                badge = "Tool",
                targetToolKey = "APPROVAL_CENTER"
            )
        }
        if (canAccessVersionHistory(actor) &&
            ("version history".contains(q) || "revisions".contains(q) || "rollback".contains(q))
        ) {
            results += AdminDashboardSearchResult(
                id = "tool_version_history",
                kind = AdminDashboardSearchKind.MANAGEMENT_SECTION,
                title = "Version History",
                subtitle = "Inspect and rollback saved revisions",
                badge = "Tool",
                targetToolKey = "VERSION_HISTORY"
            )
        }

        val adminSections = listOf(
            Triple(ManagementSection.ACTIVITY_LOG, "Activity Log", "Audit administrative actions"),
            Triple(ManagementSection.BACKUP_RESTORE, "Backup & Restore", "Encrypted backup archive management"),
            Triple(ManagementSection.MEDIA_STUDIO, "Media Studio", "Managed school image & video assets"),
            Triple(ManagementSection.NOTIFICATIONS, "Notifications", "Administrative alerts and workflow notices"),
            Triple(ManagementSection.APP_SETTINGS, "App Settings", "Language, theme & local alert settings"),
            Triple(ManagementSection.SCHOOL_PROFILE, "School Profile", "Official school identity & metadata")
        )
        adminSections.forEach { (section, label, desc) ->
            if (hasPermission(actor, section, PermissionAction.VIEW) &&
                (label.lowercase().contains(q) || desc.lowercase().contains(q) || section.key.lowercase().contains(q))
            ) {
                results += AdminDashboardSearchResult(
                    id = "section_${section.key}",
                    kind = AdminDashboardSearchKind.MANAGEMENT_SECTION,
                    title = label,
                    subtitle = desc,
                    badge = "Tool",
                    targetSection = section
                )
            }
        }

        // 3. Authorized Management Records (strictly administrative — no public Home, Academy, or Parent/student data)
        if (hasPermission(actor, ManagementSection.STAFF_MANAGEMENT, PermissionAction.VIEW)) {
            accountsWithPermissions.first().filter {
                it.account.displayName.lowercase().contains(q) ||
                    it.account.username.lowercase().contains(q) ||
                    it.account.role.lowercase().contains(q)
            }.forEach { item ->
                results += AdminDashboardSearchResult(
                    id = "staff_${item.account.id}",
                    kind = AdminDashboardSearchKind.MANAGEMENT_RECORD,
                    title = item.account.displayName,
                    subtitle = "@${item.account.username} • ${item.account.role}",
                    badge = if (item.account.isActive) "Active Staff" else "Inactive",
                    targetSection = ManagementSection.STAFF_MANAGEMENT
                )
            }
        }

        if (canAccessApprovalCenter(actor)) {
            observeVisibleApprovals(actor).first().filter {
                it.recordTitle.lowercase().contains(q) ||
                    it.changeSummary.lowercase().contains(q) ||
                    it.submittedByName.lowercase().contains(q)
            }.take(15).forEach { approval ->
                results += AdminDashboardSearchResult(
                    id = "approval_${approval.id}",
                    kind = AdminDashboardSearchKind.MANAGEMENT_RECORD,
                    title = approval.recordTitle,
                    subtitle = "${approval.submittedByName} • ${approval.changeSummary}",
                    badge = approval.status,
                    targetToolKey = "APPROVAL_CENTER"
                )
            }
        }

        if (hasPermission(actor, ManagementSection.ACTIVITY_LOG, PermissionAction.VIEW)) {
            observeVisibleActivityLogs(actor).first().filter {
                it.actionType.lowercase().contains(q) ||
                    it.actorName.lowercase().contains(q) ||
                    it.details.lowercase().contains(q)
            }.take(15).forEach { log ->
                results += AdminDashboardSearchResult(
                    id = "log_${log.id}",
                    kind = AdminDashboardSearchKind.MANAGEMENT_RECORD,
                    title = log.actionType.replace('_', ' '),
                    subtitle = "${log.actorName}: ${log.details}",
                    badge = "Audit Log",
                    targetSection = ManagementSection.ACTIVITY_LOG
                )
            }
        }

        if (hasPermission(actor, ManagementSection.NOTIFICATIONS, PermissionAction.VIEW)) {
            observeVisibleNotifications(actor).first().filter {
                it.title.lowercase().contains(q) || it.message.lowercase().contains(q)
            }.take(15).forEach { note ->
                results += AdminDashboardSearchResult(
                    id = "note_${note.id}",
                    kind = AdminDashboardSearchKind.MANAGEMENT_RECORD,
                    title = note.title,
                    subtitle = note.message,
                    badge = if (note.isRead) "Read" else "Unread",
                    targetSection = ManagementSection.NOTIFICATIONS
                )
            }
        }

        if (hasPermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.VIEW)) {
            allMediaStudioItems.first().filter {
                it.title.lowercase().contains(q) ||
                    (it.caption?.lowercase()?.contains(q) == true) ||
                    it.categoryOrAlbum.lowercase().contains(q)
            }.take(15).forEach { media ->
                results += AdminDashboardSearchResult(
                    id = "media_${media.id}",
                    kind = AdminDashboardSearchKind.MANAGEMENT_RECORD,
                    title = media.title,
                    subtitle = "${media.mediaKind} • ${media.categoryOrAlbum}",
                    badge = media.approvalStatus,
                    targetSection = ManagementSection.MEDIA_STUDIO
                )
            }
        }

        return results
    }

    // ==================== INTERNAL AUDIT & VERSION HELPERS ====================

    suspend fun getVersionHistoryForRecord(
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean = false
    ): List<VersionHistoryEntity> {
        val rows = versionDao.getAllForRecordOnce(section.key, recordId)
        if (section != ManagementSection.PHOTO_GALLERY) return rows
        return rows.filter { matchesRecordEntityKind(section, isGalleryMediaItem, it.contentSnapshotJson) }
    }

    private fun isGalleryMediaPayload(
        section: ManagementSection,
        snapshotJson: String?
    ): Boolean {
        if (section != ManagementSection.PHOTO_GALLERY || snapshotJson.isNullOrBlank()) return false
        return runCatching { org.json.JSONObject(snapshotJson).has("albumId") }.getOrDefault(false)
    }

    private fun matchesRecordEntityKind(
        section: ManagementSection,
        isGalleryMediaItem: Boolean,
        snapshotJson: String?
    ): Boolean {
        if (section != ManagementSection.PHOTO_GALLERY) return true
        return isGalleryMediaPayload(section, snapshotJson) == isGalleryMediaItem
    }

    private suspend fun getLatestVersionForRecord(
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean
    ): VersionHistoryEntity? {
        if (section != ManagementSection.PHOTO_GALLERY) {
            return versionDao.getLatestForRecord(section.key, recordId)
        }
        return versionDao.getAllForRecordOnce(section.key, recordId)
            .firstOrNull { matchesRecordEntityKind(section, isGalleryMediaItem, it.contentSnapshotJson) }
    }

    private suspend fun getLatestPendingApprovalForRecord(
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean
    ): ContentApprovalEntity? {
        if (section != ManagementSection.PHOTO_GALLERY) {
            return approvalDao.getLatestPendingForRecord(section.key, recordId)
        }
        return approvalDao.getPendingForRecordOnce(section.key, recordId)
            .firstOrNull { matchesRecordEntityKind(section, isGalleryMediaItem, it.proposedPayloadJson) }
    }

    private suspend fun insertVersionRow(
        section: ManagementSection,
        recordId: Long,
        recordTitle: String,
        previousJson: String?,
        nextJson: String,
        actor: AccountWithPermissions,
        summary: String,
        workflowStatus: String
    ) {
        val nextVer = if (section == ManagementSection.PHOTO_GALLERY) {
            val isGalleryMedia = isGalleryMediaPayload(section, nextJson)
            val sameKindMax = versionDao.getAllForRecordOnce(section.key, recordId)
                .filter { matchesRecordEntityKind(section, isGalleryMedia, it.contentSnapshotJson) }
                .maxOfOrNull { it.versionNumber } ?: 0
            sameKindMax + 1
        } else {
            versionDao.getMaxVersionNumber(section.key, recordId) + 1
        }
        versionDao.insert(
            VersionHistoryEntity(
                sectionKey = section.key,
                recordId = recordId,
                versionNumber = nextVer,
                recordTitle = recordTitle,
                previousContentJson = previousJson,
                contentSnapshotJson = nextJson,
                changedByAccountId = actor.account.id,
                changedByName = actor.account.displayName,
                changeSummary = summary,
                approvalStatus = workflowStatus
            )
        )
    }

    private suspend fun appendAuditAndNotify(
        actionType: String,
        actor: AccountWithPermissions,
        sectionKey: String,
        recordRef: String,
        details: String,
        notifyTitle: String,
        notifyMessage: String
    ) {
        logDao.appendLog(
            ActivityLogEntity(
                actionType = actionType,
                actorAccountId = actor.account.id,
                actorName = actor.account.displayName,
                actorRole = actor.account.role,
                sectionKey = sectionKey,
                recordRef = recordRef,
                details = details
            )
        )
        notificationDao.insert(
            LocalNotificationEntity(
                eventType = actionType,
                title = notifyTitle,
                message = notifyMessage,
                sectionKey = sectionKey
            )
        )
    }

    private suspend fun dispatchSystemAlertIfEnabled(
        eventId: Int,
        title: String,
        message: String
    ) {
        val notifier = localAlertNotifier ?: return
        val prefsRepo = preferencesRepository ?: return
        val enabled = runCatching { prefsRepo.preferencesFlow.first().deviceAlertsEnabled }.getOrDefault(false)
        if (enabled) {
            notifier.notifyLocalEventIfPermitted(
                notificationId = eventId,
                title = title,
                message = message,
                alertsEnabled = true
            )
        }
    }

    companion object {
        val SEARCHABLE_SECTIONS: List<ManagementSection> = listOf(
            ManagementSection.SCHOOL_PROFILE,
            ManagementSection.ABOUT_SCHOOL,
            ManagementSection.ACADEMICS,
            ManagementSection.HIGHER_SECONDARY_COURSES,
            ManagementSection.FACILITIES,
            ManagementSection.TRANSPORT,
            ManagementSection.NEWS,
            ManagementSection.ANNOUNCEMENTS,
            ManagementSection.PHOTO_GALLERY,
            ManagementSection.MEDIA_STUDIO,
            ManagementSection.SCHOOL_POSTS,
            ManagementSection.SCHOOL_HIGHLIGHTS,
            ManagementSection.STAFF_MANAGEMENT
        )
    }
}
