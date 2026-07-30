package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import pro.masterdoc.client.auth.EngineerLocationDto

class MapScreenTest {
    @Test
    fun retry_isDeferredToTheScheduledPoll() {
        assertFalse(isMapRetryEnabled())
    }

    @Test
    fun pollingDelay_staysAtTwentySeconds_whenMarkersExpireSoon() {
        assertEquals(20_000L, mapPollingDelayMillis())
    }

    @Test
    fun markers_hideLocationsOlderThanFreshnessWindow() {
        val nowEpochMillis = 1_753_906_200_000L

        assertEquals(
            listOf(
                EngineerMapMarker("На линии", 55.751244, 37.618423),
            ),
            engineerMapMarkers(
                locations =
                    listOf(
                        EngineerLocationDto(
                            userId = "active-engineer",
                            lat = 55.751244,
                            lon = 37.618423,
                            recordedAt = "2025-07-30T20:09:00Z",
                            displayName = "На линии",
                        ),
                        EngineerLocationDto(
                            userId = "stale-engineer",
                            lat = 59.93428,
                            lon = 30.335099,
                            recordedAt = "2025-07-30T20:02:59Z",
                            displayName = "Устарел",
                        ),
                    ),
                nowEpochMillis = nowEpochMillis,
            ),
        )
    }

    @Test
    fun markers_preferDisplayName_andFallBackToShortUserId() {
        val markers =
            engineerMapMarkers(
                listOf(
                    EngineerLocationDto(
                        userId = "8fb7c977-2a42-4dc7-9d95-e733eeb17eac",
                        lat = 55.751244,
                        lon = 37.618423,
                        recordedAt = "2026-07-30T20:00:00Z",
                        displayName = "Иван Петров",
                    ),
                    EngineerLocationDto(
                        userId = "abcdef0123456789",
                        lat = 59.93428,
                        lon = 30.335099,
                        recordedAt = "2026-07-30T20:01:00Z",
                    ),
                ),
                nowEpochMillis = 1_753_906_200_000L,
            )

        assertEquals(
            listOf(
                EngineerMapMarker("Иван Петров", 55.751244, 37.618423),
                EngineerMapMarker("abcdef01", 59.93428, 30.335099),
            ),
            markers,
        )
    }
}
