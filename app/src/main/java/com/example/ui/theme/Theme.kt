package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.model.AppThemeMode

@Immutable
data class ChevalierBrandPalette(
    val isDark: Boolean,
    val royalGold: Color,
    val onRoyalGold: Color,
    val burgundy: Color,
    val forestGreen: Color,
    val forestGreenContainer: Color,
    val onForestGreenContainer: Color,
    val warmIvory: Color,
    val goldAccentText: Color
)

val LocalBrandPalette = staticCompositionLocalOf {
    ChevalierBrandPalette(
        isDark = false,
        royalGold = RoyalGold,
        onRoyalGold = InkDark,
        burgundy = Burgundy,
        forestGreen = ForestGreen,
        forestGreenContainer = ForestGreenSoft,
        onForestGreenContainer = ForestGreen,
        warmIvory = WarmIvory,
        goldAccentText = RoyalGoldDeep
    )
}

private val ChevalierLightColorScheme = lightColorScheme(
    primary = Burgundy,
    onPrimary = Color.White,
    primaryContainer = BurgundyContainerLight,
    onPrimaryContainer = OnBurgundyContainerLight,
    secondary = ForestGreen,
    onSecondary = Color.White,
    secondaryContainer = ForestGreenSoft,
    onSecondaryContainer = Color(0xFF04391F),
    tertiary = RoyalGoldDeep,
    onTertiary = Color.White,
    tertiaryContainer = RoyalGoldSoft,
    onTertiaryContainer = Color(0xFF362800),
    background = WarmIvory,
    onBackground = InkDark,
    surface = WarmIvoryElevated,
    onSurface = InkDark,
    surfaceVariant = WarmIvorySurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineWarmLight,
    outlineVariant = Color(0xFFE7DEC9)
)

private val ChevalierDarkColorScheme = darkColorScheme(
    primary = BurgundyLightOnDark,
    onPrimary = Color(0xFF560A12),
    primaryContainer = BurgundyContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = ForestGreenLight,
    onSecondary = Color(0xFF00391E),
    secondaryContainer = ForestGreenContainerDark,
    onSecondaryContainer = ForestGreenSoft,
    tertiary = RoyalGoldOnDark,
    onTertiary = Color(0xFF382B00),
    tertiaryContainer = RoyalGoldContainerDark,
    onTertiaryContainer = RoyalGoldSoft,
    background = DarkBackground,
    onBackground = IvoryTextOnDark,
    surface = DarkSurface,
    onSurface = IvoryTextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = MutedTextOnDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF3E3131)
)

val ChevalierShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ChevalierAcademyTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) ChevalierDarkColorScheme else ChevalierLightColorScheme
    val brandPalette = if (darkTheme) {
        ChevalierBrandPalette(
            isDark = true,
            royalGold = RoyalGold,
            onRoyalGold = InkDark,
            burgundy = Burgundy,
            forestGreen = ForestGreenLight,
            forestGreenContainer = ForestGreenContainerDark,
            onForestGreenContainer = ForestGreenSoft,
            warmIvory = WarmIvory,
            goldAccentText = RoyalGoldOnDark
        )
    } else {
        ChevalierBrandPalette(
            isDark = false,
            royalGold = RoyalGold,
            onRoyalGold = InkDark,
            burgundy = Burgundy,
            forestGreen = ForestGreen,
            forestGreenContainer = ForestGreenSoft,
            onForestGreenContainer = ForestGreen,
            warmIvory = WarmIvory,
            goldAccentText = RoyalGoldDeep
        )
    }

    CompositionLocalProvider(LocalBrandPalette provides brandPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChevalierTypography,
            shapes = ChevalierShapes,
            content = content
        )
    }
}
