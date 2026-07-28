package pro.masterdoc.client.navigation

/**
 * Filters [NavCatalog] by enabled features, sorted by [NavItemSpec.order].
 * Fails if the result would exceed [NavMenuBuilder.MAX_ITEMS].
 */
class DefaultNavMenuBuilder(
    private val catalog: List<NavItemSpec> = NavCatalog.all,
) : NavMenuBuilder {
    override fun build(features: Set<FeatureId>): List<NavItemSpec> {
        val items =
            catalog
                .filter { spec ->
                    when (spec.destination) {
                        NavDestinationId.Board -> features.canAccessWorkOrderBoard()
                        NavDestinationId.MyWorkOrders -> FeatureId.Engineer in features
                        else -> spec.featureId in features
                    }
                }
                .sortedBy { it.order }
        require(items.size <= NavMenuBuilder.MAX_ITEMS) {
            "Primary nav may have at most ${NavMenuBuilder.MAX_ITEMS} items, got ${items.size}"
        }
        return items
    }
}
