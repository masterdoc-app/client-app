package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkOrderCommentsTest {
    @Test
    fun composerSubmitsOnlyNonBlankTextWhenIdle() {
        assertTrue(canSubmitWorkOrderComment(text = "Комментарий", sending = false))
        assertTrue(canSubmitWorkOrderComment(text = "  Комментарий  ", sending = false))
        assertFalse(canSubmitWorkOrderComment(text = "   ", sending = false))
        assertFalse(canSubmitWorkOrderComment(text = "Комментарий", sending = true))
    }
}
