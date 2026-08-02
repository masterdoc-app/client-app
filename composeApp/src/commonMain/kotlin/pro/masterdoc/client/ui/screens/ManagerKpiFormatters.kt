package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.ManagerKpis

internal data class ManagerKpiDowntimeDisplayRow(
    val label: String,
    val downtimeHours: Double,
    val openIntervals: Int,
)

internal fun formatManagerKpiDowntimeRows(
    kpis: ManagerKpis,
    assets: List<AssetDto>,
): List<ManagerKpiDowntimeDisplayRow> {
    val assetsById = assets.associateBy { it.id }
    return kpis.downtimeRanking.map { row ->
        ManagerKpiDowntimeDisplayRow(
            label = assetsById[row.assetId]?.displayName() ?: "Оборудование",
            downtimeHours = row.downtimeHours,
            openIntervals = row.openIntervals,
        )
    }
}

internal fun formatHours(value: Double): String =
    "${formatOneDecimal(value)} ч"

internal fun formatPercent(value: Double): String =
    "${formatOneDecimal(value)}%"

internal fun formatManagerKpiMetric(
    value: Double,
    sampleSize: Int,
    suffix: String = "",
): String =
    if (sampleSize == 0) {
        "н/д"
    } else {
        "${formatOneDecimal(value)}$suffix"
    }

private fun formatOneDecimal(value: Double): String {
    val scaled = kotlin.math.round(value * 10.0) / 10.0
    val asLong = scaled.toLong()
    return if (scaled == asLong.toDouble()) {
        "$asLong,0"
    } else {
        scaled.toString().replace('.', ',')
    }
}

internal fun AssetDto.displayName(): String =
    name.trim().takeIf { it.isNotEmpty() }
        ?: inventoryNo?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Оборудование"
