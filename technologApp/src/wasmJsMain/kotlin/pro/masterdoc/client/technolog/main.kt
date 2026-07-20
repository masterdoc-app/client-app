package pro.masterdoc.client.technolog

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.browser.document
import org.koin.core.context.startKoin
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.authModule
import pro.masterdoc.client.auth.createDefaultGatewayHttpClient
import pro.masterdoc.client.presentation.shell.DefaultRootComponent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
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
    ComposeViewport(document.body!!) {
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
