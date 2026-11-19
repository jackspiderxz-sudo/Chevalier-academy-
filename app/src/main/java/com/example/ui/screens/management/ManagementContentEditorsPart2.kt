package com.example.ui.screens.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.media.CropAspectRatioPreset
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.PublishSaveMode
import com.example.ui.components.AnnouncementDetailDialog
import com.example.ui.components.NewsDetailDialog
import com.example.ui.screens.AnnouncementItemCard
import com.example.ui.screens.GalleryAlbumCard
import com.example.ui.screens.NewsItemCard
import com.example.ui.screens.SocialPlatformSlotCard
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class NewsEditorSession(
    val target: NewsItemEntity,
    val liveBaseline: NewsItemEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun NewsEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allNews.collectAsStateWithLifecycle()
    val mediaStudioItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<NewsEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.NEWS, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.NEWS, PermissionAction.EDIT)
    }
    val canDelete = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.NEWS, PermissionAction.DELETE)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.NEWS, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var title by remember(session) { mutableStateOf(session.target.title) }
        var summary by remember(session) { mutableStateOf(session.target.summary) }
        var body by remember(session) { mutableStateOf(session.target.fullBody) }
        val defaultDateText = remember(session) { session.target.publishedDateText.ifBlank { mStrings.formatDate() } }
        var dateText by remember(session) { mutableStateOf(defaultDateText) }
        var isFeatured by remember(session) { mutableStateOf(session.target.isFeatured) }
        var imageUri by remember(session) { mutableStateOf(session.target.imageUri?.takeIf { it.isNotBlank() }) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }
        var showPreviewDetailDialog by remember(session) { mutableStateOf(false) }

        val cleanImageUri = imageUri?.trim()?.takeIf { it.isNotEmpty() }
        val baselineUri = baseline.imageUri?.trim()?.takeIf { it.isNotEmpty() }
        val baselineDate = baseline.publishedDateText.ifBlank { defaultDateText }
        val isDirty = title != baseline.title ||
            summary != baseline.summary ||
            body != baseline.fullBody ||
            dateText != baselineDate ||
            isFeatured != baseline.isFeatured ||
            cleanImageUri != baselineUri
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.NEWS,
                    recordId = session.target.id,
                    recordTitle = session.target.title.ifBlank { session.liveBaseline.title }
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.newsFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    title = reverted.title
                    summary = reverted.summary
                    body = reverted.fullBody
                    dateText = reverted.publishedDateText.ifBlank { mStrings.formatDate() }
                    isFeatured = reverted.isFeatured
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
            title = if (session.target.id == 0L) mStrings.newsAddTitle else mStrings.newsEditTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardNews,
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
                if (title.isBlank() || summary.isBlank() || body.isBlank()) {
                    validationError = mStrings.newsRequiredError
                    return@UniversalContentEditorScaffold
                }
                if (cleanImageUri != null && !mediaManager.isManagedMediaFileReadable(cleanImageUri)) {
                    validationError = mStrings.mediaUi.missingOrUnreadableError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    title = title.trim(),
                    summary = summary.trim(),
                    fullBody = body.trim(),
                    publishedDateText = dateText.trim().ifBlank { mStrings.formatDate() },
                    isFeatured = isFeatured,
                    imageUri = cleanImageUri
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.NEWS,
                    recordId = session.target.id,
                    recordTitle = next.title,
                    proposedJson = ContentSnapshotSerializer.newsToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardNews),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val appStrings = LocalAppStrings.current
                val previewItem = remember(session.target.id, title, summary, body, dateText, isFeatured, cleanImageUri) {
                    session.target.copy(
                        title = title.trim().ifBlank { mStrings.newsHeadlineLabel },
                        summary = summary.trim().ifBlank { mStrings.newsSummaryLabel },
                        fullBody = body.trim().ifBlank { summary.trim() },
                        publishedDateText = dateText.trim().ifBlank { mStrings.formatDate() },
                        isFeatured = isFeatured,
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
                                contentDescription = previewItem.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("news_preview_image")
                            )
                        } else {
                            ErrorBanner(mStrings.mediaUi.missingOrUnreadableError)
                        }
                    }
                    NewsItemCard(
                        item = previewItem,
                        badgeText = if (previewItem.isFeatured) appStrings.featuredNewsBadge else appStrings.tabNews,
                        highlighted = previewItem.isFeatured,
                        onClick = { showPreviewDetailDialog = true },
                        showImage = false
                    )
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mStrings.newsFullBodyLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = previewItem.fullBody.ifBlank { previewItem.summary },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (showPreviewDetailDialog) {
                    NewsDetailDialog(
                        newsItem = previewItem,
                        onDismiss = { showPreviewDetailDialog = false }
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; validationError = null },
                label = { Text(mStrings.newsHeadlineLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier.fillMaxWidth().testTag("news_editor_title")
            )
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it; validationError = null },
                label = { Text(mStrings.newsSummaryLabel) },
                enabled = effectiveCanEdit && !isBusy,
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("news_editor_summary")
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it; validationError = null },
                label = { Text(mStrings.newsFullBodyLabel) },
                enabled = effectiveCanEdit && !isBusy,
                minLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("news_editor_body")
            )
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text(mStrings.newsDateTextLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier.fillMaxWidth().testTag("news_editor_date")
            )
            UniversalMediaField(
                mStrings = mStrings,
                label = mStrings.mediaUi.fieldNewsImageLabel,
                selectedUri = imageUri,
                onUriChanged = { imageUri = it; validationError = null },
                mediaStudioItems = mediaStudioItems,
                defaultAspectPreset = CropAspectRatioPreset.RATIO_16_9,
                outputTag = "news",
                enabled = effectiveCanEdit && !isBusy,
                testTagPrefix = "news_media"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isFeatured,
                    onCheckedChange = { if (effectiveCanEdit && !isBusy) isFeatured = it },
                    enabled = effectiveCanEdit && !isBusy,
                    modifier = Modifier.testTag("news_editor_featured_checkbox")
                )
                Text(mStrings.newsFeaturedCheckLabel)
            }
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.NEWS, managementViewModel, items, { it.id },
        onAddNew = {
            editorSession = NewsEditorSession(
                target = NewsItemEntity(title = "", summary = "", fullBody = "", publishedDateText = mStrings.formatDate()),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = item.title,
            subtitle = "${item.publishedDateText}${if (item.isFeatured) " • ${mStrings.newsFeaturedTag}" else ""}",
            body = item.summary,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = canDelete,
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.NEWS, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.newsFromJson(it, publishNow = false).copy(id = item.id) }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.newsFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id) }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = NewsEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.NEWS, item.id, item.isPublished) },
            onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.NEWS, item.id, item.title) }
        )
    }
}

