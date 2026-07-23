package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.theme.ClientSpacing

enum class AppStatusChipTone {
    /** Accent — draft / needs attention (primary container). */
    Accent,

    /** Neutral — settled / in registry. */
    Neutral,

    /** Muted — secondary metadata (source, etc.). */
    Muted,
}

/**
 * Compact status / meta chip used on equipment and list cards.
 * Graphite+Cobalt: tones map to ColorScheme roles only.
 */
@Composable
fun AppStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: AppStatusChipTone = AppStatusChipTone.Neutral,
    showDot: Boolean = true,
) {
    val (container, content, dot) =
        when (tone) {
            AppStatusChipTone.Accent ->
                Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.primary,
                )
            AppStatusChipTone.Neutral ->
                Triple(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.primary,
                )
            AppStatusChipTone.Muted ->
                Triple(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }

    Row(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(container)
                .padding(horizontal = ClientSpacing.sm, vertical = ClientSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showDot) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dot),
            )
        }
        AppText(text = text, style = AppTextStyle.Label, color = content)
    }
}
