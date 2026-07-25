package pro.masterdoc.client.presentation.shell

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import pro.masterdoc.client.analytics.AnalyticsSink
import pro.masterdoc.client.analytics.ComponentAnalytics
import pro.masterdoc.client.analytics.NoopAnalyticsSink
import pro.masterdoc.client.analytics.componentAnalytics
import pro.masterdoc.client.navigation.AppDeepLink
import pro.masterdoc.client.navigation.DefaultNavMenuBuilder
import pro.masterdoc.client.navigation.NavDestinationId
import pro.masterdoc.client.navigation.NavItemSpec
import pro.masterdoc.client.navigation.NavMenuBuilder
import pro.masterdoc.client.navigation.parseAppDeepLink
import pro.masterdoc.client.session.ClientSession

interface MainShellComponent {
    val session: ClientSession
    val navItems: List<NavItemSpec>
    val pages: Value<ChildPages<PageConfig, PageChild>>

    /** Focused maintenance map id from deep link `#/ppr/{id}` (empty when unset). */
    val focusedMapId: Value<String>

    fun onNavItemSelected(index: Int)

    fun navigateTo(destination: NavDestinationId, mapId: String? = null)

    fun applyDeepLinkHash(hash: String)

    @Serializable
    data class PageConfig(val destination: NavDestinationId)

    sealed interface PageChild {
        val destination: NavDestinationId

        data class Stub(override val destination: NavDestinationId) : PageChild
    }
}

class DefaultMainShellComponent(
    componentContext: ComponentContext,
    session: ClientSession,
    navMenuBuilder: NavMenuBuilder = DefaultNavMenuBuilder(),
    analyticsSink: AnalyticsSink = NoopAnalyticsSink,
) : MainShellComponent,
    ComponentContext by componentContext,
    ComponentAnalytics by componentContext.componentAnalytics("MainShell", analyticsSink) {
    override val session: ClientSession = session
    override val navItems: List<NavItemSpec> = navMenuBuilder.build(session.features)

    private val navigation = PagesNavigation<MainShellComponent.PageConfig>()
    private val _focusedMapId = MutableValue("")
    override val focusedMapId: Value<String> = _focusedMapId

    override val pages: Value<ChildPages<MainShellComponent.PageConfig, MainShellComponent.PageChild>> =
        childPages(
            source = navigation,
            serializer = MainShellComponent.PageConfig.serializer(),
            initialPages = {
                Pages(
                    items = navItems.map { MainShellComponent.PageConfig(it.destination) },
                    selectedIndex = 0,
                )
            },
            childFactory = { config, _ ->
                MainShellComponent.PageChild.Stub(config.destination)
            },
        )

    override fun onNavItemSelected(index: Int) {
        val destination = navItems.getOrNull(index)?.destination?.name.orEmpty()
        track(
            "ui.shell.nav.select",
            mapOf(
                "index" to index.toString(),
                "destination" to destination,
            ),
        )
        navigation.select(index = index)
    }

    override fun navigateTo(destination: NavDestinationId, mapId: String?) {
        val index = navItems.indexOfFirst { it.destination == destination }
        if (index < 0) return
        track(
            "ui.shell.nav.navigate",
            mapOf(
                "destination" to destination.name,
                "mapId" to mapId.orEmpty(),
            ),
        )
        _focusedMapId.value = mapId.orEmpty()
        navigation.select(index = index)
    }

    override fun applyDeepLinkHash(hash: String) {
        val link = parseAppDeepLink(hash) ?: return
        track("ui.shell.nav.deeplink", mapOf("hash" to hash))
        when (link) {
            is AppDeepLink.Ppr -> navigateTo(NavDestinationId.Charts, mapId = link.mapId)
            AppDeepLink.Charts -> navigateTo(NavDestinationId.Charts, mapId = null)
            AppDeepLink.Equipment -> navigateTo(NavDestinationId.Equipment, mapId = null)
        }
    }
}
