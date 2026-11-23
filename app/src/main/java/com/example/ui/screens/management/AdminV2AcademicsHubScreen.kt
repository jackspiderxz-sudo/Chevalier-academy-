package com.example.ui.screens.management

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.PermissionAction
import com.example.data.v2.AcademicsV2AccessControl
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AcademicsV2Record
import com.example.data.v2.AcademicsV2RecordStatus
import com.example.data.v2.V2FacultyProfileRecord
import com.example.data.v2.V2GalleryAlbumRecord
import com.example.data.v2.V2GalleryPhotoRecord
import com.example.ui.components.Version2DesignTokens
import com.example.ui.components.Version2PremiumGridCard
import com.example.ui.components.Version2StatusBadge
import com.example.ui.components.styleForAcademicsCategory
import com.example.ui.viewmodel.ManagementViewModel
import java.util.concurrent.TimeUnit

/**
 * Admin Card 03 – Academics Management Hub (PRIMARY PRIORITY).
 * Displays ALL 13 identical Academics categories connected to the Public Academics Page.
 */
@Composable
fun AdminV2AcademicsManagementHubScreen(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onBack: () -> Unit,
    onSelectCategory: (AcademicsV2Category) -> Unit
) {
    val snapshot by managementViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val categoryPairs = remember { AcademicsV2Category.entries.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_academics_hub"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "admin_academics_header") {
            AdminV2HeaderRow(
                title = "Academics Management",
                subtitle = "All 13 Public Academics categories — draft, preview & publish workflow",
                onBack = onBack
            )
        }

        items(
            items = categoryPairs,
            key = { pair -> pair.joinToString("_") { it.routeId } }
        ) { rowCats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCats.forEach { category ->
                    val canView = AcademicsV2AccessControl.hasCategoryPermission(
                        session,
                        category,
                        PermissionAction.VIEW
                    )
                    val totalCount = when (category) {
                        AcademicsV2Category.GALLERY -> snapshot.galleryAlbums.size
                        AcademicsV2Category.FACULTY_STAFF -> snapshot.facultyProfiles.size +
                            snapshot.records.count { it.category == category }
                        else -> snapshot.records.count { it.category == category }
                    }
                    Version2PremiumGridCard(
                        title = category.title,
                        subtitle = if (canView) "${category.sections.size} sections" else "No assigned access",
                        iconStyle = styleForAcademicsCategory(category),
                        badgeText = if (canView && totalCount > 0) "$totalCount" else null,
                        isLocked = !canView,
                        onClick = { onSelectCategory(category) },
                        testTagValue = "admin_v2_academics_cat_${category.routeId}",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCats.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Unified Category Editor for any of the 13 Academics V2 categories.
 * Enforces RBAC route guard, Draft-by-default saving, preview, approval/publish controls,
 * Photo-only multi-album Gallery (Card 07), Exam Result Owner authorization (Card 09),
 * and 8-field privacy-guarded Faculty & Staff profiles (Card 11).
 */
@Composable
fun AdminV2CategoryManagementScreen(
    category: AcademicsV2Category,
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onBack: () -> Unit
) {
    val canView = AcademicsV2AccessControl.hasCategoryPermission(session, category, PermissionAction.VIEW)
    if (!canView) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Version2DesignTokens.pageBackground())
                .padding(16.dp)
                .testTag("admin_v2_access_denied"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminV2HeaderRow(
                title = category.title,
                subtitle = "Access restricted",
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
                        contentDescription = "Access denied",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "You do not have permission to view or manage ${category.title}. Contact the School Owner to request access.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    when (category) {
        AcademicsV2Category.GALLERY -> AdminV2PhotoGalleryManager(
            session = session,
            managementViewModel = managementViewModel,
            onBack = onBack
        )
        AcademicsV2Category.FACULTY_STAFF -> AdminV2FacultyStaffManager(
            session = session,
            managementViewModel = managementViewModel,
            onBack = onBack
        )
        else -> AdminV2StandardCategoryManager(
            category = category,
            session = session,
            managementViewModel = managementViewModel,
            onBack = onBack
        )
    }
}

@Composable
private fun AdminV2StandardCategoryManager(
    category: AcademicsV2Category,
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onBack: () -> Unit
) {
    val snapshot by managementViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val isOwner = AcademicsV2AccessControl.isSchoolOwner(session)
    val canCreate = AcademicsV2AccessControl.hasCategoryPermission(session, category, PermissionAction.CREATE)
    val canEdit = AcademicsV2AccessControl.hasCategoryPermission(session, category, PermissionAction.EDIT)
    val canDelete = AcademicsV2AccessControl.hasCategoryPermission(session, category, PermissionAction.DELETE)

    var selectedSectionKey by remember(category) { mutableStateOf(category.sections.first().key) }
    var editingRecord by remember { mutableStateOf<AcademicsV2Record?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var previewRecord by remember { mutableStateOf<AcademicsV2Record?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val sectionRecords = remember(snapshot, category, selectedSectionKey) {
        snapshot.records
            .filter { it.category == category && it.sectionKey == selectedSectionKey }
            .sortedByDescending { it.updatedAtMillis }
    }
    val selectedSection = remember(category, selectedSectionKey) {
        category.findSection(selectedSectionKey) ?: category.sections.first()
    }

    if (showCreateDialog || editingRecord != null) {
        V2RecordEditorDialog(
            category = category,
            initialSectionKey = editingRecord?.sectionKey ?: selectedSectionKey,
            existing = editingRecord,
            onDismiss = {
                showCreateDialog = false
                editingRecord = null
            },
            onSave = { sectionKey, title, desc, imageUri, attachmentUri, attachmentLabel, publishDate, expiryMillis, submitApproval ->
                managementViewModel.saveV2Record(
                    existingId = editingRecord?.id,
                    category = category,
                    sectionKey = sectionKey,
                    title = title,
                    description = desc,
                    imageUri = imageUri,
                    attachmentUri = attachmentUri,
                    attachmentLabel = attachmentLabel,
                    publishDateText = publishDate,
                    expiryDateMillis = expiryMillis,
                    submitForApproval = submitApproval
                ) {
                    showCreateDialog = false
                    editingRecord = null
                }
            }
        )
    }

    previewRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { previewRecord = null },
            title = { Text("Public Preview — ${selectedSection.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Version2StatusBadge(status = rec.status)
                    Text(rec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!rec.publishDateText.isNullOrBlank()) {
                        Text("Publish Date: ${rec.publishDateText}", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(rec.description, style = MaterialTheme.typography.bodyMedium)
                    if (!rec.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = rec.imageUri,
                            contentDescription = rec.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    if (!rec.attachmentLabel.isNullOrBlank()) {
                        Text("Attachment: ${rec.attachmentLabel}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewRecord = null }) { Text("Close") }
            }
        )
    }

    pendingDeleteId?.let { idToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete Record?") },
            text = { Text("This permanently deletes this content item from ${category.title}.") },
            confirmButton = {
                Button(onClick = {
                    managementViewModel.deleteV2Record(idToDelete)
                    pendingDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_category_${category.routeId}"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminV2HeaderRow(
                title = category.title,
                subtitle = "Saves as Draft (Unpublished) until explicitly published or approved",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                category.sections.forEach { sec ->
                    FilterChip(
                        selected = sec.key == selectedSectionKey,
                        onClick = { selectedSectionKey = sec.key },
                        label = { Text(sec.title) },
                        modifier = Modifier.testTag("admin_v2_section_chip_${sec.key}")
                    )
                }
            }
        }

        if (selectedSection.requiresOwnerAuthorizationToPublish) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Version2DesignTokens.OwnerGoldBorderBrush),
                    color = Version2DesignTokens.cardContainerColor(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Exam Results Policy: Exam results become publicly visible ONLY after explicit School Owner authorization and publication.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (canCreate) {
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_v2_add_record_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add Entry to ${selectedSection.title}")
                }
            }
        }

        if (sectionRecords.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No entries added in ${selectedSection.title} yet. New entries are saved as Draft (Unpublished) for review before publishing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(items = sectionRecords, key = { it.id }) { record ->
                val canPublish = AcademicsV2AccessControl.canPublishDirectly(
                    session,
                    category,
                    record.sectionKey
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_v2_record_card_${record.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Version2StatusBadge(status = record.status)
                            Row {
                                IconButton(
                                    onClick = { previewRecord = record },
                                    modifier = Modifier.testTag("admin_v2_preview_record_${record.id}")
                                ) {
                                    Icon(Icons.Filled.Visibility, contentDescription = "Preview")
                                }
                                if (canEdit) {
                                    IconButton(
                                        onClick = { editingRecord = record },
                                        modifier = Modifier.testTag("admin_v2_edit_record_${record.id}")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                }
                                if (canDelete) {
                                    IconButton(
                                        onClick = { pendingDeleteId = record.id },
                                        modifier = Modifier.testTag("admin_v2_delete_record_${record.id}")
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }

                        Text(
                            text = record.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = record.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (record.status != AcademicsV2RecordStatus.PUBLISHED) {
                                if (selectedSection.requiresOwnerAuthorizationToPublish) {
                                    if (isOwner) {
                                        Button(
                                            onClick = {
                                                managementViewModel.ownerApproveAndPublishV2Record(
                                                    recordId = record.id,
                                                    authorizeExamResult = true
                                                )
                                            },
                                            modifier = Modifier.testTag("admin_v2_owner_publish_${record.id}")
                                        ) {
                                            Text("Owner Authorize & Publish")
                                        }
                                    } else if (canEdit && record.status != AcademicsV2RecordStatus.PENDING_APPROVAL) {
                                        OutlinedButton(
                                            onClick = { managementViewModel.submitV2RecordForApproval(record.id) },
                                            modifier = Modifier.testTag("admin_v2_submit_approval_${record.id}")
                                        ) {
                                            Text("Submit for Owner Approval")
                                        }
                                    }
                                } else if (canPublish) {
                                    Button(
                                        onClick = { managementViewModel.publishV2Record(record.id) },
                                        modifier = Modifier.testTag("admin_v2_publish_record_${record.id}")
                                    ) {
                                        Text("Publish")
                                    }
                                } else if (canEdit && record.status != AcademicsV2RecordStatus.PENDING_APPROVAL) {
                                    OutlinedButton(
                                        onClick = { managementViewModel.submitV2RecordForApproval(record.id) },
                                        modifier = Modifier.testTag("admin_v2_submit_approval_${record.id}")
                                    ) {
                                        Text("Submit for Approval")
                                    }
                                }
                            } else {
                                if (canPublish || isOwner) {
                                    OutlinedButton(
                                        onClick = {
                                            managementViewModel.updateV2RecordStatus(
                                                record.id,
                                                AcademicsV2RecordStatus.UNPUBLISHED
                                            )
                                        },
                                        modifier = Modifier.testTag("admin_v2_unpublish_record_${record.id}")
                                    ) {
                                        Text("Unpublish")
                                    }
                                }
                            }
                            if ((canPublish || isOwner) && record.status != AcademicsV2RecordStatus.ARCHIVED) {
                                OutlinedButton(
                                    onClick = {
                                        managementViewModel.updateV2RecordStatus(
                                            record.id,
                                            AcademicsV2RecordStatus.ARCHIVED
                                        )
                                    },
                                    modifier = Modifier.testTag("admin_v2_archive_record_${record.id}")
                                ) {
                                    Text("Archive")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2RecordEditorDialog(
    category: AcademicsV2Category,
    initialSectionKey: String,
    existing: AcademicsV2Record?,
    onDismiss: () -> Unit,
    onSave: (
        sectionKey: String,
        title: String,
        description: String,
        imageUri: String?,
        attachmentUri: String?,
        attachmentLabel: String?,
        publishDateText: String?,
        expiryDateMillis: Long?,
        submitForApproval: Boolean
    ) -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var imageUri by remember(existing) { mutableStateOf(existing?.imageUri.orEmpty()) }
    var attachmentUri by remember(existing) { mutableStateOf(existing?.attachmentUri.orEmpty()) }
    var attachmentLabel by remember(existing) { mutableStateOf(existing?.attachmentLabel.orEmpty()) }
    var publishDateText by remember(existing) { mutableStateOf(existing?.publishDateText.orEmpty()) }
    var expiryDaysText by remember(existing) { mutableStateOf("") }
    var submitForApproval by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) imageUri = uri.toString()
    }
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
            if (attachmentLabel.isBlank()) attachmentLabel = "Attached Document"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "New ${category.title} Entry" else "Edit ${category.title} Entry")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("v2_editor_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("v2_editor_desc_input")
                )
                OutlinedTextField(
                    value = publishDateText,
                    onValueChange = { publishDateText = it },
                    label = { Text("Publish Date (e.g., 2026-06-15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (category == AcademicsV2Category.NEWS_ANNOUNCEMENTS) {
                    OutlinedTextField(
                        value = expiryDaysText,
                        onValueChange = { expiryDaysText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Auto-expire after days (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text(if (imageUri.isBlank()) "Attach Photo" else "Change Photo")
                    }
                    OutlinedButton(
                        onClick = { docPicker.launch(arrayOf("application/pdf", "image/*")) }
                    ) {
                        Text(if (attachmentUri.isBlank()) "Attach File" else "Change File")
                    }
                }
                if (attachmentUri.isNotBlank()) {
                    OutlinedTextField(
                        value = attachmentLabel,
                        onValueChange = { attachmentLabel = it },
                        label = { Text("Attachment Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Submit for Owner Final Approval",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = submitForApproval, onCheckedChange = { submitForApproval = it })
                }
                Text(
                    text = "Saving keeps content unpublished (Draft / Pending Approval) until explicitly published.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val expiryMillis = expiryDaysText.toLongOrNull()?.takeIf { it > 0 }?.let { days ->
                        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)
                    } ?: existing?.expiryDateMillis
                    onSave(
                        initialSectionKey,
                        title,
                        description,
                        imageUri.takeIf { it.isNotBlank() },
                        attachmentUri.takeIf { it.isNotBlank() },
                        attachmentLabel.takeIf { it.isNotBlank() },
                        publishDateText.takeIf { it.isNotBlank() },
                        expiryMillis,
                        submitForApproval
                    )
                },
                modifier = Modifier.testTag("v2_editor_save_btn")
            ) {
                Text("Save Unpublished")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dedicated Photo-Only Gallery Manager for Card 07.
 * Supports Album Create/Edit/Delete, Photo Upload (ImageOnly — NO video!),
 * Multi-Album Photo Reuse, and Publish/Unpublish/Archive.
 */
@Composable
private fun AdminV2PhotoGalleryManager(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onBack: () -> Unit
) {
    val snapshot by managementViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val canCreate = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.GALLERY, PermissionAction.CREATE)
    val canEdit = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.GALLERY, PermissionAction.EDIT)
    val canDelete = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.GALLERY, PermissionAction.DELETE)
    val canPublish = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.GALLERY, PermissionAction.PUBLISH)

    var selectedSectionKey by remember { mutableStateOf(AcademicsV2Category.GALLERY.sections.first().key) }
    var editingAlbum by remember { mutableStateOf<V2GalleryAlbumRecord?>(null) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var showUploadPhotoDialog by remember { mutableStateOf(false) }
    var reusePhotoTarget by remember { mutableStateOf<V2GalleryPhotoRecord?>(null) }

    if (showCreateAlbumDialog || editingAlbum != null) {
        var title by remember(editingAlbum) { mutableStateOf(editingAlbum?.title.orEmpty()) }
        var desc by remember(editingAlbum) { mutableStateOf(editingAlbum?.description.orEmpty()) }
        var dateText by remember(editingAlbum) { mutableStateOf(editingAlbum?.dateText.orEmpty()) }

        AlertDialog(
            onDismissRequest = {
                showCreateAlbumDialog = false
                editingAlbum = null
            },
            title = { Text(if (editingAlbum == null) "Create Photo Album" else "Edit Photo Album") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Album Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    managementViewModel.saveV2GalleryAlbum(
                        existingId = editingAlbum?.id,
                        sectionKey = editingAlbum?.sectionKey ?: selectedSectionKey,
                        title = title,
                        description = desc,
                        dateText = dateText
                    ) {
                        showCreateAlbumDialog = false
                        editingAlbum = null
                    }
                }) { Text("Save Album") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateAlbumDialog = false
                    editingAlbum = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showUploadPhotoDialog) {
        var photoTitle by remember { mutableStateOf("") }
        var photoDesc by remember { mutableStateOf("") }
        var photoDate by remember { mutableStateOf("") }
        var photoUri by remember { mutableStateOf("") }
        val chosenAlbums = remember { mutableStateListOf<String>() }

        val imageOnlyPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) photoUri = uri.toString()
        }

        AlertDialog(
            onDismissRequest = { showUploadPhotoDialog = false },
            title = { Text("Upload Photo (Multi-Album Reuse)") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Photos only — video uploads are not supported.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            imageOnlyPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (photoUri.isBlank()) "Select Photo from Device" else "Change Selected Photo")
                    }
                    OutlinedTextField(
                        value = photoTitle,
                        onValueChange = { photoTitle = it },
                        label = { Text("Photo Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = photoDesc,
                        onValueChange = { photoDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = photoDate,
                        onValueChange = { photoDate = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Assign to one or more albums:", style = MaterialTheme.typography.labelLarge)
                    snapshot.galleryAlbums.forEach { album ->
                        val checked = album.id in chosenAlbums
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) chosenAlbums.add(album.id)
                                    else chosenAlbums.remove(album.id)
                                }
                            )
                            Text(album.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    managementViewModel.saveV2GalleryPhoto(
                        existingId = null,
                        title = photoTitle,
                        description = photoDesc,
                        dateText = photoDate,
                        imageUri = photoUri,
                        albumIds = chosenAlbums.toList()
                    ) {
                        showUploadPhotoDialog = false
                    }
                }) { Text("Save Photo Draft") }
            },
            dismissButton = {
                TextButton(onClick = { showUploadPhotoDialog = false }) { Text("Cancel") }
            }
        )
    }

    reusePhotoTarget?.let { photo ->
        val selectedAlbumIds = remember(photo) { mutableStateListOf<String>().apply { addAll(photo.albumIds) } }
        AlertDialog(
            onDismissRequest = { reusePhotoTarget = null },
            title = { Text("Reuse Photo Across Albums") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select all albums where '${photo.title}' should appear:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    snapshot.galleryAlbums.forEach { album ->
                        val checked = album.id in selectedAlbumIds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedAlbumIds.add(album.id)
                                    else selectedAlbumIds.remove(album.id)
                                }
                            )
                            Text(album.title)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    managementViewModel.assignV2GalleryPhotoToAlbums(photo.id, selectedAlbumIds.toList())
                    reusePhotoTarget = null
                }) { Text("Apply Albums") }
            },
            dismissButton = {
                TextButton(onClick = { reusePhotoTarget = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_gallery_manager"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminV2HeaderRow(
                title = "Gallery Management (Photos Only)",
                subtitle = "Albums, photo uploads & multi-album photo reuse — no video uploads",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AcademicsV2Category.GALLERY.sections.forEach { sec ->
                    FilterChip(
                        selected = sec.key == selectedSectionKey,
                        onClick = { selectedSectionKey = sec.key },
                        label = { Text(sec.title) }
                    )
                }
            }
        }

        if (canCreate) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCreateAlbumDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_v2_create_album_btn")
                    ) {
                        Text("Create Album")
                    }
                    OutlinedButton(
                        onClick = { showUploadPhotoDialog = true },
                        enabled = snapshot.galleryAlbums.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_v2_upload_photo_btn")
                    ) {
                        Text("Upload Photo")
                    }
                }
            }
        }

        item {
            Text(
                text = "Photo Albums (${snapshot.galleryAlbums.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(items = snapshot.galleryAlbums, key = { it.id }) { album ->
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Version2StatusBadge(status = album.status)
                        Row {
                            if (canEdit) {
                                IconButton(onClick = { editingAlbum = album }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit album")
                                }
                            }
                            if (canDelete) {
                                IconButton(onClick = { managementViewModel.deleteV2GalleryAlbum(album.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete album")
                                }
                            }
                        }
                    }
                    Text(album.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (album.description.isNotBlank()) {
                        Text(album.description, style = MaterialTheme.typography.bodySmall)
                    }
                    if (canPublish) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (album.status != AcademicsV2RecordStatus.PUBLISHED) {
                                Button(onClick = {
                                    managementViewModel.updateV2GalleryAlbumStatus(album.id, AcademicsV2RecordStatus.PUBLISHED)
                                }) { Text("Publish") }
                            } else {
                                OutlinedButton(onClick = {
                                    managementViewModel.updateV2GalleryAlbumStatus(album.id, AcademicsV2RecordStatus.UNPUBLISHED)
                                }) { Text("Unpublish") }
                            }
                            OutlinedButton(onClick = {
                                managementViewModel.updateV2GalleryAlbumStatus(album.id, AcademicsV2RecordStatus.ARCHIVED)
                            }) { Text("Archive") }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Text(
                text = "Uploaded Photos (${snapshot.galleryPhotos.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(items = snapshot.galleryPhotos, key = { it.id }) { photo ->
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Version2StatusBadge(status = photo.status)
                        if (canDelete) {
                            IconButton(onClick = { managementViewModel.deleteV2GalleryPhoto(photo.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete photo")
                            }
                        }
                    }
                    Text(photo.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Used in ${photo.albumIds.size} album(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canEdit) {
                            OutlinedButton(onClick = { reusePhotoTarget = photo }) {
                                Text("Reuse in Albums")
                            }
                        }
                        if (canPublish) {
                            if (photo.status != AcademicsV2RecordStatus.PUBLISHED) {
                                Button(onClick = {
                                    managementViewModel.updateV2GalleryPhotoStatus(photo.id, AcademicsV2RecordStatus.PUBLISHED)
                                }) { Text("Publish") }
                            } else {
                                OutlinedButton(onClick = {
                                    managementViewModel.updateV2GalleryPhotoStatus(photo.id, AcademicsV2RecordStatus.UNPUBLISHED)
                                }) { Text("Unpublish") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Faculty & Staff Manager for Card 11.
 * Enforces strict privacy rules: ONLY the 8 school-approved public fields + explicit approval & consent gates.
 */
@Composable
private fun AdminV2FacultyStaffManager(
    session: AccountWithPermissions,
    managementViewModel: ManagementViewModel,
    onBack: () -> Unit
) {
    val snapshot by managementViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val canCreate = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.FACULTY_STAFF, PermissionAction.CREATE)
    val canEdit = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.FACULTY_STAFF, PermissionAction.EDIT)
    val canDelete = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.FACULTY_STAFF, PermissionAction.DELETE)
    val canPublish = AcademicsV2AccessControl.hasCategoryPermission(session, AcademicsV2Category.FACULTY_STAFF, PermissionAction.PUBLISH)

    var selectedSectionKey by remember { mutableStateOf(AcademicsV2Category.FACULTY_STAFF.sections.first().key) }
    var editingProfile by remember { mutableStateOf<V2FacultyProfileRecord?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog || editingProfile != null) {
        var name by remember(editingProfile) { mutableStateOf(editingProfile?.teacherName.orEmpty()) }
        var photoUri by remember(editingProfile) { mutableStateOf(editingProfile?.approvedProfilePhotoUri.orEmpty()) }
        var designation by remember(editingProfile) { mutableStateOf(editingProfile?.designation.orEmpty()) }
        var department by remember(editingProfile) { mutableStateOf(editingProfile?.department.orEmpty()) }
        var subjects by remember(editingProfile) { mutableStateOf(editingProfile?.teachingSubjects.orEmpty()) }
        var qualifications by remember(editingProfile) { mutableStateOf(editingProfile?.educationalQualifications.orEmpty()) }
        var experience by remember(editingProfile) { mutableStateOf(editingProfile?.teachingExperience.orEmpty()) }
        var intro by remember(editingProfile) { mutableStateOf(editingProfile?.professionalIntroduction.orEmpty()) }
        var schoolApproved by remember(editingProfile) { mutableStateOf(editingProfile?.isSchoolApproved ?: false) }
        var staffConsent by remember(editingProfile) { mutableStateOf(editingProfile?.hasStaffConsent ?: false) }

        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) photoUri = uri.toString()
        }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingProfile = null
            },
            title = { Text(if (editingProfile == null) "Add Faculty / Staff Profile" else "Edit Faculty / Staff Profile") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Privacy Policy: Enter ONLY school-approved public academic info. Never enter private mobile, personal email, home address, DOB, ID, or family details.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("1. Teacher Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text(if (photoUri.isBlank()) "2. Pick School-Approved Photo" else "2. Change Approved Photo")
                    }
                    OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("3. Designation *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("4. Department") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subjects, onValueChange = { subjects = it }, label = { Text("5. Teaching Subjects") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = qualifications, onValueChange = { qualifications = it }, label = { Text("6. Educational Qualifications") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = experience, onValueChange = { experience = it }, label = { Text("7. Teaching Experience") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = intro, onValueChange = { intro = it }, label = { Text("8. Professional Introduction") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("School Approved for Public Display", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = schoolApproved, onCheckedChange = { schoolApproved = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Staff Member Consent Recorded", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = staffConsent, onCheckedChange = { staffConsent = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    managementViewModel.saveV2FacultyProfile(
                        existingId = editingProfile?.id,
                        sectionKey = editingProfile?.sectionKey ?: selectedSectionKey,
                        teacherName = name,
                        approvedProfilePhotoUri = photoUri.takeIf { it.isNotBlank() },
                        designation = designation,
                        department = department,
                        teachingSubjects = subjects,
                        educationalQualifications = qualifications,
                        teachingExperience = experience,
                        professionalIntroduction = intro,
                        isSchoolApproved = schoolApproved,
                        hasStaffConsent = staffConsent
                    ) {
                        showCreateDialog = false
                        editingProfile = null
                    }
                }) { Text("Save Draft") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    editingProfile = null
                }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("admin_v2_faculty_manager"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminV2HeaderRow(
                title = "Faculty & Staff Profiles",
                subtitle = "Strict 8-field public directory — requires school approval & staff consent",
                onBack = onBack
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AcademicsV2Category.FACULTY_STAFF.sections.forEach { sec ->
                    FilterChip(
                        selected = sec.key == selectedSectionKey,
                        onClick = { selectedSectionKey = sec.key },
                        label = { Text(sec.title) }
                    )
                }
            }
        }
        if (canCreate) {
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_v2_add_faculty_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add Approved Staff Profile")
                }
            }
        }

        val filtered = snapshot.facultyProfiles.filter { it.sectionKey == selectedSectionKey }
        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No staff profiles added in this section yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(items = filtered, key = { it.id }) { profile ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
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
                            Version2StatusBadge(status = profile.status)
                            Row {
                                if (canEdit) {
                                    IconButton(onClick = { editingProfile = profile }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit staff profile")
                                    }
                                }
                                if (canDelete) {
                                    IconButton(onClick = { managementViewModel.deleteV2FacultyProfile(profile.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete staff profile")
                                    }
                                }
                            }
                        }
                        Text(profile.teacherName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${profile.designation} • ${profile.department}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Approval: ${if (profile.isSchoolApproved) "Yes" else "No"} • Consent: ${if (profile.hasStaffConsent) "Yes" else "No"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (canPublish) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (profile.status != AcademicsV2RecordStatus.PUBLISHED) {
                                    Button(
                                        enabled = profile.isSchoolApproved && profile.hasStaffConsent,
                                        onClick = {
                                            managementViewModel.updateV2FacultyProfileStatus(
                                                profile.id,
                                                AcademicsV2RecordStatus.PUBLISHED
                                            )
                                        }
                                    ) { Text("Publish Publicly") }
                                } else {
                                    OutlinedButton(onClick = {
                                        managementViewModel.updateV2FacultyProfileStatus(
                                            profile.id,
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
}
