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

private fun AssetDto.displayName(): String =
    name.trim().takeIf { it.isNotEmpty() }
        ?: inventoryNo?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Оборудование"
