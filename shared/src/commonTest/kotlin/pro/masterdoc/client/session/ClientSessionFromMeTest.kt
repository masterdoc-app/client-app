package pro.masterdoc.client.session

import pro.masterdoc.client.auth.MeResponse
import pro.masterdoc.client.auth.UserInfoDto
import pro.masterdoc.client.navigation.FeatureId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientSessionFromMeTest {
    @Test
    fun fromMe_mapsFeaturesAndAlwaysAddsProfile() {
        val session =
            ClientSession.fromMe(
                MeResponse(
                    userInfo = UserInfoDto(id = "u1"),
                    features = listOf("charts", "equipment", "admin"),
                ),
            )
        assertEquals(
            setOf(FeatureId.Charts, FeatureId.Equipment, FeatureId.Users, FeatureId.Profile),
            session.features,
        )
        assertEquals("admin", FeatureId.Users.wireValue)
        assertTrue(FeatureId.Users in session.features)
        assertEquals("u1", session.user?.id)
        assertNull(session.user?.email)
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

    @Test
    fun fromMe_keepsNonBlankProfileFields() {
        val session =
            ClientSession.fromMe(
                MeResponse(
                    userInfo =
                        UserInfoDto(
                            id = "u2",
                            email = "  a@b.com ",
                            givenName = "Иван",
                            familyName = "  ",
                        ),
                    features = listOf("board"),
                ),
            )
        assertEquals("a@b.com", session.user?.email)
        assertEquals("Иван", session.user?.givenName)
        assertNull(session.user?.familyName)
    }
}
