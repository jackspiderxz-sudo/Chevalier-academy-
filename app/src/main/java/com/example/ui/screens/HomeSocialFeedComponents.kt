package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.media.ParsedYouTubeVideo
import com.example.data.media.YouTubeFetchedMetadata
import com.example.data.media.YouTubeMediaHelper
import com.example.data.media.YouTubeMetadataFetcher
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureFullScreenDialogProperties
import com.example.ui.components.launchExternalUrl
import com.example.ui.theme.LocalBrandPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Thread-safe bounded LRU in-memory cache of fetched YouTube metadata (max 100 entries)
 * so scrolling the feed does not repeat network lookups or grow unbounded across long sessions.
 * Never invents content when unavailable.
 */
internal object PublicYouTubeMetadataCache {
    internal const val MAX_ENTRIES = 100
    private val lock = Any()
    private val cache = object : LinkedHashMap<String, YouTubeFetchedMetadata>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, YouTubeFetchedMetadata>?
        ): Boolean = size > MAX_ENTRIES
    }

    fun get(videoId: String): YouTubeFetchedMetadata? = synchronized(lock) {
        cache[videoId]
    }

    fun put(metadata: YouTubeFetchedMetadata) {
        synchronized(lock) {
            cache[metadata.videoId] = metadata
        }
    }

    fun size(): Int = synchronized(lock) {
        cache.size
    }

    fun clear() {
        synchronized(lock) {
            cache.clear()
        }
    }
}

data class YouTubePlayerLifecycleHooks(
    val onInitialLoad: ((baseUrl: String, html: String) -> Unit)? = null,
    val onPause: () -> Unit = {},
    val onPauseScriptEvaluated: ((script: String) -> Unit)? = null,
    val onResume: () -> Unit = {},
    val onForegroundReloadStatic: ((baseUrl: String, html: String) -> Unit)? = null,
    val onDisposeCleanup: () -> Unit = {}
)

internal fun formatSocialTimestamp(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}

internal fun formatHighlightRemainingLabel(expiresAtMillis: Long, nowMillis: Long): String {
    val remainingMs = (expiresAtMillis - nowMillis).coerceAtLeast(0L)
    val remainingHours = ceil(remainingMs / (60.0 * 60.0 * 1000.0)).toInt().coerceIn(1, 24)
    return "${remainingHours}h"
}

internal fun schoolPostPhotosToGalleryMedia(post: SchoolPostEntity): List<GalleryMediaEntity> {
    val uris = post.photoUris()
    return uris.mapIndexed { index, uri ->
        GalleryMediaEntity(
            id = post.id * 100L + index,
            albumId = post.id,
            title = post.title.takeIf { it.isNotBlank() },
            caption = post.description.takeIf { it.isNotBlank() },
            mediaUri = uri,
            mediaType = "IMAGE",
            displayOrder = index,
            isVerified = true,
            isPublished = true,
            isApproved = true
        )
    }
}

@Composable
internal fun SchoolHighlightsStrip(
    activeHighlights: List<SchoolHighlightEntity>,
    nowMillis: Long,
    onPlayHighlight: (SchoolHighlightEntity, String?) -> Unit,
    onOpenMoreHighlights: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    if (activeHighlights.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("home_highlights_empty_card"),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = brand.burgundy.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.55f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.VideoLibrary,
                            contentDescription = strings.socialHighlightsTitle,
                            tint = brand.burgundy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.socialHighlightsEmptyTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.socialHighlightsEmptyDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val visibleFour = remember(activeHighlights) { activeHighlights.take(4) }
    val remainingCount = (activeHighlights.size - visibleFour.size).coerceAtLeast(0)

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_highlights_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(visibleFour, key = { it.id }) { highlight ->
            VerticalHighlightShortCard(
                highlight = highlight,
                nowMillis = nowMillis,
                onClick = { resolvedTitle -> onPlayHighlight(highlight, resolvedTitle) },
                modifier = Modifier
                    .width(70.dp)
                    .height(114.dp)
                    .testTag("highlight_card_${highlight.id}")
            )
        }

        // Fifth "More" card
        item(key = "highlight_more_card") {
            HighlightMoreCard(
                totalActiveCount = activeHighlights.size,
                remainingCount = remainingCount,
                onClick = onOpenMoreHighlights,
                modifier = Modifier
                    .width(70.dp)
                    .height(114.dp)
                    .testTag("highlight_more_card")
            )
        }
    }
}

