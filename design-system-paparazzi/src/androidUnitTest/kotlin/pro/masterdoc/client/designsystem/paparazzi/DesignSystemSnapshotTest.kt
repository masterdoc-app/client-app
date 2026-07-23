package pro.masterdoc.client.designsystem.paparazzi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppListItem
import pro.masterdoc.client.designsystem.components.AppLoader
import pro.masterdoc.client.designsystem.components.AppLoaderSize
import pro.masterdoc.client.designsystem.components.AppMenu
import pro.masterdoc.client.designsystem.components.AppMenuItem
import pro.masterdoc.client.designsystem.components.AppNavButton
import pro.masterdoc.client.designsystem.components.AppNavButtonLayout
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientColors
import pro.masterdoc.client.designsystem.theme.ClientTheme
import pro.masterdoc.client.designsystem.theme.clientLightColorScheme

class DesignSystemSnapshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5.copy(softButtons = false),
            theme = "android:Theme.Material.Light.NoActionBar",
            // Crop to composable — avoid Android window letterbox looking cream.
            renderingMode = SessionParams.RenderingMode.SHRINK,
        )

    @Test
    fun colors_swatches() {
        snapshotFrame {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("Background", ClientColors.Background)
                Swatch("Surface", ClientColors.Surface)
                Swatch("SurfaceContainer", ClientColors.SurfaceContainer)
                Swatch("SurfaceContainerHighest", ClientColors.SurfaceContainerHighest)
                Swatch("OnSurface", ClientColors.OnSurface)
                Swatch("OnSurfaceVariant", ClientColors.OnSurfaceVariant)
                Swatch("Primary", ClientColors.Primary)
                Swatch("OnPrimary", ClientColors.OnPrimary)
                Swatch("Outline", ClientColors.Outline)
                Swatch("Error", ClientColors.Error)
            }
        }
    }

    @Test
    fun lightScheme_rejectsMaterialWarmNeutrals() {
        val scheme = clientLightColorScheme()
        // Material3 ColorLightTokens.Neutral98 / Neutral94 — warm pink, not our graphite.
        val materialWarmBackground = Color(0xFFFEF7FF)
        val materialWarmContainer = Color(0xFFF3EDF7)
        assertEquals(ClientColors.Background, scheme.background)
        assertEquals(ClientColors.SurfaceContainer, scheme.surfaceContainer)
        assertEquals(ClientColors.SurfaceContainerHighest, scheme.surfaceContainerHighest)
        assert(scheme.background != materialWarmBackground)
        assert(scheme.surfaceContainer != materialWarmContainer)
        assert(scheme.surfaceContainerHighest != Color(0xFFE6E0E9))
        // Cool bias: blue channel must dominate red (not cream/yellow).
        assertTrue(ClientColors.Background.blue > ClientColors.Background.red)
        assertTrue(scheme.surfaceContainer.blue > scheme.surfaceContainer.red)
        assertTrue(scheme.outlineVariant.blue >= scheme.outlineVariant.red)
        assertEquals(ClientColors.OutlineVariant, scheme.outlineVariant)
        assertEquals(ClientColors.SurfaceContainerHigh, scheme.secondaryContainer)
    }

    @Test
    fun appScaffold_background() {
        paparazzi.snapshot {
            ClientTheme(darkTheme = false) {
                Box(
                    modifier =
                        Modifier
                            .width(360.dp)
                            .height(640.dp)
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    AppScaffold(title = "Пользователи") { padding ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppText(text = "Email", style = AppTextStyle.Label)
                            AppText(text = "Имя", style = AppTextStyle.Label)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun typography_scale() {
        snapshotFrame {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppText("Display 32/40", style = AppTextStyle.Display)
                AppText("Title 20/28", style = AppTextStyle.Title)
                AppText("Body 16/24", style = AppTextStyle.Body)
                AppText("Label 14/20", style = AppTextStyle.Label)
            }
        }
    }

    @Test
    fun appText_body() {
        snapshotFrame {
            AppText(
                text = "Текст дизайн-системы Formaverse",
                style = AppTextStyle.Body,
            )
        }
    }

    @Test
    fun appButton_primary() {
        snapshotFrame {
            AppButton(text = "Продолжить", onClick = {})
        }
    }

    @Test
    fun appButton_secondary() {
        snapshotFrame {
            AppButton(
                text = "Отмена",
                onClick = {},
                variant = AppButtonVariant.Secondary,
            )
        }
    }

    @Test
    fun appButton_disabled() {
        snapshotFrame {
            AppButton(text = "Недоступно", onClick = {}, enabled = false)
        }
    }

    @Test
    fun appMenu_default() {
        snapshotFrame {
            AppMenu(
                title = "Меню",
                items =
                    listOf(
                        AppMenuItem(id = "docs", label = "Документы"),
                        AppMenuItem(id = "tasks", label = "Задачи"),
                        AppMenuItem(id = "settings", label = "Настройки"),
                    ),
                onItemClick = {},
            )
        }
    }

    @Test
    fun appStatusChip_tones() {
        snapshotFrame {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppStatusChip(text = "Черновик", tone = AppStatusChipTone.Accent)
                AppStatusChip(text = "В базе", tone = AppStatusChipTone.Neutral)
                AppStatusChip(text = "из AI", tone = AppStatusChipTone.Muted, showDot = false)
            }
        }
    }

    @Test
    fun appListItem_default() {
        snapshotFrame {
            AppListItem(
                title = "Объект А-12",
                subtitle = "Последняя проверка · вчера",
                onClick = {},
            )
        }
    }

    @Test
    fun appListItem_selected() {
        snapshotFrame {
            AppListItem(
                title = "Объект А-12",
                subtitle = "Последняя проверка · вчера",
                selected = true,
                onClick = {},
            )
        }
    }

    @Test
    fun navButton_bottom_selected() {
        snapshotFrame {
            AppNavButton(
                label = "Доска",
                icon = Icons.Filled.Dashboard,
                selected = true,
                onClick = {},
                layout = AppNavButtonLayout.Bottom,
            )
        }
    }

    @Test
    fun navButton_bottom_unselected() {
        snapshotFrame {
            AppNavButton(
                label = "Карта",
                icon = Icons.Filled.Map,
                selected = false,
                onClick = {},
                layout = AppNavButtonLayout.Bottom,
            )
        }
    }

    @Test
    fun navButton_rail_selected() {
        snapshotFrame {
            AppNavButton(
                label = "Доска",
                icon = Icons.Filled.Dashboard,
                selected = true,
                onClick = {},
                layout = AppNavButtonLayout.Rail,
            )
        }
    }

    @Test
    fun loader_md() {
        snapshotFrame {
            AppLoader(
                size = AppLoaderSize.Md,
                label = "Загрузка…",
                progress = 0.7f,
            )
        }
    }

    @Test
    fun loader_sm_lg() {
        snapshotFrame {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                AppLoader(size = AppLoaderSize.Sm, progress = 0.7f)
                AppLoader(size = AppLoaderSize.Lg, progress = 0.7f)
            }
        }
    }

    private fun snapshotFrame(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            ClientTheme(darkTheme = false) {
                Box(
                    modifier =
                        Modifier
                            .width(360.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun Swatch(
    name: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(48.dp)
                    .height(32.dp)
                    .background(color),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
