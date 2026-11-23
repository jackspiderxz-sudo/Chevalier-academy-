package com.example.ui.screens.management

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.CropAspectRatioPreset
import com.example.data.media.YouTubeFetchedMetadata
import com.example.data.media.YouTubeMediaHelper
import com.example.data.media.YouTubeMetadataFetcher
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.PublishSaveMode
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.launch

internal data class SchoolPostEditorSession(
    val target: SchoolPostEntity,
    val liveBaseline: SchoolPostEntity?,
    val initialRevisionInfo: EditorRevisionInfo
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SchoolFeedPostComposerDialog(
    session: SchoolPostEditorSession,
    managementViewModel: ManagementViewModel,
    mStrings: ManagementStrings,
    mediaStudioItems: List<MediaStudioItemEntity>,
    canEdit: Boolean,
    canPublish: Boolean,
    isBusy: Boolean,
    vmError: String?,
    vmStatus: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val scope = rememberCoroutineScope()
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }

    val feedVisibilityState by managementViewModel.feedVisibility.collectAsStateWithLifecycle()
    val initialFeedVisibility = remember(session.target.remoteId, feedVisibilityState) {
        feedVisibilityState.visibilityFor(session.target.remoteId)
    }

    var baseline by remember(session) { mutableStateOf(session.target) }
    var revisionInfo by remember(session) { mutableStateOf(session.initialRevisionInfo) }
    var postType by remember(session) { mutableStateOf(session.target.postType) }
    var category by remember(session) { mutableStateOf(session.target.category.ifBlank { SchoolPostCategory.GENERAL }) }
    var title by remember(session) { mutableStateOf(session.target.title) }
    var description by remember(session) { mutableStateOf(session.target.description) }
    var youtubeUrl by remember(session) { mutableStateOf(session.target.youtubeUrl.orEmpty()) }
    var customThumbnailUri by remember(session) { mutableStateOf(session.target.customThumbnailUri) }
    var photoUris by remember(session) { mutableStateOf(session.target.photoUris()) }
    var selectedPhotoIndex by remember(session) { mutableIntStateOf(0) }
    var isFeatured by remember(session) { mutableStateOf(session.target.isFeatured) }
    var showOnHomeFeed by remember(session) { mutableStateOf(initialFeedVisibility.showOnHomeFeed) }
    var showOnAcademyFeed by remember(session) { mutableStateOf(initialFeedVisibility.showOnAcademyFeed) }
    var changeSummary by remember(session) { mutableStateOf("") }
    var validationError by remember(session) { mutableStateOf<String?>(null) }

    var isFetchingYt by remember { mutableStateOf(false) }
    var fetchedMetadata by remember(session) { mutableStateOf<YouTubeFetchedMetadata?>(null) }
    var metadataAttemptedForUrl by remember(session) { mutableStateOf<String?>(null) }
    var isImportingPhotos by remember { mutableStateOf(false) }
    var showStudioMultiPicker by remember { mutableStateOf(false) }

    val parsedYouTube = remember(youtubeUrl) { YouTubeMediaHelper.parse(youtubeUrl) }
    val isEditingExisting = baseline.id > 0L

    fun triggerYouTubeMetadataFetch(rawUrl: String) {
        val parsed = YouTubeMediaHelper.parse(rawUrl) ?: return
        scope.launch {
            isFetchingYt = true
            val meta = YouTubeMetadataFetcher.fetchMetadata(parsed.canonicalWatchUrl)
            isFetchingYt = false
            fetchedMetadata = meta
            metadataAttemptedForUrl = parsed.canonicalWatchUrl
            if (postType == SchoolPostType.YOUTUBE_DIRECT && meta != null) {
                title = meta.title.orEmpty()
                description = meta.description.orEmpty()
            }
        }
    }

    LaunchedEffect(parsedYouTube?.canonicalWatchUrl, postType) {
        val canonical = parsedYouTube?.canonicalWatchUrl
        if (canonical != null && postType == SchoolPostType.YOUTUBE_DIRECT && canonical != metadataAttemptedForUrl) {
            triggerYouTubeMetadataFetch(canonical)
        } else if (canonical == null) {
            fetchedMetadata = null
            metadataAttemptedForUrl = null
        }
    }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = SchoolPostEntity.MAX_PHOTO_COUNT)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = (SchoolPostEntity.MAX_PHOTO_COUNT - photoUris.size).coerceAtLeast(0)
            if (remaining <= 0) {
                validationError = mStrings.schoolFeed.maxPhotosReachedError(SchoolPostEntity.MAX_PHOTO_COUNT)
                return@rememberLauncherForActivityResult
            }
            val toImport = uris.take(remaining)
            val truncated = uris.size > remaining
            scope.launch {
                isImportingPhotos = true
                validationError = null
                val imported = mutableListOf<String>()
                var firstFailure: String? = null
                for (uri in toImport) {
                    mediaManager.importAndOptimizeImage(uri)
                        .onSuccess { imported += it.optimizedUri }
                        .onFailure { if (firstFailure == null) firstFailure = it.message }
                }
                isImportingPhotos = false
                if (imported.isNotEmpty()) {
                    val prevSize = photoUris.size
                    photoUris = (photoUris + imported).take(SchoolPostEntity.MAX_PHOTO_COUNT)
                    selectedPhotoIndex = prevSize.coerceAtMost(photoUris.lastIndex.coerceAtLeast(0))
                }
                if (truncated) {
                    validationError = mStrings.schoolFeed.maxPhotosTruncatedError(SchoolPostEntity.MAX_PHOTO_COUNT)
                } else if (firstFailure != null) {
                    validationError = firstFailure
                }
            }
        }
    }

    if (showStudioMultiPicker) {
        UniversalMediaStudioPickerDialog(
            mStrings = mStrings,
            mediaStudioItems = mediaStudioItems,
            onSelectUri = { picked ->
                showStudioMultiPicker = false
                if (photoUris.size >= SchoolPostEntity.MAX_PHOTO_COUNT) {
                    validationError = mStrings.schoolFeed.maxPhotosAllowedError(SchoolPostEntity.MAX_PHOTO_COUNT)
                } else {
                    photoUris = photoUris + picked
                    selectedPhotoIndex = photoUris.lastIndex
                    validationError = null
                }
            },
            onDismiss = { showStudioMultiPicker = false }
        )
    }

    val isDirty = remember(
        postType, category, title, description, youtubeUrl, customThumbnailUri, photoUris, isFeatured,
        showOnHomeFeed, showOnAcademyFeed, initialFeedVisibility, baseline
    ) {
        postType != baseline.postType ||
            category != baseline.category ||
            title.trim() != baseline.title.trim() ||
            description.trim() != baseline.description.trim() ||
            youtubeUrl.trim() != baseline.youtubeUrl.orEmpty().trim() ||
            customThumbnailUri != baseline.customThumbnailUri ||
            photoUris != baseline.photoUris() ||
            isFeatured != baseline.isFeatured ||
            showOnHomeFeed != initialFeedVisibility.showOnHomeFeed ||
            showOnAcademyFeed != initialFeedVisibility.showOnAcademyFeed
    }

    fun buildValidatedEntity(): Result<Pair<SchoolPostEntity, String>> = runCatching {
        require(showOnHomeFeed || showOnAcademyFeed) {
            "Select at least one feed (Home Page School Feed or Academy Social Feed)."
        }
        val cleanCategory = category.trim().ifEmpty { SchoolPostCategory.GENERAL }
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanYt = youtubeUrl.trim().takeIf { it.isNotEmpty() }

        when (postType) {
            SchoolPostType.YOUTUBE_DIRECT -> {
                val parsed = YouTubeMediaHelper.parse(cleanYt)
                    ?: throw IllegalArgumentException(mStrings.schoolFeed.invalidYouTubeUrlError)
                val effectiveTitle = fetchedMetadata?.takeIf { it.videoId == parsed.videoId }?.title
                    ?: cleanTitle
                val effectiveDesc = fetchedMetadata?.takeIf { it.videoId == parsed.videoId }?.description
                    ?: cleanDesc
                val recordLabel = effectiveTitle.ifBlank { mStrings.schoolFeed.youtubeVideoFallbackTitle(parsed.videoId) }
                SchoolPostEntity(
                    id = baseline.id,
                    remoteId = baseline.remoteId,
                    postType = SchoolPostType.YOUTUBE_DIRECT,
                    category = cleanCategory,
                    title = effectiveTitle,
                    description = effectiveDesc,
                    youtubeUrl = parsed.canonicalWatchUrl,
                    customThumbnailUri = null,
                    photoUrisJson = "[]",
                    status = baseline.status,
                    isFeatured = if (canPublish) isFeatured else baseline.isFeatured,
                    createdAtMillis = baseline.createdAtMillis,
                    publishedAtMillis = baseline.publishedAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                ) to recordLabel
            }

            SchoolPostType.ADMIN_CUSTOM_VIDEO -> {
                val parsed = YouTubeMediaHelper.parse(cleanYt)
                    ?: throw IllegalArgumentException(mStrings.schoolFeed.invalidYouTubeUrlError)
                require(cleanTitle.isNotEmpty()) { mStrings.schoolFeed.titleRequiredCustomVideoError }
                SchoolPostEntity(
                    id = baseline.id,
                    remoteId = baseline.remoteId,
                    postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
                    category = cleanCategory,
                    title = cleanTitle,
                    description = cleanDesc,
                    youtubeUrl = parsed.canonicalWatchUrl,
                    customThumbnailUri = customThumbnailUri,
                    photoUrisJson = "[]",
                    status = baseline.status,
                    isFeatured = if (canPublish) isFeatured else baseline.isFeatured,
                    createdAtMillis = baseline.createdAtMillis,
                    publishedAtMillis = baseline.publishedAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                ) to cleanTitle
            }

            SchoolPostType.PHOTO -> {
                require(cleanTitle.isNotEmpty()) { mStrings.schoolFeed.titleRequiredPhotoPostError }
                require(photoUris.isNotEmpty()) { mStrings.schoolFeed.atLeastOnePhotoRequiredError }
                require(photoUris.size <= SchoolPostEntity.MAX_PHOTO_COUNT) {
                    mStrings.schoolFeed.maxPhotosAllowedError(SchoolPostEntity.MAX_PHOTO_COUNT)
                }
                SchoolPostEntity(
                    id = baseline.id,
                    remoteId = baseline.remoteId,
                    postType = SchoolPostType.PHOTO,
                    category = cleanCategory,
                    title = cleanTitle,
                    description = cleanDesc,
                    youtubeUrl = null,
                    customThumbnailUri = null,
                    photoUrisJson = SchoolPostEntity.encodePhotoUris(photoUris),
                    status = baseline.status,
                    isFeatured = if (canPublish) isFeatured else baseline.isFeatured,
                    createdAtMillis = baseline.createdAtMillis,
                    publishedAtMillis = baseline.publishedAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                ) to cleanTitle
            }

            else -> throw IllegalArgumentException(mStrings.schoolFeed.unsupportedPostTypeError)
        }
    }

    UniversalContentEditorScaffold(
        mStrings = mStrings,
        title = if (isEditingExisting) mStrings.schoolFeed.editPostTitle else mStrings.schoolFeed.newPostTitle,
        subtitle = postTypeDisplayName(postType, mStrings),
        canEdit = canEdit,
        canPublish = canPublish,
        isDirty = isDirty,
        isBusy = isBusy || isImportingPhotos,
        isFullScreenDialog = true,
        revisionInfo = revisionInfo,
        errorMessage = validationError ?: vmError,
        statusMessage = vmStatus,
        onClearError = {
            validationError = null
            managementViewModel.clearFeedback()
        },
        onClearStatus = { managementViewModel.clearFeedback() },
        onResetToLive = if (session.liveBaseline != null) {
            {
                val live = session.liveBaseline
                managementViewModel.resetRevisionToLive(
                    section = ManagementSection.SCHOOL_POSTS,
                    recordId = live.id,
                    recordTitle = live.title.ifBlank { mStrings.schoolFeed.postFallbackTitle(live.id) }
                ) {
                    baseline = live
                    postType = live.postType
                    category = live.category
                    title = live.title
                    description = live.description
                    youtubeUrl = live.youtubeUrl.orEmpty()
                    customThumbnailUri = live.customThumbnailUri
                    photoUris = live.photoUris()
                    selectedPhotoIndex = 0
                    isFeatured = live.isFeatured
                    revisionInfo = EditorRevisionInfo(hasLiveVersion = true)
                    validationError = null
                }
            }
        } else null,
        onSaveAction = { saveMode: PublishSaveMode ->
            buildValidatedEntity()
                .onSuccess { (entity, label) ->
                    validationError = null
                    managementViewModel.saveManagedRecord(
                        section = ManagementSection.SCHOOL_POSTS,
                        recordId = entity.id,
                        recordTitle = label,
                        proposedJson = ContentSnapshotSerializer.schoolPostToJson(entity),
                        saveMode = saveMode,
                        changeSummary = changeSummary.trim(),
                        onSuccess = {
                            managementViewModel.updatePostFeedVisibility(
                                postRemoteId = entity.remoteId,
                                showOnHomeFeed = showOnHomeFeed,
                                showOnAcademyFeed = showOnAcademyFeed,
                                onSaved = onDismiss
                            )
                        }
                    )
                }
                .onFailure { err ->
                    validationError = err.message ?: mStrings.schoolFeed.checkRequiredFieldsError
                }
        },
        onClose = onDismiss,
        dialogTestTag = "school_post_composer_dialog",
        previewContent = {
            SchoolPostPreviewCard(
                mStrings = mStrings,
                postType = postType,
                category = category,
                title = title,
                description = description,
                youtubeUrl = youtubeUrl,
                customThumbnailUri = customThumbnailUri,
                photoUris = photoUris,
                isFeatured = isFeatured,
                fetchedMetadata = fetchedMetadata
            )
        }
    ) {
        // 1. Post Type Selector
        Text(
            text = mStrings.schoolFeed.postTypeSectionLabel,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val typeOptions = listOf(
                Triple(SchoolPostType.YOUTUBE_DIRECT, mStrings.schoolFeed.postTypeYouTubeDirect, Icons.Outlined.PlayCircleOutline),
                Triple(SchoolPostType.ADMIN_CUSTOM_VIDEO, mStrings.schoolFeed.postTypeCustomVideo, Icons.Outlined.VideoLibrary),
                Triple(SchoolPostType.PHOTO, mStrings.schoolFeed.postTypePhoto, Icons.Outlined.Collections)
            )
            typeOptions.forEach { (typeKey, label, icon) ->
                FilterChip(
                    selected = postType == typeKey,
                    onClick = {
                        if (!isEditingExisting && canEdit) {
                            postType = typeKey
                            validationError = null
                            if (typeKey == SchoolPostType.YOUTUBE_DIRECT && parsedYouTube != null) {
                                triggerYouTubeMetadataFetch(parsedYouTube.canonicalWatchUrl)
                            }
                        }
                    },
                    enabled = canEdit && (!isEditingExisting || postType == typeKey),
                    label = { Text(label) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = brand.burgundy.copy(alpha = 0.14f),
                        selectedLabelColor = brand.burgundy,
                        selectedLeadingIconColor = brand.burgundy
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("post_type_chip_${typeKey.lowercase()}")
                )
            }
        }

        // 2. Category Selector
        Text(
            text = mStrings.schoolFeed.categorySectionLabel,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SchoolPostCategory.defaults.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { if (canEdit) category = cat },
                    enabled = canEdit,
                    label = { Text(mStrings.schoolFeed.categoryLabel(cat)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = brand.forestGreenContainer,
                        selectedLabelColor = brand.onForestGreenContainer
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("post_category_chip_${cat.lowercase().replace(" ", "_")}")
                )
            }
        }

        // 3. Type-specific Composer Fields
        when (postType) {
            SchoolPostType.YOUTUBE_DIRECT -> {
                InfoBanner(mStrings.schoolFeed.youtubeDirectInfoBanner)
                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = {
                        youtubeUrl = it
                        validationError = null
                    },
                    label = { Text(mStrings.schoolFeed.youtubeVideoUrlLabel) },
                    placeholder = { Text(mStrings.schoolFeed.youtubeVideoUrlPlaceholder) },
                    singleLine = true,
                    enabled = canEdit,
                    isError = youtubeUrl.isNotBlank() && parsedYouTube == null,
                    supportingText = {
                        Text(
                            if (youtubeUrl.isNotBlank() && parsedYouTube == null) {
                                mStrings.schoolFeed.youtubeUrlInvalidHint
                            } else {
                                mStrings.schoolFeed.youtubeUrlSupportedHint
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_composer_youtube_url_input")
                )

                if (parsedYouTube != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mStrings.schoolFeed.youtubePreviewTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedButton(
                            onClick = { triggerYouTubeMetadataFetch(parsedYouTube.canonicalWatchUrl) },
                            enabled = !isFetchingYt,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("post_composer_fetch_yt_btn")
                        ) {
                            if (isFetchingYt) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFetchingYt) mStrings.schoolFeed.fetchingLabel else mStrings.schoolFeed.refreshInfoBtn)
                        }
                    }

                    YouTubeDirectMetadataPreviewCard(
                        mStrings = mStrings,
                        videoId = parsedYouTube.videoId,
                        thumbnailUrl = fetchedMetadata?.thumbnailUrl ?: parsedYouTube.thumbnailUrl,
                        title = title,
                        description = description,
                        authorName = fetchedMetadata?.authorName,
                        metadataUnavailable = !isFetchingYt && metadataAttemptedForUrl != null && fetchedMetadata?.metadataAvailable == false
                    )
                }
            }

            SchoolPostType.ADMIN_CUSTOM_VIDEO -> {
                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = {
                        youtubeUrl = it
                        validationError = null
                    },
                    label = { Text(mStrings.schoolFeed.youtubeVideoUrlLabel) },
                    placeholder = { Text(mStrings.schoolFeed.youtubeVideoUrlPlaceholder) },
                    singleLine = true,
                    enabled = canEdit,
                    isError = youtubeUrl.isNotBlank() && parsedYouTube == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_composer_youtube_url_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text(mStrings.schoolFeed.postTitleLabel) },
                    singleLine = true,
                    enabled = canEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_composer_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(mStrings.schoolFeed.descriptionLabel) },
                    minLines = 3,
                    maxLines = 6,
                    enabled = canEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_composer_desc_input")
                )
                UniversalMediaField(
                    mStrings = mStrings,
                    label = mStrings.schoolFeed.optionalCustomThumbnailLabel,
                    selectedUri = customThumbnailUri,
                    onUriChanged = { customThumbnailUri = it },
                    mediaStudioItems = mediaStudioItems,
                    defaultAspectPreset = CropAspectRatioPreset.RATIO_16_9,
                    outputTag = "post_custom_thumb",
                    enabled = canEdit,
                    testTagPrefix = "post_custom_thumb"
                )
            }

            SchoolPostType.PHOTO -> {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text(mStrings.schoolFeed.postTitleLabel) },
                    singleLine = true,
                    enabled = canEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_composer_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(mStrings.schoolFeed.descriptionLabel) },
                    minLines = 3,
                    maxLines = 6,
                    enabled = canEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_composer_desc_input")
                )

                // Photo Gallery Manager (1..10 photos) reusing UniversalMediaField + Photo Picker + Crop + Optimization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mStrings.schoolFeed.photosMaxHeader(SchoolPostEntity.MAX_PHOTO_COUNT),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Surface(
                        color = if (photoUris.size >= SchoolPostEntity.MAX_PHOTO_COUNT) {
                            brand.royalGold.copy(alpha = 0.28f)
                        } else {
                            brand.forestGreenContainer
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("post_composer_photo_count")
                    ) {
                        Text(
                            text = "${photoUris.size} / ${SchoolPostEntity.MAX_PHOTO_COUNT}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = canEdit && !isImportingPhotos && photoUris.size < SchoolPostEntity.MAX_PHOTO_COUNT,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("post_composer_add_photos_btn")
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(mStrings.schoolFeed.addPhotosBtn)
                    }
                    OutlinedButton(
                        onClick = { showStudioMultiPicker = true },
                        enabled = canEdit && !isImportingPhotos && photoUris.size < SchoolPostEntity.MAX_PHOTO_COUNT,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("post_composer_add_studio_photo_btn")
                    ) {
                        Icon(Icons.Outlined.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(mStrings.schoolFeed.addFromMediaStudioBtn)
                    }
                }

                if (photoUris.isEmpty()) {
                    // Reuse UniversalMediaField directly for the first photo (Photo Picker + Studio + Crop + Optimization)
                    UniversalMediaField(
                        mStrings = mStrings,
                        label = mStrings.schoolFeed.photoOneOfMaxLabel(SchoolPostEntity.MAX_PHOTO_COUNT),
                        selectedUri = null,
                        onUriChanged = { uri ->
                            if (!uri.isNullOrBlank()) {
                                photoUris = listOf(uri)
                                selectedPhotoIndex = 0
                                validationError = null
                            }
                        },
                        mediaStudioItems = mediaStudioItems,
                        defaultAspectPreset = CropAspectRatioPreset.RATIO_4_3,
                        outputTag = "school_post_photo",
                        enabled = canEdit,
                        testTagPrefix = "post_photo_media"
                    )
                } else {
                    val safeIndex = selectedPhotoIndex.coerceIn(0, photoUris.lastIndex)
                    if (safeIndex != selectedPhotoIndex) selectedPhotoIndex = safeIndex

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_composer_photo_strip")
                    ) {
                        itemsIndexed(photoUris) { idx, uri ->
                            val isSelected = idx == safeIndex
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) brand.burgundy else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .size(74.dp)
                                    .clickable { selectedPhotoIndex = idx }
                                    .testTag("post_photo_thumb_$idx")
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(uri).size(240).build(),
                                        contentDescription = mStrings.schoolFeed.photoIndexDesc(idx + 1),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    if (canEdit) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(bottomStart = 6.dp),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .clickable {
                                                    val updated = photoUris.toMutableList().apply { removeAt(idx) }
                                                    photoUris = updated
                                                    selectedPhotoIndex = safeIndex.coerceAtMost(updated.lastIndex.coerceAtLeast(0))
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = mStrings.schoolFeed.removePhotoIndexDesc(idx + 1),
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (photoUris.size > 1 && canEdit) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (safeIndex > 0) {
                                        val mutable = photoUris.toMutableList()
                                        val moved = mutable.removeAt(safeIndex)
                                        mutable.add(safeIndex - 1, moved)
                                        photoUris = mutable
                                        selectedPhotoIndex = safeIndex - 1
                                    }
                                },
                                enabled = safeIndex > 0,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = mStrings.schoolFeed.movePhotoLeftDesc, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mStrings.schoolFeed.moveEarlierBtn)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (safeIndex < photoUris.lastIndex) {
                                        val mutable = photoUris.toMutableList()
                                        val moved = mutable.removeAt(safeIndex)
                                        mutable.add(safeIndex + 1, moved)
                                        photoUris = mutable
                                        selectedPhotoIndex = safeIndex + 1
                                    }
                                },
                                enabled = safeIndex < photoUris.lastIndex,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(mStrings.schoolFeed.moveLaterBtn)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = mStrings.schoolFeed.movePhotoRightDesc, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Active photo editor reusing UniversalMediaField (crop/adjust, replace via Photo Picker/Studio, remove)
                    UniversalMediaField(
                        mStrings = mStrings,
                        label = mStrings.schoolFeed.selectedPhotoOfTotalLabel(safeIndex + 1, photoUris.size),
                        selectedUri = photoUris[safeIndex],
                        onUriChanged = { newUri ->
                            val mutable = photoUris.toMutableList()
                            if (newUri.isNullOrBlank()) {
                                mutable.removeAt(safeIndex)
                                photoUris = mutable
                                selectedPhotoIndex = safeIndex.coerceAtMost(mutable.lastIndex.coerceAtLeast(0))
                            } else {
                                mutable[safeIndex] = newUri
                                photoUris = mutable
                            }
                        },
                        mediaStudioItems = mediaStudioItems,
                        defaultAspectPreset = CropAspectRatioPreset.RATIO_4_3,
                        outputTag = "school_post_photo",
                        enabled = canEdit,
                        testTagPrefix = "post_photo_media"
                    )
                }
            }
        }

        // 4. Pin / Feature Toggle (governed by PUBLISH permission)
        if (canPublish) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = mStrings.schoolFeed.pinPostDesc,
                            tint = brand.burgundy,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = mStrings.schoolFeed.pinFeaturePostTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = mStrings.schoolFeed.pinFeaturePostSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isFeatured,
                        onCheckedChange = { isFeatured = it },
                        enabled = canEdit && canPublish,
                        modifier = Modifier.testTag("post_composer_pin_switch")
                    )
                }
            }
        }

        // 4b. Feed Visibility Settings (single record can appear in Home Feed, Academy Feed, or both)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_composer_feed_visibility_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Feed Visibility",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Choose where this post is shown. Enabling both displays this single record in both feeds without duplicating it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Home Page School Feed",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Display on the public Home Page social feed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showOnHomeFeed,
                        onCheckedChange = {
                            showOnHomeFeed = it
                            validationError = null
                        },
                        enabled = canEdit,
                        modifier = Modifier.testTag("post_composer_home_feed_switch")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Academy Social Feed",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Display on the Academy Page academic feed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showOnAcademyFeed,
                        onCheckedChange = {
                            showOnAcademyFeed = it
                            validationError = null
                        },
                        enabled = canEdit,
                        modifier = Modifier.testTag("post_composer_academy_feed_switch")
                    )
                }
            }
        }

        // 5. Optional Change Summary
        OutlinedTextField(
            value = changeSummary,
            onValueChange = { changeSummary = it },
            label = { Text(mStrings.schoolFeed.changeSummaryOptionalLabel) },
            singleLine = true,
            enabled = canEdit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_composer_summary_input")
        )
    }
}