@Composable
internal fun VerticalHighlightShortCard(
    highlight: SchoolHighlightEntity,
    nowMillis: Long,
    onClick: (resolvedTitle: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val parsed = remember(highlight.youtubeShortsUrl) {
        YouTubeMediaHelper.parse(highlight.youtubeShortsUrl)
    }
    var fetchedMeta by remember(parsed?.videoId) {
        mutableStateOf(parsed?.videoId?.let { PublicYouTubeMetadataCache.get(it) })
    }
    LaunchedEffect(highlight.youtubeShortsUrl, highlight.title) {
        val videoId = parsed?.videoId ?: return@LaunchedEffect
        if (highlight.title.isBlank() && fetchedMeta == null) {
            val fetched = YouTubeMetadataFetcher.fetchMetadata(highlight.youtubeShortsUrl)
            if (fetched != null) {
                PublicYouTubeMetadataCache.put(fetched)
                fetchedMeta = fetched
            }
        }
    }

    val displayTitle = highlight.title.takeIf { it.isNotBlank() } ?: fetchedMeta?.title
    val thumbUrl = fetchedMeta?.thumbnailUrl ?: parsed?.thumbnailUrl
    var thumbError by remember(thumbUrl) { mutableStateOf(false) }
    val remainingLabel = remember(highlight.expiresAtMillis, nowMillis) {
        formatHighlightRemainingLabel(highlight.expiresAtMillis, nowMillis)
    }

    Card(
        onClick = { onClick(displayTitle) },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!thumbUrl.isNullOrBlank() && !thumbError) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = displayTitle ?: strings.socialHighlightShortDesc,
                    contentScale = ContentScale.Crop,
                    onError = { thumbError = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(brand.burgundy, Color(0xFF3E0B10))
                            )
                        )
                )
            }

            // Contrast scrim for badge & title readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.32f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
            )

            // Top remaining validity badge (24h window)
            Surface(
                color = Color.Black.copy(alpha = 0.56f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(0.6.dp, brand.royalGold.copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            ) {
                Text(
                    text = "$remainingLabel ${strings.socialHighlightLeftSuffix}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            // Center Play indicator
            Surface(
                shape = CircleShape,
                color = brand.burgundy.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, brand.royalGold),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = strings.socialPlayHighlightDesc,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom original title (only if available — never fabricated)
            if (!displayTitle.isNullOrBlank()) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp, vertical = 5.dp)
                )
            } else {
                Surface(
                    color = Color.Black.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                ) {
                    Text(
                        text = strings.socialYoutubeShortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            lineHeight = 10.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightMoreCard(
    totalActiveCount: Int,
    remainingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.1.dp, brand.royalGold.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            brand.burgundy,
                            Color(0xFF5D131A),
                            Color(0xFF0A472A)
                        )
                    )
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = brand.royalGold.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, brand.royalGold),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = strings.socialMoreHighlightsDesc,
                            tint = brand.royalGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = strings.socialMoreLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 13.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = if (remainingCount > 0) {
                        strings.formatMoreActiveHighlights(remainingCount)
                    } else {
                        strings.formatViewAllHighlights(totalActiveCount)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    ),
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun AllHighlightsDialog(
    activeHighlights: List<SchoolHighlightEntity>,
    nowMillis: Long,
    onPlayHighlight: (SchoolHighlightEntity, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = SecureFullScreenDialogProperties
    ) {
        ApplySecureDialogWindow()
        BackHandler(onBack = onDismiss)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("all_highlights_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.socialHighlightsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activeHighlights.size} ${strings.socialHighlightsActiveCountSuffix}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("all_highlights_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.socialCloseHighlightsDesc,
                            tint = brand.burgundy
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (activeHighlights.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.socialHighlightsDialogEmpty,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // 4 columns x 4 rows grid per page (16 tiles per page, vertically scrollable when > 16)
                    val rows = remember(activeHighlights) { activeHighlights.chunked(4) }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("all_highlights_grid_list"),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rows, key = { row -> row.joinToString("_") { it.id.toString() } }) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { hl ->
                                    VerticalHighlightShortCard(
                                        highlight = hl,
                                        nowMillis = nowMillis,
                                        onClick = { title -> onPlayHighlight(hl, title) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(9f / 15f)
                                            .testTag("all_highlights_card_${hl.id}")
                                    )
                                }
                                repeat((4 - rowItems.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.weight(1f))
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
internal fun SchoolSocialPostCard(
    post: SchoolPostEntity,
    profile: SchoolProfileEntity,
    isLiked: Boolean,
    isBookmarked: Boolean,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPlayYouTube: (url: String, displayTitle: String?) -> Unit,
    onOpenPhotoLightbox: (photoIndex: Int) -> Unit,
    onOpenExternalYouTube: ((String) -> Unit)? = null,
    isInlinePlaying: Boolean = false,
    onStopInlinePlayback: (() -> Unit)? = null,
    onInlinePlayerDisposed: (() -> Unit)? = null,
    inlinePlayerLifecycleHooks: YouTubePlayerLifecycleHooks? = null,
    showEngagementActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val parsedVideo = remember(post.youtubeUrl) {
        if (post.postType != SchoolPostType.PHOTO) YouTubeMediaHelper.parse(post.youtubeUrl) else null
    }

    var fetchedMeta by remember(parsedVideo?.videoId) {
        mutableStateOf(parsedVideo?.videoId?.let { PublicYouTubeMetadataCache.get(it) })
    }
    LaunchedEffect(post.postType, post.youtubeUrl, post.title, post.description) {
        if (post.postType == SchoolPostType.YOUTUBE_DIRECT && parsedVideo != null) {
            if (post.title.isBlank() || post.description.isBlank()) {
                val cached = PublicYouTubeMetadataCache.get(parsedVideo.videoId)
                if (cached != null) {
                    fetchedMeta = cached
                } else {
                    val fetched = YouTubeMetadataFetcher.fetchMetadata(parsedVideo.canonicalWatchUrl)
                    if (fetched != null) {
                        PublicYouTubeMetadataCache.put(fetched)
                        fetchedMeta = fetched
                    }
                }
            }
        }
    }

    val resolvedTitle = when (post.postType) {
        SchoolPostType.YOUTUBE_DIRECT -> post.title.takeIf { it.isNotBlank() } ?: fetchedMeta?.title
        else -> post.title.takeIf { it.isNotBlank() }
    }
    val resolvedDescription = when (post.postType) {
        SchoolPostType.YOUTUBE_DIRECT -> post.description.takeIf { it.isNotBlank() } ?: fetchedMeta?.description
        else -> post.description.takeIf { it.isNotBlank() }
    }

    val dateLabel = remember(post.publishedAtMillis, post.createdAtMillis) {
        formatSocialTimestamp(post.publishedAtMillis ?: post.createdAtMillis)
    }

    var overflowMenuExpanded by remember(post.id) { mutableStateOf(false) }

    val openExternalAction: (() -> Unit)? = remember(parsedVideo, onOpenExternalYouTube, context) {
        parsedVideo?.let { pv ->
            {
                val fallbackUrl = pv.externalFallbackUrl
                if (onOpenExternalYouTube != null) {
                    onOpenExternalYouTube(fallbackUrl)
                } else {
                    launchExternalUrl(context, fallbackUrl)
                }
            }
        }
    }

    val copyVideoLinkAction: (() -> Unit)? = remember(parsedVideo, context) {
        parsedVideo?.let { pv ->
            {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("YouTube Video Link", pv.canonicalWatchUrl))
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("social_post_card_${post.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (post.isFeatured) 1.4.dp else 1.dp,
            color = if (post.isFeatured) brand.royalGold.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (post.isFeatured) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Single Compact Header Row:
            // 1. School Logo
            // 2. School Name and Date
            // 3. Public / Category badge (+ Pinned badge if featured)
            // 4. Like icon only
            // 5. Bookmark icon only
            // 6. Three-dot overflow menu (⋮)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .testTag("social_post_header_${post.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.2.dp, brand.royalGold),
                    modifier = Modifier.size(38.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chevalier_emblem),
                        contentDescription = profile.shortName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(2.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.shortName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (dateLabel.isNotBlank()) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.isFeatured) {
                        Surface(
                            color = brand.royalGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.8.dp, brand.royalGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = strings.socialPinnedBadge,
                                    tint = brand.goldAccentText,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = strings.socialPinnedBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Surface(
                        color = brand.forestGreenContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = strings.socialCategoryLabel(post.category),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = brand.onForestGreenContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (showEngagementActions) {
                        val likeLabel = if (isLiked) strings.socialLikedLabel else strings.socialLikeLabel
                        val bookmarkLabel = if (isBookmarked) strings.socialBookmarkedLabel else strings.socialBookmarkLabel

                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier
                                .size(36.dp)
                                .minimumInteractiveComponentSize()
                                .semantics { contentDescription = likeLabel }
                                .testTag("post_like_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = likeLabel,
                                tint = if (isLiked) brand.burgundy else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier
                                .size(36.dp)
                                .minimumInteractiveComponentSize()
                                .semantics { contentDescription = bookmarkLabel }
                                .testTag("post_bookmark_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = bookmarkLabel,
                                tint = if (isBookmarked) brand.burgundy else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (parsedVideo != null) {
                        Box {
                            IconButton(
                                onClick = { overflowMenuExpanded = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("post_overflow_menu_btn_${post.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = strings.openTopMenuDescription,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = overflowMenuExpanded,
                                onDismissRequest = { overflowMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.socialOpenInYouTubeLabel) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = brand.burgundy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        openExternalAction?.invoke()
                                    },
                                    modifier = Modifier.testTag("post_video_external_btn_${post.id}")
                                )

                                DropdownMenuItem(
                                    text = { Text(strings.socialCopyVideoLinkLabel) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        copyVideoLinkAction?.invoke()
                                    },
                                    modifier = Modifier.testTag("post_video_copy_link_btn_${post.id}")
                                )

                                DropdownMenuItem(
                                    text = { Text(strings.closeLabel) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        onStopInlinePlayback?.invoke()
                                    },
                                    modifier = Modifier.testTag("post_video_inline_close_btn_${post.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Title & Description (never invents missing metadata)
            if (!resolvedTitle.isNullOrBlank()) {
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("social_post_title_${post.id}")
                )
            }
            if (!resolvedDescription.isNullOrBlank()) {
                Text(
                    text = resolvedDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("social_post_description_${post.id}")
                )
            }

            // Media Content by Post Type (no redundant bottom action row or extra buttons below video)
            when (post.postType) {
                SchoolPostType.YOUTUBE_DIRECT,
                SchoolPostType.ADMIN_CUSTOM_VIDEO -> {
                    val videoThumbnail = when (post.postType) {
                        SchoolPostType.ADMIN_CUSTOM_VIDEO ->
                            post.customThumbnailUri?.takeIf { it.isNotBlank() } ?: parsedVideo?.thumbnailUrl
                        else -> fetchedMeta?.thumbnailUrl ?: parsedVideo?.thumbnailUrl
                    }
                    if (parsedVideo != null) {
                        if (isInlinePlaying) {
                            SocialPostInlineYouTubePlayer(
                                postId = post.id,
                                youtubeUrl = parsedVideo.canonicalWatchUrl,
                                parsedVideo = parsedVideo,
                                onDisposed = { onInlinePlayerDisposed?.invoke() },
                                lifecycleHooks = inlinePlayerLifecycleHooks
                            )
                        } else {
                            SocialPostYouTubePreview(
                                postId = post.id,
                                thumbnailUrl = videoThumbnail,
                                titleForDescription = resolvedTitle ?: strings.youtubeVideoLabel,
                                isShort = parsedVideo.isShort,
                                onPlay = { onPlayYouTube(parsedVideo.canonicalWatchUrl, resolvedTitle) }
                            )
                        }
                    }
                }

                SchoolPostType.PHOTO -> {
                    val photos = remember(post.photoUrisJson) { post.photoUris() }
                    if (photos.isNotEmpty()) {
                        SocialPostPhotoMedia(
                            postId = post.id,
                            photos = photos,
                            contentTitle = resolvedTitle ?: profile.shortName,
                            onPhotoClick = onOpenPhotoLightbox
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialPostYouTubePreview(
    postId: Long,
    thumbnailUrl: String?,
    titleForDescription: String,
    isShort: Boolean,
    onPlay: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    var thumbFailed by remember(thumbnailUrl) { mutableStateOf(false) }

    Card(
        onClick = onPlay,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .testTag("post_video_preview_$postId")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!thumbnailUrl.isNullOrBlank() && !thumbFailed) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = titleForDescription,
                    contentScale = ContentScale.Crop,
                    onError = { thumbFailed = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(brand.burgundy, Color(0xFF3E0B10))
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            Surface(
                shape = CircleShape,
                color = brand.burgundy.copy(alpha = 0.92f),
                border = BorderStroke(1.5.dp, brand.royalGold),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = strings.socialPlayYouTubeVideoDesc,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = brand.royalGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isShort) strings.socialYoutubeShortLabel else strings.youtubeVideoLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialPostInlineYouTubePlayer(
    postId: Long,
    youtubeUrl: String,
    parsedVideo: ParsedYouTubeVideo,
    onDisposed: () -> Unit,
    lifecycleHooks: YouTubePlayerLifecycleHooks? = null
) {
    val brand = LocalBrandPalette.current

    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, brand.royalGold.copy(alpha = 0.75f)),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_video_inline_player_$postId")
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val embedFrameModifier = if (parsedVideo.isShort) {
                Modifier
                    .heightIn(max = 360.dp)
                    .aspectRatio(9f / 16f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            }
            YouTubeEmbedPlayerViewport(
                youtubeUrl = youtubeUrl,
                playerContainerTag = "post_video_inline_embed_$postId",
                fallbackCardTag = "post_video_inline_fallback_$postId",
                embedModifier = embedFrameModifier,
                fallbackModifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                wrapInCard = false,
                lifecycleHooks = lifecycleHooks,
                onDisposed = onDisposed
            )
        }
    }
}

@Composable
private fun SocialPostPhotoMedia(
    postId: Long,
    photos: List<String>,
    contentTitle: String,
    onPhotoClick: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    when {
        // 1 image: Single-image layout
        photos.size == 1 -> {
            PostPhotoTile(
                uri = photos[0],
                contentDescription = strings.formatPhotoPosition(contentTitle, 1),
                onClick = { onPhotoClick(0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("post_photo_single_$postId")
            )
        }

        // 2-4 images: Grid layout
        photos.size in 2..4 -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_photo_grid_$postId"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (photos.size) {
                    2 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            photos.forEachIndexed { idx, uri ->
                                PostPhotoTile(
                                    uri = uri,
                                    contentDescription = strings.formatPhotoPosition(contentTitle, idx + 1),
                                    onClick = { onPhotoClick(idx) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .testTag("post_photo_item_${postId}_$idx")
                                )
                            }
                        }
                    }

                    3 -> {
                        PostPhotoTile(
                            uri = photos[0],
                            contentDescription = strings.formatPhotoPosition(contentTitle, 1),
                            onClick = { onPhotoClick(0) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("post_photo_item_${postId}_0")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (idx in 1..2) {
                                PostPhotoTile(
                                    uri = photos[idx],
                                    contentDescription = strings.formatPhotoPosition(contentTitle, idx + 1),
                                    onClick = { onPhotoClick(idx) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .testTag("post_photo_item_${postId}_$idx")
                                )
                            }
                        }
                    }

                    else -> {
                        // 4 images -> 2x2 grid
                        for (row in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0..1) {
                                    val idx = row * 2 + col
                                    PostPhotoTile(
                                        uri = photos[idx],
                                        contentDescription = strings.formatPhotoPosition(contentTitle, idx + 1),
                                        onClick = { onPhotoClick(idx) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .testTag("post_photo_item_${postId}_$idx")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5-10 images: Swipeable gallery
        else -> {
            val brand = LocalBrandPalette.current
            val pagerState = rememberPagerState(pageCount = { photos.size })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_photo_pager_$postId")
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(14.dp))
                ) { page ->
                    PostPhotoTile(
                        uri = photos[page],
                        contentDescription = strings.formatPhotoOfTotal(contentTitle, page + 1, photos.size),
                        onClick = { onPhotoClick(page) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("post_photo_pager_item_${postId}_$page")
                    )
                }

                // Page counter pill
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Dot indicators
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(photos.size) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) brand.royalGold else Color.White.copy(alpha = 0.65f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostPhotoTile(
    uri: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var failed by remember(uri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Image, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!failed && uri.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .size(1080)
                    .crossfade(false)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Lifecycle-safe YouTube playback modal using YouTube Embed Player when permitted,
 * with reliable error detection and direct external YouTube fallback.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YouTubeEmbedPlayerViewport(
    youtubeUrl: String,
    playerContainerTag: String,
    fallbackCardTag: String,
    embedModifier: Modifier,
    fallbackModifier: Modifier = Modifier.fillMaxWidth(),
    wrapInCard: Boolean = true,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    lifecycleHooks: YouTubePlayerLifecycleHooks? = null,
    onDisposed: (() -> Unit)? = null,
    onCustomViewRequested: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    onCustomViewDismissed: (() -> Unit)? = null
) {
    val parsed: ParsedYouTubeVideo? = remember(youtubeUrl) { YouTubeMediaHelper.parse(youtubeUrl) }
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val currentOnDisposed by rememberUpdatedState(onDisposed)

    val embedDocumentBaseUrl = remember(context.packageName) {
        YouTubeMediaHelper.appDocumentOrigin(context.packageName)
            ?: "https://appassets.androidplatform.net"
    }
    val initialEmbedHtml = remember(parsed?.videoId, embedDocumentBaseUrl) {
        parsed?.videoId?.let {
            YouTubeMediaHelper.buildClickToPlayEmbedHtml(
                videoId = it,
                autoplay = true,
                documentBaseUrl = embedDocumentBaseUrl
            )
        }
    }
    val pausedStaticEmbedHtml = remember(parsed?.videoId, embedDocumentBaseUrl) {
        parsed?.videoId?.let {
            YouTubeMediaHelper.buildClickToPlayEmbedHtml(
                videoId = it,
                autoplay = false,
                documentBaseUrl = embedDocumentBaseUrl
            )
        }
    }

    var embedFailed by remember(youtubeUrl) { mutableStateOf(false) }
    var webViewRef by remember(youtubeUrl) { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner, youtubeUrl, embedDocumentBaseUrl, pausedStaticEmbedHtml) {
        var pausedByLifecycle = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!pausedByLifecycle) {
                        pausedByLifecycle = true
                        webViewRef?.let { view ->
                            runCatching {
                                // Require explicit user gesture before any future playback and
                                // pause both direct <video> and YouTube IFrame API before suspending WebView.
                                view.settings.mediaPlaybackRequiresUserGesture = true
                                view.evaluateJavascript(YouTubeMediaHelper.PAUSE_EMBED_PLAYER_JS, null)
                                view.onPause()
                            }
                        }
                        lifecycleHooks?.onPauseScriptEvaluated?.invoke(YouTubeMediaHelper.PAUSE_EMBED_PLAYER_JS)
                        lifecycleHooks?.onPause?.invoke()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (pausedByLifecycle) {
                        pausedByLifecycle = false
                        webViewRef?.let { view ->
                            runCatching {
                                // Keep gesture-gated playback and reload the non-autoplay static embed so
                                // returning to foreground never auto-resumes video/audio unexpectedly.
                                view.settings.mediaPlaybackRequiresUserGesture = true
                                view.onResume()
                                view.evaluateJavascript(YouTubeMediaHelper.PAUSE_EMBED_PLAYER_JS, null)
                                pausedStaticEmbedHtml?.let { staticHtml ->
                                    view.loadDataWithBaseURL(
                                        embedDocumentBaseUrl,
                                        staticHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                    lifecycleHooks?.onForegroundReloadStatic?.invoke(
                                        embedDocumentBaseUrl,
                                        staticHtml
                                    )
                                }
                            }
                        }
                        lifecycleHooks?.onResume?.invoke()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.let { view ->
                runCatching {
                    view.settings.mediaPlaybackRequiresUserGesture = true
                    view.evaluateJavascript(YouTubeMediaHelper.PAUSE_EMBED_PLAYER_JS, null)
                    view.onPause()
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.clearHistory()
                    view.removeAllViews()
                    view.destroy()
                }
            }
            webViewRef = null
            lifecycleHooks?.onDisposeCleanup?.invoke()
            currentOnDisposed?.invoke()
        }
    }

    if (parsed != null && initialEmbedHtml != null && !embedFailed) {
        val webViewContent: @Composable () -> Unit = {
            key(youtubeUrl, embedDocumentBaseUrl) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    callback?.onCustomViewHidden()
                                }

                                override fun onHideCustomView() {}
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        embedFailed = true
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    val code = errorResponse?.statusCode ?: 0
                                    if (request?.isForMainFrame == true && code >= 400) {
                                        embedFailed = true
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.cancel()
                                    embedFailed = true
                                }
                            }
                            loadDataWithBaseURL(
                                embedDocumentBaseUrl,
                                initialEmbedHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                            lifecycleHooks?.onInitialLoad?.invoke(embedDocumentBaseUrl, initialEmbedHtml)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (wrapInCard) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, brand.royalGold.copy(alpha = 0.75f)),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = embedModifier.testTag(playerContainerTag)
            ) {
                webViewContent()
            }
        } else {
            Box(modifier = embedModifier.testTag(playerContainerTag)) {
                webViewContent()
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, brand.royalGold),
            modifier = fallbackModifier.testTag(fallbackCardTag)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = brand.burgundy,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = strings.socialEmbedUnavailableTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.socialEmbedUnavailableDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Lifecycle-safe YouTube playback modal using YouTube Embed Player when permitted,
 * with reliable error detection and direct external YouTube fallback.
 */
@Composable
internal fun YouTubeVideoPlayerDialog(
    youtubeUrl: String,
    title: String?,
    onDismiss: () -> Unit,
    onOpenExternal: ((String) -> Unit)? = null,
    lifecycleHooks: YouTubePlayerLifecycleHooks? = null,
    isHighlight: Boolean = false
) {
    val parsed: ParsedYouTubeVideo? = remember(youtubeUrl) { YouTubeMediaHelper.parse(youtubeUrl) }
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activity = context as? Activity
    var isLandscapeMode by remember { mutableStateOf(false) }

    DisposableEffect(isHighlight, isLandscapeMode) {
        val originalOrientation = activity?.requestedOrientation
        if (isHighlight && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (activity != null && isLandscapeMode) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else if (activity != null && !isLandscapeMode && originalOrientation != null) {
            activity.requestedOrientation = originalOrientation
        }
        onDispose {
            if (activity != null && originalOrientation != null) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customFullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val openExternalFallback = {
        val target = parsed?.externalFallbackUrl ?: youtubeUrl
        if (onOpenExternal != null) {
            onOpenExternal(target)
        } else {
            launchExternalUrl(context, target)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = if (isHighlight) {
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        } else {
            SecureFullScreenDialogProperties
        }
    ) {
        ApplySecureDialogWindow()
        BackHandler(onBack = {
            if (customFullscreenView != null) {
                customFullscreenCallback?.onCustomViewHidden()
                customFullscreenView = null
                customFullscreenCallback = null
            } else {
                onDismiss()
            }
        })

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("youtube_player_dialog")
        ) {
            if (isHighlight) {
                // True immersive full-screen portrait player container for Highlights
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    val frameModifier = if (parsed?.isShort == true) {
                        Modifier
                            .fillMaxSize()
                            .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    }

                    YouTubeEmbedPlayerViewport(
                        youtubeUrl = youtubeUrl,
                        playerContainerTag = "youtube_embed_container",
                        fallbackCardTag = "youtube_embed_fallback_card",
                        embedModifier = frameModifier,
                        wrapInCard = false,
                        lifecycleOwner = lifecycleOwner,
                        lifecycleHooks = lifecycleHooks,
                        onCustomViewRequested = { view, callback ->
                            customFullscreenView = view
                            customFullscreenCallback = callback
                        },
                        onCustomViewDismissed = {
                            customFullscreenView = null
                            customFullscreenCallback = null
                        }
                    )
                }

                // Top translucent overlay bar with close button and title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xCC000000), Color.Transparent)
                            )
                        )
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("youtube_player_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = strings.closeLabel,
                                tint = Color.White
                            )
                        }
                        Text(
                            text = title?.takeIf { it.isNotBlank() }
                                ?: if (parsed?.isShort == true) strings.socialYoutubeShortLabel else strings.youtubeVideoLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            } else {
                // Existing Social Feed / Gallery dialog layout (unmodified)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.94f))
                        .safeDrawingPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title?.takeIf { it.isNotBlank() }
                                ?: if (parsed?.isShort == true) strings.socialYoutubeShortLabel else strings.youtubeVideoLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                isLandscapeMode = !isLandscapeMode
                                if (activity != null) {
                                    activity.requestedOrientation = if (isLandscapeMode) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("video_player_rotate_btn")
                        ) {
                            Icon(
                                imageVector = if (isLandscapeMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle full screen",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("youtube_player_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = strings.closeLabel,
                                tint = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val frameModifier = if (parsed?.isShort == true) {
                                Modifier
                                    .heightIn(max = 360.dp)
                                    .aspectRatio(9f / 16f)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            }

                            YouTubeEmbedPlayerViewport(
                                youtubeUrl = youtubeUrl,
                                playerContainerTag = "youtube_embed_container",
                                fallbackCardTag = "youtube_embed_fallback_card",
                                embedModifier = frameModifier,
                                wrapInCard = true,
                                lifecycleOwner = lifecycleOwner,
                                lifecycleHooks = lifecycleHooks
                            )

                            Button(
                                onClick = openExternalFallback,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.burgundy,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("youtube_player_external_fallback_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.openYouTubeVideoLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("youtube_player_dismiss_btn")
                            ) {
                                Text(text = strings.closeLabel, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Fullscreen custom view overlay if requested by WebChromeClient
            customFullscreenView?.let { fullscreenView ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { fullscreenView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
