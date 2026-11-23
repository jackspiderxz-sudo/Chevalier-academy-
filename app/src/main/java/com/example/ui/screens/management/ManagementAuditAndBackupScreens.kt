package com.example.ui.screens.management

import android.Manifest
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureDialogProperties
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.ManagementStrings
import com.example.data.backend.IntegrationState
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.VersionHistoryEntity
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.ManagementViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VersionHistoryScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val versions by managementViewModel.allVersionHistory.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current
    var inspectingVersion by remember { mutableStateOf<VersionHistoryEntity?>(null) }

    inspectingVersion?.let { v ->
        val sec = ManagementSection.fromKey(v.sectionKey)
        val canEdit = sec != null && managementViewModel.hasPermission(sec, PermissionAction.EDIT)
        val canPublish = sec != null && managementViewModel.hasPermission(sec, PermissionAction.PUBLISH)
        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                inspectingVersion = null
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.versionHeader(v.versionNumber, v.recordTitle))
            },
            text = {
                Column(modifier = Modifier.height(320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vmError?.let {
                        ErrorBanner(it, dismissLabel = mStrings.dismissBtn) {
                            managementViewModel.clearFeedback()
                        }
                    }
                    Text(
                        mStrings.versionChangedBy(v.changedByName, mStrings.formatTimestamp(v.changedAtMillis)),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        mStrings.versionStatus(mStrings.workflowStatusLabel(v.approvalStatus)),
                        style = MaterialTheme.typography.labelMedium,
                        color = brand.burgundy
                    )
                    HorizontalDivider()
                    Text(mStrings.versionSnapshotPayload, style = MaterialTheme.typography.titleSmall)
                    Text(v.contentSnapshotJson, style = MaterialTheme.typography.bodySmall)
                    if (!v.previousContentJson.isNullOrBlank()) {
                        HorizontalDivider()
                        Text(mStrings.versionPreviousPayload, style = MaterialTheme.typography.titleSmall)
                        Text(v.previousContentJson, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (canEdit) {
                    Button(
                        onClick = {
                            managementViewModel.restoreVersion(v.id, publishImmediately = canPublish)
                            inspectingVersion = null
                        },
                        modifier = Modifier.testTag("restore_version_confirm_btn")
                    ) { Text(if (canPublish) mStrings.versionRestoreAndPublishBtn else mStrings.versionRestoreAsRevisionBtn) }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    managementViewModel.clearFeedback()
                    inspectingVersion = null
                }) { Text(mStrings.closeBtn) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (versions.isEmpty()) {
                item { InfoBanner(mStrings.noRecordsYet) }
            } else {
                items(versions, key = { it.id }) { v ->
                    val sec = ManagementSection.fromKey(v.sectionKey)
                    Card(
                        onClick = {
                            managementViewModel.clearFeedback()
                            inspectingVersion = v
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "v${v.versionNumber} • ${sec?.let { mStrings.sectionLabel(it) } ?: v.sectionKey}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = brand.burgundy
                                )
                                Text(
                                    mStrings.workflowStatusLabel(v.approvalStatus),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = brand.forestGreen
                                )
                            }
                            Text(v.recordTitle, style = MaterialTheme.typography.titleSmall)
                            Text(mStrings.versionSummaryBy(v.changeSummary, v.changedByName), style = MaterialTheme.typography.bodySmall)
                            Text(mStrings.formatTimestamp(v.changedAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityLogScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val logs by managementViewModel.visibleActivityLogs.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedSectionFilter by remember { mutableStateOf<String?>(null) }
    var sortNewestFirst by remember { mutableStateOf(true) }

    val distinctSections = remember(logs) { logs.map { it.sectionKey }.distinct() }
    val filtered = remember(logs, searchQuery, selectedSectionFilter, sortNewestFirst) {
        val base = logs.filter { log ->
            val matchesSection = selectedSectionFilter == null || log.sectionKey == selectedSectionFilter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                log.actorName.lowercase().contains(q) ||
                log.actionType.lowercase().contains(q) ||
                log.recordRef.lowercase().contains(q) ||
                log.details.lowercase().contains(q)
            matchesSection && matchesQuery
        }
        if (sortNewestFirst) base else base.reversed()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().testTag("activity_log_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                InfoBanner(mStrings.auditAppendOnlyNotice)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(mStrings.auditSearchLabel) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = sortNewestFirst,
                        onClick = { sortNewestFirst = !sortNewestFirst },
                        label = { Text(if (sortNewestFirst) mStrings.auditSortNewest else mStrings.auditSortOldest) }
                    )
                    FilterChip(
                        selected = selectedSectionFilter == null,
                        onClick = { selectedSectionFilter = null },
                        label = { Text(mStrings.auditAllSections) }
                    )
                    distinctSections.take(6).forEach { secKey ->
                        val secLabel = ManagementSection.fromKey(secKey)?.let { mStrings.sectionLabel(it) } ?: secKey
                        FilterChip(
                            selected = selectedSectionFilter == secKey,
                            onClick = { selectedSectionFilter = if (selectedSectionFilter == secKey) null else secKey },
                            label = { Text(secLabel) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item { InfoBanner(mStrings.noRecordsYet) }
            } else {
                items(filtered, key = { it.id }) { log ->
                    val secLabel = ManagementSection.fromKey(log.sectionKey)?.let { mStrings.sectionLabel(it) } ?: log.sectionKey
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.actionType, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = brand.burgundy)
                                Text(mStrings.formatTimestamp(log.timestampMillis), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${log.actorName} (${mStrings.roleLabel(log.actorRole)}) • $secLabel", style = MaterialTheme.typography.labelMedium)
                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val items by managementViewModel.allNotifications.collectAsStateWithLifecycle()
    val alertsEnabled by managementViewModel.deviceAlertsEnabled.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val canEditNotifications = managementViewModel.hasPermission(ManagementSection.NOTIFICATIONS, PermissionAction.EDIT)
    val canDeleteNotifications = managementViewModel.hasPermission(ManagementSection.NOTIFICATIONS, PermissionAction.DELETE)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNotice = false
            managementViewModel.setDeviceAlertsEnabled(true)
        } else {
            permissionDeniedNotice = true
            managementViewModel.setDeviceAlertsEnabled(false)
        }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.clearNotificationsTitle)
            },
            text = { Text(mStrings.clearNotificationsMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        managementViewModel.clearNotifications {
                            showClearAllConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_notifications_btn")
                ) { Text(mStrings.notificationsClearAll) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text(mStrings.cancelBtn) }
            },
            modifier = Modifier.testTag("clear_notifications_confirm_dialog")
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("device_alerts_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = brand.burgundy
                                )
                                Column {
                                    Text(
                                        text = mStrings.notificationsDeviceAlertsTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = mStrings.notificationsDeviceAlertsSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = alertsEnabled,
                                onCheckedChange = { wantEnable ->
                                    if (!wantEnable) {
                                        permissionDeniedNotice = false
                                        managementViewModel.setDeviceAlertsEnabled(false)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            permissionDeniedNotice = false
                                            managementViewModel.setDeviceAlertsEnabled(true)
                                        } else {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        permissionDeniedNotice = false
                                        managementViewModel.setDeviceAlertsEnabled(true)
                                    }
                                },
                                modifier = Modifier.testTag("device_alerts_switch")
                            )
                        }
                        if (permissionDeniedNotice) {
                            ErrorBanner(
                                message = mStrings.notificationsPermissionDeniedNotice,
                                dismissLabel = mStrings.dismissBtn,
                                onDismiss = { permissionDeniedNotice = false }
                            )
                        }
                    }
                }
            }

            if (items.isNotEmpty() && (canEditNotifications || canDeleteNotifications)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canEditNotifications) {
                            OutlinedButton(
                                onClick = { managementViewModel.markAllNotificationsRead() },
                                modifier = Modifier.testTag("notifications_mark_all_read_btn")
                            ) {
                                Text(mStrings.notificationsMarkAllRead)
                            }
                        }
                        if (canDeleteNotifications) {
                            OutlinedButton(
                                onClick = { showClearAllConfirm = true },
                                modifier = Modifier.testTag("notifications_clear_all_btn")
                            ) {
                                Text(mStrings.notificationsClearAll)
                            }
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                item { InfoBanner(mStrings.notificationsEmpty) }
            } else {
                items(items, key = { it.id }) { note ->
                    Card(
                        onClick = { managementViewModel.markNotificationRead(note.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (note.isRead) MaterialTheme.colorScheme.surface else brand.royalGold.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, if (note.isRead) MaterialTheme.colorScheme.outlineVariant else brand.royalGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(note.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    if (note.isRead) mStrings.notificationRead else mStrings.notificationUnread,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = brand.burgundy
                                )
                            }
                            Text(note.message, style = MaterialTheme.typography.bodySmall)
                            Text(mStrings.formatTimestamp(note.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchScreen(
    managementViewModel: ManagementViewModel,
    mStrings: ManagementStrings,
    onNavigateToSection: (ManagementSection) -> Unit
) {
    val results by managementViewModel.searchResults.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current
    var query by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        managementViewModel.performGlobalSearch(it)
                    },
                    label = { Text(mStrings.globalSearchHint) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("mgmt_global_search_input")
                )
            }

            if (query.isNotBlank() && results.isEmpty()) {
                item { InfoBanner(mStrings.globalSearchNoResults(query)) }
            } else {
                items(results) { res ->
                    Card(
                        onClick = { onNavigateToSection(res.section) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(mStrings.sectionLabel(res.section), style = MaterialTheme.typography.labelMedium, color = brand.burgundy)
                                Text(mStrings.workflowStatusLabel(res.statusLabel), style = MaterialTheme.typography.labelSmall, color = brand.forestGreen)
                            }
                            Text(res.title, style = MaterialTheme.typography.titleMedium)
                            Text(res.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupRestoreScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val backupPreview by managementViewModel.backupPreview.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current

    var backupPassword by remember { mutableStateOf("") }
    var backupConfirm by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var showBackupPassword by remember { mutableStateOf(false) }
    var showBackupConfirm by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }
    var createPasswordError by remember { mutableStateOf<String?>(null) }
    var restorePasswordError by remember { mutableStateOf<String?>(null) }
    var selectedRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            managementViewModel.createBackupToUri(uri, backupPassword, backupConfirm)
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedRestoreUri = uri
            managementViewModel.inspectBackupUri(uri, restorePassword)
        }
    }

    backupPreview?.let { preview ->
        val uri = selectedRestoreUri
        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                managementViewModel.clearBackupPreview()
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.backupPreviewTitle)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    vmError?.let {
                        ErrorBanner(it, dismissLabel = mStrings.dismissBtn) {
                            managementViewModel.clearFeedback()
                        }
                    }
                    Text(mStrings.backupExportedText(mStrings.formatTimestamp(preview.exportedAtMillis)))
                    Text(mStrings.backupAppVersionText(preview.appVersionName, preview.schemaVersion))
                    Text(mStrings.backupAccountsCountText(preview.accountCount))
                    Text(mStrings.backupContentCountText(preview.contentRecordCount))
                    Text(mStrings.backupGalleryMediaCountText(preview.galleryItemCount, preview.mediaFileCount))
                    Text(mStrings.backupHistoryLogCountText(preview.historyAndLogCount))
                    ErrorBanner(mStrings.backupRestoreConfirmWarning)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restorePassword.isBlank()) {
                            restorePasswordError = mStrings.backupRestorePasswordRequiredError
                            managementViewModel.clearBackupPreview()
                        } else if (uri != null) {
                            managementViewModel.confirmRestoreFromUri(uri, restorePassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brand.burgundy, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_backup_restore_btn")
                ) { Text(mStrings.restorePreviewConfirmBtn) }
            },
            dismissButton = {
                TextButton(onClick = {
                    managementViewModel.clearFeedback()
                    managementViewModel.clearBackupPreview()
                }) { Text(mStrings.cancelBtn) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoBanner(mStrings.backupWarningNote)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(mStrings.backupCreateTitle, style = MaterialTheme.typography.titleMedium)
                    createPasswordError?.let { err ->
                        ErrorBanner(
                            message = err,
                            dismissLabel = mStrings.dismissBtn,
                            onDismiss = { createPasswordError = null }
                        )
                    }
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it; createPasswordError = null },
                        label = { Text(mStrings.backupPasswordLabel) },
                        isError = createPasswordError != null,
                        visualTransformation = if (showBackupPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { showBackupPassword = !showBackupPassword },
                                modifier = Modifier.testTag("backup_create_password_toggle")
                            ) {
                                Icon(
                                    imageVector = if (showBackupPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = mStrings.togglePasswordVisibilityDesc
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("backup_create_password_input")
                    )
                    OutlinedTextField(
                        value = backupConfirm,
                        onValueChange = { backupConfirm = it; createPasswordError = null },
                        label = { Text(mStrings.backupConfirmPasswordLabel) },
                        isError = createPasswordError != null,
                        visualTransformation = if (showBackupConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { showBackupConfirm = !showBackupConfirm },
                                modifier = Modifier.testTag("backup_create_confirm_toggle")
                            ) {
                                Icon(
                                    imageVector = if (showBackupConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = mStrings.togglePasswordVisibilityDesc
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("backup_create_confirm_input")
                    )
                    Button(
                        onClick = {
                            when {
                                backupPassword.length < 6 -> {
                                    createPasswordError = mStrings.backupPasswordTooShortError
                                }
                                backupPassword != backupConfirm -> {
                                    createPasswordError = mStrings.backupPasswordMismatchError
                                }
                                else -> {
                                    createPasswordError = null
                                    managementViewModel.clearFeedback()
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date())
                                    exportLauncher.launch("chevalier_backup_$stamp.chvbak")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brand.forestGreen, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_btn")
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(mStrings.exportEncryptedBackupBtn)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(mStrings.backupRestoreTitle, style = MaterialTheme.typography.titleMedium)
                    restorePasswordError?.let { err ->
                        ErrorBanner(
                            message = err,
                            dismissLabel = mStrings.dismissBtn,
                            onDismiss = { restorePasswordError = null }
                        )
                    }
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it; restorePasswordError = null },
                        label = { Text(mStrings.backupPasswordLabel) },
                        isError = restorePasswordError != null,
                        visualTransformation = if (showRestorePassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { showRestorePassword = !showRestorePassword },
                                modifier = Modifier.testTag("backup_restore_password_toggle")
                            ) {
                                Icon(
                                    imageVector = if (showRestorePassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = mStrings.togglePasswordVisibilityDesc
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("backup_restore_password_input")
                    )
                    OutlinedButton(
                        onClick = {
                            if (restorePassword.isBlank()) {
                                restorePasswordError = mStrings.backupRestorePasswordRequiredError
                            } else {
                                restorePasswordError = null
                                managementViewModel.clearFeedback()
                                openBackupLauncher.launch(arrayOf("*/*"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("inspect_restore_backup_btn")
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(mStrings.selectBackupToInspectBtn)
                    }
                }
            }

            managementViewModel.centralBackendStatus?.let { status ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("backend_status_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (status.isRemoteSyncConfigured) Icons.Outlined.Security else Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = brand.burgundy
                            )
                            Text(
                                text = mStrings.backendStatusCardTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        val authModeLabel = if (status.remoteAuth.key == "auth_mode") {
                            mStrings.backendStatusAuthModeLabel
                        } else {
                            status.remoteAuth.label
                        }
                        Text(
                            text = "$authModeLabel: ${
                                if (status.remoteAuth.state == IntegrationState.ACTIVE_LOCAL) mStrings.backendStatusLocalActiveLabel else mStrings.backendStatusUnconfiguredLabel
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.forestGreen
                        )
                        Text(text = mStrings.backendStatusAutoBackupLabel, style = MaterialTheme.typography.bodySmall)
                        Text(text = mStrings.backendStatusRemoteSyncLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = mStrings.backendStatusTenantId(status.tenantId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("backend_status_tenant_id")
                        )
                    }
                }
            }
        }
    }
}
