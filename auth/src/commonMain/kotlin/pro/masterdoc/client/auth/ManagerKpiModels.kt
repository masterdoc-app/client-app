package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable

@Serializable
data class ManagerKpis(
    val from: String,
    val to: String,
    val mttrHours: Double,
    val mttrSampleSize: Int,
    val mtbfHours: Double,
    val mtbfSampleSize: Int,
    val plannedCount: Int,
    val emergencyCount: Int,
    val plannedHours: Double,
    val emergencyHours: Double,
    val pprOnTime: Int,
    val pprLate: Int,
    val pprOpenOverdue: Int,
    val pprOpenPending: Int,
    val backlogUnder7d: Int,
    val backlog7to30d: Int,
    val backlogOver30d: Int,
    val backlogOverdue: Int,
    val downtimeRanking: List<ManagerKpiDowntimeRow>,
    val availabilityPercent: Double,
)

@Serializable
data class ManagerKpiDowntimeRow(
    val assetId: String,
    val downtimeHours: Double,
    val openIntervals: Int,
)
