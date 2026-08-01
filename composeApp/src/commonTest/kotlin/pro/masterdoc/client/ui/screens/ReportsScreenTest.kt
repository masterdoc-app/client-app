package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.DowntimeIntervalDto
import pro.masterdoc.client.auth.IsoDates

class ReportsScreenTest {
    @Test
    fun groupsIntervalsByAssetAndClipsToSelectedPeriod() {
        val rows =
            buildDowntimeRows(
                intervals =
                    listOf(
                        DowntimeIntervalDto(
                            assetId = "a1",
                            workOrderId = "wo1",
                            title = "Ремонт",
                            startedAt = "2026-07-01T10:00:00Z",
                            closedAt = "2026-07-05T10:00:00Z",
                            status = "closed",
                        ),
                        DowntimeIntervalDto(
                            assetId = "a1",
                            workOrderId = "wo2",
                            title = "Авария",
                            startedAt = "2026-07-08T10:00:00Z",
                            status = "in_progress",
                        ),
                    ),
                assets = listOf(asset("a1", "Насос №1")),
                fromDay = IsoDates.parseToEpochDay("2026-07-03")!!,
                toDay = IsoDates.parseToEpochDay("2026-07-10")!!,
            )

        assertEquals(listOf("Насос №1"), rows.map { it.label })
        assertEquals(2, rows.single().intervals.size)
        assertEquals(IsoDates.parseToEpochDay("2026-07-03"), rows.single().intervals[0].startDay)
        assertEquals(IsoDates.parseToEpochDay("2026-07-05"), rows.single().intervals[0].endDay)
        assertTrue(rows.single().intervals[1].open)
        assertEquals(IsoDates.parseToEpochDay("2026-07-10"), rows.single().intervals[1].endDay)
    }

    private fun asset(id: String, name: String) =
        AssetDto(
            id = id,
            orgId = "org",
            siteId = "site",
            name = name,
            status = "active",
            source = "manual",
        )
}
