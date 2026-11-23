package com.example.ui.screens.management

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ManagementSection
import com.example.data.preferences.AdminHeroBannerConfig
import com.example.data.repository.AdminDashboardCard
import com.example.data.repository.AdminDashboardSearchResult
import com.example.ui.components.ManagedPublicImage
import com.example.ui.components.Version2DesignTokens
import com.example.ui.viewmodel.ManagementViewModel

object AdminDashboardBlueprint {
    val NavyBackground = Color(0xFF0A1124)
    val NavyBackgroundBottom = Color(0xFF0E1730)
    val NavyHeader = Color(0xFF0E1833)
    val NavyCardSurface = Color(0xFF132040)
    val NavyCardSurfaceLocked = Color(0xFF0F1933)
    val NavyIconBadge = Color(0xFF1C2E5A)
    val NavyIconBadgeLocked = Color(0xFF162242)
    val OutlineBlue = Version2DesignTokens.GradientBlue
    val OutlineViolet = Version2DesignTokens.GradientViolet
    val AccentLavender = Color(0xFFA7B8FF)
    val TextPrimary = Color(0xFFF3F6FF)
    val TextSecondary = Color(0xFFB5C3E0)
    val TextMuted = Color(0xFF7B8BB0)
    val HeaderDivider = Color(0x44667EFF)

    val PageBackgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(NavyBackground, NavyBackgroundBottom)
    )

    val CardOutlineBrush: Brush = Version2DesignTokens.BlueVioletBorderBrush

    val LockedOutlineBrush: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF283964), Color(0xFF363263))
    )

    val HeroDefaultBackdropBrush: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF16254C), Color(0xFF1E2D5F), Color(0xFF2B2563))
    )

    val HeroReadabilityScrim: Brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xD9070D1F),
            Color(0xBF0B142E),
            Color(0x94142146)
        )
    )
}

internal fun iconForAdminDashboardCard(card: AdminDashboardCard): ImageVector = when (card) {
    AdminDashboardCard.HOME_PAGE -> Icons.Filled.Home
    AdminDashboardCard.ACADEMY -> Icons.Filled.School
    AdminDashboardCard.PARENT -> Icons.Filled.FamilyRestroom
    AdminDashboardCard.CONTACT -> Icons.Filled.ContactPhone
    AdminDashboardCard.STAFF_MANAGEMENT -> Icons.Filled.ManageAccounts
    AdminDashboardCard.COMMON_APP -> Icons.Filled.Tune
    AdminDashboardCard.MANAGEMENT_TOOLS -> Icons.Filled.Build
    AdminDashboardCard.UPDATES -> Icons.Filled.Campaign
}

/**
 * Finalized Chevalier Academy Admin Header:
 * Left: Back arrow + CHEVALIER ACADEMY identity/title (or active sub-screen title).
 * Right (exact order): Search icon -> Notification bell -> Menu icon.
 */
