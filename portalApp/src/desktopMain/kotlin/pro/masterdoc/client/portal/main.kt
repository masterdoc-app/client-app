package pro.masterdoc.client.portal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.authModule
import pro.masterdoc.client.auth.createDefaultGatewayHttpClient

fun main() =
    application {
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
        Window(onCloseRequest = ::exitApplication, title = "Fixaverse Portal") {
            PortalApp(coordinator = coordinator)
        }
    }
