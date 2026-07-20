package pro.masterdoc.client.portal

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.koin.core.context.startKoin
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.authModule
import pro.masterdoc.client.auth.createDefaultGatewayHttpClient

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val koinApp =
        startKoin {
            modules(
                authModule(
                    config = portalAuthConfig(),
                    http = createDefaultGatewayHttpClient(),
                ),
            )
        }
    val coordinator = koinApp.koin.get<AuthCoordinator>()
    ComposeViewport(document.body!!) {
        PortalApp(coordinator = coordinator)
    }
}
