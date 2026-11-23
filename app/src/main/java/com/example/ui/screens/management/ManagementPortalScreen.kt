package com.example.ui.screens.management

import android.app.Activity
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureDialogProperties
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Approval
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.config.Version2FeatureConfig
import com.example.core.i18n.ManagementStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction
import com.example.data.repository.AdminDashboardCard
import com.example.data.v2.AcademicsV2AccessControl
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AdminV2Card
import com.example.ui.components.Version2UnavailableNoticeScreen
import com.example.ui.navigation.ChevalierPublicBottomBar
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.AcademyHeroBannerEditorDialog
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.SchoolDataViewModel

enum class ManagementDestination {
    DASHBOARD,
    ADMIN_HOME_HUB,
    ADMIN_ACADEMY_HUB,
    ADMIN_PARENT_HUB,
    ADMIN_COMMON_APP_HUB,
    ADMIN_TOOLS_HUB,
    ADMIN_UPDATES_HUB,
    SCHOOL_PROFILE,
    LOGO_BANNER,
    ABOUT_SCHOOL,
    ACADEMICS,
    COURSES,
    FACILITIES,
    TRANSPORT,
    NEWS,
    ANNOUNCEMENTS,
    PHOTO_GALLERY,
    CONTACT_DETAILS,
    SOCIAL_MEDIA,
    SCHOOL_POSTS,
    SCHOOL_HIGHLIGHTS,
    MEDIA_STUDIO,
    STAFF_MANAGEMENT,
    APPROVAL_CENTER,
    VERSION_HISTORY,
    ACTIVITY_LOG,
    NOTIFICATIONS,
    GLOBAL_SEARCH,
    BACKUP_RESTORE,
    APP_SETTINGS,
    PARENT_STUDENT_PROFILE,
    PARENT_EXAMS_RESULTS,
    PARENT_ATTENDANCE,
    PARENT_HOMEWORK_ASSIGNMENTS,
    PARENT_ACTIVITIES_MEDIA,
    PARENT_SCHOOL_COMMUNICATION,
    PARENT_TIMETABLE_CALENDAR,
    PARENT_TEACHER_COMMUNICATION,
    PARENT_LEAVE_REQUEST,
    PARENT_SCHOOL_CIRCULARS,
    PARENT_EMERGENCY_INFO,
    V2_HOME_HUB,
    V2_PARENT_HUB,
    V2_ACADEMICS_HUB,
    V2_ACADEMICS_CATEGORY,
    V2_ADMIN_HUB,
    V2_SCHOOL_SETTINGS_HUB,
    V2_PREVIEW_PUBLISH,
    V2_OWNER_CONTROL_CENTER
}

private fun ParentPortalAdminModule.toManagementDestination(): ManagementDestination = when (this) {
    ParentPortalAdminModule.STUDENT_PROFILE -> ManagementDestination.PARENT_STUDENT_PROFILE
    ParentPortalAdminModule.EXAMS_RESULTS -> ManagementDestination.PARENT_EXAMS_RESULTS
    ParentPortalAdminModule.ATTENDANCE -> ManagementDestination.PARENT_ATTENDANCE
    ParentPortalAdminModule.HOMEWORK_ASSIGNMENTS -> ManagementDestination.PARENT_HOMEWORK_ASSIGNMENTS
    ParentPortalAdminModule.ACTIVITIES_MEDIA -> ManagementDestination.PARENT_ACTIVITIES_MEDIA
    ParentPortalAdminModule.SCHOOL_COMMUNICATION -> ManagementDestination.PARENT_SCHOOL_COMMUNICATION
    ParentPortalAdminModule.TIMETABLE_CALENDAR -> ManagementDestination.PARENT_TIMETABLE_CALENDAR
    ParentPortalAdminModule.PARENT_TEACHER_COMMUNICATION -> ManagementDestination.PARENT_TEACHER_COMMUNICATION
    ParentPortalAdminModule.LEAVE_REQUEST -> ManagementDestination.PARENT_LEAVE_REQUEST
    ParentPortalAdminModule.SCHOOL_CIRCULARS -> ManagementDestination.PARENT_SCHOOL_CIRCULARS
    ParentPortalAdminModule.EMERGENCY_INFO -> ManagementDestination.PARENT_EMERGENCY_INFO
}

private fun ManagementDestination.toParentPortalAdminModule(): ParentPortalAdminModule? = when (this) {
    ManagementDestination.PARENT_STUDENT_PROFILE -> ParentPortalAdminModule.STUDENT_PROFILE
    ManagementDestination.PARENT_EXAMS_RESULTS -> ParentPortalAdminModule.EXAMS_RESULTS
    ManagementDestination.PARENT_ATTENDANCE -> ParentPortalAdminModule.ATTENDANCE
    ManagementDestination.PARENT_HOMEWORK_ASSIGNMENTS -> ParentPortalAdminModule.HOMEWORK_ASSIGNMENTS
    ManagementDestination.PARENT_ACTIVITIES_MEDIA -> ParentPortalAdminModule.ACTIVITIES_MEDIA
    ManagementDestination.PARENT_SCHOOL_COMMUNICATION -> ParentPortalAdminModule.SCHOOL_COMMUNICATION
    ManagementDestination.PARENT_TIMETABLE_CALENDAR -> ParentPortalAdminModule.TIMETABLE_CALENDAR
    ManagementDestination.PARENT_TEACHER_COMMUNICATION -> ParentPortalAdminModule.PARENT_TEACHER_COMMUNICATION
    ManagementDestination.PARENT_LEAVE_REQUEST -> ParentPortalAdminModule.LEAVE_REQUEST
    ManagementDestination.PARENT_SCHOOL_CIRCULARS -> ParentPortalAdminModule.SCHOOL_CIRCULARS
    ManagementDestination.PARENT_EMERGENCY_INFO -> ParentPortalAdminModule.EMERGENCY_INFO
    else -> null
}

