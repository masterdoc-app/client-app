package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Copilot-style camera hero (LiteCameraHeroButton rings + flare fill). */
@Composable
fun TicketsCameraHeroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val buttonSize = 112.dp
    val ringSize = 148.dp
    val flareBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val flareOuter = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val flareTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(ringSize)
                    .border(1.5.dp, flareOuter, CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .size(ringSize * 0.82f)
                    .border(1.5.dp, flareBorder, CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .size(buttonSize)
                    .clickable(enabled = enabled && !isLoading, onClick = onClick)
                    .background(
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        shape = CircleShape,
                    ).border(3.dp, flareTint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Сканировать камерой",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}
