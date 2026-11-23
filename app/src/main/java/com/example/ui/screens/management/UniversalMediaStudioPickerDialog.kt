package com.example.ui.screens.management

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureFullScreenDialogProperties
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class SelectableStudioVersion(
    val uriString: String,
    val label: String,
    val isOriginal: Boolean,
    val isAdjusted: Boolean,
    val readable: Boolean
)

private data class StudioPickerEntry(
    val item: MediaStudioItemEntity,
    val thumbnailUri: String?,
    val versions: List<SelectableStudioVersion>,
    val hasAnyReadable: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversalMediaStudioPickerDialog(
    mStrings: ManagementStrings,
    mediaStudioItems: List<MediaStudioItemEntity>,
    onSelectUri: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var entries by remember { mutableStateOf<List<StudioPickerEntry>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imageItems = remember(mediaStudioItems) {
        mediaStudioItems.filter { it.mediaKind.equals("IMAGE", ignoreCase = true) }
    }

    LaunchedEffect(imageItems) {
        isLoading = true
        entries = withContext(Dispatchers.IO) {
            val adjustedDir = File(context.filesDir, "chevalier_media/adjusted")
            val sidecarFiles = if (adjustedDir.exists()) {
                adjustedDir.listFiles()?.filter { it.isFile && it.name.endsWith(".orig") }.orEmpty()
            } else {
                emptyList()
            }

            imageItems.map { item ->
                val versions = mutableListOf<SelectableStudioVersion>()
                val origReadable = mediaManager.isManagedMediaFileReadable(item.originalUri)
                versions += SelectableStudioVersion(
                    uriString = item.originalUri,
                    label = mStrings.mediaUi.studioPickerUseOriginalBtn,
                    isOriginal = true,
                    isAdjusted = false,
                    readable = origReadable
                )

                val seenUris = mutableSetOf(item.originalUri)

                val optUri = item.optimizedUri?.takeIf { it.isNotBlank() && it !in seenUris }
                if (optUri != null) {
                    seenUris += optUri
                    val isAdjPath = optUri.contains("chevalier_media/adjusted")
                    val optReadable = mediaManager.isManagedMediaFileReadable(optUri)
                    versions += SelectableStudioVersion(
                        uriString = optUri,
                        label = if (isAdjPath) mStrings.mediaUi.studioPickerUseAdjustedBtn else mStrings.mediaUi.studioPickerUseOptimizedBtn,
                        isOriginal = false,
                        isAdjusted = isAdjPath,
                        readable = optReadable
                    )
                }

                // Discover any adjusted crop/rotated copies derived from this original image
                val adjustedDerivatives = sidecarFiles.mapNotNull { sidecar ->
                    val recordedOrig = runCatching { sidecar.readText().trim() }.getOrNull()
                    if (recordedOrig == item.originalUri) {
                        val imageFile = File(adjustedDir, sidecar.name.removeSuffix(".orig"))
                        val adjUri = Uri.fromFile(imageFile).toString()
                        if (adjUri !in seenUris && mediaManager.isManagedMediaFileReadable(adjUri)) {
                            adjUri to imageFile.lastModified()
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }.sortedByDescending { it.second }

                adjustedDerivatives.forEachIndexed { idx, (adjUri, _) ->
                    seenUris += adjUri
                    val suffix = if (adjustedDerivatives.size > 1 || versions.any { it.isAdjusted }) {
                        " #${idx + 1}"
                    } else {
                        ""
                    }
                    versions += SelectableStudioVersion(
                        uriString = adjUri,
                        label = "${mStrings.mediaUi.studioPickerUseAdjustedBtn}$suffix",
                        isOriginal = false,
                        isAdjusted = true,
                        readable = true
                    )
                }

                val preferredThumb = versions.firstOrNull { it.readable && !it.isOriginal }?.uriString
                    ?: versions.firstOrNull { it.readable }?.uriString

                StudioPickerEntry(
                    item = item,
                    thumbnailUri = preferredThumb,
                    versions = versions,
                    hasAnyReadable = versions.any { it.readable }
                )
            }
        }
        isLoading = false
    }

    val filteredEntries = remember(entries, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            entries
        } else {
            entries.filter { entry ->
                entry.item.title.lowercase().contains(q) ||
                    entry.item.categoryOrAlbum.lowercase().contains(q) ||
                    entry.item.caption.orEmpty().lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = SecureFullScreenDialogProperties
    ) {
        ApplySecureDialogWindow()
        Surface(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(14.dp)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .heightIn(min = 320.dp, max = 680.dp)
                .testTag("universal_media_studio_picker_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = brand.burgundy
                        )
                        Text(
                            text = mStrings.mediaUi.studioPickerTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("studio_picker_close_btn")
                    ) {
                        Text(mStrings.closeBtn)
                    }
                }

                errorMessage?.let { err ->
                    ErrorBanner(err, dismissLabel = mStrings.dismissBtn) { errorMessage = null }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text(mStrings.mediaUi.studioPickerSearchHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_picker_search_input")
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = brand.burgundy)
                        }
                    }
                    filteredEntries.isEmpty() -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_picker_empty_state")
                        ) {
                            Text(
                                text = mStrings.mediaUi.studioPickerEmpty,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .testTag("studio_picker_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredEntries, key = { it.item.id }) { entry ->
                                val item = entry.item
                                val adjustedCount = entry.versions.count { it.isAdjusted && it.readable }
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        if (entry.hasAnyReadable) {
                                            MaterialTheme.colorScheme.outlineVariant
                                        } else {
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("studio_picker_item_${item.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(68.dp)
                                            ) {
                                                if (entry.thumbnailUri != null) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(entry.thumbnailUri)
                                                            .size(256)
                                                            .crossfade(false)
                                                            .build(),
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(RoundedCornerShape(10.dp))
                                                    )
                                                } else {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.BrokenImage,
                                                            contentDescription = mStrings.mediaUi.badgeUnavailableLabel,
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.categoryOrAlbum} • ${item.widthPx}x${item.heightPx}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (adjustedCount > 0) {
                                                    Text(
                                                        text = mStrings.studioPickerAdjustedVersionsCount(adjustedCount),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = brand.forestGreen
                                                    )
                                                }
                                                if (!entry.hasAnyReadable) {
                                                    Text(
                                                        text = mStrings.mediaUi.studioPickerUnavailableHint,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            entry.versions.forEachIndexed { versionIndex, version ->
                                                val tagKind = when {
                                                    version.isOriginal -> "original"
                                                    version.isAdjusted -> "adjusted_$versionIndex"
                                                    else -> "optimized_$versionIndex"
                                                }
                                                if (version.isAdjusted) {
                                                    Button(
                                                        onClick = {
                                                            if (version.readable && mediaManager.isManagedMediaFileReadable(version.uriString)) {
                                                                onSelectUri(version.uriString)
                                                            } else {
                                                                errorMessage = mStrings.mediaUi.missingOrUnreadableError
                                                            }
                                                        },
                                                        enabled = version.readable,
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = brand.burgundy,
                                                            contentColor = Color.White
                                                        ),
                                                        modifier = Modifier
                                                            .minimumInteractiveComponentSize()
                                                            .testTag("studio_picker_select_${item.id}_$tagKind")
                                                    ) {
                                                        Text(version.label)
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (version.readable && mediaManager.isManagedMediaFileReadable(version.uriString)) {
                                                                onSelectUri(version.uriString)
                                                            } else {
                                                                errorMessage = mStrings.mediaUi.missingOrUnreadableError
                                                            }
                                                        },
                                                        enabled = version.readable,
                                                        modifier = Modifier
                                                            .minimumInteractiveComponentSize()
                                                            .testTag("studio_picker_select_${item.id}_$tagKind")
                                                    ) {
                                                        Text(version.label)
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
    }
}
