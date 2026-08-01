package pro.masterdoc.client.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class NavItemSplitTest {
    @Test
    fun splitPinnedTrailing_keepsLastItemPinnedByDefault() {
        val items = listOf("tickets", "board", "admin", "profile")
        val (scrollable, pinned) = splitPinnedTrailing(items)

        assertEquals(listOf("tickets", "board", "admin"), scrollable)
        assertEquals(listOf("profile"), pinned)
    }

    @Test
    fun splitPinnedTrailing_pinsRequestedTrailingCount() {
        val items = (1..11).map { "item-$it" }
        val (scrollable, pinned) = splitPinnedTrailing(items, pinnedTrailingCount = 1)

        assertEquals(10, scrollable.size)
        assertEquals(listOf("item-11"), pinned)
    }

    @Test
    fun splitPinnedTrailing_whenCountExceedsSize_pinsAll() {
        val items = listOf("a", "b")
        val (scrollable, pinned) = splitPinnedTrailing(items, pinnedTrailingCount = 5)

        assertEquals(emptyList(), scrollable)
        assertEquals(items, pinned)
    }

    @Test
    fun splitPinnedTrailing_zeroPinned_keepsAllScrollable() {
        val items = listOf("a", "b", "c")
        val (scrollable, pinned) = splitPinnedTrailing(items, pinnedTrailingCount = 0)

        assertEquals(items, scrollable)
        assertEquals(emptyList(), pinned)
    }
}
