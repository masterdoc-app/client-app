package pro.masterdoc.client.analytics

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComponentAnalyticsTest {
    private class RecordingSink : AnalyticsSink {
        val events = mutableListOf<Triple<String, String, Map<String, String>>>()

        override fun emit(
            action: String,
            path: String,
            props: Map<String, String>,
        ) {
            events += Triple(action, path, props)
        }
    }

    @Test
    fun track_emitsToSinkWithComponentPath() {
        val sink = RecordingSink()
        val lifecycle = LifecycleRegistry()
        val analytics =
            DefaultComponentAnalytics(
                componentName = "MainShell",
                componentContext = DefaultComponentContext(lifecycle),
                sink = sink,
            )
        analytics.track("ui.shell.nav.select", mapOf("destination" to "users"))
        assertEquals(1, sink.events.size)
        assertEquals("ui.shell.nav.select", sink.events[0].first)
        assertEquals("MainShell", sink.events[0].second)
        assertEquals("users", sink.events[0].third["destination"])
    }

    @Test
    fun lifecycle_emitsOpenAndClose() {
        val sink = RecordingSink()
        val lifecycle = LifecycleRegistry()
        DefaultComponentAnalytics(
            componentName = "Root",
            componentContext = DefaultComponentContext(lifecycle),
            sink = sink,
        )
        lifecycle.resume()
        assertTrue(sink.events.any { it.first == "ui.Root.open" })
        lifecycle.stop()
        assertTrue(sink.events.any { it.first == "ui.Root.close" })
    }

    @Test
    fun noop_emitsNothing() {
        val noop = NoopComponentAnalytics
        noop.track("ui.anything")
        // No crash; noop has no sink — verified by type alone / no exception
    }
}
