package uc.ucworks.videosnap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Dark color scheme with glossy, neon accents.
 * Optimized for video editing with high contrast and vibrant highlights.
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors - Neon Cyan for main actions
    primary = NeonCyan,
    onPrimary = DarkBackground,
    primaryContainer = NeonBlue.copy(alpha = 0.3f),
    onPrimaryContainer = NeonCyan,

    // Secondary colors - Neon Purple for secondary actions
    secondary = NeonPurple,
    onSecondary = DarkBackground,
    secondaryContainer = NeonPurple.copy(alpha = 0.2f),
    onSecondaryContainer = NeonPurple,

    // Tertiary colors - Neon Pink for highlights
    tertiary = NeonPink,
    onTertiary = DarkBackground,
    tertiaryContainer = NeonPink.copy(alpha = 0.2f),
    onTertiaryContainer = NeonPink,

    // Background colors
    background = DarkBackground,
    onBackground = DarkOnBackground,

    // Surface colors
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnBackground.copy(alpha = 0.7f),

    // Error colors
    error = ErrorRed,
    onError = DarkBackground,

    // Outline
    outline = DarkOnSurface.copy(alpha = 0.3f),
    outlineVariant = DarkOnSurface.copy(alpha = 0.15f)
)

/**
 * Light color scheme with glossy appearance.
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = NeonBlue,
    onPrimary = LightBackground,
    primaryContainer = NeonCyan.copy(alpha = 0.2f),
    onPrimaryContainer = NeonBlue,

    // Secondary colors
    secondary = NeonPurple,
    onSecondary = LightBackground,
    secondaryContainer = NeonPurple.copy(alpha = 0.15f),
    onSecondaryContainer = NeonPurple,

    // Tertiary colors
    tertiary = NeonPink,
    onTertiary = LightBackground,
    tertiaryContainer = NeonPink.copy(alpha = 0.15f),
    onTertiaryContainer = NeonPink,

    // Background colors
    background = LightBackground,
    onBackground = LightOnBackground,

    // Surface colors
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnBackground.copy(alpha = 0.7f),

    // Error colors
    error = ErrorRed,
    onError = LightBackground,

    // Outline
    outline = LightOnSurface.copy(alpha = 0.3f),
    outlineVariant = LightOnSurface.copy(alpha = 0.15f)
)

/**
 * VideoSnap theme with glossy, neon-accented design.
 * Optimized for professional video editing with vibrant colors and high contrast.
 *
 * @param darkTheme Whether to use dark theme (defaults to system preference)
 * @param content The composable content to theme
 */
@Composable
fun VideoSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
