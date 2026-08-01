package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.theme.ClientSpacing

data class AppNavItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun AppNavBar(
    items: List<AppNavItem>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            val scroll = rememberScrollState()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .horizontalScroll(scroll)
                        .padding(horizontal = ClientSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    AppNavButton(
                        label = item.label,
                        icon = item.icon,
                        selected = item.selected,
                        onClick = item.onClick,
                        layout = AppNavButtonLayout.Bottom,
                    )
                }
            }
        }
    }
}

/**
 * Side rail. When many destinations are granted (e.g. smoke admin), the primary list scrolls
 * and the trailing item (Profile) stays pinned so it is never clipped off-screen.
 */
@Composable
fun AppNavRail(
    items: List<AppNavItem>,
    modifier: Modifier = Modifier,
    pinnedTrailingCount: Int = 1,
) {
    val (scrollable, pinned) = splitPinnedTrailing(items, pinnedTrailingCount)
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row {
            Column(
                modifier =
                    Modifier
                        .width(88.dp)
                        .fillMaxHeight()
                        .padding(vertical = ClientSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                ) {
                    scrollable.forEach { item ->
                        AppNavButton(
                            label = item.label,
                            icon = item.icon,
                            selected = item.selected,
                            onClick = item.onClick,
                            layout = AppNavButtonLayout.Rail,
                        )
                    }
                }
                if (pinned.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                    ) {
                        pinned.forEach { item ->
                            AppNavButton(
                                label = item.label,
                                icon = item.icon,
                                selected = item.selected,
                                onClick = item.onClick,
                                layout = AppNavButtonLayout.Rail,
                            )
                        }
                    }
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}
