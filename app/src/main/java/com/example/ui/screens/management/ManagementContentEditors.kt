package com.example.ui.screens.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.media.CropAspectRatioPreset
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.PublishSaveMode
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.HomeHeroBannerPreviewFrame
import com.example.ui.components.SchoolBrandHeaderCard
import com.example.ui.components.VerifiedFieldRow
import com.example.ui.screens.AboutSchoolDetailsCard
import com.example.ui.screens.AcademicInfoItemCard
import com.example.ui.screens.ContactDetailsInfoCard
import com.example.ui.screens.CourseGroupSlotCard
import com.example.ui.screens.FacilityItemCard
import com.example.ui.screens.OfficialCourseGroupSlot
import com.example.ui.screens.TransportRouteItemCard
import com.example.ui.screens.officialHigherSecondaryGroups
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SchoolProfileSectionEditor(
    managementViewModel: ManagementViewModel,
    mStrings: ManagementStrings,
    section: ManagementSection,
    onNavigateBack: () -> Unit = {},
    onRegisterNavigationGuard: (((() -> Unit) -> Unit)?) -> Unit = {}
) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val profile by managementViewModel.schoolProfile.collectAsStateWithLifecycle()
    val mediaStudioItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val canEdit = remember(authUiState, section) {
        managementViewModel.hasPermission(section, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState, section) {
        managementViewModel.hasPermission(section, PermissionAction.PUBLISH)
    }
    val scope = rememberCoroutineScope()

    var baselineProfile by remember(section) { mutableStateOf(profile) }
    var revisionInfo by remember(section) { mutableStateOf(EditorRevisionInfo(hasLiveVersion = true)) }

    var schoolName by remember(section) { mutableStateOf(profile.schoolName) }
    var shortName by remember(section) { mutableStateOf(profile.shortName) }
    var motto by remember(section) { mutableStateOf(profile.motto) }
    var addressLine by remember(section) { mutableStateOf(profile.addressLine) }
    var affiliationNumber by remember(section) { mutableStateOf(profile.affiliationNumber.orEmpty()) }
    var welcomeIntroduction by remember(section) { mutableStateOf(profile.welcomeIntroduction.orEmpty()) }
    var mediumNote by remember(section) { mutableStateOf(profile.mediumAndGradesNote.orEmpty()) }
    var profileImageUri by remember(section) {
        mutableStateOf(profile.profileImageUri?.trim()?.takeIf { it.isNotEmpty() })
    }

    var principalName by remember(section) { mutableStateOf(profile.principalName.orEmpty()) }
    var history by remember(section) { mutableStateOf(profile.history.orEmpty()) }
    var vision by remember(section) { mutableStateOf(profile.vision.orEmpty()) }
    var mission by remember(section) { mutableStateOf(profile.mission.orEmpty()) }
    var aboutImageUri by remember(section) {
        mutableStateOf(profile.aboutImageUri?.trim()?.takeIf { it.isNotEmpty() })
    }

    var phone by remember(section) { mutableStateOf(profile.phone.orEmpty()) }
    var email by remember(section) { mutableStateOf(profile.email.orEmpty()) }
    var website by remember(section) { mutableStateOf(profile.website.orEmpty()) }
    var officeHours by remember(section) { mutableStateOf(profile.officeHours.orEmpty()) }
    var mapQuery by remember(section) { mutableStateOf(profile.mapCoordinatesOrQuery.orEmpty()) }

    var validationError by remember(section) { mutableStateOf<String?>(null) }
    var showProfileRequiredErrors by remember(section) { mutableStateOf(false) }
    var contactAddressError by remember(section) { mutableStateOf<String?>(null) }
    var contactPhoneError by remember(section) { mutableStateOf<String?>(null) }
    var contactEmailError by remember(section) { mutableStateOf<String?>(null) }
    var contactWebsiteError by remember(section) { mutableStateOf<String?>(null) }

    fun clearAllValidationErrors() {
        validationError = null
        showProfileRequiredErrors = false
        contactAddressError = null
        contactPhoneError = null
        contactEmailError = null
        contactWebsiteError = null
    }

    fun hydrateSectionFromSnapshot(snapshot: SchoolProfileEntity) {
        baselineProfile = snapshot
        clearAllValidationErrors()
        when (section) {
            ManagementSection.SCHOOL_PROFILE -> {
                schoolName = snapshot.schoolName
                shortName = snapshot.shortName
                motto = snapshot.motto
                addressLine = snapshot.addressLine
                affiliationNumber = snapshot.affiliationNumber.orEmpty()
                welcomeIntroduction = snapshot.welcomeIntroduction.orEmpty()
                mediumNote = snapshot.mediumAndGradesNote.orEmpty()
                profileImageUri = snapshot.profileImageUri?.trim()?.takeIf { it.isNotEmpty() }
            }
            ManagementSection.ABOUT_SCHOOL -> {
                principalName = snapshot.principalName.orEmpty()
                history = snapshot.history.orEmpty()
                vision = snapshot.vision.orEmpty()
                mission = snapshot.mission.orEmpty()
                aboutImageUri = snapshot.aboutImageUri?.trim()?.takeIf { it.isNotEmpty() }
            }
            ManagementSection.CONTACT_DETAILS -> {
                addressLine = snapshot.addressLine
                phone = snapshot.phone.orEmpty()
                email = snapshot.email.orEmpty()
                website = snapshot.website.orEmpty()
                officeHours = snapshot.officeHours.orEmpty()
                mapQuery = snapshot.mapCoordinatesOrQuery.orEmpty()
            }
            else -> Unit
        }
    }

    LaunchedEffect(section) {
        val resolution = managementViewModel.resolveEditableRevision(section, 1L)
        val liveSnapshot = resolution.liveSnapshotJson?.let {
            runCatching { ContentSnapshotSerializer.schoolProfileFromJson(it) }.getOrNull()
        } ?: profile
        val editableSnapshot = resolution.editableSnapshotJson?.let {
            runCatching { ContentSnapshotSerializer.schoolProfileFromJson(it) }.getOrNull()
        } ?: liveSnapshot
        hydrateSectionFromSnapshot(editableSnapshot)
        revisionInfo = EditorRevisionInfo.fromResolution(resolution)
    }

    val cleanProfileImageUri = profileImageUri?.trim()?.takeIf { it.isNotEmpty() }
    val baselineProfileImageUri = baselineProfile.profileImageUri?.trim()?.takeIf { it.isNotEmpty() }
    val cleanAboutImageUri = aboutImageUri?.trim()?.takeIf { it.isNotEmpty() }
    val baselineAboutImageUri = baselineProfile.aboutImageUri?.trim()?.takeIf { it.isNotEmpty() }

    val isDirty = when (section) {
        ManagementSection.SCHOOL_PROFILE -> {
            schoolName != baselineProfile.schoolName ||
                shortName != baselineProfile.shortName ||
                motto != baselineProfile.motto ||
                addressLine != baselineProfile.addressLine ||
                affiliationNumber != baselineProfile.affiliationNumber.orEmpty() ||
                mediumNote != baselineProfile.mediumAndGradesNote.orEmpty() ||
                welcomeIntroduction != baselineProfile.welcomeIntroduction.orEmpty() ||
                cleanProfileImageUri != baselineProfileImageUri
        }
        ManagementSection.ABOUT_SCHOOL -> {
            principalName != baselineProfile.principalName.orEmpty() ||
                vision != baselineProfile.vision.orEmpty() ||
                mission != baselineProfile.mission.orEmpty() ||
                history != baselineProfile.history.orEmpty() ||
                cleanAboutImageUri != baselineAboutImageUri
        }
        ManagementSection.CONTACT_DETAILS -> {
            addressLine != baselineProfile.addressLine ||
                phone != baselineProfile.phone.orEmpty() ||
                email != baselineProfile.email.orEmpty() ||
                website != baselineProfile.website.orEmpty() ||
                officeHours != baselineProfile.officeHours.orEmpty() ||
                mapQuery != baselineProfile.mapCoordinatesOrQuery.orEmpty()
        }
        else -> false
    }

    UniversalContentEditorScaffold(
        mStrings = mStrings,
        title = mStrings.sectionLabel(section),
        canEdit = canEdit,
        canPublish = canPublish,
        isDirty = isDirty,
        isBusy = isBusy,
        isFullScreenDialog = false,
        subtitle = mStrings.sectionLabel(section),
        revisionInfo = revisionInfo,
        errorMessage = validationError ?: vmError,
        statusMessage = vmStatus,
        onClearError = {
            clearAllValidationErrors()
            managementViewModel.clearFeedback()
        },
        onClearStatus = { managementViewModel.clearFeedback() },
        onResetToLive = {
            managementViewModel.resetRevisionToLive(
                section = section,
                recordId = 1L,
                recordTitle = profile.shortName.ifBlank { baselineProfile.shortName }
            ) { liveJson ->
                val liveSnapshot = liveJson?.let {
                    runCatching { ContentSnapshotSerializer.schoolProfileFromJson(it) }.getOrNull()
                } ?: profile
                hydrateSectionFromSnapshot(liveSnapshot)
                revisionInfo = EditorRevisionInfo(kind = EditorRevisionKind.NONE, hasLiveVersion = true)
            }
        },
        onSaveAction = { mode: PublishSaveMode ->
            when (section) {
                ManagementSection.SCHOOL_PROFILE -> {
                    if (schoolName.isBlank() || shortName.isBlank() || motto.isBlank() || addressLine.isBlank()) {
                        showProfileRequiredErrors = true
                        validationError = mStrings.profileRequiredFieldsError
                        return@UniversalContentEditorScaffold
                    }
                    if (cleanProfileImageUri != null && !mediaManager.isManagedMediaFileReadable(cleanProfileImageUri)) {
                        validationError = mStrings.mediaUi.missingOrUnreadableError
                        return@UniversalContentEditorScaffold
                    }
                }
                ManagementSection.ABOUT_SCHOOL -> {
                    if (cleanAboutImageUri != null && !mediaManager.isManagedMediaFileReadable(cleanAboutImageUri)) {
                        validationError = mStrings.mediaUi.missingOrUnreadableError
                        return@UniversalContentEditorScaffold
                    }
                }
                ManagementSection.CONTACT_DETAILS -> {
                    val trimmedAddress = addressLine.trim()
                    val trimmedPhone = phone.trim()
                    val trimmedEmail = email.trim()
                    val trimmedWebsite = website.trim()

                    val addrErr = if (trimmedAddress.isBlank()) mStrings.contactAddressRequiredError else null
                    val phoneErr = if (trimmedPhone.isNotEmpty() && !isValidPhoneNumber(trimmedPhone)) mStrings.contactInvalidPhoneError else null
                    val emailErr = if (trimmedEmail.isNotEmpty() && !isValidEmailAddress(trimmedEmail)) mStrings.contactInvalidEmailError else null
                    val webErr = if (trimmedWebsite.isNotEmpty() && !isValidHttpUrl(trimmedWebsite)) mStrings.profileInvalidWebsiteError else null

                    contactAddressError = addrErr
                    contactPhoneError = phoneErr
                    contactEmailError = emailErr
                    contactWebsiteError = webErr

                    val firstErr = addrErr ?: phoneErr ?: emailErr ?: webErr
                    if (firstErr != null) {
                        validationError = firstErr
                        return@UniversalContentEditorScaffold
                    }
                }
                else -> Unit
            }
            clearAllValidationErrors()
            val updated = when (section) {
                ManagementSection.SCHOOL_PROFILE -> profile.copy(
                    schoolName = schoolName.trim(),
                    shortName = shortName.trim(),
                    motto = motto.trim(),
                    addressLine = addressLine.trim(),
                    affiliationNumber = affiliationNumber.trim().takeIf { it.isNotEmpty() },
                    welcomeIntroduction = welcomeIntroduction.trim().takeIf { it.isNotEmpty() },
                    mediumAndGradesNote = mediumNote.trim().takeIf { it.isNotEmpty() },
                    profileImageUri = cleanProfileImageUri
                )
                ManagementSection.ABOUT_SCHOOL -> profile.copy(
                    principalName = principalName.trim().takeIf { it.isNotEmpty() },
                    history = history.trim().takeIf { it.isNotEmpty() },
                    vision = vision.trim().takeIf { it.isNotEmpty() },
                    mission = mission.trim().takeIf { it.isNotEmpty() },
                    aboutImageUri = cleanAboutImageUri
                )
                ManagementSection.CONTACT_DETAILS -> profile.copy(
                    addressLine = addressLine.trim(),
                    phone = phone.trim().takeIf { it.isNotEmpty() },
                    email = email.trim().takeIf { it.isNotEmpty() },
                    website = website.trim().takeIf { it.isNotEmpty() },
                    officeHours = officeHours.trim().takeIf { it.isNotEmpty() },
                    mapCoordinatesOrQuery = mapQuery.trim().takeIf { it.isNotEmpty() }
                )
                else -> profile
            }
            managementViewModel.saveProfileSection(
                section = section,
                updatedProfile = updated,
                saveMode = mode,
                changeSummary = mStrings.changeSummaryUpdated(mStrings.sectionLabel(section)),
                onSuccess = {
                    hydrateSectionFromSnapshot(updated)
                    scope.launch {
                        val nextRes = managementViewModel.resolveEditableRevision(section, 1L)
                        revisionInfo = EditorRevisionInfo.fromResolution(nextRes)
                    }
                }
            )
        },
        onClose = onNavigateBack,
        onRegisterNavigationGuard = onRegisterNavigationGuard,
        previewContent = {
            when (section) {
                ManagementSection.SCHOOL_PROFILE -> {
                    val previewProfile = remember(
                        profile, schoolName, shortName, motto, addressLine,
                        affiliationNumber, mediumNote, welcomeIntroduction, cleanProfileImageUri
                    ) {
                        profile.copy(
                            schoolName = schoolName.trim(),
                            shortName = shortName.trim(),
                            motto = motto.trim(),
                            addressLine = addressLine.trim(),
                            affiliationNumber = affiliationNumber.trim().takeIf { it.isNotEmpty() },
                            mediumAndGradesNote = mediumNote.trim().takeIf { it.isNotEmpty() },
                            welcomeIntroduction = welcomeIntroduction.trim().takeIf { it.isNotEmpty() },
                            profileImageUri = cleanProfileImageUri
                        )
                    }
                    val isProfileImageReadable by produceState(
                        initialValue = cleanProfileImageUri != null,
                        cleanProfileImageUri
                    ) {
                        value = withContext(Dispatchers.IO) {
                            cleanProfileImageUri != null && mediaManager.isManagedMediaFileReadable(cleanProfileImageUri)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (cleanProfileImageUri != null) {
                            if (isProfileImageReadable) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(cleanProfileImageUri)
                                        .size(960)
                                        .crossfade(false)
                                        .build(),
                                    contentDescription = previewProfile.schoolName.ifBlank { mStrings.cardSchoolProfile },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag("profile_preview_image")
                                )
                            } else {
                                ErrorBanner(mStrings.mediaUi.missingOrUnreadableError)
                            }
                        }
                        SchoolBrandHeaderCard(
                            profile = previewProfile,
                            modifier = Modifier.testTag("profile_editor_live_preview")
                        )
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                VerifiedFieldRow(
                                    label = mStrings.profileShortNamePreviewLabel,
                                    value = previewProfile.shortName,
                                    icon = Icons.Outlined.Badge
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                VerifiedFieldRow(
                                    label = mStrings.profileAffiliationPreviewLabel,
                                    value = previewProfile.affiliationNumber,
                                    icon = Icons.Outlined.VerifiedUser
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                VerifiedFieldRow(
                                    label = mStrings.profileWelcomePreviewLabel,
                                    value = previewProfile.welcomeIntroduction,
                                    icon = Icons.Outlined.Info
                                )
                            }
                        }
                    }
                }
                ManagementSection.ABOUT_SCHOOL -> {
                    val previewProfile = remember(
                        profile, principalName, history, vision, mission, cleanAboutImageUri
                    ) {
                        profile.copy(
                            principalName = principalName.trim().takeIf { it.isNotEmpty() },
                            history = history.trim().takeIf { it.isNotEmpty() },
                            vision = vision.trim().takeIf { it.isNotEmpty() },
                            mission = mission.trim().takeIf { it.isNotEmpty() },
                            aboutImageUri = cleanAboutImageUri
                        )
                    }
                    val isAboutImageReadable by produceState(
                        initialValue = cleanAboutImageUri != null,
                        cleanAboutImageUri
                    ) {
                        value = withContext(Dispatchers.IO) {
                            cleanAboutImageUri != null && mediaManager.isManagedMediaFileReadable(cleanAboutImageUri)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (cleanAboutImageUri != null) {
                            if (isAboutImageReadable) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(cleanAboutImageUri)
                                        .size(960)
                                        .crossfade(false)
                                        .build(),
                                    contentDescription = mStrings.cardAboutSchool,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag("about_preview_image")
                                )
                            } else {
                                ErrorBanner(mStrings.mediaUi.missingOrUnreadableError)
                            }
                        }
                        SchoolBrandHeaderCard(profile = previewProfile)
                        AboutSchoolDetailsCard(
                            profile = previewProfile,
                            modifier = Modifier.testTag("about_editor_live_preview")
                        )
                    }
                }
                ManagementSection.CONTACT_DETAILS -> {
                    val previewProfile = remember(
                        profile, addressLine, phone, email, website, officeHours, mapQuery
                    ) {
                        profile.copy(
                            addressLine = addressLine.trim(),
                            phone = phone.trim().takeIf { it.isNotEmpty() },
                            email = email.trim().takeIf { it.isNotEmpty() },
                            website = website.trim().takeIf { it.isNotEmpty() },
                            officeHours = officeHours.trim().takeIf { it.isNotEmpty() },
                            mapCoordinatesOrQuery = mapQuery.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SchoolBrandHeaderCard(profile = previewProfile)
                        ContactDetailsInfoCard(
                            profile = previewProfile,
                            modifier = Modifier.testTag("contact_editor_live_preview")
                        )
                        if (!previewProfile.mapCoordinatesOrQuery.isNullOrBlank()) {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    VerifiedFieldRow(
                                        label = mStrings.contactMapQueryLabel,
                                        value = previewProfile.mapCoordinatesOrQuery,
                                        icon = Icons.Outlined.Map
                                    )
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    ) {
        when (section) {
            ManagementSection.SCHOOL_PROFILE -> {
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = {
                        schoolName = it
                        validationError = null
                    },
                    label = { Text(mStrings.profileOfficialNameLabel) },
                    enabled = canEdit && !isBusy,
                    isError = showProfileRequiredErrors && schoolName.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_school_name_input")
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = {
                        shortName = it
                        validationError = null
                    },
                    label = { Text(mStrings.profileShortNameLabel) },
                    enabled = canEdit && !isBusy,
                    isError = showProfileRequiredErrors && shortName.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_short_name_input")
                )
                OutlinedTextField(
                    value = motto,
                    onValueChange = {
                        motto = it
                        validationError = null
                    },
                    label = { Text(mStrings.profileMottoLabel) },
                    enabled = canEdit && !isBusy,
                    isError = showProfileRequiredErrors && motto.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_motto_input")
                )
                OutlinedTextField(
                    value = addressLine,
                    onValueChange = {
                        addressLine = it
                        validationError = null
                    },
                    label = { Text(mStrings.profileCampusAddressLabel) },
                    enabled = canEdit && !isBusy,
                    isError = showProfileRequiredErrors && addressLine.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_address_input")
                )
                OutlinedTextField(
                    value = affiliationNumber,
                    onValueChange = { affiliationNumber = it },
                    label = { Text(mStrings.profileAffiliationLabel) },
                    enabled = canEdit && !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("profile_affiliation_input")
                )
                OutlinedTextField(
                    value = mediumNote,
                    onValueChange = { mediumNote = it },
                    label = { Text(mStrings.profileMediumGradesLabel) },
                    enabled = canEdit && !isBusy,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("profile_medium_input")
                )
                OutlinedTextField(
                    value = welcomeIntroduction,
                    onValueChange = { welcomeIntroduction = it },
                    label = { Text(mStrings.profileWelcomeIntroLabel) },
                    enabled = canEdit && !isBusy,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("profile_welcome_input")
                )
                UniversalMediaField(
                    mStrings = mStrings,
                    label = mStrings.mediaUi.fieldSchoolProfilePhotoLabel,
                    selectedUri = profileImageUri,
                    onUriChanged = { profileImageUri = it; validationError = null },
                    mediaStudioItems = mediaStudioItems,
                    defaultAspectPreset = CropAspectRatioPreset.RATIO_4_3,
                    outputTag = "school_profile",
                    enabled = canEdit && !isBusy,
                    testTagPrefix = "profile_media"
                )
            }
            ManagementSection.ABOUT_SCHOOL -> {
                OutlinedTextField(
                    value = principalName,
                    onValueChange = { principalName = it },
                    label = { Text(mStrings.profilePrincipalNameLabel) },
                    enabled = canEdit && !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("about_principal_input")
                )
                OutlinedTextField(
                    value = vision,
                    onValueChange = { vision = it },
                    label = { Text(mStrings.profileVisionLabel) },
                    enabled = canEdit && !isBusy,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("about_vision_input")
                )
                OutlinedTextField(
                    value = mission,
                    onValueChange = { mission = it },
                    label = { Text(mStrings.profileMissionLabel) },
                    enabled = canEdit && !isBusy,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("about_mission_input")
                )
                OutlinedTextField(
                    value = history,
                    onValueChange = { history = it },
                    label = { Text(mStrings.profileHistoryLabel) },
                    enabled = canEdit && !isBusy,
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("about_history_input")
                )
                UniversalMediaField(
                    mStrings = mStrings,
                    label = mStrings.mediaUi.fieldAboutSchoolPhotoLabel,
                    selectedUri = aboutImageUri,
                    onUriChanged = { aboutImageUri = it; validationError = null },
                    mediaStudioItems = mediaStudioItems,
                    defaultAspectPreset = CropAspectRatioPreset.RATIO_4_3,
                    outputTag = "about_school",
                    enabled = canEdit && !isBusy,
                    testTagPrefix = "about_media"
                )
            }
            ManagementSection.CONTACT_DETAILS -> {
                OutlinedTextField(
                    value = addressLine,
                    onValueChange = {
                        addressLine = it
                        contactAddressError = null
                        validationError = null
                    },
                    label = { Text(mStrings.contactAddressLabel) },
                    enabled = canEdit && !isBusy,
                    isError = contactAddressError != null,
                    supportingText = contactAddressError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth().testTag("contact_address_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        contactPhoneError = null
                        validationError = null
                    },
                    label = { Text(mStrings.contactPhoneLabel) },
                    enabled = canEdit && !isBusy,
                    isError = contactPhoneError != null,
                    supportingText = contactPhoneError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        contactEmailError = null
                        validationError = null
                    },
                    label = { Text(mStrings.contactEmailLabel) },
                    enabled = canEdit && !isBusy,
                    isError = contactEmailError != null,
                    supportingText = contactEmailError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth().testTag("contact_email_input")
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = {
                        website = it
                        contactWebsiteError = null
                        validationError = null
                    },
                    label = { Text(mStrings.contactWebsiteLabel) },
                    enabled = canEdit && !isBusy,
                    isError = contactWebsiteError != null,
                    supportingText = contactWebsiteError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth().testTag("contact_website_input")
                )
                OutlinedTextField(
                    value = officeHours,
                    onValueChange = { officeHours = it },
                    label = { Text(mStrings.contactOfficeHoursLabel) },
                    enabled = canEdit && !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("contact_office_hours_input")
                )
                OutlinedTextField(
                    value = mapQuery,
                    onValueChange = { mapQuery = it },
                    label = { Text(mStrings.contactMapQueryLabel) },
                    enabled = canEdit && !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("contact_map_query_input")
                )
            }
            else -> Unit
        }
    }
}

private data class HeroBannerEditorSession(
    val target: CampusHeroPhotoEntity,
    val liveBaseline: CampusHeroPhotoEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun HeroBannerEditorScreen(
    managementViewModel: ManagementViewModel,
    mStrings: ManagementStrings
) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val photos by managementViewModel.allHeroPhotos.collectAsStateWithLifecycle()
    val mediaStudioItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<HeroBannerEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var imageUri by remember(session) {
            mutableStateOf<String?>(session.target.imageUri.takeIf { it.isNotBlank() })
        }
        var caption by remember(session) { mutableStateOf(session.target.caption.orEmpty()) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }

        val cleanImageUri = imageUri?.trim()?.takeIf { it.isNotEmpty() }
        val baselineUri = baseline.imageUri.takeIf { it.isNotBlank() }
        val isDirty = cleanImageUri != baselineUri ||
            caption != baseline.caption.orEmpty() ||
            orderText != baseline.displayOrder.toString()
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.LOGO_BANNER,
                    recordId = session.target.id,
                    recordTitle = session.target.caption?.takeIf { it.isNotBlank() }
                        ?: session.liveBaseline.caption?.takeIf { it.isNotBlank() }
                        ?: mStrings.heroDefaultTitle
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.heroPhotoFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    imageUri = reverted.imageUri.takeIf { uri -> uri.isNotBlank() }
                    caption = reverted.caption.orEmpty()
                    orderText = reverted.displayOrder.toString()
                    validationError = null
                    revisionInfo = EditorRevisionInfo(kind = EditorRevisionKind.NONE, hasLiveVersion = true)
                }
            }
        } else {
            null
        }

        UniversalContentEditorScaffold(
            mStrings = mStrings,
            title = if (session.target.id == 0L) mStrings.heroAddDialogTitle else mStrings.heroEditDialogTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardLogoBanner,
            revisionInfo = revisionInfo,
            errorMessage = validationError ?: vmError,
            statusMessage = vmStatus,
            onClearError = {
                validationError = null
                managementViewModel.clearFeedback()
            },
            onClearStatus = { managementViewModel.clearFeedback() },
            onResetToLive = resetHandler,
            onSaveAction = { mode: PublishSaveMode ->
                if (cleanImageUri == null) {
                    validationError = mStrings.heroImageRequiredError
                    return@UniversalContentEditorScaffold
                }
                if (!mediaManager.isManagedMediaFileReadable(cleanImageUri)) {
                    validationError = mStrings.mediaUi.missingOrUnreadableError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    imageUri = cleanImageUri,
                    caption = caption.trim().takeIf { it.isNotEmpty() },
                    displayOrder = orderText.toIntOrNull() ?: 0
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.LOGO_BANNER,
                    recordId = session.target.id,
                    recordTitle = next.caption ?: mStrings.heroDefaultTitle,
                    proposedJson = ContentSnapshotSerializer.heroPhotoToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardLogoBanner),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val previewCaption = caption.trim().takeIf { it.isNotEmpty() }
                val isPreviewReadable by produceState(initialValue = cleanImageUri != null, cleanImageUri) {
                    value = withContext(Dispatchers.IO) {
                        cleanImageUri != null && mediaManager.isManagedMediaFileReadable(cleanImageUri)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeHeroBannerPreviewFrame(
                        caption = previewCaption,
                        modifier = Modifier.testTag("hero_editor_live_preview")
                    ) {
                        if (cleanImageUri != null && isPreviewReadable) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(cleanImageUri)
                                    .size(960)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = previewCaption ?: mStrings.heroDefaultTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("hero_preview_image")
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (cleanImageUri == null) {
                                        mStrings.heroImageRequiredError
                                    } else {
                                        mStrings.mediaUi.missingOrUnreadableError
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = mStrings.orderSubtitle(orderText.toIntOrNull() ?: 0),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            UniversalMediaField(
                mStrings = mStrings,
                label = mStrings.mediaUi.fieldHeroBannerPhotoLabel,
                selectedUri = imageUri,
                onUriChanged = {
                    imageUri = it
                    validationError = null
                },
                mediaStudioItems = mediaStudioItems,
                defaultAspectPreset = CropAspectRatioPreset.RATIO_16_9,
                outputTag = "hero_banner",
                enabled = effectiveCanEdit && !isBusy,
                testTagPrefix = "hero_media"
            )
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text(mStrings.heroCaptionOptionalLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_editor_caption")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { ch -> ch.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_editor_order")
            )
        }
    }

    ManagedListScaffold(
        mStrings = mStrings,
        section = ManagementSection.LOGO_BANNER,
        managementViewModel = managementViewModel,
        items = photos,
        itemKey = { it.id },
        onAddNew = {
            editorSession = HeroBannerEditorSession(
                target = CampusHeroPhotoEntity(imageUri = ""),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = item.caption ?: mStrings.heroBannerCardTitle(item.id),
            subtitle = mStrings.orderSubtitle(item.displayOrder),
            body = item.imageUri,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.DELETE),
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.LOGO_BANNER, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.heroPhotoFromJson(it, publishNow = false).copy(id = item.id)
                        }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.heroPhotoFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id)
                        }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = HeroBannerEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = {
                managementViewModel.togglePublishState(ManagementSection.LOGO_BANNER, item.id, item.isPublished)
            },
            onDeleteConfirmed = {
                managementViewModel.deleteRecord(
                    ManagementSection.LOGO_BANNER,
                    item.id,
                    item.caption ?: mStrings.heroBannerCardTitle(item.id)
                )
            }
        )
    }
}

