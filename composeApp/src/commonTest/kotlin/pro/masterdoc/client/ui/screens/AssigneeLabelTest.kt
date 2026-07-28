package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.AdminUser

class AssigneeLabelTest {
    @Test
    fun formatAssigneeLabelUsesNameAndEmail() {
        val users =
            listOf(
                AdminUser(
                    id = "u1",
                    email = "a@example.com",
                    givenName = "Ivan",
                    familyName = "Petrov",
                    features = emptyList(),
                    state = "active",
                ),
            )
        assertEquals("Ivan Petrov · a@example.com", formatAssigneeLabel("u1", users))
    }

    @Test
    fun formatAssigneeLabelFallsBackToUserId() {
        assertEquals("unknown-id", formatAssigneeLabel("unknown-id", emptyList()))
    }

    @Test
    fun filterEquipmentEligibleAssigneesDropsBoardOnlyWhenUsersKnown() {
        val users =
            listOf(
                AdminUser(
                    id = "eng",
                    email = "e@x.com",
                    givenName = "E",
                    familyName = "N",
                    features = listOf("equipment", "board"),
                    state = "active",
                ),
                AdminUser(
                    id = "disp",
                    email = "d@x.com",
                    givenName = "D",
                    familyName = "S",
                    features = listOf("board"),
                    state = "active",
                ),
            )
        assertEquals(
            listOf("eng", "unknown"),
            filterEquipmentEligibleAssignees(listOf("eng", "disp", "unknown"), users),
        )
    }

    @Test
    fun filterEquipmentEligibleAssigneesPassthroughWithoutUsers() {
        assertEquals(
            listOf("a", "b"),
            filterEquipmentEligibleAssignees(listOf("a", "b"), emptyList()),
        )
    }
}
