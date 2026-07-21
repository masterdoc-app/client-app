package pro.masterdoc.client.presentation.shell

import com.arkivanov.decompose.ComponentContext
import pro.masterdoc.client.navigation.DefaultNavMenuBuilder
import pro.masterdoc.client.navigation.NavMenuBuilder
import pro.masterdoc.client.session.ClientSession

interface RootComponent {
    val shell: MainShellComponent
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    session: ClientSession = ClientSession.stub(),
    navMenuBuilder: NavMenuBuilder = DefaultNavMenuBuilder(),
) : RootComponent, ComponentContext by componentContext {
    override val shell: MainShellComponent =
        DefaultMainShellComponent(
            componentContext = componentContext,
            session = session,
            navMenuBuilder = navMenuBuilder,
        )
}

