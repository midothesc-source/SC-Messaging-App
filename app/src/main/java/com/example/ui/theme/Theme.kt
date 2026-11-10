package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.AppLanguage
import com.example.data.AppThemeMode

private val ObsidianGoldScheme = darkColorScheme(
    primary = ObsidianGoldPrimary,
    onPrimary = ObsidianOnPrimary,
    primaryContainer = ObsidianSurfaceVariant,
    onPrimaryContainer = ObsidianGoldSecondary,
    secondary = ObsidianGoldSecondary,
    onSecondary = ObsidianOnPrimary,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = ObsidianGoldPrimary,
    tertiary = ObsidianGoldTertiary,
    background = ObsidianBackground,
    onBackground = ObsidianOnBackground,
    surface = ObsidianSurface,
    onSurface = ObsidianOnBackground,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    outline = ObsidianOutline
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldSurfaceVariant,
    onPrimaryContainer = EmeraldSecondary,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnPrimary,
    secondaryContainer = EmeraldSurfaceVariant,
    onSecondaryContainer = EmeraldPrimary,
    tertiary = EmeraldTertiary,
    background = EmeraldBackground,
    onBackground = EmeraldOnBackground,
    surface = EmeraldSurface,
    onSurface = EmeraldOnBackground,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = EmeraldOnSurfaceVariant,
    outline = EmeraldOutline
)

private val CleanLightScheme = lightColorScheme(
    primary = CleanLightPrimary,
    onPrimary = CleanLightOnPrimary,
    primaryContainer = CleanLightSurfaceVariant,
    onPrimaryContainer = CleanLightPrimary,
    secondary = CleanLightSecondary,
    onSecondary = CleanLightOnPrimary,
    secondaryContainer = CleanLightSurfaceVariant,
    onSecondaryContainer = CleanLightSecondary,
    tertiary = CleanLightTertiary,
    background = CleanLightBackground,
    onBackground = CleanLightOnBackground,
    surface = CleanLightSurface,
    onSurface = CleanLightOnBackground,
    surfaceVariant = CleanLightSurfaceVariant,
    onSurfaceVariant = CleanLightOnSurfaceVariant,
    outline = CleanLightOutline
)

@Composable
fun ScTheme(
    themeMode: AppThemeMode = AppThemeMode.OBSIDIAN_GOLD,
    language: AppLanguage = AppLanguage.EN,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.OBSIDIAN_GOLD -> ObsidianGoldScheme
        AppThemeMode.EMERALD_DARK -> EmeraldDarkScheme
        AppThemeMode.CLEAN_LIGHT -> CleanLightScheme
    }
    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = buildScTypography(language)

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
