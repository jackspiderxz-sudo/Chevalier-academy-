package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.YouTubeMediaHelper
import com.example.data.media.YouTubeMetadataFetcher
import com.example.data.preferences.AcademyHeroBannerConfig
import com.example.data.v2.AcademicsV2Category
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.Version2DesignTokens
import com.example.ui.components.launchExternalUrl
import com.example.ui.components.styleForAcademicsCategory
import com.example.ui.theme.LocalBrandPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ACADEMY_SCHOOL_IDENTITY_TITLE = "CHEVALIER ACADEMY"

/**
 * Exact 12 Academy Page cards in the approved 3-column × 4-row grid order.
 */
enum class AcademyPageCard(
    val id: String,
    val title: String,
    val category: AcademicsV2Category
) {
    NEWS_ANNOUNCEMENTS(
        id = "news_announcements",
        title = "News & Announcements",
        category = AcademicsV2Category.NEWS_ANNOUNCEMENTS
    ),
    ACADEMIC_CALENDAR(
        id = "academic_calendar",
        title = "Academic Calendar",
        category = AcademicsV2Category.ACADEMIC_CALENDAR
    ),
    EXAMINATIONS(
        id = "examinations",
        title = "Examinations",
        category = AcademicsV2Category.EXAMINATIONS
    ),
    ABOUT_SCHOOL(
        id = "about_school",
        title = "About School",
        category = AcademicsV2Category.ABOUT_SCHOOL
    ),
    HIGHER_SECONDARY_COURSES(
        id = "higher_secondary_courses",
        title = "Higher Secondary Courses",
        category = AcademicsV2Category.HIGHER_SECONDARY_COURSES
    ),
    FACILITIES(
        id = "facilities",
        title = "Facilities",
        category = AcademicsV2Category.FACILITIES
    ),
    TRANSPORT(
        id = "transport",
        title = "Transport",
        category = AcademicsV2Category.TRANSPORT
    ),
    SCHOOL_TIMETABLE(
        id = "school_timetable",
        title = "School Timetable",
        category = AcademicsV2Category.SCHOOL_TIMETABLE
    ),
    FACULTY_STAFF(
        id = "faculty_staff",
        title = "Faculty & Staff",
        category = AcademicsV2Category.FACULTY_STAFF
    ),
    STUDENT_ACTIVITIES(
        id = "student_activities",
        title = "Student Activities",
        category = AcademicsV2Category.STUDENT_ACTIVITIES
    ),
    GALLERY(
        id = "gallery",
        title = "Gallery",
        category = AcademicsV2Category.GALLERY
    ),
    ACADEMICS(
        id = "academics",
        title = "Academics",
        category = AcademicsV2Category.ACADEMICS
    )
}

internal fun formatAcademyPostDateTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""
    val formatter = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}

/**
 * Academy Top Header following the exact Chevalier Academy header specification:
 * - Left: Back Arrow + CHEVALIER ACADEMY (always CHEVALIER ACADEMY, never "Academy").
 * - Right (exact order): 1. Search -> 2. Notification -> 3. Menu.
 * All three right-side icons are independently clickable and functional.
 */
