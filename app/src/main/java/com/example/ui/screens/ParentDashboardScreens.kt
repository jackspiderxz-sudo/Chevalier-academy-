package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.i18n.AppStrings
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.ui.components.AnnouncementDetailDialog
import com.example.ui.components.ManagedPublicImage
import com.example.ui.components.NewsDetailDialog
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.SectionHeaderText
import com.example.ui.components.VerifiedFieldRow
import com.example.ui.components.launchDialerIntent
import com.example.ui.components.launchMapsIntent
import com.example.ui.theme.BurgundyDark
import com.example.ui.theme.LocalBrandPalette

@Immutable
data class ParentStudentPreview(
    val displayName: String,
    val classAndSection: String,
    val academicYear: String,
    val photoUri: String? = null,
    val isSamplePreview: Boolean = true
)

enum class ParentModuleId(val routeKey: String) {
    STUDENT_PROFILE("student_profile"),
    EXAMS_RESULTS("exams_results"),
    ATTENDANCE("attendance"),
    HOMEWORK_ASSIGNMENTS("homework_assignments"),
    CHILD_ACTIVITIES("child_activities"),
    SCHOOL_COMMUNICATION("school_communication"),
    TIMETABLE_CALENDAR("timetable_calendar"),
    PARENT_TEACHER_COMMUNICATION("parent_teacher_communication"),
    LEAVE_REQUEST("leave_request"),
    SCHOOL_CIRCULARS("school_circulars"),
    EMERGENCY_INFO("emergency_info");

    companion object {
        fun fromRouteKey(key: String?): ParentModuleId =
            entries.firstOrNull { it.routeKey == key } ?: STUDENT_PROFILE
    }
}

private enum class ModuleAccentTone {
    BURGUNDY,
    FOREST_GREEN,
    ROYAL_GOLD
}

@Immutable
private data class ParentModuleDescriptor(
    val id: ParentModuleId,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tone: ModuleAccentTone
)

fun parentModuleTitle(moduleId: ParentModuleId, strings: AppStrings): String = when (moduleId) {
    ParentModuleId.STUDENT_PROFILE -> strings.parentModuleStudentProfile
    ParentModuleId.EXAMS_RESULTS -> strings.parentModuleExamsResults
    ParentModuleId.ATTENDANCE -> strings.parentModuleAttendance
    ParentModuleId.HOMEWORK_ASSIGNMENTS -> strings.parentModuleHomework
    ParentModuleId.CHILD_ACTIVITIES -> strings.parentModuleActivities
    ParentModuleId.SCHOOL_COMMUNICATION -> strings.parentModuleSchoolCommunication
    ParentModuleId.TIMETABLE_CALENDAR -> strings.parentModuleTimetableCalendar
    ParentModuleId.PARENT_TEACHER_COMMUNICATION -> strings.parentModuleParentTeacher
    ParentModuleId.LEAVE_REQUEST -> strings.parentModuleLeaveRequest
    ParentModuleId.SCHOOL_CIRCULARS -> strings.parentModuleSchoolCirculars
    ParentModuleId.EMERGENCY_INFO -> strings.parentModuleEmergencyInfo
}

