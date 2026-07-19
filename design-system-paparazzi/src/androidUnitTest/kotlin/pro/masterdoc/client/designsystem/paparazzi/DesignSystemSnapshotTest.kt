package pro.masterdoc.client.designsystem.paparazzi

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.theme.ClientTheme

class DesignSystemSnapshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
            theme = "android:Theme.Material.Light.NoActionBar",
        )

    @Test
    fun appButton_primary() {
        paparazzi.snapshot {
            ClientTheme(darkTheme = false) {
                AppButton(text = "Продолжить", onClick = {})
            }
        }
    }
}