@Composable
fun AcademyTopHeader(
    isSearchActive: Boolean,
    hasNotifications: Boolean,
    canEditHeroBanner: Boolean,
    onBackClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onNotificationsClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onOpenDrawerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditHeroBannerClick: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("academy_top_header")
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("academy_header_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = ACADEMY_SCHOOL_IDENTITY_TITLE,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = if (brand.isDark) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        brand.burgundy
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("academy_header_title")
                )

                // Right-side icons in exact order: 1. Search -> 2. Notification -> 3. Menu
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("academy_header_search_btn")
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Outlined.Search,
                        contentDescription = "Search Academy",
                        tint = if (isSearchActive) brand.burgundy else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("academy_header_notification_btn")
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Academy Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasNotifications) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(brand.burgundy)
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("academy_header_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Academy Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.testTag("academy_header_overflow_menu")
                    ) {
                        if (canEditHeroBanner && onEditHeroBannerClick != null) {
                            DropdownMenuItem(
                                text = { Text("Manage Hero Banner") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        tint = Version2DesignTokens.GradientViolet
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditHeroBannerClick()
                                },
                                modifier = Modifier.testTag("academy_menu_edit_hero_banner")
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(strings.titleGallery) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoLibrary,
                                    contentDescription = null,
                                    tint = brand.burgundy
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onGalleryClick()
                            },
                            modifier = Modifier.testTag("academy_menu_gallery")
                        )
                        DropdownMenuItem(
                            text = { Text(strings.titleBookmarks) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = brand.burgundy
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onBookmarksClick()
                            },
                            modifier = Modifier.testTag("academy_menu_bookmarks")
                        )
                        DropdownMenuItem(
                            text = { Text(strings.titleSettings) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = brand.burgundy
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            },
                            modifier = Modifier.testTag("academy_menu_settings")
                        )
                        DropdownMenuItem(
                            text = { Text(strings.openDrawerDescription) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = null,
                                    tint = brand.burgundy
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenDrawerClick()
                            },
                            modifier = Modifier.testTag("academy_menu_open_drawer")
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * Finalized Academy Page:
 * 1. Academy Hero Banner immediately below Top Header (no external heading or forced placeholder text).
 * 2. 12 equal-sized compact Violet–Blue cards in a 3-column × 4-row grid in exact approved order.
 * 3. Dedicated Academy Social Feed below the 12 cards (separate from Home Page School Social Feed),
 *    displaying CHEVALIER ACADEMY + date & time header and ONLY Like & Bookmark actions.
 */
@Composable
fun AcademyPageScreen(
    heroBannerConfig: AcademyHeroBannerConfig,
    academyFeedPosts: List<SchoolPostEntity>,
    likedPostIds: Set<Long>,
    likedPostRemoteIds: Set<String>,
    bookmarkedPostIds: Set<Long>,
    bookmarkedPostRemoteIds: Set<String>,
    isSearchActive: Boolean,
    searchQuery: String,
    canEditHeroBanner: Boolean,
    isHeroEditorBusy: Boolean,
    showHeroEditorDialog: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onOpenHeroEditor: () -> Unit,
    onDismissHeroEditor: () -> Unit,
    onPickHeroBannerImage: (Uri, (String) -> Unit) -> Unit,
    onPublishHeroBanner: (imageUri: String?, overlayTitle: String, overlaySubtitle: String, publishNow: Boolean) -> Unit,
    onOpenCategory: (AcademicsV2Category) -> Unit,
    onToggleLike: (Long, String) -> Unit,
    onToggleBookmark: (Long, String) -> Unit
) {
    val trimmedQuery = searchQuery.trim()
    val filteredCards = remember(trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            AcademyPageCard.entries
        } else {
            AcademyPageCard.entries.filter { card ->
                card.title.contains(trimmedQuery, ignoreCase = true) ||
                    card.category.sections.any { it.title.contains(trimmedQuery, ignoreCase = true) }
            }
        }
    }
    val cardRows = remember(filteredCards) { filteredCards.chunked(3) }
    val filteredPosts = remember(academyFeedPosts, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            academyFeedPosts
        } else {
            academyFeedPosts.filter { post ->
                post.title.contains(trimmedQuery, ignoreCase = true) ||
                    post.description.contains(trimmedQuery, ignoreCase = true) ||
                    post.category.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    var activeYouTubeVideo by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var activePhotoLightbox by remember { mutableStateOf<Pair<List<GalleryMediaEntity>, Int>?>(null) }

    if (showHeroEditorDialog && canEditHeroBanner) {
        AcademyHeroBannerEditorDialog(
            initialConfig = heroBannerConfig,
            isBusy = isHeroEditorBusy,
            onPickBannerImage = onPickHeroBannerImage,
            onPublish = onPublishHeroBanner,
            onDismiss = onDismissHeroEditor
        )
    }

    activeYouTubeVideo?.let { (url, displayTitle) ->
        YouTubeVideoPlayerDialog(
            youtubeUrl = url,
            title = displayTitle,
            onDismiss = { activeYouTubeVideo = null }
        )
    }

    activePhotoLightbox?.let { (mediaItems, currentIndex) ->
        PhotoLightboxDialog(
            photos = mediaItems,
            currentIndex = currentIndex,
            onIndexChange = { nextIdx -> activePhotoLightbox = mediaItems to nextIdx },
            onDismiss = { activePhotoLightbox = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("academy_page_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isSearchActive) {
            item(key = "academy_search_bar") {
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search Academy sections & academic posts") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("academy_search_input")
                    )
                }
            }
        }

        // 1. Academy Hero Banner — immediately below Top Header, NO external heading
        item(key = "academy_hero_banner_item") {
            AcademyHeroBannerVisual(
                config = heroBannerConfig,
                canEdit = canEditHeroBanner,
                onEditClick = if (canEditHeroBanner) onOpenHeroEditor else null
            )
        }

        // 2. Academy 12 Cards in exact 3-column × 4-row grid
        if (cardRows.isEmpty()) {
            item(key = "academy_cards_search_empty") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Version2DesignTokens.cardContainerColor(),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No Academy sections match \"$trimmedQuery\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item(key = "academy_cards_grid") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("academy_cards_grid"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    cardRows.forEachIndexed { rowIndex, rowCards ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("academy_cards_row_$rowIndex"),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowCards.forEach { card ->
                                AcademyCompactGridCard(
                                    card = card,
                                    onClick = { onOpenCategory(card.category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowCards.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Dedicated Academy Social Feed (below the 12 cards)
        item(key = "academy_social_feed_section") {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (filteredPosts.isEmpty()) {
            item(key = "academy_social_feed_empty") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("academy_social_feed_empty_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Version2DesignTokens.GradientViolet.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = Version2DesignTokens.GradientViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (trimmedQuery.isEmpty()) {
                                    "No published Academy posts yet"
                                } else {
                                    "No Academy posts match \"$trimmedQuery\""
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Educational programmes, classroom activities, student projects, and academic achievements published by the school will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredPosts,
                key = { "academy_feed_post_${it.id}" }
            ) { post ->
                val isLiked = (post.id in likedPostIds) ||
                    (post.remoteId.isNotBlank() && post.remoteId in likedPostRemoteIds)
                val isBookmarked = (post.id in bookmarkedPostIds) ||
                    (post.remoteId.isNotBlank() && post.remoteId in bookmarkedPostRemoteIds)
                AcademySocialPostCard(
                    post = post,
                    isLiked = isLiked,
                    isBookmarked = isBookmarked,
                    onToggleLike = { onToggleLike(post.id, post.remoteId) },
                    onToggleBookmark = { onToggleBookmark(post.id, post.remoteId) },
                    onPlayYouTube = { url, title -> activeYouTubeVideo = url to title },
                    onOpenPhotoLightbox = { index ->
                        val items = schoolPostPhotosToGalleryMedia(post)
                        if (items.isNotEmpty()) {
                            activePhotoLightbox = items to index
                        }
                    }
                )
            }
        }

        item(key = "academy_page_footer") {
            Spacer(modifier = Modifier.height(4.dp))
            PoweredByJkLabsFooter()
        }
    }
}

@Composable
private fun AcademyCompactGridCard(
    card: AcademyPageCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val iconStyle = remember(card) { styleForAcademicsCategory(card.category) }
    val cardShape = RoundedCornerShape(16.dp)
    val badgeBg = if (isDark) iconStyle.pastelDark else iconStyle.pastelLight

    Card(
        shape = cardShape,
        border = BorderStroke(1.25.dp, Version2DesignTokens.BlueVioletBorderBrush),
        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                ambientColor = Color(0x335B6EF5),
                spotColor = Color(0x335B6EF5)
            )
            .clip(cardShape)
            .aspectRatio(4f / 3f)
            .clickable(
                role = Role.Button,
                onClickLabel = card.title,
                onClick = onClick
            )
            .testTag("academy_card_${card.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconStyle.icon,
                    contentDescription = card.title,
                    tint = iconStyle.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Academy Social Feed Post Card:
 * - Post Header: School identity "CHEVALIER ACADEMY" + date and time directly below.
 *   Never displays "Academic Updates" as post header/title.
 * - Post Content: Academic post title, description, photo(s) or video.
 * - Post Actions: ONLY TWO action options — Like and Bookmark (no Comment, no Share).
 */
@Composable
private fun AcademySocialPostCard(
    post: SchoolPostEntity,
    isLiked: Boolean,
    isBookmarked: Boolean,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPlayYouTube: (url: String, displayTitle: String?) -> Unit,
    onOpenPhotoLightbox: (photoIndex: Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current

    val parsedVideo = remember(post.youtubeUrl, post.postType) {
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
    val dateTimeLabel = remember(post.publishedAtMillis, post.createdAtMillis) {
        formatAcademyPostDateTime(post.publishedAtMillis ?: post.createdAtMillis)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (post.isFeatured) 1.4.dp else 1.dp,
            brush = Version2DesignTokens.BlueVioletBorderBrush
        ),
        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("academy_post_card_${post.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Post Header: CHEVALIER ACADEMY + Date and Time below school identity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academy_post_header_${post.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.2.dp, brand.royalGold),
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chevalier_emblem),
                        contentDescription = ACADEMY_SCHOOL_IDENTITY_TITLE,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(2.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ACADEMY_SCHOOL_IDENTITY_TITLE,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("academy_post_school_name_${post.id}")
                    )
                    if (dateTimeLabel.isNotBlank()) {
                        Text(
                            text = dateTimeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("academy_post_datetime_${post.id}")
                        )
                    }
                }
                if (post.isFeatured) {
                    Surface(
                        color = brand.royalGold.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(0.8.dp, brand.royalGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = strings.socialPinnedBadge,
                                tint = brand.goldAccentText,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = strings.socialPinnedBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (!resolvedTitle.isNullOrBlank()) {
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("academy_post_title_${post.id}")
                )
            }
            if (!resolvedDescription.isNullOrBlank()) {
                Text(
                    text = resolvedDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("academy_post_description_${post.id}")
                )
            }

            // Photo or Video Media
            when (post.postType) {
                SchoolPostType.YOUTUBE_DIRECT,
                SchoolPostType.ADMIN_CUSTOM_VIDEO -> {
                    val videoThumbnail = when (post.postType) {
                        SchoolPostType.ADMIN_CUSTOM_VIDEO ->
                            post.customThumbnailUri?.takeIf { it.isNotBlank() } ?: parsedVideo?.thumbnailUrl
                        else -> fetchedMeta?.thumbnailUrl ?: parsedVideo?.thumbnailUrl
                    }
                    if (parsedVideo != null) {
                        AcademyPostYouTubePreview(
                            postId = post.id,
                            thumbnailUrl = videoThumbnail,
                            titleForDescription = resolvedTitle ?: strings.youtubeVideoLabel,
                            onPlay = { onPlayYouTube(parsedVideo.canonicalWatchUrl, resolvedTitle) },
                            onOpenExternal = { launchExternalUrl(context, parsedVideo.externalFallbackUrl) }
                        )
                    }
                }

                SchoolPostType.PHOTO -> {
                    val photos = remember(post.photoUrisJson) { post.photoUris() }
                    if (photos.isNotEmpty()) {
                        AcademyPostPhotoMedia(
                            postId = post.id,
                            photos = photos,
                            contentTitle = resolvedTitle ?: ACADEMY_SCHOOL_IDENTITY_TITLE,
                            onPhotoClick = onOpenPhotoLightbox
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Post Actions: ONLY TWO action options — Like and Bookmark (no Comment, no Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academy_post_actions_${post.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AcademyFeedToggleButton(
                    selected = isLiked,
                    selectedIcon = Icons.Filled.Favorite,
                    unselectedIcon = Icons.Outlined.FavoriteBorder,
                    selectedLabel = strings.socialLikedLabel,
                    unselectedLabel = strings.socialLikeLabel,
                    selectedTint = brand.burgundy,
                    onClick = onToggleLike,
                    modifier = Modifier.testTag("academy_post_like_btn_${post.id}")
                )
                AcademyFeedToggleButton(
                    selected = isBookmarked,
                    selectedIcon = Icons.Filled.Bookmark,
                    unselectedIcon = Icons.Outlined.BookmarkBorder,
                    selectedLabel = strings.socialBookmarkedLabel,
                    unselectedLabel = strings.socialBookmarkLabel,
                    selectedTint = brand.burgundy,
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("academy_post_bookmark_btn_${post.id}")
                )
            }
        }
    }
}

@Composable
private fun AcademyFeedToggleButton(
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedLabel: String,
    unselectedLabel: String,
    selectedTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (selected) selectedLabel else unselectedLabel
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) selectedTint.copy(alpha = 0.12f) else Color.Transparent,
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) selectedTint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) selectedTint else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AcademyPostYouTubePreview(
    postId: Long,
    thumbnailUrl: String?,
    titleForDescription: String,
    onPlay: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141824))
            .clickable(
                role = Role.Button,
                onClickLabel = "Play $titleForDescription",
                onClick = onPlay
            )
            .testTag("academy_post_video_preview_$postId")
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = titleForDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )
        Surface(
            shape = CircleShape,
            color = Color(0xE67A1C24),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.Center)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Surface(
            onClick = onOpenExternal,
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.62f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .minimumInteractiveComponentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open video externally",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AcademyPostPhotoMedia(
    postId: Long,
    photos: List<String>,
    contentTitle: String,
    onPhotoClick: (Int) -> Unit
) {
    val context = LocalContext.current
    if (photos.size == 1) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photos.first())
                .crossfade(true)
                .build(),
            contentDescription = contentTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onPhotoClick(0) }
                .testTag("academy_post_photo_${postId}_0")
        )
    } else {
        val pagerState = rememberPagerState(pageCount = { photos.size })
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("academy_post_photo_pager_$postId")
            ) { page ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photos[page])
                        .crossfade(true)
                        .build(),
                    contentDescription = "$contentTitle (${page + 1}/${photos.size})",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPhotoClick(page) }
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.64f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
