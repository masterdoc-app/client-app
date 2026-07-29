package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class AssetDisplayTest {
    @Test
    fun prefersNonBlankName() {
        assertEquals("Насос", assetDisplayName("Насос", "uuid-long"))
    }

    @Test
    fun fallsBackToShortId() {
        assertEquals("abcdef12…", assetDisplayName("  ", "abcdef12-3456-7890"))
    }

    @Test
    fun inventoryTooltip() {
        assertEquals("Инв. № INV-1", assetInventoryTooltip("INV-1"))
        assertEquals("Инв. № не указан", assetInventoryTooltip(null))
        assertEquals("Инв. № не указан", assetInventoryTooltip("  "))
    }
}
