package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkOrderDurationTest {
    @Test
    fun spanDaysCeilEight() {
        assertEquals(1, WorkOrderDuration.spanDays(1))
        assertEquals(1, WorkOrderDuration.spanDays(8))
        assertEquals(2, WorkOrderDuration.spanDays(9))
        assertEquals(2, WorkOrderDuration.spanDays(16))
        assertEquals(3, WorkOrderDuration.spanDays(17))
    }

    @Test
    fun fridayThreeWorkdaysSkipsWeekend() {
        assertEquals(
            listOf("2026-07-24", "2026-07-27", "2026-07-28"),
            WorkOrderDuration.occupiedDates("2026-07-24", 24),
        )
    }

    @Test
    fun clipCrossWeekShowsMonTueOnNextWeek() {
        val occupied = WorkOrderDuration.occupiedDates("2026-07-24", 24)
        val clip = WorkOrderDuration.clipToWeek(occupied, "2026-07-27")!!
        assertEquals(0, clip.startColumn) // Monday
        assertEquals(2, clip.spanColumns) // Mon–Tue
    }
}
