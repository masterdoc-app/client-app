package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
fun AppListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val background =
        when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        }
    val titleColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
    val subtitleColor =
        when {
            selected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(background)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = ClientSpacing.md, vertical = ClientSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppText(
                text = title,
                style = AppTextStyle.Body,
                color = titleColor,
                maxLines = 1,
            )
            if (subtitle != null) {
                AppText(
                    text = subtitle,
                    style = AppTextStyle.Label,
                    color = subtitleColor,
                    maxLines = 1,
                )
            }
        }
    }
}