private data class AcademicEditorSession(
    val target: AcademicInfoEntity,
    val liveBaseline: AcademicInfoEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun AcademicsEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allAcademics.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<AcademicEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.ACADEMICS, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.ACADEMICS, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.ACADEMICS, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var title by remember(session) { mutableStateOf(session.target.title) }
        var category by remember(session) { mutableStateOf(session.target.category) }
        var description by remember(session) { mutableStateOf(session.target.description) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }

        val isDirty = title != baseline.title ||
            category != baseline.category ||
            description != baseline.description ||
            orderText != baseline.displayOrder.toString()
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.ACADEMICS,
                    recordId = session.target.id,
                    recordTitle = session.target.title.ifBlank { session.liveBaseline.title }
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.academicInfoFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    title = reverted.title
                    category = reverted.category
                    description = reverted.description
                    orderText = reverted.displayOrder.toString()
                    validationError = null
                    revisionInfo = EditorRevisionInfo(kind = EditorRevisionKind.NONE, hasLiveVersion = true)
                }
            }
        } else {
            null
        }

        UniversalContentEditorScaffold(
            mStrings = mStrings,
            title = if (session.target.id == 0L) mStrings.academicsAddTitle else mStrings.academicsEditTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardAcademics,
            revisionInfo = revisionInfo,
            errorMessage = validationError ?: vmError,
            statusMessage = vmStatus,
            onClearError = {
                validationError = null
                managementViewModel.clearFeedback()
            },
            onClearStatus = { managementViewModel.clearFeedback() },
            onResetToLive = resetHandler,
            onSaveAction = { mode ->
                if (title.isBlank() || category.isBlank() || description.isBlank()) {
                    validationError = mStrings.academicsRequiredError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    title = title.trim(),
                    category = category.trim(),
                    description = description.trim(),
                    displayOrder = orderText.toIntOrNull() ?: 0
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.ACADEMICS,
                    recordId = session.target.id,
                    recordTitle = next.title,
                    proposedJson = ContentSnapshotSerializer.academicInfoToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardAcademics),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val previewItem = remember(session.target.id, title, category, description, orderText) {
                    session.target.copy(
                        title = title.trim().ifBlank { mStrings.fieldTitleRequiredLabel },
                        category = category.trim().ifBlank { mStrings.fieldCategoryRequiredLabel },
                        description = description.trim().ifBlank { mStrings.fieldDescriptionRequiredLabel },
                        displayOrder = orderText.toIntOrNull() ?: 0,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AcademicInfoItemCard(
                        info = previewItem,
                        modifier = Modifier.testTag("academics_editor_live_preview")
                    )
                    Text(
                        text = mStrings.orderSubtitle(previewItem.displayOrder),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; validationError = null },
                label = { Text(mStrings.fieldTitleRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && title.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academics_editor_title")
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it; validationError = null },
                label = { Text(mStrings.fieldCategoryRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && category.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academics_editor_category")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; validationError = null },
                label = { Text(mStrings.fieldDescriptionRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && description.isBlank(),
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academics_editor_description")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { c -> c.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academics_editor_order")
            )
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.ACADEMICS, managementViewModel, items, { it.id },
        onAddNew = {
            editorSession = AcademicEditorSession(
                target = AcademicInfoEntity(title = "", category = "Curriculum", description = ""),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = item.title,
            subtitle = "${item.category} • ${mStrings.orderSubtitle(item.displayOrder)}",
            body = item.description,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = managementViewModel.hasPermission(ManagementSection.ACADEMICS, PermissionAction.DELETE),
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.ACADEMICS, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.academicInfoFromJson(it, publishNow = false).copy(id = item.id) }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.academicInfoFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id) }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = AcademicEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.ACADEMICS, item.id, item.isPublished) },
            onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.ACADEMICS, item.id, item.title) }
        )
    }
}

