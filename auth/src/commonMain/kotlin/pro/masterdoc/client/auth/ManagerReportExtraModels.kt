package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable

@Serializable
data class KpiTrendsReport(
    val bucket: String,
    val points: List<KpiTrendPoint>,
)

@Serializable
data class KpiTrendPoint(
    val bucketStart: String,
    val mttrHours: Double,
    val mttrSampleSize: Int,
    val mtbfHours: Double,
    val mtbfSampleSize: Int,
    val availabilityPercent: Double,
)

@Serializable
data class ReactiveCompletionReport(
    val createdCount: Int,
    val closedCount: Int,
    val completionRatePercent: Double,
    val emergencyCount: Int,
    val plannedCount: Int,
    val reactivePercent: Double,
)

@Serializable
data class EngineerWorkloadReport(
    val engineers: List<EngineerWorkloadRow>,
)

@Serializable
data class EngineerWorkloadRow(
    val userId: String,
    val closedCount: Int,
    val hours: Double,
)

@Serializable
data class FailureFrequencyReport(
    val assets: List<FailureFrequencyRow>,
)

@Serializable
data class FailureFrequencyRow(
    val assetId: String,
    val emergencyCount: Int,
)
