package com.example.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.core.model.AppLanguage
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.delay

const val SPLASH_DISPLAY_DURATION_MS = 5_000L
private const val SPLASH_ARTWORK_ASPECT = 941f / 1672f
private const val SPLASH_SAFE_WIDTH_FRACTION = 0.875f

@Composable
fun SplashScreen(
    onSplashCompleted: () -> Unit
) {
    val view = LocalView.current
    val isDarkTheme = LocalBrandPalette.current.isDark
    val latestOnCompleted by rememberUpdatedState(onSplashCompleted)
    val splashBackground = colorResource(id = R.color.splash_royal_blue)

    DisposableEffect(view, isDarkTheme) {
        val window = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            show(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        onDispose {
            controller?.isAppearanceLightStatusBars = !isDarkTheme
            controller?.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_DISPLAY_DURATION_MS)
        latestOnCompleted()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBackground)
            .clipToBounds()
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        val containerAspect = if (maxHeight > 0.dp) maxWidth / maxHeight else SPLASH_ARTWORK_ASPECT
        val minCropAspect = SPLASH_ARTWORK_ASPECT * SPLASH_SAFE_WIDTH_FRACTION

        when {
            containerAspect >= SPLASH_ARTWORK_ASPECT -> {
                // Landscape or wider aspect ratio: fit full vertical artwork edge-to-edge in height,
                // extending matching royal-blue background cleanly to left and right edges.
                Image(
                    painter = painterResource(id = R.drawable.chevalier_splash_artwork),
                    contentDescription = LocalAppStrings.current.officialSchoolName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("splash_artwork_image")
                )
            }
            containerAspect >= minCropAspect -> {
                // Standard portrait aspect ratios (9:16 to ~18.5:9): crop fills 100% width and height
                // edge-to-edge without touching any central emblem or lettering.
                Image(
                    painter = painterResource(id = R.drawable.chevalier_splash_artwork),
                    contentDescription = LocalAppStrings.current.officialSchoolName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("splash_artwork_image")
                )
            }
            else -> {
                // Tall portrait screens (19.5:9 / 20:9 / 21:9): fill full width and >92% height
                // proportionally without clipping 'CHEVALIER', blending edges seamlessly into #011356.
                val renderWidth = maxWidth / SPLASH_SAFE_WIDTH_FRACTION
                val renderHeight = renderWidth / SPLASH_ARTWORK_ASPECT
                Box(
                    modifier = Modifier.requiredSize(width = renderWidth, height = renderHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chevalier_splash_artwork),
                        contentDescription = LocalAppStrings.current.officialSchoolName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("splash_artwork_image")
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(splashBackground, Color.Transparent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, splashBackground)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(
    selectedLanguage: AppLanguage,
    onLanguagePreviewSelected: (AppLanguage) -> Unit,
    onConfirmLanguage: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("language_selection_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, brand.royalGold),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(112.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.chevalier_emblem),
                            contentDescription = strings.officialSchoolName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = strings.officialSchoolName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "\"${strings.officialMotto}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = brand.goldAccentText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = brand.forestGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = strings.chooseLanguageTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = strings.chooseLanguageSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppLanguage.entries.forEach { language ->
                            val isSelected = language == selectedLanguage
                            LanguageOptionCard(
                                language = language,
                                selected = isSelected,
                                onClick = { onLanguagePreviewSelected(language) }
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                ) {
                    Button(
                        onClick = { onConfirmLanguage(selectedLanguage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("language_continue_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.burgundy,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = strings.continueButton,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.poweredByAttribution,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = brand.goldAccentText
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val brand = LocalBrandPalette.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("language_option_${language.code}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) brand.burgundy else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) brand.royalGold else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = language.code.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) brand.onRoyalGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column {
                    Text(
                        text = language.nativeLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = language.englishLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = language.englishLabel,
                tint = if (selected) brand.burgundy else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
