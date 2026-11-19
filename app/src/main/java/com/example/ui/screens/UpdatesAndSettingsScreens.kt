package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.ui.components.AnnouncementDetailDialog
import com.example.ui.components.AwaitingVerificationCard
import com.example.ui.components.ManagedPublicImage
import com.example.ui.components.NewsDetailDialog
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.SectionHeaderText
import com.example.ui.components.launchExternalUrl
import com.example.ui.theme.LocalBrandPalette

@Composable
fun NewsScreen(
    newsItems: List<NewsItemEntity>
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    var selectedNews by remember { mutableStateOf<NewsItemEntity?>(null) }

    selectedNews?.let { item ->
        NewsDetailDialog(newsItem = item, onDismiss = { selectedNews = null })
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("news_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (newsItems.isEmpty()) {
                item {
                    AwaitingVerificationCard(
                        title = strings.titleNews,
                        description = strings.newsPendingDesc,
                        icon = Icons.Outlined.Newspaper
                    )
                }
            } else {
                val featured = newsItems.firstOrNull { it.isFeatured }
                if (featured != null) {
                    item {
                        NewsItemCard(
                            item = featured,
                            badgeText = strings.featuredNewsBadge,
                            highlighted = true,
                            onClick = { selectedNews = featured }
                        )
                    }
                }
                items(newsItems.filter { it.id != featured?.id }, key = { it.id }) { news ->
                    NewsItemCard(
                        item = news,
                        badgeText = strings.tabNews,
                        highlighted = false,
                        onClick = { selectedNews = news }
                    )
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
fun AnnouncementsScreen(
    announcements: List<AnnouncementEntity>
) {
    val strings = LocalAppStrings.current
    var selectedAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }

    selectedAnnouncement?.let { item ->
        AnnouncementDetailDialog(announcement = item, onDismiss = { selectedAnnouncement = null })
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("announcements_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (announcements.isEmpty()) {
                item {
                    AwaitingVerificationCard(
                        title = strings.titleAnnouncements,
                        description = strings.announcementsPendingDesc,
                        icon = Icons.Outlined.Campaign
                    )
                }
            } else {
                items(announcements, key = { it.id }) { ann ->
                    AnnouncementItemCard(
                        item = ann,
                        onClick = { selectedAnnouncement = ann }
                    )
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
fun NewsAnnouncementsScreen(
    newsItems: List<NewsItemEntity>,
    announcements: List<AnnouncementEntity>
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedNews by remember { mutableStateOf<NewsItemEntity?>(null) }
    var selectedAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }

    selectedNews?.let { item ->
        NewsDetailDialog(newsItem = item, onDismiss = { selectedNews = null })
    }
    selectedAnnouncement?.let { item ->
        AnnouncementDetailDialog(announcement = item, onDismiss = { selectedAnnouncement = null })
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("news_announcements_screen")
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.tabAllUpdates) },
                    modifier = Modifier.testTag("updates_tab_all")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.tabNews) },
                    modifier = Modifier.testTag("updates_tab_news")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(strings.tabAnnouncements) },
                    modifier = Modifier.testTag("updates_tab_announcements")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val showNews = selectedTab == 0 || selectedTab == 1
                val showAnnouncements = selectedTab == 0 || selectedTab == 2

                val hasVisibleItems =
                    (showNews && newsItems.isNotEmpty()) ||
                        (showAnnouncements && announcements.isNotEmpty())

                if (!hasVisibleItems) {
                    item {
                        val (title, desc, icon) = when (selectedTab) {
                            1 -> Triple(strings.tabNews, strings.newsPendingDesc, Icons.Outlined.Newspaper)
                            2 -> Triple(strings.tabAnnouncements, strings.announcementsPendingDesc, Icons.Outlined.Campaign)
                            else -> Triple(strings.titleNewsAnnouncements, strings.announcementsPendingDesc, Icons.Outlined.Campaign)
                        }
                        AwaitingVerificationCard(
                            title = title,
                            description = desc,
                            icon = icon
                        )
                    }
                } else {
                    if (showAnnouncements) {
                        items(announcements, key = { "ann_${it.id}" }) { ann ->
                            AnnouncementItemCard(
                                item = ann,
                                onClick = { selectedAnnouncement = ann }
                            )
                        }
                    }

                    if (showNews) {
                        items(newsItems, key = { "news_${it.id}" }) { news ->
                            NewsItemCard(
                                item = news,
                                badgeText = if (news.isFeatured) strings.featuredNewsBadge else strings.tabNews,
                                highlighted = news.isFeatured,
                                onClick = { selectedNews = news }
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
internal fun NewsItemCard(
    item: NewsItemEntity,
    badgeText: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    showImage: Boolean = true
) {
    val brand = LocalBrandPalette.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (highlighted) 1.2.dp else 1.dp,
            color = if (highlighted) brand.royalGold.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("news_item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (highlighted) brand.burgundy else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (highlighted) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = item.publishedDateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showImage) {
                ManagedPublicImage(
                    imageUri = item.imageUri,
                    contentDescription = item.title,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("news_item_image_${item.id}")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
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
internal fun AnnouncementItemCard(
    item: AnnouncementEntity,
    onClick: () -> Unit,
    showImage: Boolean = true
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, brand.burgundy.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("announcement_item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = item.priorityLabel ?: strings.tabAnnouncements,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = brand.burgundy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = item.issuedDateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showImage) {
                ManagedPublicImage(
                    imageUri = item.imageUri,
                    contentDescription = item.title,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("announcement_item_image_${item.id}")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal val officialSocialPlatforms = listOf("Instagram", "Facebook", "YouTube", "X")

@Composable
fun SocialMediaScreen(
    links: List<SocialMediaLinkEntity>
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("social_media_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(officialSocialPlatforms) { platform ->
                val matched = links.firstOrNull {
                    it.platformName.equals(platform, ignoreCase = true) ||
                        (platform == "X" && it.platformName.equals("Twitter", ignoreCase = true))
                }
                SocialPlatformSlotCard(
                    platformName = platform,
                    verifiedLink = matched,
                    onOpenLink = { url -> launchExternalUrl(context, url) }
                )
            }

            val extraLinks = links.filter { link ->
                officialSocialPlatforms.none { platform ->
                    link.platformName.equals(platform, ignoreCase = true) ||
                        (platform == "X" && link.platformName.equals("Twitter", ignoreCase = true))
                }
            }
            items(extraLinks, key = { it.id }) { link ->
                SocialPlatformSlotCard(
                    platformName = link.platformName,
                    verifiedLink = link,
                    onOpenLink = { url -> launchExternalUrl(context, url) }
                )
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
internal fun SocialPlatformSlotCard(
    platformName: String,
    verifiedLink: SocialMediaLinkEntity?,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val isVerified = verifiedLink != null

    Card(
        onClick = { if (verifiedLink != null) onOpenLink(verifiedLink.url) },
        enabled = isVerified,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isVerified) brand.forestGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("social_slot_${platformName.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isVerified) brand.forestGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (platformName == "X") Icons.Outlined.Tag else Icons.Outlined.Public,
                        contentDescription = platformName,
                        tint = if (isVerified) brand.onForestGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = platformName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                if (verifiedLink != null) {
                    Text(
                        text = verifiedLink.handleOrLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.forestGreen
                    )
                } else {
                    Surface(
                        color = brand.royalGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = strings.awaitingVerificationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            if (isVerified) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = strings.openLink,
                    tint = brand.burgundy
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onOpenLanguageSelection: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SectionHeaderText(title = strings.languageSettingTitle)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.languageSettingDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        AppLanguage.entries.forEachIndexed { index, language ->
                            val selected = language == currentLanguage
                            SettingsRadioRow(
                                title = language.nativeLabel,
                                subtitle = language.englishLabel,
                                icon = Icons.Outlined.Translate,
                                selected = selected,
                                onClick = { onLanguageSelected(language) },
                                testTag = "settings_language_${language.code}"
                            )
                            if (index < AppLanguage.entries.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
                if (onOpenLanguageSelection != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenLanguageSelection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_change_language_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, brand.burgundy)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = brand.burgundy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.changeLanguageButton,
                            color = brand.burgundy
                        )
                    }
                }
            }

            item {
                SectionHeaderText(title = strings.themeSettingTitle)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.themeSettingDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        val options = listOf(
                            Triple(AppThemeMode.SYSTEM, strings.themeSystemDefault, Icons.Outlined.PhoneAndroid),
                            Triple(AppThemeMode.LIGHT, strings.themeLightMode, Icons.Outlined.LightMode),
                            Triple(AppThemeMode.DARK, strings.themeDarkMode, Icons.Outlined.DarkMode)
                        )
                        options.forEachIndexed { index, (mode, label, icon) ->
                            SettingsRadioRow(
                                title = label,
                                subtitle = null,
                                icon = icon,
                                selected = mode == currentThemeMode,
                                onClick = { onThemeModeSelected(mode) },
                                testTag = "settings_theme_${mode.storageKey}"
                            )
                            if (index < options.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeaderText(title = strings.appInfoSectionTitle)
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = strings.appVersionLabel,
                                tint = brand.burgundy,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = strings.appVersionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${strings.shortSchoolName} v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = strings.storageStatusLabel,
                                tint = brand.forestGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = strings.storageStatusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strings.storageStatusValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = strings.demoNoticeLabel,
                                tint = brand.goldAccentText,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = strings.demoNoticeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strings.demoNoticeValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
fun ManagementLoginScreen(
    onNavigateBackToPublic: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    BackHandler(onBack = onNavigateBackToPublic)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("management_entry_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBackToPublic,
                        modifier = Modifier.testTag("management_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backContentDescription
                        )
                    }
                    Text(
                        text = strings.titleManagementEntry,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, brand.royalGold),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chevalier_emblem),
                        contentDescription = strings.officialSchoolName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = brand.burgundy.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = strings.managementReservedBadge,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = brand.burgundy,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.managementScreenHeadline,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = strings.managementScreenHeadline,
                            tint = brand.burgundy,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = strings.managementScreenBody,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = strings.managementScreenNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateBackToPublic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("management_return_public_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand.burgundy,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(strings.returnToPublicAppBtn)
                }

                Spacer(modifier = Modifier.height(20.dp))
                PoweredByJkLabsFooter()
            }
        }
    }
}

@Composable
private fun SettingsRadioRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val brand = LocalBrandPalette.current
    Surface(
        onClick = onClick,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) brand.burgundy else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) brand.burgundy else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
