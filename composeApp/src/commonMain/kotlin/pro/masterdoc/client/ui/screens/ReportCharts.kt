package pro.masterdoc.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle

data class ReportChartPoint(
    val label: String,
    val value: Float,
)

@Composable
fun ReportColumnChart(
    points: List<ReportChartPoint>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportCartesianChart(points = points, modifier = Modifier.fillMaxWidth().weight(1f))
        ReportChartLabels(points)
    }
}

@Composable
fun ReportHorizontalBarChart(
    points: List<ReportChartPoint>,
    modifier: Modifier = Modifier,
) {
    // Vico 3.2.3 has no horizontal-bar layer; use columns for the ranking smoke UI.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportCartesianChart(points = points, modifier = Modifier.fillMaxWidth().weight(1f))
        ReportChartLabels(points)
    }
}

@Composable
private fun ReportChartLabels(points: List<ReportChartPoint>) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        points.forEach { point ->
            Column(modifier = Modifier.width(80.dp)) {
                AppText(
                    text = point.label,
                    style = AppTextStyle.Label,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
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
