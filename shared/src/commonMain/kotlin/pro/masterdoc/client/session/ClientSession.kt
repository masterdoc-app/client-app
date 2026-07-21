package pro.masterdoc.client.session

import pro.masterdoc.client.auth.MeResponse
import pro.masterdoc.client.navigation.FeatureId

/**
 * Local-only fixtures for previews / unit tests.
 * Production apps take features from gateway GET /me.
 */
object FeatureSetFixtures {
    fun board(): Set<FeatureId> = setOf(FeatureId.Board, FeatureId.Profile)

    fun copilot(): Set<FeatureId> = setOf(FeatureId.Tickets, FeatureId.Profile)

    fun chartsEquipment(): Set<FeatureId> =
        setOf(FeatureId.Charts, FeatureId.Equipment, FeatureId.Profile)

    fun usersAdmin(): Set<FeatureId> = setOf(FeatureId.Users, FeatureId.Profile)
}

/**
 * Session used by the feature shell (nav assembly). No IdP grants — only capabilities.
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
