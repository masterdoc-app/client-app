package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.WeekClip
import pro.masterdoc.client.auth.WorkOrderDto

class BoardScreenTest {
    @Test
    fun mondayIsoForEpochDayUsesCalendarDateWeek() {
        val monday = IsoDates.parseToEpochDay("2026-07-27")!!
        val sunday = IsoDates.parseToEpochDay("2026-08-02")!!

        assertEquals("2026-07-27", mondayIsoForEpochDay(monday))
        assertEquals("2026-07-27", mondayIsoForEpochDay(sunday))
    }

    @Test
    fun assignLanesReusesLaneAfterNonOverlappingItem() {
        val first = order("first")
        val overlapping = order("overlapping")
        val later = order("later")

        val result =
            assignLanes(
                listOf(
                    first to WeekClip(startColumn = 0, spanColumns = 2),
                    overlapping to WeekClip(startColumn = 1, spanColumns = 2),
                    later to WeekClip(startColumn = 3, spanColumns = 1),
                ),
            )

        assertEquals(
            mapOf("first" to 0, "overlapping" to 1, "later" to 0),
            result.associate { it.order.id to it.lane },
        )
    }

    @Test
    fun assignLanesPlacesLongerItemFirstAtSameStart() {
        val short = order("short")
        val long = order("long")

        val result =
            assignLanes(
                listOf(
                    short to WeekClip(startColumn = 2, spanColumns = 1),
                    long to WeekClip(startColumn = 2, spanColumns = 3),
                ),
            )

        assertEquals(listOf("long", "short"), result.map { it.order.id })
        assertEquals(listOf(0, 1), result.map { it.lane })
    }

    private fun order(id: String) =
        WorkOrderDto(
            id = id,
            orgId = "org",
            type = "ppr",
            status = "open",
            title = id,
            assetId = "asset",
            siteId = "site",
            dueAt = "2026-07-20",
            source = "manual",
            createdAt = "2026-07-20T00:00:00Z",
            updatedAt = "2026-07-20T00:00:00Z",
        )
}