private data class AnnouncementEditorSession(
    val target: AnnouncementEntity,
    val liveBaseline: AnnouncementEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun AnnouncementsEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allAnnouncements.collectAsStateWithLifecycle()
    val mediaStudioItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<AnnouncementEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.ANNOUNCEMENTS, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.ANNOUNCEMENTS, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.ANNOUNCEMENTS, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var title by remember(session) { mutableStateOf(session.target.title) }
        var message by remember(session) { mutableStateOf(session.target.message) }
        val defaultPriority = remember(session) {
            session.target.priorityLabel ?: mStrings.announcementDefaultPriority
        }
        var priority by remember(session) { mutableStateOf(defaultPriority) }
        val defaultDateText = remember(session) {
            session.target.issuedDateText.ifBlank { mStrings.formatDate() }
        }
        var issuedDate by remember(session) { mutableStateOf(defaultDateText) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var imageUri by remember(session) { mutableStateOf(session.target.imageUri?.takeIf { it.isNotBlank() }) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }
        var showPreviewDetailDialog by remember(session) { mutableStateOf(false) }

        val cleanImageUri = imageUri?.trim()?.takeIf { it.isNotEmpty() }
        val baselineUri = baseline.imageUri?.trim()?.takeIf { it.isNotEmpty() }
        val baselinePriority = baseline.priorityLabel ?: mStrings.announcementDefaultPriority
        val baselineDate = baseline.issuedDateText.ifBlank { defaultDateText }
        val isDirty = title != baseline.title ||
            message != baseline.message ||
            priority != baselinePriority ||
            issuedDate != baselineDate ||
            orderText != baseline.displayOrder.toString() ||
            cleanImageUri != baselineUri
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.ANNOUNCEMENTS,
                    recordId = session.target.id,
                    recordTitle = session.target.title.ifBlank { session.liveBaseline.title }
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.announcementFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    title = reverted.title
                    message = reverted.message
                    priority = reverted.priorityLabel ?: mStrings.announcementDefaultPriority
                    issuedDate = reverted.issuedDateText.ifBlank { mStrings.formatDate() }
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
            title = if (session.target.id == 0L) mStrings.announcementsAddTitle else mStrings.announcementsEditTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardAnnouncements,
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
                if (title.isBlank() || message.isBlank()) {
                    validationError = mStrings.announcementsRequiredError
                    return@UniversalContentEditorScaffold
                }
                if (cleanImageUri != null && !mediaManager.isManagedMediaFileReadable(cleanImageUri)) {
                    validationError = mStrings.mediaUi.missingOrUnreadableError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    title = title.trim(),
                    message = message.trim(),
                    priorityLabel = priority.trim().takeIf { it.isNotEmpty() },
                    issuedDateText = issuedDate.trim().ifBlank { mStrings.formatDate() },
                    displayOrder = orderText.toIntOrNull() ?: 0,
                    imageUri = cleanImageUri
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.ANNOUNCEMENTS,
                    recordId = session.target.id,
                    recordTitle = next.title,
                    proposedJson = ContentSnapshotSerializer.announcementToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardAnnouncements),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val previewItem = remember(
                    session.target.id, title, message, priority, issuedDate, orderText, cleanImageUri
                ) {
                    session.target.copy(
                        title = title.trim().ifBlank { mStrings.announcementTitleLabel },
                        message = message.trim().ifBlank { mStrings.announcementMessageLabel },
                        priorityLabel = priority.trim().takeIf { it.isNotEmpty() },
                        issuedDateText = issuedDate.trim().ifBlank { mStrings.formatDate() },
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
                                contentDescription = previewItem.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("announcement_preview_image")
                            )
                        } else {
                            ErrorBanner(mStrings.mediaUi.missingOrUnreadableError)
                        }
                    }
                    AnnouncementItemCard(
                        item = previewItem,
                        onClick = { showPreviewDetailDialog = true },
                        showImage = false
                    )
                    Text(
                        text = mStrings.orderSubtitle(previewItem.displayOrder),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showPreviewDetailDialog) {
                    AnnouncementDetailDialog(
                        announcement = previewItem,
                        onDismiss = { showPreviewDetailDialog = false }
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; validationError = null },
                label = { Text(mStrings.announcementTitleLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("announcement_editor_title")
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it; validationError = null },
                label = { Text(mStrings.announcementMessageLabel) },
                enabled = effectiveCanEdit && !isBusy,
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("announcement_editor_message")
            )
            OutlinedTextField(
                value = priority,
                onValueChange = { priority = it },
                label = { Text(mStrings.announcementPriorityLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("announcement_editor_priority")
            )
            OutlinedTextField(
                value = issuedDate,
                onValueChange = { issuedDate = it },
                label = { Text(mStrings.announcementIssuedDateLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("announcement_editor_date")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { c -> c.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("announcement_editor_order")
            )
            UniversalMediaField(
                mStrings = mStrings,
                label = mStrings.mediaUi.fieldAnnouncementImageLabel,
                selectedUri = imageUri,
                onUriChanged = {
                    imageUri = it
                    validationError = null
                },
                mediaStudioItems = mediaStudioItems,
                defaultAspectPreset = CropAspectRatioPreset.RATIO_4_3,
                outputTag = "announcement",
                enabled = effectiveCanEdit && !isBusy,
                testTagPrefix = "announcement_media"
            )
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.ANNOUNCEMENTS, managementViewModel, items, { it.id },
        onAddNew = {
            editorSession = AnnouncementEditorSession(
                target = AnnouncementEntity(title = "", message = "", issuedDateText = mStrings.formatDate()),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = item.title,
            subtitle = "${item.priorityLabel ?: mStrings.announcementDefaultPriority} • ${item.issuedDateText} • ${mStrings.orderSubtitle(item.displayOrder)}",
            body = item.message,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = managementViewModel.hasPermission(ManagementSection.ANNOUNCEMENTS, PermissionAction.DELETE),
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.ANNOUNCEMENTS, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.announcementFromJson(it, publishNow = false).copy(id = item.id) }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.announcementFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id) }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = AnnouncementEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.ANNOUNCEMENTS, item.id, item.isPublished) },
            onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.ANNOUNCEMENTS, item.id, item.title) }
        )
    }
}

