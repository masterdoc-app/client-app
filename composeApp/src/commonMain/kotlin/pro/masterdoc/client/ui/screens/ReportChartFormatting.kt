package pro.masterdoc.client.ui.screens

import androidx.compose.ui.graphics.Color
import pro.fixaverse.design.theme.FixaverseLiteTokens

/** Bright Flare-family series for report charts — stays in brand blues + Forest. */
internal val ReportChartSeriesColors: List<Color> =
    listOf(
        Color(0xFF3D8BFF),
        FixaverseLiteTokens.Flare,
        Color(0xFF5BA8FF),
        FixaverseLiteTokens.Forest,
        Color(0xFF2563EB),
        Color(0xFF7CC4FF),
    )

internal fun reportChartBarColor(index: Int): Color =
    ReportChartSeriesColors[index.floorMod(ReportChartSeriesColors.size)]

/** Lighter highlight used at the bright edge of a bar gradient. */
internal fun reportChartBarHighlight(color: Color): Color =
    Color(
        red = (color.red * 0.45f + 0.55f).coerceIn(0f, 1f),
        green = (color.green * 0.45f + 0.55f).coerceIn(0f, 1f),
        blue = (color.blue * 0.35f + 0.65f).coerceIn(0f, 1f),
        alpha = 1f,
    )

private fun Int.floorMod(modulus: Int): Int {
    val r = this % modulus
    return if (r >= 0) r else r + modulus
}

internal fun formatChartValue(value: Float): String {
    val scaled = kotlin.math.round(value * 10f) / 10f
    val asLong = scaled.toLong()
    return if (scaled == asLong.toFloat()) {
        asLong.toString()
    } else {
        scaled.toString().replace('.', ',')
    }
}

internal fun hasNonZeroChartSeries(points: List<ReportChartPoint>): Boolean =
    points.any { it.value != 0f }

internal fun chartMaxValue(points: List<ReportChartPoint>): Float =
    if (points.isEmpty()) {
        1f
    } else {
        points.maxOf { it.value }.coerceAtLeast(1f)
    }
