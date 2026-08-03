package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pro.masterdoc.client.auth.EngineerLocationDto
import pro.masterdoc.client.auth.SiteDto

class MapScreenTest {
    @Test
    fun markerUrl_opensItsOpenStreetMapCoordinates() {
        assertEquals(
            "https://www.openstreetmap.org/?mlat=55.751244&mlon=37.618423",
            engineerMapUrl(EngineerMapMarker("Инженер", 55.751244, 37.618423)),
        )
    }

    @Test
    fun mapMarkers_whenNoEngineers_centerOnFirstSiteWithCoordinates() {
        val sites =
            listOf(
                SiteDto(id = "no-coords", orgId = "o", name = "Склад"),
                SiteDto(
                    id = "ceh-1",
                    orgId = "o",
                    name = "Цех 1",
                    lat = 55.8123,
                    lon = 37.4567,
                ),
                SiteDto(
                    id = "ceh-2",
                    orgId = "o",
                    name = "Цех 2",
                    lat = 59.93,
                    lon = 30.33,
                ),
            )

        assertEquals(
            listOf(EngineerMapMarker("Цех 1", 55.8123, 37.4567)),
            mapDisplayMarkers(engineerMarkers = emptyList(), sites = sites),
        )
    }

    @Test
    fun mapMarkers_whenEngineersPresent_keepEngineerMarkersOnly() {
        val engineers = listOf(EngineerMapMarker("Иван", 55.75, 37.61))
        val sites =
            listOf(
                SiteDto(id = "ceh-1", orgId = "o", name = "Цех 1", lat = 55.8123, lon = 37.4567),
            )

        assertEquals(engineers, mapDisplayMarkers(engineerMarkers = engineers, sites = sites))
    }

    @Test
    fun mapMarkers_whenNoEngineersAndNoSiteCoords_stayEmpty() {
        assertTrue(
            mapDisplayMarkers(
                engineerMarkers = emptyList(),
                sites = listOf(SiteDto(id = "s", orgId = "o", name = "Цех")),
            ).isEmpty(),
        )
    }

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
    fun markers_preferDisplayName_andFallBackToGenericLabelNeverId() {
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
                EngineerMapMarker("Инженер", 59.93428, 30.335099),
            ),
            markers,
        )
    }
}
