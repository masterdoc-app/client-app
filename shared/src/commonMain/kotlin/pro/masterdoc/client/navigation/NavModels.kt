package pro.masterdoc.client.navigation

import kotlinx.serialization.Serializable

/**
 * Product feature identifiers aligned with gateway / feature-service `features` strings.
 */
enum class FeatureId(val wireValue: String) {
    Tickets("tickets"),
    Board("board"),
    Engineer("engineer"),
    Map("map"),
    Charts("charts"),
    Equipment("equipment"),
    Profile("profile"),

    /** Audit journal («Чёрный ящик»). */
    BlackBox("black_box"),

    /** Admin invite / user management (`admin` from feature-service). */
    Users("admin"),
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
    MyWorkOrders,
    Map,
    Charts,
    Equipment,
    Profile,
    BlackBox,
    Users,
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

/** Board nav is available only to dispatchers with the `board` feature. */
fun Set<FeatureId>.canAccessWorkOrderBoard(): Boolean =
    FeatureId.Board in this
