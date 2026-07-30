package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.UserScopeDto
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

    @Test
    fun resolveTicketsEmptyStateShowsNoScopeWhenScopesLoadedAndEmpty() {
        val state =
            resolveTicketsEmptyState(
                scope = UserScopeDto(userId = "u1", orgId = "org1"),
                assets = emptyList(),
                scopesLoaded = true,
            )
        assertEquals(TicketsEmptyState.NoScope, state)
    }

    @Test
    fun resolveTicketsEmptyStateShowsNoEquipmentWhenScopedButNoAssets() {
        val state =
            resolveTicketsEmptyState(
                scope = UserScopeDto(userId = "u1", orgId = "org1", siteIds = listOf("s1")),
                assets = emptyList(),
                scopesLoaded = true,
            )
        assertEquals(TicketsEmptyState.NoEquipment, state)
    }

    @Test
    fun resolveTicketsEmptyStateInfersNoEquipmentWithoutScopesRepository() {
        val state =
            resolveTicketsEmptyState(
                scope = null,
                assets = emptyList(),
                scopesLoaded = false,
            )
        assertEquals(TicketsEmptyState.NoEquipment, state)
    }

    @Test
    fun resolveTicketsEmptyStateReadyWhenAssetsPresent() {
        val state =
            resolveTicketsEmptyState(
                scope = UserScopeDto(userId = "u1", orgId = "org1", siteIds = listOf("s1")),
                assets = listOf(asset("a1")),
                scopesLoaded = true,
            )
        assertNull(state)
    }

    private fun asset(id: String) =
        AssetDto(
            id = id,
            orgId = "org1",
            siteId = "s1",
            name = id,
            status = "active",
            source = "manual",
        )

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
