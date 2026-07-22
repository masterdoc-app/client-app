package pro.masterdoc.client.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Graphite + Cobalt — light-first Formaverse palette.
 *
 * Cool blue-leaning neutrals only. Material3 defaults (Neutral98 `#FEF7FF`,
 * Neutral94 `#F3EDF7`, Neutral90 `#E6E0E9`) are warm pink/cream and must not
 * appear in Scaffold, nav, fields, or unset ColorScheme roles.
 */
object ClientColors {
    /** Page canvas — cool blue-gray (B ≫ R), readable as graphite not cream next to white. */
    val Background = Color(0xFFE8EDF3)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF2F5F8)
    val SurfaceContainer = Color(0xFFE8EDF3)
    val SurfaceContainerHigh = Color(0xFFDCE3EB)
    val SurfaceContainerHighest = Color(0xFFCFD7E0)
    val SurfaceVariant = Color(0xFFDCE3EB)
    val OnSurface = Color(0xFF1A1C1E)
    val OnSurfaceVariant = Color(0xFF5A616C)
    val Primary = Color(0xFF1F4B99)
    val OnPrimary = Color(0xFFFFFFFF)
    val Outline = Color(0xFFB8C0CA)
    val OutlineVariant = Color(0xFFD5DBE3)
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
        secondaryContainer = ClientColors.SurfaceContainerHigh,
        onSecondaryContainer = ClientColors.OnSurface,
        tertiary = ClientColors.OnSurfaceVariant,
        onTertiary = ClientColors.OnPrimary,
        tertiaryContainer = ClientColors.SurfaceContainer,
        onTertiaryContainer = ClientColors.OnSurface,
        background = ClientColors.Background,
        onBackground = ClientColors.OnSurface,
        surface = ClientColors.Surface,
        onSurface = ClientColors.OnSurface,
        onSurfaceVariant = ClientColors.OnSurfaceVariant,
        surfaceVariant = ClientColors.SurfaceVariant,
        surfaceTint = ClientColors.Primary,
        surfaceBright = ClientColors.Surface,
        surfaceDim = ClientColors.SurfaceContainerHigh,
        surfaceContainerLowest = ClientColors.SurfaceContainerLowest,
        surfaceContainerLow = ClientColors.SurfaceContainerLow,
        surfaceContainer = ClientColors.SurfaceContainer,
        surfaceContainerHigh = ClientColors.SurfaceContainerHigh,
        surfaceContainerHighest = ClientColors.SurfaceContainerHighest,
        inverseSurface = Color(0xFF2E3135),
        inverseOnSurface = Color(0xFFEFF1F4),
        inversePrimary = Color(0xFFADC6FF),
        outline = ClientColors.Outline,
        outlineVariant = ClientColors.OutlineVariant,
        error = ClientColors.Error,
        onError = ClientColors.OnPrimary,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        scrim = Color(0xFF000000),
    )

fun clientDarkColorScheme(): ColorScheme =
    darkColorScheme(
        primary = Color(0xFFADC6FF),
        onPrimary = Color(0xFF002E69),
        primaryContainer = Color(0xFF003E8F),
        onPrimaryContainer = Color(0xFFD6E3FF),
        secondary = Color(0xFFBFC7D4),
        onSecondary = Color(0xFF29313D),
        secondaryContainer = Color(0xFF24282C),
        onSecondaryContainer = Color(0xFFE2E2E5),
        tertiary = Color(0xFFC2C7D0),
        onTertiary = Color(0xFF29313D),
        tertiaryContainer = Color(0xFF1A1C1E),
        onTertiaryContainer = Color(0xFFE2E2E5),
        background = Color(0xFF121417),
        onBackground = Color(0xFFE2E2E5),
        surface = Color(0xFF1A1C1E),
        onSurface = Color(0xFFE2E2E5),
        onSurfaceVariant = Color(0xFFC2C7D0),
        surfaceVariant = Color(0xFF2A2E33),
        surfaceTint = Color(0xFFADC6FF),
        surfaceBright = Color(0xFF2F3438),
        surfaceDim = Color(0xFF121417),
        surfaceContainerLowest = Color(0xFF0E1012),
        surfaceContainerLow = Color(0xFF16191C),
        surfaceContainer = Color(0xFF1A1C1E),
        surfaceContainerHigh = Color(0xFF24282C),
        surfaceContainerHighest = Color(0xFF2F3438),
        inverseSurface = Color(0xFFE2E2E5),
        inverseOnSurface = Color(0xFF1A1C1E),
        inversePrimary = Color(0xFF1F4B99),
        outline = Color(0xFF8C919A),
        outlineVariant = Color(0xFF2A2E33),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color(0xFF000000),
    )
