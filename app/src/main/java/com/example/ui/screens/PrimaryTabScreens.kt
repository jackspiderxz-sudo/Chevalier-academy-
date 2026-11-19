package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.net.Uri
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.media.MediaStudioManager
import com.example.ui.components.AnnouncementDetailDialog
import com.example.ui.components.AwaitingVerificationCard
import com.example.ui.components.GalleryMediaTileContent
import com.example.ui.components.HomeHeroBannerPreviewFrame
import com.example.ui.components.ManagedPublicImage
import com.example.ui.components.NewsDetailDialog
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.SchoolBrandHeaderCard
import com.example.ui.components.SectionHeaderText
import com.example.ui.components.VerifiedFieldRow
import com.example.ui.components.isGalleryYouTubeMedia
import com.example.ui.components.launchDialerIntent
import com.example.ui.components.launchEmailIntent
import com.example.ui.components.launchExternalUrl
import com.example.ui.components.launchMapsIntent
import com.example.ui.navigation.AppRoute
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    profile: SchoolProfileEntity,
    heroPhotos: List<CampusHeroPhotoEntity>,
    newsItems: List<NewsItemEntity>,
    announcements: List<AnnouncementEntity>,
    galleryMedia: List<GalleryMediaEntity>,
    socialLinks: List<SocialMediaLinkEntity>,
    onNavigate: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current

    var selectedNews by remember { mutableStateOf<NewsItemEntity?>(null) }
    var selectedAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }
    var lightboxStartIndex by remember { mutableStateOf<Int?>(null) }

    selectedNews?.let { item ->
        NewsDetailDialog(newsItem = item, onDismiss = { selectedNews = null })
    }
    selectedAnnouncement?.let { item ->
        AnnouncementDetailDialog(announcement = item, onDismiss = { selectedAnnouncement = null })
    }
    lightboxStartIndex?.let { idx ->
        PhotoLightboxDialog(
            photos = galleryMedia,
            currentIndex = idx,
            onIndexChange = { lightboxStartIndex = it },
            onDismiss = { lightboxStartIndex = null }
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
            // SECTION 1 — SCHOOL HERO BANNER
            item {
                HomeSchoolHeroSection(
                    profile = profile,
                    heroPhotos = heroPhotos
                )
            }

            // SECTION 2 — WELCOME / ABOUT
            item {
                SectionHeaderText(title = strings.welcomeSectionTitle)
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_section_welcome"),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        ManagedPublicImage(
                            imageUri = profile.profileImageUri,
                            contentDescription = profile.shortName,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("home_welcome_profile_image")
                        )
                        val intro = profile.welcomeIntroduction
                        if (!intro.isNullOrBlank()) {
                            Text(
                                text = intro,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = brand.royalGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = strings.awaitingVerificationLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.welcomePendingDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onNavigate(AppRoute.ABOUT) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("home_welcome_read_more"),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, brand.burgundy)
                        ) {
                            Text(text = strings.readMore, color = brand.burgundy)
                            Spacer(modifier = Modifier.size(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = brand.burgundy,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // SECTION 3 — QUICK ACCESS CARDS (Exactly 4: Academics, Courses, Facilities, Transport)
            item {
                SectionHeaderText(title = strings.quickAccessTitle)
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("home_section_quick_access")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessCard(
                            title = strings.quickCardAcademicsTitle,
                            description = strings.quickCardAcademicsDesc,
                            icon = Icons.Outlined.School,
                            accentColor = brand.burgundy,
                            onClick = { onNavigate(AppRoute.ACADEMICS) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_quick_academics")
                        )
                        QuickAccessCard(
                            title = strings.quickCardCoursesTitle,
                            description = strings.quickCardCoursesDesc,
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            accentColor = brand.forestGreen,
                            onClick = { onNavigate(AppRoute.COURSES) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_quick_courses")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessCard(
                            title = strings.quickCardFacilitiesTitle,
                            description = strings.quickCardFacilitiesDesc,
                            icon = Icons.Outlined.Apartment,
                            accentColor = brand.goldAccentText,
                            onClick = { onNavigate(AppRoute.FACILITIES) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_quick_facilities")
                        )
                        QuickAccessCard(
                            title = strings.quickCardTransportTitle,
                            description = strings.quickCardTransportDesc,
                            icon = Icons.Outlined.DirectionsBus,
                            accentColor = brand.burgundy,
                            onClick = { onNavigate(AppRoute.TRANSPORT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_quick_transport")
                        )
                    }
                }
            }

            // SECTION 4 — LATEST NEWS & ANNOUNCEMENTS
            item {
                SectionHeaderText(
                    title = strings.latestUpdatesTitle,
                    actionLabel = strings.viewAll,
                    onActionClick = { onNavigate(AppRoute.NEWS_ANNOUNCEMENTS) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("home_section_updates")
                ) {
                    if (newsItems.isEmpty() && announcements.isEmpty()) {
                        AwaitingVerificationCard(
                            title = strings.latestUpdatesTitle,
                            description = strings.announcementsPendingDesc,
                            icon = Icons.Outlined.Campaign
                        )
                    } else {
                        val featuredNews = newsItems.firstOrNull { it.isFeatured } ?: newsItems.firstOrNull()
                        if (featuredNews != null) {
                            FeaturedNewsHomeCard(
                                item = featuredNews,
                                onClick = { selectedNews = featuredNews }
                            )
                        }
                        val remainingNews = newsItems.filter { it.id != featuredNews?.id }.take(2)
                        remainingNews.forEach { item ->
                            CompactNewsHomeCard(
                                item = item,
                                onClick = { selectedNews = item }
                            )
                        }
                        announcements.take(2).forEach { ann ->
                            CompactAnnouncementHomeCard(
                                item = ann,
                                onClick = { selectedAnnouncement = ann }
                            )
                        }
                    }
                }
            }

            // SECTION 5 — GALLERY PREVIEW
            item {
                SectionHeaderText(
                    title = strings.galleryPreviewTitle,
                    actionLabel = strings.viewAll,
                    onActionClick = { onNavigate(AppRoute.GALLERY) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.testTag("home_section_gallery")) {
                    if (galleryMedia.isEmpty()) {
                        AwaitingVerificationCard(
                            title = strings.galleryPreviewTitle,
                            description = strings.galleryPendingDesc,
                            icon = Icons.Outlined.PhotoLibrary
                        )
                    } else {
                        GalleryPreviewSection(
                            media = galleryMedia,
                            onPhotoClick = { index -> lightboxStartIndex = index }
                        )
                    }
                }
            }

            // SECTION 6 — CONTACT & SOCIAL MEDIA
            item {
                SectionHeaderText(
                    title = strings.contactSocialTitle,
                    actionLabel = strings.viewAll,
                    onActionClick = { onNavigate(AppRoute.CONTACT) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                HomeContactSocialSection(
                    profile = profile,
                    socialLinks = socialLinks,
                    onOpenMaps = {
                        val query = profile.mapCoordinatesOrQuery ?: profile.addressLine
                        launchMapsIntent(context, query)
                    },
                    onDialPhone = { phone -> launchDialerIntent(context, phone) },
                    onSendEmail = { email -> launchEmailIntent(context, email) },
                    onOpenSocial = { url -> launchExternalUrl(context, url) },
                    onOpenAllSocial = { onNavigate(AppRoute.SOCIAL_MEDIA) }
                )
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
private fun HomeSchoolHeroSection(
    profile: SchoolProfileEntity,
    heroPhotos: List<CampusHeroPhotoEntity>
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_hero"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Container 1 — Campus Hero Banner (16:9 responsive frame matching Admin Live Preview)
        if (heroPhotos.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { heroPhotos.size })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_campus_hero_banner")
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_campus_pager")
                ) { page ->
                    val photo = heroPhotos[page]
                    val context = LocalContext.current
                    var decodeFailed by remember(photo.imageUri) { mutableStateOf(false) }
                    HomeHeroBannerPreviewFrame(caption = photo.caption) {
                        if (!decodeFailed) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photo.imageUri)
                                    .size(1280)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = photo.caption ?: profile.schoolName,
                                contentScale = ContentScale.Crop,
                                onError = { decodeFailed = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(brand.burgundy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = photo.caption ?: profile.schoolName,
                                    tint = brand.royalGold,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                if (heroPhotos.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(
                                Color.Black.copy(alpha = 0.45f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("hero_pager_indicators"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(heroPhotos.size) { index ->
                            val active = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (active) 9.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) brand.royalGold else Color.White.copy(alpha = 0.65f)
                                    )
                            )
                        }
                    }
                }
            }
        } else {
            // Branded non-photo architectural banner (never uses logo/splash as a fake campus photo)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .testTag("home_campus_hero_banner"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, brand.royalGold.copy(alpha = 0.7f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    brand.burgundy,
                                    Color(0xFF6B141C),
                                    Color(0xFF0A5230)
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = brand.royalGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.7f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = null,
                                    tint = brand.royalGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = strings.heroBannerPendingLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = strings.heroBannerPendingDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeaturedNewsHomeCard(
    item: NewsItemEntity,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, brand.royalGold.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_featured_news_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = brand.burgundy,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = strings.featuredNewsBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = item.publishedDateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ManagedPublicImage(
                imageUri = item.imageUri,
                contentDescription = item.title,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("home_featured_news_image")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactNewsHomeCard(
    item: NewsItemEntity,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        text = strings.tabNews,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = item.publishedDateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ManagedPublicImage(
                imageUri = item.imageUri,
                contentDescription = item.title,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("home_compact_news_image_${item.id}")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactAnnouncementHomeCard(
    item: AnnouncementEntity,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, brand.burgundy.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = brand.burgundy.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.priorityLabel ?: strings.tabAnnouncements,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = brand.burgundy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = item.issuedDateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ManagedPublicImage(
                imageUri = item.imageUri,
                contentDescription = item.title,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("home_compact_announcement_image_${item.id}")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GalleryPreviewSection(
    media: List<GalleryMediaEntity>,
    onPhotoClick: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val featured = media.first()
    val thumbnails = media.drop(1).take(3)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            onClick = { onPhotoClick(0) },
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .testTag("home_gallery_featured_photo")
        ) {
            GalleryMediaTileContent(
                media = featured,
                fallbackContentDescription = strings.titleGallery,
                showCaptionOverlay = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (thumbnails.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                thumbnails.forEachIndexed { offset, thumb ->
                    Card(
                        onClick = { onPhotoClick(offset + 1) },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        GalleryMediaTileContent(
                            media = thumb,
                            fallbackContentDescription = strings.titleGallery,
                            showCaptionOverlay = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                repeat(3 - thumbnails.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeContactSocialSection(
    profile: SchoolProfileEntity,
    socialLinks: List<SocialMediaLinkEntity>,
    onOpenMaps: () -> Unit,
    onDialPhone: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    onOpenSocial: (String) -> Unit,
    onOpenAllSocial: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_contact_social"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            VerifiedFieldRow(
                label = strings.addressLabel,
                value = profile.addressLine,
                icon = Icons.Outlined.LocationOn
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.phoneLabel,
                value = profile.phone,
                icon = Icons.Outlined.Phone
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.emailLabel,
                value = profile.email,
                icon = Icons.Outlined.Email
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMaps,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_open_maps_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, brand.forestGreen)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = strings.openMapsActionLabel,
                        tint = brand.forestGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = strings.openMapsActionLabel,
                        color = brand.forestGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!profile.phone.isNullOrBlank()) {
                    Button(
                        onClick = { onDialPhone(profile.phone) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("home_call_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = brand.burgundy),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = strings.callActionLabel,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(strings.callActionLabel)
                    }
                }
            }

            if (!profile.email.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onSendEmail(profile.email) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_email_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = strings.emailActionLabel,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.emailActionLabel)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.officialChannelsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onOpenAllSocial) {
                    Text(strings.viewAll)
                }
            }

            if (socialLinks.isEmpty()) {
                Surface(
                    color = brand.royalGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = strings.awaitingVerificationLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(socialLinks, key = { it.id }) { link ->
                        OutlinedButton(
                            onClick = { onOpenSocial(link.url) },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(link.platformName)
                            Spacer(modifier = Modifier.size(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = strings.openLink,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicsScreen(
    academicInfo: List<AcademicInfoEntity>,
    onOpenCourses: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("academics_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = brand.burgundy.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.School,
                                        contentDescription = strings.academicOverviewTitle,
                                        tint = brand.burgundy
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = strings.officialSchoolName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = brand.goldAccentText
                                )
                                Text(
                                    text = strings.academicOverviewTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onOpenCourses,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("academics_open_courses_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brand.burgundy,
                                contentColor = Color.White
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = strings.exploreHigherSecondaryBtn)
                        }
                    }
                }
            }

            if (academicInfo.isEmpty()) {
                item {
                    AwaitingVerificationCard(
                        title = strings.titleAcademics,
                        description = strings.academicPendingDesc,
                        icon = Icons.AutoMirrored.Outlined.MenuBook
                    )
                }
            } else {
                items(academicInfo, key = { it.id }) { info ->
                    AcademicInfoItemCard(info = info)
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
internal fun AcademicInfoItemCard(
    info: AcademicInfoEntity,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = info.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GalleryScreen(
    albums: List<GalleryAlbumWithMedia>
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var lightboxIndex by remember { mutableStateOf<Int?>(null) }

    val categories = remember(albums) {
        albums.map { it.album.category }.distinct()
    }
    val filteredAlbums = remember(albums, selectedCategory) {
        if (selectedCategory == null) albums else albums.filter { it.album.category == selectedCategory }
    }
    val filteredPhotos = remember(filteredAlbums) {
        filteredAlbums.flatMap { it.mediaItems }
    }

    lightboxIndex?.let { idx ->
        PhotoLightboxDialog(
            photos = filteredPhotos,
            currentIndex = idx,
            onIndexChange = { lightboxIndex = it },
            onDismiss = { lightboxIndex = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("gallery_screen")
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.allPhotosTab) },
                    modifier = Modifier.testTag("gallery_tab_media")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.albumsTab) },
                    modifier = Modifier.testTag("gallery_tab_albums")
                )
            }

            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(strings.allCategoriesLabel) }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    if (filteredPhotos.isEmpty()) {
                        item {
                            AwaitingVerificationCard(
                                title = strings.titleGallery,
                                description = strings.galleryPendingDesc,
                                icon = Icons.Outlined.PhotoLibrary
                            )
                        }
                    } else {
                        val rows = filteredPhotos.chunked(2)
                        items(rows) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { photo ->
                                    val photoIndex = filteredPhotos.indexOf(photo)
                                    val context = LocalContext.current
                                    val mediaManager = remember {
                                        (context.applicationContext as? ChevalierApplication)?.container?.mediaStudioManager
                                            ?: MediaStudioManager(context)
                                    }
                                    val isImageReadable by produceState(
                                        initialValue = isGalleryYouTubeMedia(photo),
                                        photo.mediaUri,
                                        photo.mediaType
                                    ) {
                                        value = if (isGalleryYouTubeMedia(photo)) {
                                            true
                                        } else {
                                            withContext(Dispatchers.IO) {
                                                val uri = runCatching { Uri.parse(photo.mediaUri) }.getOrNull()
                                                if (uri?.scheme == "file") {
                                                    mediaManager.isManagedMediaFileReadable(photo.mediaUri)
                                                } else {
                                                    photo.mediaUri.isNotBlank()
                                                }
                                            }
                                        }
                                    }
                                    Card(
                                        onClick = { lightboxIndex = photoIndex },
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .testTag("gallery_photo_item_${photo.id}")
                                    ) {
                                        GalleryMediaTileContent(
                                            media = photo,
                                            fallbackContentDescription = strings.titleGallery,
                                            showCaptionOverlay = true,
                                            isImageReadable = isImageReadable,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    if (filteredAlbums.isEmpty()) {
                        item {
                            AwaitingVerificationCard(
                                title = strings.albumsTab,
                                description = strings.galleryPendingDesc,
                                icon = Icons.Outlined.PhotoLibrary
                            )
                        }
                    } else {
                        items(filteredAlbums, key = { it.album.id }) { albumWithMedia ->
                            GalleryAlbumCard(
                                albumWithMedia = albumWithMedia,
                                onClick = {
                                    if (albumWithMedia.mediaItems.isNotEmpty()) {
                                        selectedCategory = albumWithMedia.album.category
                                        selectedTab = 0
                                    }
                                }
                            )
                        }
                    }
                }

                item { PoweredByJkLabsFooter() }
            }
        }
    }
}

@Composable
internal fun GalleryAlbumCard(
    albumWithMedia: GalleryAlbumWithMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val mediaManager = remember {
        (context.applicationContext as? ChevalierApplication)?.container?.mediaStudioManager
            ?: MediaStudioManager(context)
    }
    val rawCoverUri = albumWithMedia.album.coverMediaUri?.trim()?.takeIf {
        it.isNotEmpty() && !MediaStudioManager.isValidYouTubeUrl(it)
    }
    val isCoverReadable by produceState(initialValue = false, rawCoverUri) {
        value = if (rawCoverUri == null) {
            false
        } else {
            withContext(Dispatchers.IO) {
                val parsed = runCatching { Uri.parse(rawCoverUri) }.getOrNull()
                if (parsed?.scheme == "file") {
                    mediaManager.isManagedMediaFileReadable(rawCoverUri)
                } else {
                    parsed?.scheme?.lowercase() in setOf("https", "http", "content")
                }
            }
        }
    }
    var coverLoadFailed by remember(rawCoverUri) { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("gallery_album_cover_${albumWithMedia.album.id}")
            ) {
                if (rawCoverUri != null && isCoverReadable && !coverLoadFailed) {
                    AsyncImage(
                        model = rawCoverUri,
                        contentDescription = albumWithMedia.album.title,
                        contentScale = ContentScale.Crop,
                        onError = { coverLoadFailed = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = brand.burgundy,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = brand.royalGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = albumWithMedia.album.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = albumWithMedia.album.title,
                    style = MaterialTheme.typography.titleMedium
                )
                albumWithMedia.album.description?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "${albumWithMedia.mediaItems.size} ${strings.photosCountSuffix}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ContactScreen(
    profile: SchoolProfileEntity,
    socialLinks: List<SocialMediaLinkEntity>,
    onOpenSocialMedia: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("contact_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SchoolBrandHeaderCard(profile = profile)
            }

            item {
                ContactDetailsInfoCard(profile = profile)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val query = profile.mapCoordinatesOrQuery ?: profile.addressLine
                            launchMapsIntent(context, query)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("contact_open_maps_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.forestGreen,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = strings.openMapsActionLabel,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(strings.openMapsActionLabel)
                    }

                    if (!profile.phone.isNullOrBlank()) {
                        Button(
                            onClick = { launchDialerIntent(context, profile.phone) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_dial_phone_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brand.burgundy,
                                contentColor = Color.White
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = strings.callActionLabel,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("${strings.callActionLabel}: ${profile.phone}")
                        }
                    }

                    if (!profile.email.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { launchEmailIntent(context, profile.email) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_send_email_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = strings.emailActionLabel,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("${strings.emailActionLabel}: ${profile.email}")
                        }
                    }

                    if (!profile.website.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { launchExternalUrl(context, profile.website) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_open_website_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = strings.websiteLabel,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(profile.website)
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenSocialMedia,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("contact_social_media_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.2.dp, brand.burgundy)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            tint = brand.burgundy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (socialLinks.isEmpty()) {
                                strings.connectOnlineBtn
                            } else {
                                "${strings.connectOnlineBtn} (${socialLinks.size})"
                            },
                            color = brand.burgundy
                        )
                    }
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
internal fun ContactDetailsInfoCard(
    profile: SchoolProfileEntity,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            VerifiedFieldRow(
                label = strings.addressLabel,
                value = profile.addressLine,
                icon = Icons.Outlined.LocationOn
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.phoneLabel,
                value = profile.phone,
                icon = Icons.Outlined.Phone
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.emailLabel,
                value = profile.email,
                icon = Icons.Outlined.Email
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.websiteLabel,
                value = profile.website,
                icon = Icons.Outlined.Language
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.officeHoursLabel,
                value = profile.officeHours,
                icon = Icons.Outlined.AccessTime
            )
        }
    }
}

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("more_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeaderText(title = strings.schoolDirectorySection)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreMenuRowCard(
                        title = strings.titleAboutSchool,
                        subtitle = strings.subtitleAboutSchool,
                        icon = Icons.Outlined.Info,
                        accentColor = brand.burgundy,
                        onClick = { onNavigate(AppRoute.ABOUT) },
                        testTag = "more_nav_about"
                    )
                    MoreMenuRowCard(
                        title = strings.titleHigherSecondaryCourses,
                        subtitle = strings.subtitleCourses,
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        accentColor = brand.forestGreen,
                        onClick = { onNavigate(AppRoute.COURSES) },
                        testTag = "more_nav_courses"
                    )
                }
            }

            item {
                SectionHeaderText(title = strings.campusServicesSection)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreMenuRowCard(
                        title = strings.titleFacilities,
                        subtitle = strings.subtitleFacilities,
                        icon = Icons.Outlined.Apartment,
                        accentColor = brand.forestGreen,
                        onClick = { onNavigate(AppRoute.FACILITIES) },
                        testTag = "more_nav_facilities"
                    )
                    MoreMenuRowCard(
                        title = strings.titleTransport,
                        subtitle = strings.subtitleTransport,
                        icon = Icons.Outlined.DirectionsBus,
                        accentColor = brand.goldAccentText,
                        onClick = { onNavigate(AppRoute.TRANSPORT) },
                        testTag = "more_nav_transport"
                    )
                }
            }

            item {
                SectionHeaderText(title = strings.updatesSectionTitle)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreMenuRowCard(
                        title = strings.titleNews,
                        subtitle = strings.subtitleNews,
                        icon = Icons.Outlined.Newspaper,
                        accentColor = brand.burgundy,
                        onClick = { onNavigate(AppRoute.NEWS) },
                        testTag = "more_nav_news"
                    )
                    MoreMenuRowCard(
                        title = strings.titleAnnouncements,
                        subtitle = strings.subtitleAnnouncements,
                        icon = Icons.Outlined.Campaign,
                        accentColor = brand.burgundy,
                        onClick = { onNavigate(AppRoute.ANNOUNCEMENTS) },
                        testTag = "more_nav_announcements"
                    )
                    MoreMenuRowCard(
                        title = strings.titleSocialMedia,
                        subtitle = strings.subtitleSocialMedia,
                        icon = Icons.Outlined.Share,
                        accentColor = brand.forestGreen,
                        onClick = { onNavigate(AppRoute.SOCIAL_MEDIA) },
                        testTag = "more_nav_social"
                    )
                }
            }

            item {
                SectionHeaderText(title = strings.appPreferencesSection)
                Spacer(modifier = Modifier.height(10.dp))
                MoreMenuRowCard(
                    title = strings.titleSettings,
                    subtitle = strings.subtitleSettings,
                    icon = Icons.Outlined.Settings,
                    accentColor = brand.burgundy,
                    onClick = { onNavigate(AppRoute.SETTINGS) },
                    testTag = "more_nav_settings"
                )
            }

            item {
                SectionHeaderText(title = strings.managementSectionTitle)
                Spacer(modifier = Modifier.height(10.dp))
                MoreMenuRowCard(
                    title = strings.managementNoticeTitle,
                    subtitle = strings.managementNoticeDesc,
                    icon = Icons.Outlined.Security,
                    accentColor = brand.burgundy,
                    onClick = { onNavigate(AppRoute.MANAGEMENT_ENTRY) },
                    testTag = "more_nav_management"
                )
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
private fun MoreMenuRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
