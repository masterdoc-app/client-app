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
                    userInfo =
                        UserInfoDto(
                            id = "u1",
                            roles = listOf("technologist"),
                        ),
                    features = listOf("charts", "equipment"),
                ),
            )
        assertEquals("technologist", session.role)
        assertTrue(FeatureId.Charts in session.features)
        assertTrue(FeatureId.Equipment in session.features)
        assertTrue(FeatureId.Profile in session.features)
    }
}
