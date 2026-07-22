package pro.masterdoc.client

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.koin.core.context.startKoin
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.authModule
import pro.masterdoc.client.auth.createDefaultGatewayHttpClient

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val koinApp =
        startKoin {
            modules(
                authModule(
                    config = appAuthConfig(),
                    http = createDefaultGatewayHttpClient(),
                ),
            )
        }
    val coordinator = koinApp.koin.get<AuthCoordinator>()
    val adminUsers = koinApp.koin.get<AdminUsersRepository>()
    val equipment = koinApp.koin.get<EquipmentRepository>()
    ComposeViewport(document.body!!) {
        AuthenticatedApp(
            coordinator = coordinator,
            adminUsersRepository = adminUsers,
            equipmentRepository = equipment,
        )
    }
}
