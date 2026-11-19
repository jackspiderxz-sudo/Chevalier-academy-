package com.example.ui.screens.management

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.i18n.ManagementStrings
import com.example.data.repository.EditableRecordResolution
import com.example.data.repository.PublishSaveMode
import com.example.data.repository.ResumedRevisionKind
import com.example.ui.theme.LocalBrandPalette

enum class EditorRevisionKind {
    NONE,
    DRAFT,
    PENDING_APPROVAL
}

@Immutable
data class EditorRevisionInfo(
    val kind: EditorRevisionKind = EditorRevisionKind.NONE,
    val versionNumber: Int? = null,
    val authorName: String? = null,
    val timestampMillis: Long? = null,
    val hasLiveVersion: Boolean = false
) {
    companion object {
        fun fromResolution(res: EditableRecordResolution): EditorRevisionInfo = EditorRevisionInfo(
            kind = when (res.revisionKind) {
                ResumedRevisionKind.DRAFT -> EditorRevisionKind.DRAFT
                ResumedRevisionKind.PENDING_APPROVAL -> EditorRevisionKind.PENDING_APPROVAL
                ResumedRevisionKind.NONE -> EditorRevisionKind.NONE
            },
            versionNumber = res.versionNumber,
            authorName = res.revisionAuthor,
            timestampMillis = res.revisionTimestampMillis,
            hasLiveVersion = res.hasLivePublishedVersion
        )
    }
}

enum class UniversalEditorTab {
    EDIT,
    PREVIEW
}

