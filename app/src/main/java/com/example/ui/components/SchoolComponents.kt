package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.media.MediaStudioManager
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
    onGalleryClick: (() -> Unit)? = null
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
                modifier = Modifier.padding(end = if (showSettingsAction || onGalleryClick != null) 4.dp else 12.dp)
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
            if (onGalleryClick != null) {
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
    media.mediaType.equals("YOUTUBE", ignoreCase = true)

@Composable
fun GalleryMediaTileContent(
    media: GalleryMediaEntity,
    fallbackContentDescription: String,
    modifier: Modifier = Modifier,
    showCaptionOverlay: Boolean = true,
    isImageReadable: Boolean = true
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val isYouTube = isGalleryYouTubeMedia(media)
    var imageLoadFailed by remember(media.mediaUri) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isYouTube) {
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
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = brand.royalGold.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, brand.royalGold),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = media.title ?: strings.youtubeVideoLabel,
                                tint = brand.royalGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = strings.youtubeVideoLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    media.title?.takeIf { it.isNotBlank() }?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
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

        if (showCaptionOverlay) {
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

@Composable
fun PhotoLightboxDialog(
    photos: List<GalleryMediaEntity>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty() || currentIndex !in photos.indices) return
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val currentPhoto = photos[currentIndex]
    val isYouTube = isGalleryYouTubeMedia(currentPhoto)
    var imageLoadFailed by remember(currentPhoto.mediaUri) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, brand.royalGold),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = if (photos.size > 1) 56.dp else 24.dp)
                        .widthIn(max = 440.dp)
                        .fillMaxWidth()
                        .testTag("gallery_lightbox_youtube_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = brand.burgundy.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, brand.royalGold),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayCircle,
                                    contentDescription = strings.youtubeVideoLabel,
                                    tint = brand.burgundy,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Surface(
                            color = brand.royalGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = strings.youtubeVideoLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = currentPhoto.title?.takeIf { it.isNotBlank() } ?: strings.titleGallery,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentPhoto.mediaUri,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = { launchExternalUrl(context, currentPhoto.mediaUri) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brand.burgundy,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lightbox_open_youtube_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = strings.openYouTubeVideoLabel,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(strings.openYouTubeVideoLabel)
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
    Dialog(onDismissRequest = onDismiss) {
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
    Dialog(onDismissRequest = onDismiss) {
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
