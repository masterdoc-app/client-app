package pro.masterdoc.client.technolog

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.koin.core.context.startKoin
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.authModule
import pro.masterdoc.client.auth.createDefaultGatewayHttpClient
import pro.masterdoc.client.presentation.shell.DefaultRootComponent

fun main() =
    application {
        val koinApp =
            startKoin {
                modules(
                    authModule(
                        config = technologAuthConfig(),
                        http = createDefaultGatewayHttpClient(),
                    ),
                )
            }
        val coordinator = koinApp.koin.get<AuthCoordinator>()
        Window(onCloseRequest = ::exitApplication, title = "Fixaverse Technolog") {
            TechnologApp(
                coordinator = coordinator,
                rootFactory = { session ->
                    DefaultRootComponent(
                        componentContext = DefaultComponentContext(LifecycleRegistry()),
                        session = session,
                    )
                },
            )
        }
    }
