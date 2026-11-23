package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.config.Version2FeatureConfig
import com.example.core.model.AppLanguage
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AcademicsV2RecordStatus
import com.example.data.v2.AdminV2Card

object Version2DesignTokens {
    val SoftCreamLight = Color(0xFFFFFBF2)
    val SoftCreamDark = Color(0xFF151412)
    val CardSurfaceLight = Color(0xFFFFFFFF)
    val CardSurfaceDark = Color(0xFF1F1E1C)

    val GradientBlue = Color(0xFF4F86F7)
    val GradientViolet = Color(0xFF7C5CFC)

    val GoldBorderStart = Color(0xFFD9A738)
    val GoldBorderEnd = Color(0xFFB37E12)
    val GoldIconTint = Color(0xFFB37D10)
    val GoldPastelLight = Color(0xFFFFF4D6)
    val GoldPastelDark = Color(0xFF332914)

    val BlueVioletBorderBrush: Brush = Brush.linearGradient(
        colors = listOf(GradientBlue, GradientViolet)
    )

    val OwnerGoldBorderBrush: Brush = Brush.linearGradient(
        colors = listOf(GoldBorderStart, GoldBorderEnd)
    )

    @Composable
    fun pageBackground(): Color =
        if (isSystemInDarkTheme()) SoftCreamDark else SoftCreamLight

    @Composable
    fun cardContainerColor(): Color =
        if (isSystemInDarkTheme()) CardSurfaceDark else CardSurfaceLight
}

/**
 * Custom crisp Gold Crown icon for Admin Card 08 (School Owner – Control Center).
 */
val GoldCrownIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoldCrown",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(3f, 18f)
            lineTo(21f, 18f)
            lineTo(21f, 20f)
            lineTo(3f, 20f)
            close()
            moveTo(3f, 16f)
            lineTo(2f, 7f)
            lineTo(7.5f, 11f)
            lineTo(12f, 4.5f)
            lineTo(16.5f, 11f)
            lineTo(22f, 7f)
            lineTo(21f, 16f)
            close()
        }
    }.build()
}

data class TopicIconStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val pastelLight: Color,
    val pastelDark: Color
)

