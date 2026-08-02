package pro.masterdoc.client.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppDeepLinkTest {
    @Test
    fun parsesPprDeepLink() {
        val link = parseAppDeepLink("#/ppr/map-123")
        assertIs<AppDeepLink.Ppr>(link)
        assertEquals("map-123", link.mapId)
        assertEquals(NavDestinationId.Charts, link.toDestination())
        assertEquals("#/ppr/map-123", link.toHash())
    }

    @Test
    fun parsesEquipmentAndCharts() {
        assertEquals(AppDeepLink.Equipment, parseAppDeepLink("#/equipment"))
        assertEquals(AppDeepLink.Charts, parseAppDeepLink("#/ppr"))
        assertNull(parseAppDeepLink(""))
        assertNull(parseAppDeepLink("#/unknown"))
    }

    @Test
    fun parsesEquipmentDetailDeepLink() {
        val link = parseAppDeepLink("#/equipment/asset-42")
        assertIs<AppDeepLink.EquipmentDetail>(link)
        assertEquals("asset-42", link.assetId)
        assertEquals(NavDestinationId.Equipment, link.toDestination())
        assertEquals("#/equipment/asset-42", link.toHash())
    }

    @Test
    fun equipmentListStillParsesWithoutId() {
        assertEquals(AppDeepLink.Equipment, parseAppDeepLink("#/equipment"))
    }

    @Test
    fun parsesAssetQrDeepLink() {
        val link = parseAppDeepLink("#/qr/opaque-token")
        assertIs<AppDeepLink.AssetQr>(link)
        assertEquals("opaque-token", link.token)
        assertEquals("#/qr/opaque-token", link.toHash())
    }

    @Test
    fun rejectsAssetQrDeepLinkWithoutToken() {
        assertNull(parseAppDeepLink("#/qr"))
        assertNull(parseAppDeepLink("#/qr/"))
    }
}
