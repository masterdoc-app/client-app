package pro.masterdoc.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AiMessagesRepository
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.ClientEventsRepository
import pro.masterdoc.client.auth.CommentsRepository
import pro.masterdoc.client.auth.EngineerLocationsRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GeocodeRepository
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.auth.authModule
import pro.masterdoc.client.auth.configureAndroidAuthPlatform
import pro.masterdoc.client.auth.createDefaultGatewayHttpClient
import pro.masterdoc.client.presentation.shell.DefaultRootComponent
import pro.masterdoc.client.presentation.shell.RootComponent
import pro.masterdoc.client.session.ClientSession
import pro.masterdoc.client.tracking.configureEngineerLocationTracking
import pro.masterdoc.client.update.RuStoreAppUpdater

class MainActivity : ComponentActivity() {
    private lateinit var appUpdater: RuStoreAppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEngineerLocationTracking(this)
        configureAndroidAuthPlatform(this)
        appUpdater = RuStoreAppUpdater(this)
        appUpdater.checkAndStart()
        val koin =
            KoinPlatformTools.defaultContext().getOrNull()
                ?: startKoin {
                    modules(
                        authModule(
                            config = appAuthConfig(),
                            http = createDefaultGatewayHttpClient(),
                        ),
                    )
                }.koin
        setContent {
            AuthenticatedApp(
                coordinator = koin.get(),
                attachmentsRepository = koin.get(),
                commentsRepository = koin.get<CommentsRepository>(),
                adminUsersRepository = koin.get<AdminUsersRepository>(),
                equipmentRepository = koin.get<EquipmentRepository>(),
                workOrdersRepository = koin.get<WorkOrdersRepository>(),
                userScopesRepository = koin.get<UserScopesRepository>(),
                clientEventsRepository = koin.get<ClientEventsRepository>(),
                engineerLocationsRepository = koin.get<EngineerLocationsRepository>(),
                geocodeRepository = koin.get<GeocodeRepository>(),
                aiMessagesRepository = koin.get<AiMessagesRepository>(),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appUpdater.isInitialized) appUpdater.checkAndStart()
    }
}

@Composable
actual fun rememberRootComponent(
    session: ClientSession,
): RootComponent =
    remember(session) {
        DefaultRootComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            session = session,
        )
    }
