package com.example.ui.screens.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.AppLanguage
import com.example.ui.theme.LocalBrandPalette

enum class ParentPortalAdminModule(
    val key: String,
    val icon: ImageVector,
    val useForestAccent: Boolean,
    val supportsAdd: Boolean = true,
    val supportsEdit: Boolean = true,
    val supportsPreview: Boolean = true,
    val supportsPublish: Boolean = true,
    val defaultBulkScope: Boolean = false
) {
    STUDENT_PROFILE(
        key = "student_profile",
        icon = Icons.Outlined.Badge,
        useForestAccent = false,
        defaultBulkScope = false
    ),
    EXAMS_RESULTS(
        key = "exams_results",
        icon = Icons.Outlined.WorkspacePremium,
        useForestAccent = true,
        defaultBulkScope = true
    ),
    ATTENDANCE(
        key = "attendance",
        icon = Icons.Outlined.FactCheck,
        useForestAccent = false,
        defaultBulkScope = true
    ),
    HOMEWORK_ASSIGNMENTS(
        key = "homework_assignments",
        icon = Icons.AutoMirrored.Outlined.Assignment,
        useForestAccent = true,
        defaultBulkScope = true
    ),
    ACTIVITIES_MEDIA(
        key = "activities_media",
        icon = Icons.Outlined.SportsSoccer,
        useForestAccent = false,
        defaultBulkScope = false
    ),
    SCHOOL_COMMUNICATION(
        key = "school_communication",
        icon = Icons.Outlined.Campaign,
        useForestAccent = true,
        defaultBulkScope = true
    ),
    TIMETABLE_CALENDAR(
        key = "timetable_calendar",
        icon = Icons.Outlined.CalendarMonth,
        useForestAccent = false,
        defaultBulkScope = true
    ),
    PARENT_TEACHER_COMMUNICATION(
        key = "parent_teacher_communication",
        icon = Icons.Outlined.Forum,
        useForestAccent = true,
        defaultBulkScope = false
    ),
    LEAVE_REQUEST(
        key = "leave_request",
        icon = Icons.Outlined.EditCalendar,
        useForestAccent = false,
        supportsAdd = false,
        defaultBulkScope = false
    ),
    SCHOOL_CIRCULARS(
        key = "school_circulars",
        icon = Icons.Outlined.Description,
        useForestAccent = true,
        defaultBulkScope = true
    ),
    EMERGENCY_INFO(
        key = "emergency_info",
        icon = Icons.Outlined.Emergency,
        useForestAccent = false,
        defaultBulkScope = false
    )
}

enum class ParentAdminTargetScope {
    INDIVIDUAL_STUDENT,
    BULK_CLASS_WISE
}

@Immutable
data class ParentPortalAdminModuleCopy(
    val title: String,
    val intro: String,
    val primarySectionTitle: String,
    val primarySectionItems: List<Pair<String, String>>,
    val secondarySectionTitle: String,
    val secondarySectionDescription: String
)

@Immutable
data class ParentPortalAdminStrings(
    val sectionTitle: String,
    val sectionSubtitle: String,
    val expandDesc: String,
    val collapseDesc: String,
    val trialBadge: String,
    val trialDisclaimer: String,
    val targetScopeTitle: String,
    val targetIndividualLabel: String,
    val targetBulkClassLabel: String,
    val sampleStudentLabel: String,
    val sampleClassOptions: List<String>,
    val sampleStudentOptions: List<String>,
    val actionsTitle: String,
    val actionAdd: String,
    val actionEdit: String,
    val actionPreview: String,
    val actionPublish: String,
    val previewDialogTitle: String,
    val previewDialogBody: String,
    val trialActionDialogTitle: String,
    val trialActionDialogTemplate: String,
    val closeBtn: String,
    val backToDashboardBtn: String,
    val samplePreviewTag: String,
    val modules: Map<ParentPortalAdminModule, ParentPortalAdminModuleCopy>
)

