package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backend.CentralBackendStatus
import com.example.data.backup.BackupPreviewInfo
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.ContentApprovalEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.LocalNotificationEntity
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.local.entity.VersionHistoryEntity
import com.example.data.preferences.AcademyHeroBannerConfig
import com.example.data.preferences.AdminHeroBannerConfig
import com.example.data.preferences.FeedVisibilityPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AdminDashboardCard
import com.example.data.repository.AdminDashboardSearchResult
import com.example.data.repository.ManagementRepository
import com.example.data.repository.ManagementSearchResult
import com.example.data.repository.PublishSaveMode
import com.example.data.v2.AcademicsV2AccessControl
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AcademicsV2ContentRepository
import com.example.data.v2.AcademicsV2RecordStatus
import com.example.data.v2.AcademicsV2Snapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ManagementAuthUiState {
    data object Loading : ManagementAuthUiState
    data class FirstTimeSetup(
        val generatedRecoveryCode: String? = null,
        val createdAccount: AccountWithPermissions? = null
    ) : ManagementAuthUiState
    data class LoginRequired(val noticeMessage: String? = null) : ManagementAuthUiState
    data class Authenticated(val session: AccountWithPermissions) : ManagementAuthUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ManagementViewModel(
    private val managementRepository: ManagementRepository,
    private val backupRestoreManager: BackupRestoreManager,
    private val preferencesRepository: UserPreferencesRepository? = null,
    val centralBackendStatus: CentralBackendStatus? = null,
    private val inactivityTimeoutMillis: Long = 5 * 60 * 1000L,
    private val academicsV2Repository: AcademicsV2ContentRepository? = null
) : ViewModel() {

    private val _authUiState = MutableStateFlow<ManagementAuthUiState>(ManagementAuthUiState.Loading)
    val authUiState: StateFlow<ManagementAuthUiState> = _authUiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ManagementSearchResult>>(emptyList())
    val searchResults: StateFlow<List<ManagementSearchResult>> = _searchResults.asStateFlow()

    private val _adminDashboardSearchResults = MutableStateFlow<List<AdminDashboardSearchResult>>(emptyList())
    val adminDashboardSearchResults: StateFlow<List<AdminDashboardSearchResult>> =
        _adminDashboardSearchResults.asStateFlow()

    private val _localHeroBannerOverride = MutableStateFlow<AdminHeroBannerConfig?>(null)
    private val _localAcademyHeroBannerOverride = MutableStateFlow<AcademyHeroBannerConfig?>(null)

    private val _backupPreview = MutableStateFlow<BackupPreviewInfo?>(null)
    val backupPreview: StateFlow<BackupPreviewInfo?> = _backupPreview.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val restoreInProgress = MutableStateFlow(false)
    private var lastActivityMillis: Long = System.currentTimeMillis()

    val masterAdminExists: StateFlow<Boolean> = managementRepository.masterAdminExists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val accountsWithPermissions: StateFlow<List<AccountWithPermissions>> = managementRepository.accountsWithPermissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val schoolProfile: StateFlow<SchoolProfileEntity> = managementRepository.schoolProfile
        .combine(MutableStateFlow(Unit)) { p, _ -> p ?: ChevalierDatabase.verifiedDefaultProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChevalierDatabase.verifiedDefaultProfile())

    val allHeroPhotos: StateFlow<List<CampusHeroPhotoEntity>> = managementRepository.allHeroPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allAcademics: StateFlow<List<AcademicInfoEntity>> = managementRepository.allAcademics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCourses: StateFlow<List<HigherSecondaryCourseEntity>> = managementRepository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allFacilities: StateFlow<List<FacilityEntity>> = managementRepository.allFacilities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTransportRoutes: StateFlow<List<TransportRouteEntity>> = managementRepository.allTransportRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allNews: StateFlow<List<NewsItemEntity>> = managementRepository.allNews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allAnnouncements: StateFlow<List<AnnouncementEntity>> = managementRepository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allGalleryAlbumsWithMedia: StateFlow<List<GalleryAlbumWithMedia>> = managementRepository.allGalleryAlbumsWithMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allGalleryMedia: StateFlow<List<GalleryMediaEntity>> = managementRepository.allGalleryMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSocialLinks: StateFlow<List<SocialMediaLinkEntity>> = managementRepository.allSocialLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSchoolPosts: StateFlow<List<SchoolPostEntity>> = managementRepository.allSchoolPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSchoolHighlights: StateFlow<List<SchoolHighlightEntity>> = managementRepository.allSchoolHighlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allApprovals: StateFlow<List<ContentApprovalEntity>> = _authUiState
        .flatMapLatest { state ->
            when (state) {
                is ManagementAuthUiState.Authenticated -> managementRepository.observeVisibleApprovals(state.session)
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingApprovalsCount: StateFlow<Int> = _authUiState
        .flatMapLatest { state ->
            when (state) {
                is ManagementAuthUiState.Authenticated -> managementRepository.observeVisiblePendingApprovalsCount(state.session)
                else -> flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val allVersionHistory: StateFlow<List<VersionHistoryEntity>> = _authUiState
        .flatMapLatest { state ->
            when (state) {
                is ManagementAuthUiState.Authenticated -> managementRepository.observeVisibleVersionHistory(state.session)
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allMediaStudioItems: StateFlow<List<MediaStudioItemEntity>> = managementRepository.allMediaStudioItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allNotifications: StateFlow<List<LocalNotificationEntity>> = _authUiState
        .flatMapLatest { state ->
            when (state) {
                is ManagementAuthUiState.Authenticated -> managementRepository.observeVisibleNotifications(state.session)
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = _authUiState
        .flatMapLatest { state ->
            when (state) {
                is ManagementAuthUiState.Authenticated -> managementRepository.observeVisibleUnreadNotificationsCount(state.session)
                else -> flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val visibleActivityLogs: StateFlow<List<ActivityLogEntity>> = _authUiState
        .flatMapLatest { state ->
            when (state) {
                is ManagementAuthUiState.Authenticated -> managementRepository.observeVisibleActivityLogs(state.session)
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val deviceAlertsEnabled: StateFlow<Boolean> = (
        preferencesRepository?.preferencesFlow?.map { it.deviceAlertsEnabled } ?: flowOf(false)
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val adminHeroBanner: StateFlow<AdminHeroBannerConfig> = combine(
        preferencesRepository?.adminHeroBannerFlow ?: flowOf(AdminHeroBannerConfig()),
        _localHeroBannerOverride
    ) { persisted, override ->
        when {
            override == null -> persisted
            persisted.updatedAtMillis >= override.updatedAtMillis && persisted.updatedAtMillis > 0L -> persisted
            else -> override
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminHeroBannerConfig())

    val academyHeroBanner: StateFlow<AcademyHeroBannerConfig> = combine(
        preferencesRepository?.academyHeroBannerFlow ?: flowOf(AcademyHeroBannerConfig()),
        _localAcademyHeroBannerOverride
    ) { persisted, override ->
        when {
            override == null -> persisted
            persisted.updatedAtMillis >= override.updatedAtMillis && persisted.updatedAtMillis > 0L -> persisted
            else -> override
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AcademyHeroBannerConfig())

    val feedVisibility: StateFlow<FeedVisibilityPreferences> = (
        preferencesRepository?.feedVisibilityFlow ?: flowOf(FeedVisibilityPreferences())
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedVisibilityPreferences())

    val academicsV2Snapshot: StateFlow<AcademicsV2Snapshot> = (
        academicsV2Repository?.snapshot ?: flowOf(AcademicsV2Snapshot())
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AcademicsV2Snapshot())

    init {
        // Unified auth state synchronization: keeps setup, login, and active session consistent
        // without racing between masterAdminExists and accountsWithPermissions during restore.
        viewModelScope.launch {
            combine(
                managementRepository.masterAdminExists,
                managementRepository.accountsWithPermissions,
                restoreInProgress
            ) { exists, accounts, restoring ->
                Triple(exists, accounts, restoring)
            }.collect { (exists, accounts, restoring) ->
                if (restoring) return@collect
                val effectiveMasterAdminExists = exists || accounts.any {
                    it.account.role == ManagementRole.MASTER_ADMIN
                }
                val current = _authUiState.value
                when {
                    !effectiveMasterAdminExists -> {
                        if (current !is ManagementAuthUiState.FirstTimeSetup) {
                            _authUiState.value = ManagementAuthUiState.FirstTimeSetup()
                        }
                    }
                    current is ManagementAuthUiState.Loading -> {
                        _authUiState.value = ManagementAuthUiState.LoginRequired()
                    }
                    current is ManagementAuthUiState.Authenticated -> {
                        val refreshed = accounts.firstOrNull { it.account.id == current.session.account.id }
                        if (refreshed == null || !refreshed.account.isActive) {
                            _authUiState.value = ManagementAuthUiState.LoginRequired(
                                "Your management account was deactivated or removed."
                            )
                        } else if (refreshed != current.session) {
                            _authUiState.value = ManagementAuthUiState.Authenticated(refreshed)
                        }
                    }
                }
            }
        }

        // Inactivity monitor
        viewModelScope.launch {
            while (true) {
                delay(10_000L)
                checkInactivityLock(System.currentTimeMillis())
            }
        }
    }

    fun onUserInteraction(nowMillis: Long = System.currentTimeMillis()) {
        lastActivityMillis = nowMillis
    }

    fun checkInactivityLock(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val current = _authUiState.value
        if (current is ManagementAuthUiState.Authenticated &&
            (nowMillis - lastActivityMillis) >= inactivityTimeoutMillis
        ) {
            _authUiState.value = ManagementAuthUiState.LoginRequired("Session locked due to inactivity.")
            return true
        }
        return false
    }

    fun clearFeedback() {
        _errorMessage.value = null
        _statusMessage.value = null
    }

    fun clearBackupPreview() {
        _backupPreview.value = null
    }

    fun currentSession(): AccountWithPermissions? =
        (_authUiState.value as? ManagementAuthUiState.Authenticated)?.session

    fun hasPermission(section: ManagementSection, action: PermissionAction): Boolean {
        val session = currentSession() ?: return false
        return managementRepository.hasPermission(session, section, action)
    }

    fun isMasterAdmin(): Boolean =
        currentSession()?.account?.role == ManagementRole.MASTER_ADMIN

    fun setupMasterAdmin(displayName: String, username: String, pin: String, confirmPin: String) {
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val result = managementRepository.setupMasterAdmin(displayName, username, pin, confirmPin)
            _isBusy.value = false
            result.onSuccess { setup ->
                _authUiState.value = ManagementAuthUiState.FirstTimeSetup(
                    generatedRecoveryCode = setup.recoveryCode,
                    createdAccount = setup.account
                )
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Setup failed."
            }
        }
    }

    fun confirmRecoveryKeySavedAndEnterDashboard() {
        val state = _authUiState.value as? ManagementAuthUiState.FirstTimeSetup ?: return
        val account = state.createdAccount ?: return
        lastActivityMillis = System.currentTimeMillis()
        _authUiState.value = ManagementAuthUiState.Authenticated(account)
        _statusMessage.value = "Welcome, ${account.account.displayName}."
    }

    fun loginWithPin(username: String, pin: String) {
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val result = managementRepository.loginWithPin(username, pin)
            _isBusy.value = false
            result.onSuccess { account ->
                lastActivityMillis = System.currentTimeMillis()
                _authUiState.value = ManagementAuthUiState.Authenticated(account)
                _statusMessage.value = "Signed in as ${account.account.displayName}."
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Authentication failed."
            }
        }
    }

    fun recoverMasterPin(
        recoveryCode: String,
        newPin: String,
        confirmPin: String,
        onNewRecoveryCodeIssued: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val result = managementRepository.recoverMasterAdminPin(recoveryCode, newPin, confirmPin)
            _isBusy.value = false
            result.onSuccess { setup ->
                lastActivityMillis = System.currentTimeMillis()
                onNewRecoveryCodeIssued(setup.recoveryCode)
                _authUiState.value = ManagementAuthUiState.Authenticated(setup.account)
                _statusMessage.value = "Master PIN updated. Save your rotated recovery key."
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Recovery failed."
            }
        }
    }

    fun logout() {
        val actor = currentSession()
        viewModelScope.launch {
            if (actor != null) {
                managementRepository.recordLogout(actor)
            }
            _authUiState.value = ManagementAuthUiState.LoginRequired()
            _searchResults.value = emptyList()
            _backupPreview.value = null
            clearFeedback()
        }
    }

    // ==================== CONTENT GOVERNANCE WRAPPERS ====================

    fun saveProfileSection(
        section: ManagementSection,
        updatedProfile: SchoolProfileEntity,
        saveMode: PublishSaveMode,
        changeSummary: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.saveSchoolProfileSection(actor, section, updatedProfile, saveMode, changeSummary)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = it
                onSuccess?.invoke()
            }.onFailure { _errorMessage.value = it.message ?: "Failed to save profile changes." }
        }
    }

    suspend fun resolveEditableRevision(
        section: ManagementSection,
        recordId: Long,
        isGalleryMediaItem: Boolean = false
    ) = managementRepository.resolveEditableRevision(section, recordId, isGalleryMediaItem)

    fun resetRevisionToLive(
        section: ManagementSection,
        recordId: Long,
        recordTitle: String,
        isGalleryMediaItem: Boolean = false,
        onResetComplete: (String?) -> Unit
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.resetDraftRevisionToLive(
                actor = actor,
                section = section,
                recordId = recordId,
                recordTitle = recordTitle,
                isGalleryMediaItem = isGalleryMediaItem
            )
            _isBusy.value = false
            res.onSuccess { liveJson ->
                _statusMessage.value = "Reverted editor to live published version."
                onResetComplete(liveJson)
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Unable to reset to live version."
            }
        }
    }

    fun saveManagedRecord(
        section: ManagementSection,
        recordId: Long,
        recordTitle: String,
        proposedJson: String,
        saveMode: PublishSaveMode,
        changeSummary: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.saveManagedContentRecord(
                actor, section, recordId, recordTitle, proposedJson, saveMode, changeSummary
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = when (saveMode) {
                    PublishSaveMode.PUBLISH_NOW -> "Published '$recordTitle'."
                    PublishSaveMode.SUBMIT_FOR_APPROVAL -> "Submitted '$recordTitle' for approval."
                    PublishSaveMode.SAVE_DRAFT -> "Saved draft '$recordTitle'."
                }
                onSuccess?.invoke()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Operation failed."
            }
        }
    }

    fun approveSubmission(approvalId: Long, publishImmediately: Boolean = true) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.approveSubmission(actor, approvalId, publishImmediately)
                .onSuccess {
                    _statusMessage.value = if (publishImmediately) "Submission approved and published." else "Submission approved."
                }
                .onFailure { _errorMessage.value = it.message ?: "Approval failed." }
            _isBusy.value = false
        }
    }

    fun rejectSubmission(approvalId: Long, reason: String, onSuccess: () -> Unit = {}) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.rejectSubmission(actor, approvalId, reason)
                .onSuccess {
                    _statusMessage.value = "Submission rejected with reason."
                    onSuccess()
                }
                .onFailure { _errorMessage.value = it.message ?: "Rejection failed." }
            _isBusy.value = false
        }
    }

    fun togglePublishState(
        section: ManagementSection,
        recordId: Long,
        currentlyPublished: Boolean,
        isGalleryMediaItem: Boolean = false
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = if (currentlyPublished) {
                managementRepository.unpublishRecord(actor, section, recordId, isGalleryMediaItem)
            } else {
                managementRepository.publishExistingRecord(actor, section, recordId, isGalleryMediaItem)
            }
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = if (currentlyPublished) "Unpublished from public app." else "Published to public app."
            }.onFailure { _errorMessage.value = it.message ?: "Publish state change failed." }
        }
    }

    fun archiveSchoolPost(postId: Long, postTitle: String) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.archiveSchoolPost(actor, postId)
                .onSuccess { _statusMessage.value = "Archived '$postTitle'." }
                .onFailure { _errorMessage.value = it.message ?: "Failed to archive post." }
            _isBusy.value = false
        }
    }

    fun restoreArchivedSchoolPost(postId: Long, postTitle: String) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.restoreArchivedSchoolPost(actor, postId)
                .onSuccess { _statusMessage.value = "Restored '$postTitle' as unpublished." }
                .onFailure { _errorMessage.value = it.message ?: "Failed to restore post." }
            _isBusy.value = false
        }
    }

    fun setSchoolPostFeatured(postId: Long, isFeatured: Boolean, postTitle: String) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.setSchoolPostFeatured(actor, postId, isFeatured)
                .onSuccess {
                    _statusMessage.value = if (isFeatured) "Pinned '$postTitle'." else "Unpinned '$postTitle'."
                }
                .onFailure { _errorMessage.value = it.message ?: "Failed to update pin state." }
            _isBusy.value = false
        }
    }

    fun deleteRecord(
        section: ManagementSection,
        recordId: Long,
        recordTitle: String,
        isGalleryMediaItem: Boolean = false
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.deleteRecord(actor, section, recordId, recordTitle, isGalleryMediaItem)
                .onSuccess { _statusMessage.value = "Deleted '$recordTitle'." }
                .onFailure { _errorMessage.value = it.message ?: "Delete failed." }
            _isBusy.value = false
        }
    }

    fun restoreVersion(versionId: Long, publishImmediately: Boolean) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.restoreVersion(actor, versionId, publishImmediately)
                .onSuccess {
                    _statusMessage.value = if (publishImmediately) {
                        "Version restored and published."
                    } else {
                        "Version restored as revision for approval."
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "Restore failed." }
            _isBusy.value = false
        }
    }

    // ==================== STAFF ACCOUNTS ====================

    fun createStaffAccount(
        username: String,
        displayName: String,
        role: String,
        pin: String,
        permissions: List<StaffPermissionEntity>,
        onSuccess: () -> Unit
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.createStaffAccount(actor, username, displayName, role, pin, permissions)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Created account for $displayName."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Failed to create account." }
        }
    }

    fun updateStaffAccount(
        targetAccountId: Long,
        displayName: String,
        isActive: Boolean,
        newPinOrNull: String?,
        permissions: List<StaffPermissionEntity>,
        onSuccess: () -> Unit
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.updateStaffAccount(
                actor, targetAccountId, displayName, isActive, newPinOrNull, permissions
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Updated $displayName."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Update failed." }
        }
    }

    fun deleteStaffAccount(targetAccountId: Long, onSuccess: () -> Unit = {}) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.deleteStaffAccount(actor, targetAccountId)
                .onSuccess {
                    _statusMessage.value = "Staff account deleted."
                    onSuccess()
                }
                .onFailure { _errorMessage.value = it.message ?: "Delete failed." }
            _isBusy.value = false
        }
    }

    // ==================== MEDIA STUDIO ====================

    fun importImage(
        uri: Uri,
        title: String,
        caption: String?,
        categoryOrAlbum: String,
        assignedSections: List<String>,
        isFeatured: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.importImageToMediaStudio(
                actor, uri, title, caption, categoryOrAlbum, assignedSections, isFeatured
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Imported '${it.title}' cleanly (original & optimized preserved)."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Image import failed." }
        }
    }

    fun addYouTubeVideo(
        url: String,
        title: String,
        caption: String?,
        categoryOrAlbum: String,
        isFeatured: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.addYouTubeVideoToMediaStudio(
                actor, url, title, caption, categoryOrAlbum, isFeatured
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Added YouTube video '${it.title}'."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Invalid YouTube URL." }
        }
    }

    fun replaceMediaImage(mediaId: Long, newUri: Uri) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.replaceMediaImage(actor, mediaId, newUri)
                .onSuccess { _statusMessage.value = "Image replaced cleanly." }
                .onFailure { _errorMessage.value = it.message ?: "Replace failed." }
            _isBusy.value = false
        }
    }

    fun updateMediaMetadata(
        mediaId: Long,
        title: String,
        caption: String?,
        categoryOrAlbum: String,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = managementRepository.updateMediaItemMetadata(
                actor = actor,
                mediaId = mediaId,
                title = title,
                caption = caption,
                categoryOrAlbum = categoryOrAlbum
            )
            _isBusy.value = false
            res.onSuccess { updated ->
                _statusMessage.value = "Updated metadata for '${updated.title}'."
                onSuccess()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Metadata update failed."
            }
        }
    }

    fun assignMediaToHero(mediaId: Long, publishNow: Boolean, adjustedUriOverride: String? = null) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val mode = if (publishNow && hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.PUBLISH)) {
                PublishSaveMode.PUBLISH_NOW
            } else {
                PublishSaveMode.SUBMIT_FOR_APPROVAL
            }
            managementRepository.assignMediaToHeroBanner(actor, mediaId, null, mode, adjustedUriOverride)
                .onSuccess {
                    _statusMessage.value = if (mode == PublishSaveMode.PUBLISH_NOW) {
                        "Assigned media to Campus Hero Banner."
                    } else {
                        "Submitted media assignment to Campus Hero Banner for approval."
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "Assignment failed." }
            _isBusy.value = false
        }
    }

    fun assignMediaToAlbum(mediaId: Long, albumId: Long, publishNow: Boolean) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val mode = if (publishNow && hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.PUBLISH)) {
                PublishSaveMode.PUBLISH_NOW
            } else {
                PublishSaveMode.SUBMIT_FOR_APPROVAL
            }
            managementRepository.assignMediaToGalleryAlbum(actor, mediaId, albumId, mode)
                .onSuccess {
                    _statusMessage.value = if (mode == PublishSaveMode.PUBLISH_NOW) {
                        "Added media to selected Gallery album."
                    } else {
                        "Submitted gallery album media assignment for approval."
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "Album assignment failed." }
            _isBusy.value = false
        }
    }

    fun searchableSectionCount(): Int {
        val session = currentSession() ?: return ManagementRepository.SEARCHABLE_SECTIONS.size
        return ManagementRepository.SEARCHABLE_SECTIONS.count { section ->
            managementRepository.hasPermission(session, section, PermissionAction.VIEW)
        }
    }

    fun setGalleryAlbumCover(
        albumId: Long,
        coverMediaUri: String?,
        publishedMsg: String = "Updated album cover.",
        submittedMsg: String = "Album cover update submitted for approval."
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.setGalleryAlbumCover(actor, albumId, coverMediaUri)
                .onSuccess { mode ->
                    _statusMessage.value = if (mode == PublishSaveMode.SUBMIT_FOR_APPROVAL) {
                        submittedMsg
                    } else {
                        publishedMsg
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "Unable to update album cover." }
            _isBusy.value = false
        }
    }

    fun moveGalleryMediaOrder(
        albumId: Long,
        mediaId: Long,
        moveUp: Boolean,
        updatedMsg: String = "Updated photo order.",
        submittedMsg: String = "Photo order update submitted for approval."
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.swapGalleryMediaOrder(actor, albumId, mediaId, moveUp)
                .onSuccess { mode ->
                    _statusMessage.value = if (mode == PublishSaveMode.SUBMIT_FOR_APPROVAL) {
                        submittedMsg
                    } else {
                        updatedMsg
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "Unable to reorder photo." }
            _isBusy.value = false
        }
    }

    suspend fun getMediaUsageWarnings(item: MediaStudioItemEntity): List<String> =
        managementRepository.getMediaUsageWarnings(item)

    fun deleteMediaItem(mediaId: Long) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.deleteMediaItem(actor, mediaId)
                .onSuccess { _statusMessage.value = "Media asset deleted." }
                .onFailure { _errorMessage.value = it.message ?: "Delete failed." }
            _isBusy.value = false
        }
    }

    // ==================== NOTIFICATIONS & SEARCH ====================

    fun setDeviceAlertsEnabled(enabled: Boolean) {
        val prefs = preferencesRepository ?: return
        onUserInteraction()
        viewModelScope.launch {
            prefs.setDeviceAlertsEnabled(enabled)
        }
    }

    fun markNotificationRead(id: Long) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            managementRepository.markNotificationRead(actor, id)
                .onFailure { _errorMessage.value = it.message ?: "Permission denied for Notifications." }
        }
    }

    fun markAllNotificationsRead() {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            managementRepository.markAllNotificationsRead(actor)
                .onFailure { _errorMessage.value = it.message ?: "Permission denied to update notifications." }
        }
    }

    fun clearNotifications(onSuccess: () -> Unit = {}) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            managementRepository.clearNotifications(actor)
                .onSuccess {
                    _statusMessage.value = "Notifications cleared."
                    onSuccess()
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Permission denied to clear notifications."
                }
        }
    }

    fun performGlobalSearch(query: String) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _searchResults.value = managementRepository.searchManagement(actor, query)
        }
    }

    fun performAdminDashboardSearch(query: String) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            _adminDashboardSearchResults.value = managementRepository.searchAdminDashboard(actor, query)
        }
    }

    fun clearAdminDashboardSearch() {
        _adminDashboardSearchResults.value = emptyList()
    }

    fun canAccessAdminCard(card: AdminDashboardCard): Boolean {
        val actor = currentSession() ?: return false
        return managementRepository.canAccessAdminDashboardCard(actor, card)
    }

    fun canEditAdminHeroBanner(): Boolean {
        val actor = currentSession() ?: return false
        return managementRepository.canEditAdminHeroBanner(actor)
    }

    fun importAdminHeroImage(
        sourceUri: Uri,
        onImported: (String) -> Unit
    ) {
        val actor = currentSession() ?: return
        if (!managementRepository.canEditAdminHeroBanner(actor)) {
            _errorMessage.value = "Permission denied to update Admin Hero Banner image."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.importAdminHeroBackgroundImage(actor, sourceUri)
                .onSuccess { optimizedUri ->
                    onImported(optimizedUri)
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Unable to import selected image."
                }
            _isBusy.value = false
        }
    }

    fun saveAdminHeroBanner(
        titleLine1: String,
        welcomeLine2: String,
        subtitleLine3: String,
        backgroundImageUri: String?,
        onSaved: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        if (!managementRepository.canEditAdminHeroBanner(actor)) {
            _errorMessage.value = "Permission denied to edit Admin Hero Banner."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.saveAdminHeroBannerConfig(
                actor = actor,
                titleLine1 = titleLine1,
                welcomeLine2 = welcomeLine2,
                subtitleLine3 = subtitleLine3,
                backgroundImageUri = backgroundImageUri
            ).onSuccess { saved ->
                _localHeroBannerOverride.value = saved
                _statusMessage.value = "Admin Hero Banner updated."
                onSaved()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Unable to save Admin Hero Banner."
            }
            _isBusy.value = false
        }
    }

    fun canEditAcademyHeroBanner(): Boolean {
        val actor = currentSession() ?: return false
        return managementRepository.canEditAcademyHeroBanner(actor)
    }

    fun importAcademyHeroImage(
        sourceUri: Uri,
        onImported: (String) -> Unit
    ) {
        val actor = currentSession() ?: return
        if (!managementRepository.canEditAcademyHeroBanner(actor)) {
            _errorMessage.value = "Permission denied to update Academy Hero Banner image."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.importAcademyHeroBannerImage(actor, sourceUri)
                .onSuccess { optimizedUri ->
                    onImported(optimizedUri)
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Unable to import selected banner image."
                }
            _isBusy.value = false
        }
    }

    fun saveAcademyHeroBanner(
        imageUri: String?,
        overlayTitle: String,
        overlaySubtitle: String,
        publishNow: Boolean = true,
        onSaved: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        if (!managementRepository.canEditAcademyHeroBanner(actor)) {
            _errorMessage.value = "Permission denied to edit Academy Hero Banner."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            managementRepository.saveAcademyHeroBannerConfig(
                actor = actor,
                imageUri = imageUri,
                overlayTitle = overlayTitle,
                overlaySubtitle = overlaySubtitle,
                publishNow = publishNow
            ).onSuccess { saved ->
                _localAcademyHeroBannerOverride.value = saved
                _statusMessage.value = if (saved.isPublished) {
                    "Academy Hero Banner published."
                } else {
                    "Academy Hero Banner saved."
                }
                onSaved()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Unable to save Academy Hero Banner."
            }
            _isBusy.value = false
        }
    }

    fun updatePostFeedVisibility(
        postRemoteId: String,
        showOnHomeFeed: Boolean,
        showOnAcademyFeed: Boolean,
        onSaved: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        onUserInteraction()
        viewModelScope.launch {
            managementRepository.setPostFeedVisibility(
                actor = actor,
                postRemoteId = postRemoteId,
                showOnHomeFeed = showOnHomeFeed,
                showOnAcademyFeed = showOnAcademyFeed
            ).onSuccess {
                onSaved()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Unable to update post feed visibility."
            }
        }
    }

    // ==================== ENCRYPTED BACKUP & RESTORE ====================

    fun createBackupToUri(uri: Uri, password: String, confirmPassword: String) {
        val actor = currentSession() ?: return
        if (!hasPermission(ManagementSection.BACKUP_RESTORE, PermissionAction.VIEW)) {
            _errorMessage.value = "Permission denied for Backup & Restore."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Backup password must be at least 6 characters."
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Password and Confirm Password do not match."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = backupRestoreManager.createEncryptedBackupToUri(uri, password)
            res.onSuccess { info ->
                managementRepository.recordBackupCreated(actor, info)
                _isBusy.value = false
                _statusMessage.value =
                    "Encrypted backup saved (${info.contentRecordCount} records, ${info.mediaFileCount} media files)."
            }.onFailure { err ->
                managementRepository.recordBackupOperationFailed(
                    actor = actor,
                    actionType = "BACKUP_CREATE_FAILED",
                    safeSummary = "Encrypted backup export failed."
                )
                _isBusy.value = false
                _errorMessage.value = err.message ?: "Backup creation failed."
            }
        }
    }

    fun inspectBackupUri(uri: Uri, password: String) {
        val actor = currentSession() ?: return
        if (!hasPermission(ManagementSection.BACKUP_RESTORE, PermissionAction.VIEW)) {
            _errorMessage.value = "Permission denied for Backup & Restore."
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "Backup password is required to open archive."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = backupRestoreManager.inspectEncryptedBackupFromUri(uri, password)
            res.onSuccess { preview ->
                _isBusy.value = false
                _backupPreview.value = preview
            }.onFailure { err ->
                managementRepository.recordBackupOperationFailed(
                    actor = actor,
                    actionType = "BACKUP_INSPECT_FAILED",
                    safeSummary = "Encrypted backup verification/decryption failed."
                )
                _isBusy.value = false
                _backupPreview.value = null
                _errorMessage.value = err.message ?: "Unable to open encrypted backup."
            }
        }
    }

    fun confirmRestoreFromUri(uri: Uri, password: String) {
        val actor = currentSession() ?: return
        if (restoreInProgress.value) return
        if (!hasPermission(ManagementSection.BACKUP_RESTORE, PermissionAction.EDIT)) {
            _errorMessage.value = "Permission denied to restore backups."
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "Backup password is required to restore archive."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            restoreInProgress.value = true
            _isBusy.value = true
            clearFeedback()
            try {
                val res = backupRestoreManager.restoreEncryptedBackupFromUri(
                    uri = uri,
                    password = password,
                    actorName = actor.account.displayName,
                    actorAccountId = actor.account.id,
                    actorRole = actor.account.role
                )
                res.onSuccess { info ->
                    _isBusy.value = false
                    _backupPreview.value = null
                    _searchResults.value = emptyList()
                    clearFeedback()
                    _authUiState.value = ManagementAuthUiState.LoginRequired(
                        noticeMessage = "Backup restored (${info.contentRecordCount} records, ${info.mediaFileCount} media files). Please sign in again using the restored account credentials."
                    )
                }.onFailure { err ->
                    managementRepository.recordBackupOperationFailed(
                        actor = actor,
                        actionType = "BACKUP_RESTORE_FAILED",
                        safeSummary = "Encrypted backup restore failed; existing data preserved."
                    )
                    _isBusy.value = false
                    _errorMessage.value = err.message ?: "Restore failed; existing data was preserved."
                }
            } finally {
                restoreInProgress.value = false
            }
        }
    }

    // ==================== VERSION 2 ACADEMICS & OWNER CONTROL CENTER ====================

    fun saveV2Record(
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
        submitForApproval: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: run {
            _errorMessage.value = "Version 2 content store is not initialized."
            return
        }
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.saveRecord(
                actor = actor,
                existingId = existingId,
                category = category,
                sectionKey = sectionKey,
                title = title,
                description = description,
                imageUri = imageUri,
                attachmentUri = attachmentUri,
                attachmentLabel = attachmentLabel,
                publishDateText = publishDateText,
                expiryDateMillis = expiryDateMillis,
                submitForApproval = submitForApproval
            )
            _isBusy.value = false
            res.onSuccess { saved ->
                _statusMessage.value = "Saved '${saved.title}' as ${saved.status.displayLabel}."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Unable to save record." }
        }
    }

    fun submitV2RecordForApproval(recordId: String) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.submitRecordForOwnerApproval(actor, recordId)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Submitted '${it.title}' for School Owner final approval."
            }.onFailure { _errorMessage.value = it.message ?: "Unable to submit for approval." }
        }
    }

    fun publishV2Record(recordId: String, authorizeExamResult: Boolean = false) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.publishRecord(actor, recordId, authorizeExamResult = authorizeExamResult)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Published '${it.title}' to Public Academics."
            }.onFailure { _errorMessage.value = it.message ?: "Unable to publish record." }
        }
    }

    fun ownerApproveAndPublishV2Record(recordId: String, authorizeExamResult: Boolean = true) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.ownerApproveAndPublishRecord(actor, recordId, authorizeExamResult)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "School Owner approved and published '${it.title}'."
            }.onFailure { _errorMessage.value = it.message ?: "Approval failed." }
        }
    }

    fun updateV2RecordStatus(recordId: String, targetStatus: AcademicsV2RecordStatus) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.updateRecordStatus(actor, recordId, targetStatus)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Updated '${it.title}' to ${it.status.displayLabel}."
            }.onFailure { _errorMessage.value = it.message ?: "Status update failed." }
        }
    }

    fun deleteV2Record(recordId: String) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.deleteRecord(actor, recordId)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Deleted content record."
            }.onFailure { _errorMessage.value = it.message ?: "Delete failed." }
        }
    }

    fun saveV2GalleryAlbum(
        existingId: String?,
        sectionKey: String,
        title: String,
        description: String,
        dateText: String,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.saveGalleryAlbum(actor, existingId, sectionKey, title, description, dateText)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Saved photo album '${it.title}' as Draft (Unpublished)."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Failed to save album." }
        }
    }

    fun updateV2GalleryAlbumStatus(albumId: String, status: AcademicsV2RecordStatus) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.updateGalleryAlbumStatus(actor, albumId, status)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Album '${it.title}' is now ${it.status.displayLabel}."
            }.onFailure { _errorMessage.value = it.message ?: "Album status update failed." }
        }
    }

    fun deleteV2GalleryAlbum(albumId: String) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.deleteGalleryAlbum(actor, albumId)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Deleted photo album."
            }.onFailure { _errorMessage.value = it.message ?: "Delete album failed." }
        }
    }

    fun saveV2GalleryPhoto(
        existingId: String?,
        title: String,
        description: String,
        dateText: String,
        imageUri: String,
        albumIds: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.saveGalleryPhoto(actor, existingId, title, description, dateText, imageUri, albumIds)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Saved photo '${it.title}' to ${it.albumIds.size} album(s) as Draft."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Failed to save photo." }
        }
    }

    fun assignV2GalleryPhotoToAlbums(photoId: String, albumIds: List<String>) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.assignGalleryPhotoToAlbums(actor, photoId, albumIds)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Updated album reuse for '${it.title}' (${it.albumIds.size} albums)."
            }.onFailure { _errorMessage.value = it.message ?: "Failed to update album assignments." }
        }
    }

    fun updateV2GalleryPhotoStatus(photoId: String, status: AcademicsV2RecordStatus) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.updateGalleryPhotoStatus(actor, photoId, status)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Photo '${it.title}' is now ${it.status.displayLabel}."
            }.onFailure { _errorMessage.value = it.message ?: "Photo status update failed." }
        }
    }

    fun deleteV2GalleryPhoto(photoId: String) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.deleteGalleryPhoto(actor, photoId)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Deleted gallery photo."
            }.onFailure { _errorMessage.value = it.message ?: "Delete photo failed." }
        }
    }

    fun saveV2FacultyProfile(
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
        hasStaffConsent: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.saveFacultyProfile(
                actor = actor,
                existingId = existingId,
                sectionKey = sectionKey,
                teacherName = teacherName,
                approvedProfilePhotoUri = approvedProfilePhotoUri,
                designation = designation,
                department = department,
                teachingSubjects = teachingSubjects,
                educationalQualifications = educationalQualifications,
                teachingExperience = teachingExperience,
                professionalIntroduction = professionalIntroduction,
                isSchoolApproved = isSchoolApproved,
                hasStaffConsent = hasStaffConsent
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Saved staff profile '${it.teacherName}' as Draft (Unpublished)."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Failed to save staff profile." }
        }
    }

    fun updateV2FacultyProfileStatus(profileId: String, status: AcademicsV2RecordStatus) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.updateFacultyProfileStatus(actor, profileId, status)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Staff profile '${it.teacherName}' is now ${it.status.displayLabel}."
            }.onFailure { _errorMessage.value = it.message ?: "Unable to update staff profile status." }
        }
    }

    fun deleteV2FacultyProfile(profileId: String) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.deleteFacultyProfile(actor, profileId)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Deleted staff profile."
            }.onFailure { _errorMessage.value = it.message ?: "Delete failed." }
        }
    }

    fun updateOwnerPrivateProfile(
        ownerName: String,
        designation: String,
        privateContactPhone: String,
        privateContactEmail: String,
        governanceNote: String
    ) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.updateOwnerPrivateProfile(
                actor = actor,
                ownerName = ownerName,
                designation = designation,
                privateContactPhone = privateContactPhone,
                privateContactEmail = privateContactEmail,
                governanceNote = governanceNote
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Saved private School Owner profile (non-public)."
            }.onFailure { _errorMessage.value = it.message ?: "Unable to save owner profile." }
        }
    }

    fun submitStaffAccessRequest(
        requestedRole: String,
        requestedCategoryKeys: List<String>,
        reason: String,
        onSuccess: () -> Unit = {}
    ) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.submitStaffAccessRequest(actor, requestedRole, requestedCategoryKeys, reason)
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Submitted staff access request to School Owner."
                onSuccess()
            }.onFailure { _errorMessage.value = it.message ?: "Unable to submit request." }
        }
    }

    fun reviewStaffAccessRequest(
        requestId: String,
        approve: Boolean,
        reviewNote: String,
        grantViewEditPublish: Boolean = true
    ) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.reviewStaffAccessRequest(actor, requestId, approve, reviewNote)
            res.onSuccess { reviewed ->
                if (approve && grantViewEditPublish) {
                    val target = accountsWithPermissions.value.firstOrNull { it.account.id == reviewed.staffAccountId }
                    if (target != null) {
                        val existingMap = target.permissions.associateBy { it.sectionKey }.toMutableMap()
                        reviewed.requestedCategoryKeys.forEach { key ->
                            existingMap[key] = StaffPermissionEntity(
                                accountId = target.account.id,
                                sectionKey = key,
                                canView = true,
                                canCreate = true,
                                canEdit = true,
                                canDelete = false,
                                canPublish = false
                            )
                        }
                        managementRepository.updateStaffAccount(
                            actor = actor,
                            targetAccountId = target.account.id,
                            displayName = target.account.displayName,
                            isActive = true,
                            newPinOrNull = null,
                            permissions = existingMap.values.toList()
                        )
                    }
                }
                _isBusy.value = false
                _statusMessage.value = if (approve) {
                    "Approved staff access for ${reviewed.staffDisplayName}."
                } else {
                    "Rejected staff access request for ${reviewed.staffDisplayName}."
                }
            }.onFailure {
                _isBusy.value = false
                _errorMessage.value = it.message ?: "Failed to review access request."
            }
        }
    }

    fun revokeStaffAccessRequest(requestId: String, reviewNote: String) {
        val actor = currentSession() ?: return
        val repo = academicsV2Repository ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val res = repo.revokeStaffAccessRequest(actor, requestId, reviewNote)
            res.onSuccess { revoked ->
                val target = accountsWithPermissions.value.firstOrNull { it.account.id == revoked.staffAccountId }
                if (target != null) {
                    val remainingPermissions = target.permissions.filterNot { it.sectionKey in revoked.requestedCategoryKeys }
                    managementRepository.updateStaffAccount(
                        actor = actor,
                        targetAccountId = target.account.id,
                        displayName = target.account.displayName,
                        isActive = target.account.isActive,
                        newPinOrNull = null,
                        permissions = remainingPermissions
                    )
                }
                _isBusy.value = false
                _statusMessage.value = "Revoked staff access for ${revoked.staffDisplayName}."
            }.onFailure {
                _isBusy.value = false
                _errorMessage.value = it.message ?: "Unable to revoke access."
            }
        }
    }

    fun updateStaffV2CategoryPermission(
        targetAccountId: Long,
        category: AcademicsV2Category,
        canView: Boolean,
        canCreate: Boolean,
        canEdit: Boolean,
        canDelete: Boolean,
        canPublish: Boolean
    ) {
        val actor = currentSession() ?: return
        if (!AcademicsV2AccessControl.isSchoolOwner(actor) &&
            !AcademicsV2AccessControl.hasSectionPermission(actor, ManagementSection.STAFF_MANAGEMENT, PermissionAction.EDIT)
        ) {
            _errorMessage.value = "Permission denied to modify staff category permissions."
            return
        }
        val target = accountsWithPermissions.value.firstOrNull { it.account.id == targetAccountId } ?: return
        onUserInteraction()
        viewModelScope.launch {
            _isBusy.value = true
            clearFeedback()
            val updatedMap = target.permissions.associateBy { it.sectionKey }.toMutableMap()
            val anyGranted = canView || canCreate || canEdit || canDelete || canPublish
            if (anyGranted) {
                updatedMap[category.permissionKey] = StaffPermissionEntity(
                    accountId = targetAccountId,
                    sectionKey = category.permissionKey,
                    canView = true,
                    canCreate = canCreate,
                    canEdit = canEdit,
                    canDelete = canDelete,
                    canPublish = canPublish
                )
            } else {
                updatedMap.remove(category.permissionKey)
                category.legacySection?.let { updatedMap.remove(it.key) }
            }
            val res = managementRepository.updateStaffAccount(
                actor = actor,
                targetAccountId = targetAccountId,
                displayName = target.account.displayName,
                isActive = target.account.isActive,
                newPinOrNull = null,
                permissions = updatedMap.values.toList()
            )
            _isBusy.value = false
            res.onSuccess {
                academicsV2Repository?.logAuditEvent(
                    actor = actor,
                    action = "UPDATE_V2_STAFF_PERMISSION",
                    targetRef = "staff/$targetAccountId/${category.permissionKey}",
                    details = "Updated ${target.account.displayName} access for ${category.title}"
                )
                _statusMessage.value = "Updated ${category.title} permissions for ${target.account.displayName}."
            }.onFailure { _errorMessage.value = it.message ?: "Unable to update staff permissions." }
        }
    }

    class Factory(
        private val managementRepository: ManagementRepository,
        private val backupRestoreManager: BackupRestoreManager,
        private val preferencesRepository: UserPreferencesRepository? = null,
        private val centralBackendStatus: CentralBackendStatus? = null,
        private val academicsV2Repository: AcademicsV2ContentRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManagementViewModel(
                managementRepository = managementRepository,
                backupRestoreManager = backupRestoreManager,
                preferencesRepository = preferencesRepository,
                centralBackendStatus = centralBackendStatus,
                academicsV2Repository = academicsV2Repository
            ) as T
        }
    }
}
