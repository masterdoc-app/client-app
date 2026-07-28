package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.AdminUser

class EngineerScopeFilterTest {
    @Test
    fun filterEngineersForScopeBindingKeepsOnlyEngineerFeature() {
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
                AdminUser(
                    id = "other",
                    email = "o@x.com",
                    givenName = "O",
                    familyName = "T",
                    features = listOf("equipment"),
                    state = "active",
                ),
            )
        assertEquals(
            listOf("eng"),
            filterEngineersForScopeBinding(users).map { it.id },
        )
    }

    @Test
    fun filterEngineersForScopeBindingReturnsEmptyWhenNoEngineers() {
        val users =
            listOf(
                AdminUser(
                    id = "disp",
                    email = "d@x.com",
                    givenName = "D",
                    familyName = "S",
                    features = listOf("board"),
                    state = "active",
                ),
            )
        assertEquals(emptyList(), filterEngineersForScopeBinding(users))
    }
}
