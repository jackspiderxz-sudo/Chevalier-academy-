package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import com.example.data.v2.AcademicsV2Snapshot
import com.example.data.v2.PublicFacultyProfile
import com.example.data.v2.V2GalleryAlbumRecord
import com.example.data.v2.V2GalleryPhotoRecord
import com.example.ui.components.PoweredByJkLabsFooter
import com.example.ui.components.Version2DesignTokens
import com.example.ui.components.Version2PremiumGridCard
import com.example.ui.components.styleForAcademicsCategory
import com.example.ui.viewmodel.SchoolDataViewModel

fun publicTestTagForAcademicsCategory(category: AcademicsV2Category): String = when (category) {
    AcademicsV2Category.ACADEMICS -> "academics_card_academics"
    AcademicsV2Category.HIGHER_SECONDARY_COURSES -> "academics_card_courses"
    AcademicsV2Category.ABOUT_SCHOOL -> "academics_card_about"
    AcademicsV2Category.FACILITIES -> "academics_card_facilities"
    AcademicsV2Category.TRANSPORT -> "academics_card_transport"
    AcademicsV2Category.NEWS_ANNOUNCEMENTS -> "academics_card_news"
    AcademicsV2Category.GALLERY -> "academics_card_gallery"
    AcademicsV2Category.SCHOOL_TIMETABLE -> "academics_card_timetable"
    AcademicsV2Category.EXAMINATIONS -> "academics_card_examinations"
    AcademicsV2Category.ACADEMIC_CALENDAR -> "academics_card_calendar"
    AcademicsV2Category.FACULTY_STAFF -> "academics_card_faculty"
    AcademicsV2Category.STUDENT_ACTIVITIES -> "academics_card_activities"
    AcademicsV2Category.ADMISSIONS -> "academics_card_admissions"
}

/**
 * Version 2 Public Academics Page with EXACTLY 13 structured cards in a responsive 2-column grid.
 * Uses Soft Cream background, white/light rounded cards, thin Blue + Violet gradient border,
 * subtle shadow, and topic-specific pastel-accented icons.
 */
