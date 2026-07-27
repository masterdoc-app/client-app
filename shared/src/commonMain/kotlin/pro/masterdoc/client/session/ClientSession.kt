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

    fun engineerEquipment(): Set<FeatureId> = setOf(FeatureId.Equipment, FeatureId.Profile)

    fun engineerCopilot(): Set<FeatureId> = setOf(FeatureId.Copilot, FeatureId.Profile)

    fun usersAdmin(): Set<FeatureId> = setOf(FeatureId.Users, FeatureId.Profile)

    fun blackBox(): Set<FeatureId> = setOf(FeatureId.BlackBox, FeatureId.Profile)
}

/**
 * Profile fields from gateway GET /me (`userInfo`).
 * Optional strings are omitted in UI when blank.
 */
data class SessionUser(
    val id: String,
    val email: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
)

/**
 * Session used by the feature shell (nav assembly). No IdP grants — only capabilities.
 */
data class ClientSession(
    val features: Set<FeatureId>,
    val user: SessionUser? = null,
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
            val info = me.userInfo
            return ClientSession(
                features = features,
                user =
                    SessionUser(
                        id = info.id,
                        email = info.email?.trim()?.takeIf { it.isNotEmpty() },
                        givenName = info.givenName?.trim()?.takeIf { it.isNotEmpty() },
                        familyName = info.familyName?.trim()?.takeIf { it.isNotEmpty() },
                    ),
            )
        }
    }
}
