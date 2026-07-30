package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
internal actual fun EngineerLocationsMap(
    markers: List<EngineerMapMarker>,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        markers.forEach { marker ->
            AppText(
                text = "${marker.label}: https://www.openstreetmap.org/?mlat=${marker.lat}&mlon=${marker.lon}",
                style = AppTextStyle.Body,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
