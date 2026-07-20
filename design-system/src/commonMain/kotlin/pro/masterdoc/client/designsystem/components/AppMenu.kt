package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.theme.ClientSpacing

data class AppMenuItem(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
)

@Composable
fun AppMenu(
    title: String,
    items: List<AppMenuItem>,
    onItemClick: (AppMenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                .padding(vertical = ClientSpacing.sm),
    ) {
        AppText(
            text = title,
            style = AppTextStyle.Title,
            modifier = Modifier.padding(horizontal = ClientSpacing.md, vertical = ClientSpacing.sm),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        items.forEach { item ->
            AppListItem(
                title = item.label,
                onClick = { onItemClick(item) },
                enabled = item.enabled,
            )
        }
    }
}
