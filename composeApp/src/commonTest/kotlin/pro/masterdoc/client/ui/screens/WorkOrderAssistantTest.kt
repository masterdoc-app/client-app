package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pro.masterdoc.client.auth.MentorHistoryTurn

class WorkOrderAssistantTest {
    @Test
    fun assigneeSeesAssistantControl() {
        assertTrue(shouldShowWoAssistant(assigneeId = "eng-1", currentUserId = "eng-1"))
    }

    @Test
    fun nonAssigneeDoesNotSeeAssistantControl() {
        assertFalse(shouldShowWoAssistant(assigneeId = "other", currentUserId = "eng-1"))
        assertFalse(shouldShowWoAssistant(assigneeId = null, currentUserId = "eng-1"))
        assertFalse(shouldShowWoAssistant(assigneeId = "eng-1", currentUserId = null))
        assertFalse(shouldShowWoAssistant(assigneeId = "eng-1", currentUserId = ""))
        assertFalse(shouldShowWoAssistant(assigneeId = "", currentUserId = "eng-1"))
    }

    @Test
    fun mentorHistoryKeepsPriorTurnsForMultiTurn() {
        val messages =
            listOf(
                WoAssistantMessage(role = "user", content = "First?"),
                WoAssistantMessage(role = "assistant", content = "Step 1"),
            )
        assertEquals(
            listOf(
                MentorHistoryTurn(role = "user", content = "First?"),
                MentorHistoryTurn(role = "assistant", content = "Step 1"),
            ),
            toMentorHistory(messages),
        )
    }

    @Test
    fun forbiddenStatusClosesAssistant() {
        assertTrue(isMentorAssigneeForbidden(403))
        assertFalse(isMentorAssigneeForbidden(500))
        assertFalse(isMentorAssigneeForbidden(401))
    }
}
