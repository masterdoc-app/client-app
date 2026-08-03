@file:OptIn(kotlin.time.ExperimentalTime::class)

package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant
import pro.masterdoc.client.auth.EngineerLocationDto
import pro.masterdoc.client.auth.EngineerLocationsGateway
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.SiteDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientSpacing

private const val MAP_POLL_INTERVAL_MS = 20_000L
private const val ENGINEER_LOCATION_FRESHNESS_MS = 7 * 60 * 1_000L

internal fun mapPollingDelayMillis(): Long = MAP_POLL_INTERVAL_MS

internal fun isMapRetryEnabled(): Boolean = false

@Serializable
internal data class EngineerMapMarker(
    val label: String,
    val lat: Double,
    val lon: Double,
)

internal fun engineerMapMarkers(
    locations: List<EngineerLocationDto>,
    nowEpochMillis: Long,
): List<EngineerMapMarker> =
    locations.mapNotNull { location ->
        val recordedAtMillis = location.recordedAtMillis() ?: return@mapNotNull null
        if (recordedAtMillis < nowEpochMillis - ENGINEER_LOCATION_FRESHNESS_MS) return@mapNotNull null
        EngineerMapMarker(
            label = location.displayName?.takeIf { it.isNotBlank() } ?: "Инженер",
            lat = location.lat,
            lon = location.lon,
        )
    }

/** Markers actually drawn: engineers when online, otherwise first цех with coordinates. */
internal fun mapDisplayMarkers(
    engineerMarkers: List<EngineerMapMarker>,
    sites: List<SiteDto>,
): List<EngineerMapMarker> {
    if (engineerMarkers.isNotEmpty()) return engineerMarkers
    val site = sites.firstOrNull { it.lat != null && it.lon != null } ?: return emptyList()
    val lat = site.lat ?: return emptyList()
    val lon = site.lon ?: return emptyList()
    return listOf(
        EngineerMapMarker(
            label = site.name.takeIf { it.isNotBlank() } ?: "Площадка",
            lat = lat,
            lon = lon,
        ),
    )
}

private fun EngineerLocationDto.recordedAtMillis(): Long? =
    runCatching { Instant.parse(recordedAt).toEpochMilliseconds() }.getOrNull()

private fun nextMarkerRefreshDelayMillis(
    locations: List<EngineerLocationDto>,
    nowEpochMillis: Long,
): Long =
    locations
        .mapNotNull { location ->
            location.recordedAtMillis()?.plus(ENGINEER_LOCATION_FRESHNESS_MS)?.minus(nowEpochMillis)
        }.filter { it > 0L }
        .minOrNull()
        ?.coerceAtMost(MAP_POLL_INTERVAL_MS)
        ?: MAP_POLL_INTERVAL_MS

@Composable
fun MapScreen(
    repository: EngineerLocationsGateway,
    equipmentRepository: EquipmentRepository? = null,
    modifier: Modifier = Modifier,
) {
    var engineerMarkers by remember { mutableStateOf<List<EngineerMapMarker>>(emptyList()) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var latestLocations by remember { mutableStateOf<List<EngineerLocationDto>>(emptyList()) }
    var locationsLoaded by remember { mutableStateOf(false) }
    var sitesLoaded by remember { mutableStateOf(equipmentRepository == null) }
    var error by remember { mutableStateOf<String?>(null) }
    val loading = !locationsLoaded || !sitesLoaded

    LaunchedEffect(equipmentRepository) {
        if (equipmentRepository == null) {
            sites = emptyList()
            sitesLoaded = true
            return@LaunchedEffect
        }
        sites = runCatching { equipmentRepository.listSites().items }.getOrDefault(emptyList())
        sitesLoaded = true
    }

    LaunchedEffect(repository) {
        while (true) {
            try {
                latestLocations = repository.list()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки карты"
            } finally {
                locationsLoaded = true
            }
            delay(mapPollingDelayMillis())
        }
    }

    LaunchedEffect(latestLocations) {
        while (true) {
            val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
            engineerMarkers = engineerMapMarkers(latestLocations, nowEpochMillis)
            delay(nextMarkerRefreshDelayMillis(latestLocations, nowEpochMillis))
        }
    }

    val displayMarkers = remember(engineerMarkers, sites) { mapDisplayMarkers(engineerMarkers, sites) }
    val noEngineersOnline = !loading && engineerMarkers.isEmpty()

    AppScaffold(title = "Карта", modifier = modifier) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp),
        ) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Always open the map: engineers when online, else first цех with coords,
                // else OSM default view (still a map, not a blank empty-state page).
                EngineerLocationsMap(markers = displayMarkers, modifier = Modifier.fillMaxSize())
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = ClientSpacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                ) {
                    if (noEngineersOnline) {
                        AppText(text = "Нет инженеров на линии")
                    }
                    if (error != null) {
                        AppText(text = error!!)
                        if (displayMarkers.isEmpty()) {
                            AppButton(
                                text = "Повторить",
                                enabled = isMapRetryEnabled(),
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal expect fun EngineerLocationsMap(
    markers: List<EngineerMapMarker>,
    modifier: Modifier = Modifier,
)
