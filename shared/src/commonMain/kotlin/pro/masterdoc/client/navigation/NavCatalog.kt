package pro.masterdoc.client.navigation

/**
 * Full catalog of primary nav items. [DefaultNavMenuBuilder] filters by features.
 */
object NavCatalog {
    val all: List<NavItemSpec> = listOf(
        NavItemSpec(
            destination = NavDestinationId.Tickets,
            featureId = FeatureId.Tickets,
            titleKey = "nav.tickets",
            iconKey = "tickets",
            order = 10,
        ),
        NavItemSpec(
            destination = NavDestinationId.Board,
            featureId = FeatureId.Board,
            titleKey = "nav.board",
            iconKey = "board",
            order = 20,
        ),
        NavItemSpec(
            destination = NavDestinationId.Map,
            featureId = FeatureId.Map,
            titleKey = "nav.map",
            iconKey = "map",
            order = 30,
        ),
        NavItemSpec(
            destination = NavDestinationId.Charts,
            featureId = FeatureId.Charts,
            titleKey = "nav.charts",
            iconKey = "charts",
            order = 40,
        ),
        NavItemSpec(
            destination = NavDestinationId.Equipment,
            featureId = FeatureId.Equipment,
            titleKey = "nav.equipment",
            iconKey = "equipment",
            order = 50,
        ),
        NavItemSpec(
            destination = NavDestinationId.BlackBox,
            featureId = FeatureId.BlackBox,
            titleKey = "nav.black_box",
            iconKey = "black_box",
            order = 65,
        ),
        NavItemSpec(
            destination = NavDestinationId.Users,
            featureId = FeatureId.Users,
            titleKey = "nav.admin",
            iconKey = "admin",
            order = 70,
        ),
        NavItemSpec(
            destination = NavDestinationId.Profile,
            featureId = FeatureId.Profile,
            titleKey = "nav.profile",
            iconKey = "profile",
            order = 100,
        ),
    )
}
