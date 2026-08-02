package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class ReportCatalogTest {
    @Test
    fun catalogHasSixReportsInStableOrder() {
        val items = reportCatalogItems()
        assertEquals(6, items.size)
        assertEquals(
            listOf(
                ReportId.KpiSummary,
                ReportId.PlannedVsEmergency,
                ReportId.PprCompliance,
                ReportId.Backlog,
                ReportId.DowntimeRanking,
                ReportId.EquipmentDowntime,
            ),
            items.map { it.id },
        )
        assertEquals("Сводка KPI", items.first().title)
        assertEquals("Простои оборудования", items.last().title)
    }
}
