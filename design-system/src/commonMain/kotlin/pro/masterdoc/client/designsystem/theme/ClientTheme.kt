package pro.masterdoc.client.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ClientTheme(
    /** Product default is light graphite; opt into dark explicitly (not OS auto). */
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) clientDarkColorScheme() else clientLightColorScheme(),
        typography = ClientTypography,
        shapes = ClientShapes,
        content = content,
    )
}
