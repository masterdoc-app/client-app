package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
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
    ReportBarChart(points = points, horizontal = false, modifier = modifier)
}

@Composable
fun ReportHorizontalBarChart(
    points: List<ReportChartPoint>,
    modifier: Modifier = Modifier,
) {
    ReportBarChart(points = points, horizontal = true, modifier = modifier)
}

@Composable
private fun ReportBarChart(
    points: List<ReportChartPoint>,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val maxValue = points.maxOf { it.value }.coerceAtLeast(1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (horizontal) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                points.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppText(
                            text = point.label,
                            style = AppTextStyle.Label,
                            modifier = Modifier.width(120.dp),
                        )
                        Canvas(modifier = Modifier.weight(1f).height(18.dp)) {
                            drawRoundRect(
                                color = trackColor,
                                cornerRadius = CornerRadius(6.dp.toPx()),
                                size = size,
                            )
                            val barWidth = size.width * (point.value / maxValue)
                            drawRoundRect(
                                color = barColor,
                                cornerRadius = CornerRadius(6.dp.toPx()),
                                size = Size(barWidth, size.height),
                            )
                        }
                        AppText(
                            text = formatChartValue(point.value),
                            style = AppTextStyle.Label,
                            modifier = Modifier.width(48.dp),
                        )
                    }
                }
            }
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val slotWidth = size.width / points.size
                val barWidth = slotWidth * 0.55f
                val baseline = size.height
                points.forEachIndexed { index, point ->
                    val barHeight = size.height * (point.value / maxValue)
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, baseline - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                points.forEach { point ->
                    Column(
                        modifier = Modifier.width(80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppText(text = point.label, style = AppTextStyle.Label)
                        AppText(text = formatChartValue(point.value), style = AppTextStyle.Label)
                    }
                }
            }
        }
    }
}

private fun formatChartValue(value: Float): String {
    val scaled = kotlin.math.round(value * 10f) / 10f
    val asLong = scaled.toLong()
    return if (scaled == asLong.toFloat()) {
        asLong.toString()
    } else {
        scaled.toString().replace('.', ',')
    }
}
