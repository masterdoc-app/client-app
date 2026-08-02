package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EquipmentQrUiTest {
    @Test
    fun qrBlockIsAvailableOnlyForActiveAssetWithFeature() {
        assertTrue(shouldShowEquipmentQr(canManageQr = true, assetStatus = "active"))
        assertFalse(shouldShowEquipmentQr(canManageQr = false, assetStatus = "active"))
        assertFalse(shouldShowEquipmentQr(canManageQr = true, assetStatus = "draft"))
    }

    @Test
    fun existingTokenProducesCanonicalQrUrlAndRotateAction() {
        val url = equipmentQrUrl("opaque-token")

        assertEquals("https://app.fixaverse.ru/#/qr/opaque-token", url)
        assertEquals("Перевыпустить", equipmentQrActionLabel(url))
    }

    @Test
    fun missingTokenProducesGenerateActionWithoutUrl() {
        assertNull(equipmentQrUrl(null))
        assertNull(equipmentQrUrl(""))
        assertEquals("Сгенерировать", equipmentQrActionLabel(null))
    }
}
