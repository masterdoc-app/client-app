package pro.masterdoc.client.designsystem.theme

import androidx.compose.runtime.Composable
import pro.fixaverse.design.theme.FixaverseTheme

/** Product shell theme — Fixaverse Lite (Paper + FlareTint muted surfaces, Rule2 borders). */
@Composable
fun ClientTheme(content: @Composable () -> Unit) = FixaverseTheme(content)
