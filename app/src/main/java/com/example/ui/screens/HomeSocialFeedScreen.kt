package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolProfileEntity
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.SectionHeaderText
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.delay

private data class ActiveYouTubePlayback(
    val url: String,
    val title: String?
)

private data class ActivePhotoLightbox(
    val photos: List<GalleryMediaEntity>,
    val index: Int
)

/**
 * Chevalier Academy Phase 3 Home Social Network Screen.
 * Preserves exact section order:
 * 1. Existing Hero Banner
 * 2. School Highlights (4 vertical Shorts + 5th "More" card, 24h expiry)
 * 3. School Social Feed / Timeline (All Posts + category filters, Like & Bookmark)
 * 4. Existing Footer
 */
@Composable
fun HomeSocialFeedScreen(
    profile: SchoolProfileEntity,
    heroPhotos: List<CampusHeroPhotoEntity>,
    publishedPosts: List<SchoolPostEntity>,
    publishedHighlights: List<SchoolHighlightEntity>,
    likedPostIds: Set<Long>,
    bookmarkedPostIds: Set<Long>,
    onToggleLike: (Long) -> Unit,
    onToggleBookmark: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    onOpenExternalYouTube: ((String) -> Unit)? = null,
    inlinePlayerLifecycleHooksProvider: ((postId: Long) -> YouTubePlayerLifecycleHooks?)? = null
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    // Refresh clock only while foregrounded (STARTED) so backgrounded apps do not wake every 30s;
    // immediately refreshes on return to foreground so expired 24-hour highlights hide promptly.
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentNowMillis by remember { mutableLongStateOf(nowMillisProvider()) }
    LaunchedEffect(lifecycleOwner, nowMillisProvider) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            currentNowMillis = nowMillisProvider()
            while (true) {
                delay(30_000L)
                currentNowMillis = nowMillisProvider()
            }
        }
    }

    val activeHighlights = remember(publishedHighlights, currentNowMillis) {
        publishedHighlights
            .filter { it.isActiveAt(currentNowMillis) }
            .sortedWith(
                compareByDescending<SchoolHighlightEntity> { it.publishedAtMillis }
                    .thenByDescending { it.id }
            )
    }

    // Defensive filter: only PUBLISHED posts are ever displayed publicly, newest-first (pinned first)
    val publicPosts = remember(publishedPosts) {
        publishedPosts
            .filter { it.status == SchoolPostStatus.PUBLISHED }
            .sortedWith(
                compareByDescending<SchoolPostEntity> { it.isFeatured }
                    .thenByDescending { it.publishedAtMillis ?: it.createdAtMillis }
                    .thenByDescending { it.id }
            )
    }

    val availableCategories = remember(publicPosts) {
        val extras = publicPosts.map { it.category }.filter { it.isNotBlank() && it !in SchoolPostCategory.defaults }
        (SchoolPostCategory.defaults + extras).distinct()
    }

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val filteredPosts = remember(publicPosts, selectedCategory) {
        val cat = selectedCategory
        if (cat.isNullOrBlank()) {
            publicPosts
        } else {
            publicPosts.filter { it.category.equals(cat, ignoreCase = true) }
        }
    }

    var showAllHighlights by remember { mutableStateOf(false) }
    var activePlayback by remember { mutableStateOf<ActiveYouTubePlayback?>(null) }
    var activeInlinePostId by remember { mutableStateOf<Long?>(null) }
    var activeLightbox by remember { mutableStateOf<ActivePhotoLightbox?>(null) }

    if (showAllHighlights) {
        AllHighlightsDialog(
            activeHighlights = activeHighlights,
            nowMillis = currentNowMillis,
            onPlayHighlight = { hl, title ->
                showAllHighlights = false
                activeInlinePostId = null
                activePlayback = ActiveYouTubePlayback(
                    url = hl.youtubeShortsUrl,
                    title = title
                )
            },
            onDismiss = { showAllHighlights = false }
        )
    }

    activePlayback?.let { playback ->
        YouTubeVideoPlayerDialog(
            youtubeUrl = playback.url,
            title = playback.title,
            onDismiss = { activePlayback = null },
            onOpenExternal = onOpenExternalYouTube,
            isHighlight = true
        )
    }

    activeLightbox?.let { lb ->
        PhotoLightboxDialog(
            photos = lb.photos,
            currentIndex = lb.index,
            onIndexChange = { newIndex -> activeLightbox = lb.copy(index = newIndex) },
            onDismiss = { activeLightbox = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // SECTION 1 — EXISTING HERO BANNER
            item(key = "section_hero") {
                HomeSchoolHeroSection(
                    profile = profile,
                    heroPhotos = heroPhotos
                )
            }

            // SECTION 2 — SCHOOL HIGHLIGHTS (24-HOUR YOUTUBE SHORTS)
            item(key = "section_highlights") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_section_highlights"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderText(
                        title = strings.socialHighlightsTitle,
                        actionLabel = if (activeHighlights.isNotEmpty()) strings.socialMoreLabel else null,
                        onActionClick = if (activeHighlights.isNotEmpty()) {
                            { showAllHighlights = true }
                        } else {
                            null
                        }
                    )
                    SchoolHighlightsStrip(
                        activeHighlights = activeHighlights,
                        nowMillis = currentNowMillis,
                        onPlayHighlight = { hl, title ->
                            activeInlinePostId = null
                            activePlayback = ActiveYouTubePlayback(
                                url = hl.youtubeShortsUrl,
                                title = title
                            )
                        },
                        onOpenMoreHighlights = {
                            activeInlinePostId = null
                            showAllHighlights = true
                        }
                    )
                }
            }

            // SECTION 4 — SCHOOL SOCIAL FEED / TIMELINE HEADER & CATEGORY FILTERS
            item(key = "section_social_feed_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_section_social_feed"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderText(title = strings.socialFeedTitle)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_feed_category_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "cat_all") {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = {
                                    activeInlinePostId = null
                                    selectedCategory = null
                                },
                                label = { Text(strings.socialAllPostsFilter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = brand.burgundy,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("feed_filter_all")
                            )
                        }
                        items(availableCategories, key = { it }) { category ->
                            val isSelected = selectedCategory == category
                            val safeTag = category.lowercase().replace(Regex("[^a-z0-9]+"), "_")
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    activeInlinePostId = null
                                    selectedCategory = category
                                },
                                label = { Text(strings.socialCategoryLabel(category)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = brand.burgundy,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("feed_filter_$safeTag")
                            )
                        }
                    }
                }
            }

            // SOCIAL FEED TIMELINE POSTS OR CLEAN EMPTY STATE
            if (filteredPosts.isEmpty()) {
                item(key = "section_social_feed_empty") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_feed_empty_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = brand.burgundy.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.6f)),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.DynamicFeed,
                                        contentDescription = strings.socialFeedTitle,
                                        tint = brand.burgundy,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = selectedCategory?.let { strings.formatCategoryEmptyTitle(strings.socialCategoryLabel(it)) }
                                    ?: strings.socialFeedEmptyTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (selectedCategory == null) {
                                    strings.socialFeedEmptyDesc
                                } else {
                                    strings.socialFeedCategoryEmptyDesc
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (selectedCategory != null) {
                                OutlinedButton(
                                    onClick = { selectedCategory = null },
                                    border = BorderStroke(1.dp, brand.burgundy),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("feed_empty_reset_filter_btn")
                                ) {
                                    Text(strings.socialShowAllPostsBtn, color = brand.burgundy)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    SchoolSocialPostCard(
                        post = post,
                        profile = profile,
                        isLiked = post.id in likedPostIds,
                        isBookmarked = post.id in bookmarkedPostIds,
                        onToggleLike = { onToggleLike(post.id) },
                        onToggleBookmark = { onToggleBookmark(post.id) },
                        onPlayYouTube = { _, _ ->
                            activePlayback = null
                            activeInlinePostId = post.id
                        },
                        onOpenPhotoLightbox = { idx ->
                            val mediaList = schoolPostPhotosToGalleryMedia(post)
                            if (mediaList.isNotEmpty() && idx in mediaList.indices) {
                                activeInlinePostId = null
                                activeLightbox = ActivePhotoLightbox(photos = mediaList, index = idx)
                            }
                        },
                        onOpenExternalYouTube = onOpenExternalYouTube,
                        isInlinePlaying = activeInlinePostId == post.id,
                        onStopInlinePlayback = {
                            if (activeInlinePostId == post.id) {
                                activeInlinePostId = null
                            }
                        },
                        onInlinePlayerDisposed = {
                            if (activeInlinePostId == post.id) {
                                activeInlinePostId = null
                            }
                        },
                        inlinePlayerLifecycleHooks = inlinePlayerLifecycleHooksProvider?.invoke(post.id)
                    )
                }
            }

            // SECTION 5 — EXISTING FOOTER
            item(key = "section_footer") {
                PoweredByJkLabsFooter()
            }
        }
    }
}

