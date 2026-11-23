package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.AcademyHeroBannerConfig
import com.example.ui.components.ManagedPublicImage
import com.example.ui.components.Version2DesignTokens

/**
 * Full-width landscape Hero Banner visual for the Academy Page.
 * - Never renders an external heading or forced placeholder text.
 * - When published with no overlay text: renders ONLY the uploaded image cleanly (no scrim, no text).
 * - When published with optional overlay text: renders the uploaded image + contrast scrim + text.
 * - Authorized admins receive a discreet Edit trigger to upload/replace the image or edit optional text.
 */
@Composable
fun AcademyHeroBannerVisual(
    config: AcademyHeroBannerConfig,
    canEdit: Boolean,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val bannerShape = RoundedCornerShape(18.dp)
    val shouldShowUploadedImage = config.hasImage && (config.isPublished || isPreviewMode)

    Card(
        shape = bannerShape,
        border = BorderStroke(1.25.dp, Version2DesignTokens.BlueVioletBorderBrush),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1A223B) else Color(0xFFEEF1FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .heightIn(min = 160.dp, max = 224.dp)
            .clip(bannerShape)
            .testTag(if (isPreviewMode) "academy_hero_preview_visual" else "academy_hero_banner")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (shouldShowUploadedImage) {
                ManagedPublicImage(
                    imageUri = config.imageUri,
                    contentDescription = config.overlayTitle.ifBlank { "Chevalier Academy Hero Banner" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(
                            if (config.hasOverlayText) {
                                "academy_hero_image_with_text"
                            } else {
                                "academy_hero_image_only"
                            }
                        )
                )

                if (config.hasOverlayText) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x660B1226),
                                        Color(0xCC0B1226)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (config.overlayTitle.isNotBlank()) {
                            Text(
                                text = config.overlayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag(
                                    if (isPreviewMode) "academy_hero_preview_title" else "academy_hero_overlay_title"
                                )
                            )
                        }
                        if (config.overlaySubtitle.isNotBlank()) {
                            Text(
                                text = config.overlaySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag(
                                    if (isPreviewMode) "academy_hero_preview_subtitle" else "academy_hero_overlay_subtitle"
                                )
                            )
                        }
                    }
                }
            } else {
                // Clean, minimal atmospheric landscape surface without forced placeholder headings or fake text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF151E38), Color(0xFF202A54), Color(0xFF2B245B))
                                } else {
                                    listOf(Color(0xFFE8EFFF), Color(0xFFEEE9FF), Color(0xFFF6F2FF))
                                }
                            )
                        )
                        .testTag("academy_hero_empty_surface"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chevalier_emblem),
                        contentDescription = "Chevalier Academy",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(64.dp)
                            .alpha(if (isDark) 0.24f else 0.18f)
                    )
                }
            }

            if (canEdit && onEditClick != null && !isPreviewMode) {
                Surface(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xCC111936),
                    border = BorderStroke(1.dp, Version2DesignTokens.BlueVioletBorderBrush),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .heightIn(min = 48.dp)
                        .testTag("academy_hero_edit_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Manage Academy Hero Banner",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (shouldShowUploadedImage) "Edit Banner" else "Upload Banner",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authorized Admin editor dialog for uploading/replacing the Academy Hero Banner image,
 * editing optional overlay text (blank allowed for image-only publishing), previewing
 * the live result, and publishing persistently.
 */
@Composable
fun AcademyHeroBannerEditorDialog(
    initialConfig: AcademyHeroBannerConfig,
    isBusy: Boolean,
    onPickBannerImage: (Uri, (String) -> Unit) -> Unit,
    onPublish: (imageUri: String?, overlayTitle: String, overlaySubtitle: String, publishNow: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var draftImageUri by remember(initialConfig) { mutableStateOf(initialConfig.imageUri) }
    var draftTitle by remember(initialConfig) { mutableStateOf(initialConfig.overlayTitle) }
    var draftSubtitle by remember(initialConfig) { mutableStateOf(initialConfig.overlaySubtitle) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            validationError = null
            onPickBannerImage(pickedUri) { importedUri ->
                draftImageUri = importedUri
            }
        }
    }

    val previewConfig = remember(draftImageUri, draftTitle, draftSubtitle) {
        AcademyHeroBannerConfig(
            imageUri = draftImageUri,
            overlayTitle = draftTitle.trim(),
            overlaySubtitle = draftSubtitle.trim(),
            isPublished = !draftImageUri.isNullOrBlank()
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        modifier = Modifier
            .widthIn(max = 520.dp)
            .testTag("academy_hero_editor_dialog"),
        title = {
            Text(
                text = "Academy Hero Banner",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = Version2DesignTokens.GradientViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Live Banner Preview",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("academy_hero_preview_box")
                ) {
                    AcademyHeroBannerVisual(
                        config = previewConfig,
                        canEdit = false,
                        isPreviewMode = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("academy_hero_upload_image_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (draftImageUri.isNullOrBlank()) "Upload Image" else "Replace Image")
                    }

                    if (!draftImageUri.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                draftImageUri = null
                                validationError = null
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("academy_hero_remove_image_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove banner image",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Overlay text is optional. Leave both fields empty to publish an image-only banner.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it.take(90) },
                    label = { Text("Overlay Title (Optional)") },
                    singleLine = true,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("academy_hero_title_input")
                )

                OutlinedTextField(
                    value = draftSubtitle,
                    onValueChange = { draftSubtitle = it.take(160) },
                    label = { Text("Overlay Subtitle (Optional)") },
                    maxLines = 3,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("academy_hero_subtitle_input")
                )

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("academy_hero_editor_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (draftImageUri.isNullOrBlank()) {
                        validationError = "Please upload a banner image before publishing."
                    } else {
                        validationError = null
                        onPublish(draftImageUri, draftTitle.trim(), draftSubtitle.trim(), true)
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Version2DesignTokens.GradientViolet),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("academy_hero_publish_btn")
            ) {
                Text("Publish Banner")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("academy_hero_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
