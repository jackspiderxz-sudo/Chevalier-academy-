package com.example.ui.screens.management

import android.app.Activity
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.DirectionsBus
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
import com.example.core.i18n.ManagementStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel

enum class ManagementDestination {
    DASHBOARD,
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
    PARENT_EMERGENCY_INFO
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

private data class DashboardCardItem(
    val destination: ManagementDestination,
    val section: ManagementSection?,
    val title: String,
    val countText: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String
)

@Composable
fun ManagementPortalScreen(
    managementViewModel: ManagementViewModel,
    mainViewModel: MainViewModel,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    onExitToPublicApp: () -> Unit
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
                mStrings = mStrings,
                currentLanguage = currentLanguage,
                currentThemeMode = currentThemeMode,
                statusMessage = statusMessage,
                errorMessage = errorMessage,
                isBusy = isBusy,
                onExitToPublicApp = onExitToPublicApp
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
            title = { Text(mStrings.recoverPinTitle) },
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
            }
        )
    }

    rotatedRecoveryKey?.let { newCode ->
        AlertDialog(
            onDismissRequest = { rotatedRecoveryKey = null },
            title = { Text(mStrings.recoveryKeyTitle) },
            text = { Text("${mStrings.rotatedRecoveryKeyPrefix} $newCode") },
            confirmButton = {
                Button(onClick = { rotatedRecoveryKey = null }) { Text(mStrings.okBtn) }
            }
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
    mStrings: ManagementStrings,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    statusMessage: String?,
    errorMessage: String?,
    isBusy: Boolean,
    onExitToPublicApp: () -> Unit
) {
    val brand = LocalBrandPalette.current
    val view = LocalView.current
    var currentDestination by remember { mutableStateOf(ManagementDestination.DASHBOARD) }
    var parentPortalMgmtExpanded by rememberSaveable { mutableStateOf(false) }
    val parentAdminStrings = remember(currentLanguage) { parentPortalAdminStringsFor(currentLanguage) }

    DisposableEffect(view, brand.isDark) {
        val window = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            show(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            // Burgundy top bar is always dark, so keep status bar icons white & clearly visible
            isAppearanceLightStatusBars = false
        }
        onDispose {
            controller?.isAppearanceLightStatusBars = !brand.isDark
        }
    }

    val unreadCount by managementViewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val pendingApprovalsCount by managementViewModel.pendingApprovalsCount.collectAsStateWithLifecycle()
    val accounts by managementViewModel.accountsWithPermissions.collectAsStateWithLifecycle()
    val academics by managementViewModel.allAcademics.collectAsStateWithLifecycle()
    val courses by managementViewModel.allCourses.collectAsStateWithLifecycle()
    val facilities by managementViewModel.allFacilities.collectAsStateWithLifecycle()
    val routes by managementViewModel.allTransportRoutes.collectAsStateWithLifecycle()
    val news by managementViewModel.allNews.collectAsStateWithLifecycle()
    val announcements by managementViewModel.allAnnouncements.collectAsStateWithLifecycle()
    val heroPhotos by managementViewModel.allHeroPhotos.collectAsStateWithLifecycle()
    val albums by managementViewModel.allGalleryAlbumsWithMedia.collectAsStateWithLifecycle()
    val socialLinks by managementViewModel.allSocialLinks.collectAsStateWithLifecycle()
    val mediaItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val versions by managementViewModel.allVersionHistory.collectAsStateWithLifecycle()
    val logs by managementViewModel.visibleActivityLogs.collectAsStateWithLifecycle()

    var activeLeaveGuard by remember { mutableStateOf<((() -> Unit) -> Unit)?>(null) }
    fun navigateWithGuard(action: () -> Unit) {
        val guard = activeLeaveGuard
        if (guard != null) {
            guard(action)
        } else {
            action()
        }
    }

    BackHandler(enabled = currentDestination != ManagementDestination.DASHBOARD) {
        managementViewModel.onUserInteraction()
        navigateWithGuard {
            managementViewModel.clearFeedback()
            currentDestination = ManagementDestination.DASHBOARD
        }
    }
    BackHandler(enabled = currentDestination == ManagementDestination.DASHBOARD) {
        onExitToPublicApp()
    }

    val roleLabel = when (session.account.role) {
        ManagementRole.MASTER_ADMIN -> mStrings.masterAdminBadge
        ManagementRole.SUPER_OWNER -> mStrings.superOwnerBadge
        else -> mStrings.staffBadge
    }

    val dashboardTitle = when (currentDestination) {
        ManagementDestination.DASHBOARD -> mStrings.portalTitle
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
        ManagementDestination.MEDIA_STUDIO -> mStrings.cardMediaStudio
        ManagementDestination.STAFF_MANAGEMENT -> mStrings.cardStaffManagement
        ManagementDestination.APPROVAL_CENTER -> mStrings.cardApprovalCenter
        ManagementDestination.VERSION_HISTORY -> mStrings.cardVersionHistory
        ManagementDestination.ACTIVITY_LOG -> mStrings.cardActivityLog
        ManagementDestination.NOTIFICATIONS -> mStrings.cardNotifications
        ManagementDestination.GLOBAL_SEARCH -> mStrings.cardGlobalSearch
        ManagementDestination.BACKUP_RESTORE -> mStrings.cardBackupRestore
        ManagementDestination.APP_SETTINGS -> mStrings.cardAppSettings
        else -> currentDestination.toParentPortalAdminModule()
            ?.let { parentPortalAdminTitle(it, currentLanguage) }
            ?: mStrings.portalTitle
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
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = brand.burgundy,
                contentColor = Color.White,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .displayCutoutPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (currentDestination != ManagementDestination.DASHBOARD) {
                                IconButton(
                                    onClick = {
                                        managementViewModel.onUserInteraction()
                                        navigateWithGuard {
                                            managementViewModel.clearFeedback()
                                            currentDestination = ManagementDestination.DASHBOARD
                                        }
                                    },
                                    modifier = Modifier.testTag("mgmt_subscreen_back_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = mStrings.backToDashboardDesc,
                                        tint = Color.White
                                    )
                                }
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.chevalier_emblem),
                                    contentDescription = "Chevalier Academy",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column {
                                Text(
                                    text = dashboardTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${session.account.displayName} • $roleLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = brand.royalGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (managementViewModel.hasPermission(ManagementSection.NOTIFICATIONS, PermissionAction.VIEW)) {
                                IconButton(
                                    onClick = {
                                        managementViewModel.onUserInteraction()
                                        navigateWithGuard {
                                            managementViewModel.clearFeedback()
                                            currentDestination = ManagementDestination.NOTIFICATIONS
                                        }
                                    },
                                    modifier = Modifier.testTag("mgmt_top_notifications_btn")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge(containerColor = brand.royalGold, contentColor = Color.Black) {
                                                    Text(unreadCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Notifications, contentDescription = mStrings.notificationsLabel, tint = Color.White)
                                    }
                                }
                            }

                            IconButton(
                                onClick = { navigateWithGuard(onExitToPublicApp) },
                                modifier = Modifier.testTag("mgmt_top_public_btn")
                            ) {
                                Icon(Icons.Outlined.Public, contentDescription = mStrings.returnPublicBtn, tint = Color.White)
                            }

                            IconButton(
                                onClick = { navigateWithGuard { managementViewModel.logout() } },
                                modifier = Modifier.testTag("mgmt_top_logout_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = mStrings.logoutBtn, tint = Color.White)
                            }
                        }
                    }
                    if (isBusy) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = brand.royalGold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val handlesOwnBanners = currentDestination == ManagementDestination.SCHOOL_PROFILE ||
                currentDestination == ManagementDestination.ABOUT_SCHOOL ||
                currentDestination == ManagementDestination.CONTACT_DETAILS
            if (!handlesOwnBanners && (!statusMessage.isNullOrBlank() || !errorMessage.isNullOrBlank())) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    if (!errorMessage.isNullOrBlank()) {
                        ErrorBanner(
                            message = errorMessage,
                            dismissLabel = mStrings.dismissBtn,
                            onDismiss = { managementViewModel.clearFeedback() }
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

            when (currentDestination) {
                ManagementDestination.DASHBOARD -> {
                    val cards = listOf(
                        DashboardCardItem(ManagementDestination.SCHOOL_PROFILE, ManagementSection.SCHOOL_PROFILE, mStrings.cardSchoolProfile, "1", Icons.Outlined.Badge, brand.burgundy, "dash_card_profile"),
                        DashboardCardItem(ManagementDestination.LOGO_BANNER, ManagementSection.LOGO_BANNER, mStrings.cardLogoBanner, "${heroPhotos.size}", Icons.Outlined.Image, brand.forestGreen, "dash_card_banner"),
                        DashboardCardItem(ManagementDestination.ABOUT_SCHOOL, ManagementSection.ABOUT_SCHOOL, mStrings.cardAboutSchool, "1", Icons.Outlined.Info, brand.burgundy, "dash_card_about"),
                        DashboardCardItem(ManagementDestination.ACADEMICS, ManagementSection.ACADEMICS, mStrings.cardAcademics, "${academics.size}", Icons.Outlined.School, brand.forestGreen, "dash_card_academics"),
                        DashboardCardItem(ManagementDestination.COURSES, ManagementSection.HIGHER_SECONDARY_COURSES, mStrings.cardCourses, "${courses.size}", Icons.AutoMirrored.Outlined.MenuBook, brand.burgundy, "dash_card_courses"),
                        DashboardCardItem(ManagementDestination.FACILITIES, ManagementSection.FACILITIES, mStrings.cardFacilities, "${facilities.size}", Icons.Outlined.Apartment, brand.forestGreen, "dash_card_facilities"),
                        DashboardCardItem(ManagementDestination.TRANSPORT, ManagementSection.TRANSPORT, mStrings.cardTransport, "${routes.size}", Icons.Outlined.DirectionsBus, brand.burgundy, "dash_card_transport"),
                        DashboardCardItem(ManagementDestination.NEWS, ManagementSection.NEWS, mStrings.cardNews, "${news.size}", Icons.Outlined.Newspaper, brand.forestGreen, "dash_card_news"),
                        DashboardCardItem(ManagementDestination.ANNOUNCEMENTS, ManagementSection.ANNOUNCEMENTS, mStrings.cardAnnouncements, "${announcements.size}", Icons.Outlined.Campaign, brand.burgundy, "dash_card_announcements"),
                        DashboardCardItem(ManagementDestination.PHOTO_GALLERY, ManagementSection.PHOTO_GALLERY, mStrings.cardPhotoGallery, "${albums.size}", Icons.Outlined.PhotoLibrary, brand.forestGreen, "dash_card_gallery"),
                        DashboardCardItem(ManagementDestination.CONTACT_DETAILS, ManagementSection.CONTACT_DETAILS, mStrings.cardContactDetails, "1", Icons.Outlined.ContactPhone, brand.burgundy, "dash_card_contact"),
                        DashboardCardItem(ManagementDestination.SOCIAL_MEDIA, ManagementSection.SOCIAL_MEDIA, mStrings.cardSocialMedia, "${socialLinks.size}", Icons.Outlined.Share, brand.forestGreen, "dash_card_social"),
                        DashboardCardItem(ManagementDestination.MEDIA_STUDIO, ManagementSection.MEDIA_STUDIO, mStrings.cardMediaStudio, "${mediaItems.size}", Icons.Outlined.PermMedia, brand.burgundy, "dash_card_media_studio"),
                        DashboardCardItem(ManagementDestination.STAFF_MANAGEMENT, ManagementSection.STAFF_MANAGEMENT, mStrings.cardStaffManagement, "${accounts.size}", Icons.Outlined.AdminPanelSettings, brand.forestGreen, "dash_card_staff"),
                        DashboardCardItem(ManagementDestination.APPROVAL_CENTER, null, mStrings.cardApprovalCenter, "$pendingApprovalsCount", Icons.Outlined.Approval, brand.burgundy, "dash_card_approvals"),
                        DashboardCardItem(ManagementDestination.VERSION_HISTORY, null, mStrings.cardVersionHistory, "${versions.size}", Icons.Outlined.History, brand.forestGreen, "dash_card_history"),
                        DashboardCardItem(ManagementDestination.ACTIVITY_LOG, ManagementSection.ACTIVITY_LOG, mStrings.cardActivityLog, "${logs.size}", Icons.AutoMirrored.Outlined.ReceiptLong, brand.burgundy, "dash_card_activity_log"),
                        DashboardCardItem(ManagementDestination.NOTIFICATIONS, ManagementSection.NOTIFICATIONS, mStrings.cardNotifications, "$unreadCount", Icons.Outlined.Notifications, brand.forestGreen, "dash_card_notifications"),
                        DashboardCardItem(ManagementDestination.GLOBAL_SEARCH, null, mStrings.cardGlobalSearch, "11", Icons.Outlined.Search, brand.burgundy, "dash_card_search"),
                        DashboardCardItem(ManagementDestination.BACKUP_RESTORE, ManagementSection.BACKUP_RESTORE, mStrings.cardBackupRestore, "AES", Icons.Outlined.Backup, brand.forestGreen, "dash_card_backup"),
                        DashboardCardItem(ManagementDestination.APP_SETTINGS, ManagementSection.APP_SETTINGS, mStrings.cardAppSettings, currentLanguage.code.uppercase(), Icons.Outlined.Settings, brand.burgundy, "dash_card_settings")
                    )

                    val visibleCards = cards.filter { card ->
                        when (card.destination) {
                            ManagementDestination.APPROVAL_CENTER -> {
                                managementViewModel.isMasterAdmin() ||
                                    ManagementSection.entries.any {
                                        managementViewModel.hasPermission(it, PermissionAction.PUBLISH) ||
                                            managementViewModel.hasPermission(it, PermissionAction.EDIT)
                                    }
                            }
                            ManagementDestination.VERSION_HISTORY -> {
                                managementViewModel.isMasterAdmin() ||
                                    ManagementSection.entries.any { managementViewModel.hasPermission(it, PermissionAction.VIEW) }
                            }
                            ManagementDestination.GLOBAL_SEARCH -> true
                            else -> card.section == null || managementViewModel.hasPermission(card.section, PermissionAction.VIEW)
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("management_dashboard_grid")
                    ) {
                        item(
                            key = "parent_portal_mgmt_header",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            ParentPortalManagementExpandableHeader(
                                strings = parentAdminStrings,
                                expanded = parentPortalMgmtExpanded,
                                onToggleExpand = {
                                    managementViewModel.onUserInteraction()
                                    parentPortalMgmtExpanded = !parentPortalMgmtExpanded
                                }
                            )
                        }

                        if (parentPortalMgmtExpanded) {
                            itemsIndexed(
                                items = ParentPortalAdminModule.entries,
                                key = { _, module -> "ppm_${module.key}" }
                            ) { index, module ->
                                ParentPortalSubmenuCard(
                                    module = module,
                                    indexNumber = index + 1,
                                    title = parentAdminStrings.modules.getValue(module).title,
                                    onClick = {
                                        managementViewModel.onUserInteraction()
                                        managementViewModel.clearFeedback()
                                        currentDestination = module.toManagementDestination()
                                    }
                                )
                            }

                            item(
                                key = "parent_portal_mgmt_divider",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                HorizontalDivider(
                                    color = brand.royalGold.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        items(visibleCards, key = { it.destination.name }) { card ->
                            Card(
                                onClick = {
                                    managementViewModel.onUserInteraction()
                                    managementViewModel.clearFeedback()
                                    currentDestination = card.destination
                                },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.testTag(card.testTag)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = card.accentColor.copy(alpha = 0.14f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = card.icon,
                                                    contentDescription = card.title,
                                                    tint = card.accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            color = brand.royalGold.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = card.countText,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = card.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
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
                        onNavigateBack = {
                            managementViewModel.clearFeedback()
                            currentDestination = ManagementDestination.DASHBOARD
                        },
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
                ManagementDestination.MEDIA_STUDIO -> MediaStudioScreen(managementViewModel, mStrings)
                ManagementDestination.STAFF_MANAGEMENT -> StaffManagementScreen(managementViewModel, mStrings)
                ManagementDestination.APPROVAL_CENTER -> ApprovalCenterScreen(managementViewModel, mStrings)
                ManagementDestination.VERSION_HISTORY -> VersionHistoryScreen(managementViewModel, mStrings)
                ManagementDestination.ACTIVITY_LOG -> ActivityLogScreen(managementViewModel, mStrings)
                ManagementDestination.NOTIFICATIONS -> NotificationsScreen(managementViewModel, mStrings)
                ManagementDestination.GLOBAL_SEARCH -> GlobalSearchScreen(managementViewModel, mStrings) { sec ->
                    currentDestination = when (sec) {
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
                        ManagementSection.MEDIA_STUDIO -> ManagementDestination.MEDIA_STUDIO
                        ManagementSection.STAFF_MANAGEMENT -> ManagementDestination.STAFF_MANAGEMENT
                        else -> ManagementDestination.DASHBOARD
                    }
                }
                ManagementDestination.BACKUP_RESTORE -> BackupRestoreScreen(managementViewModel, mStrings)
                ManagementDestination.APP_SETTINGS -> {
                    SettingsScreen(
                        currentLanguage = currentLanguage,
                        currentThemeMode = currentThemeMode,
                        onLanguageSelected = { mainViewModel.selectLanguage(it, markOnboarded = true) },
                        onThemeModeSelected = { mainViewModel.selectThemeMode(it) }
                    )
                }
                else -> {
                    val parentModule = currentDestination.toParentPortalAdminModule()
                    if (parentModule != null) {
                        ParentPortalAdminModuleScreen(
                            module = parentModule,
                            currentLanguage = currentLanguage,
                            onBackToDashboard = {
                                managementViewModel.onUserInteraction()
                                managementViewModel.clearFeedback()
                                currentDestination = ManagementDestination.DASHBOARD
                            }
                        )
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
