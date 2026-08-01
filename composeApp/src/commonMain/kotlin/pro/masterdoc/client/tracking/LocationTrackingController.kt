package pro.masterdoc.client.tracking

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.EngineerLocationsGateway
import pro.masterdoc.client.auth.UpdateEngineerLocationRequest
import pro.masterdoc.client.auth.WorkOrderDto

const val LOCATION_PING_INTERVAL_MS = 5 * 60 * 1000L

data class EngineerLocationPoint(
    val lat: Double,
    val lon: Double,
    val accuracyM: Double? = null,
)

interface EngineerLocationPingSource {
    /**
     * Returns null when location permission is not granted or no fix is available.
     * Android implementations request permission before resolving a location.
     */
    suspend fun currentLocation(): EngineerLocationPoint?
}

expect fun createEngineerLocationPingSource(): EngineerLocationPingSource

class LocationTrackingController(
    private val repository: EngineerLocationsGateway,
    private val locationSource: EngineerLocationPingSource,
    private val displayName: () -> String? = { null },
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var openAssigned: List<WorkOrderDto> = emptyList()
    private var trackingSessionActive = false
    private var pingJob: Job? = null

    fun onStartedInProgress() {
        trackingSessionActive = true
        syncTracking()
    }

    suspend fun currentLocation(): EngineerLocationPoint? = locationSource.currentLocation()

    fun onWorkOrdersChanged(openAssigned: List<WorkOrderDto>) {
        this.openAssigned = openAssigned.filter { it.status == "new" || it.status == "in_progress" }
        syncTracking()
    }

    fun close() {
        pingJob?.cancel()
        scope.cancel()
    }

    private fun syncTracking() {
        if (openAssigned.isEmpty()) {
            trackingSessionActive = false
            pingJob?.cancel()
            pingJob = null
            scope.launch { runCatching { repository.deleteMe() } }
            return
        }

        val hasInProgress = openAssigned.any { it.status == "in_progress" }
        if ((hasInProgress || trackingSessionActive) && pingJob == null) {
            pingJob =
                scope.launch {
                    ping()
                    while (isActive) {
                        delay(LOCATION_PING_INTERVAL_MS)
                        ping()
                    }
                }
        }
    }

    private suspend fun ping() {
        val point = locationSource.currentLocation() ?: return
        runCatching {
            repository.putMe(
                UpdateEngineerLocationRequest(
                    lat = point.lat,
                    lon = point.lon,
                    accuracyM = point.accuracyM,
                    displayName = displayName()?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
    }
}
