package pro.masterdoc.client.navigation

import kotlinx.serialization.Serializable

/**
 * Product feature identifiers aligned with gateway / feature-service `features` strings.
 */
enum class FeatureId(val wireValue: String) {
    Tickets("tickets"),
    Board("board"),
    Map("map"),
    Charts("charts"),
    Equipment("equipment"),
    Profile("profile"),

    /** Reserved for future masterdoc / Atlant copilot tab; not enabled in MVP fixtures. */
    Copilot("copilot"),
    ;

    companion object {
        fun fromWire(value: String): FeatureId? =
            entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * Stable destinations for Decompose child pages / shell navigation.
 */
@Serializable
enum class NavDestinationId {
    Tickets,
    Board,
    Map,
    Charts,
    Equipment,
    Profile,
    Copilot,
}

/**
 * One primary navigation item (bottom bar or side rail).
 */
data class NavItemSpec(
    val destination: NavDestinationId,
    val featureId: FeatureId,
    val titleKey: String,
    val iconKey: String,
    val order: Int,
)
