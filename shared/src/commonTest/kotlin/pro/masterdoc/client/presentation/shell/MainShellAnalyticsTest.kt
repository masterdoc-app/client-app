package pro.masterdoc.client.presentation.shell

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertTrue
import pro.masterdoc.client.analytics.AnalyticsSink
import pro.masterdoc.client.navigation.FeatureId
import pro.masterdoc.client.navigation.NavDestinationId
import pro.masterdoc.client.session.ClientSession

class MainShellAnalyticsTest {
    private class RecordingSink : AnalyticsSink {
        val actions = mutableListOf<String>()

        override fun emit(
            action: String,
            path: String,
            props: Map<String, String>,
        ) {
            actions += action
        }
    }

    @Test
    fun onNavItemSelected_tracksIntent() {
        val sink = RecordingSink()
        val shell =
            DefaultMainShellComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                session =
                    ClientSession.stub(
                        features = setOf(FeatureId.Board, FeatureId.Users, FeatureId.Equipment, FeatureId.Profile),
                    ),
                analyticsSink = sink,
            )
        require(shell.navItems.isNotEmpty())
        shell.onNavItemSelected(0)
        assertTrue(sink.actions.contains("ui.shell.nav.select"))
    }

    @Test
    fun navigateTo_tracksIntent() {
        val sink = RecordingSink()
        val shell =
            DefaultMainShellComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                session = ClientSession.stub(features = setOf(FeatureId.Equipment, FeatureId.Profile)),
                analyticsSink = sink,
            )
        shell.navigateTo(NavDestinationId.Equipment)
        assertTrue(sink.actions.contains("ui.shell.nav.navigate"))
    }
}
