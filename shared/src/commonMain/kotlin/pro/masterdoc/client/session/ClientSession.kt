package pro.masterdoc.client.session

import pro.masterdoc.client.auth.MeResponse
import pro.masterdoc.client.navigation.FeatureId

/**
 * Local-only fixtures for composeApp / unit tests.
 * Production apps take features from gateway GET /me.
 */
object RoleFeatureFixtures {
    fun featuresForRole(role: String): Set<FeatureId> =
        when (role.lowercase()) {
            "engineer" -> setOf(FeatureId.Tickets, FeatureId.Profile)
            "dispatcher" -> setOf(FeatureId.Board, FeatureId.Map, FeatureId.Profile)
            "technologist" -> setOf(FeatureId.Charts, FeatureId.Equipment, FeatureId.Profile)
            else -> setOf(FeatureId.Profile)
        }
}

/**
 * Session used by role shells (nav assembly).
 */
data class ClientSession(
    val role: String,
    val features: Set<FeatureId>,
) {
    companion object {
        fun stub(role: String): ClientSession =
            ClientSession(role = role, features = RoleFeatureFixtures.featuresForRole(role))

        fun fromMe(me: MeResponse): ClientSession {
            val role = me.userInfo.roles.firstOrNull() ?: "unknown"
            val features =
                me.features
                    .mapNotNull { FeatureId.fromWire(it) }
                    .toMutableSet()
                    .apply { add(FeatureId.Profile) }
            return ClientSession(role = role, features = features)
        }
    }
}
