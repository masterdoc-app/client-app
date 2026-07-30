package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

internal fun engineerMapUrl(marker: EngineerMapMarker): String =
    "https://www.openstreetmap.org/?mlat=${marker.lat}&mlon=${marker.lon}"

@Composable
internal fun EngineerLocationsMapFallback(
    markers: List<EngineerMapMarker>,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        markers.forEach { marker ->
            AppText(
                text = "${marker.label}: Открыть в OpenStreetMap",
                style = AppTextStyle.Body,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { uriHandler.openUri(engineerMapUrl(marker)) },
            )
        }
    }
}
