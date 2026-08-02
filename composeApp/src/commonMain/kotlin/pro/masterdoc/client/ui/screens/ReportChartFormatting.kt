package pro.masterdoc.client.ui.screens

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
