package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.WorkOrderDto

class TicketsPartitionTest {
    @Test
    fun partitionCustomerTicketsSeparatesActiveAndClosedOrders() {
        val orders =
            listOf(
                order("new", "new"),
                order("progress", "in_progress"),
                order("closed", "closed"),
                order("ignored", "cancelled"),
            )

        val (active, done) = partitionCustomerTickets(orders)

        assertEquals(listOf("new", "progress"), active.map { it.id })
        assertEquals(listOf("closed"), done.map { it.id })
    }

    private fun order(id: String, status: String) =
        WorkOrderDto(
            id = id,
            orgId = "org",
            type = "emergency",
            status = status,
            title = id,
            assetId = "asset",
            siteId = "site",
            dueAt = "2026-07-29",
            source = "manual",
            createdAt = "2026-07-29T00:00:00Z",
            updatedAt = "2026-07-29T00:00:00Z",
        )
}