@Composable
internal fun UniversalContentEditorScaffold(
    mStrings: ManagementStrings,
    title: String,
    canEdit: Boolean,
    canPublish: Boolean,
    isDirty: Boolean,
    isBusy: Boolean,
    isFullScreenDialog: Boolean = true,
    subtitle: String? = null,
    revisionInfo: EditorRevisionInfo = EditorRevisionInfo(),
    errorMessage: String? = null,
    statusMessage: String? = null,
    onClearError: () -> Unit = {},
    onClearStatus: () -> Unit = {},
    onResetToLive: (() -> Unit)? = null,
    onSaveAction: (PublishSaveMode) -> Unit,
    onClose: () -> Unit,
    onRegisterNavigationGuard: (((() -> Unit) -> Unit)?) -> Unit = {},
    dialogTestTag: String? = null,
    previewContent: (@Composable () -> Unit)? = null,
    editContent: @Composable ColumnScope.() -> Unit
) {
    var activeTab by remember(previewContent != null) { mutableStateOf(UniversalEditorTab.EDIT) }
    var pendingDiscardAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val latestDirty by rememberUpdatedState(isDirty)
    val attemptLeave: (() -> Unit) -> Unit = remember {
        { onProceed ->
            if (latestDirty) {
                pendingDiscardAction = onProceed
            } else {
                onClearError()
                onProceed()
            }
        }
    }

    if (!isFullScreenDialog) {
        DisposableEffect(attemptLeave) {
            onRegisterNavigationGuard(attemptLeave)
            onDispose { onRegisterNavigationGuard(null) }
        }
    }

    val confirmDiscardDialog = @Composable {
        pendingDiscardAction?.let { onProceed ->
            AlertDialog(
                onDismissRequest = { pendingDiscardAction = null },
                title = { Text(mStrings.editorDiscardChangesTitle) },
                text = { Text(mStrings.editorDiscardChangesMsg) },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingDiscardAction = null
                            onClearError()
                            onProceed()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("editor_confirm_discard_btn")
                    ) {
                        Text(mStrings.editorDiscardBtn)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingDiscardAction = null },
                        modifier = Modifier.testTag("editor_keep_editing_btn")
                    ) {
                        Text(mStrings.editorKeepEditingBtn)
                    }
                },
                modifier = Modifier.testTag("editor_discard_confirm_dialog")
            )
        }
    }

    val editorBody: @Composable () -> Unit = {
        val brand = LocalBrandPalette.current
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFullScreenDialog) {
                        Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .imePadding()
                    } else {
                        Modifier.imePadding()
                    }
                )
                .testTag("universal_content_editor_scaffold"),
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isFullScreenDialog) {
                        Surface(
                            color = brand.burgundy,
                            contentColor = Color.White,
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { attemptLeave(onClose) },
                                    enabled = !isBusy,
                                    modifier = Modifier.testTag("editor_close_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = mStrings.closeBtn,
                                        tint = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val metaLine = when {
                                        isDirty -> mStrings.editorUnsavedIndicator
                                        !subtitle.isNullOrBlank() -> subtitle
                                        else -> null
                                    }
                                    if (!metaLine.isNullOrBlank()) {
                                        Text(
                                            text = metaLine,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDirty) brand.royalGold else Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("editor_unsaved_indicator")
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isDirty || !subtitle.isNullOrBlank()) {
                        Surface(
                            color = if (isDirty) brand.royalGold.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subtitle ?: title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isDirty) {
                                    Text(
                                        text = mStrings.editorUnsavedIndicator,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = brand.burgundy,
                                        modifier = Modifier.testTag("editor_unsaved_indicator")
                                    )
                                }
                            }
                        }
                    }

                    if (previewContent != null) {
                        TabRow(
                            selectedTabIndex = if (activeTab == UniversalEditorTab.EDIT) 0 else 1,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = brand.burgundy
                        ) {
                            Tab(
                                selected = activeTab == UniversalEditorTab.EDIT,
                                onClick = { activeTab = UniversalEditorTab.EDIT },
                                text = { Text(mStrings.editorEditTab) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = mStrings.editorEditTab,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("editor_tab_edit")
                            )
                            Tab(
                                selected = activeTab == UniversalEditorTab.PREVIEW,
                                onClick = { activeTab = UniversalEditorTab.PREVIEW },
                                text = { Text(mStrings.editorPreviewTab) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Visibility,
                                        contentDescription = mStrings.editorPreviewTab,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("editor_tab_preview")
                            )
                        }
                    }

                    if (isBusy) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_loading_indicator"),
                            color = brand.royalGold
                        )
                    }
                }
            },
            bottomBar = {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_bottom_actions_bar")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                if (canEdit) {
                                    WorkflowSaveActionsBar(
                                        mStrings = mStrings,
                                        canPublish = canPublish,
                                        isBusy = isBusy,
                                        cancelLabel = mStrings.cancelBtn,
                                        onCancel = { attemptLeave(onClose) },
                                        onAction = onSaveAction
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = onClose,
                                            modifier = Modifier.testTag("editor_cancel_btn")
                                        ) {
                                            Text(mStrings.closeBtn)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val bodyScrollState = rememberScrollState()
            LaunchedEffect(activeTab) {
                bodyScrollState.scrollTo(0)
            }
            LaunchedEffect(errorMessage) {
                if (!errorMessage.isNullOrBlank()) {
                    bodyScrollState.animateScrollTo(0)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .verticalScroll(bodyScrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    errorMessage?.takeIf { it.isNotBlank() }?.let { err ->
                        ErrorBanner(
                            message = err,
                            dismissLabel = mStrings.dismissBtn,
                            onDismiss = onClearError
                        )
                    }

                    statusMessage?.takeIf { it.isNotBlank() }?.let { ok ->
                        SuccessBanner(
                            message = ok,
                            okLabel = mStrings.okBtn,
                            onDismiss = onClearStatus
                        )
                    }

                    if (!canEdit) {
                        InfoBanner(mStrings.noPermissionNotice)
                    }

                    if (revisionInfo.kind != EditorRevisionKind.NONE) {
                        RevisionResumptionBanner(
                            mStrings = mStrings,
                            revisionInfo = revisionInfo,
                            canResetToLive = canEdit && revisionInfo.hasLiveVersion && onResetToLive != null && !isBusy,
                            onResetToLive = { onResetToLive?.invoke() }
                        )
                    }

                    if (activeTab == UniversalEditorTab.PREVIEW && previewContent != null) {
                        InfoBanner(mStrings.editorPreviewCaption)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_preview_container")
                        ) {
                            previewContent()
                        }
                    } else {
                        editContent()
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        confirmDiscardDialog()
    }

    if (isFullScreenDialog) {
        Dialog(
            onDismissRequest = { attemptLeave(onClose) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler {
                attemptLeave(onClose)
            }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (dialogTestTag != null) Modifier.testTag(dialogTestTag) else Modifier),
                color = MaterialTheme.colorScheme.background
            ) {
                editorBody()
            }
        }
    } else {
        BackHandler(enabled = isDirty) {
            attemptLeave(onClose)
        }
        editorBody()
    }
}

@Composable
private fun RevisionResumptionBanner(
    mStrings: ManagementStrings,
    revisionInfo: EditorRevisionInfo,
    canResetToLive: Boolean,
    onResetToLive: () -> Unit
) {
    val brand = LocalBrandPalette.current
    val isPending = revisionInfo.kind == EditorRevisionKind.PENDING_APPROVAL
    val containerColor = if (isPending) {
        brand.burgundy.copy(alpha = 0.08f)
    } else {
        brand.royalGold.copy(alpha = 0.18f)
    }
    val borderColor = if (isPending) {
        brand.burgundy.copy(alpha = 0.45f)
    } else {
        brand.royalGold.copy(alpha = 0.75f)
    }
    val titleText = if (isPending) {
        mStrings.editorPendingRevisionTitle(revisionInfo.versionNumber)
    } else {
        mStrings.editorResumingDraftTitle(revisionInfo.versionNumber)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_revision_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isPending) Icons.Outlined.HourglassTop else Icons.Outlined.EditNote,
                    contentDescription = titleText,
                    tint = if (isPending) brand.burgundy else brand.goldAccentText,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!revisionInfo.authorName.isNullOrBlank() && revisionInfo.timestampMillis != null) {
                        Text(
                            text = mStrings.editorDraftMeta(
                                revisionInfo.authorName,
                                mStrings.formatTimestamp(revisionInfo.timestampMillis)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isPending) {
                        Text(
                            text = mStrings.editorPendingPreservedHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.burgundy
                        )
                    }
                }
            }
            if (canResetToLive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onResetToLive,
                        modifier = Modifier.testTag("editor_reset_to_live_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = mStrings.editorResetToLiveBtn,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.editorResetToLiveBtn)
                    }
                }
            }
        }
    }
}
