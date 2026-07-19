package pro.masterdoc.client.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object ClientColors {
    val BrandPrimary = Color(0xFF0B6E4F)
    val BrandOnPrimary = Color(0xFFFFFFFF)
    val BrandSecondary = Color(0xFF1B4332)
    val BrandSurface = Color(0xFFF4F7F5)
    val BrandOnSurface = Color(0xFF12241C)
    val BrandOutline = Color(0xFF8AA396)
    val BrandError = Color(0xFFB3261E)
}

fun clientLightColorScheme(): ColorScheme =
    lightColorScheme(
        primary = ClientColors.BrandPrimary,
        onPrimary = ClientColors.BrandOnPrimary,
        secondary = ClientColors.BrandSecondary,
        onSecondary = ClientColors.BrandOnPrimary,
        background = ClientColors.BrandSurface,
        onBackground = ClientColors.BrandOnSurface,
        surface = ClientColors.BrandSurface,
        onSurface = ClientColors.BrandOnSurface,
        outline = ClientColors.BrandOutline,
        error = ClientColors.BrandError,
    )

fun clientDarkColorScheme(): ColorScheme =
    darkColorScheme(
        primary = Color(0xFF5DDBA5),
        onPrimary = Color(0xFF003825),
        secondary = Color(0xFFA8D5C0),
        onSecondary = Color(0xFF003825),
        background = Color(0xFF0E1A14),
        onBackground = Color(0xFFE6F0EA),
        surface = Color(0xFF15231C),
        onSurface = Color(0xFFE6F0EA),
        outline = Color(0xFF6F8A7C),
        error = Color(0xFFFFB4AB),
    )
