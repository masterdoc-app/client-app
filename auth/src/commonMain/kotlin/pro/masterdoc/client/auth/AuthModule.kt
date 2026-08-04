package pro.masterdoc.client.auth

import org.koin.core.module.Module
import org.koin.dsl.module

fun authModule(
    config: AuthConfig,
    http: GatewayHttpClient,
    tokenStore: TokenStore = createDefaultTokenStore(),
    pkceStore: PkceSessionStore = createDefaultPkceSessionStore(),
): Module =
    module {
        single { config }
        single { http }
        single { tokenStore }
        single { pkceStore }
        single {
            AuthRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
                pkceStore = get(),
            )
        }
        single {
            MeRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            AdminUsersRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            EquipmentRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            WorkOrdersRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            AttachmentsRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            CommentsRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            AiMessagesRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            EngineerLocationsRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            GeocodeRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            UserScopesRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            ClientEventsRepository(
                config = get(),
                http = get(),
                tokenStore = get(),
            )
        }
        single {
            AuthCoordinator(
                authRepository = get(),
                meRepository = get(),
            )
        }
    }

expect fun createDefaultTokenStore(): TokenStore

expect fun createDefaultPkceSessionStore(): PkceSessionStore

expect fun createDefaultGatewayHttpClient(): GatewayHttpClient
