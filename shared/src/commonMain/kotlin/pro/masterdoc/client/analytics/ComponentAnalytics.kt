package pro.masterdoc.client.analytics

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import pro.masterdoc.client.auth.ClientEventsRepository

fun interface AnalyticsSink {
    fun emit(
        action: String,
        path: String,
        props: Map<String, String>,
    )
}

interface ComponentAnalytics {
    fun track(
        action: String,
        props: Map<String, String> = emptyMap(),
    )
}

object NoopComponentAnalytics : ComponentAnalytics {
    override fun track(
        action: String,
        props: Map<String, String>,
    ) = Unit
}

object NoopAnalyticsSink : AnalyticsSink {
    override fun emit(
        action: String,
        path: String,
        props: Map<String, String>,
    ) = Unit
}

class GatewayAnalyticsSink(
    private val repository: ClientEventsRepository,
) : AnalyticsSink {
    override fun emit(
        action: String,
        path: String,
        props: Map<String, String>,
    ) {
        repository.trackAsync(action = action, path = path, props = props)
    }
}

class DefaultComponentAnalytics(
    private val componentName: String,
    componentContext: ComponentContext,
    private val sink: AnalyticsSink,
) : ComponentAnalytics {
    init {
        componentContext.lifecycle.doOnStart {
            sink.emit(action = "ui.$componentName.open", path = componentName, props = emptyMap())
        }
        componentContext.lifecycle.doOnStop {
            sink.emit(action = "ui.$componentName.close", path = componentName, props = emptyMap())
        }
    }

    override fun track(
        action: String,
        props: Map<String, String>,
    ) {
        sink.emit(action = action, path = componentName, props = props)
    }
}

fun ComponentContext.componentAnalytics(
    componentName: String,
    sink: AnalyticsSink,
): ComponentAnalytics =
    DefaultComponentAnalytics(
        componentName = componentName,
        componentContext = this,
        sink = sink,
    )
