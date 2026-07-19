package pro.masterdoc.client.navigation

/**
 * Builds the primary navigation menu from enabled product features.
 * At most [MAX_ITEMS] items may be returned.
 */
interface NavMenuBuilder {
    fun build(features: Set<FeatureId>): List<NavItemSpec>

    companion object {
        const val MAX_ITEMS: Int = 5
    }
}
