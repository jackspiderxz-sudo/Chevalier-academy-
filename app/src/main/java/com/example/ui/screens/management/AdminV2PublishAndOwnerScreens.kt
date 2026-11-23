package com.example.ui.screens.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.v2.AcademicsV2AccessControl
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AcademicsV2RecordStatus
import com.example.data.v2.SchoolOwnerSection
import com.example.data.v2.V2StaffAccessRequestStatus
import com.example.ui.components.GoldCrownIcon
import com.example.ui.components.Version2DesignTokens
import com.example.ui.components.Version2StatusBadge
import com.example.ui.screens.AcademicsV2PublicHubScreen
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.SchoolDataViewModel

private enum class PreviewPublishTab(val label: String, val tag: String) {
    REVIEW("Changed Content & Publish", "preview_tab_review"),
    HOME("Public Home Preview", "preview_tab_home"),
    ACADEMICS("Public Academics Preview", "preview_tab_academics")
}

/**
 * Admin Card 07 – Preview & Publish.
 * Provides Public Home Page Preview, Public Academics Page Preview, changed content status,
 * and Publish/Unpublish controls clearly distinguishing normal Admin publishing from
 * School Owner final approval.
 */
@Composable
fun AdminV2PreviewAndPublishScreen(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    schoolDataViewModel: SchoolDataViewModel,
    onBack: () -> Unit,
    onOpenFullHomePreview: () -> Unit,
    onOpenAcademicsCategory: (AcademicsV2Category) -> Unit
) {
    val v2Snapshot by managementViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val publishedPosts by schoolDataViewModel.publishedSchoolPosts.collectAsStateWithLifecycle()
    val publishedHighlights by schoolDataViewModel.publishedSchoolHighlights.collectAsStateWithLifecycle()
    val isOwner = AcademicsV2AccessControl.isSchoolOwner(session)
    var activeTab by remember { mutableStateOf(PreviewPublishTab.REVIEW) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_preview_publish_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminV2HeaderRow(
                title = "Preview & Publish",
                subtitle = "Distinguishes Admin publishing from School Owner final approval",
                onBack = onBack
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewPublishTab.entries.forEach { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }

        when (activeTab) {
            PreviewPublishTab.REVIEW -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                            color = Version2DesignTokens.cardContainerColor(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Publication Authority Rules",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• Normal Admin / Authorized Staff: Can publish categories where they hold Publish permission.\n• School Owner Final Approval: Required for Exam Results authorization and escalated approval requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (v2Snapshot.records.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No Version 2 Academics drafts or published records yet. Use Admin → Academics to create entries.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(items = v2Snapshot.records, key = { it.id }) { rec ->
                            val section = rec.category.findSection(rec.sectionKey)
                            val requiresOwner = section?.requiresOwnerAuthorizationToPublish == true
                            val canDirectPublish = AcademicsV2AccessControl.canPublishDirectly(
                                session,
                                rec.category,
                                rec.sectionKey
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (requiresOwner) Version2DesignTokens.OwnerGoldBorderBrush
                                    else Version2DesignTokens.BlueVioletBorderBrush
                                ),
                                colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${rec.category.title} • ${section?.title ?: rec.sectionKey}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Version2StatusBadge(status = rec.status)
                                    }
                                    Text(
                                        text = rec.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (requiresOwner) {
                                            "Approval tier: School Owner Final Authorization Required"
                                        } else {
                                            "Approval tier: Authorized Admin / Staff Publish"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (rec.status != AcademicsV2RecordStatus.PUBLISHED) {
                                            if (requiresOwner) {
                                                if (isOwner) {
                                                    Button(onClick = {
                                                        managementViewModel.ownerApproveAndPublishV2Record(
                                                            rec.id,
                                                            authorizeExamResult = true
                                                        )
                                                    }) { Text("Owner Approve & Publish") }
                                                } else {
                                                    OutlinedButton(onClick = {
                                                        managementViewModel.submitV2RecordForApproval(rec.id)
                                                    }) { Text("Send to School Owner") }
                                                }
                                            } else if (canDirectPublish) {
                                                Button(onClick = {
                                                    managementViewModel.publishV2Record(rec.id)
                                                }) { Text("Publish") }
                                            }
                                        } else if (canDirectPublish || isOwner) {
                                            OutlinedButton(onClick = {
                                                managementViewModel.updateV2RecordStatus(
                                                    rec.id,
                                                    AcademicsV2RecordStatus.UNPUBLISHED
                                                )
                                            }) { Text("Unpublish") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PreviewPublishTab.HOME -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Public Home Page Snapshot",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Published Home Posts: ${publishedPosts.size} • Published Highlights: ${publishedHighlights.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onOpenFullHomePreview,
                                    modifier = Modifier.testTag("admin_v2_launch_home_preview")
                                ) {
                                    Text("Open Live Public Home Preview")
                                }
                            }
                        }
                    }
                }
            }

            PreviewPublishTab.ACADEMICS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    AcademicsV2PublicHubScreen(
                        schoolDataViewModel = schoolDataViewModel,
                        onOpenCategory = onOpenAcademicsCategory
                    )
                }
            }
        }
    }
}

