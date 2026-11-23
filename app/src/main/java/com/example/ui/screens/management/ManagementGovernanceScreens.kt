package com.example.ui.screens.management

import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureDialogProperties
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ChevalierApplication
import com.example.core.i18n.ManagementStrings
import com.example.core.security.PinSecurityManager
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ContentApprovalEntity
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.CropAspectRatioPreset
import com.example.data.media.MediaStudioManager
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaStudioScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val items by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val albums by managementViewModel.allGalleryAlbumsWithMedia.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val mediaManager = remember { (context.applicationContext as ChevalierApplication).container.mediaStudioManager }
    val scope = rememberCoroutineScope()

    val canCreate = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.MEDIA_STUDIO, PermissionAction.CREATE)
    }
    val canEdit = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.MEDIA_STUDIO, PermissionAction.EDIT)
    }
    val canDelete = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.MEDIA_STUDIO, PermissionAction.DELETE)
    }
    val canCreateHeroBanner = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.CREATE)
    }
    val canPublishHeroBanner = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.LOGO_BANNER, PermissionAction.PUBLISH)
    }
    val canCreateGallery = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.CREATE)
    }
    val canPublishGallery = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.PHOTO_GALLERY, PermissionAction.PUBLISH)
    }

    var showYouTubeDialog by remember { mutableStateOf(false) }
    var replaceTargetId by remember { mutableStateOf<Long?>(null) }
    var editMetadataTarget by remember { mutableStateOf<MediaStudioItemEntity?>(null) }
    var assignAlbumTarget by remember { mutableStateOf<MediaStudioItemEntity?>(null) }
    var assignHeroAdjustTarget by remember { mutableStateOf<MediaStudioItemEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<Pair<MediaStudioItemEntity, List<String>>?>(null) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            managementViewModel.importImage(
                uri = uri,
                title = "Campus Photo",
                caption = null,
                categoryOrAlbum = "Campus",
                assignedSections = emptyList(),
                isFeatured = false
            )
        }
    }

    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = replaceTargetId
        if (uri != null && id != null) {
            managementViewModel.replaceMediaImage(id, uri)
        }
        replaceTargetId = null
    }

    if (showYouTubeDialog) {
        var url by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var caption by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }
        val activeError = localError ?: vmError
        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                showYouTubeDialog = false
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.mediaYouTubeDialogTitle)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeError?.let {
                        ErrorBanner(it, dismissLabel = mStrings.dismissBtn) {
                            localError = null
                            managementViewModel.clearFeedback()
                        }
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; localError = null },
                        label = { Text(mStrings.mediaYouTubeUrlLabel) },
                        isError = activeError != null,
                        modifier = Modifier.fillMaxWidth().testTag("media_youtube_url_input")
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; localError = null },
                        label = { Text(mStrings.mediaVideoTitleLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("media_youtube_title_input")
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text(mStrings.mediaCaptionLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUrl = url.trim()
                        val cleanTitle = title.trim()
                        when {
                            !MediaStudioManager.isValidYouTubeUrl(cleanUrl) -> {
                                localError = mStrings.mediaInvalidYouTubeError
                            }
                            cleanTitle.isEmpty() -> {
                                localError = mStrings.mediaVideoTitleRequiredError
                            }
                            else -> {
                                localError = null
                                managementViewModel.addYouTubeVideo(cleanUrl, cleanTitle, caption, "Videos", false) {
                                    showYouTubeDialog = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("media_youtube_submit_btn")
                ) { Text(mStrings.mediaAddVideoBtn) }
            },
            dismissButton = {
                TextButton(onClick = {
                    managementViewModel.clearFeedback()
                    showYouTubeDialog = false
                }) { Text(mStrings.cancelBtn) }
            }
        )
    }

    editMetadataTarget?.let { target ->
        var title by remember(target.id, target.title) { mutableStateOf(target.title) }
        var caption by remember(target.id, target.caption) { mutableStateOf(target.caption.orEmpty()) }
        var category by remember(target.id, target.categoryOrAlbum) { mutableStateOf(target.categoryOrAlbum) }
        var localError by remember(target.id) { mutableStateOf<String?>(null) }
        val activeError = localError ?: vmError

        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                editMetadataTarget = null
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.mediaEditMetadataDialogTitle)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeError?.let { err ->
                        ErrorBanner(err, dismissLabel = mStrings.dismissBtn) {
                            localError = null
                            managementViewModel.clearFeedback()
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            localError = null
                        },
                        label = { Text(mStrings.mediaTitleLabel) },
                        singleLine = true,
                        isError = activeError != null && title.isBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_edit_title_input")
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text(mStrings.mediaCaptionLabel) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_edit_caption_input")
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = {
                            category = it
                            localError = null
                        },
                        label = { Text(mStrings.mediaCategoryLabel) },
                        singleLine = true,
                        isError = activeError != null && category.isBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_edit_category_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTitle = title.trim()
                        val cleanCategory = category.trim()
                        when {
                            cleanTitle.isEmpty() -> {
                                localError = mStrings.mediaTitleRequiredError
                            }
                            cleanCategory.isEmpty() -> {
                                localError = mStrings.mediaCategoryRequiredError
                            }
                            else -> {
                                localError = null
                                managementViewModel.updateMediaMetadata(
                                    mediaId = target.id,
                                    title = cleanTitle,
                                    caption = caption.trim().takeIf { it.isNotEmpty() },
                                    categoryOrAlbum = cleanCategory
                                ) {
                                    editMetadataTarget = null
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("media_edit_metadata_save_btn")
                ) { Text(mStrings.saveChangesBtn) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        managementViewModel.clearFeedback()
                        editMetadataTarget = null
                    },
                    modifier = Modifier.testTag("media_edit_metadata_cancel_btn")
                ) { Text(mStrings.cancelBtn) }
            },
            modifier = Modifier.testTag("media_edit_metadata_dialog")
        )
    }

    if (canCreateGallery) {
        assignAlbumTarget?.let { item ->
            AlertDialog(
                properties = SecureDialogProperties,
                onDismissRequest = {
                    managementViewModel.clearFeedback()
                    assignAlbumTarget = null
                },
                title = {
                    ApplySecureDialogWindow()
                    Text(mStrings.mediaAssignAlbumTitle(item.title))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vmError?.let {
                            ErrorBanner(it, dismissLabel = mStrings.dismissBtn) {
                                managementViewModel.clearFeedback()
                            }
                        }
                        if (albums.isEmpty()) {
                            Text(mStrings.mediaCreateAlbumFirstNotice)
                        } else {
                            albums.forEach { a ->
                                OutlinedButton(
                                    onClick = {
                                        managementViewModel.assignMediaToAlbum(
                                            mediaId = item.id,
                                            albumId = a.album.id,
                                            publishNow = canPublishGallery
                                        )
                                        assignAlbumTarget = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("media_assign_album_option_${a.album.id}")
                                ) {
                                    Text("${a.album.title} (${a.album.category})")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        managementViewModel.clearFeedback()
                        assignAlbumTarget = null
                    }) { Text(mStrings.closeBtn) }
                },
                modifier = Modifier.testTag("media_assign_album_dialog")
            )
        }
    }

    assignHeroAdjustTarget?.let { item ->
        UniversalImageCropDialog(
            mStrings = mStrings,
            sourceUri = item.originalUri,
            initialAspectPreset = CropAspectRatioPreset.RATIO_16_9,
            outputTag = "hero_banner",
            onApply = { _, adjustedUri ->
                managementViewModel.assignMediaToHero(
                    mediaId = item.id,
                    publishNow = canPublishHeroBanner,
                    adjustedUriOverride = adjustedUri
                )
                assignHeroAdjustTarget = null
            },
            onDismiss = { assignHeroAdjustTarget = null }
        )
    }

    deleteCandidate?.let { (item, warnings) ->
        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                deleteCandidate = null
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.mediaDeleteTitle)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    vmError?.let {
                        ErrorBanner(it, dismissLabel = mStrings.dismissBtn) {
                            managementViewModel.clearFeedback()
                        }
                    }
                    Text(mStrings.mediaAssetLabel(item.title))
                    if (warnings.isNotEmpty()) {
                        ErrorBanner(mStrings.mediaInUseWarning(warnings.joinToString(", ")))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        managementViewModel.deleteMediaItem(item.id)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(mStrings.deleteBtn) }
            },
            dismissButton = {
                TextButton(onClick = {
                    managementViewModel.clearFeedback()
                    deleteCandidate = null
                }) { Text(mStrings.cancelBtn) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 860.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (canCreate) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                managementViewModel.clearFeedback()
                                importPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brand.burgundy, contentColor = Color.White),
                            modifier = Modifier.testTag("media_studio_import_image_btn")
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(mStrings.mediaImportImageBtn)
                        }
                        OutlinedButton(
                            onClick = {
                                managementViewModel.clearFeedback()
                                showYouTubeDialog = true
                            },
                            modifier = Modifier.testTag("media_studio_add_youtube_btn")
                        ) {
                            Icon(Icons.Outlined.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(mStrings.mediaAddYouTubeBtn)
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                item { InfoBanner(mStrings.noRecordsYet) }
            } else {
                items(items, key = { it.id }) { item ->
                    val readable = item.mediaKind == "YOUTUBE" || mediaManager.isManagedMediaFileReadable(item.optimizedUri ?: item.originalUri)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.mediaKind == "IMAGE" && readable) {
                                AsyncImage(
                                    model = item.optimizedUri ?: item.originalUri,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
                                )
                            } else if (!readable) {
                                ErrorBanner(mStrings.mediaMissingFile(item.originalUri))
                            }
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.testTag("media_item_title_${item.id}")
                            )
                            if (item.categoryOrAlbum.isNotBlank()) {
                                Surface(
                                    color = brand.royalGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.categoryOrAlbum,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                            .testTag("media_item_category_${item.id}")
                                    )
                                }
                            }
                            item.caption?.takeIf { it.isNotBlank() }?.let { itemCaption ->
                                Text(
                                    text = itemCaption,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("media_item_caption_${item.id}")
                                )
                            }
                            Text(
                                text = if (item.mediaKind == "IMAGE") {
                                    mStrings.mediaSizeStats(
                                        item.mimeType,
                                        item.widthPx,
                                        item.heightPx,
                                        item.fileSizeBytes / 1024,
                                        item.optimizedSizeBytes / 1024
                                    )
                                } else {
                                    mStrings.mediaYouTubePrefix(item.originalUri)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (item.mediaKind == "IMAGE" && canCreateHeroBanner) {
                                    OutlinedButton(
                                        onClick = { assignHeroAdjustTarget = item },
                                        enabled = readable,
                                        modifier = Modifier.testTag("media_assign_banner_btn_${item.id}")
                                    ) {
                                        Text(mStrings.mediaAssignBannerBtn)
                                    }
                                }
                                if (canCreateGallery) {
                                    OutlinedButton(
                                        onClick = { assignAlbumTarget = item },
                                        modifier = Modifier.testTag("media_assign_album_btn_${item.id}")
                                    ) {
                                        Text(mStrings.mediaAssignAlbumBtn)
                                    }
                                }
                                if (canEdit && item.mediaKind == "IMAGE") {
                                    OutlinedButton(
                                        onClick = {
                                            managementViewModel.clearFeedback()
                                            editMetadataTarget = item
                                        },
                                        modifier = Modifier.testTag("media_edit_metadata_btn_${item.id}")
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(mStrings.mediaEditMetadataBtn)
                                    }
                                    OutlinedButton(onClick = {
                                        replaceTargetId = item.id
                                        replacePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }) { Text(mStrings.mediaReplaceBtn) }
                                }
                                if (canDelete) {
                                    IconButton(onClick = {
                                        scope.launch {
                                            val warnings = managementViewModel.getMediaUsageWarnings(item)
                                            deleteCandidate = item to warnings
                                        }
                                    }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = mStrings.deleteBtn, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffManagementScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val accounts by managementViewModel.accountsWithPermissions.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val isMasterAdmin = managementViewModel.isMasterAdmin()
    val brand = LocalBrandPalette.current

    var editingTarget by remember { mutableStateOf<AccountWithPermissions?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<AccountWithPermissions?>(null) }

    deleteCandidate?.let { candidate ->
        val acc = candidate.account
        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                deleteCandidate = null
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.deleteStaffTitle)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vmError?.let {
                        ErrorBanner(it, dismissLabel = mStrings.dismissBtn) {
                            managementViewModel.clearFeedback()
                        }
                    }
                    Text(mStrings.deleteStaffMsg(acc.displayName, acc.username))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        managementViewModel.deleteStaffAccount(acc.id) {
                            deleteCandidate = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_staff_btn")
                ) { Text(mStrings.deleteBtn) }
            },
            dismissButton = {
                TextButton(onClick = {
                    managementViewModel.clearFeedback()
                    deleteCandidate = null
                }) { Text(mStrings.cancelBtn) }
            },
            modifier = Modifier.testTag("delete_staff_confirm_dialog")
        )
    }

    if (showCreateDialog || editingTarget != null) {
        val target = editingTarget
        val isCreate = target == null
        var username by remember(target, isCreate) { mutableStateOf(target?.account?.username ?: "") }
        var displayName by remember(target, isCreate) { mutableStateOf(target?.account?.displayName ?: "") }
        var selectedRole by remember(target, isCreate) { mutableStateOf(target?.account?.role ?: ManagementRole.STAFF) }
        var pin by remember(target, isCreate) { mutableStateOf("") }
        var showPin by remember(target, isCreate) { mutableStateOf(false) }
        var isActive by remember(target, isCreate) { mutableStateOf(target?.account?.isActive ?: true) }
        var localError by remember(target, isCreate) { mutableStateOf<String?>(null) }
        val permState = remember(target, isCreate) {
            mutableStateListOf<StaffPermissionEntity>().apply {
                ManagementSection.entries.forEach { sec ->
                    val existing = target?.permissions?.firstOrNull { it.sectionKey == sec.key }
                    add(
                        existing ?: StaffPermissionEntity(
                            accountId = target?.account?.id ?: 0L,
                            sectionKey = sec.key,
                            canView = selectedRole == ManagementRole.SUPER_OWNER,
                            canCreate = selectedRole == ManagementRole.SUPER_OWNER,
                            canEdit = selectedRole == ManagementRole.SUPER_OWNER,
                            canDelete = selectedRole == ManagementRole.SUPER_OWNER,
                            canPublish = selectedRole == ManagementRole.SUPER_OWNER
                        )
                    )
                }
            }
        }
        fun applyRoleDefaults(role: String) {
            val grantAll = role == ManagementRole.SUPER_OWNER
            for (i in permState.indices) {
                permState[i] = permState[i].copy(
                    canView = grantAll,
                    canCreate = grantAll,
                    canEdit = grantAll,
                    canDelete = grantAll,
                    canPublish = grantAll
                )
            }
        }
        val activeError = localError ?: vmError

        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                showCreateDialog = false
                editingTarget = null
            },
            title = {
                ApplySecureDialogWindow()
                Text(
                    if (isCreate) mStrings.staffCreateDialogTitle
                    else mStrings.staffEditDialogTitle(target?.account?.displayName.orEmpty())
                )
            },
            text = {
                Column(
                    modifier = Modifier.height(440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeError?.let {
                        ErrorBanner(it, dismissLabel = mStrings.dismissBtn) {
                            localError = null
                            managementViewModel.clearFeedback()
                        }
                    }

                    if (isCreate) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; localError = null },
                            label = { Text(mStrings.staffUsernameRequiredLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("staff_username_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (selectedRole != ManagementRole.STAFF) {
                                        selectedRole = ManagementRole.STAFF
                                        applyRoleDefaults(ManagementRole.STAFF)
                                    }
                                    localError = null
                                },
                                modifier = Modifier.testTag("staff_role_staff_btn")
                            ) {
                                Text(if (selectedRole == ManagementRole.STAFF) "✓ ${mStrings.staffRoleStaff}" else mStrings.staffRoleStaff)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (selectedRole != ManagementRole.SUPER_OWNER) {
                                        selectedRole = ManagementRole.SUPER_OWNER
                                        applyRoleDefaults(ManagementRole.SUPER_OWNER)
                                    }
                                    localError = null
                                },
                                modifier = Modifier.testTag("staff_role_super_owner_btn")
                            ) {
                                Text(if (selectedRole == ManagementRole.SUPER_OWNER) "✓ ${mStrings.staffRoleSuperOwner}" else mStrings.staffRoleSuperOwner)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(mStrings.staffAccountActiveLabel)
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it; localError = null },
                        label = { Text(mStrings.staffDisplayNameRequiredLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("staff_displayname_input")
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { c -> c.isDigit() }) { pin = it; localError = null } },
                        label = { Text(if (isCreate) mStrings.staffInitialPinLabel else mStrings.staffResetPinLabel) },
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("staff_pin_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            for (i in permState.indices) {
                                permState[i] = permState[i].copy(canView = true, canCreate = true, canEdit = true, canDelete = true, canPublish = true)
                            }
                        }) { Text(mStrings.staffGrantAllBtn) }
                        TextButton(onClick = {
                            for (i in permState.indices) {
                                permState[i] = permState[i].copy(canView = false, canCreate = false, canEdit = false, canDelete = false, canPublish = false)
                            }
                        }) { Text(mStrings.staffClearAllBtn) }
                    }

                    HorizontalDivider()
                    Text(mStrings.staffGranularPermsTitle, style = MaterialTheme.typography.titleSmall)
                    Text(mStrings.staffPermissionsLegend, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    permState.forEachIndexed { idx, perm ->
                        val sec = ManagementSection.fromKey(perm.sectionKey) ?: return@forEachIndexed
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(mStrings.sectionLabel(sec), style = MaterialTheme.typography.labelLarge)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                PermMiniCheck(mStrings.staffPermViewCode, perm.canView) { permState[idx] = perm.copy(canView = it) }
                                PermMiniCheck(mStrings.staffPermCreateCode, perm.canCreate) { permState[idx] = perm.copy(canCreate = it) }
                                PermMiniCheck(mStrings.staffPermEditCode, perm.canEdit) { permState[idx] = perm.copy(canEdit = it) }
                                PermMiniCheck(mStrings.staffPermDeleteCode, perm.canDelete) { permState[idx] = perm.copy(canDelete = it) }
                                PermMiniCheck(mStrings.staffPermPublishCode, perm.canPublish) { permState[idx] = perm.copy(canPublish = it) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isCreate) {
                            when {
                                username.isBlank() || displayName.isBlank() -> {
                                    localError = mStrings.staffUsernameDisplayRequiredError
                                }
                                !PinSecurityManager.isValidPinFormat(pin) -> {
                                    localError = mStrings.staffPinInvalidError
                                }
                                else -> {
                                    localError = null
                                    managementViewModel.createStaffAccount(username, displayName, selectedRole, pin, permState.toList()) {
                                        showCreateDialog = false
                                    }
                                }
                            }
                        } else {
                            when {
                                displayName.isBlank() -> {
                                    localError = mStrings.staffDisplayNameRequiredError
                                }
                                pin.isNotBlank() && !PinSecurityManager.isValidPinFormat(pin) -> {
                                    localError = mStrings.staffPinInvalidError
                                }
                                else -> {
                                    localError = null
                                    managementViewModel.updateStaffAccount(
                                        target!!.account.id, displayName, isActive, pin.takeIf { it.isNotBlank() }, permState.toList()
                                    ) {
                                        editingTarget = null
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("staff_dialog_submit_btn")
                ) { Text(if (isCreate) mStrings.createAccountBtn else mStrings.saveChangesBtn) }
            },
            dismissButton = {
                TextButton(onClick = {
                    managementViewModel.clearFeedback()
                    showCreateDialog = false
                    editingTarget = null
                }) { Text(mStrings.cancelBtn) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isMasterAdmin) {
                item {
                    Button(
                        onClick = {
                            managementViewModel.clearFeedback()
                            showCreateDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brand.burgundy, contentColor = Color.White),
                        modifier = Modifier.testTag("create_staff_account_btn")
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.staffCreateHeaderBtn)
                    }
                }
            } else {
                item { InfoBanner(mStrings.staffMasterOnlyNotice) }
            }

            items(accounts, key = { it.account.id }) { item ->
                val acc = item.account
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${acc.displayName} (@${acc.username})", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${mStrings.roleLabel(acc.role)} • ${if (acc.isActive) mStrings.staffActiveLabel else mStrings.staffDeactivatedLabel} • ${mStrings.staffSectionsCount(item.permissions.count { it.canView })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isMasterAdmin && acc.role != ManagementRole.MASTER_ADMIN) {
                            Row {
                                IconButton(
                                    onClick = {
                                        managementViewModel.clearFeedback()
                                        editingTarget = item
                                    },
                                    modifier = Modifier.testTag("edit_staff_btn_${acc.id}")
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = mStrings.staffEditAccountDesc)
                                }
                                IconButton(
                                    onClick = {
                                        managementViewModel.clearFeedback()
                                        deleteCandidate = item
                                    },
                                    modifier = Modifier.testTag("delete_staff_btn_${acc.id}")
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = mStrings.staffDeleteAccountDesc, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermMiniCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApprovalCenterScreen(managementViewModel: ManagementViewModel, mStrings: ManagementStrings) {
    val approvals by managementViewModel.allApprovals.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val brand = LocalBrandPalette.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var rejectingTarget by remember { mutableStateOf<ContentApprovalEntity?>(null) }

    rejectingTarget?.let { target ->
        var reason by remember(target) { mutableStateOf("") }
        var rejectError by remember(target) { mutableStateOf<String?>(null) }
        val activeError = rejectError ?: vmError
        AlertDialog(
            properties = SecureDialogProperties,
            onDismissRequest = {
                managementViewModel.clearFeedback()
                rejectingTarget = null
            },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.approvalRejectDialogTitle(target.recordTitle))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeError?.let { err ->
                        ErrorBanner(
                            message = err,
                            dismissLabel = mStrings.dismissBtn,
                            onDismiss = {
                                rejectError = null
                                managementViewModel.clearFeedback()
                            }
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = {
                            reason = it
                            rejectError = null
                        },
                        label = { Text(mStrings.rejectionReasonHint) },
                        isError = activeError != null,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("reject_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanReason = reason.trim()
                        if (cleanReason.isEmpty()) {
                            rejectError = mStrings.approvalReasonRequiredError
                        } else {
                            rejectError = null
                            managementViewModel.rejectSubmission(target.id, cleanReason) {
                                rejectingTarget = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reject_submit_btn")
                ) { Text(mStrings.rejectBtn) }
            },
            dismissButton = {
                TextButton(onClick = {
                    managementViewModel.clearFeedback()
                    rejectingTarget = null
                }) { Text(mStrings.cancelBtn) }
            }
        )
    }

    val filtered = if (selectedTab == 0) {
        approvals.filter { it.status == ContentWorkflowStatus.PENDING_APPROVAL }
    } else {
        approvals.filter { it.status != ContentWorkflowStatus.PENDING_APPROVAL }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(mStrings.approvalPendingTab(approvals.count { it.status == ContentWorkflowStatus.PENDING_APPROVAL })) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(mStrings.approvalReviewedTab(approvals.count { it.status != ContentWorkflowStatus.PENDING_APPROVAL })) }
                )
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (filtered.isEmpty()) {
                    item { InfoBanner(mStrings.noRecordsYet) }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        val sec = ManagementSection.fromKey(item.sectionKey)
                        val canPublish = sec != null && managementViewModel.hasPermission(sec, PermissionAction.PUBLISH)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "${sec?.let { mStrings.sectionLabel(it) } ?: item.sectionKey} • ${mStrings.workflowStatusLabel(item.status)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = brand.burgundy
                                    )
                                    Text(mStrings.approvalSubmittedBy(item.submittedByName), style = MaterialTheme.typography.labelSmall)
                                }
                                Text(item.recordTitle, style = MaterialTheme.typography.titleMedium)
                                Text(item.changeSummary, style = MaterialTheme.typography.bodySmall)
                                if (!item.previousSnapshotJson.isNullOrBlank()) {
                                    Text(mStrings.approvalPreviousPreservedNote, style = MaterialTheme.typography.labelSmall, color = brand.forestGreen)
                                }
                                item.rejectionReason?.let { r ->
                                    ErrorBanner(mStrings.approvalRejectionReason(r))
                                }
                                if (item.status == ContentWorkflowStatus.PENDING_APPROVAL && canPublish) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { managementViewModel.approveSubmission(item.id, publishImmediately = true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = brand.forestGreen, contentColor = Color.White),
                                            modifier = Modifier.testTag("approve_publish_btn_${item.id}")
                                        ) { Text(mStrings.approveAndPublishBtn) }
                                        OutlinedButton(
                                            onClick = { managementViewModel.approveSubmission(item.id, publishImmediately = false) }
                                        ) { Text(mStrings.approveOnlyBtn) }
                                        OutlinedButton(
                                            onClick = {
                                                managementViewModel.clearFeedback()
                                                rejectingTarget = item
                                            },
                                            modifier = Modifier.testTag("reject_btn_${item.id}")
                                        ) { Text(mStrings.rejectBtn) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
