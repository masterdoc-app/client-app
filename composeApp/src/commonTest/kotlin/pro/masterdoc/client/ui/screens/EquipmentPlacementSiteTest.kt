package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pro.masterdoc.client.auth.SiteDto

class EquipmentPlacementSiteTest {
    @Test
    fun prefersDefaultCeh1WhenPresent() {
        assertEquals(
            "ceh-1",
            defaultEquipmentPlacementSiteId(
                listOf(
                    SiteDto(id = "smoke-a", orgId = "o", name = "Smoke Cycle Site"),
                    SiteDto(id = "ceh-1", orgId = "o", name = "Цех 1"),
                    SiteDto(id = "smoke-b", orgId = "o", name = "Smoke Cycle Цех"),
                ),
            ),
        )
    }

    @Test
    fun usesFirstSiteWhenNoDefault() {
        assertEquals(
            "smoke-a",
            defaultEquipmentPlacementSiteId(
                listOf(
                    SiteDto(id = "smoke-a", orgId = "o", name = "Smoke Cycle Site"),
                    SiteDto(id = "smoke-b", orgId = "o", name = "Smoke Cycle Цех"),
                ),
            ),
        )
    }

    @Test
    fun nullWhenNoSites() {
        assertNull(defaultEquipmentPlacementSiteId(emptyList()))
    }
}
