package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.ManagerKpis

fun kpiSummaryChartPoints(kpis: ManagerKpis): List<ReportChartPoint> =
    listOf(
        ReportChartPoint("MTTR", kpis.mttrHours.toFloat()),
        ReportChartPoint("MTBF", kpis.mtbfHours.toFloat()),
        ReportChartPoint("Готовность", kpis.availabilityPercent.toFloat()),
    )

fun plannedVsEmergencyChartPoints(kpis: ManagerKpis): List<ReportChartPoint> =
    listOf(
        ReportChartPoint("Плановые", kpis.plannedCount.toFloat()),
        ReportChartPoint("Аварийные", kpis.emergencyCount.toFloat()),
    )

fun pprComplianceChartPoints(kpis: ManagerKpis): List<ReportChartPoint> =
    listOf(
        ReportChartPoint("Вовремя", kpis.pprOnTime.toFloat()),
        ReportChartPoint("С опозданием", kpis.pprLate.toFloat()),
        ReportChartPoint("Просрочено", kpis.pprOpenOverdue.toFloat()),
        ReportChartPoint("В срок", kpis.pprOpenPending.toFloat()),
    )

fun backlogChartPoints(kpis: ManagerKpis): List<ReportChartPoint> =
    listOf(
        ReportChartPoint("<7 дн", kpis.backlogUnder7d.toFloat()),
        ReportChartPoint("7–30 дн", kpis.backlog7to30d.toFloat()),
        ReportChartPoint(">30 дн", kpis.backlogOver30d.toFloat()),
    )

fun downtimeRankingChartPoints(
    kpis: ManagerKpis,
    assets: List<AssetDto>,
): List<ReportChartPoint> {
    val namesById = assets.associate { it.id to it.displayName() }
    return kpis.downtimeRanking.map { row ->
        ReportChartPoint(
            label = namesById[row.assetId] ?: "Оборудование",
            value = row.downtimeHours.toFloat(),
        )
    }
}
