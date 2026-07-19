package pro.masterdoc.client

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import pro.masterdoc.client.di.initClientKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initClientKoin()
    ComposeViewport(document.body!!) {
        App()
    }
}
