package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AiMessageDto

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
    fun formatAssigneeShortLabelUsesNameOnlyNotEmailCompound() {
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
        assertEquals("Ivan Petrov", formatAssigneeShortLabel("u1", users))
    }

    @Test
    fun formatAssigneeShortLabelFallsBackToEmailWhenNoName() {
        val users =
            listOf(
                AdminUser(
                    id = "u1",
                    email = "a@example.com",
                    givenName = "",
                    familyName = "",
                    features = emptyList(),
                    state = "active",
                ),
            )
        assertEquals("a@example.com", formatAssigneeShortLabel("u1", users))
    }

    @Test
    fun formatAssigneeShortLabelFallsBackToGenericLabelNeverUuid() {
        assertEquals(
            "Пользователь",
            formatAssigneeShortLabel("29eb1297-8603-4976-8b6e-d0520f05589c", emptyList()),
        )
    }

    @Test
    fun formatAssigneeShortLabelUsesYouForCurrentUser() {
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
        assertEquals("Вы", formatAssigneeShortLabel("u1", users, currentUserId = "u1"))
        assertEquals("Ivan Petrov", formatAssigneeShortLabel("u1", users, currentUserId = "other"))
    }

    @Test
    fun formatWorkOrderDisplayTitleStripsTrailingNumericId() {
        assertEquals("UI cycle", formatWorkOrderDisplayTitle("UI cycle 1785922922898"))
        assertEquals("Smoke cycle", formatWorkOrderDisplayTitle("Smoke cycle 1785922852221"))
    }

    @Test
    fun formatWorkOrderDisplayTitleKeepsHumanTitles() {
        assertEquals("Авария компрессора", formatWorkOrderDisplayTitle("Авария компрессора"))
        assertEquals("Осмотр №12", formatWorkOrderDisplayTitle("Осмотр №12"))
    }

    @Test
    fun formatWorkOrderDisplayTitleFallsBackWhenOnlyOpaqueId() {
        assertEquals("Заявка", formatWorkOrderDisplayTitle("1785922922898"))
        assertEquals(
            "Заявка",
            formatWorkOrderDisplayTitle("29eb1297-8603-4976-8b6e-d0520f05589c"),
        )
        assertEquals(
            "Заявка",
            formatWorkOrderDisplayTitle("  29eb1297-8603-4976-8b6e-d0520f05589c  "),
        )
    }

    @Test
    fun assigneeInitialsUsesFirstLettersOfGivenAndFamilyName() {
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
        assertEquals("IP", assigneeInitials("u1", users))
    }

    @Test
    fun assigneeInitialsUsesSingleLetterWhenOnlyGivenName() {
        val users =
            listOf(
                AdminUser(
                    id = "u1",
                    email = "a@example.com",
                    givenName = "Ivan",
                    familyName = "",
                    features = emptyList(),
                    state = "active",
                ),
            )
        assertEquals("I", assigneeInitials("u1", users))
    }

    @Test
    fun assigneeInitialsUsesEmailFirstLetterWhenNoName() {
        val users =
            listOf(
                AdminUser(
                    id = "u1",
                    email = "a@example.com",
                    givenName = "",
                    familyName = "",
                    features = emptyList(),
                    state = "active",
                ),
            )
        assertEquals("A", assigneeInitials("u1", users))
    }

    @Test
    fun assigneeInitialsReturnsQuestionMarkForMissingUser() {
        assertEquals("?", assigneeInitials("29eb1297-8603-4976-8b6e-d0520f05589c", emptyList()))
    }

    @Test
    fun assigneeInitialsUsesInitialsForCurrentUserNotYou() {
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
        assertEquals("IP", assigneeInitials("u1", users, currentUserId = "u1"))
    }

    @Test
    fun assigneeInitialsFallsBackToCyrillicVForCurrentUserWithoutNameOrEmail() {
        val users =
            listOf(
                AdminUser(
                    id = "u1",
                    email = "",
                    givenName = "",
                    familyName = "",
                    features = emptyList(),
                    state = "active",
                ),
            )
        assertEquals("В", assigneeInitials("u1", users, currentUserId = "u1"))
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
            listOf("eng"),
            filterEngineerEligibleAssignees(listOf("eng", "disp", "unknown"), users),
        )
    }

    @Test
    fun filterEngineerEligibleAssigneesDropsUnknownWhenUsersKnown() {
        val users =
            listOf(
                AdminUser(
                    id = "eng",
                    email = "e@x.com",
                    givenName = "E",
                    familyName = "N",
                    features = listOf("engineer"),
                    state = "active",
                ),
            )
        assertEquals(
            listOf("eng"),
            filterEngineerEligibleAssignees(listOf("eng", "ghost-scope-user"), users),
        )
    }

    @Test
    fun filterEngineerEligibleAssigneesPassthroughWithoutUsers() {
        assertEquals(
            listOf("a", "b"),
            filterEngineerEligibleAssignees(listOf("a", "b"), emptyList()),
        )
    }

    @Test
    fun aiMessageEntityLabelsUsesTitlesAndNamesNeverIds() {
        val users =
            listOf(
                AdminUser(
                    id = "383177205334671363",
                    email = "eng@example.com",
                    givenName = "Иван",
                    familyName = "Петров",
                    features = emptyList(),
                    state = "active",
                ),
            )
        val message =
            AiMessageDto(
                id = "m1",
                orgId = "o1",
                kind = "outside_workshop_radius",
                workOrderId = "412138a9-249b-4790-a320-5e8cc9cf84d4",
                siteId = "s1",
                engineerId = "383177205334671363",
                title = "Инженер вне цеха",
                body = "body",
                createdAt = "2026-08-01T05:56:04Z",
            )
        assertEquals(
            "Заявка: Утечка · Инженер: Иван Петров · eng@example.com",
            aiMessageEntityLabels(
                message = message,
                workOrderTitleById = mapOf(message.workOrderId to "Утечка"),
                users = users,
            ),
        )
        assertEquals(
            "Заявка: без названия · Инженер: Пользователь",
            aiMessageEntityLabels(
                message = message,
                workOrderTitleById = emptyMap(),
                users = emptyList(),
            ),
        )
    }
}
