package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportCatalogTest {
    @Test
    fun catalogHasFourteenReportsInStableOrder() {
        val items = reportCatalogItems()
        assertEquals(14, items.size)
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
                ReportId.EquipmentWorkOrders,
                ReportId.OverdueOpenWorkOrders,
                ReportId.SiteWorkOrders,
                ReportId.TimeToFirstAction,
            ),
            items.map { it.id },
        )
        assertEquals("Сводка KPI", items.first().title)
        assertEquals("Детальный отчёт", items[items.lastIndex - 3].title)
        assertEquals("Просроченные", items[items.lastIndex - 2].title)
        assertEquals("По площадке", items[items.lastIndex - 1].title)
        assertEquals("Время реакции", items.last().title)
        items.forEach { item ->
            assertTrue(item.description.isNotBlank(), "description missing for ${item.id}")
        }
    }
}
