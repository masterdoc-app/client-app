package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import pro.fixaverse.design.theme.FixaverseLiteTokens
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
    if (points.isEmpty() || !hasNonZeroChartSeries(points)) return
    val trackColor = FixaverseLiteTokens.FlareSoft
    val maxValue = chartMaxValue(points)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (horizontal) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                points.forEachIndexed { index, point ->
                    val barColor = reportChartBarColor(index)
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
                        Canvas(modifier = Modifier.weight(1f).height(20.dp)) {
                            drawRoundRect(
                                color = trackColor,
                                cornerRadius = CornerRadius(8.dp.toPx()),
                                size = size,
                            )
                            val barWidth = size.width * (point.value / maxValue)
                            if (barWidth > 0f) {
                                drawRoundRect(
                                    brush =
                                        Brush.horizontalGradient(
                                            colors =
                                                listOf(
                                                    reportChartBarHighlight(barColor),
                                                    barColor,
                                                ),
                                            startX = 0f,
                                            endX = barWidth,
                                        ),
                                    cornerRadius = CornerRadius(8.dp.toPx()),
                                    size = Size(barWidth, size.height),
                                )
                            }
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
                val barWidth = slotWidth * 0.58f
                val baseline = size.height
                points.forEachIndexed { index, point ->
                    val barColor = reportChartBarColor(index)
                    val barHeight = size.height * (point.value / maxValue)
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val top = baseline - barHeight
                    if (barHeight > 0f) {
                        drawRoundRect(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            reportChartBarHighlight(barColor),
                                            barColor,
                                        ),
                                    startY = top,
                                    endY = baseline,
                                ),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                points.forEachIndexed { index, point ->
                    Column(
                        modifier = Modifier.width(80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .background(reportChartBarColor(index), CircleShape),
                        )
                        AppText(text = point.label, style = AppTextStyle.Label)
                        AppText(text = formatChartValue(point.value), style = AppTextStyle.Label)
                    }
                }
            }
        }
    }
}
