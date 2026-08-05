package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.EngineerWorkloadRow
import pro.masterdoc.client.auth.FailureFrequencyRow
import pro.masterdoc.client.auth.KpiTrendPoint
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

fun kpiTrendChartPoints(points: List<KpiTrendPoint>): List<ReportChartPoint> =
    points.mapNotNull { point ->
        point.mttrHours.takeIf { point.mttrSampleSize > 0 }?.let { mttrHours ->
            ReportChartPoint(
                label = point.bucketStart.takeLast(5).split("-").reversed().joinToString("."),
                value = mttrHours.toFloat(),
            )
        }
    }

fun engineerWorkloadChartPoints(
    rows: List<EngineerWorkloadRow>,
    labelForUser: (String) -> String,
): List<ReportChartPoint> =
    rows.map { row ->
        ReportChartPoint(
            label = labelForUser(row.userId),
            value = row.closedCount.toFloat(),
        )
    }

fun failureFrequencyChartPoints(
    rows: List<FailureFrequencyRow>,
    assets: List<AssetDto>,
): List<ReportChartPoint> {
    val namesById = assets.associate { it.id to it.displayName() }
    return rows.map { row ->
        ReportChartPoint(
            label = namesById[row.assetId] ?: "Оборудование",
            value = row.emergencyCount.toFloat(),
        )
    }
}
