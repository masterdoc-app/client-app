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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.pages.ChildPages
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.BrowserNav
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.EngineerLocationsGateway
import pro.masterdoc.client.auth.GeocodeRepository
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.navigation.FeatureId
import pro.masterdoc.client.designsystem.components.AppNavBar
import pro.masterdoc.client.designsystem.components.AppNavItem
import pro.masterdoc.client.designsystem.components.AppNavRail
import pro.masterdoc.client.navigation.AppDeepLink
import pro.masterdoc.client.navigation.NavDestinationId
import pro.masterdoc.client.navigation.NavItemSpec
import pro.masterdoc.client.navigation.toHash
import pro.masterdoc.client.presentation.shell.MainShellComponent
import pro.masterdoc.client.session.ClientSession
import pro.masterdoc.client.session.SessionUser
import pro.masterdoc.client.ui.screens.BlackBoxScreen
import pro.masterdoc.client.ui.screens.BoardScreen
import pro.masterdoc.client.ui.screens.ChartsScreen
import pro.masterdoc.client.ui.screens.EquipmentDetailScreen
import pro.masterdoc.client.ui.screens.EquipmentScreen
import pro.masterdoc.client.ui.screens.MapScreen
import pro.masterdoc.client.ui.screens.ProfileScreen
import pro.masterdoc.client.ui.screens.MyWorkOrdersScreen
import pro.masterdoc.client.ui.screens.TicketsScreen
import pro.masterdoc.client.ui.screens.StubDestinationScreen
import pro.masterdoc.client.ui.screens.UsersScreen
import pro.masterdoc.client.ui.screens.destinationTitle
import pro.masterdoc.client.tracking.LocationTrackingController
import pro.masterdoc.client.tracking.createEngineerLocationPingSource

private val CompactWidthBreakpoint = 600.dp