private data class GalleryAlbumEditorSession(
    val target: GalleryAlbumEntity,
    val liveBaseline: GalleryAlbumEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun PhotoGalleryEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val albums by managementViewModel.allGalleryAlbumsWithMedia.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()

    var editorSession by remember { mutableStateOf<GalleryAlbumEditorSession?>(null) }
    var selectedAlbumIdForDetail by remember { mutableStateOf<Long?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.PUBLISH)
    }

    val selectedAlbumForDetail = selectedAlbumIdForDetail?.let { targetId ->
        albums.firstOrNull { it.album.id == targetId }
    }
    if (selectedAlbumForDetail != null) {
        GalleryAlbumDetailEditor(
            albumWithMedia = selectedAlbumForDetail,
            managementViewModel = managementViewModel,
            mStrings = mStrings,
            onBack = {
                managementViewModel.clearFeedback()
                selectedAlbumIdForDetail = null
            }
        )
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var title by remember(session) { mutableStateOf(session.target.title) }
        var category by remember(session) { mutableStateOf(session.target.category) }
        var desc by remember(session) { mutableStateOf(session.target.description.orEmpty()) }
        val defaultDateText = remember(session) { session.target.eventDateText ?: mStrings.formatDate() }
        var dateText by remember(session) { mutableStateOf(defaultDateText) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }

        val baselineDate = baseline.eventDateText ?: defaultDateText
        val isDirty = title != baseline.title ||
            category != baseline.category ||
            desc != baseline.description.orEmpty() ||
            dateText != baselineDate ||
            orderText != baseline.displayOrder.toString()
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.PHOTO_GALLERY,
                    recordId = session.target.id,
                    recordTitle = session.target.title.ifBlank { session.liveBaseline.title },
                    isGalleryMediaItem = false
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.galleryAlbumFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    title = reverted.title
                    category = reverted.category
                    desc = reverted.description.orEmpty()
                    dateText = reverted.eventDateText ?: mStrings.formatDate()
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
            title = if (session.target.id == 0L) mStrings.galleryCreateAlbumTitle else mStrings.galleryEditAlbumTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardPhotoGallery,
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
                if (title.isBlank()) {
                    validationError = mStrings.galleryAlbumRequiredError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    title = title.trim(),
                    category = category.trim().ifBlank { "Campus" },
                    description = desc.trim().takeIf { it.isNotEmpty() },
                    eventDateText = dateText.trim().takeIf { it.isNotEmpty() },
                    displayOrder = orderText.toIntOrNull() ?: 0
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.PHOTO_GALLERY,
                    recordId = session.target.id,
                    recordTitle = next.title,
                    proposedJson = ContentSnapshotSerializer.galleryAlbumToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardPhotoGallery),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val matchingAlbum = albums.firstOrNull { it.album.id == session.target.id }
                val existingMedia = matchingAlbum?.mediaItems.orEmpty()
                val previewAlbum = remember(
                    session.target, title, category, desc, dateText, orderText
                ) {
                    session.target.copy(
                        title = title.trim().ifBlank { mStrings.galleryAlbumTitleLabel },
                        category = category.trim().ifBlank { "Campus" },
                        description = desc.trim().takeIf { it.isNotEmpty() },
                        eventDateText = dateText.trim().takeIf { it.isNotEmpty() },
                        displayOrder = orderText.toIntOrNull() ?: 0,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true
                    )
                }
                val previewAlbumWithMedia = remember(previewAlbum, existingMedia) {
                    GalleryAlbumWithMedia(album = previewAlbum, mediaItems = existingMedia)
                }
                val coverUri = previewAlbum.coverMediaUri?.takeIf { it.isNotBlank() }
                val isCoverReadable by produceState(initialValue = coverUri != null, coverUri) {
                    value = withContext(Dispatchers.IO) {
                        coverUri != null && mediaManager.isManagedMediaFileReadable(coverUri)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (coverUri != null && isCoverReadable) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = brand.royalGold.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = mStrings.mediaUi.galleryAlbumCoverBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(coverUri)
                                    .size(960)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = previewAlbum.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(176.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("gallery_album_preview_cover")
                            )
                        }
                    }
                    GalleryAlbumCard(
                        albumWithMedia = previewAlbumWithMedia,
                        onClick = {},
                        modifier = Modifier.testTag("gallery_album_preview_card")
                    )
                    Text(
                        text = listOfNotNull(
                            previewAlbum.eventDateText?.let { "${mStrings.galleryEventDateLabel}: $it" },
                            mStrings.orderSubtitle(previewAlbum.displayOrder)
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (existingMedia.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(existingMedia, key = { it.id }) { media ->
                                val isYouTube = media.mediaType.equals("YOUTUBE", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.size(84.dp)
                                ) {
                                    if (isYouTube) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.VideoLibrary,
                                                contentDescription = media.title ?: mStrings.mediaUi.galleryYouTubeVideoLabel,
                                                tint = brand.burgundy
                                            )
                                        }
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(media.mediaUri)
                                                .size(256)
                                                .crossfade(false)
                                                .build(),
                                            contentDescription = media.title ?: previewAlbum.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; validationError = null },
                label = { Text(mStrings.galleryAlbumTitleLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_album_editor_title")
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(mStrings.galleryAlbumCategoryLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_album_editor_category")
            )
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text(mStrings.galleryDescriptionLabel) },
                enabled = effectiveCanEdit && !isBusy,
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_album_editor_description")
            )
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text(mStrings.galleryEventDateLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_album_editor_date")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { c -> c.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_album_editor_order")
            )
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.PHOTO_GALLERY, managementViewModel, albums, { it.album.id },
        onAddNew = {
            editorSession = GalleryAlbumEditorSession(
                target = GalleryAlbumEntity(title = "", category = "Campus"),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { albumWithMedia ->
        val album = albumWithMedia.album
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ManagedItemCard(
                mStrings = mStrings,
                title = album.title,
                subtitle = "${mStrings.galleryAlbumSubtitle(album.category, albumWithMedia.mediaItems.size)} • ${mStrings.orderSubtitle(album.displayOrder)}",
                body = album.description ?: "",
                isPublished = album.isPublished,
                isApproved = album.isApproved,
                approvalStatus = album.approvalStatus,
                canEdit = canEdit,
                canDelete = managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.DELETE),
                canPublish = canPublish,
                onEdit = {
                    managementViewModel.clearFeedback()
                    scope.launch {
                        val res = managementViewModel.resolveEditableRevision(
                            section = ManagementSection.PHOTO_GALLERY,
                            recordId = album.id,
                            isGalleryMediaItem = false
                        )
                        val editable = res.editableSnapshotJson?.let {
                            runCatching { ContentSnapshotSerializer.galleryAlbumFromJson(it, publishNow = false).copy(id = album.id) }.getOrNull()
                        } ?: album
                        val live = res.liveSnapshotJson?.let {
                            runCatching { ContentSnapshotSerializer.galleryAlbumFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = album.id) }.getOrNull()
                        } ?: if (res.hasLivePublishedVersion) album else null
                        editorSession = GalleryAlbumEditorSession(
                            target = editable,
                            liveBaseline = live,
                            initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                        )
                    }
                },
                onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.PHOTO_GALLERY, album.id, album.isPublished) },
                onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.PHOTO_GALLERY, album.id, album.title) }
            )
            Button(
                onClick = {
                    managementViewModel.clearFeedback()
                    selectedAlbumIdForDetail = album.id
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = brand.burgundy,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_manage_photos_btn_${album.id}")
            ) {
                Icon(Icons.Outlined.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(mStrings.galleryManagePhotosBtn(albumWithMedia.mediaItems.size))
            }
        }
    }
}

