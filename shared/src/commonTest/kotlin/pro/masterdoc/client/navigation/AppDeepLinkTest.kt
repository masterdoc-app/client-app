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
}
