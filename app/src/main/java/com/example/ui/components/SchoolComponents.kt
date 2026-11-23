package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewParent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.ScreenProtectionConfig
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.media.MediaStudioManager
import com.example.data.media.YouTubeMediaHelper
import com.example.ui.screens.YouTubePlayerLifecycleHooks
import com.example.ui.screens.YouTubeVideoPlayerDialog
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChevalierTopBar(
    title: String,
    showBack: Boolean,
    onBackClick: () -> Unit,
    onOpenDrawerClick: () -> Unit,
    showSettingsAction: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onGalleryClick: (() -> Unit)? = null,
    onBookmarksClick: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    var topMenuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (showBack) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backContentDescription
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenDrawerClick,
                    modifier = Modifier.testTag("top_bar_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = strings.openDrawerDescription
                    )
                }
            }
        },
        actions = {
            Surface(
                color = brand.royalGold.copy(alpha = 0.22f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.7f)),
                modifier = Modifier.padding(end = if (showSettingsAction || onGalleryClick != null || onBookmarksClick != null) 4.dp else 12.dp)
            ) {
                Text(
                    text = strings.trialDemoBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (showSettingsAction) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("top_bar_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = strings.openSettingsDescription
                    )
                }
            }
            if (onGalleryClick != null || onBookmarksClick != null) {
                Box {
                    IconButton(
                        onClick = { topMenuExpanded = true },
                        modifier = Modifier.testTag("top_bar_more_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = strings.openTopMenuDescription
                        )
                    }
                    DropdownMenu(
                        expanded = topMenuExpanded,
                        onDismissRequest = { topMenuExpanded = false },
                        modifier = Modifier.testTag("top_bar_overflow_menu")
                    ) {
                        if (onGalleryClick != null) {
                            DropdownMenuItem(
                                text = { Text(text = strings.titleGallery) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        tint = brand.burgundy
                                    )
                                },
                                onClick = {
                                    topMenuExpanded = false
                                    onGalleryClick()
                                },
                                modifier = Modifier.testTag("top_bar_menu_gallery")
                            )
                        }
                        if (onBookmarksClick != null) {
                            DropdownMenuItem(
                                text = { Text(text = strings.titleBookmarks) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = brand.burgundy
                                    )
                                },
                                onClick = {
                                    topMenuExpanded = false
                                    onBookmarksClick()
                                },
                                modifier = Modifier.testTag("top_bar_menu_bookmarks")
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}

fun splitOfficialSchoolName(fullName: String): Pair<String, String?> {
    val trimmed = fullName.trim()
    val suffix = "MATRIC HIGHER SECONDARY SCHOOL"
    return if (trimmed.equals("CHEVALIER ACADEMY $suffix", ignoreCase = true)) {
        "CHEVALIER ACADEMY" to suffix
    } else if (trimmed.endsWith(suffix, ignoreCase = true) && trimmed.length > suffix.length + 1) {
        val prefix = trimmed.substring(0, trimmed.length - suffix.length).trim()
        prefix to suffix
    } else {
        trimmed to null
    }
}

@Composable
fun HomeHeroBannerPreviewFrame(
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable () -> Unit
) {
    val brand = LocalBrandPalette.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, brand.royalGold.copy(alpha = 0.7f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
            caption?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .background(
                            Color.Black.copy(alpha = 0.55f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SchoolBrandHeaderCard(
    profile: SchoolProfileEntity,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrandPalette.current
    val strings = LocalAppStrings.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, brand.royalGold.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                brand.burgundy,
                                brand.burgundy.copy(alpha = 0.90f),
                                Color(0xFF5D131A)
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, brand.royalGold),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(84.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.chevalier_emblem),
                            contentDescription = profile.schoolName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val (primaryTitle, secondaryTitle) = splitOfficialSchoolName(profile.schoolName)
                        Text(
                            text = primaryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        if (secondaryTitle != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = secondaryTitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = brand.royalGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.75f))
                        ) {
                            Text(
                                text = "\"${profile.motto}\"",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = brand.royalGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (!profile.mediumAndGradesNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.mediumAndGradesNote,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = strings.verifiedCampusLocation,
                    tint = brand.forestGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = profile.addressLine,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AwaitingVerificationCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.VerifiedUser,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("awaiting_verification_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = brand.royalGold.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.55f)),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = strings.awaitingVerificationLabel,
                        tint = brand.goldAccentText,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = strings.awaitingVerificationLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VerifiedFieldRow(
    label: String,
    value: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val isVerified = !value.isNullOrBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isVerified) {
                brand.forestGreenContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isVerified) {
                        brand.onForestGreenContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            if (isVerified) {
                Text(
                    text = value.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Surface(
                    color = brand.royalGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.8.dp, brand.royalGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = strings.awaitingVerificationLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeaderText(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .background(LocalBrandPalette.current.burgundy, RoundedCornerShape(2.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PoweredByJkLabsFooter(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = strings.poweredByAttribution,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = LocalBrandPalette.current.goldAccentText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = strings.demoNoticeValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

fun isGalleryYouTubeMedia(media: GalleryMediaEntity): Boolean =
    media.mediaType.equals("YOUTUBE", ignoreCase = true) ||
        media.mediaType.equals(SchoolPostType.YOUTUBE_DIRECT, ignoreCase = true) ||
        media.mediaType.equals(SchoolPostType.ADMIN_CUSTOM_VIDEO, ignoreCase = true)

fun resolveGalleryYouTubeThumbnailCandidates(
    media: GalleryMediaEntity,
    customThumbnailUri: String? = null
): List<String> {
    if (!isGalleryYouTubeMedia(media)) return emptyList()
    val parsed = YouTubeMediaHelper.parse(media.mediaUri)
    return buildList {
        customThumbnailUri?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        parsed?.thumbnailUrl?.let { url ->
            if (url !in this) add(url)
        }
    }
}

@Composable
fun GalleryMediaTileContent(
    media: GalleryMediaEntity,
    fallbackContentDescription: String,
    modifier: Modifier = Modifier,
    showCaptionOverlay: Boolean = true,
    isImageReadable: Boolean = true,
    customThumbnailUri: String? = null
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val isYouTube = isGalleryYouTubeMedia(media)
    val parsedYouTube = remember(media.mediaUri, isYouTube) {
        if (isYouTube) YouTubeMediaHelper.parse(media.mediaUri) else null
    }
    val youtubeThumbnailCandidates = remember(media.mediaUri, media.mediaType, customThumbnailUri) {
        resolveGalleryYouTubeThumbnailCandidates(media, customThumbnailUri)
    }
    var youtubeThumbIndex by remember(media.mediaUri, customThumbnailUri) { mutableIntStateOf(0) }
    var imageLoadFailed by remember(media.mediaUri) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isYouTube) {
            val activeThumbnailUrl = youtubeThumbnailCandidates.getOrNull(youtubeThumbIndex)
            val videoContentDesc = media.title?.takeIf { it.isNotBlank() }
                ?: media.caption?.takeIf { it.isNotBlank() }
                ?: strings.youtubeVideoLabel

            if (!activeThumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(activeThumbnailUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = videoContentDesc,
                    contentScale = ContentScale.Crop,
                    onError = { youtubeThumbIndex += 1 },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gallery_youtube_thumbnail_${media.id}")
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    brand.burgundy,
                                    Color(0xFF470C12),
                                    Color(0xFF1F0609)
                                )
                            )
                        )
                        .testTag("gallery_youtube_thumbnail_fallback_${media.id}")
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = brand.royalGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (parsedYouTube?.isShort == true) {
                            strings.socialYoutubeShortLabel
                        } else {
                            strings.youtubeVideoLabel
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = brand.burgundy.copy(alpha = 0.9f),
                border = BorderStroke(1.5.dp, brand.royalGold),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .testTag("gallery_youtube_play_overlay_${media.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = strings.socialPlayYouTubeVideoDesc,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            val visibleTitle = media.title?.takeIf { it.isNotBlank() }
            val visibleCaption = if (showCaptionOverlay) media.caption?.takeIf { it.isNotBlank() } else null
            if (visibleTitle != null || visibleCaption != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        visibleTitle?.let { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        visibleCaption?.let { caption ->
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else if (media.mediaUri.isBlank() || !isImageReadable || imageLoadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(
                        imageVector = if (media.mediaUri.isBlank()) Icons.Outlined.PhotoLibrary else Icons.Outlined.BrokenImage,
                        contentDescription = strings.mediaUnavailableLabel,
                        tint = if (media.mediaUri.isBlank()) brand.burgundy else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    if (media.mediaUri.isNotBlank()) {
                        Text(
                            text = strings.mediaUnavailableLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            AsyncImage(
                model = media.mediaUri,
                contentDescription = media.caption ?: media.title ?: fallbackContentDescription,
                contentScale = ContentScale.Crop,
                onError = { imageLoadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isYouTube && showCaptionOverlay) {
            media.caption?.takeIf { it.isNotBlank() }?.let { caption ->
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Standard DialogProperties governed by [ScreenProtectionConfig.isScreenProtectionEnabled].
 * Resolves to [SecureFlagPolicy.SecureOn] when enabled and [SecureFlagPolicy.SecureOff] when disabled.
 */
val SecureDialogProperties: DialogProperties
    get() = DialogProperties(
        securePolicy = if (ScreenProtectionConfig.isScreenProtectionEnabled) {
            SecureFlagPolicy.SecureOn
        } else {
            SecureFlagPolicy.SecureOff
        }
    )

/**
 * Full-screen DialogProperties governed by [ScreenProtectionConfig.isScreenProtectionEnabled].
 * Resolves to [SecureFlagPolicy.SecureOn] when enabled and [SecureFlagPolicy.SecureOff] when disabled.
 */
val SecureFullScreenDialogProperties: DialogProperties
    get() = DialogProperties(
        usePlatformDefaultWidth = false,
        securePolicy = if (ScreenProtectionConfig.isScreenProtectionEnabled) {
            SecureFlagPolicy.SecureOn
        } else {
            SecureFlagPolicy.SecureOff
        }
    )

/**
 * Synchronizes FLAG_SECURE on the enclosing Compose Dialog / AlertDialog window with
 * [ScreenProtectionConfig.isScreenProtectionEnabled].
 * When enabled, applies FLAG_SECURE and never clears it on dispose; when disabled, ensures
 * FLAG_SECURE remains off across dialog surfaces.
 */
@Composable
fun ApplySecureDialogWindow() {
    val dialogView = LocalView.current
    SideEffect {
        var currentParent: ViewParent? = dialogView.parent
        while (currentParent != null) {
            if (currentParent is DialogWindowProvider) {
                ScreenProtectionConfig.applyToWindow(currentParent.window)
                break
            }
            currentParent = currentParent.parent
        }
    }
}

@Composable
fun PhotoLightboxDialog(
    photos: List<GalleryMediaEntity>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onPlayYouTubeVideo: ((url: String, title: String?) -> Unit)? = null,
    onOpenExternalYouTube: ((String) -> Unit)? = null,
    youtubeLifecycleHooks: YouTubePlayerLifecycleHooks? = null,
    customThumbnailUriProvider: ((GalleryMediaEntity) -> String?)? = null
) {
    if (photos.isEmpty() || currentIndex !in photos.indices) return
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val currentPhoto = photos[currentIndex]
    val isYouTube = isGalleryYouTubeMedia(currentPhoto)
    val parsedYouTube = remember(currentPhoto.mediaUri, isYouTube) {
        if (isYouTube) YouTubeMediaHelper.parse(currentPhoto.mediaUri) else null
    }
    val customThumbUri = remember(currentPhoto, customThumbnailUriProvider) {
        customThumbnailUriProvider?.invoke(currentPhoto)
    }
    val youtubeThumbCandidates = remember(currentPhoto.mediaUri, currentPhoto.mediaType, customThumbUri) {
        resolveGalleryYouTubeThumbnailCandidates(currentPhoto, customThumbUri)
    }
    var youtubeThumbIndex by remember(currentPhoto.mediaUri, customThumbUri) { mutableIntStateOf(0) }
    var imageLoadFailed by remember(currentPhoto.mediaUri) { mutableStateOf(false) }
    var activeInlinePlayback by remember { mutableStateOf<Pair<String, String?>?>(null) }

    // Render only a single player dialog at a time to avoid duplicate playback or lifecycle observers
    activeInlinePlayback?.let { (playbackUrl, playbackTitle) ->
        YouTubeVideoPlayerDialog(
            youtubeUrl = playbackUrl,
            title = playbackTitle,
            onDismiss = { activeInlinePlayback = null },
            onOpenExternal = onOpenExternalYouTube,
            lifecycleHooks = youtubeLifecycleHooks
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = SecureFullScreenDialogProperties
    ) {
        ApplySecureDialogWindow()
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .safeDrawingPadding()
                .testTag("gallery_lightbox_dialog")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentIndex + 1} / ${photos.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("lightbox_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.closeLabel,
                        tint = Color.White
                    )
                }
            }

            if (isYouTube) {
                val resolvedPlaybackUrl = parsedYouTube?.canonicalWatchUrl ?: currentPhoto.mediaUri
                val resolvedExternalUrl = parsedYouTube?.externalFallbackUrl ?: currentPhoto.mediaUri
                val activeThumbUrl = youtubeThumbCandidates.getOrNull(youtubeThumbIndex)
                val triggerInAppPlayback = {
                    if (onPlayYouTubeVideo != null) {
                        onPlayYouTubeVideo(resolvedPlaybackUrl, currentPhoto.title)
                    } else {
                        activeInlinePlayback = resolvedPlaybackUrl to currentPhoto.title
                    }
                }
                val triggerExternalFallback = {
                    if (onOpenExternalYouTube != null) {
                        onOpenExternalYouTube(resolvedExternalUrl)
                    } else {
                        launchExternalUrl(context, resolvedExternalUrl)
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, brand.royalGold),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = if (photos.size > 1) 56.dp else 24.dp)
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .testTag("gallery_lightbox_youtube_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button, onClick = triggerInAppPlayback)
                                .testTag("lightbox_youtube_thumbnail_container")
                        ) {
                            if (!activeThumbUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(activeThumbUrl)
                                        .crossfade(false)
                                        .build(),
                                    contentDescription = currentPhoto.title ?: strings.youtubeVideoLabel,
                                    contentScale = ContentScale.Crop,
                                    onError = { youtubeThumbIndex += 1 },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("lightbox_youtube_thumbnail")
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    brand.burgundy,
                                                    Color(0xFF470C12),
                                                    Color(0xFF1F0609)
                                                )
                                            )
                                        )
                                        .testTag("lightbox_youtube_thumbnail_fallback")
                                )
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
                                        text = if (parsedYouTube?.isShort == true) {
                                            strings.socialYoutubeShortLabel
                                        } else {
                                            strings.youtubeVideoLabel
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                }
                            }

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
                        }

                        Text(
                            text = currentPhoto.title?.takeIf { it.isNotBlank() } ?: strings.titleGallery,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = triggerInAppPlayback,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brand.burgundy,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("lightbox_play_youtube_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(strings.socialPlayYouTubeVideoDesc)
                        }

                        OutlinedButton(
                            onClick = triggerExternalFallback,
                            border = BorderStroke(1.dp, brand.burgundy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("lightbox_open_youtube_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = strings.openYouTubeVideoLabel,
                                tint = brand.burgundy,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = strings.openYouTubeVideoLabel,
                                color = brand.burgundy
                            )
                        }
                    }
                }
            } else if (currentPhoto.mediaUri.isBlank() || imageLoadFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = strings.mediaUnavailableLabel,
                        tint = brand.royalGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = strings.mediaUnavailableLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                AsyncImage(
                    model = currentPhoto.mediaUri,
                    contentDescription = currentPhoto.caption ?: currentPhoto.title ?: strings.titleGallery,
                    contentScale = ContentScale.Fit,
                    onError = { imageLoadFailed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 72.dp)
                        .align(Alignment.Center)
                )
            }

            if (photos.size > 1) {
                IconButton(
                    onClick = {
                        val prev = if (currentIndex == 0) photos.lastIndex else currentIndex - 1
                        onIndexChange(prev)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("lightbox_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = strings.previousPhotoDesc,
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        val next = if (currentIndex == photos.lastIndex) 0 else currentIndex + 1
                        onIndexChange(next)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("lightbox_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.nextPhotoDesc,
                        tint = Color.White
                    )
                }
            }

            if (!currentPhoto.title.isNullOrBlank() || !currentPhoto.caption.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        currentPhoto.title?.takeIf { it.isNotBlank() }?.let { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        currentPhoto.caption?.takeIf { it.isNotBlank() }?.let { caption ->
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsDetailDialog(
    newsItem: NewsItemEntity,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = SecureDialogProperties
    ) {
        ApplySecureDialogWindow()
        BackHandler(onBack = onDismiss)
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .testTag("news_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (newsItem.isFeatured) strings.featuredNewsBadge else strings.tabNews,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = newsItem.publishedDateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = newsItem.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ManagedPublicImage(
                    imageUri = newsItem.imageUri,
                    contentDescription = newsItem.title,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("news_detail_image")
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = newsItem.fullBody.ifBlank { newsItem.summary },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("news_detail_close_button")
                    ) {
                        Text(strings.closeLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementDetailDialog(
    announcement: AnnouncementEntity,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = SecureDialogProperties
    ) {
        ApplySecureDialogWindow()
        BackHandler(onBack = onDismiss)
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, brand.burgundy.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .testTag("announcement_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = brand.burgundy.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = announcement.priorityLabel ?: strings.tabAnnouncements,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = brand.burgundy,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = announcement.issuedDateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ManagedPublicImage(
                    imageUri = announcement.imageUri,
                    contentDescription = announcement.title,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("announcement_detail_image")
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("announcement_detail_close_button")
                    ) {
                        Text(strings.closeLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun ManagedPublicImage(
    imageUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val cleanUri = imageUri?.trim()?.takeIf { it.isNotEmpty() } ?: return
    val context = LocalContext.current
    val mediaManager = remember(context) {
        (context.applicationContext as? ChevalierApplication)?.container?.mediaStudioManager
            ?: MediaStudioManager(context)
    }
    val initialReadable = remember(cleanUri) {
        val parsed = runCatching { Uri.parse(cleanUri) }.getOrNull()
        if (parsed?.scheme == "file" || parsed?.scheme == null) {
            mediaManager.isManagedMediaFileReadable(cleanUri)
        } else {
            true
        }
    }
    val isReadable by produceState(initialValue = initialReadable, cleanUri) {
        value = withContext(Dispatchers.IO) {
            val parsed = runCatching { Uri.parse(cleanUri) }.getOrNull()
            if (parsed?.scheme == "file" || parsed?.scheme == null) {
                mediaManager.isManagedMediaFileReadable(cleanUri)
            } else {
                true
            }
        }
    }
    var decodeFailed by remember(cleanUri) { mutableStateOf(false) }
    if (isReadable && !decodeFailed) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(cleanUri)
                .size(960)
                .crossfade(false)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            onError = { decodeFailed = true },
            modifier = modifier
        )
    }
}

fun launchDialerIntent(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Gracefully ignore when no dialer app is installed
    }
}

fun launchEmailIntent(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(email)}"))
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Gracefully ignore when no email client is installed
    }
}

fun launchMapsIntent(context: Context, query: String) {
    try {
        val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, geoUri)
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        launchExternalUrl(context, "https://maps.google.com/?q=${Uri.encode(query)}")
    }
}

fun launchExternalUrl(context: Context, url: String) {
    try {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
    } catch (_: ActivityNotFoundException) {
        // Gracefully ignore when no browser is available
    }
}
