package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.EngineerWorkloadRow
import pro.masterdoc.client.auth.FailureFrequencyRow
import pro.masterdoc.client.auth.KpiTrendPoint
import pro.masterdoc.client.auth.ManagerKpiDowntimeRow
import pro.masterdoc.client.auth.ManagerKpis

class ReportChartModelsTest {
    @Test
    fun kpiSummarySeriesUsesMetricValues() {
        val points = kpiSummaryChartPoints(sampleKpis(mttrHours = 4.5, mtbfHours = 20.0, availabilityPercent = 98.2))

        assertEquals(listOf("MTTR", "MTBF", "Готовность"), points.map { it.label })
        assertEquals(listOf(4.5f, 20.0f, 98.2f), points.map { it.value })
    }

    @Test
    fun plannedVsEmergencySeriesUsesCounts() {
        val points = plannedVsEmergencyChartPoints(sampleKpis(plannedCount = 10, emergencyCount = 4))

        assertEquals(listOf("Плановые", "Аварийные"), points.map { it.label })
        assertEquals(listOf(10f, 4f), points.map { it.value })
    }

    @Test
    fun pprSeriesUsesFourStatuses() {
        val points = pprComplianceChartPoints(sampleKpis(pprOnTime = 8, pprLate = 2, pprOpenOverdue = 3, pprOpenPending = 5))

        assertEquals(listOf("Вовремя", "С опозданием", "Просрочено", "В срок"), points.map { it.label })
        assertEquals(listOf(8f, 2f, 3f, 5f), points.map { it.value })
    }

    @Test
    fun backlogSeriesUsesAgeBuckets() {
        val points = backlogChartPoints(sampleKpis(backlogUnder7d = 6, backlog7to30d = 4, backlogOver30d = 1))

        assertEquals(listOf("<7 дн", "7–30 дн", ">30 дн"), points.map { it.label })
        assertEquals(listOf(6f, 4f, 1f), points.map { it.value })
    }

    @Test
    fun downtimeRankingUsesAssetNames() {
        val points =
            downtimeRankingChartPoints(
                kpis = sampleKpis(ranking = listOf(ManagerKpiDowntimeRow("a1", 18.5, 1))),
                assets =
                    listOf(
                        AssetDto(
                            id = "a1",
                            orgId = "o",
                            siteId = "s",
                            name = "Насос №1",
                            status = "active",
                            source = "manual",
                        ),
                    ),
            )

        assertEquals("Насос №1", points.single().label)
        assertEquals(false, points.single().label.contains("a1"))
        assertEquals(18.5f, points.single().value)
    }

    @Test
    fun kpiTrendSeriesUsesMttrOnlyAndSkipsMissingSamples() {
        val points =
            kpiTrendChartPoints(
                listOf(
                    KpiTrendPoint("2026-07-01", 4.5, 2, 80.0, 2, 92.1),
                    KpiTrendPoint("2026-07-02", 0.0, 0, 0.0, 0, 91.0),
                ),
            )

        assertEquals(listOf("01.07"), points.map { it.label })
        assertEquals(listOf(4.5f), points.map { it.value })
    }

    @Test
    fun workloadAndFailureMappersUseHumanLabelsNeverIds() {
        val workload =
            engineerWorkloadChartPoints(
                rows = listOf(EngineerWorkloadRow("engineer-uuid", 12, 34.5)),
                labelForUser = { "Инженер" },
            )
        val failure =
            failureFrequencyChartPoints(
                rows = listOf(FailureFrequencyRow("asset-uuid", 7)),
                assets = emptyList(),
            )

        assertEquals("Инженер", workload.single().label)
        assertEquals(12f, workload.single().value)
        assertEquals("Оборудование", failure.single().label)
        assertEquals(7f, failure.single().value)
    }

    private fun sampleKpis(
        mttrHours: Double = 0.0,
        mtbfHours: Double = 0.0,
        availabilityPercent: Double = 0.0,
        plannedCount: Int = 0,
        emergencyCount: Int = 0,
        pprOnTime: Int = 0,
        pprLate: Int = 0,
        pprOpenOverdue: Int = 0,
        pprOpenPending: Int = 0,
        backlogUnder7d: Int = 0,
        backlog7to30d: Int = 0,
        backlogOver30d: Int = 0,
        ranking: List<ManagerKpiDowntimeRow> = emptyList(),
    ) = ManagerKpis(
        from = "2026-07-01",
        to = "2026-07-31",
        mttrHours = mttrHours,
        mttrSampleSize = 1,
        mtbfHours = mtbfHours,
        mtbfSampleSize = 1,
        plannedCount = plannedCount,
        emergencyCount = emergencyCount,
        plannedHours = 0.0,
        emergencyHours = 0.0,
        pprOnTime = pprOnTime,
        pprLate = pprLate,
        pprOpenOverdue = pprOpenOverdue,
        pprOpenPending = pprOpenPending,
        backlogUnder7d = backlogUnder7d,
        backlog7to30d = backlog7to30d,
        backlogOver30d = backlogOver30d,
        backlogOverdue = 0,
        downtimeRanking = ranking,
        availabilityPercent = availabilityPercent,
    )
}
