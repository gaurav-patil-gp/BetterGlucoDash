package com.eggyswarehouse.betterglucodash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// ── M3 colour schemes ─────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary               = PrimaryDark,
    onPrimary             = OnPrimaryDark,
    primaryContainer      = PrimaryContainerDark,
    onPrimaryContainer    = OnPrimaryContainerDark,
    background            = BackgroundDark,
    onBackground          = OnSurfaceDark,
    surface               = SurfaceDark,
    onSurface             = OnSurfaceDark,
    surfaceVariant        = SurfaceVariantDark,
    onSurfaceVariant      = OnSurfaceVariantDark,
    error                 = ErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary               = PrimaryLight,
    onPrimary             = OnPrimaryLight,
    primaryContainer      = PrimaryContainerLight,
    onPrimaryContainer    = OnPrimaryContainerLight,
    background            = BackgroundLight,
    onBackground          = OnSurfaceLight,
    surface               = SurfaceLight,
    onSurface             = OnSurfaceLight,
    surfaceVariant        = SurfaceVariantLight,
    onSurfaceVariant      = OnSurfaceVariantLight,
    error                 = ErrorLight
)

// ── Glucose CompositionLocal pairs ────────────────────────────────────────────

private val DarkGlucoseColors = GlucoseColors(
    inRange      = GlucoseGreenDark,
    slightlyHigh = GlucoseAmberDark,
    high         = GlucoseOrangeDark,
    low          = GlucoseRedDark
)

private val LightGlucoseColors = GlucoseColors(
    inRange      = GlucoseGreenLight,
    slightlyHigh = GlucoseAmberLight,
    high         = GlucoseOrangeLight,
    low          = GlucoseRedLight
)

/**
 * Root theme composable for BetterGlucoDash.
 *
 * V2 ships dark-first: [forceDark] defaults to `true` so the deep-navy OLED
 * design is always active regardless of system setting. Setting [forceDark] to
 * `false` passes control to [isSystemInDarkTheme] — ready for a V3 settings toggle.
 *
 * Dynamic colour (Android 12+ wallpaper theming) is intentionally disabled so
 * the glucose semantic accent colours (green/yellow/orange/red) always render
 * correctly and predictably.
 */
@Composable
fun BetterGlucoDashTheme(
    forceDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme   = if (forceDark) DarkColorScheme   else LightColorScheme
    val glucoseColors = if (forceDark) DarkGlucoseColors else LightGlucoseColors

    CompositionLocalProvider(LocalGlucoseColors provides glucoseColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}

/** Convenience accessor — `MaterialTheme.glucoseColors.inRange` etc. */
val MaterialTheme.glucoseColors: GlucoseColors
    @Composable
    get() = LocalGlucoseColors.current