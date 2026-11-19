package com.example.core.i18n

import androidx.compose.runtime.Immutable
import com.example.core.model.AppLanguage
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class ManagementAuthStrings(
    val portalTitle: String,
    val dashboardSubtitle: String,
    val localTrialSecurityDisclaimer: String,
    val firstTimeSetupTitle: String,
    val firstTimeSetupSubtitle: String,
    val defaultAdminDisplayName: String,
    val displayNameLabel: String,
    val usernameLabel: String,
    val pinLabel: String,
    val confirmPinLabel: String,
    val pinRuleHint: String,
    val createMasterAdminBtn: String,
    val recoveryKeyTitle: String,
    val recoveryKeySubtitle: String,
    val recoveryKeySavedBtn: String,
    val rotatedRecoveryKeyPrefix: String,
    val loginTitle: String,
    val loginSubtitle: String,
    val loginBtn: String,
    val forgotPinBtn: String,
    val recoverPinTitle: String,
    val recoveryCodeInputLabel: String,
    val newPinLabel: String,
    val resetPinBtn: String,
    val logoutBtn: String,
    val returnPublicBtn: String,
    val backToDashboardDesc: String,
    val togglePinVisibilityDesc: String,
    val togglePasswordVisibilityDesc: String,
    val masterAdminBadge: String,
    val superOwnerBadge: String,
    val staffBadge: String,
    val notificationsLabel: String,
    val globalSearchHint: String
)

@Immutable
data class ManagementActionsAndStatusStrings(
    val cancelBtn: String,
    val closeBtn: String,
    val okBtn: String,
    val dismissBtn: String,
    val saveChangesBtn: String,
    val createAccountBtn: String,
    val saveDraftBtn: String,
    val submitForApprovalBtn: String,
    val publishNowBtn: String,
    val publishBtn: String,
    val unpublishBtn: String,
    val approveAndPublishBtn: String,
    val approveOnlyBtn: String,
    val rejectBtn: String,
    val rejectionReasonHint: String,
    val deleteBtn: String,
    val editBtn: String,
    val addNewBtn: String,
    val restoreVersionBtn: String,
    val statusPublished: String,
    val statusPendingApproval: String,
    val statusRejected: String,
    val statusDraft: String,
    val statusApproved: String,
    val statusUnpublished: String,
    val deleteRecordTitle: String,
    val deleteRecordMsgTemplate: String,
    val deleteStaffTitle: String,
    val deleteStaffMsgTemplate: String,
    val clearNotificationsTitle: String,
    val clearNotificationsMsg: String,
    val orderLabelTemplate: String,
    val noRecordsYet: String,
    val noPermissionNotice: String,
    val statusDraftDiscarded: String,
    val editorEditTab: String,
    val editorPreviewTab: String,
    val editorUnsavedIndicator: String,
    val editorResumingDraftTitleTemplate: String,
    val editorPendingRevisionTitleTemplate: String,
    val editorDraftMetaTemplate: String,
    val editorPendingPreservedHint: String,
    val editorResetToLiveBtn: String,
    val editorDiscardChangesTitle: String,
    val editorDiscardChangesMsg: String,
    val editorKeepEditingBtn: String,
    val editorDiscardBtn: String,
    val editorPreviewCaption: String
)

@Immutable
data class ManagementBackupAndDashboardStrings(
    val backupCreateTitle: String,
    val backupRestoreTitle: String,
    val backupPasswordLabel: String,
    val backupConfirmPasswordLabel: String,
    val backupWarningNote: String,
    val exportEncryptedBackupBtn: String,
    val selectBackupToInspectBtn: String,
    val restorePreviewConfirmBtn: String,
    val backupPreviewTitle: String,
    val backupExportedTemplate: String,
    val backupAppVersionTemplate: String,
    val backupAccountsCountTemplate: String,
    val backupContentCountTemplate: String,
    val backupGalleryMediaCountTemplate: String,
    val backupHistoryLogCountTemplate: String,
    val backupRestoreConfirmWarning: String,
    val backupPasswordTooShortError: String,
    val backupPasswordMismatchError: String,
    val backupRestorePasswordRequiredError: String,
    val cardSchoolProfile: String,
    val cardLogoBanner: String,
    val cardAboutSchool: String,
    val cardAcademics: String,
    val cardCourses: String,
    val cardFacilities: String,
    val cardTransport: String,
    val cardNews: String,
    val cardAnnouncements: String,
    val cardPhotoGallery: String,
    val cardContactDetails: String,
    val cardSocialMedia: String,
    val cardMediaStudio: String,
    val cardStaffManagement: String,
    val cardApprovalCenter: String,
    val cardVersionHistory: String,
    val cardActivityLog: String,
    val cardNotifications: String,
    val cardGlobalSearch: String,
    val cardBackupRestore: String,
    val cardAppSettings: String,
    val approvalPendingTabTemplate: String,
    val approvalReviewedTabTemplate: String,
    val approvalSubmittedByTemplate: String,
    val approvalPreviousPreservedNote: String,
    val approvalRejectionReasonTemplate: String,
    val approvalRejectDialogTitleTemplate: String,
    val approvalReasonRequiredError: String,
    val versionHeaderTemplate: String,
    val versionChangedByTemplate: String,
    val versionStatusTemplate: String,
    val versionSnapshotPayload: String,
    val versionPreviousPayload: String,
    val versionRestoreAndPublishBtn: String,
    val versionRestoreAsRevisionBtn: String,
    val versionSummaryByTemplate: String,
    val auditAppendOnlyNotice: String,
    val auditSearchLabel: String,
    val auditSortNewest: String,
    val auditSortOldest: String,
    val auditAllSections: String,
    val notificationsMarkAllRead: String,
    val notificationsClearAll: String,
    val notificationsEmpty: String,
    val notificationRead: String,
    val notificationUnread: String,
    val notificationsDeviceAlertsTitle: String,
    val notificationsDeviceAlertsSubtitle: String,
    val notificationsPermissionDeniedNotice: String,
    val backendStatusCardTitle: String,
    val backendStatusLocalActiveLabel: String,
    val backendStatusUnconfiguredLabel: String,
    val backendStatusAutoBackupLabel: String,
    val backendStatusRemoteSyncLabel: String,
    val globalSearchNoResultsTemplate: String
)

@Immutable
data class ManagementStaffAndMediaStrings(
    val staffCreateHeaderBtn: String,
    val staffMasterOnlyNotice: String,
    val staffCreateDialogTitle: String,
    val staffEditDialogTitleTemplate: String,
    val staffUsernameRequiredLabel: String,
    val staffDisplayNameRequiredLabel: String,
    val staffRoleStaff: String,
    val staffRoleSuperOwner: String,
    val staffAccountActiveLabel: String,
    val staffInitialPinLabel: String,
    val staffResetPinLabel: String,
    val staffGrantAllBtn: String,
    val staffClearAllBtn: String,
    val staffGranularPermsTitle: String,
    val staffPermissionsLegend: String,
    val staffPermViewCode: String,
    val staffPermCreateCode: String,
    val staffPermEditCode: String,
    val staffPermDeleteCode: String,
    val staffPermPublishCode: String,
    val staffActiveLabel: String,
    val staffDeactivatedLabel: String,
    val staffSectionsCountTemplate: String,
    val staffEditAccountDesc: String,
    val staffDeleteAccountDesc: String,
    val staffUsernameDisplayRequiredError: String,
    val staffDisplayNameRequiredError: String,
    val staffPinInvalidError: String,
    val mediaImportImageBtn: String,
    val mediaAddYouTubeBtn: String,
    val mediaYouTubeDialogTitle: String,
    val mediaYouTubeUrlLabel: String,
    val mediaVideoTitleLabel: String,
    val mediaCaptionLabel: String,
    val mediaAddVideoBtn: String,
    val mediaAssignAlbumTitleTemplate: String,
    val mediaCreateAlbumFirstNotice: String,
    val mediaDeleteTitle: String,
    val mediaAssetLabelTemplate: String,
    val mediaInUseWarningTemplate: String,
    val mediaMissingFileTemplate: String,
    val mediaAssignBannerBtn: String,
    val mediaAssignAlbumBtn: String,
    val mediaReplaceBtn: String,
    val mediaYouTubePrefixTemplate: String,
    val mediaSizeStatsTemplate: String,
    val mediaInvalidYouTubeError: String,
    val mediaVideoTitleRequiredError: String,
    val photoAdjTitle: String,
    val photoAdjSubtitle: String,
    val photoAdjLivePreviewTitle: String,
    val photoAdjZoomInDesc: String,
    val photoAdjZoomOutDesc: String,
    val photoAdjResetBtn: String,
    val photoAdjCropApplyBtn: String,
    val photoAdjCropFailedError: String,
    val photoAdjImageDesc: String
)

@Immutable
data class ManagementProfileAndHeroStrings(
    val profileOfficialNameLabel: String,
    val profileShortNameLabel: String,
    val profileMottoLabel: String,
    val profileCampusAddressLabel: String,
    val profileAffiliationLabel: String,
    val profileMediumGradesLabel: String,
    val profileWelcomeIntroLabel: String,
    val profilePrincipalNameLabel: String,
    val profileVisionLabel: String,
    val profileMissionLabel: String,
    val profileHistoryLabel: String,
    val contactAddressLabel: String,
    val contactPhoneLabel: String,
    val contactEmailLabel: String,
    val contactWebsiteLabel: String,
    val contactOfficeHoursLabel: String,
    val contactMapQueryLabel: String,
    val profileRequiredFieldsError: String,
    val contactAddressRequiredError: String,
    val contactInvalidPhoneError: String,
    val contactInvalidEmailError: String,
    val profileInvalidWebsiteError: String,
    val profileShortNamePreviewLabel: String,
    val profileAffiliationPreviewLabel: String,
    val profileWelcomePreviewLabel: String,
    val changeSummaryUpdatedTemplate: String,
    val heroAddDialogTitle: String,
    val heroEditDialogTitle: String,
    val heroPickPhotoBtn: String,
    val heroAdjustCropBtn: String,
    val heroCaptionOptionalLabel: String,
    val heroDisplayOrderLabel: String,
    val heroDefaultTitle: String,
    val heroBannerCardTitleTemplate: String,
    val heroImageRequiredError: String,
    val heroImageLoadFailedError: String
)

@Immutable
data class ManagementSectionEditorsStrings(
    val academicsAddTitle: String,
    val academicsEditTitle: String,
    val fieldTitleRequiredLabel: String,
    val fieldCategoryRequiredLabel: String,
    val fieldDescriptionRequiredLabel: String,
    val academicsRequiredError: String,
    val coursesAddTitle: String,
    val coursesEditTitle: String,
    val courseGroupCodeRequiredLabel: String,
    val courseStreamTitleRequiredLabel: String,
    val courseSubjectsRequiredLabel: String,
    val courseMediumRequiredLabel: String,
    val courseClassLevelLabel: String,
    val courseEligibilityLabel: String,
    val coursesRequiredError: String,
    val facilitiesAddTitle: String,
    val facilitiesEditTitle: String,
    val facilityNameRequiredLabel: String,
    val facilitySpecLabel: String,
    val facilitiesRequiredError: String,
    val transportAddTitle: String,
    val transportEditTitle: String,
    val transportRouteNumberLabel: String,
    val transportRouteNameLabel: String,
    val transportStopsLabel: String,
    val transportMorningLabel: String,
    val transportAfternoonLabel: String,
    val transportCoordinatorLabel: String,
    val transportRequiredError: String,
    val transportCardTitleTemplate: String,
    val newsAddTitle: String,
    val newsEditTitle: String,
    val newsHeadlineLabel: String,
    val newsSummaryLabel: String,
    val newsFullBodyLabel: String,
    val newsDateTextLabel: String,
    val newsFeaturedCheckLabel: String,
    val newsFeaturedTag: String,
    val newsRequiredError: String,
    val announcementsAddTitle: String,
    val announcementsEditTitle: String,
    val announcementTitleLabel: String,
    val announcementMessageLabel: String,
    val announcementPriorityLabel: String,
    val announcementIssuedDateLabel: String,
    val announcementDefaultPriority: String,
    val announcementsRequiredError: String,
    val galleryCreateAlbumTitle: String,
    val galleryEditAlbumTitle: String,
    val galleryAlbumTitleLabel: String,
    val galleryAlbumCategoryLabel: String,
    val galleryDescriptionLabel: String,
    val galleryEventDateLabel: String,
    val galleryAlbumRequiredError: String,
    val galleryAlbumSubtitleTemplate: String,
    val galleryAddMediaTitleTemplate: String,
    val galleryYouTubeSwitchLabel: String,
    val galleryYouTubeUrlRequiredLabel: String,
    val galleryPickImageBtn: String,
    val galleryReplaceSelectedImageBtn: String,
    val galleryMediaTitleLabel: String,
    val galleryMediaDefaultTitle: String,
    val galleryMediaImageRequiredError: String,
    val galleryMediaProcessFailedError: String,
    val galleryAddMediaToAlbumBtnTemplate: String,
    val socialAddTitle: String,
    val socialEditTitle: String,
    val socialPlatformLabel: String,
    val socialHandleLabel: String,
    val socialUrlLabel: String,
    val socialRequiredError: String,
    val socialInvalidUrlError: String
)

@Immutable
data class ManagementUniversalMediaStrings(
    val fieldSchoolProfilePhotoLabel: String,
    val fieldAboutSchoolPhotoLabel: String,
    val fieldHeroBannerPhotoLabel: String,
    val fieldFacilityPhotoLabel: String,
    val fieldNewsImageLabel: String,
    val fieldAnnouncementImageLabel: String,
    val fieldGalleryPhotoLabel: String,
    val pickFromDeviceBtn: String,
    val pickFromStudioBtn: String,
    val cropAdjustBtn: String,
    val replaceImageBtn: String,
    val removeImageBtn: String,
    val importingImageText: String,
    val badgeOriginalLabel: String,
    val badgeAdjustedLabel: String,
    val badgeUnavailableLabel: String,
    val missingOrUnreadableError: String,
    val emptySelectionHint: String,
    val studioPickerTitle: String,
    val studioPickerSearchHint: String,
    val studioPickerEmpty: String,
    val studioPickerUseOriginalBtn: String,
    val studioPickerUseAdjustedBtn: String,
    val studioPickerUseOptimizedBtn: String,
    val studioPickerAdjustedVersionsCountTemplate: String,
    val studioPickerUnavailableHint: String,
    val cropperTitle: String,
    val cropperSubtitle: String,
    val cropperAspectOriginal: String,
    val cropperAspectFree: String,
    val cropperAspect16_9: String,
    val cropperAspect4_3: String,
    val cropperAspect1_1: String,
    val cropperFreeRatioLabel: String,
    val cropperRotateLeftDesc: String,
    val cropperRotateRightDesc: String,
    val cropperRotateLeftLabel: String,
    val cropperRotateRightLabel: String,
    val cropperLivePreviewLabel: String,
    val cropperApplyBtn: String,
    val galleryManagePhotosBtnTemplate: String,
    val galleryDetailTitleTemplate: String,
    val galleryDetailBackDesc: String,
    val galleryAlbumCoverBadge: String,
    val gallerySetCoverBtn: String,
    val galleryEditMediaTitle: String,
    val galleryDetailEmpty: String,
    val galleryMediaOrderLabel: String,
    val galleryMoveUpDesc: String,
    val galleryMoveDownDesc: String,
    val galleryYouTubeVideoLabel: String,
    val galleryCoverSetPublishedStatus: String,
    val galleryCoverSubmittedStatus: String,
    val galleryOrderUpdatedStatus: String,
    val galleryOrderSubmittedStatus: String
)

