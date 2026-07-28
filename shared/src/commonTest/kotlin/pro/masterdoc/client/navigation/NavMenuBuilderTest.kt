package pro.masterdoc.client.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pro.masterdoc.client.session.FeatureSetFixtures

class NavMenuBuilderTest {
    private val builder: NavMenuBuilder = DefaultNavMenuBuilder()

    @Test
    fun ticketsFixture_hasTicketsAndProfile() {
        val items = builder.build(FeatureSetFixtures.tickets())
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
    fun boardOnly_hasBoardAndProfile() {
        val items = builder.build(FeatureSetFixtures.board())
        assertEquals(
            listOf(NavDestinationId.Board, NavDestinationId.Profile),
            items.map { it.destination },
        )
        assertTrue(FeatureSetFixtures.board().canAccessWorkOrderBoard())
    }

    @Test
    fun chartsEquipment_hasChartsEquipmentProfile() {
        val items = builder.build(FeatureSetFixtures.chartsEquipment())
        assertEquals(
            listOf(NavDestinationId.Board, NavDestinationId.Charts, NavDestinationId.Equipment, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun engineerEquipment_hasBoardEquipmentProfile_withoutCopilot() {
        val features = FeatureSetFixtures.engineerEquipment()
        val items = builder.build(features)
        assertEquals(
            listOf(NavDestinationId.Board, NavDestinationId.Equipment, NavDestinationId.Profile),
            items.map { it.destination },
        )
        assertTrue(features.canAccessWorkOrderBoard())
        assertTrue(NavCatalog.all.none { it.destination.name == "Copilot" })
        assertFalse(items.any { it.titleKey.contains("copilot", ignoreCase = true) })
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
}
