package pro.masterdoc.client.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import pro.masterdoc.client.session.FeatureSetFixtures

class NavMenuBuilderTest {
    private val builder: NavMenuBuilder = DefaultNavMenuBuilder()

    @Test
    fun copilotFixture_hasTicketsAndProfile() {
        val items = builder.build(FeatureSetFixtures.copilot())
        assertEquals(
            listOf(NavDestinationId.Tickets, NavDestinationId.Profile),
            items.map { it.destination },
        )
        assertTrue(items.size <= NavMenuBuilder.MAX_ITEMS)
    }

    @Test
    fun boardFixture_hasBoardMapProfile() {
        // Board fixture is board+profile; Map is separate wire — keep board-only nav for MVP fixture
        val items = builder.build(FeatureSetFixtures.board() + FeatureId.Map)
        assertEquals(
            listOf(NavDestinationId.Board, NavDestinationId.Map, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun chartsEquipment_hasChartsEquipmentProfile() {
        val items = builder.build(FeatureSetFixtures.chartsEquipment())
        assertEquals(
            listOf(NavDestinationId.Charts, NavDestinationId.Equipment, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun usersAdmin_hasUsersAndProfile() {
        val items = builder.build(FeatureSetFixtures.usersAdmin())
        assertEquals(
            listOf(NavDestinationId.Users, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun blackBox_hasBlackBoxAndProfile() {
        val items = builder.build(FeatureSetFixtures.blackBox())
        assertEquals(
            listOf(NavDestinationId.BlackBox, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun multiFeatureUnion_includesAllMatchingNavItems() {
        val features =
            FeatureSetFixtures.chartsEquipment() +
                FeatureSetFixtures.usersAdmin() +
                FeatureSetFixtures.board() +
                FeatureSetFixtures.blackBox() +
                FeatureId.Map
        val items = builder.build(features)
        assertEquals(
            listOf(
                NavDestinationId.Board,
                NavDestinationId.Map,
                NavDestinationId.Charts,
                NavDestinationId.Equipment,
                NavDestinationId.BlackBox,
                NavDestinationId.Users,
                NavDestinationId.Profile,
            ),
            items.map { it.destination },
        )
    }

    @Test
    fun alwaysIncludesProfileWhenPresentInFeatures() {
        val items = builder.build(setOf(FeatureId.Profile))
        assertEquals(listOf(NavDestinationId.Profile), items.map { it.destination })
    }

    @Test
    fun rejectsMoreThanMaxItems() {
        val oversized =
            (0..NavMenuBuilder.MAX_ITEMS).map { index ->
                NavItemSpec(
                    destination = NavDestinationId.Profile,
                    featureId = FeatureId.Profile,
                    titleKey = "nav.$index",
                    iconKey = "profile",
                    order = index,
                )
            }
        val limited = DefaultNavMenuBuilder(catalog = oversized)
        assertFailsWith<IllegalArgumentException> {
            limited.build(setOf(FeatureId.Profile))
        }
    }

    @Test
    fun copilotReserved_notInCopilotTicketFixture() {
        val items = builder.build(FeatureSetFixtures.copilot())
        assertTrue(items.none { it.destination == NavDestinationId.Copilot })
    }
}