@Composable
fun MainShellContent(
    component: MainShellComponent,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    adminUsersRepository: AdminUsersRepository? = null,
    equipmentRepository: EquipmentRepository? = null,
    workOrdersRepository: WorkOrdersRepository? = null,
    userScopesRepository: UserScopesRepository? = null,
    engineerLocationsGateway: EngineerLocationsGateway? = null,
    geocodeRepository: GeocodeRepository? = null,
) {
    val pages by component.pages.subscribeAsState()
    val focusedMapId by component.focusedMapId.subscribeAsState()
    val focusedAssetId by component.focusedAssetId.subscribeAsState()
    val navUiItems = component.navItems.toAppNavItems(pages) { index ->
        component.onNavItemSelected(index)
    }
    val locationTrackingController =
        remember(engineerLocationsGateway, component.session.user) {
            engineerLocationsGateway?.let { repository ->
                LocationTrackingController(
                    repository = repository,
                    locationSource = createEngineerLocationPingSource(),
                    displayName = { component.session.user?.displayName() },
                )
            }
        }
    DisposableEffect(locationTrackingController) {
        onDispose { locationTrackingController?.close() }
    }

    LaunchedEffect(Unit) {
        component.applyDeepLinkHash(BrowserNav.currentHash())
    }
    val onOpenEquipment: (String) -> Unit = { id ->
        BrowserNav.setHash(AppDeepLink.EquipmentDetail(id).toHash())
        component.navigateTo(NavDestinationId.Equipment, assetId = id)
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
                        workOrdersRepository = workOrdersRepository,
                        userScopesRepository = userScopesRepository,
                        engineerLocationsGateway = engineerLocationsGateway,
                        geocodeRepository = geocodeRepository,
                        locationTrackingController = locationTrackingController,
                        focusedMapId = focusedMapId,
                        focusedAssetId = focusedAssetId,
                        onOpenEquipment = onOpenEquipment,
                        onEquipmentBack = {
                            BrowserNav.setHash(AppDeepLink.Equipment.toHash())
                            component.navigateTo(NavDestinationId.Equipment, assetId = null)
                        },
                        onOpenLinkedPpr = { map ->
                            BrowserNav.setHash(AppDeepLink.Ppr(map.id).toHash())
                            component.navigateTo(NavDestinationId.Charts, mapId = map.id)
                        },
                        onPprDraftReady = { mapId ->
                            BrowserNav.setHash(AppDeepLink.Ppr(mapId).toHash())
                            component.navigateTo(NavDestinationId.Charts, mapId = mapId)
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
                        workOrdersRepository = workOrdersRepository,
                        userScopesRepository = userScopesRepository,
                        engineerLocationsGateway = engineerLocationsGateway,
                        geocodeRepository = geocodeRepository,
                        locationTrackingController = locationTrackingController,
                        focusedMapId = focusedMapId,
                        focusedAssetId = focusedAssetId,
                        onOpenEquipment = onOpenEquipment,
                        onEquipmentBack = {
                            BrowserNav.setHash(AppDeepLink.Equipment.toHash())
                            component.navigateTo(NavDestinationId.Equipment, assetId = null)
                        },
                        onOpenLinkedPpr = { map ->
                            BrowserNav.setHash(AppDeepLink.Ppr(map.id).toHash())
                            component.navigateTo(NavDestinationId.Charts, mapId = map.id)
                        },
                        onPprDraftReady = { mapId ->
                            BrowserNav.setHash(AppDeepLink.Ppr(mapId).toHash())
                            component.navigateTo(NavDestinationId.Charts, mapId = mapId)
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
    workOrdersRepository: WorkOrdersRepository?,
    userScopesRepository: UserScopesRepository?,
    engineerLocationsGateway: EngineerLocationsGateway?,
    geocodeRepository: GeocodeRepository?,
    locationTrackingController: LocationTrackingController?,
    focusedMapId: String?,
    focusedAssetId: String?,
    onOpenEquipment: (String) -> Unit,
    onEquipmentBack: () -> Unit,
    onOpenLinkedPpr: (pro.masterdoc.client.auth.MaintenanceMapDto) -> Unit,
    onPprDraftReady: (mapId: String) -> Unit,
) {
    val active = pages.items.getOrNull(pages.selectedIndex)?.instance ?: return
    when (active) {
        is MainShellComponent.PageChild.MyWorkOrders ->
            if (workOrdersRepository != null) {
                MyWorkOrdersScreen(
                    repository = workOrdersRepository,
                    equipmentRepository = equipmentRepository,
                    currentUserId = session.user?.id,
                    onOpenEquipment = onOpenEquipment,
                    locationTrackingController = locationTrackingController,
                )
            } else {
                StubDestinationScreen(active.destination)
            }
        is MainShellComponent.PageChild.Stub ->
            when (active.destination) {
                NavDestinationId.Tickets ->
                    if (workOrdersRepository != null && equipmentRepository != null) {
                        TicketsScreen(
                            repository = workOrdersRepository,
                            equipmentRepository = equipmentRepository,
                            currentUserId = session.user?.id,
                            userScopesRepository = userScopesRepository,
                        )
                    } else {
                        StubDestinationScreen(active.destination)
                    }
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
                            userScopesRepository = userScopesRepository,
                            geocodeRepository = geocodeRepository,
                            currentUserId = session.user?.id,
                        )
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                NavDestinationId.BlackBox ->
                    if (adminUsersRepository != null) {
                        BlackBoxScreen(repository = adminUsersRepository)
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                NavDestinationId.Equipment ->
                    if (equipmentRepository != null) {
                        val focus = focusedAssetId?.takeIf { it.isNotBlank() }
                        if (focus != null) {
                            EquipmentDetailScreen(
                                assetId = focus,
                                repository = equipmentRepository,
                                onBack = onEquipmentBack,
                                onOpenLinkedPpr = onOpenLinkedPpr,
                                onPprDraftReady = onPprDraftReady,
                            )
                        } else {
                            EquipmentScreen(
                                repository = equipmentRepository,
                                onOpenLinkedPpr = onOpenLinkedPpr,
                                onPprDraftReady = onPprDraftReady,
                            )
                        }
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                NavDestinationId.Charts ->
                    if (equipmentRepository != null) {
                        ChartsScreen(
                            repository = equipmentRepository,
                            focusedMapId = focusedMapId?.takeIf { it.isNotBlank() },
                            onOpenEquipment = onOpenEquipment,
                        )
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                NavDestinationId.Board ->
                    if (workOrdersRepository != null) {
                        BoardScreen(
                            repository = workOrdersRepository,
                            userScopesRepository = userScopesRepository,
                            equipmentRepository = equipmentRepository,
                            adminUsersRepository = adminUsersRepository,
                            hasAdminUsers = FeatureId.Users in session.features,
                            currentUserId = session.user?.id,
                            dispatcherMode = FeatureId.Board in session.features,
                            onOpenEquipment = onOpenEquipment,
                        )
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                NavDestinationId.Map ->
                    if (engineerLocationsGateway != null) {
                        MapScreen(repository = engineerLocationsGateway)
                    } else {
                        StubDestinationScreen(active.destination)
                    }
                else -> StubDestinationScreen(active.destination)
            }
    }
}

private fun SessionUser.displayName(): String? =
    listOfNotNull(givenName, familyName).joinToString(" ").trim().takeIf { it.isNotEmpty() } ?: email

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
        NavDestinationId.MyWorkOrders -> Icons.Filled.ListAlt
        NavDestinationId.Map -> Icons.Filled.Map
        NavDestinationId.Charts -> Icons.Filled.BarChart
        NavDestinationId.Equipment -> Icons.Filled.PrecisionManufacturing
        NavDestinationId.Profile -> Icons.Filled.Person
        NavDestinationId.BlackBox -> Icons.Filled.History
        NavDestinationId.Users -> Icons.Filled.AdminPanelSettings
    }
