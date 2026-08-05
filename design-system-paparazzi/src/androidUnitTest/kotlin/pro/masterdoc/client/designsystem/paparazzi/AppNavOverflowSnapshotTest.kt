package pro.masterdoc.client.designsystem.paparazzi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test
import pro.masterdoc.client.designsystem.components.AppNavBar
import pro.masterdoc.client.designsystem.components.AppNavItem
import pro.masterdoc.client.designsystem.components.AppNavRail
import pro.masterdoc.client.designsystem.theme.ClientTheme

class AppNavOverflowSnapshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5.copy(softButtons = false),
            theme = "android:Theme.Material.Light.NoActionBar",
            renderingMode = SessionParams.RenderingMode.SHRINK,
        )

    @Test
    fun navRail_manyItems_profilePinned() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.height(360.dp)) {
                    AppNavRail(items = manyItems(selectedKey = "profile"))
                }
            }
        }
    }

    @Test
    fun navBar_manyItems_profilePinned() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.width(320.dp)) {
                    AppNavBar(items = manyItems(selectedKey = "profile"))
                }
            }
        }
    }

    @Test
    fun navRail_fewItems() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.height(640.dp)) {
                    AppNavRail(items = fewItems(selectedKey = "tickets"))
                }
            }
        }
    }

    @Test
    fun navBar_fewItems() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.width(400.dp)) {
                    AppNavBar(items = fewItems(selectedKey = "tickets"))
                }
            }
        }
    }
}

private fun manyItems(selectedKey: String): List<AppNavItem> {
    val defs =
        listOf(
            "tickets" to ("Заявки" to Icons.Filled.Assignment),
            "board" to ("Доска" to Icons.Filled.Dashboard),
            "charts" to ("Отчёты" to Icons.Filled.BarChart),
            "equipment" to ("Оборудование" to Icons.Filled.PrecisionManufacturing),
            "maps" to ("Карты" to Icons.Filled.Map),
            "ai" to ("ИИ" to Icons.Filled.History),
            "admin" to ("Админ" to Icons.Filled.AdminPanelSettings),
            "dashboard" to ("Дашборд" to Icons.Filled.Assessment),
            "profile" to ("Профиль" to Icons.Filled.Person),
        )
    return defs.map { (key, labelIcon) ->
        val (label, icon) = labelIcon
        navItem(key, label, icon, selected = key == selectedKey)
    }
}

private fun fewItems(selectedKey: String): List<AppNavItem> =
    listOf(
        navItem("tickets", "Заявки", Icons.Filled.Assignment, selected = selectedKey == "tickets"),
        navItem("board", "Доска", Icons.Filled.Dashboard, selected = selectedKey == "board"),
        navItem("profile", "Профиль", Icons.Filled.Person, selected = selectedKey == "profile"),
    )

private fun navItem(
    key: String,
    label: String,
    icon: ImageVector,
    selected: Boolean,
): AppNavItem =
    AppNavItem(
        key = key,
        label = label,
        icon = icon,
        selected = selected,
        onClick = {},
    )
