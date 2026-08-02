package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.ManagerKpiDowntimeRow
import pro.masterdoc.client.auth.ManagerKpis

class ManagerKpiFormattersTest {
    @Test
    fun usesAssetNameInsteadOfRawId() {
        val rows =
            formatManagerKpiDowntimeRows(
                kpis = kpis(ManagerKpiDowntimeRow("asset-1", 18.5, 1)),
                assets = listOf(asset(id = "asset-1", name = "Насос №1", inventoryNo = "INV-1")),
            )

        assertEquals("Насос №1", rows.single().label)
        assertEquals(false, rows.single().label.contains("asset-1"))
    }

    @Test
    fun fallsBackToInventoryNumberWhenNameIsBlank() {
        val rows =
            formatManagerKpiDowntimeRows(
                kpis = kpis(ManagerKpiDowntimeRow("asset-2", 4.0, 0)),
                assets = listOf(asset(id = "asset-2", name = " ", inventoryNo = "INV-2")),
            )

        assertEquals("INV-2", rows.single().label)
    }

    @Test
    fun fallsBackToGenericEquipmentLabelWhenAssetIsUnknown() {
        val rows =
            formatManagerKpiDowntimeRows(
                kpis = kpis(ManagerKpiDowntimeRow("missing-asset", 2.0, 0)),
                assets = emptyList(),
            )

        assertEquals("Оборудование", rows.single().label)
        assertEquals(false, rows.single().label.contains("missing-asset"))
    }

    private fun kpis(row: ManagerKpiDowntimeRow) =
        ManagerKpis(
            from = "2026-07-01",
            to = "2026-07-31",
            mttrHours = 0.0,
            mttrSampleSize = 0,
            mtbfHours = 0.0,
            mtbfSampleSize = 0,
            plannedCount = 0,
            emergencyCount = 0,
            plannedHours = 0.0,
            emergencyHours = 0.0,
            pprOnTime = 0,
            pprLate = 0,
            pprOpenOverdue = 0,
            pprOpenPending = 0,
            backlogUnder7d = 0,
            backlog7to30d = 0,
            backlogOver30d = 0,
            backlogOverdue = 0,
            downtimeRanking = listOf(row),
            availabilityPercent = 100.0,
        )

    private fun asset(id: String, name: String, inventoryNo: String?) =
        AssetDto(
            id = id,
            orgId = "org",
            siteId = "site",
            name = name,
            inventoryNo = inventoryNo,
            status = "active",
            source = "manual",
        )
}
