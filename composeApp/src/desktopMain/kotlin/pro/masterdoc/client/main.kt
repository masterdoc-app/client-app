package pro.masterdoc.client

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import pro.masterdoc.client.di.initClientKoin

fun main() {
    initClientKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Fixaverse",
        ) {
            App()
        }
    }
}
