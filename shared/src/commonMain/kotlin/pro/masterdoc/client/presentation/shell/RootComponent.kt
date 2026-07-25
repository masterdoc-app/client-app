package pro.masterdoc.client.presentation.shell

import com.arkivanov.decompose.ComponentContext
import pro.masterdoc.client.analytics.AnalyticsSink
import pro.masterdoc.client.analytics.ComponentAnalytics
import pro.masterdoc.client.analytics.NoopAnalyticsSink
import pro.masterdoc.client.analytics.componentAnalytics
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
    analyticsSink: AnalyticsSink = NoopAnalyticsSink,
) : RootComponent,
    ComponentContext by componentContext,
    ComponentAnalytics by componentContext.componentAnalytics("Root", analyticsSink) {
    override val shell: MainShellComponent =
        DefaultMainShellComponent(
            componentContext = componentContext,
            session = session,
            navMenuBuilder = navMenuBuilder,
            analyticsSink = analyticsSink,
        )
}