private data class SocialMediaEditorSession(
    val target: SocialMediaLinkEntity,
    val liveBaseline: SocialMediaLinkEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@Composable
fun SocialMediaEditorScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allSocialLinks.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editorSession by remember { mutableStateOf<SocialMediaEditorSession?>(null) }

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SOCIAL_MEDIA, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SOCIAL_MEDIA, PermissionAction.EDIT)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SOCIAL_MEDIA, PermissionAction.PUBLISH)
    }

    editorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var platform by remember(session) { mutableStateOf(session.target.platformName) }
        var handle by remember(session) { mutableStateOf(session.target.handleOrLabel) }
        var url by remember(session) { mutableStateOf(session.target.url) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }

        val isDirty = platform != baseline.platformName ||
            handle != baseline.handleOrLabel ||
            url != baseline.url ||
            orderText != baseline.displayOrder.toString()
        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit
        val urlInvalid = validationError == mStrings.socialInvalidUrlError && !isValidHttpUrl(url)

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.SOCIAL_MEDIA,
                    recordId = session.target.id,
                    recordTitle = session.target.platformName.ifBlank { session.liveBaseline.platformName }
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.socialFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    baseline = reverted
                    platform = reverted.platformName
                    handle = reverted.handleOrLabel
                    url = reverted.url
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
            title = if (session.target.id == 0L) mStrings.socialAddTitle else mStrings.socialEditTitle,
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = mStrings.cardSocialMedia,
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
                if (platform.isBlank() || handle.isBlank()) {
                    validationError = mStrings.socialRequiredError
                    return@UniversalContentEditorScaffold
                }
                if (!isValidHttpUrl(url)) {
                    validationError = mStrings.socialInvalidUrlError
                    return@UniversalContentEditorScaffold
                }
                validationError = null
                val next = session.target.copy(
                    platformName = platform.trim(),
                    handleOrLabel = handle.trim(),
                    url = url.trim(),
                    displayOrder = orderText.toIntOrNull() ?: 0
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.SOCIAL_MEDIA,
                    recordId = session.target.id,
                    recordTitle = next.platformName,
                    proposedJson = ContentSnapshotSerializer.socialToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(mStrings.cardSocialMedia),
                    onSuccess = { editorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                editorSession = null
            },
            previewContent = {
                val previewLink = remember(session.target.id, platform, handle, url, orderText) {
                    session.target.copy(
                        platformName = platform.trim().ifBlank { "Instagram" },
                        handleOrLabel = handle.trim().ifBlank { mStrings.socialHandleLabel },
                        url = url.trim().ifBlank { "https://" },
                        displayOrder = orderText.toIntOrNull() ?: 0,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialPlatformSlotCard(
                        platformName = previewLink.platformName,
                        verifiedLink = previewLink,
                        onOpenLink = {},
                        modifier = Modifier.testTag("social_editor_live_preview")
                    )
                    Text(
                        text = previewLink.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mStrings.orderSubtitle(previewLink.displayOrder),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it; validationError = null },
                label = { Text(mStrings.socialPlatformLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError == mStrings.socialRequiredError && platform.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("social_editor_platform")
            )
            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it; validationError = null },
                label = { Text(mStrings.socialHandleLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = validationError == mStrings.socialRequiredError && handle.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("social_editor_handle")
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; validationError = null },
                label = { Text(mStrings.socialUrlLabel) },
                enabled = effectiveCanEdit && !isBusy,
                isError = urlInvalid,
                supportingText = if (urlInvalid) {
                    { Text(mStrings.socialInvalidUrlError) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("social_editor_url")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { c -> c.isDigit() } },
                label = { Text(mStrings.heroDisplayOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("social_editor_order")
            )
        }
    }

    ManagedListScaffold(
        mStrings, ManagementSection.SOCIAL_MEDIA, managementViewModel, items, { it.id },
        onAddNew = {
            editorSession = SocialMediaEditorSession(
                target = SocialMediaLinkEntity(platformName = "Instagram", handleOrLabel = "", url = "https://"),
                liveBaseline = null,
                initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
            )
        }
    ) { item ->
        ManagedItemCard(
            mStrings = mStrings,
            title = "${item.platformName} — ${item.handleOrLabel}",
            subtitle = mStrings.orderSubtitle(item.displayOrder),
            body = item.url,
            isPublished = item.isPublished,
            isApproved = item.isApproved,
            approvalStatus = item.approvalStatus,
            canEdit = canEdit,
            canDelete = managementViewModel.hasPermission(ManagementSection.SOCIAL_MEDIA, PermissionAction.DELETE),
            canPublish = canPublish,
            onEdit = {
                managementViewModel.clearFeedback()
                scope.launch {
                    val res = managementViewModel.resolveEditableRevision(ManagementSection.SOCIAL_MEDIA, item.id)
                    val editable = res.editableSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.socialFromJson(it, publishNow = false).copy(id = item.id) }.getOrNull()
                    } ?: item
                    val live = res.liveSnapshotJson?.let {
                        runCatching { ContentSnapshotSerializer.socialFromJson(it, publishNow = res.hasLivePublishedVersion).copy(id = item.id) }.getOrNull()
                    } ?: if (res.hasLivePublishedVersion) item else null
                    editorSession = SocialMediaEditorSession(
                        target = editable,
                        liveBaseline = live,
                        initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                    )
                }
            },
            onTogglePublish = { managementViewModel.togglePublishState(ManagementSection.SOCIAL_MEDIA, item.id, item.isPublished) },
            onDeleteConfirmed = { managementViewModel.deleteRecord(ManagementSection.SOCIAL_MEDIA, item.id, item.platformName) }
        )
    }
}
