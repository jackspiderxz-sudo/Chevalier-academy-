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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.v2.AcademicsV2Category
import com.example.data.v2.AcademicsV2Record
import com.example.data.v2.AcademicsV2SectionSpec
import com.example.data.v2.PublicFacultyProfile
import com.example.data.v2.V2GalleryAlbumRecord
import com.example.data.v2.V2GalleryPhotoRecord
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.Version2DesignTokens
import com.example.ui.components.styleForAcademicsCategory
import com.example.ui.viewmodel.SchoolDataViewModel

private const val PRIMARY_VERTICAL_OPTION_COUNT = 5

/**
 * Academy Category Detail Screen opened from any of the 12 cards on the Academy Page.
 * Preserves the agreed section hierarchy with five primary vertical navigation options
 * (plus any additional agreed sections in the category) and displays ONLY published records
 * with clean empty states and zero fake data.
 */
@Composable
fun AcademyCategoryDetailScreen(
    category: AcademicsV2Category,
    schoolDataViewModel: SchoolDataViewModel
) {
    val v2Snapshot by schoolDataViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val schoolProfile by schoolDataViewModel.schoolProfile.collectAsStateWithLifecycle()
    val academicInfo by schoolDataViewModel.academicInfo.collectAsStateWithLifecycle()
    val courses by schoolDataViewModel.higherSecondaryCourses.collectAsStateWithLifecycle()
    val facilities by schoolDataViewModel.facilities.collectAsStateWithLifecycle()
    val transport by schoolDataViewModel.transportRoutes.collectAsStateWithLifecycle()
    val news by schoolDataViewModel.newsItems.collectAsStateWithLifecycle()
    val announcements by schoolDataViewModel.announcements.collectAsStateWithLifecycle()
    val roomAlbums by schoolDataViewModel.galleryAlbums.collectAsStateWithLifecycle()

    val isDark = isSystemInDarkTheme()
    val iconStyle = remember(category) { styleForAcademicsCategory(category) }
    val primaryOptions = remember(category) { category.sections.take(PRIMARY_VERTICAL_OPTION_COUNT) }
    val additionalSections = remember(category) { category.sections.drop(PRIMARY_VERTICAL_OPTION_COUNT) }

    var selectedSectionKey by rememberSaveable(category.routeId) {
        mutableStateOf(primaryOptions.firstOrNull()?.key ?: "")
    }
    var lightboxPhoto by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedFacultyProfile by remember { mutableStateOf<PublicFacultyProfile?>(null) }

    val now = remember(v2Snapshot) { System.currentTimeMillis() }
    val publicV2Records = remember(v2Snapshot, category, now) {
        v2Snapshot.records
            .filter { it.category == category && it.isVisiblePublicly(now) }
            .sortedByDescending { it.updatedAtMillis }
    }
    val publicFaculty = remember(v2Snapshot, category) {
        if (category == AcademicsV2Category.FACULTY_STAFF) {
            v2Snapshot.facultyProfiles
                .mapNotNull { it.toPublicProfileOrNull() }
                .sortedBy { it.teacherName.lowercase() }
        } else {
            emptyList()
        }
    }

    lightboxPhoto?.let { (photoUri, photoTitle) ->
        AcademyCategoryPhotoLightbox(
            imageUri = photoUri,
            title = photoTitle,
            onDismiss = { lightboxPhoto = null }
        )
    }

    selectedFacultyProfile?.let { faculty ->
        AcademyFacultyPublicProfileDialog(
            profile = faculty,
            onDismiss = { selectedFacultyProfile = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("academy_category_detail_${category.routeId}"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category Identity Header Card
        item(key = "academy_category_header") {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.25.dp, Version2DesignTokens.BlueVioletBorderBrush),
                colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) iconStyle.pastelDark else iconStyle.pastelLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconStyle.icon,
                            contentDescription = category.title,
                            tint = iconStyle.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("academy_category_detail_title")
                        )
                        Text(
                            text = "${primaryOptions.size} primary sections" +
                                if (additionalSections.isNotEmpty()) " • ${additionalSections.size} more" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (category == AcademicsV2Category.GALLERY) {
            item(key = "academy_gallery_photo_only_notice") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Version2DesignTokens.cardContainerColor(),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Photo Gallery",
                            tint = Version2DesignTokens.GradientViolet
                        )
                        Text(
                            text = "Photo Gallery — published school photo albums only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Five Primary Vertical Navigation Options
        items(
            items = primaryOptions.withIndex().toList(),
            key = { (_, section) -> "primary_${section.key}" }
        ) { (index, section) ->
            AcademyVerticalNavSectionCard(
                category = category,
                section = section,
                optionIndex = index,
                isExpanded = selectedSectionKey == section.key,
                onToggle = {
                    selectedSectionKey = if (selectedSectionKey == section.key) "" else section.key
                },
                publicV2Records = publicV2Records,
                publicFaculty = publicFaculty,
                allPublishedV2Albums = v2Snapshot.galleryAlbums.filter { it.isVisiblePublicly },
                allPublishedV2Photos = v2Snapshot.galleryPhotos.filter { it.isVisiblePublicly },
                schoolProfile = schoolProfile,
                academicInfo = academicInfo,
                courses = courses,
                facilities = facilities,
                transport = transport,
                news = news,
                announcements = announcements,
                roomAlbums = roomAlbums,
                onOpenPhoto = { uri, title -> lightboxPhoto = uri to title },
                onOpenFacultyProfile = { selectedFacultyProfile = it },
                testTag = "academy_vertical_nav_option_$index"
            )
        }

        // Additional agreed hierarchy sections (if category defines >5 sections)
        if (additionalSections.isNotEmpty()) {
            item(key = "academy_additional_sections_label") {
                Text(
                    text = "More in ${category.title}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(
                items = additionalSections.withIndex().toList(),
                key = { (_, section) -> "extra_${section.key}" }
            ) { (extraIdx, section) ->
                AcademyVerticalNavSectionCard(
                    category = category,
                    section = section,
                    optionIndex = PRIMARY_VERTICAL_OPTION_COUNT + extraIdx,
                    isExpanded = selectedSectionKey == section.key,
                    onToggle = {
                        selectedSectionKey = if (selectedSectionKey == section.key) "" else section.key
                    },
                    publicV2Records = publicV2Records,
                    publicFaculty = publicFaculty,
                    allPublishedV2Albums = v2Snapshot.galleryAlbums.filter { it.isVisiblePublicly },
                    allPublishedV2Photos = v2Snapshot.galleryPhotos.filter { it.isVisiblePublicly },
                    schoolProfile = schoolProfile,
                    academicInfo = academicInfo,
                    courses = courses,
                    facilities = facilities,
                    transport = transport,
                    news = news,
                    announcements = announcements,
                    roomAlbums = roomAlbums,
                    onOpenPhoto = { uri, title -> lightboxPhoto = uri to title },
                    onOpenFacultyProfile = { selectedFacultyProfile = it },
                    testTag = "academy_vertical_nav_extra_${section.key}"
                )
            }
        }

        item(key = "academy_category_footer") {
            Spacer(modifier = Modifier.height(6.dp))
            PoweredByJkLabsFooter()
        }
    }
}

@Composable
private fun AcademyVerticalNavSectionCard(
    category: AcademicsV2Category,
    section: AcademicsV2SectionSpec,
    optionIndex: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    publicV2Records: List<AcademicsV2Record>,
    publicFaculty: List<PublicFacultyProfile>,
    allPublishedV2Albums: List<V2GalleryAlbumRecord>,
    allPublishedV2Photos: List<V2GalleryPhotoRecord>,
    schoolProfile: SchoolProfileEntity,
    academicInfo: List<AcademicInfoEntity>,
    courses: List<HigherSecondaryCourseEntity>,
    facilities: List<FacilityEntity>,
    transport: List<TransportRouteEntity>,
    news: List<NewsItemEntity>,
    announcements: List<AnnouncementEntity>,
    roomAlbums: List<GalleryAlbumWithMedia>,
    onOpenPhoto: (String, String) -> Unit,
    onOpenFacultyProfile: (PublicFacultyProfile) -> Unit,
    testTag: String
) {
    val sectionRecords = remember(publicV2Records, section.key) {
        publicV2Records.filter { it.sectionKey == section.key }
    }
    val sectionFaculty = remember(publicFaculty, section.key) {
        publicFaculty.filter { it.sectionKey == section.key }
    }
    val sectionAlbums = remember(allPublishedV2Albums, category, section.key) {
        if (category == AcademicsV2Category.GALLERY) {
            allPublishedV2Albums.filter { it.sectionKey == section.key }
        } else {
            emptyList()
        }
    }
    val legacyItems = remember(
        category, section.key, schoolProfile, academicInfo, courses, facilities, transport, news, announcements
    ) {
        collectAcademyPublishedLegacyCards(
            category = category,
            sectionKey = section.key,
            schoolProfile = schoolProfile,
            academicInfo = academicInfo,
            courses = courses,
            facilities = facilities,
            transport = transport,
            news = news,
            announcements = announcements
        )
    }
    val photoOnlyRoomAlbums = remember(category, section.key, roomAlbums) {
        if (category == AcademicsV2Category.GALLERY && section.key == "photo_gallery") {
            roomAlbums.mapNotNull { album ->
                val photos = album.mediaItems.filter { isAcademyPhotoOnly(it.mediaType, it.mediaUri) }
                if (photos.isEmpty()) null else album.album.title to photos
            }
        } else {
            emptyList()
        }
    }

    val publishedItemCount = when (category) {
        AcademicsV2Category.GALLERY -> sectionAlbums.size + photoOnlyRoomAlbums.size
        AcademicsV2Category.FACULTY_STAFF -> sectionFaculty.size + sectionRecords.size
        else -> sectionRecords.size + legacyItems.size
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            brush = Version2DesignTokens.BlueVioletBorderBrush
        ),
        colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = section.title,
                        onClick = onToggle
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Version2DesignTokens.GradientViolet.copy(alpha = 0.14f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${optionIndex + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Version2DesignTokens.GradientViolet
                        )
                    }
                }
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (publishedItemCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Version2DesignTokens.GradientBlue.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "$publishedItemCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Version2DesignTokens.GradientBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse ${section.title}" else "Expand ${section.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .testTag("academy_section_content_${section.key}"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (category) {
                        AcademicsV2Category.GALLERY -> {
                            if (sectionAlbums.isEmpty() && photoOnlyRoomAlbums.isEmpty()) {
                                Text(
                                    text = "No published photo albums in ${section.title} yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                sectionAlbums.forEach { album ->
                                    val albumPhotos = allPublishedV2Photos.filter { album.id in it.albumIds }
                                    AcademyAlbumBlock(
                                        title = album.title,
                                        subtitle = album.dateText.ifBlank { album.description },
                                        photos = albumPhotos.map { it.imageUri to it.title },
                                        onOpenPhoto = onOpenPhoto
                                    )
                                }
                                photoOnlyRoomAlbums.forEach { (albumTitle, photos) ->
                                    AcademyAlbumBlock(
                                        title = albumTitle,
                                        subtitle = "",
                                        photos = photos.map { it.mediaUri to (it.caption?.takeIf(String::isNotBlank) ?: albumTitle) },
                                        onOpenPhoto = onOpenPhoto
                                    )
                                }
                            }
                        }

                        AcademicsV2Category.FACULTY_STAFF -> {
                            if (sectionFaculty.isEmpty() && sectionRecords.isEmpty()) {
                                Text(
                                    text = "Only school-approved and staff-consented public profiles are listed here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                sectionFaculty.forEach { faculty ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenFacultyProfile(faculty) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE7F6F3)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!faculty.approvedProfilePhotoUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = faculty.approvedProfilePhotoUri,
                                                        contentDescription = faculty.teacherName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.Person,
                                                        contentDescription = faculty.teacherName,
                                                        tint = Color(0xFF27736A)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = faculty.teacherName,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = listOf(faculty.designation, faculty.department)
                                                        .filter { it.isNotBlank() }
                                                        .joinToString(" • "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "View",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = Version2DesignTokens.GradientViolet
                                            )
                                        }
                                    }
                                }
                                sectionRecords.forEach { record ->
                                    AcademyPublishedRecordTile(record = record, onOpenPhoto = onOpenPhoto)
                                }
                            }
                        }

                        else -> {
                            if (sectionRecords.isEmpty() && legacyItems.isEmpty()) {
                                Text(
                                    text = if (section.requiresOwnerAuthorizationToPublish) {
                                        "Exam results appear here only after school authorization and publication."
                                    } else {
                                        "No published updates in ${section.title} yet."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                sectionRecords.forEach { record ->
                                    AcademyPublishedRecordTile(record = record, onOpenPhoto = onOpenPhoto)
                                }
                                legacyItems.forEach { (itemTitle, itemBody) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = itemTitle,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            if (itemBody.isNotBlank()) {
                                                Text(
                                                    text = itemBody,
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
            }
        }
    }
}

@Composable
private fun AcademyPublishedRecordTile(
    record: AcademicsV2Record,
    onOpenPhoto: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (!record.publishDateText.isNullOrBlank()) {
                Text(
                    text = "Published: ${record.publishDateText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (record.description.isNotBlank()) {
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!record.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = record.imageUri,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenPhoto(record.imageUri, record.title) }
                )
            }
            if (!record.attachmentUri.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attachment",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = record.attachmentLabel ?: "Attached school document",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AcademyAlbumBlock(
    title: String,
    subtitle: String,
    photos: List<Pair<String, String>>,
    onOpenPhoto: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (photos.isEmpty()) {
                Text(
                    text = "No published photos in this album yet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items = photos) { (uri, label) ->
                        Column(
                            modifier = Modifier
                                .width(132.dp)
                                .clickable { onOpenPhoto(uri, label) },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            if (label.isNotBlank()) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademyCategoryPhotoLightbox(
    imageUri: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close photo preview")
                    }
                }
                AsyncImage(
                    model = imageUri,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun AcademyFacultyPublicProfileDialog(
    profile: PublicFacultyProfile,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.25.dp, Version2DesignTokens.BlueVioletBorderBrush),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = profile.teacherName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close profile")
                    }
                }
                if (profile.designation.isNotBlank()) {
                    Text(text = profile.designation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (profile.department.isNotBlank()) {
                    Text(text = "Department: ${profile.department}", style = MaterialTheme.typography.bodySmall)
                }
                if (profile.teachingSubjects.isNotBlank()) {
                    Text(text = "Subjects: ${profile.teachingSubjects}", style = MaterialTheme.typography.bodySmall)
                }
                if (profile.educationalQualifications.isNotBlank()) {
                    Text(text = "Qualifications: ${profile.educationalQualifications}", style = MaterialTheme.typography.bodySmall)
                }
                if (profile.teachingExperience.isNotBlank()) {
                    Text(text = "Experience: ${profile.teachingExperience}", style = MaterialTheme.typography.bodySmall)
                }
                if (profile.professionalIntroduction.isNotBlank()) {
                    Text(text = profile.professionalIntroduction, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF1D6F3D),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Approved public profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun isAcademyPhotoOnly(mediaType: String, mediaUri: String): Boolean {
    val lowerType = mediaType.lowercase()
    val lowerUri = mediaUri.lowercase()
    if (lowerType.contains("video") || lowerType.contains("youtube")) return false
    if (lowerUri.contains("youtube.com") || lowerUri.contains("youtu.be") ||
        lowerUri.endsWith(".mp4") || lowerUri.endsWith(".mov") || lowerUri.endsWith(".webm")
    ) {
        return false
    }
    return true
}

private fun collectAcademyPublishedLegacyCards(
    category: AcademicsV2Category,
    sectionKey: String,
    schoolProfile: SchoolProfileEntity,
    academicInfo: List<AcademicInfoEntity>,
    courses: List<HigherSecondaryCourseEntity>,
    facilities: List<FacilityEntity>,
    transport: List<TransportRouteEntity>,
    news: List<NewsItemEntity>,
    announcements: List<AnnouncementEntity>
): List<Pair<String, String>> = when (category) {
    AcademicsV2Category.ACADEMICS -> {
        if (sectionKey == "academic_structure") {
            academicInfo.map { it.title to it.description }
        } else {
            emptyList()
        }
    }
    AcademicsV2Category.HIGHER_SECONDARY_COURSES -> {
        if (sectionKey == "available_groups_streams") {
            courses.map { course ->
                val detail = listOfNotNull(
                    course.subjectsSummary.takeIf { it.isNotBlank() },
                    course.eligibilityNote?.takeIf { it.isNotBlank() }
                ).joinToString("\n")
                course.streamTitle to detail
            }
        } else {
            emptyList()
        }
    }
    AcademicsV2Category.ABOUT_SCHOOL -> {
        when (sectionKey) {
            "school_profile" -> listOfNotNull(
                schoolProfile.schoolName.takeIf { it.isNotBlank() }?.let { name ->
                    val details = listOfNotNull(
                        schoolProfile.addressLine.takeIf { it.isNotBlank() },
                        schoolProfile.officeHours?.takeIf { it.isNotBlank() }
                    ).joinToString("\n")
                    name to details
                }
            )
            "history_background" -> listOfNotNull(
                schoolProfile.history?.takeIf { it.isNotBlank() }?.let { "History" to it }
            )
            "vision_mission" -> listOfNotNull(
                schoolProfile.vision?.takeIf { it.isNotBlank() }?.let { "Vision" to it },
                schoolProfile.mission?.takeIf { it.isNotBlank() }?.let { "Mission" to it }
            )
            "motto_core_values" -> listOfNotNull(
                schoolProfile.motto.takeIf { it.isNotBlank() }?.let { "Motto" to it }
            )
            "leadership_management" -> listOfNotNull(
                schoolProfile.principalName?.takeIf { it.isNotBlank() }?.let { "Principal" to it }
            )
            "recognitions_affiliations" -> listOfNotNull(
                schoolProfile.affiliationNumber?.takeIf { it.isNotBlank() }?.let { "Affiliation" to it }
            )
            else -> emptyList()
        }
    }
    AcademicsV2Category.FACILITIES -> {
        if (sectionKey == "campus_infrastructure") {
            facilities.map { it.name to it.description }
        } else {
            emptyList()
        }
    }
    AcademicsV2Category.TRANSPORT -> {
        if (sectionKey == "bus_routes") {
            transport.map { route ->
                val timings = listOfNotNull(
                    route.morningTiming?.takeIf { it.isNotBlank() },
                    route.afternoonTiming?.takeIf { it.isNotBlank() }
                ).joinToString(" - ")
                val body = listOf(route.stopsSummary, timings)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                route.routeName to body
            }
        } else {
            emptyList()
        }
    }
    AcademicsV2Category.NEWS_ANNOUNCEMENTS -> {
        when (sectionKey) {
            "latest_news" -> news.map { it.title to it.summary }
            "announcements" -> announcements.map { it.title to it.message }
            else -> emptyList()
        }
    }
    else -> emptyList()
}
