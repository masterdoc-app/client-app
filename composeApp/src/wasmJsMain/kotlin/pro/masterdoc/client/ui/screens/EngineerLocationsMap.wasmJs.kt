@file:OptIn(ExperimentalWasmJsInterop::class)

package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned

private data class MapBounds(
    val left: Float,
    val top: Float,
    val width: Int,
    val height: Int,
)

@Composable
internal actual fun EngineerLocationsMap(
    markers: List<EngineerMapMarker>,
    modifier: Modifier,
) {
    val id = remember { "engineer-locations-map-${nextMapId++}" }
    var bounds by remember { mutableStateOf<MapBounds?>(null) }
    val markersJson = remember(markers) { markers.toLeafletJson() }

    DisposableEffect(id) {
        onDispose { destroyLeafletMap(id) }
    }
    LaunchedEffect(id, bounds, markersJson) {
        val currentBounds = bounds ?: return@LaunchedEffect
        showLeafletMap(
            id = id,
            left = currentBounds.left,
            top = currentBounds.top,
            width = currentBounds.width,
            height = currentBounds.height,
            markersJson = markersJson,
        )
    }

    Box(
        modifier =
            modifier
                .background(Color.Transparent)
                .onGloballyPositioned { coordinates ->
                    val position: Offset = coordinates.localToRoot(Offset.Zero)
                    bounds =
                        MapBounds(
                            left = position.x,
                            top = position.y,
                            width = coordinates.size.width,
                            height = coordinates.size.height,
                        )
                },
    )
}

private var nextMapId = 0

private fun List<EngineerMapMarker>.toLeafletJson(): String =
    joinToString(prefix = "[", postfix = "]") { marker ->
        """{"label":${marker.label.toJsonString()},"lat":${marker.lat},"lon":${marker.lon}}"""
    }

private fun String.toJsonString(): String =
    buildString {
        append('"')
        this@toJsonString.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else ->
                    if (char < ' ') {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
            }
        }
        append('"')
    }

@JsFun(
    """
    (id, left, top, width, height, markersJson) => {
      let host = document.getElementById(id);
      if (!host) {
        host = document.createElement('div');
        host.id = id;
        host.style.position = 'fixed';
        host.style.zIndex = '10';
        host.style.overflow = 'hidden';
        host.style.borderRadius = '12px';
        document.body.appendChild(host);
      }
      host.style.left = left + 'px';
      host.style.top = top + 'px';
      host.style.width = width + 'px';
      host.style.height = height + 'px';

      const update = () => {
        if (!window.L) {
          if (!document.getElementById('leaflet-css')) {
            const link = document.createElement('link');
            link.id = 'leaflet-css';
            link.rel = 'stylesheet';
            link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
            document.head.appendChild(link);
            const script = document.createElement('script');
            script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
            document.head.appendChild(script);
          }
          setTimeout(update, 50);
          return;
        }
        const map = host.__leafletMap || (host.__leafletMap = L.map(host).setView([55.751244, 37.618423], 5));
        if (!host.__markerLayer) host.__markerLayer = L.layerGroup().addTo(map);
        host.__markerLayer.clearLayers();
        const markers = JSON.parse(markersJson);
        const points = markers.map(marker => {
          const point = [marker.lat, marker.lon];
          const popup = document.createElement('span');
          popup.textContent = marker.label;
          L.marker(point).bindPopup(popup).addTo(host.__markerLayer);
          return point;
        });
        if (points.length === 1) {
          map.setView(points[0], 14);
        } else if (points.length > 1) {
          map.fitBounds(points, { padding: [32, 32] });
        }
        map.invalidateSize();
      };
      update();
    }
    """,
)
private external fun showLeafletMap(
    id: String,
    left: Float,
    top: Float,
    width: Int,
    height: Int,
    markersJson: String,
)

@JsFun(
    """
    id => {
      const host = document.getElementById(id);
      if (!host) return;
      if (host.__leafletMap) host.__leafletMap.remove();
      host.remove();
    }
    """,
)
private external fun destroyLeafletMap(id: String)