private fun parentModuleDescriptor(
    moduleId: ParentModuleId,
    strings: AppStrings
): ParentModuleDescriptor = when (moduleId) {
    ParentModuleId.STUDENT_PROFILE -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleStudentProfile,
        description = strings.parentModuleStudentProfileDesc,
        icon = Icons.Outlined.Badge,
        tone = ModuleAccentTone.BURGUNDY
    )
    ParentModuleId.EXAMS_RESULTS -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleExamsResults,
        description = strings.parentModuleExamsResultsDesc,
        icon = Icons.Outlined.WorkspacePremium,
        tone = ModuleAccentTone.ROYAL_GOLD
    )
    ParentModuleId.ATTENDANCE -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleAttendance,
        description = strings.parentModuleAttendanceDesc,
        icon = Icons.Outlined.FactCheck,
        tone = ModuleAccentTone.FOREST_GREEN
    )
    ParentModuleId.HOMEWORK_ASSIGNMENTS -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleHomework,
        description = strings.parentModuleHomeworkDesc,
        icon = Icons.AutoMirrored.Outlined.Assignment,
        tone = ModuleAccentTone.BURGUNDY
    )
    ParentModuleId.CHILD_ACTIVITIES -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleActivities,
        description = strings.parentModuleActivitiesDesc,
        icon = Icons.Outlined.SportsSoccer,
        tone = ModuleAccentTone.FOREST_GREEN
    )
    ParentModuleId.SCHOOL_COMMUNICATION -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleSchoolCommunication,
        description = strings.parentModuleSchoolCommunicationDesc,
        icon = Icons.Outlined.Campaign,
        tone = ModuleAccentTone.BURGUNDY
    )
    ParentModuleId.TIMETABLE_CALENDAR -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleTimetableCalendar,
        description = strings.parentModuleTimetableCalendarDesc,
        icon = Icons.Outlined.CalendarMonth,
        tone = ModuleAccentTone.ROYAL_GOLD
    )
    ParentModuleId.PARENT_TEACHER_COMMUNICATION -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleParentTeacher,
        description = strings.parentModuleParentTeacherDesc,
        icon = Icons.Outlined.Forum,
        tone = ModuleAccentTone.FOREST_GREEN
    )
    ParentModuleId.LEAVE_REQUEST -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleLeaveRequest,
        description = strings.parentModuleLeaveRequestDesc,
        icon = Icons.Outlined.EditCalendar,
        tone = ModuleAccentTone.ROYAL_GOLD
    )
    ParentModuleId.SCHOOL_CIRCULARS -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleSchoolCirculars,
        description = strings.parentModuleSchoolCircularsDesc,
        icon = Icons.Outlined.Description,
        tone = ModuleAccentTone.BURGUNDY
    )
    ParentModuleId.EMERGENCY_INFO -> ParentModuleDescriptor(
        id = moduleId,
        title = strings.parentModuleEmergencyInfo,
        description = strings.parentModuleEmergencyInfoDesc,
        icon = Icons.Outlined.Emergency,
        tone = ModuleAccentTone.BURGUNDY
    )
}

