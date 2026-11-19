package com.example.ui.screens.management

import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ChevalierApplication
import com.example.core.i18n.ManagementStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.model.AppLanguage
import com.example.data.media.MediaStudioManager
import com.example.ui.components.HomeHeroBannerPreviewFrame
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

@Composable
fun HeroPhotoAdjustmentDialog(
    sourceUri: String,
    captionPreview: String?,
    onApply: (originalUri: String, adjustedUri: String) -> Unit,
    onDismiss: () -> Unit,
    mStrings: ManagementStrings = managementStringsFor(AppLanguage.ENGLISH)
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val mediaManager = remember { (context.applicationContext as ChevalierApplication).container.mediaStudioManager }
    val scope = rememberCoroutineScope()

    val originalUri = remember(sourceUri) { mediaManager.resolveOriginalUri(sourceUri) }

    var intrinsicWidth by remember(originalUri) { mutableFloatStateOf(1600f) }
    var intrinsicHeight by remember(originalUri) { mutableFloatStateOf(900f) }

    LaunchedEffect(originalUri) {
        withContext(Dispatchers.IO) {
            readOrientedDimensions(originalUri)?.let { (w, h) ->
                intrinsicWidth = w.toFloat().coerceAtLeast(1f)
                intrinsicHeight = h.toFloat().coerceAtLeast(1f)
            }
        }
    }

    var zoom by remember(originalUri) { mutableFloatStateOf(1f) }
    var panFractionX by remember(originalUri) { mutableFloatStateOf(0f) }
    var panFractionY by remember(originalUri) { mutableFloatStateOf(0f) }
    var isSavingCrop by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(14.dp)
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .testTag("hero_photo_adjustment_dialog"),
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
                    text = mStrings.photoAdjTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mStrings.photoAdjSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                errorMessage?.let { ErrorBanner(it, dismissLabel = mStrings.dismissBtn, onDismiss = { errorMessage = null }) }

                // Interactive Crop Canvas (matches exact 16:9 Home Hero Banner ratio)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, brand.burgundy),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(MediaStudioManager.HERO_BANNER_ASPECT_RATIO)
                            .clipToBounds()
                            .testTag("photo_adj_crop_viewport")
                    ) {
                        val density = LocalDensity.current
                        val viewW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                        val viewH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                        val baseScale = max(viewW / intrinsicWidth, viewH / intrinsicHeight)
                        val totalScale = baseScale * zoom
                        val scaledW = intrinsicWidth * totalScale
                        val scaledH = intrinsicHeight * totalScale
                        val maxPanX = max(0f, (scaledW - viewW) / 2f)
                        val maxPanY = max(0f, (scaledH - viewH) / 2f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(intrinsicWidth, intrinsicHeight) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                        val nextScale = baseScale * nextZoom
                                        val nextMaxPanX = max(0f, (intrinsicWidth * nextScale - viewW) / 2f)
                                        val nextMaxPanY = max(0f, (intrinsicHeight * nextScale - viewH) / 2f)

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
                            AdjustedImageLayer(
                                imageUri = originalUri,
                                contentDescription = mStrings.photoAdjImageDesc,
                                scaledWidthPx = scaledW,
                                scaledHeightPx = scaledH,
                                translationXPx = panFractionX * maxPanX,
                                translationYPx = panFractionY * maxPanY
                            )

                            // Subtle rule-of-thirds crop guide overlay
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

                // Zoom In / Out & Reset Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { zoom = (zoom - 0.25f).coerceIn(1f, 4f) },
                        modifier = Modifier.testTag("photo_adj_zoom_out_btn")
                    ) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = mStrings.photoAdjZoomOutDesc)
                    }
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it.coerceIn(1f, 4f) },
                        valueRange = 1f..4f,
                        colors = SliderDefaults.colors(
                            thumbColor = brand.burgundy,
                            activeTrackColor = brand.burgundy
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("photo_adj_zoom_slider")
                    )
                    IconButton(
                        onClick = { zoom = (zoom + 0.25f).coerceIn(1f, 4f) },
                        modifier = Modifier.testTag("photo_adj_zoom_in_btn")
                    ) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = mStrings.photoAdjZoomInDesc)
                    }
                    Text(
                        text = mStrings.formatZoom(zoom),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = brand.burgundy
                    )
                }

                // Live Preview matching exact Home Hero Banner Display
                Text(
                    text = mStrings.photoAdjLivePreviewTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                HomeHeroBannerPreviewFrame(
                    caption = captionPreview,
                    modifier = Modifier.testTag("photo_adj_live_preview")
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        val density = LocalDensity.current
                        val viewW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                        val viewH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                        val baseScale = max(viewW / intrinsicWidth, viewH / intrinsicHeight)
                        val totalScale = baseScale * zoom
                        val scaledW = intrinsicWidth * totalScale
                        val scaledH = intrinsicHeight * totalScale
                        val maxPanX = max(0f, (scaledW - viewW) / 2f)
                        val maxPanY = max(0f, (scaledH - viewH) / 2f)

                        AdjustedImageLayer(
                            imageUri = originalUri,
                            contentDescription = mStrings.photoAdjImageDesc,
                            scaledWidthPx = scaledW,
                            scaledHeightPx = scaledH,
                            translationXPx = panFractionX * maxPanX,
                            translationYPx = panFractionY * maxPanY
                        )
                    }
                }

                // Actions: Reset, Cancel, Crop & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            zoom = 1f
                            panFractionX = 0f
                            panFractionY = 0f
                            errorMessage = null
                        },
                        enabled = !isSavingCrop,
                        modifier = Modifier.testTag("photo_adj_reset_btn")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(mStrings.photoAdjResetBtn)
                    }

                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSavingCrop
                    ) {
                        Text(mStrings.cancelBtn)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            scope.launch {
                                isSavingCrop = true
                                errorMessage = null
                                mediaManager.saveAdjustedHeroCrop(
                                    sourceImageUri = originalUri,
                                    zoom = zoom,
                                    panFractionX = panFractionX,
                                    panFractionY = panFractionY,
                                    viewportAspectRatio = MediaStudioManager.HERO_BANNER_ASPECT_RATIO
                                ).onSuccess { result ->
                                    isSavingCrop = false
                                    onApply(result.originalUri, result.optimizedUri)
                                }.onFailure { err ->
                                    isSavingCrop = false
                                    errorMessage = err.message ?: mStrings.photoAdjCropFailedError
                                }
                            }
                        },
                        enabled = !isSavingCrop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.burgundy,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("photo_adj_apply_crop_btn")
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
                            Text(mStrings.photoAdjCropApplyBtn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustedImageLayer(
    imageUri: String,
    contentDescription: String,
    scaledWidthPx: Float,
    scaledHeightPx: Float,
    translationXPx: Float,
    translationYPx: Float
) {
    val density = LocalDensity.current
    val widthDp = with(density) { scaledWidthPx.toDp() }
    val heightDp = with(density) { scaledHeightPx.toDp() }
    Box(
        modifier = Modifier
            .size(widthDp, heightDp)
            .graphicsLayer {
                translationX = translationXPx
                translationY = translationYPx
            }
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun readOrientedDimensions(uriString: String): Pair<Int, Int>? {
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
