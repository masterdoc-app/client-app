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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import pro.masterdoc.client.auth.EngineerLocationDto
import pro.masterdoc.client.auth.EngineerLocationsGateway
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientSpacing

private const val MAP_POLL_INTERVAL_MS = 20_000L

@Serializable
internal data class EngineerMapMarker(
    val label: String,
    val lat: Double,
    val lon: Double,
)

internal fun engineerMapMarkers(locations: List<EngineerLocationDto>): List<EngineerMapMarker> =
    locations.map { location ->
        EngineerMapMarker(
            label = location.displayName?.takeIf { it.isNotBlank() } ?: location.userId.take(8),
            lat = location.lat,
            lon = location.lon,
        )
    }

@Composable
fun MapScreen(
    repository: EngineerLocationsGateway,
    modifier: Modifier = Modifier,
) {
    var markers by remember { mutableStateOf<List<EngineerMapMarker>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(repository, reloadKey) {
        while (true) {
            try {
                markers = engineerMapMarkers(repository.list())
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки карты"
            } finally {
                loading = false
            }
            delay(MAP_POLL_INTERVAL_MS)
        }
    }

    AppScaffold(title = "Карта", modifier = modifier) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp),
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                markers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppText(text = error ?: "Нет инженеров на линии")
                        if (error != null) {
                            AppButton(text = "Повторить", onClick = { reloadKey++ })
                        }
                    }
                }
                else -> {
                    EngineerLocationsMap(markers = markers, modifier = Modifier.fillMaxSize())
                    if (error != null) {
                        AppText(text = error)
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
