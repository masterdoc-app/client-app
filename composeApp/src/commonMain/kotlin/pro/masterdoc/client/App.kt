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
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.BrowserNav
import pro.masterdoc.client.auth.parseQueryParams
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientTheme
import pro.masterdoc.client.presentation.shell.DefaultRootComponent
import pro.masterdoc.client.presentation.shell.RootComponent
import pro.masterdoc.client.session.ClientSession
import pro.masterdoc.client.ui.shell.MainShellContent

private sealed interface ShellUiState {
    data object Loading : ShellUiState

    data class Ready(val root: RootComponent) : ShellUiState

    data class Error(val message: String) : ShellUiState
}

@Composable
fun App(root: RootComponent) {
    ClientTheme {
        MainShellContent(component = root.shell)
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
) {
    ClientTheme {
        var state by remember { mutableStateOf<ShellUiState>(ShellUiState.Loading) }
        val scope = rememberCoroutineScope()

        fun logoutAndRestart() {
            scope.launch {
                coordinator.logout()
                state = ShellUiState.Loading
                state = bootstrap(coordinator)
            }
        }

        LaunchedEffect(coordinator) {
            state = bootstrap(coordinator)
        }

        when (val s = state) {
            ShellUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is ShellUiState.Ready ->
                MainShellContent(
                    component = s.root.shell,
                    onLogout = ::logoutAndRestart,
                    adminUsersRepository = adminUsersRepository,
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
                            scope.launch {
                                state = ShellUiState.Loading
                                state = bootstrap(coordinator)
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

private fun rootForSession(session: ClientSession): RootComponent =
    DefaultRootComponent(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        session = session,
    )

private suspend fun bootstrap(coordinator: AuthCoordinator): ShellUiState {
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
            BrowserNav.replaceTo("/")
            ShellUiState.Ready(rootForSession(ClientSession.fromMe(me)))
        } catch (e: Exception) {
            ShellUiState.Error(e.message ?: "Ошибка обмена кода")
        }
    }

    if (!coordinator.hasSession()) {
        return startLoginOrError(coordinator)
    }

    return try {
        val me = coordinator.loadMe()
        ShellUiState.Ready(rootForSession(ClientSession.fromMe(me)))
    } catch (_: Exception) {
        coordinator.logout()
        startLoginOrError(coordinator)
    }
}

private suspend fun startLoginOrError(coordinator: AuthCoordinator): ShellUiState =
    runCatching { coordinator.startLogin() }
        .onSuccess { BrowserNav.navigateTo(it) }
        .fold(
            onSuccess = { ShellUiState.Loading },
            onFailure = { ShellUiState.Error(it.message ?: "Не удалось начать вход") },
        )

@Composable
expect fun rememberRootComponent(
    session: ClientSession = ClientSession.stub(),
): RootComponent
