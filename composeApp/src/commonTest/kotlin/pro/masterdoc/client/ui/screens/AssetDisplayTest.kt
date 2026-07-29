package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.AssetDto

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

    @Test
    fun resolvesAssetForWorkOrderLink() {
        val asset =
            AssetDto(
                id = "asset-1",
                orgId = "org-1",
                siteId = "site-1",
                name = "Насос",
                inventoryNo = "INV-1",
                status = "active",
                source = "manual",
            )

        assertEquals(asset, findAssetById(listOf(asset), "asset-1"))
        assertEquals(null, findAssetById(listOf(asset), "missing"))
    }
}
