package pro.masterdoc.client

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.koin.core.context.startKoin
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AiMessagesRepository
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.ClientEventsRepository
import pro.masterdoc.client.auth.CommentsRepository
import pro.masterdoc.client.auth.EngineerLocationsRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GeocodeRepository
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.auth.WarehouseRepository
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
    val workOrders = koinApp.koin.get<WorkOrdersRepository>()
    val userScopes = koinApp.koin.get<UserScopesRepository>()
    val clientEvents = koinApp.koin.get<ClientEventsRepository>()
    val engineerLocations = koinApp.koin.get<EngineerLocationsRepository>()
    val geocode = koinApp.koin.get<GeocodeRepository>()
    val aiMessages = koinApp.koin.get<AiMessagesRepository>()
    val viewportRoot =
        document.getElementById("fixaverse-compose-root")
            ?: document.body
            ?: error("No document body")
    ComposeViewport(viewportRoot) {
        AuthenticatedApp(
            coordinator = coordinator,
            attachmentsRepository = koinApp.koin.get<AttachmentsRepository>(),
            commentsRepository = koinApp.koin.get<CommentsRepository>(),
            warehouseRepository = koinApp.koin.get<WarehouseRepository>(),
            adminUsersRepository = adminUsers,
            equipmentRepository = equipment,
            workOrdersRepository = workOrders,
            userScopesRepository = userScopes,
            clientEventsRepository = clientEvents,
            engineerLocationsRepository = engineerLocations,
            geocodeRepository = geocode,
            aiMessagesRepository = aiMessages,
        )
    }
}
