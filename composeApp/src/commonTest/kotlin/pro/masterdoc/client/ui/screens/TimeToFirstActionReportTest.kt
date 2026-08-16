package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeToFirstActionReportTest {
    @Test
    fun formatTimeToFirstActionLabelWhenNotStarted() {
        assertEquals("ещё не в работе", formatTimeToFirstActionLabel("2026-07-10T00:00:00Z", null))
    }

    @Test
    fun formatTimeToFirstActionLabelRoundsHours() {
        assertEquals(
            "6 ч до работы",
            formatTimeToFirstActionLabel("2026-07-10T00:00:00Z", "2026-07-10T06:00:00Z"),
        )
        assertEquals(
            "1 ч до работы",
            formatTimeToFirstActionLabel("2026-07-10T00:00:00Z", "2026-07-10T00:45:00Z"),
        )
    }
}