fun parentPortalAdminStringsFor(language: AppLanguage): ParentPortalAdminStrings = when (language) {
    AppLanguage.ENGLISH -> englishParentPortalAdminStrings()
    AppLanguage.TAMIL -> tamilParentPortalAdminStrings()
    AppLanguage.HINDI -> hindiParentPortalAdminStrings()
}

fun parentPortalAdminTitle(module: ParentPortalAdminModule, language: AppLanguage): String =
    parentPortalAdminStringsFor(language).modules[module]?.title ?: module.key

@Composable
fun ParentPortalManagementExpandableHeader(
    strings: ParentPortalAdminStrings,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrandPalette.current
    Card(
        onClick = onToggleExpand,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) {
                brand.burgundy.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) brand.burgundy else brand.royalGold.copy(alpha = 0.75f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_parent_portal_mgmt_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = brand.burgundy.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FamilyRestroom,
                        contentDescription = strings.sectionTitle,
                        tint = brand.burgundy,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = strings.sectionTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.sectionSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = brand.royalGold.copy(alpha = 0.24f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(0.8.dp, brand.royalGold.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "${ParentPortalAdminModule.entries.size}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) strings.collapseDesc else strings.expandDesc,
                tint = brand.burgundy,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ParentPortalSubmenuCard(
    module: ParentPortalAdminModule,
    indexNumber: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrandPalette.current
    val accentColor = if (module.useForestAccent) brand.forestGreen else brand.burgundy

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.testTag("ppm_submenu_card_${module.key}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = module.icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    color = brand.royalGold.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = indexNumber.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentPortalAdminModuleScreen(
    module: ParentPortalAdminModule,
    currentLanguage: AppLanguage,
    onBackToDashboard: () -> Unit
) {
    val strings = remember(currentLanguage) { parentPortalAdminStringsFor(currentLanguage) }
    val copy = strings.modules.getValue(module)
    val brand = LocalBrandPalette.current
    val accentColor = if (module.useForestAccent) brand.forestGreen else brand.burgundy

    var selectedScope by rememberSaveable(module.key) {
        mutableStateOf(
            if (module.defaultBulkScope) {
                ParentAdminTargetScope.BULK_CLASS_WISE
            } else {
                ParentAdminTargetScope.INDIVIDUAL_STUDENT
            }
        )
    }
    var selectedClassIndex by rememberSaveable(module.key) { mutableStateOf(0) }
    var selectedStudentIndex by rememberSaveable(module.key) { mutableStateOf(0) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var activeTrialActionLabel by remember { mutableStateOf<String?>(null) }

    val activeTargetSummary = if (selectedScope == ParentAdminTargetScope.BULK_CLASS_WISE) {
        strings.sampleClassOptions.getOrElse(selectedClassIndex) { strings.sampleClassOptions.first() }
    } else {
        strings.sampleStudentOptions.getOrElse(selectedStudentIndex) { strings.sampleStudentOptions.first() }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .testTag("ppm_screen_${module.key}"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Module Header Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, brand.royalGold.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = accentColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = module.icon,
                                        contentDescription = copy.title,
                                        tint = accentColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = brand.royalGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.7.dp, brand.royalGold.copy(alpha = 0.65f))
                                ) {
                                    Text(
                                        text = strings.trialBadge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = copy.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = copy.intro,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.trialDisclaimer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Common Management Target Selector: Individual Student vs Bulk / Class-wise
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ppm_scope_selector_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.targetScopeTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedScope == ParentAdminTargetScope.INDIVIDUAL_STUDENT,
                                onClick = { selectedScope = ParentAdminTargetScope.INDIVIDUAL_STUDENT },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text(strings.targetIndividualLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = brand.burgundy.copy(alpha = 0.14f),
                                    selectedLabelColor = brand.burgundy,
                                    selectedLeadingIconColor = brand.burgundy
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("ppm_scope_individual")
                            )
                            FilterChip(
                                selected = selectedScope == ParentAdminTargetScope.BULK_CLASS_WISE,
                                onClick = { selectedScope = ParentAdminTargetScope.BULK_CLASS_WISE },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Groups,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text(strings.targetBulkClassLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = brand.forestGreenContainer,
                                    selectedLabelColor = brand.onForestGreenContainer,
                                    selectedLeadingIconColor = brand.onForestGreenContainer
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("ppm_scope_bulk_class")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        val options = if (selectedScope == ParentAdminTargetScope.BULK_CLASS_WISE) {
                            strings.sampleClassOptions
                        } else {
                            strings.sampleStudentOptions
                        }
                        val activeIndex = if (selectedScope == ParentAdminTargetScope.BULK_CLASS_WISE) {
                            selectedClassIndex
                        } else {
                            selectedStudentIndex
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.forEachIndexed { idx, label ->
                                FilterChip(
                                    selected = activeIndex == idx,
                                    onClick = {
                                        if (selectedScope == ParentAdminTargetScope.BULK_CLASS_WISE) {
                                            selectedClassIndex = idx
                                        } else {
                                            selectedStudentIndex = idx
                                        }
                                    },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }
            }

            // Clearly Identified Action Area: Add, Edit, Preview, Publish
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ppm_actions_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = strings.actionsTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (module.supportsAdd) {
                                Button(
                                    onClick = { activeTrialActionLabel = strings.actionAdd },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = brand.burgundy,
                                        contentColor = Color.White
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("ppm_action_add")
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.actionAdd)
                                }
                            }
                            if (module.supportsEdit) {
                                OutlinedButton(
                                    onClick = { activeTrialActionLabel = strings.actionEdit },
                                    border = BorderStroke(1.dp, brand.burgundy),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("ppm_action_edit")
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = brand.burgundy, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.actionEdit, color = brand.burgundy)
                                }
                            }
                            if (module.supportsPreview) {
                                OutlinedButton(
                                    onClick = { showPreviewDialog = true },
                                    border = BorderStroke(1.dp, brand.goldAccentText),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("ppm_action_preview")
                                ) {
                                    Icon(Icons.Outlined.Visibility, contentDescription = null, tint = brand.goldAccentText, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.actionPreview, color = brand.goldAccentText)
                                }
                            }
                            if (module.supportsPublish) {
                                Button(
                                    onClick = { activeTrialActionLabel = strings.actionPublish },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = brand.forestGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("ppm_action_publish")
                                ) {
                                    Icon(Icons.Outlined.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.actionPublish)
                                }
                            }
                        }
                    }
                }
            }

            // Initial UI Section 1 — Structured Sample Preview List
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = copy.primarySectionTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = brand.royalGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = strings.samplePreviewTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = activeTargetSummary,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
                        )

                        copy.primarySectionItems.forEachIndexed { index, (label, detail) ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index < copy.primarySectionItems.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // Initial UI Section 2 — Planned Integration & Workflow Outline
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = copy.secondarySectionTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = copy.secondarySectionDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Explicit Back to Admin Dashboard Button
            item {
                OutlinedButton(
                    onClick = onBackToDashboard,
                    border = BorderStroke(1.dp, brand.burgundy),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("ppm_back_to_dashboard_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = brand.burgundy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.backToDashboardBtn,
                        color = brand.burgundy,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("${strings.previewDialogTitle}: ${copy.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = activeTargetSummary,
                        style = MaterialTheme.typography.labelLarge,
                        color = brand.burgundy
                    )
                    Text(
                        text = strings.previewDialogBody,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPreviewDialog = false },
                    modifier = Modifier.testTag("ppm_preview_dialog_close")
                ) {
                    Text(strings.closeBtn)
                }
            },
            modifier = Modifier.testTag("ppm_preview_dialog")
        )
    }

    activeTrialActionLabel?.let { actionLabel ->
        AlertDialog(
            onDismissRequest = { activeTrialActionLabel = null },
            title = { Text("${strings.trialActionDialogTitle} — $actionLabel") },
            text = {
                Text(
                    text = strings.trialActionDialogTemplate
                        .replace("{action}", actionLabel)
                        .replace("{module}", copy.title)
                        .replace("{target}", activeTargetSummary),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { activeTrialActionLabel = null },
                    modifier = Modifier.testTag("ppm_trial_action_dialog_close")
                ) {
                    Text(strings.closeBtn)
                }
            },
            modifier = Modifier.testTag("ppm_trial_action_dialog")
        )
    }
}

private fun englishParentPortalAdminStrings() = ParentPortalAdminStrings(
    sectionTitle = "Parent Portal Management",
    sectionSubtitle = "11 Parent Portal management submenus (Trial UI Preview)",
    expandDesc = "Expand Parent Portal Management",
    collapseDesc = "Collapse Parent Portal Management",
    trialBadge = "Trial Version UI — No Live Parent Sync",
    trialDisclaimer = "Initial trial-version interface only. Real student data, guardian authentication, and parent synchronization are planned for a separate integration phase.",
    targetScopeTitle = "Selection Scope",
    targetIndividualLabel = "Individual Student",
    targetBulkClassLabel = "Bulk / Class-wise",
    sampleStudentLabel = "Sample Student",
    sampleClassOptions = listOf(
        "Class VIII · Section A (Sample)",
        "Class X · Section B (Sample)",
        "Class XII · Science (Sample)"
    ),
    sampleStudentOptions = listOf(
        "Sample Student 01 · Grade VIII-A",
        "Sample Student 02 · Grade X-B"
    ),
    actionsTitle = "Management Actions",
    actionAdd = "Add",
    actionEdit = "Edit",
    actionPreview = "Preview",
    actionPublish = "Publish",
    previewDialogTitle = "Parent Portal Preview",
    previewDialogBody = "This trial preview illustrates how content configured here will appear in the Chevalier Academy Parent Portal once live parent integration is enabled. No data is synchronized in this trial version.",
    trialActionDialogTitle = "Trial UI Action",
    trialActionDialogTemplate = "\"{action}\" for {module} ({target}) is staged as a trial UI action. Operational data entry, validation, and parent publishing workflows will be connected in the upcoming integration phase.",
    closeBtn = "Close",
    backToDashboardBtn = "Back to Admin Dashboard",
    samplePreviewTag = "Sample Preview",
    modules = mapOf(
        ParentPortalAdminModule.STUDENT_PROFILE to ParentPortalAdminModuleCopy(
            title = "Student Profile Management",
            intro = "Structure and review student profile layouts, class-section assignments, and academic year metadata prepared for the Parent Portal.",
            primarySectionTitle = "Student Profile Structure",
            primarySectionItems = listOf(
                "Student Identity & Photograph Slot" to "Placeholder layout for student display name, admission reference, and portrait photo.",
                "Class, Section & Academic Year" to "Structured grade, section, and academic session fields ready for future student records."
            ),
            secondarySectionTitle = "Individual & Class-Wise Onboarding Area",
            secondarySectionDescription = "Supports reviewing individual student cards or preparing bulk class-wise profile updates in the full integration phase."
        ),
        ParentPortalAdminModule.EXAMS_RESULTS to ParentPortalAdminModuleCopy(
            title = "Exams & Results Management",
            intro = "Organize exam terms, assessment templates, and parent-facing result report structures without fabricated student marks.",
            primarySectionTitle = "Assessment & Report Card Slots",
            primarySectionItems = listOf(
                "Term Exam Schedule Template" to "Structured slot for quarterly, half-yearly, and annual examination timetables.",
                "Subject Performance & Result Layout" to "Prepared report card layout for marks, grades, and teacher remarks once linked."
            ),
            secondarySectionTitle = "Bulk Result Preparation & Review",
            secondarySectionDescription = "Designed for class-wise result entry or individual student review with preview before publishing."
        ),
        ParentPortalAdminModule.ATTENDANCE to ParentPortalAdminModuleCopy(
            title = "Attendance Management",
            intro = "Manage daily attendance register templates, class-wise summaries, and parent attendance overview slots.",
            primarySectionTitle = "Attendance Register Layout",
            primarySectionItems = listOf(
                "Daily Class Attendance Sheet" to "Structured register view for class-wise presence, absence, and late markings.",
                "Monthly Parent Overview Slot" to "Feeds the Attendance Overview card on the Parent Dashboard after live integration."
            ),
            secondarySectionTitle = "Absence Notification Workflow Area",
            secondarySectionDescription = "Prepared area for reviewing individual student attendance corrections or bulk daily attendance sheets."
        ),
        ParentPortalAdminModule.HOMEWORK_ASSIGNMENTS to ParentPortalAdminModuleCopy(
            title = "Homework & Assignments Management",
            intro = "Prepare subject-wise homework posts, assignment instructions, and due-date notices for classes or individual students.",
            primarySectionTitle = "Homework Composer Outline",
            primarySectionItems = listOf(
                "Subject & Assignment Details" to "Structured slot for subject name, topic description, and submission due date.",
                "Worksheet & Reference Attachment Slot" to "Prepared container for teacher instructions and study materials."
            ),
            secondarySectionTitle = "Parent Dashboard Homework Feed",
            secondarySectionDescription = "Published class-wise or individual assignments will surface in Latest School Updates and Homework & Assignments."
        ),
        ParentPortalAdminModule.ACTIVITIES_MEDIA to ParentPortalAdminModuleCopy(
            title = "Student Activities & Media Management",
            intro = "Curate co-curricular clubs, sports participation, student achievements, and activity media showcases for parents.",
            primarySectionTitle = "Activities & Media Slots",
            primarySectionItems = listOf(
                "Clubs, Sports & Achievements Log" to "Structured layout for recording house events, competitions, and participation honors.",
                "Activity Photograph & Media Slot" to "Prepared preview frame for verified campus and student activity media."
            ),
            secondarySectionTitle = "Individual & Group Activity Tagging",
            secondarySectionDescription = "Supports tagging highlights for an individual student or publishing bulk class/club activity summaries."
        ),
        ParentPortalAdminModule.SCHOOL_COMMUNICATION to ParentPortalAdminModuleCopy(
            title = "School Communication Management",
            intro = "Draft and organize official school-to-parent broadcasts and targeted class or individual parent notices.",
            primarySectionTitle = "School Broadcast Templates",
            primarySectionItems = listOf(
                "General & Class-Wise Announcements" to "Structured composer for official Chevalier Academy parent updates and reminders.",
                "Priority Notice Tagging" to "Allows distinguishing routine school notices from urgent parent advisories."
            ),
            secondarySectionTitle = "Audience & Preview Controls",
            secondarySectionDescription = "Prepared for selecting class-wise audiences or individual student guardians before publishing."
        ),
        ParentPortalAdminModule.TIMETABLE_CALENDAR to ParentPortalAdminModuleCopy(
            title = "Timetable & Academic Calendar Management",
            intro = "Maintain weekly class period timetables, academic milestones, exam dates, and official school holiday calendars.",
            primarySectionTitle = "Schedule & Calendar Modules",
            primarySectionItems = listOf(
                "Weekly Class Timetable Grid" to "Structured Monday–Saturday period schedule organized by class and section.",
                "Academic Calendar & Holidays" to "Structured timeline for term dates, celebrations, PTM dates, and holidays."
            ),
            secondarySectionTitle = "Class-Wise Schedule Publishing",
            secondarySectionDescription = "Allows class-wise bulk timetable management alongside individualized elective or schedule previews."
        ),
        ParentPortalAdminModule.PARENT_TEACHER_COMMUNICATION to ParentPortalAdminModuleCopy(
            title = "Parent–Teacher Communication Management",
            intro = "Manage structured teacher remarks, parent meeting schedules, and two-way communication guidelines.",
            primarySectionTitle = "Communication Channels Outline",
            primarySectionItems = listOf(
                "Class Teacher & Subject Remarks" to "Structured slot for periodic academic progress notes and parent feedback.",
                "Parent–Teacher Meeting (PTM) Slots" to "Prepared layout for scheduling individual parent conferences or class-wise PTM windows."
            ),
            secondarySectionTitle = "Moderation & Visibility Controls",
            secondarySectionDescription = "Trial interface outlining individual guardian thread review and class-wise teacher announcements."
        ),
        ParentPortalAdminModule.LEAVE_REQUEST to ParentPortalAdminModuleCopy(
            title = "Leave Request Management",
            intro = "Review incoming parent-submitted student leave applications, approval states, and class teacher remarks.",
            primarySectionTitle = "Leave Applications Queue",
            primarySectionItems = listOf(
                "Pending Review Queue (Sample Slot)" to "Displays incoming student leave dates, reason summary, and guardian contact context.",
                "Approval Status & Remarks Area" to "Prepared controls for Pending, Approved, and Returned leave decisions."
            ),
            secondarySectionTitle = "Attendance Cross-Reference Area",
            secondarySectionDescription = "Supports reviewing individual student leave requests or filtering bulk class-wise absence requests."
        ),
        ParentPortalAdminModule.SCHOOL_CIRCULARS to ParentPortalAdminModuleCopy(
            title = "School Circulars Management",
            intro = "Prepare official numbered school circulars, policy notices, and event letters distributed to parents.",
            primarySectionTitle = "Official Circular Register",
            primarySectionItems = listOf(
                "Circular Title, Reference & Date" to "Structured official circular header aligned with Chevalier Academy communications.",
                "Document / Circular Preview Area" to "Prepared slot for formal notice body and downloadable circular attachments."
            ),
            secondarySectionTitle = "Distribution & Publishing Scope",
            secondarySectionDescription = "Supports class-wise bulk circular distribution or targeted individual student circular notices."
        ),
        ParentPortalAdminModule.EMERGENCY_INFO to ParentPortalAdminModuleCopy(
            title = "Emergency Information Management",
            intro = "Maintain verified school emergency desk contacts, transport helplines, and student emergency contact fields.",
            primarySectionTitle = "Emergency Contact Directory",
            primarySectionItems = listOf(
                "Official School Emergency Desk" to "Verified campus office phone, medical room, and transport coordinator contacts.",
                "Student Emergency & Medical Slot" to "Prepared structure for guardian emergency phone numbers and medical notes."
            ),
            secondarySectionTitle = "Urgent Advisory Readiness",
            secondarySectionDescription = "Prepared for verifying individual student emergency contacts or updating class-wise emergency instructions."
        )
    )
)

private fun tamilParentPortalAdminStrings(): ParentPortalAdminStrings {
    val base = englishParentPortalAdminStrings()
    return base.copy(
        sectionTitle = "பெற்றோர் தளம் நிர்வாகம்",
        sectionSubtitle = "11 பெற்றோர் தளம் நிர்வாக துணைப் பிரிவுகள் (சோதனை UI)",
        expandDesc = "பெற்றோர் தளம் நிர்வாகத்தை விரி",
        collapseDesc = "பெற்றோர் தளம் நிர்வாகத்தை சுருக்கு",
        trialBadge = "சோதனை பதிப்பு UI — நேரடி பெற்றோர் ஒத்திசைவு இல்லை",
        trialDisclaimer = "இது ஆரம்ப சோதனை பதிப்பு UI மட்டுமே. உண்மையான மாணவர் தரவு மற்றும் பெற்றோர் ஒத்திசைவு அடுத்த ஒருங்கிணைப்பு கட்டத்தில் இணைக்கப்படும்.",
        targetScopeTitle = "தேர்வு வரம்பு",
        targetIndividualLabel = "தனிப்பட்ட மாணவர்",
        targetBulkClassLabel = "வகுப்பு / மொத்தத் தேர்வு",
        actionsTitle = "நிர்வாகச் செயல்கள்",
        actionAdd = "சேர்",
        actionEdit = "திருத்து",
        actionPreview = "முன்னோட்டம்",
        actionPublish = "வெளியிடு",
        previewDialogTitle = "பெற்றோர் தளம் முன்னோட்டம்",
        trialActionDialogTitle = "சோதனை UI செயல்",
        closeBtn = "மூடு",
        backToDashboardBtn = "நிர்வாக முகப்புக்குத் திரும்பு",
        samplePreviewTag = "மாதிரி முன்னோட்டம்",
        modules = mapOf(
            ParentPortalAdminModule.STUDENT_PROFILE to base.modules.getValue(ParentPortalAdminModule.STUDENT_PROFILE).copy(
                title = "மாணவர் சுயவிவர நிர்வாகம்"
            ),
            ParentPortalAdminModule.EXAMS_RESULTS to base.modules.getValue(ParentPortalAdminModule.EXAMS_RESULTS).copy(
                title = "தேர்வுகள் & முடிவுகள் நிர்வாகம்"
            ),
            ParentPortalAdminModule.ATTENDANCE to base.modules.getValue(ParentPortalAdminModule.ATTENDANCE).copy(
                title = "வருகைப் பதிவு நிர்வாகம்"
            ),
            ParentPortalAdminModule.HOMEWORK_ASSIGNMENTS to base.modules.getValue(ParentPortalAdminModule.HOMEWORK_ASSIGNMENTS).copy(
                title = "வீட்டுப்பாடம் & பணிகள் நிர்வாகம்"
            ),
            ParentPortalAdminModule.ACTIVITIES_MEDIA to base.modules.getValue(ParentPortalAdminModule.ACTIVITIES_MEDIA).copy(
                title = "மாணவர் செயல்பாடுகள் & ஊடக நிர்வாகம்"
            ),
            ParentPortalAdminModule.SCHOOL_COMMUNICATION to base.modules.getValue(ParentPortalAdminModule.SCHOOL_COMMUNICATION).copy(
                title = "பள்ளித் தொடர்பு நிர்வாகம்"
            ),
            ParentPortalAdminModule.TIMETABLE_CALENDAR to base.modules.getValue(ParentPortalAdminModule.TIMETABLE_CALENDAR).copy(
                title = "கால அட்டவணை & கல்வி நாட்காட்டி நிர்வாகம்"
            ),
            ParentPortalAdminModule.PARENT_TEACHER_COMMUNICATION to base.modules.getValue(ParentPortalAdminModule.PARENT_TEACHER_COMMUNICATION).copy(
                title = "பெற்றோர்–ஆசிரியர் தொடர்பு நிர்வாகம்"
            ),
            ParentPortalAdminModule.LEAVE_REQUEST to base.modules.getValue(ParentPortalAdminModule.LEAVE_REQUEST).copy(
                title = "விடுப்பு விண்ணப்ப நிர்வாகம்"
            ),
            ParentPortalAdminModule.SCHOOL_CIRCULARS to base.modules.getValue(ParentPortalAdminModule.SCHOOL_CIRCULARS).copy(
                title = "பள்ளி சுற்றறிக்கை நிர்வாகம்"
            ),
            ParentPortalAdminModule.EMERGENCY_INFO to base.modules.getValue(ParentPortalAdminModule.EMERGENCY_INFO).copy(
                title = "அவசரத் தகவல் நிர்வாகம்"
            )
        )
    )
}

private fun hindiParentPortalAdminStrings(): ParentPortalAdminStrings {
    val base = englishParentPortalAdminStrings()
    return base.copy(
        sectionTitle = "अभिभावक पोर्टल प्रबंधन",
        sectionSubtitle = "11 अभिभावक पोर्टल प्रबंधन उप-मेनू (ट्रायल UI)",
        expandDesc = "अभिभावक पोर्टल प्रबंधन खोलें",
        collapseDesc = "अभिभावक पोर्टल प्रबंधन बंद करें",
        trialBadge = "ट्रायल संस्करण UI — कोई लाइव सिंक नहीं",
        trialDisclaimer = "यह केवल प्रारंभिक ट्रायल संस्करण UI है। वास्तविक छात्र डेटा और अभिभावक सिंक्रनाइज़ेशन अगले एकीकरण चरण में जोड़े जाएँगे।",
        targetScopeTitle = "चयन दायरा",
        targetIndividualLabel = "व्यक्तिगत छात्र",
        targetBulkClassLabel = "थोक / कक्षा-वार",
        actionsTitle = "प्रबंधन क्रियाएँ",
        actionAdd = "जोड़ें",
        actionEdit = "संपादित करें",
        actionPreview = "पूर्वावलोकन",
        actionPublish = "प्रकाशित करें",
        previewDialogTitle = "अभिभावक पोर्टल पूर्वावलोकन",
        trialActionDialogTitle = "ट्रायल UI क्रिया",
        closeBtn = "बंद करें",
        backToDashboardBtn = "एडमिन डैशबोर्ड पर लौटें",
        samplePreviewTag = "नमूना पूर्वावलोकन",
        modules = mapOf(
            ParentPortalAdminModule.STUDENT_PROFILE to base.modules.getValue(ParentPortalAdminModule.STUDENT_PROFILE).copy(
                title = "छात्र प्रोफ़ाइल प्रबंधन"
            ),
            ParentPortalAdminModule.EXAMS_RESULTS to base.modules.getValue(ParentPortalAdminModule.EXAMS_RESULTS).copy(
                title = "परीक्षा एवं परिणाम प्रबंधन"
            ),
            ParentPortalAdminModule.ATTENDANCE to base.modules.getValue(ParentPortalAdminModule.ATTENDANCE).copy(
                title = "उपस्थिति प्रबंधन"
            ),
            ParentPortalAdminModule.HOMEWORK_ASSIGNMENTS to base.modules.getValue(ParentPortalAdminModule.HOMEWORK_ASSIGNMENTS).copy(
                title = "गृहकार्य एवं असाइनमेंट प्रबंधन"
            ),
            ParentPortalAdminModule.ACTIVITIES_MEDIA to base.modules.getValue(ParentPortalAdminModule.ACTIVITIES_MEDIA).copy(
                title = "छात्र गतिविधियाँ एवं मीडिया प्रबंधन"
            ),
            ParentPortalAdminModule.SCHOOL_COMMUNICATION to base.modules.getValue(ParentPortalAdminModule.SCHOOL_COMMUNICATION).copy(
                title = "विद्यालय संचार प्रबंधन"
            ),
            ParentPortalAdminModule.TIMETABLE_CALENDAR to base.modules.getValue(ParentPortalAdminModule.TIMETABLE_CALENDAR).copy(
                title = "समय-सारणी एवं शैक्षणिक कैलेंडर प्रबंधन"
            ),
            ParentPortalAdminModule.PARENT_TEACHER_COMMUNICATION to base.modules.getValue(ParentPortalAdminModule.PARENT_TEACHER_COMMUNICATION).copy(
                title = "अभिभावक–शिक्षक संवाद प्रबंधन"
            ),
            ParentPortalAdminModule.LEAVE_REQUEST to base.modules.getValue(ParentPortalAdminModule.LEAVE_REQUEST).copy(
                title = "अवकाश अनुरोध प्रबंधन"
            ),
            ParentPortalAdminModule.SCHOOL_CIRCULARS to base.modules.getValue(ParentPortalAdminModule.SCHOOL_CIRCULARS).copy(
                title = "विद्यालय परिपत्र प्रबंधन"
            ),
            ParentPortalAdminModule.EMERGENCY_INFO to base.modules.getValue(ParentPortalAdminModule.EMERGENCY_INFO).copy(
                title = "आपातकालीन जानकारी प्रबंधन"
            )
        )
    )
}