@Composable
fun AdminDashboardTopBar(
    isDashboardRoot: Boolean,
    title: String,
    adminDisplayName: String,
    adminRoleLabel: String,
    unreadNotificationsCount: Int,
    canEditHeroBanner: Boolean,
    canOpenSettings: Boolean,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEditHeroBannerClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onExitToPublicApp: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = AdminDashboardBlueprint.NavyHeader,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_dashboard_header")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(if (isDashboardRoot) "mgmt_header_back_btn" else "mgmt_subscreen_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isDashboardRoot) "Back to public app" else "Back",
                        tint = AdminDashboardBlueprint.TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    if (!isDashboardRoot) {
                        Text(
                            text = "CHEVALIER ACADEMY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = AdminDashboardBlueprint.AccentLavender,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (isDashboardRoot) "CHEVALIER ACADEMY" else title,
                        style = if (isDashboardRoot) {
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        } else {
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        },
                        color = AdminDashboardBlueprint.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("mgmt_header_title")
                    )
                }

                // Right side in exact order: 1. Search -> 2. Notification Bell -> 3. Menu
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mgmt_top_search_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search Admin Dashboard",
                        tint = AdminDashboardBlueprint.TextPrimary
                    )
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mgmt_top_notifications_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(
                                    containerColor = AdminDashboardBlueprint.OutlineViolet,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Admin Notifications",
                            tint = AdminDashboardBlueprint.TextPrimary
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mgmt_top_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Admin Menu",
                            tint = AdminDashboardBlueprint.TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.testTag("mgmt_header_menu_dropdown")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = adminDisplayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = adminRoleLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()

                        if (canEditHeroBanner) {
                            DropdownMenuItem(
                                text = { Text("Edit Hero Banner") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditHeroBannerClick()
                                },
                                modifier = Modifier.testTag("mgmt_menu_edit_hero")
                            )
                        }

                        if (canOpenSettings) {
                            DropdownMenuItem(
                                text = { Text("App Settings") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettingsClick()
                                },
                                modifier = Modifier.testTag("mgmt_menu_settings")
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Public App") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Public, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onExitToPublicApp()
                            },
                            modifier = Modifier.testTag("mgmt_menu_public_app")
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Logout") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onLogoutClick()
                            },
                            modifier = Modifier.testTag("mgmt_menu_logout")
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = AdminDashboardBlueprint.HeaderDivider)
        }
    }
}

/**
 * Compact, full-width, landscape rectangular Admin Hero Banner.
 * Displays an Admin-uploaded school background image with dark readability scrim
 * and three editable text lines.
 */
@Composable
fun AdminHeroBannerCard(
    config: AdminHeroBannerConfig,
    adminDisplayName: String,
    canEdit: Boolean,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bannerShape = RoundedCornerShape(18.dp)

    Card(
        shape = bannerShape,
        border = BorderStroke(1.25.dp, AdminDashboardBlueprint.CardOutlineBrush),
        colors = CardDefaults.cardColors(containerColor = AdminDashboardBlueprint.NavyCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(144.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!config.backgroundImageUri.isNullOrBlank()) {
                ManagedPublicImage(
                    imageUri = config.backgroundImageUri,
                    contentDescription = config.titleLine1,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AdminDashboardBlueprint.HeroDefaultBackdropBrush)
                )
            }

            // Dark overlay for crisp, accessible contrast over any uploaded school photo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AdminDashboardBlueprint.HeroReadabilityScrim)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 18.dp,
                        end = if (canEdit && onEditClick != null) 60.dp else 18.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = config.titleLine1.ifBlank { AdminHeroBannerConfig.DEFAULT_TITLE_LINE_1 },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = AdminDashboardBlueprint.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("admin_hero_line_1")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = config.resolvedWelcomeLine(adminDisplayName),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AdminDashboardBlueprint.AccentLavender,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("admin_hero_line_2")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = config.subtitleLine3.ifBlank { AdminHeroBannerConfig.DEFAULT_SUBTITLE_LINE_3 },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdminDashboardBlueprint.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("admin_hero_line_3")
                )
            }

            if (canEdit && onEditClick != null) {
                Surface(
                    shape = CircleShape,
                    color = AdminDashboardBlueprint.NavyHeader.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, AdminDashboardBlueprint.CardOutlineBrush),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_hero_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Admin Hero Banner",
                            tint = AdminDashboardBlueprint.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Finalized Chevalier Academy Admin Dashboard Home Body:
 * 1. Compact landscape rectangular Hero Banner directly below the header.
 * 2. Exact 8 equal-sized compact rectangular cards in a 2-column x 4-row grid.
 *    Same layout for all authorized Admin roles; RBAC restricts unauthorized cards.
 */
@Composable
fun AdminDashboardHomeContent(
    session: AccountWithPermissions,
    heroConfig: AdminHeroBannerConfig,
    canEditHeroBanner: Boolean,
    canAccessCard: (AdminDashboardCard) -> Boolean,
    onEditHeroBanner: () -> Unit,
    onCardClick: (AdminDashboardCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AdminDashboardBlueprint.PageBackgroundBrush),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .testTag("management_dashboard_grid"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AdminHeroBannerCard(
                    config = heroConfig,
                    adminDisplayName = session.account.displayName,
                    canEdit = canEditHeroBanner,
                    onEditClick = if (canEditHeroBanner) onEditHeroBanner else null,
                    modifier = Modifier.testTag("admin_hero_banner_card")
                )
            }

            items(
                items = AdminDashboardCard.entries,
                key = { it.id }
            ) { card ->
                val allowed = canAccessCard(card)
                AdminDashboardGridCard(
                    card = card,
                    isAllowed = allowed,
                    onClick = { onCardClick(card) }
                )
            }
        }
    }
}