@Composable
fun AcademicsV2PublicHubScreen(
    schoolDataViewModel: SchoolDataViewModel,
    onOpenCategory: (AcademicsV2Category) -> Unit
) {
    val v2Snapshot by schoolDataViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val academicInfo by schoolDataViewModel.academicInfo.collectAsStateWithLifecycle()
    val courses by schoolDataViewModel.higherSecondaryCourses.collectAsStateWithLifecycle()
    val facilities by schoolDataViewModel.facilities.collectAsStateWithLifecycle()
    val transport by schoolDataViewModel.transportRoutes.collectAsStateWithLifecycle()
    val news by schoolDataViewModel.newsItems.collectAsStateWithLifecycle()
    val announcements by schoolDataViewModel.announcements.collectAsStateWithLifecycle()
    val roomAlbums by schoolDataViewModel.galleryAlbums.collectAsStateWithLifecycle()

    val categories = remember { AcademicsV2Category.entries }
    val categoryPairs = remember(categories) { categories.chunked(2) }
    val now = remember(v2Snapshot) { System.currentTimeMillis() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("academics_v2_hub_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "v2_academics_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Academics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Official school academic directory, schedules, photo gallery, faculty and admissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(
            items = categoryPairs,
            key = { pair -> pair.joinToString("_") { it.routeId } }
        ) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { category ->
                    val publishedCount = remember(
                        category,
                        v2Snapshot,
                        academicInfo,
                        courses,
                        facilities,
                        transport,
                        news,
                        announcements,
                        roomAlbums
                    ) {
                        countPublishedItemsForCategory(
                            category = category,
                            snapshot = v2Snapshot,
                            nowMillis = now,
                            academicInfo = academicInfo,
                            courses = courses,
                            facilities = facilities,
                            transport = transport,
                            news = news,
                            announcements = announcements,
                            roomAlbums = roomAlbums
                        )
                    }
                    val badge = if (publishedCount > 0) "$publishedCount" else null
                    Version2PremiumGridCard(
                        title = category.title,
                        subtitle = "${category.sections.size} sections",
                        iconStyle = styleForAcademicsCategory(category),
                        badgeText = badge,
                        onClick = { onOpenCategory(category) },
                        testTagValue = publicTestTagForAcademicsCategory(category),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item(key = "v2_academics_footer") {
            Spacer(modifier = Modifier.height(6.dp))
            PoweredByJkLabsFooter()
        }
    }
}

private fun countPublishedItemsForCategory(
    category: AcademicsV2Category,
    snapshot: AcademicsV2Snapshot,
    nowMillis: Long,
    academicInfo: List<AcademicInfoEntity>,
    courses: List<HigherSecondaryCourseEntity>,
    facilities: List<FacilityEntity>,
    transport: List<TransportRouteEntity>,
    news: List<NewsItemEntity>,
    announcements: List<AnnouncementEntity>,
    roomAlbums: List<GalleryAlbumWithMedia>
): Int {
    val v2RecordsCount = snapshot.records.count { it.category == category && it.isVisiblePublicly(nowMillis) }
    return when (category) {
        AcademicsV2Category.ACADEMICS -> v2RecordsCount + academicInfo.size
        AcademicsV2Category.HIGHER_SECONDARY_COURSES -> v2RecordsCount + courses.size
        AcademicsV2Category.FACILITIES -> v2RecordsCount + facilities.size
        AcademicsV2Category.TRANSPORT -> v2RecordsCount + transport.size
        AcademicsV2Category.NEWS_ANNOUNCEMENTS -> v2RecordsCount + news.size + announcements.size
        AcademicsV2Category.GALLERY -> {
            val v2Albums = snapshot.galleryAlbums.count { it.isVisiblePublicly }
            val photoOnlyRoomAlbums = roomAlbums.count { album ->
                album.mediaItems.any { isPhotoOnlyMediaType(it.mediaType, it.mediaUri) }
            }
            v2Albums + photoOnlyRoomAlbums
        }
        AcademicsV2Category.FACULTY_STAFF -> {
            v2RecordsCount + snapshot.facultyProfiles.count { it.toPublicProfileOrNull() != null }
        }
        else -> v2RecordsCount
    }
}

private fun isPhotoOnlyMediaType(mediaType: String, mediaUri: String): Boolean {
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

/**
 * Unified Public Category Detail Screen for any of the 13 Version 2 Academics cards.
 * Displays all exact agreed sub-sections and ONLY published, authorized content.
 */
@Composable
fun AcademicsV2CategoryDetailScreen(
    category: AcademicsV2Category,
    schoolDataViewModel: SchoolDataViewModel,
    onBack: () -> Unit,
    onOpenFacultyProfile: (String) -> Unit
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

    var lightboxPhoto by remember { mutableStateOf<Pair<String, String>?>(null) }

    lightboxPhoto?.let { (photoUri, photoTitle) ->
        PublicPhotoPreviewDialog(
            imageUri = photoUri,
            title = photoTitle,
            onDismiss = { lightboxPhoto = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("academics_v2_detail_${category.routeId}"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "detail_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("academics_v2_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${category.sections.size} structured sections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Photo-only Gallery dedicated public viewer (multi-album reuse, zero video gallery!)
        if (category == AcademicsV2Category.GALLERY) {
            item(key = "gallery_v2_notice") {
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

        // Render every exact agreed sub-section for this category
        items(items = category.sections, key = { it.key }) { section ->
            val sectionRecords = publicV2Records.filter { it.sectionKey == section.key }
            val sectionFaculty = publicFaculty.filter { it.sectionKey == section.key }
            val sectionAlbums = if (category == AcademicsV2Category.GALLERY) {
                v2Snapshot.galleryAlbums.filter { it.isVisiblePublicly && it.sectionKey == section.key }
            } else {
                emptyList()
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academics_v2_section_${section.key}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (category == AcademicsV2Category.GALLERY) {
                        PublicGallerySectionContent(
                            sectionKey = section.key,
                            v2Albums = sectionAlbums,
                            allPublishedV2Photos = v2Snapshot.galleryPhotos.filter { it.isVisiblePublicly },
                            roomAlbums = if (section.key == "photo_gallery") roomAlbums else emptyList(),
                            onOpenPhoto = { uri, title -> lightboxPhoto = uri to title }
                        )
                    } else if (category == AcademicsV2Category.FACULTY_STAFF) {
                        PublicFacultySectionContent(
                            profiles = sectionFaculty,
                            records = sectionRecords,
                            onOpenFacultyProfile = onOpenFacultyProfile
                        )
                    } else {
                        val legacyItems = collectPublishedLegacyCards(
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

                        if (sectionRecords.isEmpty() && legacyItems.isEmpty()) {
                            val emptyText = if (section.requiresOwnerAuthorizationToPublish) {
                                "Exam results appear here only after School Owner authorization and publication."
                            } else {
                                "No published school updates in ${section.title} yet."
                            }
                            Text(
                                text = emptyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            sectionRecords.forEach { record ->
                                PublicV2RecordCard(
                                    record = record,
                                    onOpenPhoto = { uri, title -> lightboxPhoto = uri to title }
                                )
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
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

        item(key = "detail_footer") {
            PoweredByJkLabsFooter()
        }
    }
}

@Composable
private fun PublicV2RecordCard(
    record: AcademicsV2Record,
    onOpenPhoto: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("public_v2_record_${record.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (!record.publishDateText.isNullOrBlank()) {
                Text(
                    text = "Published: ${record.publishDateText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = record.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!record.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = record.imageUri,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
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
private fun PublicGallerySectionContent(
    sectionKey: String,
    v2Albums: List<V2GalleryAlbumRecord>,
    allPublishedV2Photos: List<V2GalleryPhotoRecord>,
    roomAlbums: List<GalleryAlbumWithMedia>,
    onOpenPhoto: (String, String) -> Unit
) {
    val photoOnlyRoomAlbums = roomAlbums.mapNotNull { album ->
        val photos = album.mediaItems.filter { isPhotoOnlyMediaType(it.mediaType, it.mediaUri) }
        if (photos.isEmpty()) null else album.album.title to photos
    }

    if (v2Albums.isEmpty() && photoOnlyRoomAlbums.isEmpty()) {
        Text(
            text = "No published photo albums in this section yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    v2Albums.forEach { album ->
        val albumPhotos = allPublishedV2Photos.filter { album.id in it.albumIds }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("public_v2_album_${album.id}")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (album.dateText.isNotBlank()) {
                    Text(
                        text = album.dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (album.description.isNotBlank()) {
                    Text(
                        text = album.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (albumPhotos.isEmpty()) {
                    Text(
                        text = "No published photos in this album yet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items = albumPhotos, key = { "${album.id}_${it.id}" }) { photo ->
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable { onOpenPhoto(photo.imageUri, photo.title) }
                                    .testTag("public_v2_photo_${album.id}_${photo.id}"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AsyncImage(
                                    model = photo.imageUri,
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text(
                                    text = photo.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
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

    if (sectionKey == "photo_gallery") {
        photoOnlyRoomAlbums.forEach { (albumTitle, photos) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = albumTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items = photos, key = { it.id }) { media ->
                            val captionText = media.caption.orEmpty()
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable { onOpenPhoto(media.mediaUri, captionText.ifBlank { albumTitle }) },
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AsyncImage(
                                    model = media.mediaUri,
                                    contentDescription = captionText.ifBlank { albumTitle },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                if (captionText.isNotBlank()) {
                                    Text(
                                        text = captionText,
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
}

@Composable
private fun PublicFacultySectionContent(
    profiles: List<PublicFacultyProfile>,
    records: List<AcademicsV2Record>,
    onOpenFacultyProfile: (String) -> Unit
) {
    if (profiles.isEmpty() && records.isEmpty()) {
        Text(
            text = "Only school-approved and staff-consented public profiles are listed here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    profiles.forEach { profile ->
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFacultyProfile(profile.id) }
                .testTag("public_faculty_card_${profile.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE7F6F3)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.approvedProfilePhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.approvedProfilePhotoUri,
                            contentDescription = profile.teacherName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = profile.teacherName,
                            tint = Color(0xFF27736A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = profile.teacherName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = listOf(profile.designation, profile.department)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.teachingSubjects.isNotBlank()) {
                        Text(
                            text = "Subjects: ${profile.teachingSubjects}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "View Profile",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    records.forEach { record ->
        PublicV2RecordCard(record = record, onOpenPhoto = { _, _ -> })
    }
}

/**
 * Dedicated Detailed Profile Page for a school-approved, staff-consented Faculty & Staff member.
 * Displays ONLY the 8 approved public fields and NEVER displays private personal contact info.
 */
@Composable
fun PublicFacultyProfileDetailScreen(
    profileId: String,
    schoolDataViewModel: SchoolDataViewModel,
    onBack: () -> Unit
) {
    val snapshot by schoolDataViewModel.academicsV2Snapshot.collectAsStateWithLifecycle()
    val profile = remember(snapshot, profileId) {
        snapshot.facultyProfiles.firstOrNull { it.id == profileId }?.toPublicProfileOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Version2DesignTokens.pageBackground())
            .testTag("public_faculty_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "faculty_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("public_faculty_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Faculty & Staff Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (profile == null) {
            item(key = "faculty_unavailable") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This staff profile is currently unavailable or not published.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            item(key = "faculty_profile_card") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.25.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    colors = CardDefaults.cardColors(containerColor = Version2DesignTokens.cardContainerColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("public_faculty_detail_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE7F6F3)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!profile.approvedProfilePhotoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = profile.approvedProfilePhotoUri,
                                        contentDescription = profile.teacherName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = profile.teacherName,
                                        tint = Color(0xFF27736A),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = profile.teacherName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = profile.designation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (profile.department.isNotBlank()) {
                                    Text(
                                        text = profile.department,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        FacultyPublicFieldRow(label = "Designation", value = profile.designation)
                        FacultyPublicFieldRow(label = "Department", value = profile.department)
                        FacultyPublicFieldRow(label = "Teaching Subjects", value = profile.teachingSubjects)
                        FacultyPublicFieldRow(label = "Educational Qualifications", value = profile.educationalQualifications)
                        FacultyPublicFieldRow(label = "Teaching Experience", value = profile.teachingExperience)
                        FacultyPublicFieldRow(label = "Professional Introduction", value = profile.professionalIntroduction)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "Approved Public Profile",
                                tint = Color(0xFF1D6F3D),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "School-approved public profile (private personal information is never displayed).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacultyPublicFieldRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PublicPhotoPreviewDialog(
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close preview")
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

/**
 * Surfaces verified/published Room records alongside V2 records for Cards 01-06
 * without introducing any fake or demo school content.
 */
private fun collectPublishedLegacyCards(
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
