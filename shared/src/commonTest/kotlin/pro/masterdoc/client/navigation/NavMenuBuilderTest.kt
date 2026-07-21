package pro.masterdoc.client.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import pro.masterdoc.client.session.RoleFeatureFixtures

class NavMenuBuilderTest {
    private val builder: NavMenuBuilder = DefaultNavMenuBuilder()

    @Test
    fun engineerMenu_hasTicketsAndProfile() {
        val items = builder.build(RoleFeatureFixtures.featuresForRole("engineer"))
        assertEquals(
            listOf(NavDestinationId.Tickets, NavDestinationId.Profile),
            items.map { it.destination },
        )
        assertTrue(items.size <= NavMenuBuilder.MAX_ITEMS)
    }

    @Test
    fun dispatcherMenu_hasBoardMapProfile() {
        val items = builder.build(RoleFeatureFixtures.featuresForRole("dispatcher"))
        assertEquals(
            listOf(NavDestinationId.Board, NavDestinationId.Map, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun technologistMenu_hasChartsEquipmentProfile() {
        val items = builder.build(RoleFeatureFixtures.featuresForRole("technologist"))
        assertEquals(
            listOf(NavDestinationId.Charts, NavDestinationId.Equipment, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun adminMenu_hasUsersAndProfile() {
        val items = builder.build(RoleFeatureFixtures.featuresForRole("admin"))
        assertEquals(
            listOf(NavDestinationId.Users, NavDestinationId.Profile),
            items.map { it.destination },
        )
    }

    @Test
    fun multiFeatureUnion_includesAllMatchingNavItems() {
        val features =
            RoleFeatureFixtures.featuresForRole("technologist") +
                RoleFeatureFixtures.featuresForRole("admin") +
                RoleFeatureFixtures.featuresForRole("dispatcher")
        val items = builder.build(features)
        assertEquals(
            listOf(
                NavDestinationId.Board,
                NavDestinationId.Map,
                NavDestinationId.Charts,
                NavDestinationId.Equipment,
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
    fun copilotReserved_notInEngineerFixture() {
        val items = builder.build(RoleFeatureFixtures.featuresForRole("engineer"))
        assertTrue(items.none { it.destination == NavDestinationId.Copilot })
    }
}
