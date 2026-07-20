package pro.masterdoc.client.technolog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.pages.ChildPages
import pro.masterdoc.client.auth.AuthCoordinator
import pro.masterdoc.client.auth.BrowserNav
import pro.masterdoc.client.auth.RoleRouter
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppNavBar
import pro.masterdoc.client.designsystem.components.AppNavItem
import pro.masterdoc.client.designsystem.components.AppNavRail
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientTheme
import pro.masterdoc.client.navigation.NavDestinationId
import pro.masterdoc.client.navigation.NavItemSpec
import pro.masterdoc.client.presentation.shell.MainShellComponent
import pro.masterdoc.client.presentation.shell.RootComponent
import pro.masterdoc.client.session.ClientSession

private val CompactWidthBreakpoint = 600.dp

private sealed interface TechnologUiState {
    data object Loading : TechnologUiState

    data class Ready(val root: RootComponent) : TechnologUiState

    data class Error(val message: String) : TechnologUiState
}

@Composable
fun TechnologApp(
    coordinator: AuthCoordinator,
    rootFactory: (ClientSession) -> RootComponent,
) {
    ClientTheme {
        var state by remember { mutableStateOf<TechnologUiState>(TechnologUiState.Loading) }

        LaunchedEffect(coordinator) {
            if (!coordinator.hasSession()) {
                BrowserNav.replaceTo("/")
                return@LaunchedEffect
            }
            state =
                try {
                    val me = coordinator.loadMe()
                    when (val route = RoleRouter.resolve(me.userInfo.roles)) {
                        is pro.masterdoc.client.auth.RoleRoute.App -> {
                            if (route.path != RoleRouter.TECHNOLOGIST_PATH) {
                                BrowserNav.replaceTo(route.path)
                                TechnologUiState.Loading
                            } else {
                                val session = ClientSession.fromMe(me)
                                TechnologUiState.Ready(rootFactory(session))
                            }
                        }
                        is pro.masterdoc.client.auth.RoleRoute.NoWebApp -> {
                            BrowserNav.replaceTo("/")
                            TechnologUiState.Loading
                        }
                    }
                } catch (e: Exception) {
                    TechnologUiState.Error(e.message ?: "Не удалось загрузить сессию")
                }
        }

        when (val s = state) {
            TechnologUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is TechnologUiState.Ready -> MainShellContent(component = s.root.shell)
            is TechnologUiState.Error ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppText(text = s.message)
                    AppButton(
                        text = "На портал",
                        onClick = {
                            coordinator.logout()
                            BrowserNav.replaceTo("/")
                        },
                    )
                }
        }
    }
}

@Composable
fun MainShellContent(
    component: MainShellComponent,
    modifier: Modifier = Modifier,
) {
    val pages by component.pages.subscribeAsState()
    val navUiItems =
        component.navItems.toAppNavItems(pages) { index ->
            component.onNavItemSelected(index)
        }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= CompactWidthBreakpoint
        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavRail(items = navUiItems)
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    ActivePage(pages)
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = { AppNavBar(items = navUiItems) },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    ActivePage(pages)
                }
            }
        }
    }
}

@Composable
private fun ActivePage(
    pages: ChildPages<MainShellComponent.PageConfig, MainShellComponent.PageChild>,
) {
    val active = pages.items.getOrNull(pages.selectedIndex)?.instance ?: return
    when (active) {
        is MainShellComponent.PageChild.Stub -> StubDestinationScreen(active.destination)
    }
}

@Composable
private fun StubDestinationScreen(
    destination: NavDestinationId,
    modifier: Modifier = Modifier,
) {
    val title = destinationTitle(destination)
    AppScaffold(title = title, modifier = modifier) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = title)
        }
    }
}

private fun destinationTitle(destination: NavDestinationId): String =
    when (destination) {
        NavDestinationId.Tickets -> "Заявки"
        NavDestinationId.Board -> "Доска"
        NavDestinationId.Map -> "Карта"
        NavDestinationId.Charts -> "Графики"
        NavDestinationId.Equipment -> "Оборудование"
        NavDestinationId.Profile -> "Профиль"
        NavDestinationId.Copilot -> "Наставник"
    }

private fun List<NavItemSpec>.toAppNavItems(
    pages: ChildPages<MainShellComponent.PageConfig, MainShellComponent.PageChild>,
    onSelect: (Int) -> Unit,
): List<AppNavItem> =
    mapIndexed { index, spec ->
        AppNavItem(
            key = spec.destination.name,
            label = destinationTitle(spec.destination),
            icon = iconFor(spec.destination),
            selected = pages.selectedIndex == index,
            onClick = { onSelect(index) },
        )
    }

private fun iconFor(destination: NavDestinationId): ImageVector =
    when (destination) {
        NavDestinationId.Tickets -> Icons.Filled.BarChart
        NavDestinationId.Board -> Icons.Filled.BarChart
        NavDestinationId.Map -> Icons.Filled.BarChart
        NavDestinationId.Charts -> Icons.Filled.BarChart
        NavDestinationId.Equipment -> Icons.Filled.PrecisionManufacturing
        NavDestinationId.Profile -> Icons.Filled.Person
        NavDestinationId.Copilot -> Icons.Filled.BarChart
    }