fun styleForAcademicsCategory(category: AcademicsV2Category): TopicIconStyle = when (category) {
    AcademicsV2Category.ACADEMICS -> TopicIconStyle(
        icon = Icons.Filled.MenuBook,
        iconTint = Color(0xFF2F66D0),
        pastelLight = Color(0xFFEAF2FF),
        pastelDark = Color(0xFF1D283B)
    )
    AcademicsV2Category.HIGHER_SECONDARY_COURSES -> TopicIconStyle(
        icon = Icons.Filled.School,
        iconTint = Color(0xFF6B4FD1),
        pastelLight = Color(0xFFF1ECFF),
        pastelDark = Color(0xFF28213D)
    )
    AcademicsV2Category.ABOUT_SCHOOL -> TopicIconStyle(
        icon = Icons.Filled.AccountBalance,
        iconTint = Color(0xFF0E7C7B),
        pastelLight = Color(0xFFE5F7F6),
        pastelDark = Color(0xFF172E2E)
    )
    AcademicsV2Category.FACILITIES -> TopicIconStyle(
        icon = Icons.Filled.Domain,
        iconTint = Color(0xFF2B7A4B),
        pastelLight = Color(0xFFE8F7EE),
        pastelDark = Color(0xFF1A2F22)
    )
    AcademicsV2Category.TRANSPORT -> TopicIconStyle(
        icon = Icons.Filled.DirectionsBus,
        iconTint = Color(0xFFC26A18),
        pastelLight = Color(0xFFFFF2E2),
        pastelDark = Color(0xFF352617)
    )
    AcademicsV2Category.NEWS_ANNOUNCEMENTS -> TopicIconStyle(
        icon = Icons.Filled.Campaign,
        iconTint = Color(0xFFC93B5B),
        pastelLight = Color(0xFFFFEBF0),
        pastelDark = Color(0xFF381D24)
    )
    AcademicsV2Category.GALLERY -> TopicIconStyle(
        icon = Icons.Filled.PhotoLibrary,
        iconTint = Color(0xFF7B4AC7),
        pastelLight = Color(0xFFF3EBFF),
        pastelDark = Color(0xFF2A203A)
    )
    AcademicsV2Category.SCHOOL_TIMETABLE -> TopicIconStyle(
        icon = Icons.Filled.Schedule,
        iconTint = Color(0xFF1E779E),
        pastelLight = Color(0xFFE6F5FC),
        pastelDark = Color(0xFF192D36)
    )
    AcademicsV2Category.EXAMINATIONS -> TopicIconStyle(
        icon = Icons.Filled.Assignment,
        iconTint = Color(0xFF9C4A1A),
        pastelLight = Color(0xFFFFEFE6),
        pastelDark = Color(0xFF342219)
    )
    AcademicsV2Category.ACADEMIC_CALENDAR -> TopicIconStyle(
        icon = Icons.Filled.CalendarMonth,
        iconTint = Color(0xFF3961C9),
        pastelLight = Color(0xFFECF0FF),
        pastelDark = Color(0xFF1E263D)
    )
    AcademicsV2Category.FACULTY_STAFF -> TopicIconStyle(
        icon = Icons.Filled.Badge,
        iconTint = Color(0xFF27736A),
        pastelLight = Color(0xFFE7F6F3),
        pastelDark = Color(0xFF192E2B)
    )
    AcademicsV2Category.STUDENT_ACTIVITIES -> TopicIconStyle(
        icon = Icons.Filled.EmojiEvents,
        iconTint = Color(0xFFB36B00),
        pastelLight = Color(0xFFFFF5DF),
        pastelDark = Color(0xFF342A14)
    )
    AcademicsV2Category.ADMISSIONS -> TopicIconStyle(
        icon = Icons.Filled.HowToReg,
        iconTint = Color(0xFF4758C7),
        pastelLight = Color(0xFFEDF0FF),
        pastelDark = Color(0xFF20253B)
    )
}

fun styleForAdminCard(card: AdminV2Card): TopicIconStyle = when (card) {
    AdminV2Card.HOME_PAGE -> TopicIconStyle(
        icon = Icons.Filled.Home,
        iconTint = Color(0xFF2F66D0),
        pastelLight = Color(0xFFEAF2FF),
        pastelDark = Color(0xFF1D283B)
    )
    AdminV2Card.PARENT -> TopicIconStyle(
        icon = Icons.Filled.FamilyRestroom,
        iconTint = Color(0xFF0E7C7B),
        pastelLight = Color(0xFFE5F7F6),
        pastelDark = Color(0xFF172E2E)
    )
    AdminV2Card.ACADEMICS -> TopicIconStyle(
        icon = Icons.Filled.School,
        iconTint = Color(0xFF6B4FD1),
        pastelLight = Color(0xFFF1ECFF),
        pastelDark = Color(0xFF28213D)
    )
    AdminV2Card.CONTACT -> TopicIconStyle(
        icon = Icons.Filled.ContactPhone,
        iconTint = Color(0xFF2B7A4B),
        pastelLight = Color(0xFFE8F7EE),
        pastelDark = Color(0xFF1A2F22)
    )
    AdminV2Card.ADMIN -> TopicIconStyle(
        icon = Icons.Filled.AdminPanelSettings,
        iconTint = Color(0xFFC26A18),
        pastelLight = Color(0xFFFFF2E2),
        pastelDark = Color(0xFF352617)
    )
    AdminV2Card.SCHOOL_SETTINGS -> TopicIconStyle(
        icon = Icons.Filled.Settings,
        iconTint = Color(0xFF4758C7),
        pastelLight = Color(0xFFEDF0FF),
        pastelDark = Color(0xFF20253B)
    )
    AdminV2Card.PREVIEW_PUBLISH -> TopicIconStyle(
        icon = Icons.Filled.Publish,
        iconTint = Color(0xFF7B4AC7),
        pastelLight = Color(0xFFF3EBFF),
        pastelDark = Color(0xFF2A203A)
    )
    AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER -> TopicIconStyle(
        icon = GoldCrownIcon,
        iconTint = Version2DesignTokens.GoldIconTint,
        pastelLight = Version2DesignTokens.GoldPastelLight,
        pastelDark = Version2DesignTokens.GoldPastelDark
    )
}