/**
 * Dedicated "Bookmarks" screen displaying only published, publicly visible posts
 * bookmarked on the current Android device. Unbookmarking immediately removes the post.
 */
@Composable
fun BookmarksScreen(
    profile: SchoolProfileEntity,
    publishedPosts: List<SchoolPostEntity>,
    likedPostIds: Set<Long>,
    bookmarkedPostIds: Set<Long>,
    onToggleLike: (Long) -> Unit,
    onToggleBookmark: (Long) -> Unit,
    onExploreFeed: () -> Unit,
    onOpenExternalYouTube: ((String) -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    // Strictly only PUBLISHED posts that are currently bookmarked on this device
    val bookmarkedPosts = remember(publishedPosts, bookmarkedPostIds) {
        publishedPosts
            .filter { it.status == SchoolPostStatus.PUBLISHED && it.id in bookmarkedPostIds }
            .sortedWith(
                compareByDescending<SchoolPostEntity> { it.publishedAtMillis ?: it.createdAtMillis }
                    .thenByDescending { it.id }
            )
    }

    var activePlayback by remember { mutableStateOf<ActiveYouTubePlayback?>(null) }
    var activeLightbox by remember { mutableStateOf<ActivePhotoLightbox?>(null) }

    activePlayback?.let { playback ->
        YouTubeVideoPlayerDialog(
            youtubeUrl = playback.url,
            title = playback.title,
            onDismiss = { activePlayback = null },
            onOpenExternal = onOpenExternalYouTube
        )
    }

    activeLightbox?.let { lb ->
        PhotoLightboxDialog(
            photos = lb.photos,
            currentIndex = lb.index,
            onIndexChange = { newIndex -> activeLightbox = lb.copy(index = newIndex) },
            onDismiss = { activeLightbox = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("bookmarks_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (bookmarkedPosts.isEmpty()) {
                item(key = "bookmarks_empty") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bookmarks_empty_state"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = brand.burgundy.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.65f)),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkBorder,
                                        contentDescription = strings.titleBookmarks,
                                        tint = brand.burgundy,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = strings.socialBookmarksEmptyTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.socialBookmarksEmptyDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onExploreFeed,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.burgundy,
                                    contentColor = Color.White
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("bookmarks_explore_home_btn")
                            ) {
                                Text(strings.socialBrowseFeedBtn)
                            }
                        }
                    }
                }
            } else {
                items(bookmarkedPosts, key = { it.id }) { post ->
                    SchoolSocialPostCard(
                        post = post,
                        profile = profile,
                        isLiked = post.id in likedPostIds,
                        isBookmarked = true,
                        onToggleLike = { onToggleLike(post.id) },
                        onToggleBookmark = { onToggleBookmark(post.id) },
                        onPlayYouTube = { url, displayTitle ->
                            activePlayback = ActiveYouTubePlayback(url = url, title = displayTitle)
                        },
                        onOpenPhotoLightbox = { idx ->
                            val mediaList = schoolPostPhotosToGalleryMedia(post)
                            if (mediaList.isNotEmpty() && idx in mediaList.indices) {
                                activeLightbox = ActivePhotoLightbox(photos = mediaList, index = idx)
                            }
                        },
                        onOpenExternalYouTube = onOpenExternalYouTube
                    )
                }
            }

            item(key = "bookmarks_footer") {
                PoweredByJkLabsFooter()
            }
        }
    }
}
