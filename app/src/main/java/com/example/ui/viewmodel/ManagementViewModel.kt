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
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.local.entity.VersionHistoryEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.data.repository.ManagementSearchResult
import com.example.data.repository.PublishSaveMode
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
    private val inactivityTimeoutMillis: Long = 5 * 60 * 1000L
) : ViewModel() {

    private val _authUiState = MutableStateFlow<ManagementAuthUiState>(ManagementAuthUiState.Loading)
    val authUiState: StateFlow<ManagementAuthUiState> = _authUiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ManagementSearchResult>>(emptyList())
    val searchResults: StateFlow<List<ManagementSearchResult>> = _searchResults.asStateFlow()

    private val _backupPreview = MutableStateFlow<BackupPreviewInfo?>(null)
    val backupPreview: StateFlow<BackupPreviewInfo?> = _backupPreview.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

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

    init {
        viewModelScope.launch {
            managementRepository.masterAdminExists.collect { exists ->
                val current = _authUiState.value
                when {
                    !exists && current !is ManagementAuthUiState.FirstTimeSetup -> {
                        _authUiState.value = ManagementAuthUiState.FirstTimeSetup()
                    }
                    exists && current is ManagementAuthUiState.Loading -> {
                        _authUiState.value = ManagementAuthUiState.LoginRequired()
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

        // Keep active authenticated session synced with account & permission updates
        viewModelScope.launch {
            managementRepository.accountsWithPermissions.collect { accounts ->
                val current = _authUiState.value
                if (current is ManagementAuthUiState.Authenticated) {
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
                .onSuccess { _statusMessage.value = "Added media to selected Gallery album." }
                .onFailure { _errorMessage.value = it.message ?: "Album assignment failed." }
            _isBusy.value = false
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
            _isBusy.value = true
            clearFeedback()
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
                _statusMessage.value =
                    "Restore completed (${info.contentRecordCount} records, ${info.mediaFileCount} media files)."
            }.onFailure { err ->
                managementRepository.recordBackupOperationFailed(
                    actor = actor,
                    actionType = "BACKUP_RESTORE_FAILED",
                    safeSummary = "Encrypted backup restore failed; existing data preserved."
                )
                _isBusy.value = false
                _errorMessage.value = err.message ?: "Restore failed; existing data was preserved."
            }
        }
    }

    class Factory(
        private val managementRepository: ManagementRepository,
        private val backupRestoreManager: BackupRestoreManager,
        private val preferencesRepository: UserPreferencesRepository? = null,
        private val centralBackendStatus: CentralBackendStatus? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManagementViewModel(
                managementRepository = managementRepository,
                backupRestoreManager = backupRestoreManager,
                preferencesRepository = preferencesRepository,
                centralBackendStatus = centralBackendStatus
            ) as T
        }
    }
}