/**
 * Shared Version 2 premium grid card used across:
 * - Public Academics Page (13 cards)
 * - Admin Management Panel (8 cards)
 * - Admin Management -> Academics Hub (13 cards)
 *
 * Uses white/light rounded surface, subtle shadow, thin Blue+Violet gradient border
 * (or restrained Gold border + Gold Crown for School Owner – Control Center),
 * small pastel-accented topic icon, and clean spacing safe for large font scaling.
 */
@Composable
fun Version2PremiumGridCard(
    title: String,
    subtitle: String,
    iconStyle: TopicIconStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    isOwnerAccent: Boolean = false,
    isLocked: Boolean = false,
    testTagValue: String = ""
) {
    val dark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(18.dp)
    val borderBrush = if (isOwnerAccent) {
        Version2DesignTokens.OwnerGoldBorderBrush
    } else {
        Version2DesignTokens.BlueVioletBorderBrush
    }
    val badgeBg = if (dark) iconStyle.pastelDark else iconStyle.pastelLight

    Card(
        shape = cardShape,
        border = BorderStroke(1.25.dp, borderBrush),
        colors = CardDefaults.cardColors(
            containerColor = Version2DesignTokens.cardContainerColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = cardShape,
                ambientColor = if (isOwnerAccent) Color(0x28C8921F) else Color(0x225B6EF5),
                spotColor = if (isOwnerAccent) Color(0x28C8921F) else Color(0x225B6EF5)
            )
            .clip(cardShape)
            .heightIn(min = 132.dp)
            .clickable(
                enabled = !isLocked,
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
            .then(if (testTagValue.isNotBlank()) Modifier.testTag(testTagValue) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconStyle.icon,
                        contentDescription = title,
                        tint = if (isLocked) MaterialTheme.colorScheme.outline else iconStyle.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Restricted access",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = iconStyle.iconTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun Version2StatusBadge(
    status: AcademicsV2RecordStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        AcademicsV2RecordStatus.PUBLISHED -> Color(0xFFE7F6EC) to Color(0xFF1D6F3D)
        AcademicsV2RecordStatus.PENDING_APPROVAL -> Color(0xFFFFF4DC) to Color(0xFF9A6100)
        AcademicsV2RecordStatus.DRAFT -> Color(0xFFEDF1FB) to Color(0xFF39559C)
        AcademicsV2RecordStatus.UNPUBLISHED -> Color(0xFFF3EFEA) to Color(0xFF6B5C4C)
        AcademicsV2RecordStatus.ARCHIVED -> Color(0xFFECEFF1) to Color(0xFF4F5D66)
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = status.displayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

/**
 * Displays the exact localized Version 2 availability notice whenever an unreleased
 * Version 2 route or feature is accessed while Version 2 is disabled.
 */
@Composable
fun Version2UnavailableNoticeScreen(
    language: AppLanguage,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noticeText = Version2FeatureConfig.comingInVersion2Message(language)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .padding(16.dp)
            .testTag("version2_unavailable_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("version2_unavailable_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.25.dp, Version2DesignTokens.BlueVioletBorderBrush),
            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("version2_unavailable_notice_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDF1FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = noticeText,
                        tint = Version2DesignTokens.GradientViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = noticeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("version2_unavailable_message")
                )
                Button(
                    onClick = onBack,
                    modifier = Modifier.testTag("version2_unavailable_done_button")
                ) {
                    Text("Go Back")
                }
            }
        }
    }
}

@Composable
fun Version2UnavailableBanner(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val noticeText = Version2FeatureConfig.comingInVersion2Message(language)
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
        color = Version2DesignTokens.cardContainerColor(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("version2_unavailable_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = noticeText,
                tint = Version2DesignTokens.GradientBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = noticeText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
