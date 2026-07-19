package pro.masterdoc.client.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) clientDarkColorScheme() else clientLightColorScheme(),
        typography = ClientTypography,
        shapes = ClientShapes,
        content = content,
    )
}
