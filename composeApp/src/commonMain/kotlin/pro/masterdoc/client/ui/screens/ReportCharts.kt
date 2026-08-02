package pro.masterdoc.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

data class ReportChartPoint(
    val label: String,
    val value: Float,
)

@Composable
fun ReportColumnChart(
    points: List<ReportChartPoint>,
    modifier: Modifier = Modifier,
) {
    ReportCartesianChart(points = points, modifier = modifier)
}

@Composable
fun ReportHorizontalBarChart(
    points: List<ReportChartPoint>,
    modifier: Modifier = Modifier,
) {
    // Vico 3.2.3 has no horizontal-bar layer; use columns for the ranking smoke UI.
    ReportCartesianChart(points = points, modifier = modifier)
}

@Composable
private fun ReportCartesianChart(
    points: List<ReportChartPoint>,
    modifier: Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            columnSeries { series(points.map { it.value }) }
        }
    }

    CartesianChartHost(
        chart =
            rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
