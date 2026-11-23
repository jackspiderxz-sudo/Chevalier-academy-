package com.example.ui.screens.management

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction
import com.example.data.media.CropAspectRatioPreset
import com.example.data.media.MediaStudioManager
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.PublishSaveMode
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureDialogProperties
import com.example.ui.components.SecureFullScreenDialogProperties
import com.example.ui.components.GalleryMediaTileContent
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class GalleryMediaEditorSession(
    val target: GalleryMediaEntity,
    val liveBaseline: GalleryMediaEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryAlbumDetailEditor(
    albumWithMedia: GalleryAlbumWithMedia,
    managementViewModel: ManagementViewModel,
    mStrings: ManagementStrings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()

    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()
    val mediaStudioItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.EDIT)
    }
    val canDelete = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.DELETE)
    }
    val canPublish = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.PUBLISH)
    }

    val album = albumWithMedia.album
    val sortedItems = remember(albumWithMedia.mediaItems) {
        albumWithMedia.mediaItems.sortedWith(
            compareBy<GalleryMediaEntity> { it.displayOrder }
                .thenByDescending { it.addedAtMillis }
                .thenBy { it.id }
        )
    }

    var mediaEditorSession by remember { mutableStateOf<GalleryMediaEditorSession?>(null) }
    var deleteCandidate by remember { mutableStateOf<GalleryMediaEntity?>(null) }

    mediaEditorSession?.let { session ->
        var baseline by remember(session) { mutableStateOf(session.target) }
        var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
        var isYouTube by remember(session) {
            mutableStateOf(session.target.mediaType.equals("YOUTUBE", ignoreCase = true))
        }
        var mediaUri by remember(session) {
            mutableStateOf(
                if (!session.target.mediaType.equals("YOUTUBE", ignoreCase = true)) {
                    session.target.mediaUri
                } else {
                    ""
                }
            )
        }
        var youtubeUrl by remember(session) {
            mutableStateOf(
                if (session.target.mediaType.equals("YOUTUBE", ignoreCase = true)) {
                    session.target.mediaUri
                } else {
                    ""
                }
            )
        }
        var mediaTitle by remember(session) { mutableStateOf(session.target.title.orEmpty()) }
        var mediaCaption by remember(session) { mutableStateOf(session.target.caption.orEmpty()) }
        var orderText by remember(session) { mutableStateOf(session.target.displayOrder.toString()) }
        var validationError by remember(session) { mutableStateOf<String?>(null) }
        var showPreviewLightbox by remember(session) { mutableStateOf(false) }

        val baselineIsYouTube = baseline.mediaType.equals("YOUTUBE", ignoreCase = true)
        val currentResolvedUri = if (isYouTube) youtubeUrl.trim() else mediaUri.trim()
        val isDirty = isYouTube != baselineIsYouTube ||
            currentResolvedUri != baseline.mediaUri.trim() ||
            mediaTitle != baseline.title.orEmpty() ||
            mediaCaption != baseline.caption.orEmpty() ||
            orderText != baseline.displayOrder.toString()

        val effectiveCanEdit = if (session.target.id == 0L) (canCreate || canEdit) else canEdit

        val resetHandler: (() -> Unit)? = if (session.liveBaseline != null && session.target.id > 0L) {
            {
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.PHOTO_GALLERY,
                    recordId = session.target.id,
                    recordTitle = session.target.title
                        ?: session.liveBaseline.title
                        ?: mStrings.galleryMediaDefaultTitle,
                    isGalleryMediaItem = true
                ) { liveJson ->
                    val reverted = liveJson?.let {
                        runCatching {
                            ContentSnapshotSerializer.galleryMediaFromJson(it, publishNow = true)
                                .copy(id = session.target.id)
                        }.getOrNull()
                    } ?: session.liveBaseline
                    val revertedYouTube = reverted.mediaType.equals("YOUTUBE", ignoreCase = true)
                    baseline = reverted
                    isYouTube = revertedYouTube
                    mediaUri = if (!revertedYouTube) reverted.mediaUri else ""
                    youtubeUrl = if (revertedYouTube) reverted.mediaUri else ""
                    mediaTitle = reverted.title.orEmpty()
                    mediaCaption = reverted.caption.orEmpty()
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
            title = if (session.target.id == 0L) {
                mStrings.galleryAddMediaTitle(album.title)
            } else {
                mStrings.mediaUi.galleryEditMediaTitle
            },
            canEdit = effectiveCanEdit,
            canPublish = canPublish,
            isDirty = isDirty,
            isBusy = isBusy,
            isFullScreenDialog = true,
            subtitle = album.title,
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
                val resolvedUri = if (isYouTube) youtubeUrl.trim() else mediaUri.trim()
                if (isYouTube) {
                    if (!MediaStudioManager.isValidYouTubeUrl(resolvedUri)) {
                        validationError = mStrings.mediaInvalidYouTubeError
                        return@UniversalContentEditorScaffold
                    }
                } else {
                    if (resolvedUri.isBlank()) {
                        validationError = mStrings.galleryMediaImageRequiredError
                        return@UniversalContentEditorScaffold
                    }
                    if (!mediaManager.isManagedMediaFileReadable(resolvedUri)) {
                        validationError = mStrings.mediaUi.missingOrUnreadableError
                        return@UniversalContentEditorScaffold
                    }
                }
                validationError = null
                val next = session.target.copy(
                    albumId = album.id,
                    title = mediaTitle.trim().takeIf { it.isNotEmpty() },
                    caption = mediaCaption.trim().takeIf { it.isNotEmpty() },
                    mediaUri = resolvedUri,
                    mediaType = if (isYouTube) "YOUTUBE" else "IMAGE",
                    displayOrder = orderText.toIntOrNull() ?: session.target.displayOrder
                )
                managementViewModel.saveManagedRecord(
                    section = ManagementSection.PHOTO_GALLERY,
                    recordId = session.target.id,
                    recordTitle = next.title ?: mStrings.galleryMediaDefaultTitle,
                    proposedJson = ContentSnapshotSerializer.galleryMediaToJson(next),
                    saveMode = mode,
                    changeSummary = mStrings.changeSummaryUpdated(album.title),
                    onSuccess = { mediaEditorSession = null }
                )
            },
            onClose = {
                managementViewModel.clearFeedback()
                mediaEditorSession = null
            },
            dialogTestTag = "gallery_media_edit_dialog",
            previewContent = {
                val appStrings = LocalAppStrings.current
                val resolvedUri = if (isYouTube) youtubeUrl.trim() else mediaUri.trim()
                val previewMedia = remember(
                    session.target.id, album.id, isYouTube, resolvedUri, mediaTitle, mediaCaption, orderText
                ) {
                    session.target.copy(
                        albumId = album.id,
                        title = mediaTitle.trim().takeIf { it.isNotEmpty() } ?: mStrings.galleryMediaDefaultTitle,
                        caption = mediaCaption.trim().takeIf { it.isNotEmpty() },
                        mediaUri = resolvedUri,
                        mediaType = if (isYouTube) "YOUTUBE" else "IMAGE",
                        displayOrder = orderText.toIntOrNull() ?: session.target.displayOrder,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true
                    )
                }
                val isPreviewReadable by produceState(initialValue = false, isYouTube, resolvedUri) {
                    value = withContext(Dispatchers.IO) {
                        if (isYouTube) {
                            MediaStudioManager.isValidYouTubeUrl(resolvedUri)
                        } else {
                            resolvedUri.isNotBlank() && mediaManager.isManagedMediaFileReadable(resolvedUri)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isYouTube && resolvedUri.isNotBlank() && !isPreviewReadable) {
                        ErrorBanner(mStrings.mediaUi.missingOrUnreadableError)
                    } else if (isYouTube && resolvedUri.isNotBlank() && !isPreviewReadable) {
                        ErrorBanner(mStrings.mediaInvalidYouTubeError)
                    }
                    Card(
                        onClick = {
                            if (resolvedUri.isNotBlank() && (isYouTube || isPreviewReadable)) {
                                showPreviewLightbox = true
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .testTag("gallery_media_preview_card")
                    ) {
                        GalleryMediaTileContent(
                            media = previewMedia,
                            fallbackContentDescription = previewMedia.title ?: appStrings.titleGallery,
                            showCaptionOverlay = true,
                            isImageReadable = if (isYouTube) true else (resolvedUri.isBlank() || isPreviewReadable),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = listOfNotNull(
                            album.title,
                            mStrings.orderSubtitle(previewMedia.displayOrder),
                            if (isYouTube) mStrings.mediaUi.galleryYouTubeVideoLabel else null
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showPreviewLightbox) {
                    PhotoLightboxDialog(
                        photos = listOf(previewMedia),
                        currentIndex = 0,
                        onIndexChange = {},
                        onDismiss = { showPreviewLightbox = false }
                    )
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isYouTube,
                    onCheckedChange = {
                        if (effectiveCanEdit && !isBusy) {
                            isYouTube = it
                            validationError = null
                        }
                    },
                    enabled = effectiveCanEdit && !isBusy,
                    modifier = Modifier.testTag("gallery_media_youtube_switch")
                )
                Text(mStrings.galleryYouTubeSwitchLabel)
            }

            if (isYouTube) {
                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = { youtubeUrl = it; validationError = null },
                    label = { Text(mStrings.galleryYouTubeUrlRequiredLabel) },
                    enabled = effectiveCanEdit && !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gallery_media_youtube_input")
                )
            } else {
                UniversalMediaField(
                    mStrings = mStrings,
                    label = mStrings.mediaUi.fieldGalleryPhotoLabel,
                    selectedUri = mediaUri.ifBlank { null },
                    onUriChanged = { next ->
                        mediaUri = next.orEmpty()
                        validationError = null
                    },
                    mediaStudioItems = mediaStudioItems,
                    defaultAspectPreset = CropAspectRatioPreset.FREE,
                    outputTag = "gallery_media",
                    enabled = effectiveCanEdit && !isBusy,
                    testTagPrefix = "gallery_detail_media_field"
                )
            }

            OutlinedTextField(
                value = mediaTitle,
                onValueChange = { mediaTitle = it },
                label = { Text(mStrings.galleryMediaTitleLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_media_title_input")
            )
            OutlinedTextField(
                value = mediaCaption,
                onValueChange = { mediaCaption = it },
                label = { Text(mStrings.mediaCaptionLabel) },
                enabled = effectiveCanEdit && !isBusy,
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_media_caption_input")
            )
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter { ch -> ch.isDigit() } },
                label = { Text(mStrings.mediaUi.galleryMediaOrderLabel) },
                enabled = effectiveCanEdit && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_media_order_input")
            )
        }
    }

    deleteCandidate?.let { candidate ->
        val candidateLabel = candidate.title ?: mStrings.galleryMediaDefaultTitle
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.deleteRecordTitle)
            },
            text = { Text(mStrings.deleteRecordMsg(candidateLabel)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate = null
                        managementViewModel.deleteRecord(
                            section = ManagementSection.PHOTO_GALLERY,
                            recordId = candidate.id,
                            recordTitle = candidateLabel,
                            isGalleryMediaItem = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("gallery_delete_confirm_btn")
                ) {
                    Text(mStrings.deleteBtn)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteCandidate = null },
                    modifier = Modifier.testTag("gallery_delete_cancel_btn")
                ) {
                    Text(mStrings.cancelBtn)
                }
            },
            modifier = Modifier.testTag("gallery_delete_confirm_dialog"),
            properties = SecureDialogProperties
        )
    }

    Dialog(
        onDismissRequest = onBack,
        properties = SecureFullScreenDialogProperties
    ) {
        ApplySecureDialogWindow()
        BackHandler { onBack() }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("gallery_album_detail_editor"),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding(),
                topBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = brand.burgundy,
                            contentColor = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("gallery_detail_back_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = mStrings.mediaUi.galleryDetailBackDesc,
                                        tint = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mStrings.galleryDetailTitle(album.title),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = mStrings.galleryAlbumSubtitle(album.category, sortedItems.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = brand.royalGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                },
                floatingActionButton = {
                    if (canCreate) {
                        FloatingActionButton(
                            onClick = {
                                managementViewModel.clearFeedback()
                                val nextOrder = (sortedItems.maxOfOrNull { it.displayOrder } ?: -1) + 1
                                mediaEditorSession = GalleryMediaEditorSession(
                                    target = GalleryMediaEntity(
                                        albumId = album.id,
                                        mediaUri = "",
                                        displayOrder = nextOrder
                                    ),
                                    liveBaseline = null,
                                    initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
                                )
                            },
                            containerColor = brand.burgundy,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("gallery_detail_add_btn")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = mStrings.addNewBtn)
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                            .testTag("gallery_detail_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vmError?.takeIf { it.isNotBlank() }?.let { err ->
                            item {
                                ErrorBanner(err, dismissLabel = mStrings.dismissBtn) {
                                    managementViewModel.clearFeedback()
                                }
                            }
                        }
                        vmStatus?.takeIf { it.isNotBlank() }?.let { ok ->
                            item {
                                SuccessBanner(ok, okLabel = mStrings.okBtn) {
                                    managementViewModel.clearFeedback()
                                }
                            }
                        }

                        // Album cover summary card
                        item {
                            val coverUri = album.coverMediaUri?.takeIf { it.isNotBlank() }
                            val coverReadable = coverUri != null && mediaManager.isManagedMediaFileReadable(coverUri)
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gallery_album_cover_summary")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        if (coverReadable) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(coverUri)
                                                    .size(256)
                                                    .crossfade(false)
                                                    .build(),
                                                contentDescription = mStrings.mediaUi.galleryAlbumCoverBadge,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PhotoLibrary,
                                                    contentDescription = mStrings.mediaUi.galleryAlbumCoverBadge,
                                                    tint = brand.burgundy
                                                )
                                            }
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        WorkflowStatusBadge(
                                            mStrings = mStrings,
                                            isPublished = album.isPublished,
                                            isApproved = album.isApproved,
                                            approvalStatus = album.approvalStatus
                                        )
                                        Text(
                                            text = album.description ?: album.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (sortedItems.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gallery_detail_empty_state")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = mStrings.mediaUi.galleryDetailEmpty,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (canCreate) {
                                            Button(
                                                onClick = {
                                                    managementViewModel.clearFeedback()
                                                    mediaEditorSession = GalleryMediaEditorSession(
                                                        target = GalleryMediaEntity(
                                                            albumId = album.id,
                                                            mediaUri = "",
                                                            displayOrder = 0
                                                        ),
                                                        liveBaseline = null,
                                                        initialRevisionInfo = EditorRevisionInfo(hasLiveVersion = false)
                                                    )
                                                }
                                            ) {
                                                Text(mStrings.addNewBtn)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(sortedItems, key = { _, item -> item.id }) { index, media ->
                                val isYouTube = media.mediaType.equals("YOUTUBE", ignoreCase = true)
                                val isReadable = isYouTube || mediaManager.isManagedMediaFileReadable(media.mediaUri)
                                val isCover = !isYouTube && album.coverMediaUri == media.mediaUri
                                val itemTitle = media.title ?: mStrings.galleryMediaDefaultTitle

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCover) brand.royalGold else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gallery_detail_item_${media.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(72.dp)
                                            ) {
                                                when {
                                                    isYouTube -> {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.VideoLibrary,
                                                                contentDescription = mStrings.mediaUi.galleryYouTubeVideoLabel,
                                                                tint = brand.burgundy,
                                                                modifier = Modifier.size(30.dp)
                                                            )
                                                        }
                                                    }
                                                    isReadable -> {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(media.mediaUri)
                                                                .size(256)
                                                                .crossfade(false)
                                                                .build(),
                                                            contentDescription = itemTitle,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(10.dp))
                                                        )
                                                    }
                                                    else -> {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.BrokenImage,
                                                                contentDescription = mStrings.mediaUi.badgeUnavailableLabel,
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    WorkflowStatusBadge(
                                                        mStrings = mStrings,
                                                        isPublished = media.isPublished,
                                                        isApproved = media.isApproved,
                                                        approvalStatus = media.approvalStatus
                                                    )
                                                    if (isCover) {
                                                        Surface(
                                                            color = brand.royalGold.copy(alpha = 0.25f),
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier
                                                                .semantics(mergeDescendants = true) {}
                                                                .testTag("gallery_cover_badge_${media.id}")
                                                        ) {
                                                            Text(
                                                                text = mStrings.mediaUi.galleryAlbumCoverBadge,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = itemTitle,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = mStrings.orderSubtitle(media.displayOrder),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = brand.burgundy
                                                )
                                                media.caption?.takeIf { it.isNotBlank() }?.let { cap ->
                                                    Text(
                                                        text = cap,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (canEdit) {
                                                IconButton(
                                                    onClick = {
                                                        managementViewModel.moveGalleryMediaOrder(
                                                            albumId = album.id,
                                                            mediaId = media.id,
                                                            moveUp = true,
                                                            updatedMsg = mStrings.mediaUi.galleryOrderUpdatedStatus,
                                                            submittedMsg = mStrings.mediaUi.galleryOrderSubmittedStatus
                                                        )
                                                    },
                                                    enabled = !isBusy && index > 0,
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("gallery_move_up_${media.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ArrowUpward,
                                                        contentDescription = mStrings.mediaUi.galleryMoveUpDesc
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        managementViewModel.moveGalleryMediaOrder(
                                                            albumId = album.id,
                                                            mediaId = media.id,
                                                            moveUp = false,
                                                            updatedMsg = mStrings.mediaUi.galleryOrderUpdatedStatus,
                                                            submittedMsg = mStrings.mediaUi.galleryOrderSubmittedStatus
                                                        )
                                                    },
                                                    enabled = !isBusy && index < sortedItems.lastIndex,
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("gallery_move_down_${media.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ArrowDownward,
                                                        contentDescription = mStrings.mediaUi.galleryMoveDownDesc
                                                    )
                                                }
                                                if (!isYouTube && isReadable && !isCover) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            managementViewModel.setGalleryAlbumCover(
                                                                albumId = album.id,
                                                                coverMediaUri = media.mediaUri,
                                                                publishedMsg = mStrings.mediaUi.galleryCoverSetPublishedStatus,
                                                                submittedMsg = mStrings.mediaUi.galleryCoverSubmittedStatus
                                                            )
                                                        },
                                                        enabled = !isBusy,
                                                        modifier = Modifier
                                                            .minimumInteractiveComponentSize()
                                                            .testTag("gallery_set_cover_${media.id}")
                                                    ) {
                                                        Icon(Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.size(4.dp))
                                                        Text(mStrings.mediaUi.gallerySetCoverBtn)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        managementViewModel.clearFeedback()
                                                        scope.launch {
                                                            val res = managementViewModel.resolveEditableRevision(
                                                                section = ManagementSection.PHOTO_GALLERY,
                                                                recordId = media.id,
                                                                isGalleryMediaItem = true
                                                            )
                                                            val editable = res.editableSnapshotJson?.let {
                                                                runCatching {
                                                                    ContentSnapshotSerializer.galleryMediaFromJson(it, publishNow = false)
                                                                        .copy(id = media.id)
                                                                }.getOrNull()
                                                            } ?: media
                                                            val live = res.liveSnapshotJson?.let {
                                                                runCatching {
                                                                    ContentSnapshotSerializer.galleryMediaFromJson(it, publishNow = res.hasLivePublishedVersion)
                                                                        .copy(id = media.id)
                                                                }.getOrNull()
                                                            } ?: if (res.hasLivePublishedVersion) media else null
                                                            mediaEditorSession = GalleryMediaEditorSession(
                                                                target = editable,
                                                                liveBaseline = live,
                                                                initialRevisionInfo = EditorRevisionInfo.fromResolution(res)
                                                            )
                                                        }
                                                    },
                                                    enabled = !isBusy,
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("gallery_edit_media_${media.id}")
                                                ) {
                                                    Icon(Icons.Outlined.Edit, contentDescription = mStrings.editBtn)
                                                }
                                            }
                                            if (canPublish) {
                                                IconButton(
                                                    onClick = {
                                                        managementViewModel.togglePublishState(
                                                            section = ManagementSection.PHOTO_GALLERY,
                                                            recordId = media.id,
                                                            currentlyPublished = media.isPublished,
                                                            isGalleryMediaItem = true
                                                        )
                                                    },
                                                    enabled = !isBusy,
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("gallery_toggle_publish_${media.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = if (media.isPublished) Icons.Outlined.VisibilityOff else Icons.Outlined.Public,
                                                        contentDescription = if (media.isPublished) mStrings.unpublishBtn else mStrings.publishBtn,
                                                        tint = if (media.isPublished) brand.burgundy else brand.forestGreen
                                                    )
                                                }
                                            }
                                            if (canDelete) {
                                                IconButton(
                                                    onClick = { deleteCandidate = media },
                                                    enabled = !isBusy,
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("gallery_delete_media_${media.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = mStrings.deleteBtn,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}
