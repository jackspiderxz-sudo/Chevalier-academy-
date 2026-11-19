package com.example.ui.screens.management

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.ManagementStrings
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.media.CropAspectRatioPreset
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversalMediaField(
    mStrings: ManagementStrings,
    label: String,
    selectedUri: String?,
    onUriChanged: (String?) -> Unit,
    mediaStudioItems: List<MediaStudioItemEntity>,
    defaultAspectPreset: CropAspectRatioPreset = CropAspectRatioPreset.RATIO_4_3,
    outputTag: String = "section_media",
    enabled: Boolean = true,
    testTagPrefix: String = "universal_media"
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()

    var isImporting by remember { mutableStateOf(false) }
    var isReadable by remember(selectedUri) { mutableStateOf(true) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showStudioPicker by remember { mutableStateOf(false) }
    var cropSourceUri by remember { mutableStateOf<String?>(null) }

    val cleanUri = selectedUri?.trim()?.takeIf { it.isNotEmpty() }

    LaunchedEffect(cleanUri) {
        if (cleanUri == null) {
            isReadable = true
        } else {
            val readable = withContext(Dispatchers.IO) {
                mediaManager.isManagedMediaFileReadable(cleanUri)
            }
            isReadable = readable
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                localError = null
                mediaManager.importAndOptimizeImage(uri)
                    .onSuccess { processed ->
                        isImporting = false
                        isReadable = true
                        onUriChanged(processed.optimizedUri)
                    }
                    .onFailure { err ->
                        isImporting = false
                        localError = err.message ?: mStrings.heroImageLoadFailedError
                    }
            }
        }
    }

    if (showStudioPicker) {
        UniversalMediaStudioPickerDialog(
            mStrings = mStrings,
            mediaStudioItems = mediaStudioItems,
            onSelectUri = { picked ->
                localError = null
                isReadable = true
                showStudioPicker = false
                onUriChanged(picked)
            },
            onDismiss = { showStudioPicker = false }
        )
    }

    cropSourceUri?.let { src ->
        UniversalImageCropDialog(
            mStrings = mStrings,
            sourceUri = src,
            initialAspectPreset = defaultAspectPreset,
            outputTag = outputTag,
            onApply = { _, adjustedUri ->
                localError = null
                isReadable = true
                cropSourceUri = null
                onUriChanged(adjustedUri)
            },
            onDismiss = { cropSourceUri = null }
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_field")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (cleanUri != null) {
                    val isAdjusted = cleanUri.contains("chevalier_media/adjusted")
                    val badgeLabel = when {
                        !isReadable -> mStrings.mediaUi.badgeUnavailableLabel
                        isAdjusted -> mStrings.mediaUi.badgeAdjustedLabel
                        else -> mStrings.mediaUi.badgeOriginalLabel
                    }
                    val badgeContainer = when {
                        !isReadable -> MaterialTheme.colorScheme.errorContainer
                        isAdjusted -> brand.royalGold.copy(alpha = 0.25f)
                        else -> brand.forestGreenContainer
                    }
                    val badgeContent = when {
                        !isReadable -> MaterialTheme.colorScheme.onErrorContainer
                        isAdjusted -> MaterialTheme.colorScheme.onSurface
                        else -> brand.onForestGreenContainer
                    }
                    Surface(
                        color = badgeContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .testTag("${testTagPrefix}_status_badge")
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = badgeContent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (isImporting) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_loading")
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = brand.burgundy
                    )
                    Text(
                        text = mStrings.mediaUi.importingImageText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val displayError = localError ?: if (cleanUri != null && !isReadable) {
                mStrings.mediaUi.missingOrUnreadableError
            } else {
                null
            }
            displayError?.let { err ->
                Box(modifier = Modifier.testTag("${testTagPrefix}_error")) {
                    ErrorBanner(err, dismissLabel = mStrings.dismissBtn) {
                        localError = null
                    }
                }
            }

            if (cleanUri != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .testTag("${testTagPrefix}_preview")
                ) {
                    if (isReadable) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(cleanUri)
                                .size(960)
                                .crossfade(false)
                                .build(),
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Outlined.BrokenImage,
                                contentDescription = mStrings.mediaUi.badgeUnavailableLabel,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = mStrings.mediaUi.emptySelectionHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("${testTagPrefix}_empty_hint")
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cleanUri == null) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = enabled && !isImporting,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTagPrefix}_pick_device_btn")
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.mediaUi.pickFromDeviceBtn)
                    }
                    OutlinedButton(
                        onClick = {
                            localError = null
                            showStudioPicker = true
                        },
                        enabled = enabled && !isImporting,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTagPrefix}_pick_studio_btn")
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.mediaUi.pickFromStudioBtn)
                    }
                } else {
                    OutlinedButton(
                        onClick = { cropSourceUri = cleanUri },
                        enabled = enabled && !isImporting && isReadable,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTagPrefix}_crop_btn")
                    ) {
                        Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.mediaUi.cropAdjustBtn)
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = enabled && !isImporting,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTagPrefix}_change_btn")
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.mediaUi.replaceImageBtn)
                    }
                    OutlinedButton(
                        onClick = {
                            localError = null
                            showStudioPicker = true
                        },
                        enabled = enabled && !isImporting,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTagPrefix}_pick_studio_btn")
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.mediaUi.pickFromStudioBtn)
                    }
                    TextButton(
                        onClick = {
                            localError = null
                            onUriChanged(null)
                        },
                        enabled = enabled && !isImporting,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTagPrefix}_remove_btn")
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = mStrings.mediaUi.removeImageBtn,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = mStrings.mediaUi.removeImageBtn,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