private data class CourseEditorSession(
    val target: HigherSecondaryCourseEntity,
    val liveBaseline: HigherSecondaryCourseEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun CoursesEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allCourses.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<CourseEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.HIGHER_SECONDARY_COURSES, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.HIGHER_SECONDARY_COURSES, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.HIGHER_SECONDARY_COURSES, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var groupCode by remember(session) { mutableStateOf(session.target.groupCode) }
        var streamTitle by remember(session) { mutableStateOf(session.target.streamTitle) }
        var subjects by remember(session) { mutableStateOf(session.target.subjectsSummary) }
        var medium by remember(session) { mutableStateOf(session.target.mediumOfInstruction) }
        var classLevel by remember(session) { mutableStateOf(session.target.classLevelLabel.orEmpty()) }
        var eligibility by remember(session) { mutableStateOf(session.target.eligibilityNote.orEmpty()) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }

        val isDirty = groupCode != baseline.groupCode ||
            streamTitle != baseline.streamTitle ||
            subjects != baseline.subjectsSummary ||
            medium != baseline.mediumOfInstruction ||
            classLevel != baseline.classLevelLabel.orEmpty() ||
            eligibility != baseline.eligibilityNote.orEmpty() ||
            orderText != baseline.displayOrder.toString()
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                val fallbackTitle = "${session.liveBaseline.groupCode} - ${session.liveBaseline.streamTitle}"
                val currentTitle = "${session.target.groupCode} - ${session.target.streamTitle}".trim().trim('-', ' ')
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.HIGHER_SECONDARY_COURSES,
                    recordId = session.target.id,
                    recordTitle = currentTitle.ifBlank { fallbackTitle }
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.courseFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    groupCode = reverted.groupCode
                    streamTitle = reverted.streamTitle
                    subjects = reverted.subjectsSummary
                    medium = reverted.mediumOfInstruction
                    classLevel = reverted.classLevelLabel.orEmpty()
                    eligibility = reverted.eligibilityNote.orEmpty()
                    orderText = reverted.displayOrder.toString()
                    validationError = null
                    revisionInfo = EditorRevisionInfo(kind = EditorRevisionKind.NONE, hasLiveVersion = true)
                }
            }
        } else {
            null
        }

        UniversalContentEditorScaffold(
            mStrings = mStrings,
            title = if (session.target.id == 0L) mStrings.coursesAddTitle else mStrings.coursesEditTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardCourses,
            revisionInfo = revisionInfo,
            errorMessage = validationError ?: vmError,
            statusMessage = vmStatus,
            onClearError = {
                validationError = null
                managementViewModel.clearFeedback()
            },
            onClearStatus = { managementViewModel.clearFeedback() },
            onResetToLive = resetHandler,
            onSaveAction = { mode ->
                if (groupCode.isBlank() || streamTitle.isBlank() || subjects.isBlank() || medium.isBlank()) {
                    validationError = mStrings.coursesRequiredError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    groupCode = groupCode.trim(),
                    streamTitle = streamTitle.trim(),
                    subjectsSummary = subjects.trim(),
                    mediumOfInstruction = medium.trim(),
                    classLevelLabel = classLevel.trim().takeIf { it.isNotEmpty() },
                    eligibilityNote = eligibility.trim().takeIf { it.isNotEmpty() },
                    displayOrder = orderText.toIntOrNull() ?: 0
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.HIGHER_SECONDARY_COURSES,
                    recordId = session.target.id,
                    recordTitle = "${next.groupCode} - ${next.streamTitle}",
                    proposedJson = ContentSnapshotSerializer.courseToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardCourses),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val previewCourse = remember(
                    session.target.id, groupCode, streamTitle, subjects, medium, classLevel, eligibility, orderText
                ) {
                    session.target.copy(
                        groupCode = groupCode.trim().ifBlank { "Group" },
                        streamTitle = streamTitle.trim().ifBlank { mStrings.courseStreamTitleRequiredLabel },
                        subjectsSummary = subjects.trim().ifBlank { mStrings.courseSubjectsRequiredLabel },
                        mediumOfInstruction = medium.trim().ifBlank { "English" },
                        classLevelLabel = classLevel.trim().takeIf { it.isNotEmpty() },
                        eligibilityNote = eligibility.trim().takeIf { it.isNotEmpty() },
                        displayOrder = orderText.toIntOrNull() ?: 0,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true
                    )
                }
                val previewSlot = remember(previewCourse) {
                    officialHigherSecondaryGroups.firstOrNull { slot ->
                        previewCourse.groupCode.equals(slot.groupCode, ignoreCase = true) ||
                            previewCourse.groupCode.equals(slot.groupLabel, ignoreCase = true) ||
                            previewCourse.streamTitle.contains(slot.title, ignoreCase = true)
                    } ?: OfficialCourseGroupSlot(
                        groupCode = previewCourse.groupCode,
                        groupLabel = previewCourse.groupCode,
                        title = previewCourse.streamTitle
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CourseGroupSlotCard(
                        slot = previewSlot,
                        verifiedCourse = previewCourse,
                        modifier = Modifier.testTag("courses_editor_live_preview")
                    )
                    Text(
                        text = mStrings.orderSubtitle(previewCourse.displayOrder),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = groupCode,
                onValueChange = { groupCode = it; validationError = null },
                label = { Text(mStrings.courseGroupCodeRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && groupCode.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("courses_editor_group_code")
            )
            OutlinedTextField(
                value = streamTitle,
                onValueChange = { streamTitle = it; validationError = null },
                label = { Text(mStrings.courseStreamTitleRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && streamTitle.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("courses_editor_stream_title")
            )
            OutlinedTextField(
                value = subjects,
                onValueChange = { subjects = it; validationError = null },
                label = { Text(mStrings.courseSubjectsRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && subjects.isBlank(),
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("courses_editor_subjects")
            )
            OutlinedTextField(
                value = medium,
                onValueChange = { medium = it; validationError = null },
                label = { Text(mStrings.courseMediumRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && medium.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("courses_editor_medium")
            )
            OutlinedTextField(
                value = classLevel,
                onValueChange = { classLevel = it },
                label = { Text(mStrings.courseClassLevelLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("courses_editor_class_level")
            )
            OutlinedTextField(
                value = eligibility,
                onValueChange = { eligibility = it },
                label = { Text(mStrings.courseEligibilityLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("courses_editor_eligibility")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { c -> c.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("courses_editor_order")
            )
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.HIGHER_SECONDARY_COURSES, managementViewModel, items, { it.id },
        onAddNew = {
            editorSession = CourseEditorSession(
                target = HigherSecondaryCourseEntity(
                    streamTitle = "", groupCode = "Group", subjectsSummary = "", mediumOfInstruction = "English"
                ),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = "${item.groupCode} — ${item.streamTitle}",
            subtitle = "${item.mediumOfInstruction} • ${mStrings.orderSubtitle(item.displayOrder)}",
            body = item.subjectsSummary,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = managementViewModel.hasPermission(ManagementSection.HIGHER_SECONDARY_COURSES, PermissionAction.DELETE),
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.HIGHER_SECONDARY_COURSES, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.courseFromJson(it, publishNow = false).copy(id = item.id) }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.courseFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id) }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = CourseEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.HIGHER_SECONDARY_COURSES, item.id, item.isPublished) },
            onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.HIGHER_SECONDARY_COURSES, item.id, item.streamTitle) }
        )
    }
}

private data class FacilityEditorSession(
    val target: FacilityEntity,
    val liveBaseline: FacilityEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun FacilitiesEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allFacilities.collectAsStateWithLifecycle()
    val mediaStudioItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<FacilityEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.FACILITIES, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.FACILITIES, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.FACILITIES, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var name by remember(session) { mutableStateOf(session.target.name) }
        var category by remember(session) { mutableStateOf(session.target.category) }
        var description by remember(session) { mutableStateOf(session.target.description) }
        var spec by remember(session) { mutableStateOf(session.target.capacityOrSpec.orEmpty()) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var imageUri by remember(session) { mutableStateOf(session.target.imageUri?.takeIf { it.isNotBlank() }) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }

        val cleanImageUri = imageUri?.trim()?.takeIf { it.isNotEmpty() }
        val baselineUri = baseline.imageUri?.trim()?.takeIf { it.isNotEmpty() }
        val isDirty = name != baseline.name ||
            category != baseline.category ||
            description != baseline.description ||
            spec != baseline.capacityOrSpec.orEmpty() ||
            orderText != baseline.displayOrder.toString() ||
            cleanImageUri != baselineUri
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.FACILITIES,
                    recordId = session.target.id,
                    recordTitle = session.target.name.ifBlank { session.liveBaseline.name }
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.facilityFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    name = reverted.name
                    category = reverted.category
                    description = reverted.description
                    spec = reverted.capacityOrSpec.orEmpty()
                    orderText = reverted.displayOrder.toString()
                    imageUri = reverted.imageUri?.takeIf { uri -> uri.isNotBlank() }
                    validationError = null
                    revisionInfo = EditorRevisionInfo(kind = EditorRevisionKind.NONE, hasLiveVersion = true)
                }
            }
        } else {
            null
        }

        UniversalContentEditorScaffold(
            mStrings = mStrings,
            title = if (session.target.id == 0L) mStrings.facilitiesAddTitle else mStrings.facilitiesEditTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardFacilities,
            revisionInfo = revisionInfo,
            errorMessage = validationError ?: vmError,
            statusMessage = vmStatus,
            onClearError = {
                validationError = null
                managementViewModel.clearFeedback()
            },
            onClearStatus = { managementViewModel.clearFeedback() },
            onResetToLive = resetHandler,
            onSaveAction = { mode ->
                if (name.isBlank() || category.isBlank() || description.isBlank()) {
                    validationError = mStrings.facilitiesRequiredError
                    return@UniversalContentEditorScaffold
                }
                if (cleanImageUri != null && !mediaManager.isManagedMediaFileReadable(cleanImageUri)) {
                    validationError = mStrings.mediaUi.missingOrUnreadableError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    name = name.trim(),
                    category = category.trim(),
                    description = description.trim(),
                    capacityOrSpec = spec.trim().takeIf { it.isNotEmpty() },
                    displayOrder = orderText.toIntOrNull() ?: 0,
                    imageUri = cleanImageUri
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.FACILITIES,
                    recordId = session.target.id,
                    recordTitle = next.name,
                    proposedJson = ContentSnapshotSerializer.facilityToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardFacilities),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val previewFacility = remember(session.target.id, name, category, description, spec, orderText, cleanImageUri) {
                    session.target.copy(
                        name = name.trim().ifBlank { mStrings.facilityNameRequiredLabel },
                        category = category.trim().ifBlank { mStrings.fieldCategoryRequiredLabel },
                        description = description.trim().ifBlank { mStrings.fieldDescriptionRequiredLabel },
                        capacityOrSpec = spec.trim().takeIf { it.isNotEmpty() },
                        displayOrder = orderText.toIntOrNull() ?: 0,
                        imageUri = cleanImageUri,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true
                    )
                }
                val isPreviewReadable by produceState(initialValue = cleanImageUri != null, cleanImageUri) {
                    value = withContext(Dispatchers.IO) {
                        cleanImageUri != null && mediaManager.isManagedMediaFileReadable(cleanImageUri)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (cleanImageUri != null) {
                        if (isPreviewReadable) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(cleanImageUri)
                                    .size(960)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = previewFacility.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("facility_preview_image")
                            )
                        } else {
                            ErrorBanner(mStrings.mediaUi.missingOrUnreadableError)
                        }
                    }
                    FacilityItemCard(
                        facility = previewFacility,
                        modifier = Modifier.testTag("facility_editor_live_preview"),
                        showImage = false
                    )
                    Text(
                        text = mStrings.orderSubtitle(previewFacility.displayOrder),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; validationError = null },
                label = { Text(mStrings.facilityNameRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("facility_editor_name")
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it; validationError = null },
                label = { Text(mStrings.fieldCategoryRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("facility_editor_category")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; validationError = null },
                label = { Text(mStrings.fieldDescriptionRequiredLabel) },
                enabled = effectiveCanEdit && !isBusy,
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("facility_editor_description")
            )
            OutlinedTextField(
                value = spec,
                onValueChange = { spec = it },
                label = { Text(mStrings.facilitySpecLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("facility_editor_spec")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { c -> c.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("facility_editor_order")
            )
            UniversalMediaField(
                mStrings = mStrings,
                label = mStrings.mediaUi.fieldFacilityPhotoLabel,
                selectedUri = imageUri,
                onUriChanged = {
                    imageUri = it
                    validationError = null
                },
                mediaStudioItems = mediaStudioItems,
                defaultAspectPreset = CropAspectRatioPreset.RATIO_4_3,
                outputTag = "facility",
                enabled = effectiveCanEdit && !isBusy,
                testTagPrefix = "facility_media"
            )
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.FACILITIES, managementViewModel, items, { it.id },
        onAddNew = {
            editorSession = FacilityEditorSession(
                target = FacilityEntity(name = "", category = "Academic", description = ""),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = item.name,
            subtitle = "${item.category} • ${mStrings.orderSubtitle(item.displayOrder)}",
            body = item.description,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = managementViewModel.hasPermission(ManagementSection.FACILITIES, PermissionAction.DELETE),
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.FACILITIES, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.facilityFromJson(it, publishNow = false).copy(id = item.id) }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.facilityFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id) }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = FacilityEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.FACILITIES, item.id, item.isPublished) },
            onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.FACILITIES, item.id, item.name) }
        )
    }
}

private data class TransportEditorSession(
    val target: TransportRouteEntity,
    val liveBaseline: TransportRouteEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun TransportEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allTransportRoutes.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<TransportEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.TRANSPORT, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.TRANSPORT, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.TRANSPORT, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var routeNumber by remember(session) { mutableStateOf(session.target.routeNumber) }
        var routeName by remember(session) { mutableStateOf(session.target.routeName) }
        var stops by remember(session) { mutableStateOf(session.target.stopsSummary) }
        var morning by remember(session) { mutableStateOf(session.target.morningTiming.orEmpty()) }
        var afternoon by remember(session) { mutableStateOf(session.target.afternoonTiming.orEmpty()) }
        var coordinator by remember(session) { mutableStateOf(session.target.coordinatorContact.orEmpty()) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }

        val isDirty = routeNumber != baseline.routeNumber ||
            routeName != baseline.routeName ||
            stops != baseline.stopsSummary ||
            morning != baseline.morningTiming.orEmpty() ||
            afternoon != baseline.afternoonTiming.orEmpty() ||
            coordinator != baseline.coordinatorContact.orEmpty() ||
            orderText != baseline.displayOrder.toString()
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                val fallbackTitle = "${session.liveBaseline.routeNumber} - ${session.liveBaseline.routeName}"
                val currentTitle = "${session.target.routeNumber} - ${session.target.routeName}".trim().trim('-', ' ')
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.TRANSPORT,
                    recordId = session.target.id,
                    recordTitle = currentTitle.ifBlank { fallbackTitle }
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.transportFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    routeNumber = reverted.routeNumber
                    routeName = reverted.routeName
                    stops = reverted.stopsSummary
                    morning = reverted.morningTiming.orEmpty()
                    afternoon = reverted.afternoonTiming.orEmpty()
                    coordinator = reverted.coordinatorContact.orEmpty()
                    orderText = reverted.displayOrder.toString()
                    validationError = null
                    revisionInfo = EditorRevisionInfo(kind = EditorRevisionKind.NONE, hasLiveVersion = true)
                }
            }
        } else {
            null
        }

        UniversalContentEditorScaffold(
            mStrings = mStrings,
            title = if (session.target.id == 0L) mStrings.transportAddTitle else mStrings.transportEditTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardTransport,
            revisionInfo = revisionInfo,
            errorMessage = validationError ?: vmError,
            statusMessage = vmStatus,
            onClearError = {
                validationError = null
                managementViewModel.clearFeedback()
            },
            onClearStatus = { managementViewModel.clearFeedback() },
            onResetToLive = resetHandler,
            onSaveAction = { mode ->
                if (routeNumber.isBlank() || routeName.isBlank() || stops.isBlank()) {
                    validationError = mStrings.transportRequiredError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    routeNumber = routeNumber.trim(),
                    routeName = routeName.trim(),
                    stopsSummary = stops.trim(),
                    morningTiming = morning.trim().takeIf { it.isNotEmpty() },
                    afternoonTiming = afternoon.trim().takeIf { it.isNotEmpty() },
                    coordinatorContact = coordinator.trim().takeIf { it.isNotEmpty() },
                    displayOrder = orderText.toIntOrNull() ?: 0
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.TRANSPORT,
                    recordId = session.target.id,
                    recordTitle = "${next.routeNumber} - ${next.routeName}",
                    proposedJson = ContentSnapshotSerializer.transportToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardTransport),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val previewRoute = remember(
                    session.target.id, routeNumber, routeName, stops, morning, afternoon, coordinator, orderText
                ) {
                    session.target.copy(
                        routeNumber = routeNumber.trim().ifBlank { "01" },
                        routeName = routeName.trim().ifBlank { mStrings.transportRouteNameLabel },
                        stopsSummary = stops.trim().ifBlank { mStrings.transportStopsLabel },
                        morningTiming = morning.trim().takeIf { it.isNotEmpty() },
                        afternoonTiming = afternoon.trim().takeIf { it.isNotEmpty() },
                        coordinatorContact = coordinator.trim().takeIf { it.isNotEmpty() },
                        displayOrder = orderText.toIntOrNull() ?: 0,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TransportRouteItemCard(
                        route = previewRoute,
                        modifier = Modifier.testTag("transport_editor_live_preview")
                    )
                    Text(
                        text = mStrings.orderSubtitle(previewRoute.displayOrder),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = routeNumber,
                onValueChange = { routeNumber = it; validationError = null },
                label = { Text(mStrings.transportRouteNumberLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && routeNumber.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_editor_route_number")
            )
            OutlinedTextField(
                value = routeName,
                onValueChange = { routeName = it; validationError = null },
                label = { Text(mStrings.transportRouteNameLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && routeName.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_editor_route_name")
            )
            OutlinedTextField(
                value = stops,
                onValueChange = { stops = it; validationError = null },
                label = { Text(mStrings.transportStopsLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError != null && stops.isBlank(),
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_editor_stops")
            )
            OutlinedTextField(
                value = morning,
                onValueChange = { morning = it },
                label = { Text(mStrings.transportMorningLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_editor_morning")
            )
            OutlinedTextField(
                value = afternoon,
                onValueChange = { afternoon = it },
                label = { Text(mStrings.transportAfternoonLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_editor_afternoon")
            )
            OutlinedTextField(
                value = coordinator,
                onValueChange = { coordinator = it },
                label = { Text(mStrings.transportCoordinatorLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_editor_coordinator")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { c -> c.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transport_editor_order")
            )
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.TRANSPORT, managementViewModel, items, { it.id },
        onAddNew = {
            editorSession = TransportEditorSession(
                target = TransportRouteEntity(routeNumber = "01", routeName = "", stopsSummary = ""),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = mStrings.transportCardTitle(item.routeNumber, item.routeName),
            subtitle = listOfNotNull(item.morningTiming, item.afternoonTiming, mStrings.orderSubtitle(item.displayOrder)).joinToString(" | "),
            body = item.stopsSummary,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = managementViewModel.hasPermission(ManagementSection.TRANSPORT, PermissionAction.DELETE),
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.TRANSPORT, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.transportFromJson(it, publishNow = false).copy(id = item.id) }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.transportFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id) }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = TransportEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.TRANSPORT, item.id, item.isPublished) },
            onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.TRANSPORT, item.id, item.routeName) }
        )
    }
}
