package pro.masterdoc.client.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Graphite + Cobalt — light-first Formaverse palette.
 *
 * Cool neutrals only: Material3 default surfaceContainer* (Neutral98 `#FEF7FF` etc.)
 * are warm/pink and must not leak into Scaffold, nav, or text fields.
 */
object ClientColors {
    val Background = Color(0xFFF2F3F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF7F8FA)
    val SurfaceContainer = Color(0xFFF2F3F5)
    val SurfaceContainerHigh = Color(0xFFE8EAED)
    val SurfaceContainerHighest = Color(0xFFDEE1E6)
    val SurfaceVariant = Color(0xFFE8EAED)
    val OnSurface = Color(0xFF1A1C1E)
    val OnSurfaceVariant = Color(0xFF5C6370)
    val Primary = Color(0xFF1F4B99)
    val OnPrimary = Color(0xFFFFFFFF)
    val Outline = Color(0xFFC5CAD3)
    val Error = Color(0xFFB3261E)

    val PrimaryContainer = Color(0xFFD6E3FF)
    val OnPrimaryContainer = Color(0xFF001A41)
}

fun clientLightColorScheme(): ColorScheme =
    lightColorScheme(
        primary = ClientColors.Primary,
        onPrimary = ClientColors.OnPrimary,
        primaryContainer = ClientColors.PrimaryContainer,
        onPrimaryContainer = ClientColors.OnPrimaryContainer,
        secondary = ClientColors.OnSurfaceVariant,
        onSecondary = ClientColors.OnPrimary,
        background = ClientColors.Background,
        onBackground = ClientColors.OnSurface,
        surface = ClientColors.Surface,
        onSurface = ClientColors.OnSurface,
        onSurfaceVariant = ClientColors.OnSurfaceVariant,
        surfaceVariant = ClientColors.SurfaceVariant,
        surfaceContainerLowest = ClientColors.SurfaceContainerLowest,
        surfaceContainerLow = ClientColors.SurfaceContainerLow,
        surfaceContainer = ClientColors.SurfaceContainer,
        surfaceContainerHigh = ClientColors.SurfaceContainerHigh,
        surfaceContainerHighest = ClientColors.SurfaceContainerHighest,
        outline = ClientColors.Outline,
        error = ClientColors.Error,
    )

fun clientDarkColorScheme(): ColorScheme =
    darkColorScheme(
        primary = Color(0xFFADC6FF),
        onPrimary = Color(0xFF002E69),
        primaryContainer = Color(0xFF003E8F),
        onPrimaryContainer = Color(0xFFD6E3FF),
        secondary = Color(0xFFBFC7D4),
        onSecondary = Color(0xFF29313D),
        background = Color(0xFF121417),
        onBackground = Color(0xFFE2E2E5),
        surface = Color(0xFF1A1C1E),
        onSurface = Color(0xFFE2E2E5),
        onSurfaceVariant = Color(0xFFC2C7D0),
        surfaceVariant = Color(0xFF2A2E33),
        surfaceContainerLowest = Color(0xFF0E1012),
        surfaceContainerLow = Color(0xFF16191C),
        surfaceContainer = Color(0xFF1A1C1E),
        surfaceContainerHigh = Color(0xFF24282C),
        surfaceContainerHighest = Color(0xFF2F3438),
        outline = Color(0xFF8C919A),
        error = Color(0xFFFFB4AB),
    )
