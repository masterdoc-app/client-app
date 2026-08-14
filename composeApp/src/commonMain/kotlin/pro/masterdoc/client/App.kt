package pro.masterdoc.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.launch
import pro.masterdoc.client.analytics.AnalyticsSink
import pro.masterdoc.client.analytics.GatewayAnalyticsSink
import pro.masterdoc.client.analytics.NoopAnalyticsSink
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AiMessagesRepository
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.BrowserNav
import pro.masterdoc.client.auth.ClientEventsRepository
import pro.masterdoc.client.auth.CommentsRepository
import pro.masterdoc.client.auth.EngineerLocationsRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GeocodeRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.auth.parseQueryParams
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientTheme
import pro.masterdoc.client.platform.AppTextSelection
import pro.masterdoc.client.presentation.shell.DefaultRootComponent
import pro.masterdoc.client.presentation.shell.RootComponent
import pro.masterdoc.client.session.ClientSession
import pro.masterdoc.client.ui.LoginScreen
import pro.masterdoc.client.ui.shell.MainShellContent

private sealed interface ShellUiState {
    data object Loading : ShellUiState

    data class Login(
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : ShellUiState

    data class Ready(val root: RootComponent) : ShellUiState

    data class Error(val message: String) : ShellUiState
}

@Composable
fun App(root: RootComponent) {
    AppTextSelection {
        ClientTheme {
            MainShellContent(component = root.shell)
        }
    }
}

@Composable
fun App() {
    App(root = rememberRootComponent())
}

/**
 * Production entry: OIDC → GET /me → feature shell. Client never branches on IdP grants.
 */
@Composable
fun AuthenticatedApp(
    coordinator: AuthCoordinator,
    adminUsersRepository: AdminUsersRepository,
    equipmentRepository: EquipmentRepository,
    workOrdersRepository: WorkOrdersRepository,
    attachmentsRepository: AttachmentsRepository,
    commentsRepository: CommentsRepository,
    userScopesRepository: UserScopesRepository,
    clientEventsRepository: ClientEventsRepository? = null,
    engineerLocationsRepository: EngineerLocationsRepository? = null,
    geocodeRepository: GeocodeRepository? = null,
    aiMessagesRepository: AiMessagesRepository? = null,
) {
    AppTextSelection {
        ClientTheme {
            var state by remember { mutableStateOf<ShellUiState>(ShellUiState.Loading) }
            val scope = rememberCoroutineScope()
            val analyticsSink: AnalyticsSink =
                remember(clientEventsRepository) {
                    clientEventsRepository?.let { GatewayAnalyticsSink(it) } ?: NoopAnalyticsSink
                }

            fun logoutAndRestart() {
                scope.launch {
                    if (usesInAppPasswordLogin()) {
                        coordinator.logout()
                        state = ShellUiState.Login()
                        return@launch
                    }
                    state = ShellUiState.Loading
                    runCatching { coordinator.logoutRedirectUrl() }
                        .onSuccess { BrowserNav.navigateTo(it) }
                        .onFailure {
                            // Local clear even if end_session URL cannot be built.
                            coordinator.logout()
                            state = ShellUiState.Error(it.message ?: "Не удалось выйти")
                        }
                }
            }

            fun loginWithPassword(
                email: String,
                password: String,
            ) {
                scope.launch {
                    state = ShellUiState.Login(isLoading = true)
                    try {
                        val me = coordinator.loginWithPassword(email, password)
                        state = ShellUiState.Ready(rootForSession(ClientSession.fromMe(me), analyticsSink))
                    } catch (error: Exception) {
                        coordinator.logout()
                        state = ShellUiState.Login(error = loginErrorMessage(error))
                    }
                }
            }

            LaunchedEffect(coordinator, analyticsSink) {
                state = bootstrap(coordinator, analyticsSink)
            }

            when (val s = state) {
                ShellUiState.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                is ShellUiState.Login ->
                    LoginScreen(
                        isLoading = s.isLoading,
                        error = s.error,
                        onSubmit = ::loginWithPassword,
                    )
                is ShellUiState.Ready ->
                    MainShellContent(
                        component = s.root.shell,
                        onLogout = ::logoutAndRestart,
                        adminUsersRepository = adminUsersRepository,
                        equipmentRepository = equipmentRepository,
                        workOrdersRepository = workOrdersRepository,
                        attachmentsRepository = attachmentsRepository,
                        commentsRepository = commentsRepository,
                        userScopesRepository = userScopesRepository,
                        engineerLocationsGateway = engineerLocationsRepository,
                        geocodeRepository = geocodeRepository,
                        aiMessagesRepository = aiMessagesRepository,
                    )
                is ShellUiState.Error ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppText(text = s.message)
                        AppButton(
                            text = "Повторить",
                            onClick = {
                                when (authErrorRetryMode(BrowserNav.currentPath())) {
                                    AuthErrorRetryMode.NavigateHome ->
                                        BrowserNav.replaceTo("/")
                                    AuthErrorRetryMode.RetryBootstrap ->
                                        scope.launch {
                                            state = ShellUiState.Loading
                                            state = bootstrap(coordinator, analyticsSink)
                                        }
                                }
                            },
                        )
                        AppButton(
                            text = "Выйти",
                            onClick = ::logoutAndRestart,
                        )
                    }
            }
        }
    }
}

