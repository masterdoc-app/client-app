package pro.masterdoc.client.presentation.shell

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import pro.masterdoc.client.navigation.DefaultNavMenuBuilder
import pro.masterdoc.client.navigation.NavDestinationId
import pro.masterdoc.client.navigation.NavItemSpec
import pro.masterdoc.client.navigation.NavMenuBuilder
import pro.masterdoc.client.session.ClientSession

interface MainShellComponent {
    val session: ClientSession
    val navItems: List<NavItemSpec>
    val pages: Value<ChildPages<PageConfig, PageChild>>

    fun onNavItemSelected(index: Int)

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
) : MainShellComponent, ComponentContext by componentContext {
    override val session: ClientSession = session
    override val navItems: List<NavItemSpec> = navMenuBuilder.build(session.features)

    private val navigation = PagesNavigation<MainShellComponent.PageConfig>()

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
        navigation.select(index = index)
    }
}
