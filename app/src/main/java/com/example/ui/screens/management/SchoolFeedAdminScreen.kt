package com.example.ui.screens.management

import androidx.compose.foundation.BorderStroke
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureDialogProperties
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.YouTubeFetchedMetadata
import com.example.data.media.YouTubeMediaHelper
import com.example.data.media.YouTubeMetadataFetcher
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.PublishSaveMode
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.launch

private enum class PostStatusFilter {
    ALL,
    PUBLISHED,
    UNPUBLISHED,
    ARCHIVED;

    fun label(mStrings: ManagementStrings): String = when (this) {
        ALL -> mStrings.schoolFeed.filterAll
        PUBLISHED -> mStrings.statusPublished
        UNPUBLISHED -> mStrings.schoolFeed.filterDraftPending
        ARCHIVED -> mStrings.schoolFeed.filterArchived
    }
}

private enum class HighlightFilter {
    ALL,
    ACTIVE,
    EXPIRED,
    UNPUBLISHED;

    fun label(mStrings: ManagementStrings): String = when (this) {
        ALL -> mStrings.schoolFeed.filterAll
        ACTIVE -> mStrings.schoolFeed.filterActive24h
        EXPIRED -> mStrings.schoolFeed.filterExpired
        UNPUBLISHED -> mStrings.schoolFeed.filterDraftPending
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchoolFeedAdminScreen(
    managementViewModel: ManagementViewModel,
    mStrings: ManagementStrings,
    initialTab: Int = 0
) {
    val authUiState by managementViewModel.authUiState.collectAsStateWithLifecycle()
    val posts by managementViewModel.allSchoolPosts.collectAsStateWithLifecycle()
    val highlights by managementViewModel.allSchoolHighlights.collectAsStateWithLifecycle()
    val approvals by managementViewModel.allApprovals.collectAsStateWithLifecycle()
    val mediaStudioItems by managementViewModel.allMediaStudioItems.collectAsStateWithLifecycle()
    val isBusy by managementViewModel.isBusy.collectAsStateWithLifecycle()
    val vmError by managementViewModel.errorMessage.collectAsStateWithLifecycle()
    val vmStatus by managementViewModel.statusMessage.collectAsStateWithLifecycle()

    val brand = LocalBrandPalette.current
    val scope = rememberCoroutineScope()

    val canViewPosts = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_POSTS, PermissionAction.VIEW)
    }
    val canCreatePosts = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_POSTS, PermissionAction.CREATE)
    }
    val canEditPosts = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_POSTS, PermissionAction.EDIT)
    }
    val canDeletePosts = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_POSTS, PermissionAction.DELETE)
    }
    val canPublishPosts = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_POSTS, PermissionAction.PUBLISH)
    }

    val canViewHighlights = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.VIEW)
    }
    val canCreateHighlights = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.CREATE)
    }
    val canEditHighlights = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.EDIT)
    }
    val canDeleteHighlights = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.DELETE)
    }
    val canPublishHighlights = remember(authUiState) {
        managementViewModel.hasPermission(ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.PUBLISH)
    }

    var activeTab by remember(canViewPosts, canViewHighlights, initialTab) {
        mutableIntStateOf(
            when {
                initialTab == 1 && canViewHighlights -> 1
                canViewPosts -> 0
                canViewHighlights -> 1
                else -> 0
            }
        )
    }
    var postStatusFilter by remember { mutableStateOf(PostStatusFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var highlightFilter by remember { mutableStateOf(HighlightFilter.ALL) }

    var postEditorSession by remember { mutableStateOf<SchoolPostEditorSession?>(null) }
    var highlightEditorTarget by remember { mutableStateOf<SchoolHighlightEntity?>(null) }
    var highlightRevisionInfo by remember { mutableStateOf(EditorRevisionInfo()) }
    var pendingDeletePost by remember { mutableStateOf<SchoolPostEntity?>(null) }
    var pendingDeleteHighlight by remember { mutableStateOf<SchoolHighlightEntity?>(null) }

    val pendingPostIds = remember(approvals) {
        approvals
            .filter { it.sectionKey == ManagementSection.SCHOOL_POSTS.key && it.status == ContentWorkflowStatus.PENDING_APPROVAL }
            .map { it.targetRecordId }
            .toSet()
    }
    val pendingHighlightIds = remember(approvals) {
        approvals
            .filter { it.sectionKey == ManagementSection.SCHOOL_HIGHLIGHTS.key && it.status == ContentWorkflowStatus.PENDING_APPROVAL }
            .map { it.targetRecordId }
            .toSet()
    }

    // Delete Post Confirmation Dialog
    pendingDeletePost?.let { post ->
        val label = post.title.ifBlank { mStrings.schoolFeed.postFallbackTitle(post.id) }
        AlertDialog(
            onDismissRequest = { pendingDeletePost = null },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.deleteRecordTitle)
            },
            text = { Text(mStrings.deleteRecordMsg(label)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeletePost = null
                        managementViewModel.deleteRecord(ManagementSection.SCHOOL_POSTS, post.id, label)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_post_btn")
                ) { Text(mStrings.deleteBtn) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePost = null }) { Text(mStrings.cancelBtn) }
            },
            modifier = Modifier.testTag("delete_post_confirm_dialog"),
            properties = SecureDialogProperties
        )
    }

    // Delete Highlight Confirmation Dialog
    pendingDeleteHighlight?.let { hl ->
        val label = hl.title.ifBlank { mStrings.schoolFeed.highlightFallbackTitle(hl.id) }
        AlertDialog(
            onDismissRequest = { pendingDeleteHighlight = null },
            title = {
                ApplySecureDialogWindow()
                Text(mStrings.deleteRecordTitle)
            },
            text = { Text(mStrings.schoolFeed.deleteHighlightMsg(label)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteHighlight = null
                        managementViewModel.deleteRecord(ManagementSection.SCHOOL_HIGHLIGHTS, hl.id, label)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_highlight_btn")
                ) { Text(mStrings.deleteBtn) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteHighlight = null }) { Text(mStrings.cancelBtn) }
            },
            modifier = Modifier.testTag("delete_highlight_confirm_dialog"),
            properties = SecureDialogProperties
        )
    }

    // Post Composer Full-Screen Dialog
    postEditorSession?.let { session ->
        SchoolFeedPostComposerDialog(
            session = session,
            managementViewModel = managementViewModel,
            mStrings = mStrings,
            mediaStudioItems = mediaStudioItems,
            canEdit = if (session.target.id == 0L) canCreatePosts else canEditPosts,
            canPublish = canPublishPosts,
            isBusy = isBusy,
            vmError = vmError,
            vmStatus = vmStatus,
            onDismiss = { postEditorSession = null }
        )
    }

    // Highlight Composer Full-Screen Dialog
    highlightEditorTarget?.let { target ->
        SchoolHighlightComposerDialog(
            target = target,
            revisionInfo = highlightRevisionInfo,
            managementViewModel = managementViewModel,
            mStrings = mStrings,
            canEdit = if (target.id == 0L) canCreateHighlights else canEditHighlights,
            canPublish = canPublishHighlights,
            isBusy = isBusy,
            vmError = vmError,
            vmStatus = vmStatus,
            onDismiss = { highlightEditorTarget = null }
        )
    }

    val nowMillis = System.currentTimeMillis()
    val activeHighlightCount = highlights.count { it.isActiveAt(nowMillis) }

    Scaffold(
        floatingActionButton = {
            if (activeTab == 0 && canCreatePosts) {
                FloatingActionButton(
                    onClick = {
                        managementViewModel.onUserInteraction()
                        managementViewModel.clearFeedback()
                        postEditorSession = SchoolPostEditorSession(
                            target = SchoolPostEntity(
                                postType = SchoolPostType.PHOTO,
                                category = SchoolPostCategory.GENERAL,
                                title = "",
                                status = SchoolPostStatus.UNPUBLISHED
                            ),
                            liveBaseline = null,
                            initialRevisionInfo = EditorRevisionInfo()
                        )
                    },
                    containerColor = brand.burgundy,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("mgmt_fab_add_school_posts")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = mStrings.schoolFeed.createSchoolPostDesc)
                }
            } else if (activeTab == 1 && canCreateHighlights) {
                FloatingActionButton(
                    onClick = {
                        managementViewModel.onUserInteraction()
                        managementViewModel.clearFeedback()
                        highlightRevisionInfo = EditorRevisionInfo()
                        highlightEditorTarget = SchoolHighlightEntity(
                            title = "",
                            youtubeShortsUrl = "",
                            status = SchoolPostStatus.UNPUBLISHED
                        )
                    },
                    containerColor = brand.forestGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("mgmt_fab_add_school_highlights")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = mStrings.schoolFeed.add24hHighlightDesc)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("school_feed_admin_screen")
        ) {
            if (canViewPosts && canViewHighlights) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = brand.burgundy
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text(mStrings.schoolFeed.postsTabLabel(posts.size)) },
                        modifier = Modifier.testTag("school_feed_tab_posts")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text(mStrings.schoolFeed.highlightsTabLabel(activeHighlightCount)) },
                        modifier = Modifier.testTag("school_feed_tab_highlights")
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (activeTab == 0 && canViewPosts) {
                    val filteredPosts = remember(posts, postStatusFilter, categoryFilter) {
                        posts.filter { post ->
                            val matchesStatus = when (postStatusFilter) {
                                PostStatusFilter.ALL -> true
                                PostStatusFilter.PUBLISHED -> post.status == SchoolPostStatus.PUBLISHED
                                PostStatusFilter.UNPUBLISHED -> post.status == SchoolPostStatus.UNPUBLISHED
                                PostStatusFilter.ARCHIVED -> post.status == SchoolPostStatus.ARCHIVED
                            }
                            val matchesCat = categoryFilter == null || post.category == categoryFilter
                            matchesStatus && matchesCat
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxSize()
                            .testTag("school_posts_admin_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "post_filters") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PostStatusFilter.entries.forEach { filter ->
                                        FilterChip(
                                            selected = postStatusFilter == filter,
                                            onClick = { postStatusFilter = filter },
                                            label = { Text(filter.label(mStrings)) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = brand.burgundy.copy(alpha = 0.14f),
                                                selectedLabelColor = brand.burgundy
                                            ),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("post_filter_${filter.name.lowercase()}")
                                        )
                                    }
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = categoryFilter == null,
                                        onClick = { categoryFilter = null },
                                        label = { Text(mStrings.schoolFeed.allCategoriesLabel) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    )
                                    SchoolPostCategory.defaults.forEach { cat ->
                                        FilterChip(
                                            selected = categoryFilter == cat,
                                            onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                                            label = { Text(mStrings.schoolFeed.categoryLabel(cat)) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredPosts.isEmpty()) {
                            item(key = "empty_posts") {
                                EmptyFeedCard(
                                    message = if (posts.isEmpty()) {
                                        mStrings.schoolFeed.emptyPostsMessage
                                    } else {
                                        mStrings.schoolFeed.emptyFilteredPostsMessage
                                    },
                                    actionLabel = if (posts.isEmpty() && canCreatePosts) mStrings.schoolFeed.createFirstPostBtn else null,
                                    onAction = {
                                        postEditorSession = SchoolPostEditorSession(
                                            target = SchoolPostEntity(
                                                postType = SchoolPostType.PHOTO,
                                                category = SchoolPostCategory.GENERAL,
                                                title = "",
                                                status = SchoolPostStatus.UNPUBLISHED
                                            ),
                                            liveBaseline = null,
                                            initialRevisionInfo = EditorRevisionInfo()
                                        )
                                    }
                                )
                            }
                        } else {
                            items(filteredPosts, key = { it.id }) { post ->
                                SchoolPostAdminCard(
                                    post = post,
                                    mStrings = mStrings,
                                    hasPendingApproval = post.id in pendingPostIds,
                                    canEdit = canEditPosts,
                                    canDelete = canDeletePosts,
                                    canPublish = canPublishPosts,
                                    onEdit = {
                                        scope.launch {
                                            val resolved = managementViewModel.resolveEditableRevision(
                                                section = ManagementSection.SCHOOL_POSTS,
                                                recordId = post.id
                                            )
                                            val resumed = resolved.editableSnapshotJson?.let { json ->
                                                runCatching {
                                                    ContentSnapshotSerializer.schoolPostFromJson(
                                                        jsonStr = json,
                                                        publishNow = post.status == SchoolPostStatus.PUBLISHED,
                                                        forcedStatus = post.status
                                                    ).copy(id = post.id)
                                                }.getOrNull()
                                            } ?: post
                                            postEditorSession = SchoolPostEditorSession(
                                                target = resumed,
                                                liveBaseline = if (resolved.hasLivePublishedVersion) post else null,
                                                initialRevisionInfo = EditorRevisionInfo.fromResolution(resolved)
                                            )
                                        }
                                    },
                                    onTogglePublish = {
                                        managementViewModel.togglePublishState(
                                            section = ManagementSection.SCHOOL_POSTS,
                                            recordId = post.id,
                                            currentlyPublished = post.status == SchoolPostStatus.PUBLISHED
                                        )
                                    },
                                    onTogglePin = {
                                        managementViewModel.setSchoolPostFeatured(
                                            postId = post.id,
                                            isFeatured = !post.isFeatured,
                                            postTitle = post.title.ifBlank { mStrings.schoolFeed.postFallbackTitle(post.id) }
                                        )
                                    },
                                    onArchive = {
                                        managementViewModel.archiveSchoolPost(
                                            postId = post.id,
                                            postTitle = post.title.ifBlank { mStrings.schoolFeed.postFallbackTitle(post.id) }
                                        )
                                    },
                                    onRestore = {
                                        managementViewModel.restoreArchivedSchoolPost(
                                            postId = post.id,
                                            postTitle = post.title.ifBlank { mStrings.schoolFeed.postFallbackTitle(post.id) }
                                        )
                                    },
                                    onRequestDelete = { pendingDeletePost = post }
                                )
                            }
                        }

                        item(key = "posts_bottom_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                } else if (canViewHighlights) {
                    val filteredHighlights = remember(highlights, highlightFilter, nowMillis) {
                        highlights.filter { hl ->
                            when (highlightFilter) {
                                HighlightFilter.ALL -> true
                                HighlightFilter.ACTIVE -> hl.isActiveAt(nowMillis)
                                HighlightFilter.EXPIRED -> hl.status == SchoolPostStatus.PUBLISHED && hl.expiresAtMillis <= nowMillis
                                HighlightFilter.UNPUBLISHED -> hl.status != SchoolPostStatus.PUBLISHED
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxSize()
                            .testTag("school_highlights_admin_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "highlights_info") {
                            InfoBanner(mStrings.schoolFeed.highlightsInfoBanner)
                        }
                        item(key = "highlights_filters") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HighlightFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = highlightFilter == filter,
                                        onClick = { highlightFilter = filter },
                                        label = { Text(filter.label(mStrings)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = brand.forestGreenContainer,
                                            selectedLabelColor = brand.onForestGreenContainer
                                        ),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("highlight_filter_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (filteredHighlights.isEmpty()) {
                            item(key = "empty_highlights") {
                                EmptyFeedCard(
                                    message = if (highlights.isEmpty()) {
                                        mStrings.schoolFeed.emptyHighlightsMessage
                                    } else {
                                        mStrings.schoolFeed.emptyFilteredHighlightsMessage
                                    },
                                    actionLabel = if (highlights.isEmpty() && canCreateHighlights) mStrings.schoolFeed.addHighlightBtn else null,
                                    onAction = {
                                        highlightRevisionInfo = EditorRevisionInfo()
                                        highlightEditorTarget = SchoolHighlightEntity(
                                            title = "",
                                            youtubeShortsUrl = "",
                                            status = SchoolPostStatus.UNPUBLISHED
                                        )
                                    }
                                )
                            }
                        } else {
                            items(filteredHighlights, key = { it.id }) { hl ->
                                SchoolHighlightAdminCard(
                                    highlight = hl,
                                    nowMillis = nowMillis,
                                    mStrings = mStrings,
                                    hasPendingApproval = hl.id in pendingHighlightIds,
                                    canEdit = canEditHighlights,
                                    canDelete = canDeleteHighlights,
                                    canPublish = canPublishHighlights,
                                    onEdit = {
                                        scope.launch {
                                            val resolved = managementViewModel.resolveEditableRevision(
                                                section = ManagementSection.SCHOOL_HIGHLIGHTS,
                                                recordId = hl.id
                                            )
                                            val resumed = resolved.editableSnapshotJson?.let { json ->
                                                runCatching {
                                                    ContentSnapshotSerializer.schoolHighlightFromJson(
                                                        jsonStr = json,
                                                        publishNow = hl.status == SchoolPostStatus.PUBLISHED,
                                                        forcedStatus = hl.status
                                                    ).copy(id = hl.id)
                                                }.getOrNull()
                                            } ?: hl
                                            highlightRevisionInfo = EditorRevisionInfo.fromResolution(resolved)
                                            highlightEditorTarget = resumed
                                        }
                                    },
                                    onTogglePublish = {
                                        managementViewModel.togglePublishState(
                                            section = ManagementSection.SCHOOL_HIGHLIGHTS,
                                            recordId = hl.id,
                                            currentlyPublished = hl.status == SchoolPostStatus.PUBLISHED
                                        )
                                    },
                                    onRequestDelete = { pendingDeleteHighlight = hl }
                                )
                            }
                        }

                        item(key = "highlights_bottom_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                } else {
                    Box(modifier = Modifier.padding(20.dp)) {
                        InfoBanner(mStrings.noPermissionNotice)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SchoolPostAdminCard(
    post: SchoolPostEntity,
    mStrings: ManagementStrings,
    hasPendingApproval: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    canPublish: Boolean,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val parsedYt = remember(post.youtubeUrl) { YouTubeMediaHelper.parse(post.youtubeUrl) }
    val photos = remember(post.photoUrisJson) { post.photoUris() }
    val previewThumb = when (post.postType) {
        SchoolPostType.YOUTUBE_DIRECT -> parsedYt?.thumbnailUrl
        SchoolPostType.ADMIN_CUSTOM_VIDEO -> post.customThumbnailUri ?: parsedYt?.thumbnailUrl
        SchoolPostType.PHOTO -> photos.firstOrNull()
        else -> null
    }
    val isPublished = post.status == SchoolPostStatus.PUBLISHED
    val isArchived = post.status == SchoolPostStatus.ARCHIVED
    val canArchiveThis = !isArchived && if (isPublished) canPublish else canEdit

    val (statusLabel, statusBg, statusFg) = when {
        hasPendingApproval -> Triple(
            mStrings.statusPendingApproval,
            brand.royalGold.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.onSurface
        )
        isPublished -> Triple(
            mStrings.statusPublished,
            brand.forestGreenContainer,
            brand.onForestGreenContainer
        )
        isArchived -> Triple(
            mStrings.schoolFeed.statusArchived,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Triple(
            mStrings.statusUnpublished,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (post.isFeatured && !isArchived) 1.5.dp else 1.dp,
            color = if (post.isFeatured && !isArchived) brand.royalGold else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_school_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(color = statusBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (post.isFeatured && !isArchived) {
                    Surface(color = brand.royalGold.copy(alpha = 0.28f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = mStrings.schoolFeed.pinnedBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Surface(color = brand.burgundy.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    val typeText = if (post.postType == SchoolPostType.PHOTO) {
                        mStrings.schoolFeed.photoPostCountLabel(photos.size)
                    } else {
                        postTypeDisplayName(post.postType, mStrings)
                    }
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = brand.burgundy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = mStrings.schoolFeed.categoryLabel(post.category),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!previewThumb.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(previewThumb).size(320).crossfade(false).build(),
                        contentDescription = post.title.ifBlank { mStrings.schoolFeed.postPreviewDesc },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 92.dp, height = 68.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val displayTitle = post.title.ifBlank {
                        parsedYt?.let { mStrings.schoolFeed.youtubeVideoFallbackTitle(it.videoId) }
                            ?: mStrings.schoolFeed.postFallbackTitle(post.id)
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.description.isNotBlank()) {
                        Text(
                            text = post.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = mStrings.schoolFeed.updatedTimestampLabel(mStrings.formatTimestamp(post.updatedAtMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Governed Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canPublish && !isArchived) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.testTag("post_pin_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = if (post.isFeatured) mStrings.schoolFeed.unpinPostDesc else mStrings.schoolFeed.pinPostDesc,
                            tint = if (post.isFeatured) brand.burgundy else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onTogglePublish,
                        modifier = Modifier.testTag("post_publish_toggle_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isPublished) Icons.Outlined.VisibilityOff else Icons.Outlined.Public,
                            contentDescription = if (isPublished) mStrings.unpublishBtn else mStrings.publishBtn,
                            tint = if (isPublished) brand.burgundy else brand.forestGreen
                        )
                    }
                }
                if (canArchiveThis) {
                    IconButton(
                        onClick = onArchive,
                        modifier = Modifier.testTag("post_archive_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = mStrings.schoolFeed.archivePostDesc,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isArchived && canEdit) {
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier.testTag("post_restore_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Unarchive,
                            contentDescription = mStrings.schoolFeed.restoreArchivedPostDesc,
                            tint = brand.forestGreen
                        )
                    }
                }
                if (canEdit) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("post_edit_btn_${post.id}")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = mStrings.editBtn)
                    }
                }
                if (canDelete) {
                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier.testTag("post_delete_btn_${post.id}")
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

@Composable
private fun SchoolHighlightAdminCard(
    highlight: SchoolHighlightEntity,
    nowMillis: Long,
    mStrings: ManagementStrings,
    hasPendingApproval: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    canPublish: Boolean,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val parsed = remember(highlight.youtubeShortsUrl) { YouTubeMediaHelper.parse(highlight.youtubeShortsUrl) }
    val isPublished = highlight.status == SchoolPostStatus.PUBLISHED
    val isActive = highlight.isActiveAt(nowMillis)
    val isExpired = isPublished && !isActive

    val remainingHours = ((highlight.expiresAtMillis - nowMillis).coerceAtLeast(0L) / (60L * 60L * 1000L))
    val (badgeLabel, badgeBg, badgeFg) = when {
        hasPendingApproval -> Triple(mStrings.statusPendingApproval, brand.royalGold.copy(alpha = 0.28f), MaterialTheme.colorScheme.onSurface)
        isActive -> Triple(mStrings.schoolFeed.highlightActiveBadge(remainingHours.coerceAtLeast(1L)), brand.forestGreenContainer, brand.onForestGreenContainer)
        isExpired -> Triple(mStrings.schoolFeed.highlightExpiredBadge, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple(mStrings.statusUnpublished, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isActive) brand.forestGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_highlight_card_${highlight.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Row {
                    if (canPublish) {
                        IconButton(
                            onClick = onTogglePublish,
                            modifier = Modifier.testTag("highlight_publish_toggle_btn_${highlight.id}")
                        ) {
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
                            modifier = Modifier.testTag("highlight_edit_btn_${highlight.id}")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = mStrings.editBtn)
                        }
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onRequestDelete,
                            modifier = Modifier.testTag("highlight_delete_btn_${highlight.id}")
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = mStrings.deleteBtn, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (parsed != null) {
                    Box(
                        modifier = Modifier
                            .size(width = 76.dp, height = 96.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(parsed.thumbnailUrl).size(320).crossfade(false).build(),
                            contentDescription = highlight.title.ifBlank { mStrings.schoolFeed.youtubeShortThumbnailDesc },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = highlight.title.ifBlank {
                            parsed?.let { mStrings.schoolFeed.youtubeShortFallbackTitle(it.videoId) }
                                ?: mStrings.schoolFeed.schoolHighlightFallbackTitle
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = highlight.youtubeShortsUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.burgundy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mStrings.schoolFeed.expiresTimestampLabel(mStrings.formatTimestamp(highlight.expiresAtMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolHighlightComposerDialog(
    target: SchoolHighlightEntity,
    revisionInfo: EditorRevisionInfo,
    managementViewModel: ManagementViewModel,
    mStrings: ManagementStrings,
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

    var shortsUrl by remember(target) { mutableStateOf(target.youtubeShortsUrl) }
    var title by remember(target) { mutableStateOf(target.title) }
    var validationError by remember(target) { mutableStateOf<String?>(null) }
    var fetchedMeta by remember(target) { mutableStateOf<YouTubeFetchedMetadata?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var fetchedForUrl by remember(target) { mutableStateOf<String?>(null) }

    val parsed = remember(shortsUrl) { YouTubeMediaHelper.parse(shortsUrl) }
    val isValidShort = parsed?.isShort == true

    LaunchedEffect(parsed?.canonicalWatchUrl, isValidShort) {
        val canonical = parsed?.canonicalWatchUrl
        if (isValidShort && canonical != null && canonical != fetchedForUrl) {
            isFetching = true
            val meta = YouTubeMetadataFetcher.fetchMetadata(canonical)
            isFetching = false
            fetchedMeta = meta
            fetchedForUrl = canonical
            if (title.isBlank() && !meta?.title.isNullOrBlank()) {
                title = meta!!.title!!
            }
        }
    }

    val isDirty = shortsUrl.trim() != target.youtubeShortsUrl.trim() || title.trim() != target.title.trim()

    UniversalContentEditorScaffold(
        mStrings = mStrings,
        title = if (target.id > 0L) mStrings.schoolFeed.editHighlightTitle else mStrings.schoolFeed.newHighlightTitle,
        subtitle = mStrings.schoolFeed.highlightSubtitle,
        canEdit = canEdit,
        canPublish = canPublish,
        isDirty = isDirty,
        isBusy = isBusy,
        isFullScreenDialog = true,
        revisionInfo = revisionInfo,
        errorMessage = validationError ?: vmError,
        statusMessage = vmStatus,
        onClearError = {
            validationError = null
            managementViewModel.clearFeedback()
        },
        onClearStatus = { managementViewModel.clearFeedback() },
        onSaveAction = { saveMode: PublishSaveMode ->
            val p = YouTubeMediaHelper.parse(shortsUrl)
            if (p == null || !p.isShort) {
                validationError = mStrings.schoolFeed.invalidShortsUrlError
                return@UniversalContentEditorScaffold
            }
            val now = System.currentTimeMillis()
            val cleanTitle = title.trim()
            val entity = SchoolHighlightEntity(
                id = target.id,
                title = cleanTitle,
                youtubeShortsUrl = p.canonicalWatchUrl,
                status = target.status,
                createdAtMillis = if (target.id > 0L) target.createdAtMillis else now,
                publishedAtMillis = if (target.id > 0L) target.publishedAtMillis else now,
                expiresAtMillis = if (target.id > 0L) target.expiresAtMillis else SchoolHighlightEntity.computeExpiryMillis(now)
            )
            validationError = null
            managementViewModel.saveManagedRecord(
                section = ManagementSection.SCHOOL_HIGHLIGHTS,
                recordId = entity.id,
                recordTitle = cleanTitle.ifBlank { mStrings.schoolFeed.highlightVideoIdTitle(p.videoId) },
                proposedJson = ContentSnapshotSerializer.schoolHighlightToJson(entity),
                saveMode = saveMode,
                changeSummary = if (target.id > 0L) mStrings.schoolFeed.changeSummaryUpdatedHighlight else mStrings.schoolFeed.changeSummaryAddedHighlight,
                onSuccess = onDismiss
            )
        },
        onClose = onDismiss,
        dialogTestTag = "school_highlight_composer_dialog"
    ) {
        InfoBanner(mStrings.schoolFeed.highlightComposerInfoBanner)

        OutlinedTextField(
            value = shortsUrl,
            onValueChange = {
                shortsUrl = it
                validationError = null
            },
            label = { Text(mStrings.schoolFeed.youtubeShortsUrlLabel) },
            placeholder = { Text("https://www.youtube.com/shorts/...") },
            singleLine = true,
            enabled = canEdit,
            isError = shortsUrl.isNotBlank() && !isValidShort,
            supportingText = {
                Text(
                    if (shortsUrl.isNotBlank() && !isValidShort) {
                        mStrings.schoolFeed.onlyShortsAcceptedHint
                    } else {
                        mStrings.schoolFeed.autoThumbnailPreviewHint
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("highlight_shorts_url_input")
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(mStrings.schoolFeed.highlightLabelOptional) },
            singleLine = true,
            enabled = canEdit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("highlight_title_input")
        )

        if (isValidShort && parsed != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = brand.forestGreen, modifier = Modifier.size(18.dp))
                    Text(mStrings.schoolFeed.highlightThumbnailPreviewTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isFetching = true
                            fetchedMeta = YouTubeMetadataFetcher.fetchMetadata(parsed.canonicalWatchUrl)
                            isFetching = false
                        }
                    },
                    enabled = !isFetching,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(mStrings.schoolFeed.refreshBtn)
                }
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("highlight_thumbnail_preview_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fetchedMeta?.thumbnailUrl ?: parsed.thumbnailUrl)
                            .crossfade(false)
                            .build(),
                        contentDescription = title.ifBlank { mStrings.schoolFeed.youtubeShortsPreviewDesc },
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
                            contentDescription = mStrings.schoolFeed.youtubeShortLabel,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedCard(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
