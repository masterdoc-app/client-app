package pro.masterdoc.client.portal

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
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.BrowserNav
import pro.masterdoc.client.auth.RoleRoute
import pro.masterdoc.client.auth.parseQueryParams
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientTheme

private sealed interface PortalUiState {
    data object Loading : PortalUiState

    data class NoWebApp(val roles: List<String>) : PortalUiState

    data class Error(val message: String) : PortalUiState
}

@Composable
fun PortalApp(coordinator: AuthCoordinator) {
    ClientTheme {
        var state by remember { mutableStateOf<PortalUiState>(PortalUiState.Loading) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(coordinator) {
            state = bootstrap(coordinator)
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                PortalUiState.Loading -> CircularProgressIndicator()
                is PortalUiState.NoWebApp ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppText(text = "Нет web-клиента для вашей роли")
                        AppText(text = "Роли: ${s.roles.joinToString()}")
                        AppButton(
                            text = "Выйти",
                            onClick = {
                                scope.launch {
                                    coordinator.logout()
                                    state = PortalUiState.Loading
                                    state = startLoginOrError(coordinator)
                                }
                            },
                        )
                    }
                is PortalUiState.Error ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppText(text = s.message)
                        AppButton(
                            text = "Повторить",
                            onClick = {
                                scope.launch {
                                    state = PortalUiState.Loading
                                    state = startLoginOrError(coordinator)
                                }
                            },
                        )
                    }
            }
        }
    }
}

private suspend fun bootstrap(coordinator: AuthCoordinator): PortalUiState {
    val path = BrowserNav.currentPath()
    if (path.contains("/auth/callback")) {
        val params = parseQueryParams(BrowserNav.currentSearch())
        val error = params["error"]
        if (error != null) {
            return PortalUiState.Error(params["error_description"] ?: error)
        }
        val code = params["code"]
            ?: return PortalUiState.Error("Нет code в callback")
        return try {
            when (val route = coordinator.completeCallback(code, params["state"])) {
                is RoleRoute.App -> {
                    BrowserNav.replaceTo(route.path)
                    PortalUiState.Loading
                }
                is RoleRoute.NoWebApp -> PortalUiState.NoWebApp(route.roles)
            }
        } catch (e: Exception) {
            PortalUiState.Error(e.message ?: "Ошибка обмена кода")
        }
    }

    if (!coordinator.hasSession()) {
        return startLoginOrError(coordinator)
    }

    return try {
        when (val route = coordinator.resolveRouteForCurrentSession()) {
            is RoleRoute.App -> {
                BrowserNav.replaceTo(route.path)
                PortalUiState.Loading
            }
            is RoleRoute.NoWebApp -> PortalUiState.NoWebApp(route.roles)
        }
    } catch (e: Exception) {
        coordinator.logout()
        startLoginOrError(coordinator)
    }
}

private suspend fun startLoginOrError(coordinator: AuthCoordinator): PortalUiState =
    runCatching { coordinator.startLogin() }
        .onSuccess { BrowserNav.navigateTo(it) }
        .fold(
            onSuccess = { PortalUiState.Loading },
            onFailure = { PortalUiState.Error(it.message ?: "Не удалось начать вход") },
        )
