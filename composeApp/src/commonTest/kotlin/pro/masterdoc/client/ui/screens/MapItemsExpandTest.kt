package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapItemsExpandTest {
    @Test
    fun collapsed_showsPreviewLimitOnly() {
        val items = (1..6).map { "item-$it" }
        assertEquals(
            listOf("item-1", "item-2", "item-3", "item-4", "item-5"),
            visibleMapItems(items, expanded = false, previewLimit = 5),
        )
    }

    @Test
    fun expanded_showsAllItems() {
        val items = (1..6).map { "item-$it" }
        assertEquals(items, visibleMapItems(items, expanded = true, previewLimit = 5))
    }

    @Test
    fun overflowLabel_nullWhenFitsInPreview() {
        assertNull(mapItemsOverflowLabel(total = 5, previewLimit = 5, expanded = false))
        assertNull(mapItemsOverflowLabel(total = 3, previewLimit = 5, expanded = true))
    }

    @Test
    fun overflowLabel_showsRemainingWhenCollapsed() {
        assertEquals("… ещё 1", mapItemsOverflowLabel(total = 6, previewLimit = 5, expanded = false))
    }

    @Test
    fun overflowLabel_collapseWhenExpanded() {
        assertEquals("Свернуть", mapItemsOverflowLabel(total = 6, previewLimit = 5, expanded = true))
    }
}
