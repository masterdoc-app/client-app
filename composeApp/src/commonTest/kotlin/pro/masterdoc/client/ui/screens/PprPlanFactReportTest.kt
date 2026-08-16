package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.IsoDates
import kotlin.test.Test
import kotlin.test.assertEquals

class PprPlanFactReportTest {
    private val today = 20_000L

    @Test
    fun openOverdueReturnsProsrochen() {
        assertEquals(
            "Просрочен",
            formatPprPlanFactOutcomeLabel(
                "new",
                IsoDates.formatEpochDay(today - 1),
                null,
                today,
            ),
        )
    }

    @Test
    fun openDueTodayOrLaterReturnsOzhidaet() {
        val dueDay = IsoDates.formatEpochDay(today)
        assertEquals(
            "Ожидает",
            formatPprPlanFactOutcomeLabel("in_progress", dueDay, null, today),
        )
    }

    @Test
    fun closedOnTimeReturnsVovremya() {
        assertEquals(
            "Вовремя",
            formatPprPlanFactOutcomeLabel(
                status = "closed",
                dueAt = "2026-07-15",
                closedAt = "2026-07-15T18:00:00Z",
                todayEpochDay = today,
            ),
        )
        assertEquals(
            "Вовремя",
            formatPprPlanFactOutcomeLabel(
                status = "closed",
                dueAt = "2026-07-15",
                closedAt = "2026-07-10T00:00:00Z",
                todayEpochDay = today,
            ),
        )
    }

    @Test
    fun closedLateReturnsSOpozdaniem() {
        assertEquals(
            "С опозданием",
            formatPprPlanFactOutcomeLabel(
                status = "closed",
                dueAt = "2026-07-10",
                closedAt = "2026-07-15T12:00:00Z",
                todayEpochDay = today,
            ),
        )
    }

    @Test
    fun closedWithoutParseableClosedAtReturnsZakryt() {
        assertEquals(
            "Закрыт",
            formatPprPlanFactOutcomeLabel("closed", "2026-07-10", null, today),
        )
        assertEquals(
            "Закрыт",
            formatPprPlanFactOutcomeLabel("closed", "2026-07-10", "invalid", today),
        )
    }
}
