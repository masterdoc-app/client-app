package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

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
    NavigationBar(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun AppNavRail(
    items: List<AppNavItem>,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier.fillMaxHeight()) {
        Column {
            items.forEach { item ->
                NavigationRailItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                )
            }
        }
    }
}
