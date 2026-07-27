package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable

/** Wasm: SelectionContainer. Android/Desktop: identity. */
@Composable
expect fun AppTextSelection(content: @Composable () -> Unit)
