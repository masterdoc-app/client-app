package pro.masterdoc.client.session

import pro.masterdoc.client.auth.MeResponse
import pro.masterdoc.client.navigation.FeatureId

/**
 * Local-only fixtures for previews / unit tests.
 * Production apps take features from gateway GET /me.
 */
object RoleFeatureFixtures {
    fun featuresForRole(role: String): Set<FeatureId> =
        when (role.lowercase()) {
            "engineer" -> setOf(FeatureId.Tickets, FeatureId.Profile)
            "dispatcher" -> setOf(FeatureId.Board, FeatureId.Map, FeatureId.Profile)
            "technologist" -> setOf(FeatureId.Charts, FeatureId.Equipment, FeatureId.Profile)
            "admin" -> setOf(FeatureId.Users, FeatureId.Profile)
            else -> setOf(FeatureId.Profile)
        }
}

/**
 * Session used by the feature shell (nav assembly). No roles — only capabilities.
 */
data class ClientSession(
    val features: Set<FeatureId>,
) {
    companion object {
        fun stub(features: Set<FeatureId> = setOf(FeatureId.Tickets, FeatureId.Profile)): ClientSession =
            ClientSession(features = features)

        fun fromMe(me: MeResponse): ClientSession {
            val features =
                me.features
                    .mapNotNull { FeatureId.fromWire(it) }
                    .toMutableSet()
                    .apply { add(FeatureId.Profile) }
            return ClientSession(features = features)
        }
    }
}