@Composable
private fun YouTubeDirectMetadataPreviewCard(
    mStrings: ManagementStrings,
    videoId: String,
    thumbnailUrl: String,
    title: String,
    description: String,
    authorName: String?,
    metadataUnavailable: Boolean
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_composer_yt_preview_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(thumbnailUrl).crossfade(false).build(),
                    contentDescription = title.ifBlank { mStrings.schoolFeed.youtubeVideoThumbnailDesc },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = mStrings.schoolFeed.youtubeVideoLabel,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(34.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = mStrings.schoolFeed.youtubeVideoFallbackTitle(videoId),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = brand.burgundy
                    )
                }
                if (!authorName.isNullOrBlank()) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.labelMedium,
                        color = brand.forestGreen
                    )
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (metadataUnavailable) {
                    Text(
                        text = mStrings.schoolFeed.youtubeMetadataUnavailableNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("post_composer_yt_fallback_notice")
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolPostPreviewCard(
    mStrings: ManagementStrings,
    postType: String,
    category: String,
    title: String,
    description: String,
    youtubeUrl: String,
    customThumbnailUri: String?,
    photoUris: List<String>,
    isFeatured: Boolean,
    fetchedMetadata: YouTubeFetchedMetadata?
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val parsedYt = YouTubeMediaHelper.parse(youtubeUrl)
    val previewImage = when (postType) {
        SchoolPostType.YOUTUBE_DIRECT -> fetchedMetadata?.thumbnailUrl ?: parsedYt?.thumbnailUrl
        SchoolPostType.ADMIN_CUSTOM_VIDEO -> customThumbnailUri ?: parsedYt?.thumbnailUrl
        SchoolPostType.PHOTO -> photoUris.firstOrNull()
        else -> null
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = brand.forestGreenContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = mStrings.schoolFeed.categoryLabel(category),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = brand.onForestGreenContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = postTypeDisplayName(postType, mStrings),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (isFeatured) {
                    Surface(color = brand.royalGold.copy(alpha = 0.28f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = mStrings.schoolFeed.pinnedBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            if (!previewImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(previewImage).crossfade(false).build(),
                    contentDescription = title.ifBlank { mStrings.schoolFeed.postMediaPreviewDesc },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            val displayTitle = title.ifBlank {
                parsedYt?.let { mStrings.schoolFeed.youtubeVideoFallbackTitle(it.videoId) }
                    ?: mStrings.schoolFeed.untitledPostTitle
            }
            Text(displayTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (postType == SchoolPostType.PHOTO && photoUris.size > 1) {
                Text(
                    text = mStrings.schoolFeed.photosAttachedLabel(photoUris.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = brand.burgundy
                )
            }
        }
    }
}

internal fun postTypeDisplayName(postType: String, mStrings: ManagementStrings): String =
    mStrings.schoolFeed.postTypeLabel(postType)
