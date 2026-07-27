package pro.masterdoc.client.platform

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable

@Composable
actual fun AppTextSelection(content: @Composable () -> Unit) {
    SelectionContainer(content = content)
}
