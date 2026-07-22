package pro.masterdoc.client.session

import pro.masterdoc.client.auth.MeResponse
import pro.masterdoc.client.auth.UserInfoDto
import pro.masterdoc.client.navigation.FeatureId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientSessionFromMeTest {
    @Test
    fun fromMe_mapsFeaturesAndAlwaysAddsProfile() {
        val session =
            ClientSession.fromMe(
                MeResponse(
                    userInfo = UserInfoDto(id = "u1"),
                    features = listOf("charts", "equipment", "user_invite"),
                ),
            )
        assertEquals(
            setOf(FeatureId.Charts, FeatureId.Equipment, FeatureId.Users, FeatureId.Profile),
            session.features,
        )
        assertTrue(FeatureId.Users in session.features)
    }

    @Test
    fun fromMe_ignoresUnknownWireFeatures() {
        val session =
            ClientSession.fromMe(
                MeResponse(
                    userInfo = UserInfoDto(id = "u1"),
                    features = listOf("unknown_feature", "board"),
                ),
            )
        assertEquals(setOf(FeatureId.Board, FeatureId.Profile), session.features)
    }
}
