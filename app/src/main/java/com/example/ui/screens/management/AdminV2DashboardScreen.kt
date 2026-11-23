package com.example.ui.screens.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.PermissionAction
import com.example.data.v2.AcademicsV2AccessControl
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AcademicsV2RecordStatus
import com.example.data.v2.AdminV2Card
import com.example.ui.components.Version2DesignTokens
import com.example.ui.components.Version2PremiumGridCard
import com.example.ui.components.styleForAdminCard
import com.example.ui.viewmodel.ManagementViewModel

/**
 * Version 2 Admin Management Panel with EXACTLY 8 premium cards in a responsive 2-column grid.
 * No separate right-side Admin menu. Card 08 (School Owner – Control Center) uses the Gold Crown
 * icon and subtle Gold accent while maintaining identical dimensions and alignment.
 */
@Composable
fun AdminV2DashboardGrid(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onSelectAdminCard: (AdminV2Card) -> Unit
) {
    val v2Snapshot by managementViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val pendingCount = remember(v2Snapshot) {
        v2Snapshot.records.count { it.status == AcademicsV2RecordStatus.PENDING_APPROVAL } +
            v2Snapshot.staffAccessRequests.count { it.status.key == "PENDING" }
    }
    val draftCount = remember(v2Snapshot) {
        v2Snapshot.records.count { it.status != AcademicsV2RecordStatus.PUBLISHED } +
            v2Snapshot.galleryAlbums.count { it.status != AcademicsV2RecordStatus.PUBLISHED } +
            v2Snapshot.facultyProfiles.count { it.status != AcademicsV2RecordStatus.PUBLISHED }
    }

    val cardPairs = remember { AdminV2Card.entries.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_dashboard_grid"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "admin_v2_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Admin Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Signed in as ${session.account.displayName} (${AcademicsV2AccessControl.roleDisplayLabel(session.account.role)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(
            items = cardPairs,
            key = { pair -> pair.joinToString("_") { it.name } }
        ) { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCards.forEach { card ->
                    val canAccess = AcademicsV2AccessControl.canAccessAdminCard(session, card)
                    val badge = when (card) {
                        AdminV2Card.ACADEMICS -> "13"
                        AdminV2Card.PREVIEW_PUBLISH -> if (draftCount > 0) "$draftCount" else null
                        AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER -> if (canAccess && pendingCount > 0) "$pendingCount" else null
                        else -> null
                    }
                    Version2PremiumGridCard(
                        title = card.title,
                        subtitle = if (canAccess) card.subtitle else "Restricted by role permissions",
                        iconStyle = styleForAdminCard(card),
                        badgeText = badge,
                        isOwnerAccent = card.isOwnerCard,
                        isLocked = !canAccess,
                        onClick = { onSelectAdminCard(card) },
                        testTagValue = card.testTag,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCards.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Admin Card 01 – Home Page Management Hub.
 * Preserves the finalized Home Page design while routing directly to existing verified managers.
 */
@Composable
fun AdminV2HomePageHubScreen(
    onBack: () -> Unit,
    onOpenHeroBanner: () -> Unit,
    onOpenSchoolPosts: () -> Unit,
    onOpenHighlights: () -> Unit,
    onOpenLivePreview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_home_page_hub"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminV2HeaderRow(
                title = "Home Page Management",
                subtitle = "Manage Hero Banner, posts, highlights and publish status",
                onBack = onBack
            )
        }
        item {
            AdminV2ActionTile(
                title = "Hero Banner & Logo",
                description = "Manage Hero Banner photos, school emblem and headline text.",
                actionLabel = "Open Banner Editor",
                testTag = "admin_v2_home_banner_btn",
                onClick = onOpenHeroBanner
            )
        }
        item {
            AdminV2ActionTile(
                title = "Home Feed Posts",
                description = "Create, edit, publish or unpublish Home Page school feed posts.",
                actionLabel = "Manage Posts",
                testTag = "admin_v2_home_posts_btn",
                onClick = onOpenSchoolPosts
            )
        }
        item {
            AdminV2ActionTile(
                title = "Highlights Section",
                description = "Manage Home Page Highlights cards and visibility status.",
                actionLabel = "Manage Highlights",
                testTag = "admin_v2_home_highlights_btn",
                onClick = onOpenHighlights
            )
        }
        item {
            AdminV2ActionTile(
                title = "Preview Public Home Page",
                description = "Inspect the live public Home Page presentation before publishing changes.",
                actionLabel = "Open Preview",
                testTag = "admin_v2_home_preview_btn",
                onClick = onOpenLivePreview
            )
        }
    }
}

/**
 * Admin Card 02 – Parent Info Management Hub.
 * Strictly scoped to approved Public School Profile parent information & circulars.
 * Does NOT invent an unapproved private student/parent portal.
 */
@Composable
fun AdminV2ParentInfoHubScreen(
    onBack: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    onOpenNewsCircularsV2: () -> Unit,
    onOpenTransportV2: () -> Unit,
    onOpenCalendarV2: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_parent_hub"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminV2HeaderRow(
                title = "Parent Information",
                subtitle = "Public School Profile parent circulars, transport & calendar notices",
                onBack = onBack
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                color = Version2DesignTokens.cardContainerColor(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Scoped strictly to approved public parent communications and school profile notices. Private student portals are outside the Public School Profile scope.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        item {
            AdminV2ActionTile(
                title = "Parent Circulars & Notices (V2)",
                description = "Publish official school circulars, notices and important parent updates.",
                actionLabel = "Manage Circulars",
                testTag = "admin_v2_parent_circulars_btn",
                onClick = onOpenNewsCircularsV2
            )
        }
        item {
            AdminV2ActionTile(
                title = "School Announcements",
                description = "Manage urgent announcements and public notices for parents.",
                actionLabel = "Open Announcements",
                testTag = "admin_v2_parent_announcements_btn",
                onClick = onOpenAnnouncements
            )
        }
        item {
            AdminV2ActionTile(
                title = "Transport Information",
                description = "Keep bus routes, stops, timings and parent transport guidelines updated.",
                actionLabel = "Manage Transport",
                testTag = "admin_v2_parent_transport_btn",
                onClick = onOpenTransportV2
            )
        }
        item {
            AdminV2ActionTile(
                title = "Academic Calendar & Holidays",
                description = "Publish academic year dates, holidays and school events for families.",
                actionLabel = "Manage Calendar",
                testTag = "admin_v2_parent_calendar_btn",
                onClick = onOpenCalendarV2
            )
        }
    }
}

/**
 * Admin Card 05 – Admin & Staff Management Hub.
 * Provides Staff Management, Role & Permission Assignment across all 13 Academics V2 categories,
 * and Staff Access Request submission for School Owner approval.
 */
@Composable
fun AdminV2AdminTeamHubScreen(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onBack: () -> Unit,
    onOpenLegacyStaffEditor: () -> Unit
) {
    val accounts by managementViewModel.accountsWithPermissions.collectAsStateWithLifecycle()
    val nonMasterAccounts = remember(accounts) {
        accounts.filter { it.account.role != ManagementRole.MASTER_ADMIN }
    }
    val canManageStaff = AcademicsV2AccessControl.isSchoolOwner(session) ||
        AcademicsV2AccessControl.hasSectionPermission(
            session,
            com.example.data.local.entity.ManagementSection.STAFF_MANAGEMENT,
            PermissionAction.EDIT
        )

    var selectedStaffId by remember(nonMasterAccounts) {
        mutableStateOf(nonMasterAccounts.firstOrNull()?.account?.id)
    }
    var requestReason by remember { mutableStateOf("") }
    val requestedKeys = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_admin_team_hub"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminV2HeaderRow(
                title = "Admin & Staff Access",
                subtitle = "Manage staff accounts, responsibilities, roles and category permissions",
                onBack = onBack
            )
        }

        if (canManageStaff) {
            item {
                AdminV2ActionTile(
                    title = "Create & Manage Staff Accounts",
                    description = "Add staff accounts, reset PINs, activate/deactivate accounts and manage core permissions.",
                    actionLabel = "Open Staff Directory",
                    testTag = "admin_v2_open_staff_accounts_btn",
                    onClick = onOpenLegacyStaffEditor
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Assign 13-Category Academics Responsibilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (nonMasterAccounts.isEmpty()) {
                            Text(
                                text = "No staff accounts created yet. Create a staff account above to assign category permissions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                nonMasterAccounts.forEach { staff ->
                                    val selected = staff.account.id == selectedStaffId
                                    if (selected) {
                                        Button(
                                            onClick = { selectedStaffId = staff.account.id },
                                            modifier = Modifier.testTag("admin_v2_select_staff_${staff.account.username}")
                                        ) {
                                            Text(staff.account.displayName)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { selectedStaffId = staff.account.id },
                                            modifier = Modifier.testTag("admin_v2_select_staff_${staff.account.username}")
                                        ) {
                                            Text(staff.account.displayName)
                                        }
                                    }
                                }
                            }

                            val activeTarget = nonMasterAccounts.firstOrNull { it.account.id == selectedStaffId }
                            if (activeTarget != null) {
                                Text(
                                    text = "Permissions for ${activeTarget.account.displayName} (${AcademicsV2AccessControl.roleDisplayLabel(activeTarget.account.role)})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                AcademicsV2Category.entries.forEach { cat ->
                                    val hasView = AcademicsV2AccessControl.hasCategoryPermission(
                                        activeTarget,
                                        cat,
                                        PermissionAction.VIEW
                                    )
                                    val hasPublish = AcademicsV2AccessControl.hasCategoryPermission(
                                        activeTarget,
                                        cat,
                                        PermissionAction.PUBLISH
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cat.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (hasView) {
                                                    if (hasPublish) "View, Create, Edit & Publish" else "View, Create & Edit (Draft)"
                                                } else {
                                                    "No Access"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = hasView,
                                            onCheckedChange = { granted ->
                                                managementViewModel.updateStaffV2CategoryPermission(
                                                    targetAccountId = activeTarget.account.id,
                                                    category = cat,
                                                    canView = granted,
                                                    canCreate = granted,
                                                    canEdit = granted,
                                                    canDelete = false,
                                                    canPublish = false
                                                )
                                            },
                                            modifier = Modifier.testTag("admin_v2_perm_toggle_${cat.routeId}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!AcademicsV2AccessControl.isSchoolOwner(session)) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Request Additional Staff Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Submit a request for School Owner review and approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AcademicsV2Category.entries.take(6).forEach { cat ->
                            val checked = cat.permissionKey in requestedKeys
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) requestedKeys.add(cat.permissionKey)
                                        else requestedKeys.remove(cat.permissionKey)
                                    }
                                )
                                Text(cat.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        OutlinedTextField(
                            value = requestReason,
                            onValueChange = { requestReason = it },
                            label = { Text("Reason for access request") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                managementViewModel.submitStaffAccessRequest(
                                    requestedRole = AcademicsV2AccessControl.roleDisplayLabel(session.account.role),
                                    requestedCategoryKeys = requestedKeys.toList(),
                                    reason = requestReason
                                ) {
                                    requestReason = ""
                                    requestedKeys.clear()
                                }
                            },
                            modifier = Modifier.testTag("admin_v2_submit_access_request_btn")
                        ) {
                            Text("Submit to School Owner")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Admin Card 06 – School Settings Hub.
 * Connects directly to existing School Profile, Logo, Social Media Links, and App Settings editors.
 */
@Composable
fun AdminV2SchoolSettingsHubScreen(
    onBack: () -> Unit,
    onOpenSchoolProfile: () -> Unit,
    onOpenLogoBanner: () -> Unit,
    onOpenSocialMedia: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_school_settings_hub"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminV2HeaderRow(
                title = "School Settings",
                subtitle = "School Name, Logo, Address, Website, Social Media & General Settings",
                onBack = onBack
            )
        }
        item {
            AdminV2ActionTile(
                title = "School Profile & Website Information",
                description = "Update School Name, Address, Office Hours, Vision, Mission and official Website info.",
                actionLabel = "Edit School Profile",
                testTag = "admin_v2_settings_profile_btn",
                onClick = onOpenSchoolProfile
            )
        }
        item {
            AdminV2ActionTile(
                title = "School Logo & Campus Banner",
                description = "Update official school logo and campus banner photography.",
                actionLabel = "Manage Logo",
                testTag = "admin_v2_settings_logo_btn",
                onClick = onOpenLogoBanner
            )
        }
        item {
            AdminV2ActionTile(
                title = "Social Media Links",
                description = "Configure verified official school social media channels.",
                actionLabel = "Manage Social Links",
                testTag = "admin_v2_settings_social_btn",
                onClick = onOpenSocialMedia
            )
        }
        item {
            AdminV2ActionTile(
                title = "General School Profile & App Settings",
                description = "Configure language, theme and local device alert preferences.",
                actionLabel = "Open App Settings",
                testTag = "admin_v2_settings_general_btn",
                onClick = onOpenAppSettings
            )
        }
    }
}

@Composable
internal fun AdminV2HeaderRow(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("admin_v2_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdminV2ActionTile(
    title: String,
    description: String,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClick,
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionLabel)
            }
        }
    }
}
