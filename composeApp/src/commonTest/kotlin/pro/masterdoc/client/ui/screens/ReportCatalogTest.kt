package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportCatalogTest {
    @Test
    fun catalogHasTenReportsInStableOrder() {
        val items = reportCatalogItems()
        assertEquals(10, items.size)
        assertEquals(
            listOf(
                ReportId.KpiSummary,
                ReportId.PlannedVsEmergency,
                ReportId.PprCompliance,
                ReportId.Backlog,
                ReportId.DowntimeRanking,
                ReportId.EquipmentDowntime,
                ReportId.KpiTrends,
                ReportId.ReactiveCompletion,
                ReportId.EngineerWorkload,
                ReportId.FailureFrequency,
            ),
            items.map { it.id },
        )
        assertEquals("Сводка KPI", items.first().title)
        assertEquals("Частота отказов", items.last().title)
        items.forEach { item ->
            assertTrue(item.description.isNotBlank(), "description missing for ${item.id}")
        }
    }
}