@Composable
fun ManagementPortalScreen(
    managementViewModel: ManagementViewModel,
    mainViewModel: MainViewModel,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    onExitToPublicApp: () -> Unit,
    onOpenLanguageSelection: (() -> Unit)? = null,
    schoolDataViewModel: SchoolDataViewModel? = null,
    onSelectPublicTab: ((String) -> Unit)? = null
) {
    val authState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val statusMessage by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val errorMessage by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val mStrings = remember(currentLanguage) { managementStringsFor(currentLanguage) }

    when (val state = authState) {
        ManagementAuthUiState.Loading -> {
            BackHandler(onBack = onExitToPublicApp)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ManagementAuthUiState.FirstTimeSetup -> {
            BackHandler(onBack = onExitToPublicApp)
            MasterAdminSetupScreen(
                mStrings = mStrings,
                setupState = state,
                isBusy = isBusy,
                errorMessage = errorMessage,
                onSubmitSetup = { displayName, username, pin, confirmPin ->
                    managementViewModel.setupMasterAdmin(displayName, username, pin, confirmPin)
                },
                onRecoverySaved = {
                    managementViewModel.confirmRecoveryKeySavedAndEnterDashboard()
                },
                onBackToPublic = onExitToPublicApp
            )
        }

        is ManagementAuthUiState.LoginRequired -> {
            BackHandler(onBack = onExitToPublicApp)
            val accounts by managementViewModel.accountsWithPermissions.collectAsStateWithLifecycle()
            ManagementPinLoginScreen(
                mStrings = mStrings,
                activeUsernames = accounts.filter { it.account.isActive }.map { it.account.username to it.account.displayName },
                noticeMessage = state.noticeMessage,
                errorMessage = errorMessage,
                isBusy = isBusy,
                onClearFeedback = { managementViewModel.clearFeedback() },
                onLogin = { user, pin -> managementViewModel.loginWithPin(user, pin) },
                onRecoverMasterPin = { code, newPin, confirmPin, onIssued ->
                    managementViewModel.recoverMasterPin(code, newPin, confirmPin, onIssued)
                },
                onBackToPublic = onExitToPublicApp
            )
        }

        is ManagementAuthUiState.Authenticated -> {
            ManagementDashboardShell(
                session = state.session,
                managementViewModel = managementViewModel,
                mainViewModel = mainViewModel,
                schoolDataViewModel = schoolDataViewModel,
                mStrings = mStrings,
                currentLanguage = currentLanguage,
                currentThemeMode = currentThemeMode,
                statusMessage = statusMessage,
                errorMessage = errorMessage,
                isBusy = isBusy,
                onExitToPublicApp = onExitToPublicApp,
                onOpenLanguageSelection = onOpenLanguageSelection,
                onSelectPublicTab = onSelectPublicTab
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MasterAdminSetupScreen(
    mStrings: ManagementStrings,
    setupState: ManagementAuthUiState.FirstTimeSetup,
    isBusy: Boolean,
    errorMessage: String?,
    onSubmitSetup: (String, String, String, String) -> Unit,
    onRecoverySaved: () -> Unit,
    onBackToPublic: () -> Unit
) {
    val brand = LocalBrandPalette.current
    var displayName by remember { mutableStateOf(mStrings.defaultAdminDisplayName) }
    var username by remember { mutableStateOf("admin") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("master_admin_setup_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToPublic,
                        modifier = Modifier.testTag("setup_back_public_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = mStrings.returnPublicBtn)
                    }
                    Text(
                        text = mStrings.firstTimeSetupTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                EmblemHeaderCard()

                if (setupState.generatedRecoveryCode != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = brand.burgundy.copy(alpha = 0.09f)),
                        border = BorderStroke(1.5.dp, brand.royalGold),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_recovery_key_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = mStrings.recoveryKeyTitle,
                                tint = brand.burgundy,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = mStrings.recoveryKeyTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = mStrings.recoveryKeySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, brand.burgundy)
                            ) {
                                Text(
                                    text = setupState.generatedRecoveryCode,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = brand.burgundy,
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .testTag("generated_recovery_code_text")
                                )
                            }
                            Button(
                                onClick = onRecoverySaved,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.forestGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("confirm_recovery_saved_btn")
                            ) {
                                Text(mStrings.recoveryKeySavedBtn)
                            }
                        }
                    }
                } else {
                    Text(
                        text = mStrings.firstTimeSetupSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    DisclaimerCard(mStrings.localTrialSecurityDisclaimer)

                    errorMessage?.let { err ->
                        ErrorBanner(err)
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(mStrings.displayNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_display_name_input")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(mStrings.usernameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_username_input")
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) pin = it },
                        label = { Text(mStrings.pinLabel) },
                        supportingText = { Text(mStrings.pinRuleHint) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = mStrings.togglePinVisibilityDesc
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) confirmPin = it },
                        label = { Text(mStrings.confirmPinLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_confirm_pin_input")
                    )

                    Button(
                        onClick = { onSubmitSetup(displayName, username, pin, confirmPin) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.burgundy,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("create_master_admin_btn")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(mStrings.createMasterAdminBtn)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManagementPinLoginScreen(
    mStrings: ManagementStrings,
    activeUsernames: List<Pair<String, String>>,
    noticeMessage: String?,
    errorMessage: String?,
    isBusy: Boolean,
    onClearFeedback: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRecoverMasterPin: (String, String, String, (String) -> Unit) -> Unit,
    onBackToPublic: () -> Unit
) {
    val brand = LocalBrandPalette.current
    var username by remember(activeUsernames) {
        mutableStateOf(activeUsernames.firstOrNull()?.first ?: "admin")
    }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var rotatedRecoveryKey by remember { mutableStateOf<String?>(null) }

    if (showRecoveryDialog) {
        var recoveryCodeInput by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var showRecoveryPin by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                onClearFeedback()
                showRecoveryDialog = false
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.recoverPinTitle)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    errorMessage?.let { err ->
                        ErrorBanner(
                            message = err,
                            dismissLabel = mStrings.dismissBtn,
                            onDismiss = onClearFeedback
                        )
                    }
                    OutlinedTextField(
                        value = recoveryCodeInput,
                        onValueChange = { recoveryCodeInput = it },
                        label = { Text(mStrings.recoveryCodeInputLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("recover_code_input")
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) newPin = it },
                        label = { Text(mStrings.newPinLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (showRecoveryPin) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showRecoveryPin = !showRecoveryPin }) {
                                Icon(
                                    imageVector = if (showRecoveryPin) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = mStrings.togglePinVisibilityDesc
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("recover_new_pin_input")
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) confirmPin = it },
                        label = { Text(mStrings.confirmPinLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (showRecoveryPin) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("recover_confirm_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isBusy,
                    onClick = {
                        onRecoverMasterPin(recoveryCodeInput, newPin, confirmPin) { rotated ->
                            rotatedRecoveryKey = rotated
                            showRecoveryDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_recover_pin_btn")
                ) { Text(mStrings.resetPinBtn) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onClearFeedback()
                    showRecoveryDialog = false
                }) { Text(mStrings.cancelBtn) }
            },
            properties = SecureDialogProperties
        )
    }

    rotatedRecoveryKey?.let { newCode ->
        AlertDialog(
            onDismissRequest = { rotatedRecoveryKey = null },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.recoveryKeyTitle)
            },
            text = { Text("${mStrings.rotatedRecoveryKeyPrefix} $newCode") },
            confirmButton = {
                Button(onClick = { rotatedRecoveryKey = null }) { Text(mStrings.okBtn) }
            },
            properties = SecureDialogProperties
        )
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("management_login_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToPublic,
                        modifier = Modifier.testTag("management_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = mStrings.returnPublicBtn)
                    }
                    Text(
                        text = mStrings.loginTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                EmblemHeaderCard()

                Text(
                    text = mStrings.loginSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                DisclaimerCard(mStrings.localTrialSecurityDisclaimer)

                noticeMessage?.let { InfoBanner(it) }
                errorMessage?.let { ErrorBanner(it) }

                if (activeUsernames.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activeUsernames.forEach { (user, display) ->
                            FilterChip(
                                selected = username.equals(user, ignoreCase = true),
                                onClick = { username = user },
                                label = { Text("$display (@$user)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(mStrings.usernameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_username_input")
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) pin = it },
                    label = { Text(mStrings.pinLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = mStrings.togglePinVisibilityDesc
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_input")
                )

                Button(
                    onClick = { onLogin(username, pin) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand.burgundy,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_unlock_btn")
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(mStrings.loginBtn)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            onClearFeedback()
                            showRecoveryDialog = true
                        },
                        modifier = Modifier.testTag("forgot_pin_btn")
                    ) {
                        Text(mStrings.forgotPinBtn)
                    }
                    TextButton(
                        onClick = onBackToPublic,
                        modifier = Modifier.testTag("management_return_public_button")
                    ) {
                        Text(mStrings.returnPublicBtn)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementDashboardShell(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    mainViewModel: MainViewModel,
    schoolDataViewModel: SchoolDataViewModel? = null,
    mStrings: ManagementStrings,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    statusMessage: String?,
    errorMessage: String?,
    isBusy: Boolean,
    onExitToPublicApp: () -> Unit,
    onOpenLanguageSelection: (() -> Unit)? = null,
    onSelectPublicTab: ((String) -> Unit)? = null
) {
    val brand = LocalBrandPalette.current
    val view = LocalView.current
    var currentDestination by rememberSaveable { mutableStateOf(ManagementDestination.DASHBOARD) }
    var parentHubDestination by rememberSaveable { mutableStateOf<ManagementDestination?>(null) }
    var selectedV2CategoryRoute by rememberSaveable { mutableStateOf(AcademicsV2Category.ACADEMICS.routeId) }
    var showHeroEditorDialog by remember { mutableStateOf(false) }
    var showAcademyHeroEditorDialog by remember { mutableStateOf(false) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    val isVersion2AdminEnabled by Version2FeatureConfig.isVersion2AdminPanelEnabledFlow.collectAsStateWithLifecycle()
    val parentAdminStrings = remember(currentLanguage) { parentPortalAdminStringsFor(currentLanguage) }
    val adminHeroBanner by managementViewModel.adminHeroBanner.collectAsStateWithLifecycle()
    val academyHeroBanner by managementViewModel.academyHeroBanner.collectAsStateWithLifecycle()

    DisposableEffect(view, brand.isDark) {
        val window = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            show(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            // Dark Navy Admin header is always dark, so keep status bar icons white & clearly visible
            isAppearanceLightStatusBars = false
        }
        onDispose {
            controller?.isAppearanceLightStatusBars = !brand.isDark
        }
    }

    val unreadCount by managementViewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    var activeLeaveGuard by remember { mutableStateOf<((() -> Unit) -> Unit)?>(null) }
    fun navigateWithGuard(action: () -> Unit) {
        val guard = activeLeaveGuard
        if (guard != null) {
            guard(action)
        } else {
            action()
        }
    }

    fun canAccessDestination(destination: ManagementDestination): Boolean {
        return when (destination) {
            ManagementDestination.DASHBOARD,
            ManagementDestination.GLOBAL_SEARCH -> true
            ManagementDestination.ADMIN_HOME_HUB ->
                managementViewModel.canAccessAdminCard(AdminDashboardCard.HOME_PAGE)
            ManagementDestination.ADMIN_ACADEMY_HUB ->
                managementViewModel.canAccessAdminCard(AdminDashboardCard.ACADEMY)
            ManagementDestination.ADMIN_PARENT_HUB ->
                managementViewModel.canAccessAdminCard(AdminDashboardCard.PARENT)
            ManagementDestination.ADMIN_COMMON_APP_HUB ->
                managementViewModel.canAccessAdminCard(AdminDashboardCard.COMMON_APP)
            ManagementDestination.ADMIN_TOOLS_HUB ->
                managementViewModel.canAccessAdminCard(AdminDashboardCard.MANAGEMENT_TOOLS)
            ManagementDestination.ADMIN_UPDATES_HUB ->
                managementViewModel.canAccessAdminCard(AdminDashboardCard.UPDATES)
            ManagementDestination.SCHOOL_PROFILE ->
                managementViewModel.hasPermission(ManagementSection.SCHOOL_PROFILE, PermissionAction.VIEW)
            ManagementDestination.LOGO_BANNER ->
                managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.VIEW)
            ManagementDestination.ABOUT_SCHOOL ->
                managementViewModel.hasPermission(ManagementSection.ABOUT_SCHOOL, PermissionAction.VIEW)
            ManagementDestination.ACADEMICS ->
                managementViewModel.hasPermission(ManagementSection.ACADEMICS, PermissionAction.VIEW)
            ManagementDestination.COURSES ->
                managementViewModel.hasPermission(ManagementSection.HIGHER_SECONDARY_COURSES, PermissionAction.VIEW)
            ManagementDestination.FACILITIES ->
                managementViewModel.hasPermission(ManagementSection.FACILITIES, PermissionAction.VIEW)
            ManagementDestination.TRANSPORT ->
                managementViewModel.hasPermission(ManagementSection.TRANSPORT, PermissionAction.VIEW)
            ManagementDestination.NEWS ->
                managementViewModel.hasPermission(ManagementSection.NEWS, PermissionAction.VIEW)
            ManagementDestination.ANNOUNCEMENTS ->
                managementViewModel.hasPermission(ManagementSection.ANNOUNCEMENTS, PermissionAction.VIEW)
            ManagementDestination.PHOTO_GALLERY ->
                managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.VIEW)
            ManagementDestination.CONTACT_DETAILS ->
                managementViewModel.hasPermission(ManagementSection.CONTACT_DETAILS, PermissionAction.VIEW)
            ManagementDestination.SOCIAL_MEDIA ->
                managementViewModel.hasPermission(ManagementSection.SOCIAL_MEDIA, PermissionAction.VIEW)
            ManagementDestination.SCHOOL_POSTS ->
                managementViewModel.hasPermission(ManagementSection.SCHOOL_POSTS, PermissionAction.VIEW)
            ManagementDestination.SCHOOL_HIGHLIGHTS ->
                managementViewModel.hasPermission(ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.VIEW)
            ManagementDestination.MEDIA_STUDIO ->
                managementViewModel.hasPermission(ManagementSection.MEDIA_STUDIO, PermissionAction.VIEW)
            ManagementDestination.STAFF_MANAGEMENT ->
                managementViewModel.hasPermission(ManagementSection.STAFF_MANAGEMENT, PermissionAction.VIEW)
            ManagementDestination.APPROVAL_CENTER ->
                managementViewModel.isMasterAdmin() ||
                    ManagementSection.entries.any {
                        managementViewModel.hasPermission(it, PermissionAction.PUBLISH) ||
                            managementViewModel.hasPermission(it, PermissionAction.EDIT)
                    }
            ManagementDestination.VERSION_HISTORY ->
                managementViewModel.isMasterAdmin() ||
                    ManagementSection.entries.any { managementViewModel.hasPermission(it, PermissionAction.VIEW) }
            ManagementDestination.ACTIVITY_LOG ->
                managementViewModel.hasPermission(ManagementSection.ACTIVITY_LOG, PermissionAction.VIEW)
            ManagementDestination.NOTIFICATIONS ->
                managementViewModel.hasPermission(ManagementSection.NOTIFICATIONS, PermissionAction.VIEW)
            ManagementDestination.BACKUP_RESTORE ->
                managementViewModel.hasPermission(ManagementSection.BACKUP_RESTORE, PermissionAction.VIEW)
            ManagementDestination.APP_SETTINGS ->
                managementViewModel.hasPermission(ManagementSection.APP_SETTINGS, PermissionAction.VIEW)
            ManagementDestination.V2_HOME_HUB,
            ManagementDestination.V2_PARENT_HUB,
            ManagementDestination.V2_ACADEMICS_HUB,
            ManagementDestination.V2_ACADEMICS_CATEGORY,
            ManagementDestination.V2_ADMIN_HUB,
            ManagementDestination.V2_SCHOOL_SETTINGS_HUB,
            ManagementDestination.V2_PREVIEW_PUBLISH,
            ManagementDestination.V2_OWNER_CONTROL_CENTER -> true
            else -> {
                if (destination.toParentPortalAdminModule() != null) {
                    managementViewModel.canAccessAdminCard(AdminDashboardCard.PARENT)
                } else {
                    false
                }
            }
        }
    }

    fun navigateToDestination(
        target: ManagementDestination,
        labelForError: String,
        fromHub: ManagementDestination? = null
    ) {
        managementViewModel.onUserInteraction()
        if (!canAccessDestination(target)) {
            permissionDeniedMessage = "Access restricted: You do not have permission to open $labelForError."
            return
        }
        navigateWithGuard {
            permissionDeniedMessage = null
            managementViewModel.clearFeedback()
            parentHubDestination = fromHub
            currentDestination = target
        }
    }

    fun navigateToAdminCard(card: AdminDashboardCard) {
        val target = when (card) {
            AdminDashboardCard.HOME_PAGE -> ManagementDestination.ADMIN_HOME_HUB
            AdminDashboardCard.ACADEMY -> ManagementDestination.ADMIN_ACADEMY_HUB
            AdminDashboardCard.PARENT -> ManagementDestination.ADMIN_PARENT_HUB
            AdminDashboardCard.CONTACT -> ManagementDestination.CONTACT_DETAILS
            AdminDashboardCard.STAFF_MANAGEMENT -> ManagementDestination.STAFF_MANAGEMENT
            AdminDashboardCard.COMMON_APP -> ManagementDestination.ADMIN_COMMON_APP_HUB
            AdminDashboardCard.MANAGEMENT_TOOLS -> ManagementDestination.ADMIN_TOOLS_HUB
            AdminDashboardCard.UPDATES -> ManagementDestination.ADMIN_UPDATES_HUB
        }
        navigateToDestination(target = target, labelForError = card.title, fromHub = null)
    }

    fun navigateBackOneStep() {
        managementViewModel.onUserInteraction()
        navigateWithGuard {
            permissionDeniedMessage = null
            managementViewModel.clearFeedback()
            when {
                currentDestination == ManagementDestination.V2_ACADEMICS_CATEGORY -> {
                    currentDestination = ManagementDestination.V2_ACADEMICS_HUB
                }
                currentDestination.toParentPortalAdminModule() != null -> {
                    currentDestination = ManagementDestination.ADMIN_PARENT_HUB
                    parentHubDestination = null
                }
                parentHubDestination != null && currentDestination != parentHubDestination -> {
                    currentDestination = parentHubDestination!!
                    parentHubDestination = null
                }
                else -> {
                    currentDestination = ManagementDestination.DASHBOARD
                    parentHubDestination = null
                }
            }
        }
    }

    BackHandler(enabled = currentDestination != ManagementDestination.DASHBOARD) {
        navigateBackOneStep()
    }
    BackHandler(enabled = currentDestination == ManagementDestination.DASHBOARD) {
        navigateWithGuard(onExitToPublicApp)
    }

    val roleLabel = when (session.account.role) {
        ManagementRole.MASTER_ADMIN -> mStrings.masterAdminBadge
        ManagementRole.SUPER_OWNER -> mStrings.superOwnerBadge
        else -> mStrings.staffBadge
    }

    val dashboardTitle = when (currentDestination) {
        ManagementDestination.DASHBOARD -> "CHEVALIER ACADEMY"
        ManagementDestination.ADMIN_HOME_HUB -> AdminDashboardCard.HOME_PAGE.title
        ManagementDestination.ADMIN_ACADEMY_HUB -> AdminDashboardCard.ACADEMY.title
        ManagementDestination.ADMIN_PARENT_HUB -> AdminDashboardCard.PARENT.title
        ManagementDestination.ADMIN_COMMON_APP_HUB -> AdminDashboardCard.COMMON_APP.title
        ManagementDestination.ADMIN_TOOLS_HUB -> AdminDashboardCard.MANAGEMENT_TOOLS.title
        ManagementDestination.ADMIN_UPDATES_HUB -> AdminDashboardCard.UPDATES.title
        ManagementDestination.SCHOOL_PROFILE -> mStrings.cardSchoolProfile
        ManagementDestination.LOGO_BANNER -> mStrings.cardLogoBanner
        ManagementDestination.ABOUT_SCHOOL -> mStrings.cardAboutSchool
        ManagementDestination.ACADEMICS -> mStrings.cardAcademics
        ManagementDestination.COURSES -> mStrings.cardCourses
        ManagementDestination.FACILITIES -> mStrings.cardFacilities
        ManagementDestination.TRANSPORT -> mStrings.cardTransport
        ManagementDestination.NEWS -> mStrings.cardNews
        ManagementDestination.ANNOUNCEMENTS -> mStrings.cardAnnouncements
        ManagementDestination.PHOTO_GALLERY -> mStrings.cardPhotoGallery
        ManagementDestination.CONTACT_DETAILS -> mStrings.cardContactDetails
        ManagementDestination.SOCIAL_MEDIA -> mStrings.cardSocialMedia
        ManagementDestination.SCHOOL_POSTS -> mStrings.cardSchoolPosts
        ManagementDestination.SCHOOL_HIGHLIGHTS -> mStrings.cardSchoolHighlights
        ManagementDestination.MEDIA_STUDIO -> mStrings.cardMediaStudio
        ManagementDestination.STAFF_MANAGEMENT -> mStrings.cardStaffManagement
        ManagementDestination.APPROVAL_CENTER -> mStrings.cardApprovalCenter
        ManagementDestination.VERSION_HISTORY -> mStrings.cardVersionHistory
        ManagementDestination.ACTIVITY_LOG -> mStrings.cardActivityLog
        ManagementDestination.NOTIFICATIONS -> mStrings.cardNotifications
        ManagementDestination.GLOBAL_SEARCH -> mStrings.cardGlobalSearch
        ManagementDestination.BACKUP_RESTORE -> mStrings.cardBackupRestore
        ManagementDestination.APP_SETTINGS -> mStrings.cardAppSettings
        ManagementDestination.V2_HOME_HUB -> AdminV2Card.HOME_PAGE.title
        ManagementDestination.V2_PARENT_HUB -> AdminV2Card.PARENT.title
        ManagementDestination.V2_ACADEMICS_HUB -> "Academics Management"
        ManagementDestination.V2_ACADEMICS_CATEGORY ->
            AcademicsV2Category.fromRouteId(selectedV2CategoryRoute)?.title ?: "Academics"
        ManagementDestination.V2_ADMIN_HUB -> AdminV2Card.ADMIN.title
        ManagementDestination.V2_SCHOOL_SETTINGS_HUB -> AdminV2Card.SCHOOL_SETTINGS.title
        ManagementDestination.V2_PREVIEW_PUBLISH -> AdminV2Card.PREVIEW_PUBLISH.title
        ManagementDestination.V2_OWNER_CONTROL_CENTER -> AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER.title
        else -> currentDestination.toParentPortalAdminModule()
            ?.let { parentPortalAdminTitle(it, currentLanguage) }
            ?: "CHEVALIER ACADEMY"
    }

    val canEditHeroBanner = managementViewModel.canEditAdminHeroBanner()
    val canEditAcademyHeroBanner = managementViewModel.canEditAcademyHeroBanner()
    val canOpenAppSettings = managementViewModel.hasPermission(ManagementSection.APP_SETTINGS, PermissionAction.VIEW)

    if (showHeroEditorDialog && canEditHeroBanner) {
        AdminHeroBannerEditorDialog(
            initialConfig = adminHeroBanner,
            adminDisplayName = session.account.displayName,
            isBusy = isBusy,
            onPickBackgroundImage = { uri, onImported ->
                managementViewModel.importAdminHeroImage(uri, onImported)
            },
            onSave = { line1, line2, line3, imageUri ->
                managementViewModel.saveAdminHeroBanner(
                    titleLine1 = line1,
                    welcomeLine2 = line2,
                    subtitleLine3 = line3,
                    backgroundImageUri = imageUri,
                    onSaved = { showHeroEditorDialog = false }
                )
            },
            onDismiss = { showHeroEditorDialog = false }
        )
    }

    if (showAcademyHeroEditorDialog && canEditAcademyHeroBanner) {
        AcademyHeroBannerEditorDialog(
            initialConfig = academyHeroBanner,
            isBusy = isBusy,
            onPickBannerImage = { uri, onImported ->
                managementViewModel.importAcademyHeroImage(uri, onImported)
            },
            onPublish = { imageUri, overlayTitle, overlaySubtitle, publishNow ->
                managementViewModel.saveAcademyHeroBanner(
                    imageUri = imageUri,
                    overlayTitle = overlayTitle,
                    overlaySubtitle = overlaySubtitle,
                    publishNow = publishNow,
                    onSaved = { showAcademyHeroEditorDialog = false }
                )
            },
            onDismiss = { showAcademyHeroEditorDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        managementViewModel.onUserInteraction()
                    }
                }
            }
            .testTag("management_dashboard_shell"),
        containerColor = if (currentDestination == ManagementDestination.DASHBOARD) {
            AdminDashboardBlueprint.NavyBackground
        } else {
            MaterialTheme.colorScheme.background
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AdminDashboardTopBar(
                    isDashboardRoot = currentDestination == ManagementDestination.DASHBOARD,
                    title = dashboardTitle,
                    adminDisplayName = session.account.displayName,
                    adminRoleLabel = roleLabel,
                    unreadNotificationsCount = unreadCount,
                    canEditHeroBanner = canEditHeroBanner,
                    canOpenSettings = canOpenAppSettings,
                    onBackClick = {
                        if (currentDestination == ManagementDestination.DASHBOARD) {
                            navigateWithGuard(onExitToPublicApp)
                        } else {
                            navigateBackOneStep()
                        }
                    },
                    onSearchClick = {
                        managementViewModel.clearAdminDashboardSearch()
                        navigateToDestination(
                            target = ManagementDestination.GLOBAL_SEARCH,
                            labelForError = mStrings.cardGlobalSearch
                        )
                    },
                    onNotificationsClick = {
                        navigateToDestination(
                            target = ManagementDestination.NOTIFICATIONS,
                            labelForError = mStrings.cardNotifications
                        )
                    },
                    onEditHeroBannerClick = {
                        if (canEditHeroBanner) {
                            permissionDeniedMessage = null
                            showHeroEditorDialog = true
                        } else {
                            permissionDeniedMessage = "Access restricted: You do not have permission to edit the Admin Hero Banner."
                        }
                    },
                    onOpenSettingsClick = {
                        navigateToDestination(
                            target = ManagementDestination.APP_SETTINGS,
                            labelForError = mStrings.cardAppSettings
                        )
                    },
                    onExitToPublicApp = { navigateWithGuard(onExitToPublicApp) },
                    onLogoutClick = { navigateWithGuard { managementViewModel.logout() } }
                )
                if (isBusy) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AdminDashboardBlueprint.OutlineViolet
                    )
                }
            }
        },
        bottomBar = {
            if (currentDestination == ManagementDestination.DASHBOARD) {
                ChevalierPublicBottomBar(
                    selectedRoute = null,
                    currentThemeMode = currentThemeMode,
                    onSelectRoute = { route ->
                        navigateWithGuard {
                            val tabHandler = onSelectPublicTab
                            if (tabHandler != null) {
                                tabHandler(route)
                            } else {
                                onExitToPublicApp()
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val effectiveError = permissionDeniedMessage ?: errorMessage
            val handlesOwnBanners = currentDestination == ManagementDestination.SCHOOL_PROFILE ||
                currentDestination == ManagementDestination.ABOUT_SCHOOL ||
                currentDestination == ManagementDestination.CONTACT_DETAILS
            if (!handlesOwnBanners && (!statusMessage.isNullOrBlank() || !effectiveError.isNullOrBlank())) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    if (!effectiveError.isNullOrBlank()) {
                        ErrorBanner(
                            message = effectiveError,
                            dismissLabel = mStrings.dismissBtn,
                            onDismiss = {
                                permissionDeniedMessage = null
                                managementViewModel.clearFeedback()
                            }
                        )
                    } else if (!statusMessage.isNullOrBlank()) {
                        SuccessBanner(
                            message = statusMessage,
                            okLabel = mStrings.okBtn,
                            onDismiss = { managementViewModel.clearFeedback() }
                        )
                    }
                }
            }

            if (!canAccessDestination(currentDestination)) {
                AdminAccessDeniedScreen(sectionTitle = dashboardTitle)
            } else {
                when (currentDestination) {
                    ManagementDestination.DASHBOARD -> {
                        if (isVersion2AdminEnabled) {
                            AdminV2DashboardGrid(
                                session = session,
                                managementViewModel = managementViewModel,
                                onSelectAdminCard = { card ->
                                    managementViewModel.onUserInteraction()
                                    managementViewModel.clearFeedback()
                                    if (!AcademicsV2AccessControl.canAccessAdminCard(session, card)) {
                                        permissionDeniedMessage = "Access restricted for ${card.title}."
                                        return@AdminV2DashboardGrid
                                    }
                                    currentDestination = when (card) {
                                        AdminV2Card.HOME_PAGE -> ManagementDestination.V2_HOME_HUB
                                        AdminV2Card.PARENT -> ManagementDestination.V2_PARENT_HUB
                                        AdminV2Card.ACADEMICS -> ManagementDestination.V2_ACADEMICS_HUB
                                        AdminV2Card.CONTACT -> ManagementDestination.CONTACT_DETAILS
                                        AdminV2Card.ADMIN -> ManagementDestination.V2_ADMIN_HUB
                                        AdminV2Card.SCHOOL_SETTINGS -> ManagementDestination.V2_SCHOOL_SETTINGS_HUB
                                        AdminV2Card.PREVIEW_PUBLISH -> ManagementDestination.V2_PREVIEW_PUBLISH
                                        AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER -> ManagementDestination.V2_OWNER_CONTROL_CENTER
                                    }
                                }
                            )
                        } else {
                            AdminDashboardHomeContent(
                                session = session,
                                heroConfig = adminHeroBanner,
                                canEditHeroBanner = canEditHeroBanner,
                                canAccessCard = { card -> managementViewModel.canAccessAdminCard(card) },
                                onEditHeroBanner = {
                                    if (canEditHeroBanner) {
                                        permissionDeniedMessage = null
                                        showHeroEditorDialog = true
                                    } else {
                                        permissionDeniedMessage = "Access restricted: You do not have permission to edit the Admin Hero Banner."
                                    }
                                },
                                onCardClick = { card -> navigateToAdminCard(card) }
                            )
                        }
                    }

                    ManagementDestination.ADMIN_HOME_HUB -> {
                        AdminSectionHubScreen(
                            hubTestTag = "admin_hub_home_page",
                            subtitle = "Manage public Home Page hero banners, school posts, highlights, and media assets.",
                            items = listOf(
                                AdminHubRouteItem(
                                    id = "logo_banner",
                                    title = mStrings.cardLogoBanner,
                                    description = "Manage public campus hero photos and banner sequence.",
                                    icon = Icons.Outlined.Image,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_logo_banner",
                                    onClick = { navigateToDestination(ManagementDestination.LOGO_BANNER, mStrings.cardLogoBanner, ManagementDestination.ADMIN_HOME_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "school_posts",
                                    title = mStrings.cardSchoolPosts,
                                    description = "Create, review, and publish Home feed posts.",
                                    icon = Icons.Outlined.DynamicFeed,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.SCHOOL_POSTS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_school_posts",
                                    onClick = { navigateToDestination(ManagementDestination.SCHOOL_POSTS, mStrings.cardSchoolPosts, ManagementDestination.ADMIN_HOME_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "school_highlights",
                                    title = mStrings.cardSchoolHighlights,
                                    description = "Manage active school highlights featured on the Home Page.",
                                    icon = Icons.Outlined.AutoAwesome,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_school_highlights",
                                    onClick = { navigateToDestination(ManagementDestination.SCHOOL_HIGHLIGHTS, mStrings.cardSchoolHighlights, ManagementDestination.ADMIN_HOME_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "media_studio",
                                    title = mStrings.cardMediaStudio,
                                    description = "Upload and organize managed campus photography.",
                                    icon = Icons.Outlined.PermMedia,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.MEDIA_STUDIO, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_media_studio",
                                    onClick = { navigateToDestination(ManagementDestination.MEDIA_STUDIO, mStrings.cardMediaStudio, ManagementDestination.ADMIN_HOME_HUB) }
                                )
                            )
                        )
                    }

                    ManagementDestination.ADMIN_ACADEMY_HUB -> {
                        AdminSectionHubScreen(
                            hubTestTag = "admin_hub_academy",
                            subtitle = "Manage Academy Hero Banner, academic programs, courses, facilities, transport, gallery, and school overview.",
                            items = listOf(
                                AdminHubRouteItem(
                                    id = "academy_hero_banner",
                                    title = "Academy Hero Banner",
                                    description = "Upload or replace the Academy Page hero banner image with optional overlay text.",
                                    icon = Icons.Outlined.PhotoLibrary,
                                    isAllowed = canEditAcademyHeroBanner,
                                    testTag = "admin_hub_open_academy_hero_banner",
                                    onClick = { showAcademyHeroEditorDialog = true }
                                ),
                                AdminHubRouteItem(
                                    id = "academics",
                                    title = mStrings.cardAcademics,
                                    description = "Edit academic curriculum and department details.",
                                    icon = Icons.Outlined.School,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.ACADEMICS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_academics",
                                    onClick = { navigateToDestination(ManagementDestination.ACADEMICS, mStrings.cardAcademics, ManagementDestination.ADMIN_ACADEMY_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "courses",
                                    title = mStrings.cardCourses,
                                    description = "Manage higher secondary streams and subject groups.",
                                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.HIGHER_SECONDARY_COURSES, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_courses",
                                    onClick = { navigateToDestination(ManagementDestination.COURSES, mStrings.cardCourses, ManagementDestination.ADMIN_ACADEMY_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "facilities",
                                    title = mStrings.cardFacilities,
                                    description = "Manage campus laboratories, library, and facilities.",
                                    icon = Icons.Outlined.Apartment,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.FACILITIES, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_facilities",
                                    onClick = { navigateToDestination(ManagementDestination.FACILITIES, mStrings.cardFacilities, ManagementDestination.ADMIN_ACADEMY_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "transport",
                                    title = mStrings.cardTransport,
                                    description = "Manage school bus routes and stop schedules.",
                                    icon = Icons.Outlined.DirectionsBus,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.TRANSPORT, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_transport",
                                    onClick = { navigateToDestination(ManagementDestination.TRANSPORT, mStrings.cardTransport, ManagementDestination.ADMIN_ACADEMY_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "photo_gallery",
                                    title = mStrings.cardPhotoGallery,
                                    description = "Manage official school photo albums and event galleries.",
                                    icon = Icons.Outlined.PhotoLibrary,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_gallery",
                                    onClick = { navigateToDestination(ManagementDestination.PHOTO_GALLERY, mStrings.cardPhotoGallery, ManagementDestination.ADMIN_ACADEMY_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "about_school",
                                    title = mStrings.cardAboutSchool,
                                    description = "Edit school history, vision, mission, and leadership details.",
                                    icon = Icons.Outlined.Info,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.ABOUT_SCHOOL, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_about",
                                    onClick = { navigateToDestination(ManagementDestination.ABOUT_SCHOOL, mStrings.cardAboutSchool, ManagementDestination.ADMIN_ACADEMY_HUB) }
                                )
                            )
                        )
                    }

                    ManagementDestination.ADMIN_PARENT_HUB -> {
                        val parentItems = buildList {
                            add(
                                AdminHubRouteItem(
                                    id = "parent_announcements",
                                    title = mStrings.cardAnnouncements,
                                    description = "Manage school notices and parent-facing announcements.",
                                    icon = Icons.Outlined.Campaign,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.ANNOUNCEMENTS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_parent_announcements",
                                    onClick = { navigateToDestination(ManagementDestination.ANNOUNCEMENTS, mStrings.cardAnnouncements, ManagementDestination.ADMIN_PARENT_HUB) }
                                )
                            )
                            ParentPortalAdminModule.entries.forEach { module ->
                                val copy = parentAdminStrings.modules.getValue(module)
                                add(
                                    AdminHubRouteItem(
                                        id = "ppm_${module.key}",
                                        title = copy.title,
                                        description = copy.intro,
                                        icon = module.icon,
                                        isAllowed = managementViewModel.canAccessAdminCard(AdminDashboardCard.PARENT),
                                        testTag = "ppm_submenu_card_${module.key}",
                                        onClick = {
                                            navigateToDestination(
                                                target = module.toManagementDestination(),
                                                labelForError = copy.title,
                                                fromHub = ManagementDestination.ADMIN_PARENT_HUB
                                            )
                                        }
                                    )
                                )
                            }
                        }
                        AdminSectionHubScreen(
                            hubTestTag = "admin_hub_parent",
                            subtitle = parentAdminStrings.sectionSubtitle,
                            items = parentItems
                        )
                    }

                    ManagementDestination.ADMIN_COMMON_APP_HUB -> {
                        AdminSectionHubScreen(
                            hubTestTag = "admin_hub_common_app",
                            subtitle = "Manage general app preferences, school identity, and official social links.",
                            items = listOf(
                                AdminHubRouteItem(
                                    id = "app_settings",
                                    title = mStrings.cardAppSettings,
                                    description = "Configure language, theme mode, and app preferences.",
                                    icon = Icons.Outlined.Settings,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.APP_SETTINGS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_app_settings",
                                    onClick = { navigateToDestination(ManagementDestination.APP_SETTINGS, mStrings.cardAppSettings, ManagementDestination.ADMIN_COMMON_APP_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "school_profile",
                                    title = mStrings.cardSchoolProfile,
                                    description = "Update school name, motto, and official affiliation details.",
                                    icon = Icons.Outlined.Badge,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.SCHOOL_PROFILE, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_school_profile",
                                    onClick = { navigateToDestination(ManagementDestination.SCHOOL_PROFILE, mStrings.cardSchoolProfile, ManagementDestination.ADMIN_COMMON_APP_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "social_media",
                                    title = mStrings.cardSocialMedia,
                                    description = "Manage verified official school social media links.",
                                    icon = Icons.Outlined.Share,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.SOCIAL_MEDIA, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_social_media",
                                    onClick = { navigateToDestination(ManagementDestination.SOCIAL_MEDIA, mStrings.cardSocialMedia, ManagementDestination.ADMIN_COMMON_APP_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "logo_banner",
                                    title = mStrings.cardLogoBanner,
                                    description = "Manage campus logo and public banner assets.",
                                    icon = Icons.Outlined.Image,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_common_logo",
                                    onClick = { navigateToDestination(ManagementDestination.LOGO_BANNER, mStrings.cardLogoBanner, ManagementDestination.ADMIN_COMMON_APP_HUB) }
                                )
                            )
                        )
                    }

                    ManagementDestination.ADMIN_TOOLS_HUB -> {
                        AdminSectionHubScreen(
                            hubTestTag = "admin_hub_management_tools",
                            subtitle = "Access administrative governance, audit trail, backups, and media tools.",
                            items = listOf(
                                AdminHubRouteItem(
                                    id = "approval_center",
                                    title = mStrings.cardApprovalCenter,
                                    description = "Review, approve, or reject pending content submissions.",
                                    icon = Icons.Outlined.Approval,
                                    isAllowed = canAccessDestination(ManagementDestination.APPROVAL_CENTER),
                                    testTag = "admin_hub_open_approvals",
                                    onClick = { navigateToDestination(ManagementDestination.APPROVAL_CENTER, mStrings.cardApprovalCenter, ManagementDestination.ADMIN_TOOLS_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "version_history",
                                    title = mStrings.cardVersionHistory,
                                    description = "Inspect content revisions and restore previous versions.",
                                    icon = Icons.Outlined.History,
                                    isAllowed = canAccessDestination(ManagementDestination.VERSION_HISTORY),
                                    testTag = "admin_hub_open_version_history",
                                    onClick = { navigateToDestination(ManagementDestination.VERSION_HISTORY, mStrings.cardVersionHistory, ManagementDestination.ADMIN_TOOLS_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "activity_log",
                                    title = mStrings.cardActivityLog,
                                    description = "Review administrative security and content audit logs.",
                                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.ACTIVITY_LOG, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_activity_log",
                                    onClick = { navigateToDestination(ManagementDestination.ACTIVITY_LOG, mStrings.cardActivityLog, ManagementDestination.ADMIN_TOOLS_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "backup_restore",
                                    title = mStrings.cardBackupRestore,
                                    description = "Export or restore encrypted school administration backups.",
                                    icon = Icons.Outlined.Backup,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.BACKUP_RESTORE, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_backup_restore",
                                    onClick = { navigateToDestination(ManagementDestination.BACKUP_RESTORE, mStrings.cardBackupRestore, ManagementDestination.ADMIN_TOOLS_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "tools_media_studio",
                                    title = mStrings.cardMediaStudio,
                                    description = "Manage stored images and optimized media assets.",
                                    icon = Icons.Outlined.PermMedia,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.MEDIA_STUDIO, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_tools_media",
                                    onClick = { navigateToDestination(ManagementDestination.MEDIA_STUDIO, mStrings.cardMediaStudio, ManagementDestination.ADMIN_TOOLS_HUB) }
                                )
                            )
                        )
                    }

                    ManagementDestination.ADMIN_UPDATES_HUB -> {
                        AdminSectionHubScreen(
                            hubTestTag = "admin_hub_updates",
                            subtitle = "Manage official school news, announcements, and administrative notifications.",
                            items = listOf(
                                AdminHubRouteItem(
                                    id = "news",
                                    title = mStrings.cardNews,
                                    description = "Create and publish official school news updates.",
                                    icon = Icons.Outlined.Newspaper,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.NEWS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_news",
                                    onClick = { navigateToDestination(ManagementDestination.NEWS, mStrings.cardNews, ManagementDestination.ADMIN_UPDATES_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "announcements",
                                    title = mStrings.cardAnnouncements,
                                    description = "Publish official notices and school-wide announcements.",
                                    icon = Icons.Outlined.Campaign,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.ANNOUNCEMENTS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_announcements",
                                    onClick = { navigateToDestination(ManagementDestination.ANNOUNCEMENTS, mStrings.cardAnnouncements, ManagementDestination.ADMIN_UPDATES_HUB) }
                                ),
                                AdminHubRouteItem(
                                    id = "notifications",
                                    title = mStrings.cardNotifications,
                                    description = "View administrative alerts and local device alert settings.",
                                    icon = Icons.Outlined.Notifications,
                                    isAllowed = managementViewModel.hasPermission(ManagementSection.NOTIFICATIONS, PermissionAction.VIEW),
                                    testTag = "admin_hub_open_notifications",
                                    onClick = { navigateToDestination(ManagementDestination.NOTIFICATIONS, mStrings.cardNotifications, ManagementDestination.ADMIN_UPDATES_HUB) }
                                )
                            )
                        )
                    }

                    ManagementDestination.SCHOOL_PROFILE,
                    ManagementDestination.ABOUT_SCHOOL,
                    ManagementDestination.CONTACT_DETAILS -> {
                        val sec = when (currentDestination) {
                            ManagementDestination.ABOUT_SCHOOL -> ManagementSection.ABOUT_SCHOOL
                            ManagementDestination.CONTACT_DETAILS -> ManagementSection.CONTACT_DETAILS
                            else -> ManagementSection.SCHOOL_PROFILE
                        }
                        SchoolProfileSectionEditor(
                            managementViewModel = managementViewModel,
                            mStrings = mStrings,
                            section = sec,
                            onNavigateBack = { navigateBackOneStep() },
                            onRegisterNavigationGuard = { guard -> activeLeaveGuard = guard }
                        )
                    }
                    ManagementDestination.LOGO_BANNER -> HeroBannerEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.ACADEMICS -> AcademicsEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.COURSES -> CoursesEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.FACILITIES -> FacilitiesEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.TRANSPORT -> TransportEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.NEWS -> NewsEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.ANNOUNCEMENTS -> AnnouncementsEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.PHOTO_GALLERY -> PhotoGalleryEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.SOCIAL_MEDIA -> SocialMediaEditorScreen(managementViewModel, mStrings)
                    ManagementDestination.SCHOOL_POSTS -> SchoolFeedAdminScreen(managementViewModel, mStrings, initialTab = 0)
                    ManagementDestination.SCHOOL_HIGHLIGHTS -> SchoolFeedAdminScreen(managementViewModel, mStrings, initialTab = 1)
                    ManagementDestination.MEDIA_STUDIO -> MediaStudioScreen(managementViewModel, mStrings)
                    ManagementDestination.STAFF_MANAGEMENT -> StaffManagementScreen(managementViewModel, mStrings)
                    ManagementDestination.APPROVAL_CENTER -> ApprovalCenterScreen(managementViewModel, mStrings)
                    ManagementDestination.VERSION_HISTORY -> VersionHistoryScreen(managementViewModel, mStrings)
                    ManagementDestination.ACTIVITY_LOG -> ActivityLogScreen(managementViewModel, mStrings)
                    ManagementDestination.NOTIFICATIONS -> NotificationsScreen(managementViewModel, mStrings)
                    ManagementDestination.GLOBAL_SEARCH -> AdminDashboardSearchScreen(
                        managementViewModel = managementViewModel,
                        onOpenCard = { card -> navigateToAdminCard(card) },
                        onOpenSection = { sec ->
                            val target = when (sec) {
                                ManagementSection.SCHOOL_PROFILE -> ManagementDestination.SCHOOL_PROFILE
                                ManagementSection.ABOUT_SCHOOL -> ManagementDestination.ABOUT_SCHOOL
                                ManagementSection.CONTACT_DETAILS -> ManagementDestination.CONTACT_DETAILS
                                ManagementSection.LOGO_BANNER -> ManagementDestination.LOGO_BANNER
                                ManagementSection.ACADEMICS -> ManagementDestination.ACADEMICS
                                ManagementSection.HIGHER_SECONDARY_COURSES -> ManagementDestination.COURSES
                                ManagementSection.FACILITIES -> ManagementDestination.FACILITIES
                                ManagementSection.TRANSPORT -> ManagementDestination.TRANSPORT
                                ManagementSection.NEWS -> ManagementDestination.NEWS
                                ManagementSection.ANNOUNCEMENTS -> ManagementDestination.ANNOUNCEMENTS
                                ManagementSection.PHOTO_GALLERY -> ManagementDestination.PHOTO_GALLERY
                                ManagementSection.SOCIAL_MEDIA -> ManagementDestination.SOCIAL_MEDIA
                                ManagementSection.SCHOOL_POSTS -> ManagementDestination.SCHOOL_POSTS
                                ManagementSection.SCHOOL_HIGHLIGHTS -> ManagementDestination.SCHOOL_HIGHLIGHTS
                                ManagementSection.MEDIA_STUDIO -> ManagementDestination.MEDIA_STUDIO
                                ManagementSection.STAFF_MANAGEMENT -> ManagementDestination.STAFF_MANAGEMENT
                                ManagementSection.ACTIVITY_LOG -> ManagementDestination.ACTIVITY_LOG
                                ManagementSection.NOTIFICATIONS -> ManagementDestination.NOTIFICATIONS
                                ManagementSection.BACKUP_RESTORE -> ManagementDestination.BACKUP_RESTORE
                                ManagementSection.APP_SETTINGS -> ManagementDestination.APP_SETTINGS
                            }
                            navigateToDestination(target, mStrings.sectionLabel(sec))
                        },
                        onOpenToolKey = { key ->
                            when (key) {
                                "APPROVAL_CENTER" -> navigateToDestination(ManagementDestination.APPROVAL_CENTER, mStrings.cardApprovalCenter)
                                "VERSION_HISTORY" -> navigateToDestination(ManagementDestination.VERSION_HISTORY, mStrings.cardVersionHistory)
                            }
                        }
                    )
                    ManagementDestination.BACKUP_RESTORE -> BackupRestoreScreen(managementViewModel, mStrings)
                    ManagementDestination.APP_SETTINGS -> {
                        SettingsScreen(
                            currentLanguage = currentLanguage,
                            currentThemeMode = currentThemeMode,
                            onLanguageSelected = { mainViewModel.selectLanguage(it, markOnboarded = true) },
                            onThemeModeSelected = { mainViewModel.selectThemeMode(it) },
                            onOpenLanguageSelection = onOpenLanguageSelection
                        )
                    }
                ManagementDestination.V2_HOME_HUB -> {
                    if (!isVersion2AdminEnabled) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = AdminV2Card.HOME_PAGE.title,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        AdminV2HomePageHubScreen(
                            onBack = { currentDestination = ManagementDestination.DASHBOARD },
                            onOpenHeroBanner = { currentDestination = ManagementDestination.LOGO_BANNER },
                            onOpenSchoolPosts = { currentDestination = ManagementDestination.SCHOOL_POSTS },
                            onOpenHighlights = { currentDestination = ManagementDestination.SCHOOL_HIGHLIGHTS },
                            onOpenLivePreview = { onExitToPublicApp() }
                        )
                    }
                }
                ManagementDestination.V2_PARENT_HUB -> {
                    if (!isVersion2AdminEnabled) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = AdminV2Card.PARENT.title,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        AdminV2ParentInfoHubScreen(
                            onBack = { currentDestination = ManagementDestination.DASHBOARD },
                            onOpenAnnouncements = { currentDestination = ManagementDestination.ANNOUNCEMENTS },
                            onOpenNewsCircularsV2 = {
                                selectedV2CategoryRoute = AcademicsV2Category.NEWS_ANNOUNCEMENTS.routeId
                                currentDestination = ManagementDestination.V2_ACADEMICS_CATEGORY
                            },
                            onOpenTransportV2 = {
                                selectedV2CategoryRoute = AcademicsV2Category.TRANSPORT.routeId
                                currentDestination = ManagementDestination.V2_ACADEMICS_CATEGORY
                            },
                            onOpenCalendarV2 = {
                                selectedV2CategoryRoute = AcademicsV2Category.ACADEMIC_CALENDAR.routeId
                                currentDestination = ManagementDestination.V2_ACADEMICS_CATEGORY
                            }
                        )
                    }
                }
                ManagementDestination.V2_ACADEMICS_HUB -> {
                    if (!isVersion2AdminEnabled) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = "Academics Management",
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        AdminV2AcademicsManagementHubScreen(
                            session = session,
                            managementViewModel = managementViewModel,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD },
                            onSelectCategory = { cat ->
                                selectedV2CategoryRoute = cat.routeId
                                currentDestination = ManagementDestination.V2_ACADEMICS_CATEGORY
                            }
                        )
                    }
                }
                ManagementDestination.V2_ACADEMICS_CATEGORY -> {
                    val category = AcademicsV2Category.fromRouteId(selectedV2CategoryRoute)
                        ?: AcademicsV2Category.ACADEMICS
                    if (!isVersion2AdminEnabled) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = category.title,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        AdminV2CategoryManagementScreen(
                            category = category,
                            session = session,
                            managementViewModel = managementViewModel,
                            onBack = { currentDestination = ManagementDestination.V2_ACADEMICS_HUB }
                        )
                    }
                }
                ManagementDestination.V2_ADMIN_HUB -> {
                    if (!isVersion2AdminEnabled) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = AdminV2Card.ADMIN.title,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        AdminV2AdminTeamHubScreen(
                            session = session,
                            managementViewModel = managementViewModel,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD },
                            onOpenLegacyStaffEditor = { currentDestination = ManagementDestination.STAFF_MANAGEMENT }
                        )
                    }
                }
                ManagementDestination.V2_SCHOOL_SETTINGS_HUB -> {
                    if (!isVersion2AdminEnabled) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = AdminV2Card.SCHOOL_SETTINGS.title,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        AdminV2SchoolSettingsHubScreen(
                            onBack = { currentDestination = ManagementDestination.DASHBOARD },
                            onOpenSchoolProfile = { currentDestination = ManagementDestination.SCHOOL_PROFILE },
                            onOpenLogoBanner = { currentDestination = ManagementDestination.LOGO_BANNER },
                            onOpenSocialMedia = { currentDestination = ManagementDestination.SOCIAL_MEDIA },
                            onOpenAppSettings = { currentDestination = ManagementDestination.APP_SETTINGS }
                        )
                    }
                }
                ManagementDestination.V2_PREVIEW_PUBLISH -> {
                    if (!isVersion2AdminEnabled || schoolDataViewModel == null) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = AdminV2Card.PREVIEW_PUBLISH.title,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        AdminV2PreviewAndPublishScreen(
                            session = session,
                            managementViewModel = managementViewModel,
                            schoolDataViewModel = schoolDataViewModel,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD },
                            onOpenFullHomePreview = { onExitToPublicApp() },
                            onOpenAcademicsCategory = { cat ->
                                selectedV2CategoryRoute = cat.routeId
                                currentDestination = ManagementDestination.V2_ACADEMICS_CATEGORY
                            }
                        )
                    }
                }
                ManagementDestination.V2_OWNER_CONTROL_CENTER -> {
                    if (!isVersion2AdminEnabled) {
                        Version2UnavailableNoticeScreen(
                            language = currentLanguage,
                            title = AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER.title,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD }
                        )
                    } else {
                        SchoolOwnerControlCenterScreen(
                            session = session,
                            managementViewModel = managementViewModel,
                            onBack = { currentDestination = ManagementDestination.DASHBOARD },
                            onOpenStaffAccounts = { currentDestination = ManagementDestination.STAFF_MANAGEMENT },
                            onOpenLegacyApprovals = { currentDestination = ManagementDestination.APPROVAL_CENTER },
                            onOpenBackupRestore = { currentDestination = ManagementDestination.BACKUP_RESTORE }
                        )
                    }
                }
                    else -> {
                        val parentModule = currentDestination.toParentPortalAdminModule()
                        if (parentModule != null) {
                            ParentPortalAdminModuleScreen(
                                module = parentModule,
                                currentLanguage = currentLanguage,
                                onBackToDashboard = {
                                    navigateBackOneStep()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmblemHeaderCard() {
    val brand = LocalBrandPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(2.dp, brand.royalGold),
        shadowElevation = 3.dp,
        modifier = Modifier.size(84.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.chevalier_emblem),
            contentDescription = "Chevalier Academy Emblem",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(14.dp))
        )
    }
}

@Composable
private fun DisclaimerCard(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ErrorBanner(
    message: String,
    dismissLabel: String = "Dismiss",
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mgmt_error_banner")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            onDismiss?.let {
                TextButton(onClick = it) { Text(dismissLabel) }
            }
        }
    }
}

@Composable
internal fun SuccessBanner(
    message: String,
    okLabel: String = "OK",
    onDismiss: (() -> Unit)? = null
) {
    val brand = LocalBrandPalette.current
    Surface(
        color = brand.forestGreenContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mgmt_success_banner")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = brand.onForestGreenContainer,
                modifier = Modifier.weight(1f)
            )
            onDismiss?.let {
                TextButton(onClick = it) { Text(okLabel) }
            }
        }
    }
}

@Composable
internal fun InfoBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}