@Composable
private fun AdminDashboardGridCard(
    card: AdminDashboardCard,
    isAllowed: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderBrush = if (isAllowed) {
        AdminDashboardBlueprint.CardOutlineBrush
    } else {
        AdminDashboardBlueprint.LockedOutlineBrush
    }

    Card(
        shape = shape,
        border = BorderStroke(1.25.dp, borderBrush),
        colors = CardDefaults.cardColors(
            containerColor = if (isAllowed) {
                AdminDashboardBlueprint.NavyCardSurface
            } else {
                AdminDashboardBlueprint.NavyCardSurfaceLocked
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAllowed) 3.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClickLabel = card.title,
                onClick = onClick
            )
            .testTag("admin_dash_card_${card.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (!isAllowed) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Restricted access for ${card.title}",
                    tint = AdminDashboardBlueprint.TextMuted,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .testTag("admin_dash_card_lock_${card.id}")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isAllowed) {
                                AdminDashboardBlueprint.NavyIconBadge
                            } else {
                                AdminDashboardBlueprint.NavyIconBadgeLocked
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForAdminDashboardCard(card),
                        contentDescription = null,
                        tint = if (isAllowed) {
                            AdminDashboardBlueprint.AccentLavender
                        } else {
                            AdminDashboardBlueprint.TextMuted
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isAllowed) {
                        AdminDashboardBlueprint.TextPrimary
                    } else {
                        AdminDashboardBlueprint.TextMuted
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (!isAllowed) 14.dp else 0.dp)
                )
            }
        }
    }
}

/**
 * Dialog allowing authorized Admins to upload/change the Hero Banner background image,
 * edit all three text lines, preview changes before saving, and persist the configuration.
 */