/**
 * Admin Card 08 – School Owner – Control Center.
 * Strictly guarded for School Owner (MASTER_ADMIN / SUPER_OWNER) with all 6 agreed sections:
 * 1. Owner Profile & Contact (private, NEVER public)
 * 2. Staff Access Approval (approve / reject / revoke)
 * 3. Role & Permission Control
 * 4. Approval Requests
 * 5. Final Preview & Approval (including Exam Results authorization)
 * 6. Access History & Security
 */
@Composable
fun SchoolOwnerControlCenterScreen(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onBack: () -> Unit,
    onOpenStaffAccounts: () -> Unit,
    onOpenLegacyApprovals: () -> Unit,
    onOpenBackupRestore: () -> Unit
) {
    if (!AcademicsV2AccessControl.isSchoolOwner(session)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Version2DesignTokens.pageBackground())
                .padding(16.dp)
                .testTag("owner_control_center_denied"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminV2HeaderRow(
                title = "School Owner – Control Center",
                subtitle = "Owner authority required",
                onBack = onBack
            )
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Restricted to School Owner",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Access denied. Only the School Owner / Master Admin may access the School Owner Control Center.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    val snapshot by managementViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val accounts by managementViewModel.accountsWithPermissions.collectAsStateWithLifecycle()
    val roomLogs by managementViewModel.visibleActivityLogs.collectAsStateWithLifecycle()
    val roomPendingApprovals by managementViewModel.pendingApprovalsCount.collectAsStateWithLifecycle()

    var activeSection by remember { mutableStateOf(SchoolOwnerSection.OWNER_PROFILE_CONTACT) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("school_owner_control_center_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "owner_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AdminV2HeaderRow(
                        title = "School Owner – Control Center",
                        subtitle = "Highest governance, staff approval & final publication control",
                        onBack = onBack
                    )
                }
                Icon(
                    imageVector = GoldCrownIcon,
                    contentDescription = "School Owner Crown",
                    tint = Version2DesignTokens.GoldIconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        item(key = "owner_section_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SchoolOwnerSection.entries.forEach { sec ->
                    FilterChip(
                        selected = activeSection == sec,
                        onClick = { activeSection = sec },
                        label = { Text("${sec.index}. ${sec.title}") },
                        modifier = Modifier.testTag(sec.testTag)
                    )
                }
            }
        }

        when (activeSection) {
            SchoolOwnerSection.OWNER_PROFILE_CONTACT -> {
                item(key = "owner_profile_editor") {
                    val savedProfile = snapshot.ownerPrivateProfile
                    var ownerName by remember(savedProfile.updatedAtMillis) { mutableStateOf(savedProfile.ownerName) }
                    var designation by remember(savedProfile.updatedAtMillis) { mutableStateOf(savedProfile.designation) }
                    var privatePhone by remember(savedProfile.updatedAtMillis) { mutableStateOf(savedProfile.privateContactPhone) }
                    var privateEmail by remember(savedProfile.updatedAtMillis) { mutableStateOf(savedProfile.privateContactEmail) }
                    var governanceNote by remember(savedProfile.updatedAtMillis) { mutableStateOf(savedProfile.governanceNote) }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.25.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_private_profile_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Private Owner Information",
                                    tint = Version2DesignTokens.GoldIconTint
                                )
                                Text(
                                    text = "Private Owner Profile & Contact (Never Public)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Stored strictly for School Owner governance inside Control Center. Never exposed on Public Home, Public Academics, or Contact pages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("Owner / Correspondent Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = designation,
                                onValueChange = { designation = it },
                                label = { Text("Official Governance Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = privatePhone,
                                onValueChange = { privatePhone = it },
                                label = { Text("Private Owner Contact Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = privateEmail,
                                onValueChange = { privateEmail = it },
                                label = { Text("Private Owner Contact Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = governanceNote,
                                onValueChange = { governanceNote = it },
                                label = { Text("Internal Governance Notes") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    managementViewModel.updateOwnerPrivateProfile(
                                        ownerName = ownerName,
                                        designation = designation,
                                        privateContactPhone = privatePhone,
                                        privateContactEmail = privateEmail,
                                        governanceNote = governanceNote
                                    )
                                },
                                modifier = Modifier.testTag("owner_save_private_profile_btn")
                            ) {
                                Text("Save Private Owner Profile")
                            }
                        }
                    }
                }
            }

            SchoolOwnerSection.STAFF_ACCESS_APPROVAL -> {
                if (snapshot.staffAccessRequests.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No staff access requests recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(items = snapshot.staffAccessRequests, key = { it.id }) { req ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_access_req_${req.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${req.staffDisplayName} (@${req.staffUsername}) — ${req.status.key}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Requested: ${req.requestedCategoryKeys.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Reason: ${req.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (req.status == V2StaffAccessRequestStatus.PENDING) {
                                        Button(
                                            onClick = {
                                                managementViewModel.reviewStaffAccessRequest(
                                                    requestId = req.id,
                                                    approve = true,
                                                    reviewNote = "Approved by School Owner"
                                                )
                                            },
                                            modifier = Modifier.testTag("owner_approve_req_${req.id}")
                                        ) { Text("Approve") }
                                        OutlinedButton(
                                            onClick = {
                                                managementViewModel.reviewStaffAccessRequest(
                                                    requestId = req.id,
                                                    approve = false,
                                                    reviewNote = "Rejected by School Owner"
                                                )
                                            },
                                            modifier = Modifier.testTag("owner_reject_req_${req.id}")
                                        ) { Text("Reject") }
                                    } else if (req.status == V2StaffAccessRequestStatus.APPROVED) {
                                        OutlinedButton(
                                            onClick = {
                                                managementViewModel.revokeStaffAccessRequest(
                                                    requestId = req.id,
                                                    reviewNote = "Revoked by School Owner"
                                                )
                                            },
                                            modifier = Modifier.testTag("owner_revoke_req_${req.id}")
                                        ) { Text("Revoke Access") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SchoolOwnerSection.ROLE_PERMISSION_CONTROL -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Role & Permission Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total registered management accounts: ${accounts.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            accounts.forEach { acc ->
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${acc.account.displayName} (${AcademicsV2AccessControl.roleDisplayLabel(acc.account.role)})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Active: ${acc.account.isActive} • Permissions: ${acc.permissions.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (acc.account.role != ManagementRole.MASTER_ADMIN) {
                                        OutlinedButton(onClick = {
                                            managementViewModel.updateStaffAccount(
                                                targetAccountId = acc.account.id,
                                                displayName = acc.account.displayName,
                                                isActive = !acc.account.isActive,
                                                newPinOrNull = null,
                                                permissions = acc.permissions
                                            ) {}
                                        }) {
                                            Text(if (acc.account.isActive) "Suspend" else "Activate")
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(onClick = onOpenStaffAccounts) {
                                Text("Open Full Staff Role Editor")
                            }
                        }
                    }
                }
            }

            SchoolOwnerSection.APPROVAL_REQUESTS -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Pending Content Approval Requests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Core School Content Queue: $roomPendingApprovals pending",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(onClick = onOpenLegacyApprovals) {
                                Text("Open Core Approval Queue")
                            }
                        }
                    }
                }
                val pendingV2 = snapshot.records.filter { it.status == AcademicsV2RecordStatus.PENDING_APPROVAL }
                if (pendingV2.isEmpty()) {
                    item {
                        Text(
                            text = "No pending Version 2 Academics approval requests.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(items = pendingV2, key = { it.id }) { rec ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${rec.category.title} — ${rec.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(rec.description, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        managementViewModel.ownerApproveAndPublishV2Record(rec.id, authorizeExamResult = true)
                                    }) { Text("Approve & Publish") }
                                    OutlinedButton(onClick = {
                                        managementViewModel.updateV2RecordStatus(rec.id, AcademicsV2RecordStatus.DRAFT)
                                    }) { Text("Return to Draft") }
                                }
                            }
                        }
                    }
                }
            }

            SchoolOwnerSection.FINAL_PREVIEW_APPROVAL -> {
                val unpublishedV2 = snapshot.records.filter { it.status != AcademicsV2RecordStatus.PUBLISHED }
                if (unpublishedV2.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "All Version 2 records are currently published or no drafts exist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(items = unpublishedV2, key = { it.id }) { rec ->
                        val isExamResult = rec.category == AcademicsV2Category.EXAMINATIONS &&
                            rec.sectionKey == "exam_results"
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rec.category.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Version2DesignTokens.GoldIconTint
                                    )
                                    Version2StatusBadge(status = rec.status)
                                }
                                Text(rec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(rec.description, style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = {
                                        managementViewModel.ownerApproveAndPublishV2Record(
                                            recordId = rec.id,
                                            authorizeExamResult = true
                                        )
                                    },
                                    modifier = Modifier.testTag("owner_final_approve_${rec.id}")
                                ) {
                                    Text(
                                        if (isExamResult) "Authorize & Publish Exam Result"
                                        else "Final Owner Approve & Publish"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SchoolOwnerSection.ACCESS_HISTORY_SECURITY -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Security & Encrypted Backups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Export or restore encrypted school database backups and review governance audit trails.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onOpenBackupRestore) {
                                Text("Open Encrypted Backup & Restore")
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "Version 2 Audit Trail (${snapshot.accessAuditEvents.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(items = snapshot.accessAuditEvents.take(25), key = { it.id }) { ev ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${ev.action} • ${ev.actorName} (${ev.actorRole})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = ev.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (roomLogs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Core Portal Activity Logs (${roomLogs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(items = roomLogs.take(15), key = { it.id }) { log ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${log.actionType} • ${log.actorName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
