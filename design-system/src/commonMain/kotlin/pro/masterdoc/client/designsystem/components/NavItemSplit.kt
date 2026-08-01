package pro.masterdoc.client.designsystem.components

/**
 * Split nav items so trailing entries can stay pinned (e.g. Profile)
 * while the rest scroll when the rail overflows.
 */
fun <T> splitPinnedTrailing(
    items: List<T>,
    pinnedTrailingCount: Int = 1,
): Pair<List<T>, List<T>> {
    if (items.isEmpty() || pinnedTrailingCount <= 0) {
        return items to emptyList()
    }
    val pinCount = pinnedTrailingCount.coerceAtMost(items.size)
    val splitAt = items.size - pinCount
    return items.subList(0, splitAt) to items.subList(splitAt, items.size)
}
