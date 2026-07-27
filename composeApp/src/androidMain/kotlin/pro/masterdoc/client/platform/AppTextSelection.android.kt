package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable

@Composable
actual fun AppTextSelection(content: @Composable () -> Unit) {
    content()
}