@Composable
fun AdminHeroBannerEditorDialog(
    initialConfig: AdminHeroBannerConfig,
    adminDisplayName: String,
    isBusy: Boolean,
    onPickBackgroundImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onSave: (line1: String, line2: String, line3: String, imageUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var line1 by remember(initialConfig) { mutableStateOf(initialConfig.titleLine1) }
    var line2 by remember(initialConfig) { mutableStateOf(initialConfig.welcomeLine2) }
    var line3 by remember(initialConfig) { mutableStateOf(initialConfig.subtitleLine3) }
    var backgroundImageUri by remember(initialConfig) { mutableStateOf(initialConfig.backgroundImageUri) }
    var showPreview by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickBackgroundImage(uri) { optimizedUri ->
                backgroundImageUri = optimizedUri
            }
        }
    }

    val draftConfig = AdminHeroBannerConfig(
        titleLine1 = line1.ifBlank { AdminHeroBannerConfig.DEFAULT_TITLE_LINE_1 },
        welcomeLine2 = line2.ifBlank { AdminHeroBannerConfig.DEFAULT_WELCOME_LINE_2 },
        subtitleLine3 = line3.ifBlank { AdminHeroBannerConfig.DEFAULT_SUBTITLE_LINE_3 },
        backgroundImageUri = backgroundImageUri
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_hero_editor_dialog"),
        title = {
            Text(
                text = "Customize Admin Hero Banner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("admin_hero_upload_image_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (backgroundImageUri.isNullOrBlank()) "Upload Photo" else "Change Photo")
                    }

                    if (!backgroundImageUri.isNullOrBlank()) {
                        IconButton(
                            onClick = { backgroundImageUri = null },
                            enabled = !isBusy,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("admin_hero_remove_image_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove background image"
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = line1,
                    onValueChange = { line1 = it },
                    label = { Text("Line 1 (Banner Title)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_hero_input_line_1")
                )

                OutlinedTextField(
                    value = line2,
                    onValueChange = { line2 = it },
                    label = { Text("Line 2 (Welcome Line)") },
                    supportingText = {
                        Text("Use {Admin Name} to insert the signed-in admin's name automatically.")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_hero_input_line_2")
                )

                OutlinedTextField(
                    value = line3,
                    onValueChange = { line3 = it },
                    label = { Text("Line 3 (Subtitle)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_hero_input_line_3")
                )

                OutlinedButton(
                    onClick = { showPreview = !showPreview },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("admin_hero_preview_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showPreview) "Hide Preview" else "Preview Hero Banner")
                }

                if (showPreview) {
                    AdminHeroBannerCard(
                        config = draftConfig,
                        adminDisplayName = adminDisplayName,
                        canEdit = false,
                        onEditClick = null,
                        modifier = Modifier.testTag("admin_hero_preview_card")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(line1, line2, line3, backgroundImageUri) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminDashboardBlueprint.OutlineViolet,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_hero_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_hero_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Authorized Admin Dashboard Search screen.
 * Strictly searches authorized Admin Dashboard sections and management records only;
 * never exposes public Home content, public Academy content, or private Parent/student records.
 */
@Composable
fun AdminDashboardSearchScreen(
    managementViewModel: ManagementViewModel,
    onOpenCard: (AdminDashboardCard) -> Unit,
    onOpenSection: (ManagementSection) -> Unit,
    onOpenToolKey: (String) -> Unit
) {
    val results by managementViewModel.adminDashboardSearchResults.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminDashboardBlueprint.PageBackgroundBrush)
            .testTag("admin_dashboard_search_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        managementViewModel.performAdminDashboardSearch(it)
                    },
                    label = { Text("Search authorized Admin sections & records") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = AdminDashboardBlueprint.AccentLavender
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    managementViewModel.clearAdminDashboardSearch()
                                },
                                modifier = Modifier.testTag("admin_search_clear_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = AdminDashboardBlueprint.TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = AdminDashboardBlueprint.TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mgmt_global_search_input")
                )
            }

            if (query.isBlank()) {
                item {
                    Text(
                        text = "Search authorized Admin Dashboard sections, staff accounts, approvals, audit logs, notifications, and media records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminDashboardBlueprint.TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else if (results.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AdminDashboardBlueprint.NavyCardSurface,
                        border = BorderStroke(1.dp, AdminDashboardBlueprint.LockedOutlineBrush),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_search_empty_state")
                    ) {
                        Text(
                            text = "No authorized Admin Dashboard results found for \"$query\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdminDashboardBlueprint.TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(items = results, key = { it.id }) { res ->
                    AdminSearchResultCard(
                        result = res,
                        onClick = {
                            when {
                                res.targetCard != null -> onOpenCard(res.targetCard)
                                res.targetSection != null -> onOpenSection(res.targetSection)
                                res.targetToolKey != null -> onOpenToolKey(res.targetToolKey)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminSearchResultCard(
    result: AdminDashboardSearchResult,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AdminDashboardBlueprint.CardOutlineBrush),
        colors = CardDefaults.cardColors(containerColor = AdminDashboardBlueprint.NavyCardSurface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_search_result_${result.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AdminDashboardBlueprint.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AdminDashboardBlueprint.NavyIconBadge
                ) {
                    Text(
                        text = result.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = AdminDashboardBlueprint.AccentLavender,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = result.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AdminDashboardBlueprint.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
