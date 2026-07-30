package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkOrdersListQueryTest {
    @Test
    fun noParams() {
        assertEquals("", workOrdersListQuery(null, null))
        assertEquals("", workOrdersListQuery("", ""))
        assertEquals("", workOrdersListQuery("  ", "  "))
    }

    @Test
    fun assigneeOnly() {
        assertEquals("?assigneeId=engineer-1", workOrdersListQuery("engineer-1", null))
    }

    @Test
    fun createdByOnly() {
        assertEquals("?createdBy=customer-1", workOrdersListQuery(null, "customer-1"))
    }

    @Test
    fun bothParams() {
        assertEquals(
            "?assigneeId=engineer-1&createdBy=customer-1",
            workOrdersListQuery("engineer-1", "customer-1"),
        )
    }
}
