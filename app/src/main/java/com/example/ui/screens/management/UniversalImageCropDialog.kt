package com.example.ui.screens.management

import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.components.ApplySecureDialogWindow
import com.example.ui.components.SecureFullScreenDialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ChevalierApplication
import com.example.core.i18n.ManagementStrings
import com.example.data.media.CropAspectRatioPreset
import com.example.data.media.MediaAdjustmentSpec
import com.example.data.media.MediaStudioManager
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

@Composable
fun UniversalImageCropDialog(
    mStrings: ManagementStrings,
    sourceUri: String,
    initialAspectPreset: CropAspectRatioPreset = CropAspectRatioPreset.FREE,
    outputTag: String = "media_adj",
    onApply: (originalUri: String, adjustedUri: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val mediaManager = remember {
        (context.applicationContext as ChevalierApplication).container.mediaStudioManager
    }
    val scope = rememberCoroutineScope()

    val originalUri = remember(sourceUri) { mediaManager.resolveOriginalUri(sourceUri) }

    var intrinsicWidth by remember(originalUri) { mutableFloatStateOf(1600f) }
    var intrinsicHeight by remember(originalUri) { mutableFloatStateOf(1200f) }
    var isSourceReadable by remember(originalUri) { mutableStateOf(true) }

    var selectedPreset by remember(originalUri, initialAspectPreset) {
        mutableStateOf(initialAspectPreset)
    }
    var freeAspectRatio by remember(originalUri) { mutableFloatStateOf(4f / 3f) }
    var zoom by remember(originalUri) { mutableFloatStateOf(1f) }
    var panFractionX by remember(originalUri) { mutableFloatStateOf(0f) }
    var panFractionY by remember(originalUri) { mutableFloatStateOf(0f) }
    var rotationDegrees by remember(originalUri) { mutableIntStateOf(0) }
    var isSavingCrop by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(originalUri) {
        withContext(Dispatchers.IO) {
            val readable = mediaManager.isManagedMediaFileReadable(originalUri)
            val dims = if (readable) readCropOrientedDimensions(originalUri) else null
            if (!readable || dims == null) {
                isSourceReadable = false
                errorMessage = mStrings.mediaUi.missingOrUnreadableError
            } else {
                isSourceReadable = true
                intrinsicWidth = dims.first.toFloat().coerceAtLeast(1f)
                intrinsicHeight = dims.second.toFloat().coerceAtLeast(1f)
                freeAspectRatio = (intrinsicWidth / intrinsicHeight).coerceIn(0.6f, 2.2f)
            }
        }
    }

    val isQuarterTurnRotated = (rotationDegrees % 180) != 0
    val effectiveImgW = if (isQuarterTurnRotated) intrinsicHeight else intrinsicWidth
    val effectiveImgH = if (isQuarterTurnRotated) intrinsicWidth else intrinsicHeight
    val naturalRotatedAspect = (effectiveImgW / effectiveImgH).coerceIn(0.25f, 4f)

    val resolvedAspectRatio = when (selectedPreset) {
        CropAspectRatioPreset.ORIGINAL -> naturalRotatedAspect
        CropAspectRatioPreset.FREE -> freeAspectRatio.coerceIn(0.6f, 2.2f)
        else -> (selectedPreset.fixedAspectRatio ?: naturalRotatedAspect)
    }.coerceIn(0.45f, 2.4f)

    val downsampledRequest = remember(originalUri, context) {
        ImageRequest.Builder(context)
            .data(originalUri)
            .size(1280)
            .crossfade(false)
            .build()
    }

    Dialog(
        onDismissRequest = { if (!isSavingCrop) onDismiss() },
        properties = SecureFullScreenDialogProperties
    ) {
        ApplySecureDialogWindow()
        Surface(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(14.dp)
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .testTag("universal_image_crop_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = mStrings.mediaUi.cropperTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mStrings.mediaUi.cropperSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                errorMessage?.let { err ->
                    ErrorBanner(err, dismissLabel = mStrings.dismissBtn) { errorMessage = null }
                }

                // Aspect ratio preset selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CropAspectRatioPreset.entries.forEach { preset ->
                        val chipLabel = when (preset) {
                            CropAspectRatioPreset.ORIGINAL -> mStrings.mediaUi.cropperAspectOriginal
                            CropAspectRatioPreset.FREE -> mStrings.mediaUi.cropperAspectFree
                            CropAspectRatioPreset.RATIO_16_9 -> mStrings.mediaUi.cropperAspect16_9
                            CropAspectRatioPreset.RATIO_4_3 -> mStrings.mediaUi.cropperAspect4_3
                            CropAspectRatioPreset.RATIO_1_1 -> mStrings.mediaUi.cropperAspect1_1
                        }
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                panFractionX = 0f
                                panFractionY = 0f
                            },
                            label = { Text(chipLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = brand.burgundy,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("crop_aspect_${preset.name.lowercase()}")
                        )
                    }
                }

                if (selectedPreset == CropAspectRatioPreset.FREE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = mStrings.mediaUi.cropperFreeRatioLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = freeAspectRatio,
                            onValueChange = { freeAspectRatio = it.coerceIn(0.6f, 2.2f) },
                            valueRange = 0.6f..2.2f,
                            colors = SliderDefaults.colors(
                                thumbColor = brand.burgundy,
                                activeTrackColor = brand.burgundy
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("crop_free_ratio_slider")
                        )
                    }
                }

                // Interactive Crop Canvas
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, brand.burgundy),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.scrim),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(resolvedAspectRatio)
                            .clipToBounds()
                            .testTag("universal_crop_viewport")
                    ) {
                        val density = LocalDensity.current
                        val viewW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                        val viewH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                        val baseScale = max(viewW / effectiveImgW, viewH / effectiveImgH)
                        val totalScale = baseScale * zoom
                        val scaledRotatedW = effectiveImgW * totalScale
                        val scaledRotatedH = effectiveImgH * totalScale
                        val maxPanX = max(0f, (scaledRotatedW - viewW) / 2f)
                        val maxPanY = max(0f, (scaledRotatedH - viewH) / 2f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(effectiveImgW, effectiveImgH, resolvedAspectRatio) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                        val nextScale = baseScale * nextZoom
                                        val nextMaxPanX = max(0f, (effectiveImgW * nextScale - viewW) / 2f)
                                        val nextMaxPanY = max(0f, (effectiveImgH * nextScale - viewH) / 2f)

                                        val curPxX = panFractionX * nextMaxPanX + pan.x
                                        val curPxY = panFractionY * nextMaxPanY + pan.y

                                        zoom = nextZoom
                                        panFractionX = if (nextMaxPanX > 0.5f) {
                                            (curPxX / nextMaxPanX).coerceIn(-1f, 1f)
                                        } else {
                                            0f
                                        }
                                        panFractionY = if (nextMaxPanY > 0.5f) {
                                            (curPxY / nextMaxPanY).coerceIn(-1f, 1f)
                                        } else {
                                            0f
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSourceReadable) {
                                UniversalAdjustedImageLayer(
                                    imageModel = downsampledRequest,
                                    contentDescription = mStrings.mediaUi.cropperTitle,
                                    unrotatedScaledWidthPx = intrinsicWidth * totalScale,
                                    unrotatedScaledHeightPx = intrinsicHeight * totalScale,
                                    rotationDegrees = rotationDegrees,
                                    translationXPx = panFractionX * maxPanX,
                                    translationYPx = panFractionY * maxPanY
                                )
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val guideColor = Color.White.copy(alpha = 0.35f)
                                val thirdW = size.width / 3f
                                val thirdH = size.height / 3f
                                drawLine(guideColor, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth = 1f)
                                drawLine(guideColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), strokeWidth = 1f)
                                drawLine(guideColor, Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth = 1f)
                                drawLine(guideColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), strokeWidth = 1f)
                            }
                        }
                    }
                }

                // Zoom & Rotation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { zoom = (zoom - 0.25f).coerceIn(1f, 4f) },
                        enabled = !isSavingCrop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("crop_zoom_out_btn")
                    ) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = mStrings.photoAdjZoomOutDesc)
                    }
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it.coerceIn(1f, 4f) },
                        valueRange = 1f..4f,
                        enabled = !isSavingCrop,
                        colors = SliderDefaults.colors(
                            thumbColor = brand.burgundy,
                            activeTrackColor = brand.burgundy
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("crop_zoom_slider")
                    )
                    IconButton(
                        onClick = { zoom = (zoom + 0.25f).coerceIn(1f, 4f) },
                        enabled = !isSavingCrop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("crop_zoom_in_btn")
                    ) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = mStrings.photoAdjZoomInDesc)
                    }
                    Text(
                        text = mStrings.formatZoom(zoom),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = brand.burgundy
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            rotationDegrees = MediaStudioManager.rotateQuarterTurns(rotationDegrees, clockwise = false)
                            panFractionX = 0f
                            panFractionY = 0f
                        },
                        enabled = !isSavingCrop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("crop_rotate_left_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.RotateLeft,
                            contentDescription = mStrings.mediaUi.cropperRotateLeftDesc,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.mediaUi.cropperRotateLeftLabel)
                    }
                    OutlinedButton(
                        onClick = {
                            rotationDegrees = MediaStudioManager.rotateQuarterTurns(rotationDegrees, clockwise = true)
                            panFractionX = 0f
                            panFractionY = 0f
                        },
                        enabled = !isSavingCrop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("crop_rotate_right_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                            contentDescription = mStrings.mediaUi.cropperRotateRightDesc,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(mStrings.mediaUi.cropperRotateRightLabel)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${rotationDegrees}°",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("crop_rotation_label")
                    )
                }

                // Live Cropped Preview
                Text(
                    text = mStrings.mediaUi.cropperLivePreviewLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Card(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .align(Alignment.CenterHorizontally)
                        .testTag("crop_live_preview")
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(resolvedAspectRatio)
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        val density = LocalDensity.current
                        val viewW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                        val viewH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                        val baseScale = max(viewW / effectiveImgW, viewH / effectiveImgH)
                        val totalScale = baseScale * zoom
                        val maxPanX = max(0f, (effectiveImgW * totalScale - viewW) / 2f)
                        val maxPanY = max(0f, (effectiveImgH * totalScale - viewH) / 2f)

                        if (isSourceReadable) {
                            UniversalAdjustedImageLayer(
                                imageModel = downsampledRequest,
                                contentDescription = mStrings.mediaUi.cropperLivePreviewLabel,
                                unrotatedScaledWidthPx = intrinsicWidth * totalScale,
                                unrotatedScaledHeightPx = intrinsicHeight * totalScale,
                                rotationDegrees = rotationDegrees,
                                translationXPx = panFractionX * maxPanX,
                                translationYPx = panFractionY * maxPanY
                            )
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedPreset = initialAspectPreset
                            zoom = 1f
                            panFractionX = 0f
                            panFractionY = 0f
                            rotationDegrees = 0
                            freeAspectRatio = (intrinsicWidth / intrinsicHeight).coerceIn(0.6f, 2.2f)
                            errorMessage = null
                        },
                        enabled = !isSavingCrop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("crop_reset_btn")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(mStrings.photoAdjResetBtn)
                    }

                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSavingCrop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("crop_cancel_btn")
                    ) {
                        Text(mStrings.cancelBtn)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            scope.launch {
                                isSavingCrop = true
                                errorMessage = null
                                mediaManager.saveAdjustedMedia(
                                    sourceImageUri = originalUri,
                                    spec = MediaAdjustmentSpec(
                                        aspectPreset = selectedPreset,
                                        customAspectRatio = if (selectedPreset == CropAspectRatioPreset.FREE) {
                                            freeAspectRatio
                                        } else {
                                            null
                                        },
                                        zoom = zoom,
                                        panFractionX = panFractionX,
                                        panFractionY = panFractionY,
                                        rotationDegrees = rotationDegrees
                                    ),
                                    outputTag = outputTag
                                ).onSuccess { result ->
                                    isSavingCrop = false
                                    onApply(result.originalUri, result.optimizedUri)
                                }.onFailure { err ->
                                    isSavingCrop = false
                                    errorMessage = err.message ?: mStrings.photoAdjCropFailedError
                                }
                            }
                        },
                        enabled = !isSavingCrop && isSourceReadable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.burgundy,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("crop_apply_btn")
                    ) {
                        if (isSavingCrop) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(mStrings.mediaUi.cropperApplyBtn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversalAdjustedImageLayer(
    imageModel: Any,
    contentDescription: String,
    unrotatedScaledWidthPx: Float,
    unrotatedScaledHeightPx: Float,
    rotationDegrees: Int,
    translationXPx: Float,
    translationYPx: Float
) {
    val density = LocalDensity.current
    val widthDp = with(density) { unrotatedScaledWidthPx.toDp() }
    val heightDp = with(density) { unrotatedScaledHeightPx.toDp() }
    Box(
        modifier = Modifier
            .requiredSize(widthDp, heightDp)
            .graphicsLayer {
                translationX = translationXPx
                translationY = translationYPx
                rotationZ = rotationDegrees.toFloat()
            }
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun readCropOrientedDimensions(uriString: String): Pair<Int, Int>? {
    return try {
        val uri = Uri.parse(uriString)
        val path = uri.path ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val orientation = ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        ) {
            opts.outHeight to opts.outWidth
        } else {
            opts.outWidth to opts.outHeight
        }
    } catch (_: Throwable) {
        null
    }
}