private fun rootForSession(
    session: ClientSession,
    analyticsSink: AnalyticsSink,
): RootComponent =
    DefaultRootComponent(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        session = session,
        analyticsSink = analyticsSink,
    )

private suspend fun bootstrap(
    coordinator: AuthCoordinator,
    analyticsSink: AnalyticsSink,
): ShellUiState {
    if (usesInAppPasswordLogin()) {
        if (!coordinator.hasSession()) {
            return ShellUiState.Login()
        }
        return try {
            val me = coordinator.loadMe()
            ShellUiState.Ready(rootForSession(ClientSession.fromMe(me), analyticsSink))
        } catch (_: Exception) {
            coordinator.logout()
            ShellUiState.Login()
        }
    }

    val path = BrowserNav.currentPath()
    if (path.contains("/auth/callback")) {
        val params = parseQueryParams(BrowserNav.currentSearch())
        val error = params["error"]
        if (error != null) {
            return ShellUiState.Error(params["error_description"] ?: error)
        }
        val code =
            params["code"]
                ?: return ShellUiState.Error("Нет code в callback")
        return try {
            val me = coordinator.completeCallback(code, params["state"])
            BrowserNav.replaceTo(postLoginLocation(BrowserNav.consumePendingDeepLink()))
            ShellUiState.Ready(rootForSession(ClientSession.fromMe(me), analyticsSink))
        } catch (e: Exception) {
            ShellUiState.Error(e.message ?: "Ошибка обмена кода")
        }
    }

    if (!coordinator.hasSession()) {
        return startLoginOrError(coordinator)
    }

    return try {
        val me = coordinator.loadMe()
        ShellUiState.Ready(rootForSession(ClientSession.fromMe(me), analyticsSink))
    } catch (_: Exception) {
        coordinator.logout()
        startLoginOrError(coordinator)
    }
}

internal fun loginErrorMessage(error: Throwable): String =
    when ((error as? GatewayHttpException)?.status) {
        401 -> "Неверный email или пароль"
        502 -> "Сервис входа временно недоступен"
        null -> "Сервис входа временно недоступен"
        else -> "Не удалось войти. Попробуйте ещё раз"
    }

private suspend fun startLoginOrError(coordinator: AuthCoordinator): ShellUiState {
    pendingDeepLinkHash(BrowserNav.currentHash())?.let(BrowserNav::savePendingDeepLink)
    return runCatching { coordinator.startLogin() }
        .onSuccess { BrowserNav.navigateTo(it) }
        .fold(
            onSuccess = { ShellUiState.Loading },
            onFailure = { ShellUiState.Error(it.message ?: "Не удалось начать вход") },
        )
}

@Composable
expect fun rememberRootComponent(
    session: ClientSession = ClientSession.stub(),
): RootComponent
