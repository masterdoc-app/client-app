package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssetQrScanResolveTest {
    @Test
    fun resolvesFullStickerUrl() {
        assertEquals(
            "opaque_token-42",
            resolveScannedAssetQrToken("https://app.fixaverse.ru/#/qr/opaque_token-42"),
        )
    }

    @Test
    fun resolvesBareToken() {
        assertEquals("opaque_token-42", resolveScannedAssetQrToken("  opaque_token-42  "))
    }

    @Test
    fun rejectsForeignUrl() {
        assertNull(resolveScannedAssetQrToken("https://example.com/#/qr/opaque-token"))
    }
}