@Composable
fun ParentPortalTopBar(
    isSearchActive: Boolean,
    hasNotifications: Boolean,
    onToggleSearch: () -> Unit,
    onNotificationsClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onOpenDrawerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_portal_top_header")
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.5.dp, brand.royalGold),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chevalier_emblem),
                        contentDescription = strings.parentPortalBranding,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = strings.parentPortalBranding,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = brand.goldAccentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.parentPortalTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        ),
                        color = if (brand.isDark) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            brand.burgundy
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleSearch,
                        modifier = Modifier.testTag("parent_header_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Outlined.Search,
                            contentDescription = strings.parentSearchContentDesc,
                            tint = if (isSearchActive) brand.burgundy else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.testTag("parent_header_notification_button")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = strings.parentNotificationsContentDesc,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            if (hasNotifications) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(brand.burgundy)
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("top_bar_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = strings.openTopMenuDescription,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
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
                                    menuExpanded = false
                                    onGalleryClick()
                                },
                                modifier = Modifier.testTag("top_bar_menu_gallery")
                            )
                            DropdownMenuItem(
                                text = { Text(text = strings.parentMenuDrawerOption) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenDrawerClick()
                                },
                                modifier = Modifier.testTag("top_bar_menu_drawer")
                            )
                            DropdownMenuItem(
                                text = { Text(text = strings.titleSettings) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsClick()
                                },
                                modifier = Modifier.testTag("top_bar_menu_settings")
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = brand.royalGold.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
fun ParentDashboardScreen(
    announcements: List<AnnouncementEntity>,
    newsItems: List<NewsItemEntity>,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenModule: (ParentModuleId) -> Unit,
    onOpenAllUpdates: () -> Unit,
    studentPreview: ParentStudentPreview? = null
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    val activeStudent = studentPreview ?: remember(strings) {
        ParentStudentPreview(
            displayName = strings.parentSampleStudentName,
            classAndSection = strings.parentSampleClassSectionValue,
            academicYear = strings.parentSampleAcademicYearValue,
            photoUri = null,
            isSamplePreview = true
        )
    }

    val allModules = remember(strings) {
        ParentModuleId.entries.map { parentModuleDescriptor(it, strings) }
    }

    val filteredModules = remember(allModules, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank()) {
            allModules
        } else {
            allModules.filter { module ->
                module.title.contains(trimmed, ignoreCase = true) ||
                    module.description.contains(trimmed, ignoreCase = true)
            }
        }
    }

    var selectedAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }
    var selectedNews by remember { mutableStateOf<NewsItemEntity?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val effectiveContentWidth = minOf(maxWidth, 860.dp) - 32.dp
        val moduleColumns = when {
            effectiveContentWidth >= 720.dp -> 4
            effectiveContentWidth >= 480.dp -> 3
            else -> 2
        }
        val moduleRows = remember(filteredModules, moduleColumns) {
            filteredModules.chunked(moduleColumns)
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .testTag("parent_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optional animated inline search bar controlled by Top Header Search Icon
            item {
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = strings.parentSearchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = brand.burgundy
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("parent_search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = strings.parentClearSearchDesc
                                    )
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brand.burgundy,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_dashboard_search_field")
                    )
                }
            }

            // SECTION B — STUDENT INFORMATION CARD
            item {
                StudentInformationCard(
                    student = activeStudent,
                    onClick = { onOpenModule(ParentModuleId.STUDENT_PROFILE) }
                )
            }

            // SECTION C — YOUR CHILD AT A GLANCE
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("parent_glance_section")
                ) {
                    SectionHeaderText(title = strings.parentGlanceSectionTitle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ChildGlanceOverviewCard(
                            title = strings.parentAttendanceOverviewTitle,
                            statusText = strings.parentAttendanceOverviewEmpty,
                            badgeText = strings.parentAwaitingStudentLinkBadge,
                            icon = Icons.Outlined.EventAvailable,
                            accentColor = brand.forestGreen,
                            iconContainerColor = brand.forestGreenContainer,
                            onClick = { onOpenModule(ParentModuleId.ATTENDANCE) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("parent_glance_attendance_card")
                        )
                        ChildGlanceOverviewCard(
                            title = strings.parentExamResultsOverviewTitle,
                            statusText = strings.parentExamResultsOverviewEmpty,
                            badgeText = strings.parentAwaitingStudentLinkBadge,
                            icon = Icons.Outlined.Assessment,
                            accentColor = brand.burgundy,
                            iconContainerColor = brand.burgundy.copy(alpha = 0.12f),
                            onClick = { onOpenModule(ParentModuleId.EXAMS_RESULTS) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("parent_glance_exams_card")
                        )
                    }
                }
            }

            // SECTION D — LATEST SCHOOL UPDATES
            item {
                LatestSchoolUpdatesCardSection(
                    announcements = announcements,
                    newsItems = newsItems,
                    onViewAllClick = onOpenAllUpdates,
                    onAnnouncementClick = { selectedAnnouncement = it },
                    onNewsClick = { selectedNews = it },
                    onHomeworkClick = { onOpenModule(ParentModuleId.HOMEWORK_ASSIGNMENTS) }
                )
            }

            // SECTION E — PARENT SERVICES (11 MODULES)
            item {
                SectionHeaderText(
                    title = strings.parentServicesSectionTitle,
                    modifier = Modifier.testTag("parent_services_section")
                )
            }

            if (filteredModules.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_search_empty_state")
                    ) {
                        Text(
                            text = strings.parentSearchNoResults,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                        )
                    }
                }
            } else {
                items(
                    items = moduleRows,
                    key = { row -> row.joinToString("_") { it.id.routeKey } }
                ) { rowModules ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowModules.forEach { module ->
                            ParentServiceModuleCard(
                                descriptor = module,
                                onClick = { onOpenModule(module.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 116.dp)
                                    .testTag("parent_module_card_${module.id.routeKey}")
                            )
                        }
                        repeat(moduleColumns - rowModules.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // SECTION F — FOOTER
            item {
                PoweredByJkLabsFooter(modifier = Modifier.testTag("powered_by_jk_labs_footer"))
            }
        }
    }

    selectedAnnouncement?.let { item ->
        AnnouncementDetailDialog(
            announcement = item,
            onDismiss = { selectedAnnouncement = null }
        )
    }

    selectedNews?.let { item ->
        NewsDetailDialog(
            newsItem = item,
            onDismiss = { selectedNews = null }
        )
    }
}

@Composable
private fun StudentInformationCard(
    student: ParentStudentPreview,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val goldAccent = brand.royalGold

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = brand.burgundy),
        border = BorderStroke(1.2.dp, goldAccent.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_student_info_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            brand.burgundy,
                            BurgundyDark
                        )
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = goldAccent.copy(alpha = 0.11f),
                        radius = size.minDimension * 0.65f,
                        center = Offset(size.width * 0.94f, size.height * 0.12f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.minDimension * 0.48f,
                        center = Offset(size.width * 0.08f, size.height * 0.98f)
                    )
                }
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (student.isSamplePreview) {
                        Surface(
                            color = goldAccent.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.8.dp, goldAccent.copy(alpha = 0.85f))
                        ) {
                            Text(
                                text = strings.parentSamplePreviewBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = goldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.parentModuleStudentProfile,
                        tint = goldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Student profile photograph frame
                    Surface(
                        shape = CircleShape,
                        color = brand.warmIvory,
                        border = BorderStroke(2.2.dp, goldAccent),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("parent_student_avatar")
                    ) {
                        if (!student.photoUri.isNullOrBlank()) {
                            ManagedPublicImage(
                                imageUri = student.photoUri,
                                contentDescription = strings.parentStudentPhotoDesc,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                brand.warmIvory,
                                                goldAccent.copy(alpha = 0.26f)
                                            )
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = strings.parentStudentPhotoDesc,
                                    tint = brand.burgundy,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = student.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                lineHeight = 26.sp
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = student.classAndSection,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.24f))
                        ) {
                            Text(
                                text = "${strings.parentAcademicYearLabel}: ${student.academicYear}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildGlanceOverviewCard(
    title: String,
    statusText: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    iconContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrandPalette.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconContainerColor,
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = brand.royalGold.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.6.dp, brand.royalGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun LatestSchoolUpdatesCardSection(
    announcements: List<AnnouncementEntity>,
    newsItems: List<NewsItemEntity>,
    onViewAllClick: () -> Unit,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    onNewsClick: (NewsItemEntity) -> Unit,
    onHomeworkClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val recentAnnouncements = remember(announcements) { announcements.take(2) }
    val recentNews = remember(newsItems) { newsItems.take(1) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("parent_latest_updates_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeaderText(
                title = strings.parentLatestUpdatesTitle,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onViewAllClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("parent_updates_view_all")
            ) {
                Text(
                    text = strings.viewAll,
                    style = MaterialTheme.typography.labelLarge,
                    color = brand.burgundy
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = brand.burgundy,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. School Announcements (live published data if present, else clean pending slot)
                if (recentAnnouncements.isNotEmpty()) {
                    recentAnnouncements.forEachIndexed { index, announcement ->
                        ParentUpdateRowItem(
                            categoryLabel = announcement.priorityLabel ?: strings.parentUpdateTypeAnnouncement,
                            title = announcement.title,
                            subtitle = announcement.message,
                            dateText = announcement.issuedDateText,
                            icon = Icons.Outlined.Campaign,
                            accentColor = brand.burgundy,
                            onClick = { onAnnouncementClick(announcement) },
                            modifier = Modifier.testTag("parent_update_announcement_${announcement.id}")
                        )
                        if (index < recentAnnouncements.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    ParentUpdateRowItem(
                        categoryLabel = strings.parentUpdateTypeAnnouncement,
                        title = strings.titleAnnouncements,
                        subtitle = strings.parentSchoolUpdatesEmptyNotice,
                        dateText = null,
                        icon = Icons.Outlined.Campaign,
                        accentColor = brand.burgundy,
                        onClick = onViewAllClick,
                        modifier = Modifier.testTag("parent_update_announcement_placeholder")
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 2. New Homework Notification row
                ParentUpdateRowItem(
                    categoryLabel = strings.parentUpdateTypeHomework,
                    title = strings.parentModuleHomework,
                    subtitle = strings.parentHomeworkEmptyNotice,
                    dateText = null,
                    icon = Icons.Outlined.MenuBook,
                    accentColor = brand.forestGreen,
                    onClick = onHomeworkClick,
                    modifier = Modifier.testTag("parent_update_homework_row")
                )

                // 3. Other important school updates (live published news if available)
                if (recentNews.isNotEmpty()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    val newsItem = recentNews.first()
                    ParentUpdateRowItem(
                        categoryLabel = strings.parentUpdateTypeGeneral,
                        title = newsItem.title,
                        subtitle = newsItem.summary,
                        dateText = newsItem.publishedDateText,
                        icon = Icons.Outlined.Newspaper,
                        accentColor = brand.goldAccentText,
                        onClick = { onNewsClick(newsItem) },
                        modifier = Modifier.testTag("parent_update_news_${newsItem.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentUpdateRowItem(
    categoryLabel: String,
    title: String,
    subtitle: String,
    dateText: String?,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accentColor.copy(alpha = 0.13f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = categoryLabel,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!dateText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun ParentServiceModuleCard(
    descriptor: ParentModuleDescriptor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrandPalette.current
    val (accentColor, containerColor) = when (descriptor.tone) {
        ModuleAccentTone.BURGUNDY -> brand.burgundy to brand.burgundy.copy(alpha = 0.12f)
        ModuleAccentTone.FOREST_GREEN -> brand.forestGreen to brand.forestGreenContainer
        ModuleAccentTone.ROYAL_GOLD -> brand.goldAccentText to brand.royalGold.copy(alpha = 0.20f)
    }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = containerColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = descriptor.icon,
                            contentDescription = descriptor.title,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = descriptor.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ParentModuleDetailScreen(
    moduleId: ParentModuleId,
    schoolProfile: SchoolProfileEntity,
    announcements: List<AnnouncementEntity>,
    onBackToDashboard: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenAnnouncements: () -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val descriptor = remember(moduleId, strings) { parentModuleDescriptor(moduleId, strings) }
    var selectedAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("parent_module_screen_${moduleId.routeKey}"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = brand.burgundy.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.65f)),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = descriptor.icon,
                                    contentDescription = descriptor.title,
                                    tint = brand.burgundy,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Surface(
                            color = brand.royalGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.8.dp, brand.royalGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = strings.parentModulePlaceholderBadge,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text = descriptor.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = descriptor.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Reusing verified official school announcements for School Communication & School Circulars
            if (moduleId == ParentModuleId.SCHOOL_COMMUNICATION || moduleId == ParentModuleId.SCHOOL_CIRCULARS) {
                item {
                    SectionHeaderText(title = strings.parentModuleCircularsLiveHeader)
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (announcements.isEmpty()) {
                            Text(
                                text = strings.announcementsPendingDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                announcements.take(4).forEachIndexed { index, announcement ->
                                    ParentUpdateRowItem(
                                        categoryLabel = announcement.priorityLabel ?: strings.parentUpdateTypeAnnouncement,
                                        title = announcement.title,
                                        subtitle = announcement.message,
                                        dateText = announcement.issuedDateText,
                                        icon = Icons.Outlined.Campaign,
                                        accentColor = brand.burgundy,
                                        onClick = { selectedAnnouncement = announcement }
                                    )
                                    if (index < minOf(announcements.size, 4) - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenAnnouncements,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, brand.burgundy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("parent_module_open_announcements_btn")
                    ) {
                        Text(text = strings.parentModuleOpenAnnouncementsBtn, color = brand.burgundy)
                    }
                }
            }

            // Reusing verified official school contact info for Emergency Information
            if (moduleId == ParentModuleId.EMERGENCY_INFO) {
                item {
                    SectionHeaderText(title = strings.parentModuleEmergencySchoolOfficeTitle)
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            VerifiedFieldRow(
                                label = strings.addressLabel,
                                value = schoolProfile.addressLine,
                                icon = Icons.Outlined.LocationOn
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            VerifiedFieldRow(
                                label = strings.phoneLabel,
                                value = schoolProfile.phone,
                                icon = Icons.Outlined.Phone
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            VerifiedFieldRow(
                                label = strings.emailLabel,
                                value = schoolProfile.email,
                                icon = Icons.Outlined.Email
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val verifiedPhone = schoolProfile.phone
                                if (!verifiedPhone.isNullOrBlank()) {
                                    Button(
                                        onClick = { launchDialerIntent(context, verifiedPhone) },
                                        colors = ButtonDefaults.buttonColors(containerColor = brand.burgundy),
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Text(text = strings.callActionLabel, color = Color.White)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val mapQuery = schoolProfile.mapCoordinatesOrQuery
                                            ?.takeIf { it.isNotBlank() }
                                            ?: "${schoolProfile.schoolName}, ${schoolProfile.addressLine}"
                                        launchMapsIntent(context, mapQuery)
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, brand.forestGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(text = strings.openMapsActionLabel, color = brand.forestGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.parentModuleEmergencyStudentNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenContact,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, brand.burgundy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("parent_module_open_contact_btn")
                    ) {
                        Text(text = strings.parentModuleOpenContactBtn, color = brand.burgundy)
                    }
                }
            }

            item {
                Button(
                    onClick = onBackToDashboard,
                    colors = ButtonDefaults.buttonColors(containerColor = brand.burgundy),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("parent_module_back_dashboard_btn")
                ) {
                    Text(
                        text = strings.parentModuleReturnDashboardBtn,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }

    selectedAnnouncement?.let { item ->
        AnnouncementDetailDialog(
            announcement = item,
            onDismiss = { selectedAnnouncement = null }
        )
    }
}
