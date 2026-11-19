package com.example.ui.screens.management

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction
import com.example.data.repository.PublishSaveMode
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.ManagementViewModel

@Composable
internal fun WorkflowStatusBadge(
    mStrings: ManagementStrings,
    isPublished: Boolean,
    isApproved: Boolean,
    approvalStatus: String
) {
    val brand = LocalBrandPalette.current
    val (label, bg, fg) = when {
        isPublished && isApproved && approvalStatus == ApprovalStatus.APPROVED ->
            Triple(mStrings.statusPublished, brand.forestGreenContainer, brand.onForestGreenContainer)
        approvalStatus == ApprovalStatus.PENDING ->
            Triple(mStrings.statusPendingApproval, brand.royalGold.copy(alpha = 0.28f), MaterialTheme.colorScheme.onSurface)
        approvalStatus == ApprovalStatus.REJECTED ->
            Triple(mStrings.statusRejected, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        else ->
            Triple(
                mStrings.workflowStatusLabel(approvalStatus, isPublished, isApproved),
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T : Any> ManagedListScaffold(
    mStrings: ManagementStrings,
    section: ManagementSection,
    managementViewModel: ManagementViewModel,
    items: List<T>,
    itemKey: (T) -> Any,
    onAddNew: () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    val brand = LocalBrandPalette.current
    val canCreate = managementViewModel.hasPermission(section, PermissionAction.CREATE)

    Scaffold(
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(
                    onClick = {
                        managementViewModel.onUserInteraction()
                        managementViewModel.clearFeedback()
                        onAddNew()
                    },
                    containerColor = brand.burgundy,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("mgmt_fab_add_${section.key.lowercase()}")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = mStrings.addNewBtn)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (items.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(mStrings.noRecordsYet, style = MaterialTheme.typography.bodyMedium)
                                if (canCreate) {
                                    Button(onClick = {
                                        managementViewModel.clearFeedback()
                                        onAddNew()
                                    }) {
                                        Text(mStrings.addNewBtn)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(items, key = itemKey) { item ->
                        itemContent(item)
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }
}

@Composable
internal fun ManagedItemCard(
    mStrings: ManagementStrings,
    title: String,
    subtitle: String,
    body: String,
    isPublished: Boolean,
    isApproved: Boolean,
    approvalStatus: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canPublish: Boolean,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    val brand = LocalBrandPalette.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(mStrings.deleteRecordTitle) },
            text = { Text(mStrings.deleteRecordMsg(title)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(mStrings.deleteBtn) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(mStrings.cancelBtn) }
            }
        )
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkflowStatusBadge(mStrings, isPublished, isApproved, approvalStatus)
                Row {
                    if (canPublish) {
                        IconButton(onClick = onTogglePublish) {
                            Icon(
                                imageVector = if (isPublished) Icons.Outlined.VisibilityOff else Icons.Outlined.Public,
                                contentDescription = if (isPublished) mStrings.unpublishBtn else mStrings.publishBtn,
                                tint = if (isPublished) brand.burgundy else brand.forestGreen
                            )
                        }
                    }
                    if (canEdit) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("managed_edit_btn_${title.lowercase().take(24)}")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = mStrings.editBtn)
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = mStrings.deleteBtn,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = brand.burgundy)
            }
            if (body.isNotBlank()) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkflowSaveActionsBar(
    mStrings: ManagementStrings,
    canPublish: Boolean,
    isBusy: Boolean = false,
    cancelLabel: String? = null,
    onCancel: (() -> Unit)? = null,
    onAction: (PublishSaveMode) -> Unit
) {
    val brand = LocalBrandPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (onCancel != null && !cancelLabel.isNullOrBlank()) {
            TextButton(
                onClick = onCancel,
                enabled = !isBusy,
                modifier = Modifier.testTag("editor_cancel_btn")
            ) {
                Text(cancelLabel)
            }
        }
        OutlinedButton(
            onClick = { onAction(PublishSaveMode.SAVE_DRAFT) },
            enabled = !isBusy,
            modifier = Modifier.testTag("editor_save_draft_btn")
        ) {
            Text(mStrings.saveDraftBtn)
        }
        Button(
            onClick = { onAction(PublishSaveMode.SUBMIT_FOR_APPROVAL) },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = brand.burgundy, contentColor = Color.White),
            modifier = Modifier.testTag("editor_submit_approval_btn")
        ) {
            Text(mStrings.submitForApprovalBtn)
        }
        if (canPublish) {
            Button(
                onClick = { onAction(PublishSaveMode.PUBLISH_NOW) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = brand.forestGreen, contentColor = Color.White),
                modifier = Modifier.testTag("editor_publish_now_btn")
            ) {
                Text(mStrings.publishNowBtn)
            }
        }
    }
}

internal fun isValidHttpUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) return false
    return try {
        val uri = Uri.parse(trimmed)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        (scheme == "https" || scheme == "http") &&
            !host.isNullOrBlank() &&
            host.none { it.isWhitespace() } &&
            (host.equals("localhost", ignoreCase = true) || host.contains('.'))
    } catch (_: Throwable) {
        false
    }
}

internal fun isValidPhoneNumber(phone: String): Boolean {
    val trimmed = phone.trim()
    if (trimmed.isBlank()) return false
    if (!trimmed.all { it.isDigit() || it in " +-()." }) return false
    val plusCount = trimmed.count { it == '+' }
    if (plusCount > 1 || (plusCount == 1 && !trimmed.startsWith('+'))) return false
    val digitCount = trimmed.count { it.isDigit() }
    return digitCount in 7..15
}

private val EMAIL_FORMAT_REGEX = Regex(
    "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"
)

internal fun isValidEmailAddress(email: String): Boolean {
    val trimmed = email.trim()
    if (trimmed.isBlank() || trimmed.any { it.isWhitespace() } || trimmed.contains("..")) return false
    return EMAIL_FORMAT_REGEX.matches(trimmed)
}
