package pro.masterdoc.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun EngineerLocationsMap(
    markers: List<EngineerMapMarker>,
    modifier: Modifier,
) = EngineerLocationsMapFallback(markers = markers, modifier = modifier)
