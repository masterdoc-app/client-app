package pro.masterdoc.client.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.pages.ChildPages
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.BrowserNav
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.designsystem.components.AppNavBar
import pro.masterdoc.client.designsystem.components.AppNavItem
import pro.masterdoc.client.designsystem.components.AppNavRail
import pro.masterdoc.client.navigation.AppDeepLink
import pro.masterdoc.client.navigation.NavDestinationId
import pro.masterdoc.client.navigation.NavItemSpec
import pro.masterdoc.client.navigation.toHash
import pro.masterdoc.client.presentation.shell.MainShellComponent
import pro.masterdoc.client.session.ClientSession
import pro.masterdoc.client.ui.screens.ChartsScreen
import pro.masterdoc.client.ui.screens.EquipmentScreen
import pro.masterdoc.client.ui.screens.ProfileScreen
import pro.masterdoc.client.ui.screens.StubDestinationScreen
import pro.masterdoc.client.ui.screens.UsersScreen
import pro.masterdoc.client.ui.screens.destinationTitle

private val CompactWidthBreakpoint = 600.dp

@Composable
fun MainShellContent(
    component: MainShellComponent,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    adminUsersRepository: AdminUsersRepository? = null,
    equipmentRepository: EquipmentRepository? = null,
) {
    val pages by component.pages.subscribeAsState()
    val focusedMapId by component.focusedMapId.subscribeAsState()
    val navUiItems = component.navItems.toAppNavItems(pages) { index ->
        component.onNavItemSelected(index)
    }

    LaunchedEffect(Unit) {
        component.applyDeepLinkHash(BrowserNav.currentHash())
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= CompactWidthBreakpoint
        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavRail(items = navUiItems)
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    ActivePage(
                        pages = pages,
                        session = component.session,
                        onLogout = onLogout,
                        adminUsersRepository = adminUsersRepository,
                        equipmentRepository = equipmentRepository,
                        focusedMapId = focusedMapId,
                        onOpenLinkedPpr = { map ->
                            BrowserNav.setHash(AppDeepLink.Ppr(map.id).toHash())
                            component.navigateTo(NavDestinationId.Charts, mapId = map.id)
                        },
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = { AppNavBar(items = navUiItems) },
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    ActivePage(
                        pages = pages,
                        session = component.session,
                        onLogout = onLogout,
                        adminUsersRepository = adminUsersRepository,
                        equipmentRepository = equipmentRepository,
                        focusedMapId = focusedMapId,
                        onOpenLinkedPpr = { map ->
                            BrowserNav.setHash(AppDeepLink.Ppr(map.id).toHash())
                            component.navigateTo(NavDestinationId.Charts, mapId = map.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePage(
    pages: ChildPages<MainShellComponent.PageConfig, MainShellComponent.PageChild>,
    session: ClientSession,
    onLogout: () -> Unit,
    adminUsersRepository: AdminUsersRepository?,
    equipmentRepository: EquipmentRepository?,
    focusedMapId: String?,
    onOpenLinkedPpr: (pro.masterdoc.client.auth.MaintenanceMapDto) -> Unit,
) {
    val active = pages.items.getOrNull(pages.selectedIndex)?.instance ?: return
    when (active) {
        is MainShellComponent.PageChild.Stub ->
            when (active.destination) {
                NavDestinationId.Profile ->
                    ProfileScreen(
                        onLogout = onLogout,
                        user = session.user,
                        features = session.features,
                    )
                NavDestinationId.Users ->
                    if (adminUsersRepository != null) {
                        UsersScreen(
                            repository = adminUsersRepository,
                            equipmentRepository = equipmentRepository,
                            currentUserId = session.user?.id,
                        )
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                NavDestinationId.Equipment ->
                    if (equipmentRepository != null) {
                        EquipmentScreen(
                            repository = equipmentRepository,
                            onOpenLinkedPpr = onOpenLinkedPpr,
                        )
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                NavDestinationId.Charts ->
                    if (equipmentRepository != null) {
                        ChartsScreen(
                            repository = equipmentRepository,
                            focusedMapId = focusedMapId?.takeIf { it.isNotBlank() },
                        )
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                else -> StubDestinationScreen(active.destination)
            }
    }
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
        NavDestinationId.Tickets -> Icons.Filled.Assignment
        NavDestinationId.Board -> Icons.Filled.Dashboard
        NavDestinationId.Map -> Icons.Filled.Map
        NavDestinationId.Charts -> Icons.Filled.BarChart
        NavDestinationId.Equipment -> Icons.Filled.PrecisionManufacturing
        NavDestinationId.Profile -> Icons.Filled.Person
        NavDestinationId.Copilot -> Icons.Filled.SmartToy
        NavDestinationId.Users -> Icons.Filled.PersonAdd
    }
