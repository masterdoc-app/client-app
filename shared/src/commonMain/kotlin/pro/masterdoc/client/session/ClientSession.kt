package pro.masterdoc.client.session

import pro.masterdoc.client.navigation.FeatureId

/**
 * Stub role → feature sets until gateway GET /me is wired.
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
 * Minimal session used by the shell until auth is integrated.
 */
data class ClientSession(
    val role: String,
    val features: Set<FeatureId>,
) {
    companion object {
        fun stub(role: String): ClientSession =
            ClientSession(role = role, features = RoleFeatureFixtures.featuresForRole(role))
    }
}