@Immutable
data class ManagementStrings(
    val language: AppLanguage,
    val auth: ManagementAuthStrings,
    val actions: ManagementActionsAndStatusStrings,
    val dashboard: ManagementBackupAndDashboardStrings,
    val staffMedia: ManagementStaffAndMediaStrings,
    val profileHero: ManagementProfileAndHeroStrings,
    val content: ManagementSectionEditorsStrings,
    val mediaUi: ManagementUniversalMediaStrings
) {
    // Auth & Shell
    val portalTitle: String get() = auth.portalTitle
    val dashboardSubtitle: String get() = auth.dashboardSubtitle
    val localTrialSecurityDisclaimer: String get() = auth.localTrialSecurityDisclaimer
    val firstTimeSetupTitle: String get() = auth.firstTimeSetupTitle
    val firstTimeSetupSubtitle: String get() = auth.firstTimeSetupSubtitle
    val defaultAdminDisplayName: String get() = auth.defaultAdminDisplayName
    val displayNameLabel: String get() = auth.displayNameLabel
    val usernameLabel: String get() = auth.usernameLabel
    val pinLabel: String get() = auth.pinLabel
    val confirmPinLabel: String get() = auth.confirmPinLabel
    val pinRuleHint: String get() = auth.pinRuleHint
    val createMasterAdminBtn: String get() = auth.createMasterAdminBtn
    val recoveryKeyTitle: String get() = auth.recoveryKeyTitle
    val recoveryKeySubtitle: String get() = auth.recoveryKeySubtitle
    val recoveryKeySavedBtn: String get() = auth.recoveryKeySavedBtn
    val rotatedRecoveryKeyPrefix: String get() = auth.rotatedRecoveryKeyPrefix
    val loginTitle: String get() = auth.loginTitle
    val loginSubtitle: String get() = auth.loginSubtitle
    val loginBtn: String get() = auth.loginBtn
    val forgotPinBtn: String get() = auth.forgotPinBtn
    val recoverPinTitle: String get() = auth.recoverPinTitle
    val recoveryCodeInputLabel: String get() = auth.recoveryCodeInputLabel
    val newPinLabel: String get() = auth.newPinLabel
    val resetPinBtn: String get() = auth.resetPinBtn
    val logoutBtn: String get() = auth.logoutBtn
    val returnPublicBtn: String get() = auth.returnPublicBtn
    val backToDashboardDesc: String get() = auth.backToDashboardDesc
    val togglePinVisibilityDesc: String get() = auth.togglePinVisibilityDesc
    val togglePasswordVisibilityDesc: String get() = auth.togglePasswordVisibilityDesc
    val masterAdminBadge: String get() = auth.masterAdminBadge
    val superOwnerBadge: String get() = auth.superOwnerBadge
    val staffBadge: String get() = auth.staffBadge
    val notificationsLabel: String get() = auth.notificationsLabel
    val globalSearchHint: String get() = auth.globalSearchHint

    // Common Dialog & Workflow Actions
    val cancelBtn: String get() = actions.cancelBtn
    val closeBtn: String get() = actions.closeBtn
    val okBtn: String get() = actions.okBtn
    val dismissBtn: String get() = actions.dismissBtn
    val saveChangesBtn: String get() = actions.saveChangesBtn
    val createAccountBtn: String get() = actions.createAccountBtn
    val saveDraftBtn: String get() = actions.saveDraftBtn
    val submitForApprovalBtn: String get() = actions.submitForApprovalBtn
    val publishNowBtn: String get() = actions.publishNowBtn
    val publishBtn: String get() = actions.publishBtn
    val unpublishBtn: String get() = actions.unpublishBtn
    val approveAndPublishBtn: String get() = actions.approveAndPublishBtn
    val approveOnlyBtn: String get() = actions.approveOnlyBtn
    val rejectBtn: String get() = actions.rejectBtn
    val rejectionReasonHint: String get() = actions.rejectionReasonHint
    val deleteBtn: String get() = actions.deleteBtn
    val editBtn: String get() = actions.editBtn
    val addNewBtn: String get() = actions.addNewBtn
    val restoreVersionBtn: String get() = actions.restoreVersionBtn

    // Status Badges & Confirmation Dialogs
    val statusPublished: String get() = actions.statusPublished
    val statusPendingApproval: String get() = actions.statusPendingApproval
    val statusRejected: String get() = actions.statusRejected
    val statusDraft: String get() = actions.statusDraft
    val statusApproved: String get() = actions.statusApproved
    val statusUnpublished: String get() = actions.statusUnpublished
    val deleteRecordTitle: String get() = actions.deleteRecordTitle
    val deleteRecordMsgTemplate: String get() = actions.deleteRecordMsgTemplate
    val deleteStaffTitle: String get() = actions.deleteStaffTitle
    val deleteStaffMsgTemplate: String get() = actions.deleteStaffMsgTemplate
    val clearNotificationsTitle: String get() = actions.clearNotificationsTitle
    val clearNotificationsMsg: String get() = actions.clearNotificationsMsg
    val orderLabelTemplate: String get() = actions.orderLabelTemplate
    val noRecordsYet: String get() = actions.noRecordsYet
    val noPermissionNotice: String get() = actions.noPermissionNotice
    val statusDraftDiscarded: String get() = actions.statusDraftDiscarded
    val editorEditTab: String get() = actions.editorEditTab
    val editorPreviewTab: String get() = actions.editorPreviewTab
    val editorUnsavedIndicator: String get() = actions.editorUnsavedIndicator
    val editorResumingDraftTitleTemplate: String get() = actions.editorResumingDraftTitleTemplate
    val editorPendingRevisionTitleTemplate: String get() = actions.editorPendingRevisionTitleTemplate
    val editorDraftMetaTemplate: String get() = actions.editorDraftMetaTemplate
    val editorPendingPreservedHint: String get() = actions.editorPendingPreservedHint
    val editorResetToLiveBtn: String get() = actions.editorResetToLiveBtn
    val editorDiscardChangesTitle: String get() = actions.editorDiscardChangesTitle
    val editorDiscardChangesMsg: String get() = actions.editorDiscardChangesMsg
    val editorKeepEditingBtn: String get() = actions.editorKeepEditingBtn
    val editorDiscardBtn: String get() = actions.editorDiscardBtn
    val editorPreviewCaption: String get() = actions.editorPreviewCaption

    // Backup & Restore
    val backupCreateTitle: String get() = dashboard.backupCreateTitle
    val backupRestoreTitle: String get() = dashboard.backupRestoreTitle
    val backupPasswordLabel: String get() = dashboard.backupPasswordLabel
    val backupConfirmPasswordLabel: String get() = dashboard.backupConfirmPasswordLabel
    val backupWarningNote: String get() = dashboard.backupWarningNote
    val exportEncryptedBackupBtn: String get() = dashboard.exportEncryptedBackupBtn
    val selectBackupToInspectBtn: String get() = dashboard.selectBackupToInspectBtn
    val restorePreviewConfirmBtn: String get() = dashboard.restorePreviewConfirmBtn
    val backupPreviewTitle: String get() = dashboard.backupPreviewTitle
    val backupExportedTemplate: String get() = dashboard.backupExportedTemplate
    val backupAppVersionTemplate: String get() = dashboard.backupAppVersionTemplate
    val backupAccountsCountTemplate: String get() = dashboard.backupAccountsCountTemplate
    val backupContentCountTemplate: String get() = dashboard.backupContentCountTemplate
    val backupGalleryMediaCountTemplate: String get() = dashboard.backupGalleryMediaCountTemplate
    val backupHistoryLogCountTemplate: String get() = dashboard.backupHistoryLogCountTemplate
    val backupRestoreConfirmWarning: String get() = dashboard.backupRestoreConfirmWarning
    val backupPasswordTooShortError: String get() = dashboard.backupPasswordTooShortError
    val backupPasswordMismatchError: String get() = dashboard.backupPasswordMismatchError
    val backupRestorePasswordRequiredError: String get() = dashboard.backupRestorePasswordRequiredError

    // Dashboard Section Card Labels
    val cardSchoolProfile: String get() = dashboard.cardSchoolProfile
    val cardLogoBanner: String get() = dashboard.cardLogoBanner
    val cardAboutSchool: String get() = dashboard.cardAboutSchool
    val cardAcademics: String get() = dashboard.cardAcademics
    val cardCourses: String get() = dashboard.cardCourses
    val cardFacilities: String get() = dashboard.cardFacilities
    val cardTransport: String get() = dashboard.cardTransport
    val cardNews: String get() = dashboard.cardNews
    val cardAnnouncements: String get() = dashboard.cardAnnouncements
    val cardPhotoGallery: String get() = dashboard.cardPhotoGallery
    val cardContactDetails: String get() = dashboard.cardContactDetails
    val cardSocialMedia: String get() = dashboard.cardSocialMedia
    val cardMediaStudio: String get() = dashboard.cardMediaStudio
    val cardStaffManagement: String get() = dashboard.cardStaffManagement
    val cardApprovalCenter: String get() = dashboard.cardApprovalCenter
    val cardVersionHistory: String get() = dashboard.cardVersionHistory
    val cardActivityLog: String get() = dashboard.cardActivityLog
    val cardNotifications: String get() = dashboard.cardNotifications
    val cardGlobalSearch: String get() = dashboard.cardGlobalSearch
    val cardBackupRestore: String get() = dashboard.cardBackupRestore
    val cardAppSettings: String get() = dashboard.cardAppSettings

    // Approval Center, Version History, Activity Log, Notifications, Search
    val approvalPendingTabTemplate: String get() = dashboard.approvalPendingTabTemplate
    val approvalReviewedTabTemplate: String get() = dashboard.approvalReviewedTabTemplate
    val approvalSubmittedByTemplate: String get() = dashboard.approvalSubmittedByTemplate
    val approvalPreviousPreservedNote: String get() = dashboard.approvalPreviousPreservedNote
    val approvalRejectionReasonTemplate: String get() = dashboard.approvalRejectionReasonTemplate
    val approvalRejectDialogTitleTemplate: String get() = dashboard.approvalRejectDialogTitleTemplate
    val approvalReasonRequiredError: String get() = dashboard.approvalReasonRequiredError
    val versionHeaderTemplate: String get() = dashboard.versionHeaderTemplate
    val versionChangedByTemplate: String get() = dashboard.versionChangedByTemplate
    val versionStatusTemplate: String get() = dashboard.versionStatusTemplate
    val versionSnapshotPayload: String get() = dashboard.versionSnapshotPayload
    val versionPreviousPayload: String get() = dashboard.versionPreviousPayload
    val versionRestoreAndPublishBtn: String get() = dashboard.versionRestoreAndPublishBtn
    val versionRestoreAsRevisionBtn: String get() = dashboard.versionRestoreAsRevisionBtn
    val versionSummaryByTemplate: String get() = dashboard.versionSummaryByTemplate
    val auditAppendOnlyNotice: String get() = dashboard.auditAppendOnlyNotice
    val auditSearchLabel: String get() = dashboard.auditSearchLabel
    val auditSortNewest: String get() = dashboard.auditSortNewest
    val auditSortOldest: String get() = dashboard.auditSortOldest
    val auditAllSections: String get() = dashboard.auditAllSections
    val notificationsMarkAllRead: String get() = dashboard.notificationsMarkAllRead
    val notificationsClearAll: String get() = dashboard.notificationsClearAll
    val notificationsEmpty: String get() = dashboard.notificationsEmpty
    val notificationRead: String get() = dashboard.notificationRead
    val notificationUnread: String get() = dashboard.notificationUnread
    val notificationsDeviceAlertsTitle: String get() = dashboard.notificationsDeviceAlertsTitle
    val notificationsDeviceAlertsSubtitle: String get() = dashboard.notificationsDeviceAlertsSubtitle
    val notificationsPermissionDeniedNotice: String get() = dashboard.notificationsPermissionDeniedNotice
    val backendStatusCardTitle: String get() = dashboard.backendStatusCardTitle
    val backendStatusLocalActiveLabel: String get() = dashboard.backendStatusLocalActiveLabel
    val backendStatusUnconfiguredLabel: String get() = dashboard.backendStatusUnconfiguredLabel
    val backendStatusAutoBackupLabel: String get() = dashboard.backendStatusAutoBackupLabel
    val backendStatusRemoteSyncLabel: String get() = dashboard.backendStatusRemoteSyncLabel
    val globalSearchNoResultsTemplate: String get() = dashboard.globalSearchNoResultsTemplate

    // Staff Management
    val staffCreateHeaderBtn: String get() = staffMedia.staffCreateHeaderBtn
    val staffMasterOnlyNotice: String get() = staffMedia.staffMasterOnlyNotice
    val staffCreateDialogTitle: String get() = staffMedia.staffCreateDialogTitle
    val staffEditDialogTitleTemplate: String get() = staffMedia.staffEditDialogTitleTemplate
    val staffUsernameRequiredLabel: String get() = staffMedia.staffUsernameRequiredLabel
    val staffDisplayNameRequiredLabel: String get() = staffMedia.staffDisplayNameRequiredLabel
    val staffRoleStaff: String get() = staffMedia.staffRoleStaff
    val staffRoleSuperOwner: String get() = staffMedia.staffRoleSuperOwner
    val staffAccountActiveLabel: String get() = staffMedia.staffAccountActiveLabel
    val staffInitialPinLabel: String get() = staffMedia.staffInitialPinLabel
    val staffResetPinLabel: String get() = staffMedia.staffResetPinLabel
    val staffGrantAllBtn: String get() = staffMedia.staffGrantAllBtn
    val staffClearAllBtn: String get() = staffMedia.staffClearAllBtn
    val staffGranularPermsTitle: String get() = staffMedia.staffGranularPermsTitle
    val staffPermissionsLegend: String get() = staffMedia.staffPermissionsLegend
    val staffPermViewCode: String get() = staffMedia.staffPermViewCode
    val staffPermCreateCode: String get() = staffMedia.staffPermCreateCode
    val staffPermEditCode: String get() = staffMedia.staffPermEditCode
    val staffPermDeleteCode: String get() = staffMedia.staffPermDeleteCode
    val staffPermPublishCode: String get() = staffMedia.staffPermPublishCode
    val staffActiveLabel: String get() = staffMedia.staffActiveLabel
    val staffDeactivatedLabel: String get() = staffMedia.staffDeactivatedLabel
    val staffSectionsCountTemplate: String get() = staffMedia.staffSectionsCountTemplate
    val staffEditAccountDesc: String get() = staffMedia.staffEditAccountDesc
    val staffDeleteAccountDesc: String get() = staffMedia.staffDeleteAccountDesc
    val staffUsernameDisplayRequiredError: String get() = staffMedia.staffUsernameDisplayRequiredError
    val staffDisplayNameRequiredError: String get() = staffMedia.staffDisplayNameRequiredError
    val staffPinInvalidError: String get() = staffMedia.staffPinInvalidError

    // Media Studio & Photo Adjustment
    val mediaImportImageBtn: String get() = staffMedia.mediaImportImageBtn
    val mediaAddYouTubeBtn: String get() = staffMedia.mediaAddYouTubeBtn
    val mediaYouTubeDialogTitle: String get() = staffMedia.mediaYouTubeDialogTitle
    val mediaYouTubeUrlLabel: String get() = staffMedia.mediaYouTubeUrlLabel
    val mediaVideoTitleLabel: String get() = staffMedia.mediaVideoTitleLabel
    val mediaCaptionLabel: String get() = staffMedia.mediaCaptionLabel
    val mediaAddVideoBtn: String get() = staffMedia.mediaAddVideoBtn
    val mediaAssignAlbumTitleTemplate: String get() = staffMedia.mediaAssignAlbumTitleTemplate
    val mediaCreateAlbumFirstNotice: String get() = staffMedia.mediaCreateAlbumFirstNotice
    val mediaDeleteTitle: String get() = staffMedia.mediaDeleteTitle
    val mediaAssetLabelTemplate: String get() = staffMedia.mediaAssetLabelTemplate
    val mediaInUseWarningTemplate: String get() = staffMedia.mediaInUseWarningTemplate
    val mediaMissingFileTemplate: String get() = staffMedia.mediaMissingFileTemplate
    val mediaAssignBannerBtn: String get() = staffMedia.mediaAssignBannerBtn
    val mediaAssignAlbumBtn: String get() = staffMedia.mediaAssignAlbumBtn
    val mediaReplaceBtn: String get() = staffMedia.mediaReplaceBtn
    val mediaYouTubePrefixTemplate: String get() = staffMedia.mediaYouTubePrefixTemplate
    val mediaSizeStatsTemplate: String get() = staffMedia.mediaSizeStatsTemplate
    val mediaInvalidYouTubeError: String get() = staffMedia.mediaInvalidYouTubeError
    val mediaVideoTitleRequiredError: String get() = staffMedia.mediaVideoTitleRequiredError
    val photoAdjTitle: String get() = staffMedia.photoAdjTitle
    val photoAdjSubtitle: String get() = staffMedia.photoAdjSubtitle
    val photoAdjLivePreviewTitle: String get() = staffMedia.photoAdjLivePreviewTitle
    val photoAdjZoomInDesc: String get() = staffMedia.photoAdjZoomInDesc
    val photoAdjZoomOutDesc: String get() = staffMedia.photoAdjZoomOutDesc
    val photoAdjResetBtn: String get() = staffMedia.photoAdjResetBtn
    val photoAdjCropApplyBtn: String get() = staffMedia.photoAdjCropApplyBtn
    val photoAdjCropFailedError: String get() = staffMedia.photoAdjCropFailedError
    val photoAdjImageDesc: String get() = staffMedia.photoAdjImageDesc

    // Profile, About, Contact & Hero
    val profileOfficialNameLabel: String get() = profileHero.profileOfficialNameLabel
    val profileShortNameLabel: String get() = profileHero.profileShortNameLabel
    val profileMottoLabel: String get() = profileHero.profileMottoLabel
    val profileCampusAddressLabel: String get() = profileHero.profileCampusAddressLabel
    val profileAffiliationLabel: String get() = profileHero.profileAffiliationLabel
    val profileMediumGradesLabel: String get() = profileHero.profileMediumGradesLabel
    val profileWelcomeIntroLabel: String get() = profileHero.profileWelcomeIntroLabel
    val profilePrincipalNameLabel: String get() = profileHero.profilePrincipalNameLabel
    val profileVisionLabel: String get() = profileHero.profileVisionLabel
    val profileMissionLabel: String get() = profileHero.profileMissionLabel
    val profileHistoryLabel: String get() = profileHero.profileHistoryLabel
    val contactAddressLabel: String get() = profileHero.contactAddressLabel
    val contactPhoneLabel: String get() = profileHero.contactPhoneLabel
    val contactEmailLabel: String get() = profileHero.contactEmailLabel
    val contactWebsiteLabel: String get() = profileHero.contactWebsiteLabel
    val contactOfficeHoursLabel: String get() = profileHero.contactOfficeHoursLabel
    val contactMapQueryLabel: String get() = profileHero.contactMapQueryLabel
    val profileRequiredFieldsError: String get() = profileHero.profileRequiredFieldsError
    val contactAddressRequiredError: String get() = profileHero.contactAddressRequiredError
    val contactInvalidPhoneError: String get() = profileHero.contactInvalidPhoneError
    val contactInvalidEmailError: String get() = profileHero.contactInvalidEmailError
    val profileInvalidWebsiteError: String get() = profileHero.profileInvalidWebsiteError
    val profileShortNamePreviewLabel: String get() = profileHero.profileShortNamePreviewLabel
    val profileAffiliationPreviewLabel: String get() = profileHero.profileAffiliationPreviewLabel
    val profileWelcomePreviewLabel: String get() = profileHero.profileWelcomePreviewLabel
    val changeSummaryUpdatedTemplate: String get() = profileHero.changeSummaryUpdatedTemplate
    val heroAddDialogTitle: String get() = profileHero.heroAddDialogTitle
    val heroEditDialogTitle: String get() = profileHero.heroEditDialogTitle
    val heroPickPhotoBtn: String get() = profileHero.heroPickPhotoBtn
    val heroAdjustCropBtn: String get() = profileHero.heroAdjustCropBtn
    val heroCaptionOptionalLabel: String get() = profileHero.heroCaptionOptionalLabel
    val heroDisplayOrderLabel: String get() = profileHero.heroDisplayOrderLabel
    val heroDefaultTitle: String get() = profileHero.heroDefaultTitle
    val heroBannerCardTitleTemplate: String get() = profileHero.heroBannerCardTitleTemplate
    val heroImageRequiredError: String get() = profileHero.heroImageRequiredError
    val heroImageLoadFailedError: String get() = profileHero.heroImageLoadFailedError

    // Academics, Courses, Facilities, Transport, News, Announcements, Gallery, Social
    val academicsAddTitle: String get() = content.academicsAddTitle
    val academicsEditTitle: String get() = content.academicsEditTitle
    val fieldTitleRequiredLabel: String get() = content.fieldTitleRequiredLabel
    val fieldCategoryRequiredLabel: String get() = content.fieldCategoryRequiredLabel
    val fieldDescriptionRequiredLabel: String get() = content.fieldDescriptionRequiredLabel
    val academicsRequiredError: String get() = content.academicsRequiredError
    val coursesAddTitle: String get() = content.coursesAddTitle
    val coursesEditTitle: String get() = content.coursesEditTitle
    val courseGroupCodeRequiredLabel: String get() = content.courseGroupCodeRequiredLabel
    val courseStreamTitleRequiredLabel: String get() = content.courseStreamTitleRequiredLabel
    val courseSubjectsRequiredLabel: String get() = content.courseSubjectsRequiredLabel
    val courseMediumRequiredLabel: String get() = content.courseMediumRequiredLabel
    val courseClassLevelLabel: String get() = content.courseClassLevelLabel
    val courseEligibilityLabel: String get() = content.courseEligibilityLabel
    val coursesRequiredError: String get() = content.coursesRequiredError
    val facilitiesAddTitle: String get() = content.facilitiesAddTitle
    val facilitiesEditTitle: String get() = content.facilitiesEditTitle
    val facilityNameRequiredLabel: String get() = content.facilityNameRequiredLabel
    val facilitySpecLabel: String get() = content.facilitySpecLabel
    val facilitiesRequiredError: String get() = content.facilitiesRequiredError
    val transportAddTitle: String get() = content.transportAddTitle
    val transportEditTitle: String get() = content.transportEditTitle
    val transportRouteNumberLabel: String get() = content.transportRouteNumberLabel
    val transportRouteNameLabel: String get() = content.transportRouteNameLabel
    val transportStopsLabel: String get() = content.transportStopsLabel
    val transportMorningLabel: String get() = content.transportMorningLabel
    val transportAfternoonLabel: String get() = content.transportAfternoonLabel
    val transportCoordinatorLabel: String get() = content.transportCoordinatorLabel
    val transportRequiredError: String get() = content.transportRequiredError
    val transportCardTitleTemplate: String get() = content.transportCardTitleTemplate
    val newsAddTitle: String get() = content.newsAddTitle
    val newsEditTitle: String get() = content.newsEditTitle
    val newsHeadlineLabel: String get() = content.newsHeadlineLabel
    val newsSummaryLabel: String get() = content.newsSummaryLabel
    val newsFullBodyLabel: String get() = content.newsFullBodyLabel
    val newsDateTextLabel: String get() = content.newsDateTextLabel
    val newsFeaturedCheckLabel: String get() = content.newsFeaturedCheckLabel
    val newsFeaturedTag: String get() = content.newsFeaturedTag
    val newsRequiredError: String get() = content.newsRequiredError
    val announcementsAddTitle: String get() = content.announcementsAddTitle
    val announcementsEditTitle: String get() = content.announcementsEditTitle
    val announcementTitleLabel: String get() = content.announcementTitleLabel
    val announcementMessageLabel: String get() = content.announcementMessageLabel
    val announcementPriorityLabel: String get() = content.announcementPriorityLabel
    val announcementIssuedDateLabel: String get() = content.announcementIssuedDateLabel
    val announcementDefaultPriority: String get() = content.announcementDefaultPriority
    val announcementsRequiredError: String get() = content.announcementsRequiredError
    val galleryCreateAlbumTitle: String get() = content.galleryCreateAlbumTitle
    val galleryEditAlbumTitle: String get() = content.galleryEditAlbumTitle
    val galleryAlbumTitleLabel: String get() = content.galleryAlbumTitleLabel
    val galleryAlbumCategoryLabel: String get() = content.galleryAlbumCategoryLabel
    val galleryDescriptionLabel: String get() = content.galleryDescriptionLabel
    val galleryEventDateLabel: String get() = content.galleryEventDateLabel
    val galleryAlbumRequiredError: String get() = content.galleryAlbumRequiredError
    val galleryAlbumSubtitleTemplate: String get() = content.galleryAlbumSubtitleTemplate
    val galleryAddMediaTitleTemplate: String get() = content.galleryAddMediaTitleTemplate
    val galleryYouTubeSwitchLabel: String get() = content.galleryYouTubeSwitchLabel
    val galleryYouTubeUrlRequiredLabel: String get() = content.galleryYouTubeUrlRequiredLabel
    val galleryPickImageBtn: String get() = content.galleryPickImageBtn
    val galleryReplaceSelectedImageBtn: String get() = content.galleryReplaceSelectedImageBtn
    val galleryMediaTitleLabel: String get() = content.galleryMediaTitleLabel
    val galleryMediaDefaultTitle: String get() = content.galleryMediaDefaultTitle
    val galleryMediaImageRequiredError: String get() = content.galleryMediaImageRequiredError
    val galleryMediaProcessFailedError: String get() = content.galleryMediaProcessFailedError
    val galleryAddMediaToAlbumBtnTemplate: String get() = content.galleryAddMediaToAlbumBtnTemplate
    val socialAddTitle: String get() = content.socialAddTitle
    val socialEditTitle: String get() = content.socialEditTitle
    val socialPlatformLabel: String get() = content.socialPlatformLabel
    val socialHandleLabel: String get() = content.socialHandleLabel
    val socialUrlLabel: String get() = content.socialUrlLabel
    val socialRequiredError: String get() = content.socialRequiredError
    val socialInvalidUrlError: String get() = content.socialInvalidUrlError

    val locale: Locale
        get() = when (language) {
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.TAMIL -> Locale.forLanguageTag("ta-IN")
            AppLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
        }

    fun formatTimestamp(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", locale).format(Date(millis))

    fun formatDate(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("dd MMM yyyy", locale).format(Date(millis))

    fun formatZoom(zoom: Float): String =
        String.format(locale, "%.1fx", zoom)

    fun roleLabel(role: String): String = when (role) {
        ManagementRole.MASTER_ADMIN -> masterAdminBadge
        ManagementRole.SUPER_OWNER -> superOwnerBadge
        else -> staffBadge
    }

    fun workflowStatusLabel(status: String, isPublished: Boolean = false, isApproved: Boolean = false): String = when {
        isPublished && isApproved && status == ApprovalStatus.APPROVED -> statusPublished
        status == ContentWorkflowStatus.PUBLISHED -> statusPublished
        status == ApprovalStatus.PENDING || status == ContentWorkflowStatus.PENDING_APPROVAL -> statusPendingApproval
        status == ApprovalStatus.REJECTED || status == ContentWorkflowStatus.REJECTED -> statusRejected
        status == ContentWorkflowStatus.UNPUBLISHED -> statusUnpublished
        status == ContentWorkflowStatus.DRAFT_DISCARDED -> statusDraftDiscarded
        status == ApprovalStatus.APPROVED -> statusApproved
        status == ContentWorkflowStatus.DRAFT || status == ApprovalStatus.DRAFT || status.isBlank() -> statusDraft
        else -> status
    }

    fun sectionLabel(section: ManagementSection): String = when (section) {
        ManagementSection.SCHOOL_PROFILE -> cardSchoolProfile
        ManagementSection.LOGO_BANNER -> cardLogoBanner
        ManagementSection.ABOUT_SCHOOL -> cardAboutSchool
        ManagementSection.ACADEMICS -> cardAcademics
        ManagementSection.HIGHER_SECONDARY_COURSES -> cardCourses
        ManagementSection.FACILITIES -> cardFacilities
        ManagementSection.TRANSPORT -> cardTransport
        ManagementSection.NEWS -> cardNews
        ManagementSection.ANNOUNCEMENTS -> cardAnnouncements
        ManagementSection.PHOTO_GALLERY -> cardPhotoGallery
        ManagementSection.CONTACT_DETAILS -> cardContactDetails
        ManagementSection.SOCIAL_MEDIA -> cardSocialMedia
        ManagementSection.MEDIA_STUDIO -> cardMediaStudio
        ManagementSection.STAFF_MANAGEMENT -> cardStaffManagement
        ManagementSection.NOTIFICATIONS -> cardNotifications
        ManagementSection.ACTIVITY_LOG -> cardActivityLog
        ManagementSection.BACKUP_RESTORE -> cardBackupRestore
        ManagementSection.APP_SETTINGS -> cardAppSettings
    }

    fun deleteRecordMsg(title: String): String = deleteRecordMsgTemplate.replace("{title}", title)
    fun deleteStaffMsg(name: String, username: String): String =
        deleteStaffMsgTemplate.replace("{name}", name).replace("{username}", username)
    fun editorResumingDraftTitle(ver: Int?): String =
        if (ver != null) editorResumingDraftTitleTemplate.replace("{ver}", ver.toString())
        else editorResumingDraftTitleTemplate.replace(" (v{ver})", "").replace("(v{ver})", "").trim()
    fun editorPendingRevisionTitle(ver: Int?): String =
        if (ver != null) editorPendingRevisionTitleTemplate.replace("{ver}", ver.toString())
        else editorPendingRevisionTitleTemplate.replace(" (v{ver})", "").replace("(v{ver})", "").trim()
    fun editorDraftMeta(name: String, time: String): String =
        editorDraftMetaTemplate.replace("{name}", name).replace("{time}", time)
    fun orderSubtitle(order: Int): String = orderLabelTemplate.replace("{order}", order.toString())
    fun approvalPendingTab(count: Int): String = approvalPendingTabTemplate.replace("{count}", count.toString())
    fun approvalReviewedTab(count: Int): String = approvalReviewedTabTemplate.replace("{count}", count.toString())
    fun approvalSubmittedBy(name: String): String = approvalSubmittedByTemplate.replace("{name}", name)
    fun approvalRejectionReason(reason: String): String = approvalRejectionReasonTemplate.replace("{reason}", reason)
    fun approvalRejectDialogTitle(title: String): String = approvalRejectDialogTitleTemplate.replace("{title}", title)
    fun versionHeader(ver: Int, title: String): String = versionHeaderTemplate.replace("{ver}", ver.toString()).replace("{title}", title)
    fun versionChangedBy(name: String, time: String): String = versionChangedByTemplate.replace("{name}", name).replace("{time}", time)
    fun versionStatus(status: String): String = versionStatusTemplate.replace("{status}", status)
    fun versionSummaryBy(summary: String, name: String): String = versionSummaryByTemplate.replace("{summary}", summary).replace("{name}", name)
    fun globalSearchNoResults(query: String): String = globalSearchNoResultsTemplate.replace("{query}", query)
    fun staffEditDialogTitle(name: String): String = staffEditDialogTitleTemplate.replace("{name}", name)
    fun staffSectionsCount(count: Int): String = staffSectionsCountTemplate.replace("{count}", count.toString())
    fun mediaAssignAlbumTitle(title: String): String = mediaAssignAlbumTitleTemplate.replace("{title}", title)
    fun mediaAssetLabel(title: String): String = mediaAssetLabelTemplate.replace("{title}", title)
    fun mediaInUseWarning(warnings: String): String = mediaInUseWarningTemplate.replace("{warnings}", warnings)
    fun mediaMissingFile(uri: String): String = mediaMissingFileTemplate.replace("{uri}", uri)
    fun mediaYouTubePrefix(url: String): String = mediaYouTubePrefixTemplate.replace("{url}", url)
    fun mediaSizeStats(mime: String, w: Int, h: Int, origKb: Long, optKb: Long): String =
        mediaSizeStatsTemplate
            .replace("{mime}", mime)
            .replace("{w}", w.toString())
            .replace("{h}", h.toString())
            .replace("{orig}", origKb.toString())
            .replace("{opt}", optKb.toString())
    fun changeSummaryUpdated(sectionName: String): String = changeSummaryUpdatedTemplate.replace("{section}", sectionName)
    fun heroBannerCardTitle(id: Long): String = heroBannerCardTitleTemplate.replace("{id}", id.toString())
    fun transportCardTitle(routeNumber: String, routeName: String): String =
        transportCardTitleTemplate.replace("{num}", routeNumber).replace("{name}", routeName)
    fun galleryAlbumSubtitle(category: String, count: Int): String =
        galleryAlbumSubtitleTemplate.replace("{category}", category).replace("{count}", count.toString())
    fun galleryAddMediaTitle(albumTitle: String): String = galleryAddMediaTitleTemplate.replace("{album}", albumTitle)
    fun galleryAddMediaToAlbumBtn(albumTitle: String): String = galleryAddMediaToAlbumBtnTemplate.replace("{album}", albumTitle)
    fun backupExportedText(time: String): String = backupExportedTemplate.replace("{time}", time)
    fun backupAppVersionText(versionName: String, schemaVersion: Int): String =
        backupAppVersionTemplate.replace("{ver}", versionName).replace("{schema}", schemaVersion.toString())
    fun backupAccountsCountText(count: Int): String = backupAccountsCountTemplate.replace("{count}", count.toString())
    fun backupContentCountText(count: Int): String = backupContentCountTemplate.replace("{count}", count.toString())
    fun backupGalleryMediaCountText(galleryCount: Int, fileCount: Int): String =
        backupGalleryMediaCountTemplate.replace("{gallery}", galleryCount.toString()).replace("{files}", fileCount.toString())
    fun backupHistoryLogCountText(count: Int): String = backupHistoryLogCountTemplate.replace("{count}", count.toString())
    fun studioPickerAdjustedVersionsCount(count: Int): String =
        mediaUi.studioPickerAdjustedVersionsCountTemplate.replace("{count}", count.toString())
    fun galleryManagePhotosBtn(count: Int): String =
        mediaUi.galleryManagePhotosBtnTemplate.replace("{count}", count.toString())
    fun galleryDetailTitle(albumTitle: String): String =
        mediaUi.galleryDetailTitleTemplate.replace("{album}", albumTitle)
}

fun managementStringsFor(language: AppLanguage): ManagementStrings = when (language) {
    AppLanguage.ENGLISH -> ManagementStrings(
        language = AppLanguage.ENGLISH,
        auth = ManagementAuthStrings(
            portalTitle = "Management Dashboard",
            dashboardSubtitle = "Local School Administration & Governance",
            localTrialSecurityDisclaimer = "Local offline trial authentication (salted PBKDF2 PIN). Designed for on-device demonstration, not cloud/server security.",
            firstTimeSetupTitle = "Master Admin First-Time Setup",
            firstTimeSetupSubtitle = "Create the device Master Administrator account. No default PIN exists.",
            defaultAdminDisplayName = "Principal / Administrator",
            displayNameLabel = "Administrator Name",
            usernameLabel = "Username",
            pinLabel = "PIN (min 6 digits)",
            confirmPinLabel = "Confirm PIN",
            pinRuleHint = "Enter at least 6 numeric digits.",
            createMasterAdminBtn = "Create Master Admin",
            recoveryKeyTitle = "Save Your Offline Recovery Key",
            recoveryKeySubtitle = "This one-time key is the only way to reset a forgotten Master Admin PIN locally. Store it safely.",
            recoveryKeySavedBtn = "I Have Saved This Key — Enter Dashboard",
            rotatedRecoveryKeyPrefix = "New rotated recovery key:",
            loginTitle = "Management Portal Login",
            loginSubtitle = "Sign in with Master Admin, Super Owner, or Staff credentials.",
            loginBtn = "Unlock Dashboard",
            forgotPinBtn = "Recover Master PIN",
            recoverPinTitle = "Master Admin PIN Recovery",
            recoveryCodeInputLabel = "One-Time Recovery Key",
            newPinLabel = "New PIN (min 6 digits)",
            resetPinBtn = "Reset PIN",
            logoutBtn = "Lock & Sign Out",
            returnPublicBtn = "Public App",
            backToDashboardDesc = "Back to Dashboard",
            togglePinVisibilityDesc = "Toggle PIN visibility",
            togglePasswordVisibilityDesc = "Show or hide password",
            masterAdminBadge = "Master Admin",
            superOwnerBadge = "Super Owner",
            staffBadge = "Staff",
            notificationsLabel = "Notifications",
            globalSearchHint = "Search permitted content, staff, media..."
        ),
        actions = ManagementActionsAndStatusStrings(
            cancelBtn = "Cancel",
            closeBtn = "Close",
            okBtn = "OK",
            dismissBtn = "Dismiss",
            saveChangesBtn = "Save Changes",
            createAccountBtn = "Create Account",
            saveDraftBtn = "Save Draft",
            submitForApprovalBtn = "Submit for Approval",
            publishNowBtn = "Publish Now",
            publishBtn = "Publish",
            unpublishBtn = "Unpublish",
            approveAndPublishBtn = "Approve & Publish",
            approveOnlyBtn = "Approve Only",
            rejectBtn = "Reject",
            rejectionReasonHint = "Enter rejection reason (required)",
            deleteBtn = "Delete",
            editBtn = "Edit",
            addNewBtn = "Add New",
            restoreVersionBtn = "Restore Version",
            statusPublished = "Published",
            statusPendingApproval = "Pending Approval",
            statusRejected = "Rejected",
            statusDraft = "Draft",
            statusApproved = "Approved",
            statusUnpublished = "Unpublished",
            deleteRecordTitle = "Delete Record?",
            deleteRecordMsgTemplate = "Are you sure you want to delete \"{title}\"?",
            deleteStaffTitle = "Delete Staff Account?",
            deleteStaffMsgTemplate = "Are you sure you want to permanently remove \"{name}\" (@{username})? This action cannot be undone.",
            clearNotificationsTitle = "Clear All Notifications?",
            clearNotificationsMsg = "Are you sure you want to remove all local management notifications? This cannot be undone.",
            orderLabelTemplate = "Order: {order}",
            noRecordsYet = "No items created yet.",
            noPermissionNotice = "Your account does not currently hold permission for this action.",
            statusDraftDiscarded = "Draft Discarded",
            editorEditTab = "Edit",
            editorPreviewTab = "Preview",
            editorUnsavedIndicator = "Unsaved changes",
            editorResumingDraftTitleTemplate = "Resuming Draft (v{ver})",
            editorPendingRevisionTitleTemplate = "Pending Revision (v{ver})",
            editorDraftMetaTemplate = "Saved by {name} • {time}",
            editorPendingPreservedHint = "Live public version remains unchanged until approved.",
            editorResetToLiveBtn = "Reset to Live",
            editorDiscardChangesTitle = "Discard unsaved changes?",
            editorDiscardChangesMsg = "You have unsaved edits. Leaving now will discard your changes.",
            editorKeepEditingBtn = "Keep Editing",
            editorDiscardBtn = "Discard",
            editorPreviewCaption = "Public App Preview — reflects how this content appears once published."
        ),
        dashboard = ManagementBackupAndDashboardStrings(
            backupCreateTitle = "Create Password-Protected Encrypted Backup",
            backupRestoreTitle = "Inspect & Restore Encrypted Backup",
            backupPasswordLabel = "Backup Password (min 6 chars)",
            backupConfirmPasswordLabel = "Confirm Backup Password",
            backupWarningNote = "Remember this password. Encrypted backups cannot be recovered without it. Restoring replaces local school records after preview confirmation.",
            exportEncryptedBackupBtn = "Export Encrypted Backup (.chvbak)",
            selectBackupToInspectBtn = "Select Backup & Preview",
            restorePreviewConfirmBtn = "Confirm & Restore Backup",
            backupPreviewTitle = "Backup Archive Preview",
            backupExportedTemplate = "Exported: {time}",
            backupAppVersionTemplate = "App Version: {ver} • Schema v{schema}",
            backupAccountsCountTemplate = "Accounts: {count}",
            backupContentCountTemplate = "School Content Records: {count}",
            backupGalleryMediaCountTemplate = "Gallery Items: {gallery} • Media Files: {files}",
            backupHistoryLogCountTemplate = "History & Audit Logs: {count}",
            backupRestoreConfirmWarning = "Warning: Confirming restore replaces current local records and media safely inside a transaction.",
            backupPasswordTooShortError = "Backup password must be at least 6 characters.",
            backupPasswordMismatchError = "Password and Confirm Password do not match.",
            backupRestorePasswordRequiredError = "Please enter the backup password before selecting an archive.",
            cardSchoolProfile = "School Profile",
            cardLogoBanner = "Logo & Banner",
            cardAboutSchool = "About School",
            cardAcademics = "Academics",
            cardCourses = "Higher Secondary Courses",
            cardFacilities = "Facilities",
            cardTransport = "Transport",
            cardNews = "News",
            cardAnnouncements = "Announcements",
            cardPhotoGallery = "Photo Gallery",
            cardContactDetails = "Contact Details",
            cardSocialMedia = "Social Media",
            cardMediaStudio = "Media Studio",
            cardStaffManagement = "Staff Management",
            cardApprovalCenter = "Approval Center",
            cardVersionHistory = "Version History",
            cardActivityLog = "Activity Log",
            cardNotifications = "Notifications",
            cardGlobalSearch = "Global Search",
            cardBackupRestore = "Backup & Restore",
            cardAppSettings = "App Settings",
            approvalPendingTabTemplate = "Pending ({count})",
            approvalReviewedTabTemplate = "Reviewed ({count})",
            approvalSubmittedByTemplate = "By {name}",
            approvalPreviousPreservedNote = "Previous snapshot preserved while pending review.",
            approvalRejectionReasonTemplate = "Rejection reason: {reason}",
            approvalRejectDialogTitleTemplate = "Reject \"{title}\"",
            approvalReasonRequiredError = "Please enter a clear rejection reason before rejecting.",
            versionHeaderTemplate = "v{ver} — {title}",
            versionChangedByTemplate = "Changed by: {name} • {time}",
            versionStatusTemplate = "Status: {status}",
            versionSnapshotPayload = "Snapshot Payload:",
            versionPreviousPayload = "Previous Version Payload:",
            versionRestoreAndPublishBtn = "Restore & Publish",
            versionRestoreAsRevisionBtn = "Restore as Pending Revision",
            versionSummaryByTemplate = "{summary} • By {name}",
            auditAppendOnlyNotice = "Append-only audit log. Records cannot be edited or deleted.",
            auditSearchLabel = "Search actor, action, record...",
            auditSortNewest = "Sort: Newest First",
            auditSortOldest = "Sort: Oldest First",
            auditAllSections = "All Sections",
            notificationsMarkAllRead = "Mark All Read",
            notificationsClearAll = "Clear All",
            notificationsEmpty = "No local management notifications yet.",
            notificationRead = "Read",
            notificationUnread = "Unread",
            notificationsDeviceAlertsTitle = "Device Alert Notifications",
            notificationsDeviceAlertsSubtitle = "Show local Android notifications for approvals, backups, and security updates. Remote push (FCM) is unconfigured.",
            notificationsPermissionDeniedNotice = "Notification permission denied on this device. Events remain recorded in the local management inbox.",
            backendStatusCardTitle = "Backend, Sync & Security Status",
            backendStatusLocalActiveLabel = "Active (Local)",
            backendStatusUnconfiguredLabel = "Unconfigured",
            backendStatusAutoBackupLabel = "OS Auto Backup: Disabled for local DB, preferences, and PIN verifiers.",
            backendStatusRemoteSyncLabel = "Remote Cloud Sync & Push (FCM): Unconfigured",
            globalSearchNoResultsTemplate = "No matching permitted records found for \"{query}\"."
        ),
        staffMedia = ManagementStaffAndMediaStrings(
            staffCreateHeaderBtn = "Create Staff / Super Owner Account",
            staffMasterOnlyNotice = "Only Master Admin can create, modify, or delete staff accounts.",
            staffCreateDialogTitle = "Create Staff / Super Owner",
            staffEditDialogTitleTemplate = "Edit {name}",
            staffUsernameRequiredLabel = "Username *",
            staffDisplayNameRequiredLabel = "Display Name *",
            staffRoleStaff = "Staff",
            staffRoleSuperOwner = "Super Owner",
            staffAccountActiveLabel = "Account Active",
            staffInitialPinLabel = "Initial PIN (min 6 digits) *",
            staffResetPinLabel = "Reset PIN (leave blank to keep)",
            staffGrantAllBtn = "Grant All",
            staffClearAllBtn = "Clear All",
            staffGranularPermsTitle = "18-Section Granular Permissions",
            staffPermissionsLegend = "V = View, C = Create, E = Edit, D = Delete, P = Publish",
            staffPermViewCode = "V",
            staffPermCreateCode = "C",
            staffPermEditCode = "E",
            staffPermDeleteCode = "D",
            staffPermPublishCode = "P",
            staffActiveLabel = "Active",
            staffDeactivatedLabel = "Deactivated",
            staffSectionsCountTemplate = "{count} sections",
            staffEditAccountDesc = "Edit account",
            staffDeleteAccountDesc = "Delete account",
            staffUsernameDisplayRequiredError = "Username and display name are required.",
            staffDisplayNameRequiredError = "Display name is required.",
            staffPinInvalidError = "PIN must be at least 6 numeric digits.",
            mediaImportImageBtn = "Import Image (Photo Picker)",
            mediaAddYouTubeBtn = "Add YouTube URL",
            mediaYouTubeDialogTitle = "Add YouTube Video Reference",
            mediaYouTubeUrlLabel = "YouTube URL (https://youtube.com/...)",
            mediaVideoTitleLabel = "Video Title *",
            mediaCaptionLabel = "Caption",
            mediaAddVideoBtn = "Add Video",
            mediaAssignAlbumTitleTemplate = "Assign \"{title}\" to Gallery Album",
            mediaCreateAlbumFirstNotice = "Create a Gallery album in Photo Gallery first.",
            mediaDeleteTitle = "Delete Media Asset?",
            mediaAssetLabelTemplate = "Asset: {title}",
            mediaInUseWarningTemplate = "In use in: {warnings}. Deleting removes the underlying file.",
            mediaMissingFileTemplate = "Local file missing or unreadable: {uri}",
            mediaAssignBannerBtn = "Assign Banner",
            mediaAssignAlbumBtn = "Assign Album",
            mediaReplaceBtn = "Replace",
            mediaYouTubePrefixTemplate = "YouTube: {url}",
            mediaSizeStatsTemplate = "{mime} • {w}x{h} • Orig {orig}KB -> Opt {opt}KB",
            mediaInvalidYouTubeError = "Enter a valid YouTube URL (youtube.com or youtu.be).",
            mediaVideoTitleRequiredError = "Video title is required.",
            photoAdjTitle = "Adjust Home Hero Banner Photo",
            photoAdjSubtitle = "Drag to reposition, pinch or use sliders to zoom. Your original photo is preserved untouched.",
            photoAdjLivePreviewTitle = "Live Home Hero Banner Preview",
            photoAdjZoomInDesc = "Zoom In",
            photoAdjZoomOutDesc = "Zoom Out",
            photoAdjResetBtn = "Reset",
            photoAdjCropApplyBtn = "Crop & Apply",
            photoAdjCropFailedError = "Failed to crop and save image.",
            photoAdjImageDesc = "Adjustable Hero Banner Photo"
        ),
        profileHero = ManagementProfileAndHeroStrings(
            profileOfficialNameLabel = "Official School Name *",
            profileShortNameLabel = "Short Name *",
            profileMottoLabel = "Motto *",
            profileCampusAddressLabel = "Campus Address *",
            profileAffiliationLabel = "Affiliation / Code (Optional)",
            profileMediumGradesLabel = "Medium & Grades Note",
            profileWelcomeIntroLabel = "Home Welcome Introduction",
            profilePrincipalNameLabel = "Principal Name",
            profileVisionLabel = "School Vision",
            profileMissionLabel = "School Mission",
            profileHistoryLabel = "School History",
            contactAddressLabel = "Address *",
            contactPhoneLabel = "Official Phone",
            contactEmailLabel = "Official Email",
            contactWebsiteLabel = "Official Website URL",
            contactOfficeHoursLabel = "Office Hours",
            contactMapQueryLabel = "Google Maps Query / Coordinates",
            profileRequiredFieldsError = "Official name, short name, motto, and address are required.",
            contactAddressRequiredError = "Address is required.",
            contactInvalidPhoneError = "Enter a valid phone number (7–15 digits; digits, +, spaces, (), or -).",
            contactInvalidEmailError = "Enter a valid email address (e.g., office@school.edu).",
            profileInvalidWebsiteError = "Website URL must start with http:// or https://.",
            profileShortNamePreviewLabel = "Short Name",
            profileAffiliationPreviewLabel = "Affiliation / Code",
            profileWelcomePreviewLabel = "Home Welcome Introduction",
            changeSummaryUpdatedTemplate = "Updated {section}",
            heroAddDialogTitle = "Add Campus Banner Photo",
            heroEditDialogTitle = "Edit Campus Banner Photo",
            heroPickPhotoBtn = "Pick Campus Photo (Photo Picker)",
            heroAdjustCropBtn = "Adjust / Crop / Preview Banner",
            heroCaptionOptionalLabel = "Caption (Optional)",
            heroDisplayOrderLabel = "Display Order",
            heroDefaultTitle = "Campus Banner",
            heroBannerCardTitleTemplate = "Campus Banner #{id}",
            heroImageRequiredError = "Please select an image first.",
            heroImageLoadFailedError = "Failed to load selected image."
        ),
        content = ManagementSectionEditorsStrings(
            academicsAddTitle = "Add Academic Info",
            academicsEditTitle = "Edit Academic Info",
            fieldTitleRequiredLabel = "Title *",
            fieldCategoryRequiredLabel = "Category *",
            fieldDescriptionRequiredLabel = "Description *",
            academicsRequiredError = "Title, category, and description are required.",
            coursesAddTitle = "Add Higher Secondary Course",
            coursesEditTitle = "Edit Course",
            courseGroupCodeRequiredLabel = "Group Code (e.g. Group I) *",
            courseStreamTitleRequiredLabel = "Stream Title *",
            courseSubjectsRequiredLabel = "Subjects Summary *",
            courseMediumRequiredLabel = "Medium of Instruction *",
            courseClassLevelLabel = "Classes (e.g. XI & XII)",
            courseEligibilityLabel = "Eligibility Note",
            coursesRequiredError = "Group code, stream title, subjects, and medium are required.",
            facilitiesAddTitle = "Add Facility",
            facilitiesEditTitle = "Edit Facility",
            facilityNameRequiredLabel = "Facility Name *",
            facilitySpecLabel = "Capacity / Specification",
            facilitiesRequiredError = "Facility name, category, and description are required.",
            transportAddTitle = "Add Transport Route",
            transportEditTitle = "Edit Transport Route",
            transportRouteNumberLabel = "Route Number *",
            transportRouteNameLabel = "Route Name *",
            transportStopsLabel = "Stops Summary *",
            transportMorningLabel = "Morning Timing",
            transportAfternoonLabel = "Afternoon Timing",
            transportCoordinatorLabel = "Coordinator Contact",
            transportRequiredError = "Route number, name, and stops are required.",
            transportCardTitleTemplate = "Route {num} — {name}",
            newsAddTitle = "Add News Article",
            newsEditTitle = "Edit News Article",
            newsHeadlineLabel = "Headline *",
            newsSummaryLabel = "Summary *",
            newsFullBodyLabel = "Full Article Body *",
            newsDateTextLabel = "Date Text *",
            newsFeaturedCheckLabel = "Featured News Story",
            newsFeaturedTag = "Featured",
            newsRequiredError = "Title, summary, and full body are required.",
            announcementsAddTitle = "Add Announcement",
            announcementsEditTitle = "Edit Announcement",
            announcementTitleLabel = "Announcement Title *",
            announcementMessageLabel = "Official Message *",
            announcementPriorityLabel = "Priority / Label (e.g. Circular)",
            announcementIssuedDateLabel = "Issued Date",
            announcementDefaultPriority = "Announcement",
            announcementsRequiredError = "Title and message are required.",
            galleryCreateAlbumTitle = "Create Photo Album",
            galleryEditAlbumTitle = "Edit Photo Album",
            galleryAlbumTitleLabel = "Album Title *",
            galleryAlbumCategoryLabel = "Category (Campus / Events / Academics)",
            galleryDescriptionLabel = "Description",
            galleryEventDateLabel = "Event Date",
            galleryAlbumRequiredError = "Album title is required.",
            galleryAlbumSubtitleTemplate = "{category} • {count} items",
            galleryAddMediaTitleTemplate = "Add Media to {album}",
            galleryYouTubeSwitchLabel = "YouTube Video Link (instead of Photo)",
            galleryYouTubeUrlRequiredLabel = "YouTube URL *",
            galleryPickImageBtn = "Pick Image (JPG/PNG/WEBP)",
            galleryReplaceSelectedImageBtn = "Photo Selected — Tap to Replace",
            galleryMediaTitleLabel = "Photo / Video Title",
            galleryMediaDefaultTitle = "Gallery Media",
            galleryMediaImageRequiredError = "Please choose an image first.",
            galleryMediaProcessFailedError = "Unable to process photo.",
            galleryAddMediaToAlbumBtnTemplate = "+ Add Photo / Video to \"{album}\"",
            socialAddTitle = "Add Social Link",
            socialEditTitle = "Edit Social Link",
            socialPlatformLabel = "Platform (Instagram/Facebook/YouTube/X) *",
            socialHandleLabel = "Handle or Label *",
            socialUrlLabel = "Official URL (https://...) *",
            socialRequiredError = "Platform, handle, and valid http(s) URL are required.",
            socialInvalidUrlError = "Enter a valid official URL starting with http:// or https://."
        ),
        mediaUi = ManagementUniversalMediaStrings(
            fieldSchoolProfilePhotoLabel = "School Profile / Campus Photo",
            fieldAboutSchoolPhotoLabel = "About School Photo",
            fieldHeroBannerPhotoLabel = "Campus Hero Banner Photo *",
            fieldFacilityPhotoLabel = "Facility Photo",
            fieldNewsImageLabel = "Article Image",
            fieldAnnouncementImageLabel = "Announcement / Circular Image",
            fieldGalleryPhotoLabel = "Gallery Photo",
            pickFromDeviceBtn = "Choose from Device",
            pickFromStudioBtn = "Media Studio",
            cropAdjustBtn = "Crop / Rotate",
            replaceImageBtn = "Change",
            removeImageBtn = "Remove",
            importingImageText = "Processing selected image...",
            badgeOriginalLabel = "Original",
            badgeAdjustedLabel = "Adjusted",
            badgeUnavailableLabel = "Unavailable",
            missingOrUnreadableError = "Selected image file is missing or unreadable.",
            emptySelectionHint = "Optional photo. Pick from device or choose an existing asset from Media Studio.",
            studioPickerTitle = "Select Image from Media Studio",
            studioPickerSearchHint = "Search managed images...",
            studioPickerEmpty = "No managed images available in Media Studio yet.",
            studioPickerUseOriginalBtn = "Original",
            studioPickerUseAdjustedBtn = "Adjusted",
            studioPickerUseOptimizedBtn = "Optimized",
            studioPickerAdjustedVersionsCountTemplate = "{count} adjusted version(s)",
            studioPickerUnavailableHint = "Local image file missing or unreadable",
            cropperTitle = "Crop & Adjust Image",
            cropperSubtitle = "Drag to position, pinch or slide to zoom, and rotate 90°. Originals are always preserved.",
            cropperAspectOriginal = "Original",
            cropperAspectFree = "Free",
            cropperAspect16_9 = "16:9",
            cropperAspect4_3 = "4:3",
            cropperAspect1_1 = "1:1",
            cropperFreeRatioLabel = "Free Frame Ratio",
            cropperRotateLeftDesc = "Rotate 90° Left",
            cropperRotateRightDesc = "Rotate 90° Right",
            cropperRotateLeftLabel = "-90°",
            cropperRotateRightLabel = "+90°",
            cropperLivePreviewLabel = "Live Cropped Preview",
            cropperApplyBtn = "Apply",
            galleryManagePhotosBtnTemplate = "Manage Photos ({count})",
            galleryDetailTitleTemplate = "Album: {album}",
            galleryDetailBackDesc = "Back to Photo Gallery",
            galleryAlbumCoverBadge = "Album Cover",
            gallerySetCoverBtn = "Set Cover",
            galleryEditMediaTitle = "Edit Photo / Video",
            galleryDetailEmpty = "No photos or videos in this album yet.",
            galleryMediaOrderLabel = "Display Order",
            galleryMoveUpDesc = "Move Photo Up",
            galleryMoveDownDesc = "Move Photo Down",
            galleryYouTubeVideoLabel = "YouTube Video",
            galleryCoverSetPublishedStatus = "Updated album cover.",
            galleryCoverSubmittedStatus = "Album cover update submitted for approval.",
            galleryOrderUpdatedStatus = "Updated photo order.",
            galleryOrderSubmittedStatus = "Photo order update submitted for approval."
        )
    )

    AppLanguage.TAMIL -> ManagementStrings(
        language = AppLanguage.TAMIL,
        auth = ManagementAuthStrings(
            portalTitle = "நிர்வாகக் கட்டுப்பாட்டுப் பலகை",
            dashboardSubtitle = "உள்ளூர் பள்ளி நிர்வாகம் & உள்ளடக்க மேலாண்மை",
            localTrialSecurityDisclaimer = "உள்ளூர் ஆஃப்லைன் சோதனை அங்கீகாரம் (PBKDF2 PIN). இது சாதன சோதனைக்கானது; சர்வர் பாதுகாப்பு அல்ல.",
            firstTimeSetupTitle = "முதன்மை நிர்வாகி முதல் அமைப்பு",
            firstTimeSetupSubtitle = "முதன்மை நிர்வாகி கணக்கை உருவாக்கவும். இயல்புநிலை PIN இல்லை.",
            defaultAdminDisplayName = "முதல்வர் / நிர்வாகி",
            displayNameLabel = "நிர்வாகி பெயர்",
            usernameLabel = "பயனர் பெயர்",
            pinLabel = "PIN (குறைந்தது 6 இலக்கங்கள்)",
            confirmPinLabel = "PIN உறுதிப்படுத்தல்",
            pinRuleHint = "குறைந்தது 6 எண்களை உள்ளிடவும்.",
            createMasterAdminBtn = "முதன்மை நிர்வாகியை உருவாக்கு",
            recoveryKeyTitle = "மீட்புக் குறியீட்டைச் சேமிக்கவும்",
            recoveryKeySubtitle = "மறந்த PIN-ஐ மீட்டெடுக்க இந்த ஒருமுறைக் குறியீடு அவசியம்.",
            recoveryKeySavedBtn = "சேமித்துவிட்டேன் — பலகைக்குச் செல்",
            rotatedRecoveryKeyPrefix = "புதிய சுழற்சி மீட்புக் குறியீடு:",
            loginTitle = "நிர்வாக உள்நுழைவு",
            loginSubtitle = "நிர்வாகி அல்லது பணியாளர் கணக்கில் உள்நுழையவும்.",
            loginBtn = "உள்நுழைக",
            forgotPinBtn = "முதன்மை PIN மீட்பு",
            recoverPinTitle = "முதன்மை PIN மீட்பு",
            recoveryCodeInputLabel = "மீட்புக் குறியீடு",
            newPinLabel = "புதிய PIN (குறைந்தது 6 இலக்கங்கள்)",
            resetPinBtn = "PIN மாற்றுக",
            logoutBtn = "பூட்டி வெளியேறு",
            returnPublicBtn = "பொது செயலி",
            backToDashboardDesc = "பலகைக்குத் திரும்பு",
            togglePinVisibilityDesc = "PIN-ஐக் காட்டு / மறை",
            togglePasswordVisibilityDesc = "கடவுச்சொல்லைக் காட்டு / மறை",
            masterAdminBadge = "முதன்மை நிர்வாகி",
            superOwnerBadge = "சிறப்பு பொறுப்பாளர்",
            staffBadge = "பணியாளர்",
            notificationsLabel = "அறிவிப்புகள்",
            globalSearchHint = "அனுமதிக்கப்பட்ட பிரிவுகளில் தேடு..."
        ),
        actions = ManagementActionsAndStatusStrings(
            cancelBtn = "ரத்து செய்",
            closeBtn = "மூடு",
            okBtn = "சரி",
            dismissBtn = "மறை",
            saveChangesBtn = "மாற்றங்களைச் சேமி",
            createAccountBtn = "கணக்கை உருவாக்கு",
            saveDraftBtn = "வரைவாக சேமி",
            submitForApprovalBtn = "ஒப்புதலுக்கு அனுப்பு",
            publishNowBtn = "இப்போதே வெளியிடு",
            publishBtn = "வெளியிடு",
            unpublishBtn = "வெளியீட்டை நிறுத்து",
            approveAndPublishBtn = "ஒப்புதல் & வெளியிடு",
            approveOnlyBtn = "ஒப்புதல் மட்டும்",
            rejectBtn = "நிராகரி",
            rejectionReasonHint = "நிராகரிப்பு காரணம் (கட்டாயம்)",
            deleteBtn = "நீக்கு",
            editBtn = "திருத்து",
            addNewBtn = "புதிதாகச் சேர்",
            restoreVersionBtn = "பதிப்பை மீட்டெடு",
            statusPublished = "வெளியிடப்பட்டது",
            statusPendingApproval = "ஒப்புதல் நிலுவையில்",
            statusRejected = "நிராகரிக்கப்பட்டது",
            statusDraft = "வரைவு",
            statusApproved = "ஒப்புதலளிக்கப்பட்டது",
            statusUnpublished = "வெளியிடப்படவில்லை",
            deleteRecordTitle = "பதிவை நீக்கவா?",
            deleteRecordMsgTemplate = "\"{title}\" பதிவை நீக்க விரும்புகிறீர்களா?",
            deleteStaffTitle = "பணியாளர் கணக்கை நீக்கவா?",
            deleteStaffMsgTemplate = "\"{name}\" (@{username}) கணக்கை நிரந்தரமாக நீக்க விரும்புகிறீர்களா? இதை மீட்டெடுக்க முடியாது.",
            clearNotificationsTitle = "அனைத்து அறிவிப்புகளையும் அழிக்கவா?",
            clearNotificationsMsg = "அனைத்து உள்ளூர் நிர்வாக அறிவிப்புகளையும் அழிக்க விரும்புகிறீர்களா? இதை மீட்டெடுக்க முடியாது.",
            orderLabelTemplate = "வரிசை: {order}",
            noRecordsYet = "இன்னும் பதிவுகள் இல்லை.",
            noPermissionNotice = "இந்தச் செயலுக்கு உங்கள் கணக்கிற்கு அனுமதி இல்லை.",
            statusDraftDiscarded = "வரைவு நிராகரிக்கப்பட்டது",
            editorEditTab = "திருத்து",
            editorPreviewTab = "முன்னோட்டம்",
            editorUnsavedIndicator = "சேமிக்கப்படாத மாற்றங்கள்",
            editorResumingDraftTitleTemplate = "வரைவைத் தொடர்கிறது (v{ver})",
            editorPendingRevisionTitleTemplate = "ஒப்புதல் நிலுவையில் (v{ver})",
            editorDraftMetaTemplate = "{name} சேமித்தது • {time}",
            editorPendingPreservedHint = "ஒப்புதல் கிடைக்கும் வரை நேரலைப் பதிப்பு மாறாமல் இருக்கும்.",
            editorResetToLiveBtn = "நேரலைக்கு மீட்டமை",
            editorDiscardChangesTitle = "சேமிக்கப்படாத மாற்றங்களை நீக்கவா?",
            editorDiscardChangesMsg = "சேமிக்கப்படாத மாற்றங்கள் உள்ளன. இப்போது வெளியேறினால் அவை அழிக்கப்படும்.",
            editorKeepEditingBtn = "தொடர்ந்து திருத்து",
            editorDiscardBtn = "நீக்கி வெளியேறு",
            editorPreviewCaption = "பொது செயலி முன்னோட்டம் — வெளியிடப்பட்டதும் இது இவ்வாறு தோன்றும்."
        ),
        dashboard = ManagementBackupAndDashboardStrings(
            backupCreateTitle = "கடவுச்சொல்-பாதுகாக்கப்பட்ட காப்பு உருவாக்கு",
            backupRestoreTitle = "மறைகுறியாக்கப்பட்ட காப்பை மீட்டெடு",
            backupPasswordLabel = "காப்பு கடவுச்சொல் (குறைந்தது 6 எழுத்துக்கள்)",
            backupConfirmPasswordLabel = "கடவுச்சொல் உறுதிப்படுத்தல்",
            backupWarningNote = "இந்தக் கடவுச்சொல்லை நினைவில் கொள்ளுங்கள். கடவுச்சொல் இன்றி காப்பைத் திறக்க முடியாது. மீட்டெடுப்பு தற்போதைய உள்ளூர் தரவை மாற்றும்.",
            exportEncryptedBackupBtn = "காப்பு ஏற்றுமதி செய் (.chvbak)",
            selectBackupToInspectBtn = "காப்பைத் தேர்ந்தெடுத்து முன்னோட்டம் பார்",
            restorePreviewConfirmBtn = "உறுதிசெய்து மீட்டெடு",
            backupPreviewTitle = "காப்புக் கோப்பு முன்னோட்டம்",
            backupExportedTemplate = "ஏற்றுமதி நேரம்: {time}",
            backupAppVersionTemplate = "செயலி பதிப்பு: {ver} • திட்டம் v{schema}",
            backupAccountsCountTemplate = "கணக்குகள்: {count}",
            backupContentCountTemplate = "பள்ளி உள்ளடக்கப் பதிவுகள்: {count}",
            backupGalleryMediaCountTemplate = "தொகுப்பு உருப்படிகள்: {gallery} • மீடியா கோப்புகள்: {files}",
            backupHistoryLogCountTemplate = "வரலாறு & தணிக்கைப் பதிவுகள்: {count}",
            backupRestoreConfirmWarning = "எச்சரிக்கை: மீட்டெடுப்பை உறுதிசெய்தால் தற்போதைய உள்ளூர் பதிவுகளும் மீடியாவும் மாற்றப்படும்.",
            backupPasswordTooShortError = "காப்பு கடவுச்சொல் குறைந்தது 6 எழுத்துக்கள் இருக்க வேண்டும்.",
            backupPasswordMismatchError = "கடவுச்சொல்லும் உறுதிப்படுத்தலும் பொருந்தவில்லை.",
            backupRestorePasswordRequiredError = "காப்புக் கோப்பைத் தேர்ந்தெடுக்கும் முன் கடவுச்சொல்லை உள்ளிடவும்.",
            cardSchoolProfile = "பள்ளி சுயவிவரம்",
            cardLogoBanner = "சின்னம் & பேனர்",
            cardAboutSchool = "பள்ளி பற்றி",
            cardAcademics = "கல்வி",
            cardCourses = "மேல்நிலைப் பாடப்பிரிவுகள்",
            cardFacilities = "வசதிகள்",
            cardTransport = "போக்குவரத்து",
            cardNews = "செய்திகள்",
            cardAnnouncements = "அறிவிப்புகள்",
            cardPhotoGallery = "படத்தொகுப்பு",
            cardContactDetails = "தொடர்பு விவரங்கள்",
            cardSocialMedia = "சமூக ஊடகங்கள்",
            cardMediaStudio = "மீடியா ஸ்டுடியோ",
            cardStaffManagement = "பணியாளர் மேலாண்மை",
            cardApprovalCenter = "ஒப்புதல் மையம்",
            cardVersionHistory = "பதிப்பு வரலாறு",
            cardActivityLog = "செயல்பாட்டுப் பதிவு",
            cardNotifications = "அறிவிப்புகள்",
            cardGlobalSearch = "உலகளாவிய தேடல்",
            cardBackupRestore = "காப்பு & மீட்பு",
            cardAppSettings = "செயலி அமைப்புகள்",
            approvalPendingTabTemplate = "நிலுவையில் ({count})",
            approvalReviewedTabTemplate = "மதிப்பாய்வு செய்தவை ({count})",
            approvalSubmittedByTemplate = "சமர்ப்பித்தவர்: {name}",
            approvalPreviousPreservedNote = "மதிப்பாய்வின் போது முந்தைய நேரலைப் பதிப்பு பாதுகாக்கப்பட்டுள்ளது.",
            approvalRejectionReasonTemplate = "நிராகரிப்பு காரணம்: {reason}",
            approvalRejectDialogTitleTemplate = "\"{title}\" நிராகரி",
            approvalReasonRequiredError = "நிராகரிப்பதற்கு முன் தெளிவான காரணத்தை உள்ளிடவும்.",
            versionHeaderTemplate = "v{ver} — {title}",
            versionChangedByTemplate = "மாற்றியவர்: {name} • {time}",
            versionStatusTemplate = "நிலை: {status}",
            versionSnapshotPayload = "பதிப்புத் தரவு:",
            versionPreviousPayload = "முந்தைய பதிப்புத் தரவு:",
            versionRestoreAndPublishBtn = "மீட்டெடுத்து வெளியிடு",
            versionRestoreAsRevisionBtn = "ஒப்புதலுக்கு மீட்டெடு",
            versionSummaryByTemplate = "{summary} • {name}",
            auditAppendOnlyNotice = "மாற்ற முடியாத தணிக்கைப் பதிவு. பதிவுகளைத் திருத்தவோ நீக்கவோ முடியாது.",
            auditSearchLabel = "பயனர், செயல், பதிவைத் தேடு...",
            auditSortNewest = "வரிசை: புதியவை முதலில்",
            auditSortOldest = "வரிசை: பழையவை முதலில்",
            auditAllSections = "அனைத்துப் பிரிவுகள்",
            notificationsMarkAllRead = "அனைத்தையும் படித்ததாகக் குறி",
            notificationsClearAll = "அனைத்தையும் அழி",
            notificationsEmpty = "இன்னும் உள்ளூர் நிர்வாக அறிவிப்புகள் இல்லை.",
            notificationRead = "படித்தது",
            notificationUnread = "புதியது",
            notificationsDeviceAlertsTitle = "சாதன எச்சரிக்கை அறிவிப்புகள்",
            notificationsDeviceAlertsSubtitle = "ஒப்புதல்கள், காப்பு மற்றும் பாதுகாப்பு புதுப்பிப்புகளுக்கு உள்ளூர் Android அறிவிப்புகளைக் காட்டும். தொலைநிலை Push (FCM) கட்டமைக்கப்படவில்லை.",
            notificationsPermissionDeniedNotice = "இந்தச் சாதனத்தில் அறிவிப்பு அனுமதி மறுக்கப்பட்டது. நிகழ்வுகள் உள்ளூர் நிர்வாக இன்பாக்ஸில் தொடர்ந்து பதிவு செய்யப்படும்.",
            backendStatusCardTitle = "பின்தளம், ஒத்திசைவு & பாதுகாப்பு நிலை",
            backendStatusLocalActiveLabel = "செயலில் (உள்ளூர்)",
            backendStatusUnconfiguredLabel = "கட்டமைக்கப்படவில்லை",
            backendStatusAutoBackupLabel = "OS தானியங்கி காப்பு: உள்ளூர் தரவுத்தளம், விருப்பங்கள் மற்றும் PIN சரிபார்ப்பிகளுக்கு முடக்கப்பட்டது.",
            backendStatusRemoteSyncLabel = "தொலைநிலை மேக ஒத்திசைவு & Push (FCM): கட்டமைக்கப்படவில்லை",
            globalSearchNoResultsTemplate = "\"{query}\" என்பதற்கான பொருந்தும் பதிவுகள் இல்லை."
        ),
        staffMedia = ManagementStaffAndMediaStrings(
            staffCreateHeaderBtn = "பணியாளர் / சிறப்பு பொறுப்பாளர் கணக்கு உருவாக்கு",
            staffMasterOnlyNotice = "முதன்மை நிர்வாகி மட்டுமே பணியாளர் கணக்குகளை உருவாக்கவோ, மாற்றவோ, நீக்கவோ முடியும்.",
            staffCreateDialogTitle = "பணியாளர் / சிறப்பு பொறுப்பாளர் உருவாக்கு",
            staffEditDialogTitleTemplate = "{name} கணக்கைத் திருத்து",
            staffUsernameRequiredLabel = "பயனர் பெயர் *",
            staffDisplayNameRequiredLabel = "காட்சிப் பெயர் *",
            staffRoleStaff = "பணியாளர்",
            staffRoleSuperOwner = "சிறப்பு பொறுப்பாளர்",
            staffAccountActiveLabel = "கணக்கு செயல்பாட்டில் உள்ளது",
            staffInitialPinLabel = "தொடக்க PIN (குறைந்தது 6 இலக்கங்கள்) *",
            staffResetPinLabel = "PIN மாற்றுக (தொடர காலியாக விடவும்)",
            staffGrantAllBtn = "அனைத்தையும் வழங்கு",
            staffClearAllBtn = "அனைத்தையும் நீக்கு",
            staffGranularPermsTitle = "18-பிரிவு நுணுக்க அனுமதிகள்",
            staffPermissionsLegend = "பா = பார்வை, உரு = உருவாக்கு, தி = திருத்து, நீ = நீக்கு, வெ = வெளியிடு",
            staffPermViewCode = "பா",
            staffPermCreateCode = "உரு",
            staffPermEditCode = "தி",
            staffPermDeleteCode = "நீ",
            staffPermPublishCode = "வெ",
            staffActiveLabel = "செயலில்",
            staffDeactivatedLabel = "முடக்கப்பட்டது",
            staffSectionsCountTemplate = "{count} பிரிவுகள்",
            staffEditAccountDesc = "கணக்கைத் திருத்து",
            staffDeleteAccountDesc = "கணக்கை நீக்கு",
            staffUsernameDisplayRequiredError = "பயனர் பெயரும் காட்சிப் பெயரும் தேவை.",
            staffDisplayNameRequiredError = "காட்சிப் பெயர் தேவை.",
            staffPinInvalidError = "PIN குறைந்தது 6 எண்களாக இருக்க வேண்டும்.",
            mediaImportImageBtn = "படம் இறக்குமதி செய் (Photo Picker)",
            mediaAddYouTubeBtn = "YouTube இணைப்பு சேர்",
            mediaYouTubeDialogTitle = "YouTube வீடியோ குறிப்பைச் சேர்",
            mediaYouTubeUrlLabel = "YouTube URL (https://youtube.com/...)",
            mediaVideoTitleLabel = "வீடியோ தலைப்பு *",
            mediaCaptionLabel = "விளக்கம்",
            mediaAddVideoBtn = "வீடியோ சேர்",
            mediaAssignAlbumTitleTemplate = "\"{title}\" படத்தொகுப்பில் சேர்",
            mediaCreateAlbumFirstNotice = "முதலில் படத்தொகுப்பு பிரிவில் ஒரு ஆல்பத்தை உருவாக்கவும்.",
            mediaDeleteTitle = "மீடியா கோப்பை நீக்கவா?",
            mediaAssetLabelTemplate = "கோப்பு: {title}",
            mediaInUseWarningTemplate = "பயன்பாட்டில் உள்ளது: {warnings}. நீக்கினால் கோப்பும் நீக்கப்படும்.",
            mediaMissingFileTemplate = "உள்ளூர் கோப்பு கிடைக்கவில்லை: {uri}",
            mediaAssignBannerBtn = "பேனராக அமை",
            mediaAssignAlbumBtn = "ஆல்பத்தில் சேர்",
            mediaReplaceBtn = "மாற்று",
            mediaYouTubePrefixTemplate = "YouTube: {url}",
            mediaSizeStatsTemplate = "{mime} • {w}x{h} • அசல் {orig}KB -> சுருக்கம் {opt}KB",
            mediaInvalidYouTubeError = "செல்லுபடியாகும் YouTube URL-ஐ உள்ளிடவும்.",
            mediaVideoTitleRequiredError = "வீடியோ தலைப்பு தேவை.",
            photoAdjTitle = "முகப்பு பேனர் படத்தைச் சரிசெய்",
            photoAdjSubtitle = "நகர்த்த இழுக்கவும், பெரிதாக்க பிஞ்ச் அல்லது ஸ்லைடரைப் பயன்படுத்தவும். அசல் படம் பாதுகாப்பாக வைக்கப்படும்.",
            photoAdjLivePreviewTitle = "முகப்பு பேனர் நேரடி முன்னோட்டம்",
            photoAdjZoomInDesc = "பெரிதாக்கு",
            photoAdjZoomOutDesc = "சிறிதாக்கு",
            photoAdjResetBtn = "மீட்டமை",
            photoAdjCropApplyBtn = "நறுக்கிப் பயன்படுத்து",
            photoAdjCropFailedError = "படத்தை நறுக்கிச் சேமிக்க முடியவில்லை.",
            photoAdjImageDesc = "சரிசெய்யக்கூடிய பேனர் படம்"
        ),
        profileHero = ManagementProfileAndHeroStrings(
            profileOfficialNameLabel = "அதிகாரப்பூர்வ பள்ளி பெயர் *",
            profileShortNameLabel = "சுருக்கப் பெயர் *",
            profileMottoLabel = "குறிக்கோள் *",
            profileCampusAddressLabel = "பள்ளி முகவரி *",
            profileAffiliationLabel = "இணைப்பு / குறியீடு (விருப்பம்)",
            profileMediumGradesLabel = "பயிற்று மொழி & வகுப்புகள் குறிப்பு",
            profileWelcomeIntroLabel = "முகப்பு வரவேற்பு அறிமுகம்",
            profilePrincipalNameLabel = "முதல்வர் பெயர்",
            profileVisionLabel = "பள்ளியின் தொலைநோக்கு",
            profileMissionLabel = "பள்ளியின் நோக்கம்",
            profileHistoryLabel = "பள்ளியின் வரலாறு",
            contactAddressLabel = "முகவரி *",
            contactPhoneLabel = "அதிகாரப்பூர்வ தொலைபேசி",
            contactEmailLabel = "அதிகாரப்பூர்வ மின்னஞ்சல்",
            contactWebsiteLabel = "அதிகாரப்பூர்வ இணையதளம்",
            contactOfficeHoursLabel = "அலுவலக நேரம்",
            contactMapQueryLabel = "Google Maps முகவரி / ஆயத்தொலைவு",
            profileRequiredFieldsError = "பள்ளி பெயர், சுருக்கப் பெயர், குறிக்கோள் மற்றும் முகவரி ஆகியவை தேவை.",
            contactAddressRequiredError = "முகவரி தேவை.",
            contactInvalidPhoneError = "சரியான தொலைபேசி எண்ணை உள்ளிடவும் (7–15 இலக்கங்கள்; +, இடைவெளி, (), அல்லது -).",
            contactInvalidEmailError = "சரியான மின்னஞ்சல் முகவரியை உள்ளிடவும் (எ.கா., office@school.edu).",
            profileInvalidWebsiteError = "இணையதள முகவரி http:// அல்லது https:// உடன் தொடங்க வேண்டும்.",
            profileShortNamePreviewLabel = "சுருக்கப் பெயர்",
            profileAffiliationPreviewLabel = "இணைப்பு / குறியீடு",
            profileWelcomePreviewLabel = "முகப்பு வரவேற்பு அறிமுகம்",
            changeSummaryUpdatedTemplate = "{section} புதுப்பிக்கப்பட்டது",
            heroAddDialogTitle = "பள்ளி பேனர் படம் சேர்",
            heroEditDialogTitle = "பள்ளி பேனர் படம் திருத்து",
            heroPickPhotoBtn = "படம் தேர்ந்தெடு (Photo Picker)",
            heroAdjustCropBtn = "பேனர் சரிசெய் / நறுக்கு / முன்னோட்டம்",
            heroCaptionOptionalLabel = "தலைப்பு (விருப்பம்)",
            heroDisplayOrderLabel = "காட்சி வரிசை",
            heroDefaultTitle = "பள்ளி பேனர்",
            heroBannerCardTitleTemplate = "பள்ளி பேனர் #{id}",
            heroImageRequiredError = "முதலில் ஒரு படத்தைத் தேர்ந்தெடுக்கவும்.",
            heroImageLoadFailedError = "தேர்ந்தெடுத்த படத்தை ஏற்ற முடியவில்லை."
        ),
        content = ManagementSectionEditorsStrings(
            academicsAddTitle = "கல்வித் தகவல் சேர்",
            academicsEditTitle = "கல்வித் தகவல் திருத்து",
            fieldTitleRequiredLabel = "தலைப்பு *",
            fieldCategoryRequiredLabel = "வகை *",
            fieldDescriptionRequiredLabel = "விளக்கம் *",
            academicsRequiredError = "தலைப்பு, வகை மற்றும் விளக்கம் ஆகியவை தேவை.",
            coursesAddTitle = "மேல்நிலைப் பாடப்பிரிவு சேர்",
            coursesEditTitle = "பாடப்பிரிவு திருத்து",
            courseGroupCodeRequiredLabel = "பிரிவு குறியீடு (எ.கா. Group I) *",
            courseStreamTitleRequiredLabel = "பாடப்பிரிவு தலைப்பு *",
            courseSubjectsRequiredLabel = "பாடங்களின் சுருக்கம் *",
            courseMediumRequiredLabel = "பயிற்று மொழி *",
            courseClassLevelLabel = "வகுப்புகள் (எ.கா. XI & XII)",
            courseEligibilityLabel = "தகுதி குறிப்பு",
            coursesRequiredError = "பிரிவு குறியீடு, தலைப்பு, பாடங்கள் மற்றும் மொழி ஆகியவை தேவை.",
            facilitiesAddTitle = "வசதி சேர்",
            facilitiesEditTitle = "வசதி திருத்து",
            facilityNameRequiredLabel = "வசதி பெயர் *",
            facilitySpecLabel = "திறன் / விவரக்குறிப்பு",
            facilitiesRequiredError = "வசதி பெயர், வகை மற்றும் விளக்கம் ஆகியவை தேவை.",
            transportAddTitle = "போக்குவரத்து வழித்தடம் சேர்",
            transportEditTitle = "போக்குவரத்து வழித்தடம் திருத்து",
            transportRouteNumberLabel = "வழித்தட எண் *",
            transportRouteNameLabel = "வழித்தட பெயர் *",
            transportStopsLabel = "நிறுத்தங்கள் சுருக்கம் *",
            transportMorningLabel = "காலை நேரம்",
            transportAfternoonLabel = "மாலை நேரம்",
            transportCoordinatorLabel = "ஒருங்கிணைப்பாளர் தொடர்பு",
            transportRequiredError = "வழித்தட எண், பெயர் மற்றும் நிறுத்தங்கள் தேவை.",
            transportCardTitleTemplate = "வழித்தடம் {num} — {name}",
            newsAddTitle = "செய்தி சேர்",
            newsEditTitle = "செய்தி திருத்து",
            newsHeadlineLabel = "தலைப்புச் செய்தி *",
            newsSummaryLabel = "சுருக்கம் *",
            newsFullBodyLabel = "முழுச் செய்தி *",
            newsDateTextLabel = "தேதி *",
            newsFeaturedCheckLabel = "சிறப்புச் செய்தி",
            newsFeaturedTag = "சிறப்பு",
            newsRequiredError = "தலைப்பு, சுருக்கம் மற்றும் முழுச் செய்தி தேவை.",
            announcementsAddTitle = "அறிவிப்பு சேர்",
            announcementsEditTitle = "அறிவிப்பு திருத்து",
            announcementTitleLabel = "அறிவிப்பு தலைப்பு *",
            announcementMessageLabel = "அதிகாரப்பூர்வ செய்தி *",
            announcementPriorityLabel = "முன்னுரிமை / வகை (எ.கா. சுற்றறிக்கை)",
            announcementIssuedDateLabel = "வெளியிடப்பட்ட தேதி",
            announcementDefaultPriority = "அறிவிப்பு",
            announcementsRequiredError = "தலைப்பும் செய்தியும் தேவை.",
            galleryCreateAlbumTitle = "புகைப்பட ஆல்பம் உருவாக்கு",
            galleryEditAlbumTitle = "புகைப்பட ஆல்பம் திருத்து",
            galleryAlbumTitleLabel = "ஆல்பம் தலைப்பு *",
            galleryAlbumCategoryLabel = "வகை (வளாகம் / நிகழ்வுகள் / கல்வி)",
            galleryDescriptionLabel = "விளக்கம்",
            galleryEventDateLabel = "நிகழ்வு தேதி",
            galleryAlbumRequiredError = "ஆல்பம் தலைப்பு தேவை.",
            galleryAlbumSubtitleTemplate = "{category} • {count} உருப்படிகள்",
            galleryAddMediaTitleTemplate = "{album} ஆல்பத்தில் மீடியா சேர்",
            galleryYouTubeSwitchLabel = "YouTube வீடியோ இணைப்பு (படத்திற்குப் பதிலாக)",
            galleryYouTubeUrlRequiredLabel = "YouTube URL *",
            galleryPickImageBtn = "படம் தேர்ந்தெடு (JPG/PNG/WEBP)",
            galleryReplaceSelectedImageBtn = "படம் தேர்ந்தெடுக்கப்பட்டது — மாற்ற தட்டவும்",
            galleryMediaTitleLabel = "படம் / வீடியோ தலைப்பு",
            galleryMediaDefaultTitle = "தொகுப்பு மீடியா",
            galleryMediaImageRequiredError = "முதலில் ஒரு படத்தைத் தேர்ந்தெடுக்கவும்.",
            galleryMediaProcessFailedError = "படத்தைச் செயலாக்க முடியவில்லை.",
            galleryAddMediaToAlbumBtnTemplate = "+ \"{album}\" இல் படம் / வீடியோ சேர்",
            socialAddTitle = "சமூக ஊடக இணைப்பு சேர்",
            socialEditTitle = "சமூக ஊடக இணைப்பு திருத்து",
            socialPlatformLabel = "தளம் (Instagram/Facebook/YouTube/X) *",
            socialHandleLabel = "பெயர் அல்லது கைப்பிடி *",
            socialUrlLabel = "அதிகாரப்பூர்வ URL (https://...) *",
            socialRequiredError = "தளம், பெயர் மற்றும் சரியான http(s) URL தேவை.",
            socialInvalidUrlError = "http:// அல்லது https:// உடன் தொடங்கும் சரியான அதிகாரப்பூர்வ URL ஐ உள்ளிடவும்."
        ),
        mediaUi = ManagementUniversalMediaStrings(
            fieldSchoolProfilePhotoLabel = "பள்ளி சுயவிவரம் / வளாகப் படம்",
            fieldAboutSchoolPhotoLabel = "பள்ளி பற்றி படம்",
            fieldHeroBannerPhotoLabel = "முகப்பு வளாக பேனர் படம் *",
            fieldFacilityPhotoLabel = "வசதிப் படம்",
            fieldNewsImageLabel = "செய்திப் படம்",
            fieldAnnouncementImageLabel = "அறிவிப்பு / சுற்றறிக்கை படம்",
            fieldGalleryPhotoLabel = "படத்தொகுப்பு படம்",
            pickFromDeviceBtn = "சாதனத்திலிருந்து தேர்ந்தெடு",
            pickFromStudioBtn = "மீடியா ஸ்டுடியோ",
            cropAdjustBtn = "நறுக்கு / சுழற்று",
            replaceImageBtn = "மாற்று",
            removeImageBtn = "அகற்று",
            importingImageText = "தேர்ந்தெடுத்த படம் செயலாக்கப்படுகிறது...",
            badgeOriginalLabel = "அசல்",
            badgeAdjustedLabel = "சரிசெய்யப்பட்டது",
            badgeUnavailableLabel = "கிடைக்கவில்லை",
            missingOrUnreadableError = "தேர்ந்தெடுத்த படக் கோப்பு காணவில்லை அல்லது படிக்க முடியவில்லை.",
            emptySelectionHint = "விருப்பப் படம். சாதனத்திலிருந்து அல்லது மீடியா ஸ்டுடியோவிலிருந்து தேர்ந்தெடுக்கவும்.",
            studioPickerTitle = "மீடியா ஸ்டுடியோவிலிருந்து தேர்ந்தெடு",
            studioPickerSearchHint = "படங்களைத் தேடு...",
            studioPickerEmpty = "மீடியா ஸ்டுடியோவில் படங்கள் எதுவும் இல்லை.",
            studioPickerUseOriginalBtn = "அசல்",
            studioPickerUseAdjustedBtn = "சரிசெய்யப்பட்டது",
            studioPickerUseOptimizedBtn = "மேம்படுத்தப்பட்டது",
            studioPickerAdjustedVersionsCountTemplate = "{count} சரிசெய்யப்பட்ட பதிப்புகள்",
            studioPickerUnavailableHint = "உள்ளூர் படக் கோப்பு காணவில்லை அல்லது படிக்க முடியவில்லை",
            cropperTitle = "படத்தை நறுக்கி சரிசெய்",
            cropperSubtitle = "நகர்த்த இழுக்கவும், பெரிதாக்கவும், 90° சுழற்றவும். அசல் படம் எப்போதும் பாதுகாக்கப்படும்.",
            cropperAspectOriginal = "அசல்",
            cropperAspectFree = "சுதந்திரம்",
            cropperAspect16_9 = "16:9",
            cropperAspect4_3 = "4:3",
            cropperAspect1_1 = "1:1",
            cropperFreeRatioLabel = "சுதந்திர விகிதம்",
            cropperRotateLeftDesc = "90° இடது சுழற்று",
            cropperRotateRightDesc = "90° வலது சுழற்று",
            cropperRotateLeftLabel = "-90°",
            cropperRotateRightLabel = "+90°",
            cropperLivePreviewLabel = "நேரடி நறுக்கல் முன்னோட்டம்",
            cropperApplyBtn = "பயன்படுத்து",
            galleryManagePhotosBtnTemplate = "படங்களை நிர்வகி ({count})",
            galleryDetailTitleTemplate = "ஆல்பம்: {album}",
            galleryDetailBackDesc = "படத்தொகுப்புக்குத் திரும்பு",
            galleryAlbumCoverBadge = "ஆல்பம் அட்டைப்படம்",
            gallerySetCoverBtn = "அட்டைப்படமாக அமை",
            galleryEditMediaTitle = "படம் / வீடியோ திருத்து",
            galleryDetailEmpty = "இந்த ஆல்பத்தில் இன்னும் படங்கள் அல்லது வீடியோக்கள் இல்லை.",
            galleryMediaOrderLabel = "காட்சி வரிசை",
            galleryMoveUpDesc = "மேலே நகர்த்து",
            galleryMoveDownDesc = "கீழே நகர்த்து",
            galleryYouTubeVideoLabel = "YouTube வீடியோ",
            galleryCoverSetPublishedStatus = "ஆல்பம் அட்டைப்படம் புதுப்பிக்கப்பட்டது.",
            galleryCoverSubmittedStatus = "ஆல்பம் அட்டைப்பட மாற்றம் ஒப்புதலுக்கு அனுப்பப்பட்டது.",
            galleryOrderUpdatedStatus = "படங்களின் வரிசை புதுப்பிக்கப்பட்டது.",
            galleryOrderSubmittedStatus = "பட வரிசை மாற்றம் ஒப்புதலுக்கு அனுப்பப்பட்டது."
        )
    )

    AppLanguage.HINDI -> ManagementStrings(
        language = AppLanguage.HINDI,
        auth = ManagementAuthStrings(
            portalTitle = "प्रबंधन डैशबोर्ड",
            dashboardSubtitle = "स्थानीय विद्यालय प्रशासन एवं सामग्री शासन",
            localTrialSecurityDisclaimer = "स्थानीय ऑफ़लाइन परीक्षण प्रमाणीकरण (PBKDF2 PIN)। यह डिवाइस डेमो के लिए है, सर्वर सुरक्षा के लिए नहीं।",
            firstTimeSetupTitle = "मास्टर एडमिन प्रथम-बार सेटअप",
            firstTimeSetupSubtitle = "मास्टर एडमिनिस्ट्रेटर खाता बनाएँ। कोई डिफ़ॉल्ट PIN मौजूद नहीं है।",
            defaultAdminDisplayName = "प्राचार्य / प्रशासक",
            displayNameLabel = "प्रशासक का नाम",
            usernameLabel = "उपयोगकर्ता नाम",
            pinLabel = "PIN (न्यूनतम 6 अंक)",
            confirmPinLabel = "PIN की पुष्टि करें",
            pinRuleHint = "कम से कम 6 अंक दर्ज करें।",
            createMasterAdminBtn = "मास्टर एडमिन बनाएँ",
            recoveryKeyTitle = "अपनी रिकवरी कुंजी सुरक्षित रखें",
            recoveryKeySubtitle = "भूले हुए PIN को स्थानीय रूप से रीसेट करने का यही एकमात्र तरीका है।",
            recoveryKeySavedBtn = "मैंने कुंजी सुरक्षित कर ली है — डैशबोर्ड खोलें",
            rotatedRecoveryKeyPrefix = "नई रोटेट की गई रिकवरी कुंजी:",
            loginTitle = "प्रबंधन पोर्टल लॉगिन",
            loginSubtitle = "मास्टर एडमिन, सुपर ओनर या स्टाफ़ खाते से साइन इन करें।",
            loginBtn = "डैशबोर्ड अनलॉक करें",
            forgotPinBtn = "मास्टर PIN पुनर्प्राप्त करें",
            recoverPinTitle = "मास्टर एडमिन PIN रिकवरी",
            recoveryCodeInputLabel = "वन-टाइम रिकवरी कुंजी",
            newPinLabel = "नया PIN (न्यूनतम 6 अंक)",
            resetPinBtn = "PIN रीसेट करें",
            logoutBtn = "लॉक और साइन आउट",
            returnPublicBtn = "सार्वजनिक ऐप",
            backToDashboardDesc = "डैशबोर्ड पर वापस जाएँ",
            togglePinVisibilityDesc = "PIN दिखाएँ या छिपाएँ",
            togglePasswordVisibilityDesc = "पासवर्ड दिखाएँ या छिपाएँ",
            masterAdminBadge = "मास्टर एडमिन",
            superOwnerBadge = "सुपर ओनर",
            staffBadge = "स्टाफ़",
            notificationsLabel = "सूचनाएँ",
            globalSearchHint = "अनुमत सामग्री, स्टाफ़, मीडिया खोजें..."
        ),
        actions = ManagementActionsAndStatusStrings(
            cancelBtn = "रद्द करें",
            closeBtn = "बंद करें",
            okBtn = "ठीक है",
            dismissBtn = "बंद करें",
            saveChangesBtn = "परिवर्तन सहेजें",
            createAccountBtn = "खाता बनाएँ",
            saveDraftBtn = "ड्राफ़्ट सहेजें",
            submitForApprovalBtn = "स्वीकृति हेतु भेजें",
            publishNowBtn = "अभी प्रकाशित करें",
            publishBtn = "प्रकाशित करें",
            unpublishBtn = "अप्रकाशित करें",
            approveAndPublishBtn = "स्वीकृत एवं प्रकाशित करें",
            approveOnlyBtn = "केवल स्वीकृत करें",
            rejectBtn = "अस्वीकार करें",
            rejectionReasonHint = "अस्वीकृति का कारण दर्ज करें (आवश्यक)",
            deleteBtn = "हटाएँ",
            editBtn = "संपादित करें",
            addNewBtn = "नया जोड़ें",
            restoreVersionBtn = "संस्करण पुनर्स्थापित करें",
            statusPublished = "प्रकाशित",
            statusPendingApproval = "स्वीकृति लंबित",
            statusRejected = "अस्वीकृत",
            statusDraft = "ड्राफ़्ट",
            statusApproved = "स्वीकृत",
            statusUnpublished = "अप्रकाशित",
            deleteRecordTitle = "रिकॉर्ड हटाएँ?",
            deleteRecordMsgTemplate = "क्या आप वाकई \"{title}\" को हटाना चाहते हैं?",
            deleteStaffTitle = "स्टाफ़ खाता हटाएँ?",
            deleteStaffMsgTemplate = "क्या आप वाकई \"{name}\" (@{username}) को स्थायी रूप से हटाना चाहते हैं? यह क्रिया पूर्ववत नहीं की जा सकती।",
            clearNotificationsTitle = "सभी सूचनाएँ हटाएँ?",
            clearNotificationsMsg = "क्या आप वाकई सभी स्थानीय प्रबंधन सूचनाएँ साफ़ करना चाहते हैं? इसे पूर्ववत नहीं किया जा सकता।",
            orderLabelTemplate = "क्रम: {order}",
            noRecordsYet = "अभी तक कोई रिकॉर्ड नहीं बनाया गया है।",
            noPermissionNotice = "आपके खाते को इस क्रिया की अनुमति नहीं है।",
            statusDraftDiscarded = "ड्राफ़्ट छोड़ा गया",
            editorEditTab = "संपादित करें",
            editorPreviewTab = "पूर्वावलोकन",
            editorUnsavedIndicator = "असहेजे गए परिवर्तन",
            editorResumingDraftTitleTemplate = "ड्राफ़्ट फिर से शुरू (v{ver})",
            editorPendingRevisionTitleTemplate = "स्वीकृति लंबित संशोधन (v{ver})",
            editorDraftMetaTemplate = "{name} द्वारा सहेजा गया • {time}",
            editorPendingPreservedHint = "स्वीकृति मिलने तक लाइव सार्वजनिक संस्करण अपरिवर्तित रहेगा।",
            editorResetToLiveBtn = "लाइव पर रीसेट करें",
            editorDiscardChangesTitle = "असहेजे गए परिवर्तन हटाएँ?",
            editorDiscardChangesMsg = "आपके पास असहेजे गए संपादन हैं। अभी वापस जाने पर ये परिवर्तन हट जाएँगे।",
            editorKeepEditingBtn = "संपादन जारी रखें",
            editorDiscardBtn = "हटाएँ",
            editorPreviewCaption = "सार्वजनिक ऐप पूर्वावलोकन — प्रकाशित होने पर यह सामग्री ऐसी दिखेगी।"
        ),
        dashboard = ManagementBackupAndDashboardStrings(
            backupCreateTitle = "पासवर्ड-सुरक्षित एन्क्रिप्टेड बैकअप बनाएँ",
            backupRestoreTitle = "एन्क्रिप्टेड बैकअप जांचें व पुनर्स्थापित करें",
            backupPasswordLabel = "बैकअप पासवर्ड (न्यूनतम 6 अक्षर)",
            backupConfirmPasswordLabel = "पासवर्ड की पुष्टि करें",
            backupWarningNote = "इस पासवर्ड को याद रखें। बिना पासवर्ड के बैकअप नहीं खुलेगा। पुनर्स्थापना से स्थानीय रिकॉर्ड बदल जाएँगे।",
            exportEncryptedBackupBtn = "एन्क्रिप्टेड बैकअप निर्यात करें (.chvbak)",
            selectBackupToInspectBtn = "बैकअप चुनें व पूर्वावलोकन करें",
            restorePreviewConfirmBtn = "पुष्टि करें व पुनर्स्थापित करें",
            backupPreviewTitle = "बैकअप संग्रह पूर्वावलोकन",
            backupExportedTemplate = "निर्यात समय: {time}",
            backupAppVersionTemplate = "ऐप संस्करण: {ver} • स्कीमा v{schema}",
            backupAccountsCountTemplate = "खाते: {count}",
            backupContentCountTemplate = "विद्यालय सामग्री रिकॉर्ड: {count}",
            backupGalleryMediaCountTemplate = "गैलरी आइटम: {gallery} • मीडिया फ़ाइलें: {files}",
            backupHistoryLogCountTemplate = "इतिहास और ऑडिट लॉग: {count}",
            backupRestoreConfirmWarning = "चेतावनी: पुनर्स्थापना की पुष्टि करने पर वर्तमान स्थानीय रिकॉर्ड और मीडिया सुरक्षित रूप से प्रतिस्थापित हो जाएँगे।",
            backupPasswordTooShortError = "बैकअप पासवर्ड कम से कम 6 अक्षरों का होना चाहिए।",
            backupPasswordMismatchError = "पासवर्ड और पुष्टि पासवर्ड मेल नहीं खाते।",
            backupRestorePasswordRequiredError = "बैकअप फ़ाइल चुनने से पहले कृपया पासवर्ड दर्ज करें।",
            cardSchoolProfile = "विद्यालय प्रोफ़ाइल",
            cardLogoBanner = "लोगो एवं बैनर",
            cardAboutSchool = "विद्यालय परिचय",
            cardAcademics = "शैक्षणिक",
            cardCourses = "उच्च माध्यमिक पाठ्यक्रम",
            cardFacilities = "सुविधाएँ",
            cardTransport = "परिवहन",
            cardNews = "समाचार",
            cardAnnouncements = "घोषणाएँ",
            cardPhotoGallery = "फ़ोटो गैलरी",
            cardContactDetails = "संपर्क विवरण",
            cardSocialMedia = "सोशल मीडिया",
            cardMediaStudio = "मीडिया स्टूडियो",
            cardStaffManagement = "स्टाफ़ प्रबंधन",
            cardApprovalCenter = "स्वीकृति केंद्र",
            cardVersionHistory = "संस्करण इतिहास",
            cardActivityLog = "गतिविधि लॉग",
            cardNotifications = "सूचनाएँ",
            cardGlobalSearch = "ग्लोबल सर्च",
            cardBackupRestore = "बैकअप व रीस्टोर",
            cardAppSettings = "ऐप सेटिंग्स",
            approvalPendingTabTemplate = "लंबित ({count})",
            approvalReviewedTabTemplate = "समीक्षित ({count})",
            approvalSubmittedByTemplate = "प्रस्तुतकर्ता: {name}",
            approvalPreviousPreservedNote = "समीक्षा लंबित रहने तक पिछला लाइव संस्करण सुरक्षित रखा गया है।",
            approvalRejectionReasonTemplate = "अस्वीकृति कारण: {reason}",
            approvalRejectDialogTitleTemplate = "\"{title}\" अस्वीकार करें",
            approvalReasonRequiredError = "अस्वीकार करने से पहले कृपया स्पष्ट कारण दर्ज करें।",
            versionHeaderTemplate = "v{ver} — {title}",
            versionChangedByTemplate = "बदला गया: {name} • {time}",
            versionStatusTemplate = "स्थिति: {status}",
            versionSnapshotPayload = "स्नैपशॉट डेटा:",
            versionPreviousPayload = "पिछला संस्करण डेटा:",
            versionRestoreAndPublishBtn = "पुनर्स्थापित करें व प्रकाशित करें",
            versionRestoreAsRevisionBtn = "समीक्षा हेतु पुनर्स्थापित करें",
            versionSummaryByTemplate = "{summary} • {name}",
            auditAppendOnlyNotice = "केवल-जोड़ने योग्य ऑडिट लॉग। रिकॉर्ड संपादित या हटाए नहीं जा सकते।",
            auditSearchLabel = "उपयोगकर्ता, क्रिया, रिकॉर्ड खोजें...",
            auditSortNewest = "क्रम: नवीनतम पहले",
            auditSortOldest = "क्रम: सबसे पुराने पहले",
            auditAllSections = "सभी अनुभाग",
            notificationsMarkAllRead = "सभी पढ़े हुए चिह्नित करें",
            notificationsClearAll = "सभी साफ़ करें",
            notificationsEmpty = "अभी तक कोई स्थानीय प्रबंधन सूचना नहीं है।",
            notificationRead = "पढ़ा गया",
            notificationUnread = "अपठित",
            notificationsDeviceAlertsTitle = "डिवाइस अलर्ट सूचनाएँ",
            notificationsDeviceAlertsSubtitle = "स्वीकृति, बैकअप और सुरक्षा अपडेट के लिए स्थानीय Android सूचनाएँ दिखाएँ। रिमोट पुश (FCM) कॉन्फ़िगर नहीं है।",
            notificationsPermissionDeniedNotice = "इस डिवाइस पर सूचना अनुमति अस्वीकृत है। ईवेंट स्थानीय प्रबंधन इनबॉक्स में सहेजे जाते रहेंगे।",
            backendStatusCardTitle = "बैकएंड, सिंक और सुरक्षा स्थिति",
            backendStatusLocalActiveLabel = "सक्रिय (स्थानीय)",
            backendStatusUnconfiguredLabel = "कॉन्फ़िगर नहीं",
            backendStatusAutoBackupLabel = "OS ऑटो बैकअप: स्थानीय डेटाबेस, प्राथमिकताओं और PIN सत्यापनकर्ता के लिए बंद है।",
            backendStatusRemoteSyncLabel = "रिमोट क्लाउड सिंक व पुश (FCM): कॉन्फ़िगर नहीं",
            globalSearchNoResultsTemplate = "\"{query}\" के लिए कोई अनुमत रिकॉर्ड नहीं मिला।"
        ),
        staffMedia = ManagementStaffAndMediaStrings(
            staffCreateHeaderBtn = "स्टाफ़ / सुपर ओनर खाता बनाएँ",
            staffMasterOnlyNotice = "केवल मास्टर एडमिन ही स्टाफ़ खाते बना, बदल या हटा सकते हैं।",
            staffCreateDialogTitle = "स्टाफ़ / सुपर ओनर बनाएँ",
            staffEditDialogTitleTemplate = "{name} संपादित करें",
            staffUsernameRequiredLabel = "उपयोगकर्ता नाम *",
            staffDisplayNameRequiredLabel = "प्रदर्शित नाम *",
            staffRoleStaff = "स्टाफ़",
            staffRoleSuperOwner = "सुपर ओनर",
            staffAccountActiveLabel = "खाता सक्रिय है",
            staffInitialPinLabel = "प्रारंभिक PIN (न्यूनतम 6 अंक) *",
            staffResetPinLabel = "PIN रीसेट करें (वर्तमान रखने हेतु खाली छोड़ें)",
            staffGrantAllBtn = "सभी अनुमति दें",
            staffClearAllBtn = "सभी हटाएँ",
            staffGranularPermsTitle = "18-अनुभाग विस्तृत अनुमतियाँ",
            staffPermissionsLegend = "दे = देखें, ब = बनाएँ, सं = संपादित, ह = हटाएँ, प्र = प्रकाशित",
            staffPermViewCode = "दे",
            staffPermCreateCode = "ब",
            staffPermEditCode = "सं",
            staffPermDeleteCode = "ह",
            staffPermPublishCode = "प्र",
            staffActiveLabel = "सक्रिय",
            staffDeactivatedLabel = "निष्क्रिय",
            staffSectionsCountTemplate = "{count} अनुभाग",
            staffEditAccountDesc = "खाता संपादित करें",
            staffDeleteAccountDesc = "खाता हटाएँ",
            staffUsernameDisplayRequiredError = "उपयोगकर्ता नाम और प्रदर्शित नाम आवश्यक हैं।",
            staffDisplayNameRequiredError = "प्रदर्शित नाम आवश्यक है।",
            staffPinInvalidError = "PIN कम से कम 6 अंकों का होना चाहिए।",
            mediaImportImageBtn = "फ़ोटो आयात करें (Photo Picker)",
            mediaAddYouTubeBtn = "YouTube लिंक जोड़ें",
            mediaYouTubeDialogTitle = "YouTube वीडियो संदर्भ जोड़ें",
            mediaYouTubeUrlLabel = "YouTube URL (https://youtube.com/...)",
            mediaVideoTitleLabel = "वीडियो शीर्षक *",
            mediaCaptionLabel = "कैप्शन",
            mediaAddVideoBtn = "वीडियो जोड़ें",
            mediaAssignAlbumTitleTemplate = "\"{title}\" को गैलरी एल्बम में जोड़ें",
            mediaCreateAlbumFirstNotice = "पहले फ़ोटो गैलरी में एक एल्बम बनाएँ।",
            mediaDeleteTitle = "मीडिया फ़ाइल हटाएँ?",
            mediaAssetLabelTemplate = "फ़ाइल: {title}",
            mediaInUseWarningTemplate = "उपयोग में: {warnings}। हटाने पर मूल फ़ाइल भी हट जाएगी।",
            mediaMissingFileTemplate = "स्थानीय फ़ाइल अनुपलब्ध या अपठनीय: {uri}",
            mediaAssignBannerBtn = "बैनर में लगाएँ",
            mediaAssignAlbumBtn = "एल्बम में जोड़ें",
            mediaReplaceBtn = "बदलें",
            mediaYouTubePrefixTemplate = "YouTube: {url}",
            mediaSizeStatsTemplate = "{mime} • {w}x{h} • मूल {orig}KB -> अनुकूलित {opt}KB",
            mediaInvalidYouTubeError = "मान्य YouTube URL दर्ज करें (youtube.com या youtu.be)।",
            mediaVideoTitleRequiredError = "वीडियो शीर्षक आवश्यक है।",
            photoAdjTitle = "होम हीरो बैनर फ़ोटो समायोजित करें",
            photoAdjSubtitle = "स्थिति बदलने के लिए खींचें, ज़ूम करने के लिए पिंच या स्लाइडर का उपयोग करें। मूल फ़ोटो सुरक्षित रहेगी।",
            photoAdjLivePreviewTitle = "होम हीरो बैनर लाइव पूर्वावलोकन",
            photoAdjZoomInDesc = "ज़ूम इन",
            photoAdjZoomOutDesc = "ज़ूम आउट",
            photoAdjResetBtn = "रीसेट करें",
            photoAdjCropApplyBtn = "क्रॉप और लागू करें",
            photoAdjCropFailedError = "छवि क्रॉप और सहेजने में विफल।",
            photoAdjImageDesc = "समायोज्य हीरो बैनर फ़ोटो"
        ),
        profileHero = ManagementProfileAndHeroStrings(
            profileOfficialNameLabel = "आधिकारिक विद्यालय नाम *",
            profileShortNameLabel = "संक्षिप्त नाम *",
            profileMottoLabel = "आदर्श वाक्य *",
            profileCampusAddressLabel = "परिसर पता *",
            profileAffiliationLabel = "संबद्धता / कोड (वैकल्पिक)",
            profileMediumGradesLabel = "माध्यम और कक्षाएँ विवरण",
            profileWelcomeIntroLabel = "होम स्वागत परिचय",
            profilePrincipalNameLabel = "प्राचार्य का नाम",
            profileVisionLabel = "विद्यालय दृष्टि (Vision)",
            profileMissionLabel = "विद्यालय उद्देश्य (Mission)",
            profileHistoryLabel = "विद्यालय इतिहास",
            contactAddressLabel = "पता *",
            contactPhoneLabel = "आधिकारिक फ़ोन",
            contactEmailLabel = "आधिकारिक ईमेल",
            contactWebsiteLabel = "आधिकारिक वेबसाइट URL",
            contactOfficeHoursLabel = "कार्यालय समय",
            contactMapQueryLabel = "Google Maps खोज / निर्देशांक",
            profileRequiredFieldsError = "विद्यालय नाम, संक्षिप्त नाम, आदर्श वाक्य और पता आवश्यक हैं।",
            contactAddressRequiredError = "पता आवश्यक है।",
            contactInvalidPhoneError = "मान्य फ़ोन नंबर दर्ज करें (7–15 अंक; +, स्पेस, (), या -)।",
            contactInvalidEmailError = "मान्य ईमेल पता दर्ज करें (उदा., office@school.edu)।",
            profileInvalidWebsiteError = "वेबसाइट URL http:// या https:// से शुरू होना चाहिए।",
            profileShortNamePreviewLabel = "संक्षिप्त नाम",
            profileAffiliationPreviewLabel = "संबद्धता / कोड",
            profileWelcomePreviewLabel = "होम स्वागत परिचय",
            changeSummaryUpdatedTemplate = "{section} अपडेट किया गया",
            heroAddDialogTitle = "परिसर बैनर फ़ोटो जोड़ें",
            heroEditDialogTitle = "परिसर बैनर फ़ोटो संपादित करें",
            heroPickPhotoBtn = "परिसर फ़ोटो चुनें (Photo Picker)",
            heroAdjustCropBtn = "बैनर समायोजित / क्रॉप / पूर्वावलोकन",
            heroCaptionOptionalLabel = "कैप्शन (वैकल्पिक)",
            heroDisplayOrderLabel = "प्रदर्शन क्रम",
            heroDefaultTitle = "परिसर बैनर",
            heroBannerCardTitleTemplate = "परिसर बैनर #{id}",
            heroImageRequiredError = "कृपया पहले एक छवि चुनें।",
            heroImageLoadFailedError = "चयनित छवि लोड करने में विफल।"
        ),
        content = ManagementSectionEditorsStrings(
            academicsAddTitle = "शैक्षणिक जानकारी जोड़ें",
            academicsEditTitle = "शैक्षणिक जानकारी संपादित करें",
            fieldTitleRequiredLabel = "शीर्षक *",
            fieldCategoryRequiredLabel = "श्रेणी *",
            fieldDescriptionRequiredLabel = "विवरण *",
            academicsRequiredError = "शीर्षक, श्रेणी और विवरण आवश्यक हैं।",
            coursesAddTitle = "उच्च माध्यमिक पाठ्यक्रम जोड़ें",
            coursesEditTitle = "पाठ्यक्रम संपादित करें",
            courseGroupCodeRequiredLabel = "समूह कोड (उदा. Group I) *",
            courseStreamTitleRequiredLabel = "संकाय शीर्षक *",
            courseSubjectsRequiredLabel = "विषय सारांश *",
            courseMediumRequiredLabel = "शिक्षण माध्यम *",
            courseClassLevelLabel = "कक्षाएँ (उदा. XI & XII)",
            courseEligibilityLabel = "पात्रता विवरण",
            coursesRequiredError = "समूह कोड, संकाय शीर्षक, विषय और माध्यम आवश्यक हैं।",
            facilitiesAddTitle = "सुविधा जोड़ें",
            facilitiesEditTitle = "सुविधा संपादित करें",
            facilityNameRequiredLabel = "सुविधा नाम *",
            facilitySpecLabel = "क्षमता / विशिष्टता",
            facilitiesRequiredError = "सुविधा नाम, श्रेणी और विवरण आवश्यक हैं।",
            transportAddTitle = "परिवहन मार्ग जोड़ें",
            transportEditTitle = "परिवहन मार्ग संपादित करें",
            transportRouteNumberLabel = "मार्ग संख्या *",
            transportRouteNameLabel = "मार्ग का नाम *",
            transportStopsLabel = "स्टॉप सारांश *",
            transportMorningLabel = "सुबह का समय",
            transportAfternoonLabel = "दोपहर का समय",
            transportCoordinatorLabel = "समन्वयक संपर्क",
            transportRequiredError = "मार्ग संख्या, नाम और स्टॉप आवश्यक हैं।",
            transportCardTitleTemplate = "मार्ग {num} — {name}",
            newsAddTitle = "समाचार लेख जोड़ें",
            newsEditTitle = "समाचार लेख संपादित करें",
            newsHeadlineLabel = "मुख्य शीर्षक *",
            newsSummaryLabel = "सारांश *",
            newsFullBodyLabel = "पूरा लेख *",
            newsDateTextLabel = "दिनांक *",
            newsFeaturedCheckLabel = "प्रमुख समाचार",
            newsFeaturedTag = "प्रमुख",
            newsRequiredError = "शीर्षक, सारांश और पूरा लेख आवश्यक हैं।",
            announcementsAddTitle = "घोषणा जोड़ें",
            announcementsEditTitle = "घोषणा संपादित करें",
            announcementTitleLabel = "घोषणा शीर्षक *",
            announcementMessageLabel = "आधिकारिक संदेश *",
            announcementPriorityLabel = "प्राथमिकता / लेबल (उदा. परिपत्र)",
            announcementIssuedDateLabel = "जारी दिनांक",
            announcementDefaultPriority = "घोषणा",
            announcementsRequiredError = "शीर्षक और संदेश आवश्यक हैं।",
            galleryCreateAlbumTitle = "फ़ोटो एल्बम बनाएँ",
            galleryEditAlbumTitle = "फ़ोटो एल्बम संपादित करें",
            galleryAlbumTitleLabel = "एल्बम शीर्षक *",
            galleryAlbumCategoryLabel = "श्रेणी (परिसर / कार्यक्रम / शैक्षणिक)",
            galleryDescriptionLabel = "विवरण",
            galleryEventDateLabel = "कार्यक्रम दिनांक",
            galleryAlbumRequiredError = "एल्बम शीर्षक आवश्यक है।",
            galleryAlbumSubtitleTemplate = "{category} • {count} आइटम",
            galleryAddMediaTitleTemplate = "{album} में मीडिया जोड़ें",
            galleryYouTubeSwitchLabel = "YouTube वीडियो लिंक (फ़ोटो के बजाय)",
            galleryYouTubeUrlRequiredLabel = "YouTube URL *",
            galleryPickImageBtn = "फ़ोटो चुनें (JPG/PNG/WEBP)",
            galleryReplaceSelectedImageBtn = "फ़ोटो चयनित — बदलने के लिए टैप करें",
            galleryMediaTitleLabel = "फ़ोटो / वीडियो शीर्षक",
            galleryMediaDefaultTitle = "गैलरी मीडिया",
            galleryMediaImageRequiredError = "कृपया पहले एक छवि चुनें।",
            galleryMediaProcessFailedError = "फ़ोटो प्रोसेस करने में असमर्थ।",
            galleryAddMediaToAlbumBtnTemplate = "+ \"{album}\" में फ़ोटो / वीडियो जोड़ें",
            socialAddTitle = "सोशल मीडिया लिंक जोड़ें",
            socialEditTitle = "सोशल मीडिया लिंक संपादित करें",
            socialPlatformLabel = "प्लेटफ़ॉर्म (Instagram/Facebook/YouTube/X) *",
            socialHandleLabel = "हैंडल या लेबल *",
            socialUrlLabel = "आधिकारिक URL (https://...) *",
            socialRequiredError = "प्लेटफ़ॉर्म, हैंडल और मान्य http(s) URL आवश्यक हैं।",
            socialInvalidUrlError = "http:// या https:// से शुरू होने वाला मान्य आधिकारिक URL दर्ज करें।"
        ),
        mediaUi = ManagementUniversalMediaStrings(
            fieldSchoolProfilePhotoLabel = "विद्यालय प्रोफ़ाइल / परिसर फ़ोटो",
            fieldAboutSchoolPhotoLabel = "विद्यालय परिचय फ़ोटो",
            fieldHeroBannerPhotoLabel = "मुख्य परिसर बैनर फ़ोटो *",
            fieldFacilityPhotoLabel = "सुविधा फ़ोटो",
            fieldNewsImageLabel = "समाचार छवि",
            fieldAnnouncementImageLabel = "घोषणा / परिपत्र छवि",
            fieldGalleryPhotoLabel = "गैलरी फ़ोटो",
            pickFromDeviceBtn = "डिवाइस से चुनें",
            pickFromStudioBtn = "मीडिया स्टूडियो",
            cropAdjustBtn = "क्रॉप / घुमाएँ",
            replaceImageBtn = "बदलें",
            removeImageBtn = "हटाएँ",
            importingImageText = "चयनित छवि प्रोसेस हो रही है...",
            badgeOriginalLabel = "मूल",
            badgeAdjustedLabel = "समायोजित",
            badgeUnavailableLabel = "अनुपलब्ध",
            missingOrUnreadableError = "चयनित छवि फ़ाइल अनुपलब्ध या अपठनीय है।",
            emptySelectionHint = "वैकल्पिक फ़ोटो। डिवाइस से चुनें या मीडिया स्टूडियो से मौजूदा छवि चुनें।",
            studioPickerTitle = "मीडिया स्टूडियो से छवि चुनें",
            studioPickerSearchHint = "प्रबंधित छवियाँ खोजें...",
            studioPickerEmpty = "मीडिया स्टूडियो में अभी कोई छवि उपलब्ध नहीं है।",
            studioPickerUseOriginalBtn = "मूल",
            studioPickerUseAdjustedBtn = "समायोजित",
            studioPickerUseOptimizedBtn = "अनुकूलित",
            studioPickerAdjustedVersionsCountTemplate = "{count} समायोजित संस्करण",
            studioPickerUnavailableHint = "स्थानीय फ़ोटो फ़ाइल अनुपलब्ध या अपठनीय है",
            cropperTitle = "छवि क्रॉप और समायोजित करें",
            cropperSubtitle = "स्थिति बदलने के लिए खींचें, ज़ूम करें और 90° घुमाएँ। मूल फ़ोटो हमेशा सुरक्षित रहती है।",
            cropperAspectOriginal = "मूल",
            cropperAspectFree = "मुक्त",
            cropperAspect16_9 = "16:9",
            cropperAspect4_3 = "4:3",
            cropperAspect1_1 = "1:1",
            cropperFreeRatioLabel = "मुक्त अनुपात",
            cropperRotateLeftDesc = "90° बाएँ घुमाएँ",
            cropperRotateRightDesc = "90° दाएँ घुमाएँ",
            cropperRotateLeftLabel = "-90°",
            cropperRotateRightLabel = "+90°",
            cropperLivePreviewLabel = "लाइव क्रॉप पूर्वावलोकन",
            cropperApplyBtn = "लागू करें",
            galleryManagePhotosBtnTemplate = "फ़ोटो प्रबंधित करें ({count})",
            galleryDetailTitleTemplate = "एल्बम: {album}",
            galleryDetailBackDesc = "फ़ोटो गैलरी पर वापस जाएँ",
            galleryAlbumCoverBadge = "एल्बम कवर",
            gallerySetCoverBtn = "कवर बनाएँ",
            galleryEditMediaTitle = "फ़ोटो / वीडियो संपादित करें",
            galleryDetailEmpty = "इस एल्बम में अभी कोई फ़ोटो या वीडियो नहीं है।",
            galleryMediaOrderLabel = "प्रदर्शन क्रम",
            galleryMoveUpDesc = "ऊपर ले जाएँ",
            galleryMoveDownDesc = "नीचे ले जाएँ",
            galleryYouTubeVideoLabel = "YouTube वीडियो",
            galleryCoverSetPublishedStatus = "एल्बम कवर अपडेट किया गया।",
            galleryCoverSubmittedStatus = "एल्बम कवर अपडेट स्वीकृति के लिए भेजा गया।",
            galleryOrderUpdatedStatus = "फ़ोटो क्रम अपडेट किया गया।",
            galleryOrderSubmittedStatus = "फ़ोटो क्रम अपडेट स्वीकृति के लिए भेजा गया।"
        )
    )
}
