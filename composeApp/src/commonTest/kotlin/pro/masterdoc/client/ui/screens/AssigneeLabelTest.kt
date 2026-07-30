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
    fun formatAssigneeLabelFallsBackToGenericLabelNeverUuid() {
        assertEquals(
            "Пользователь",
            formatAssigneeLabel("29eb1297-8603-4976-8b6e-d0520f05589c", emptyList()),
        )
    }

    @Test
    fun formatAssigneeLabelUsesYouForCurrentUser() {
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
        assertEquals("Вы", formatAssigneeLabel("u1", users, currentUserId = "u1"))
        assertEquals("Ivan Petrov · a@example.com", formatAssigneeLabel("u1", users, currentUserId = "other"))
    }

    @Test
    fun resolvePprLabelsPreferTitles() {
        assertEquals(
            "Карта ТО" to "Смазка",
            resolvePprLabels(
                mapTitle = "Карта ТО",
                itemTitle = "Смазка",
                mapId = "map-uuid",
                itemId = "item-uuid",
            ),
        )
        assertEquals(
            "ППР" to "Пункт ППР",
            resolvePprLabels(
                mapTitle = null,
                itemTitle = null,
                mapId = "map-uuid",
                itemId = "item-uuid",
            ),
        )
        assertEquals(
            "—" to "—",
            resolvePprLabels(
                mapTitle = null,
                itemTitle = null,
                mapId = null,
                itemId = null,
            ),
        )
    }

    @Test
    fun filterEngineerEligibleAssigneesDropsBoardOnlyWhenUsersKnown() {
        val users =
            listOf(
                AdminUser(
                    id = "eng",
                    email = "e@x.com",
                    givenName = "E",
                    familyName = "N",
                    features = listOf("engineer", "board"),
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
            filterEngineerEligibleAssignees(listOf("eng", "disp", "unknown"), users),
        )
    }

    @Test
    fun filterEngineerEligibleAssigneesPassthroughWithoutUsers() {
        assertEquals(
            listOf("a", "b"),
            filterEngineerEligibleAssignees(listOf("a", "b"), emptyList()),
        )
    }
}
