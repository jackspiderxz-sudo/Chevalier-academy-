package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.ui.components.AwaitingVerificationCard
import com.example.ui.components.ManagedPublicImage
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.SchoolBrandHeaderCard
import com.example.ui.components.VerifiedFieldRow
import com.example.ui.theme.LocalBrandPalette

internal data class OfficialCourseGroupSlot(
    val groupCode: String,
    val groupLabel: String,
    val title: String
)

internal val officialHigherSecondaryGroups = listOf(
    OfficialCourseGroupSlot(
        groupCode = "GROUP_1",
        groupLabel = "Group 1",
        title = "Bio-Maths"
    ),
    OfficialCourseGroupSlot(
        groupCode = "GROUP_2",
        groupLabel = "Group 2",
        title = "Computer Science"
    ),
    OfficialCourseGroupSlot(
        groupCode = "GROUP_3",
        groupLabel = "Group 3",
        title = "Commerce with Computer Applications"
    ),
    OfficialCourseGroupSlot(
        groupCode = "GROUP_4",
        groupLabel = "Group 4",
        title = "Commerce with Business Mathematics"
    )
)

@Composable
fun AboutSchoolScreen(
    profile: SchoolProfileEntity
) {
    val strings = LocalAppStrings.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("about_school_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    SchoolBrandHeaderCard(profile = profile)
                    ManagedPublicImage(
                        imageUri = profile.aboutImageUri,
                        contentDescription = profile.shortName,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(MaterialTheme.shapes.large)
                            .testTag("about_school_image")
                    )
                }
            }

            item {
                AboutSchoolDetailsCard(profile = profile)
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
internal fun AboutSchoolDetailsCard(
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
                label = strings.welcomeSectionTitle,
                value = profile.welcomeIntroduction,
                icon = Icons.Outlined.Info
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.verifiedCampusLocation,
                value = profile.addressLine,
                icon = Icons.Outlined.LocationOn
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.emblemAttributionLabel,
                value = profile.emblemMark,
                icon = Icons.Outlined.School
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.principalLabel,
                value = profile.principalName,
                icon = Icons.Outlined.Person
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.historyLabel,
                value = profile.history,
                icon = Icons.Outlined.HistoryEdu
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.visionLabel,
                value = profile.vision,
                icon = Icons.Outlined.Visibility
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerifiedFieldRow(
                label = strings.missionLabel,
                value = profile.mission,
                icon = Icons.Outlined.Flag
            )
        }
    }
}

@Composable
fun HigherSecondaryCoursesScreen(
    courses: List<HigherSecondaryCourseEntity>
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("courses_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, brand.royalGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = strings.coursesOverviewBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            items(officialHigherSecondaryGroups, key = { it.groupCode }) { slot ->
                val matched = courses.firstOrNull {
                    it.groupCode.equals(slot.groupCode, ignoreCase = true) ||
                        it.groupCode.equals(slot.groupLabel, ignoreCase = true) ||
                        it.streamTitle.contains(slot.title, ignoreCase = true)
                }
                CourseGroupSlotCard(slot = slot, verifiedCourse = matched)
            }

            // Also display any extra verified courses that don't match the 4 standard slots
            val extraCourses = courses.filter { course ->
                officialHigherSecondaryGroups.none { slot ->
                    course.groupCode.equals(slot.groupCode, ignoreCase = true) ||
                        course.groupCode.equals(slot.groupLabel, ignoreCase = true) ||
                        course.streamTitle.contains(slot.title, ignoreCase = true)
                }
            }
            items(extraCourses, key = { "extra_${it.id}" }) { extra ->
                CourseGroupSlotCard(
                    slot = OfficialCourseGroupSlot(
                        groupCode = extra.groupCode,
                        groupLabel = extra.groupCode,
                        title = extra.streamTitle
                    ),
                    verifiedCourse = extra
                )
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
internal fun CourseGroupSlotCard(
    slot: OfficialCourseGroupSlot,
    verifiedCourse: HigherSecondaryCourseEntity?,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (verifiedCourse != null) 1.2.dp else 1.dp,
            color = if (verifiedCourse != null) brand.forestGreen.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("course_group_${slot.groupCode.lowercase()}")
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
                        text = slot.groupLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = brand.burgundy,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (verifiedCourse != null && !verifiedCourse.classLevelLabel.isNullOrBlank()) {
                    Surface(
                        color = brand.forestGreenContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = verifiedCourse.classLevelLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = brand.onForestGreenContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = brand.royalGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = strings.awaitingVerificationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = verifiedCourse?.streamTitle ?: "${slot.groupLabel} — ${slot.title}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (verifiedCourse != null) {
                Text(
                    text = "${strings.subjectsLabel}: ${verifiedCourse.subjectsSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${strings.mediumLabel}: ${verifiedCourse.mediumOfInstruction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.forestGreen
                )
                verifiedCourse.eligibilityNote?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${strings.eligibilityLabel}: $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = strings.coursesPendingDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FacilitiesScreen(
    facilities: List<FacilityEntity>
) {
    val strings = LocalAppStrings.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("facilities_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (facilities.isEmpty()) {
                item {
                    AwaitingVerificationCard(
                        title = strings.titleFacilities,
                        description = strings.facilitiesPendingDesc,
                        icon = Icons.Outlined.Apartment
                    )
                }
            } else {
                items(facilities, key = { it.id }) { facility ->
                    FacilityItemCard(facility = facility)
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
internal fun FacilityItemCard(
    facility: FacilityEntity,
    modifier: Modifier = Modifier,
    showImage: Boolean = true
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showImage) {
                ManagedPublicImage(
                    imageUri = facility.imageUri,
                    contentDescription = facility.name,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("facility_item_image_${facility.id}")
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = facility.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = facility.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = facility.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            facility.capacityOrSpec?.takeIf { it.isNotBlank() }?.let { spec ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = spec,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalBrandPalette.current.forestGreen
                )
            }
        }
    }
}

@Composable
fun TransportScreen(
    routes: List<TransportRouteEntity>
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("transport_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (routes.isEmpty()) {
                item {
                    AwaitingVerificationCard(
                        title = strings.titleTransport,
                        description = strings.transportPendingDesc,
                        icon = Icons.Outlined.DirectionsBus
                    )
                }
            } else {
                items(routes, key = { it.id }) { route ->
                    TransportRouteItemCard(route = route)
                }
            }

            item { PoweredByJkLabsFooter() }
        }
    }
}

@Composable
internal fun TransportRouteItemCard(
    route: TransportRouteEntity,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = brand.royalGold.copy(alpha = 0.24f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${strings.routeNumberPrefix} ${route.routeNumber}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = route.routeName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = route.stopsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val timings = listOfNotNull(route.morningTiming, route.afternoonTiming)
            if (timings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${strings.pickupTimingLabel}: ${timings.joinToString(" • ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = brand.forestGreen
                )
            }
            route.coordinatorContact?.takeIf { it.isNotBlank() }?.let { contact ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${strings.coordinatorLabel}: $contact",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
