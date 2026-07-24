package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IdTokenProfileTest {
    @Test
    fun parse_readsEmailAndNamesFromPayload() {
        val token = fakeJwt("""{"email":"a@b.com","given_name":"Иван","family_name":"Петров"}""")
        val profile = IdTokenProfile.parse(token)!!
        assertEquals("a@b.com", profile.email)
        assertEquals("Иван", profile.givenName)
        assertEquals("Петров", profile.familyName)
    }

    @Test
    fun parse_fallsBackToPreferredUsernameWhenEmailMissing() {
        val token = fakeJwt("""{"preferred_username":"mail@example.com"}""")
        assertEquals("mail@example.com", IdTokenProfile.parse(token)?.email)
    }

    @Test
    fun parse_ignoresPreferredUsernameWithoutAt() {
        val token = fakeJwt("""{"preferred_username":"not-an-email"}""")
        assertNull(IdTokenProfile.parse(token)?.email)
    }

    @Test
    fun withProfileFromIdToken_fillsBlankMeFields() {
        val me =
            MeResponse(
                userInfo = UserInfoDto(id = "u1"),
                features = listOf("board"),
            )
        val token = fakeJwt("""{"email":"x@y.z","given_name":"A"}""")
        val enriched = me.withProfileFromIdToken(token)
        assertEquals("x@y.z", enriched.userInfo.email)
        assertEquals("A", enriched.userInfo.givenName)
        assertNull(enriched.userInfo.familyName)
        assertEquals("u1", enriched.userInfo.id)
    }

    @Test
    fun withProfileFromIdToken_keepsExistingMeFields() {
        val me =
            MeResponse(
                userInfo =
                    UserInfoDto(
                        id = "u1",
                        email = "from-me@x.com",
                        givenName = "Me",
                    ),
            )
        val token = fakeJwt("""{"email":"id@x.com","given_name":"Id","family_name":"Token"}""")
        val enriched = me.withProfileFromIdToken(token)
        assertEquals("from-me@x.com", enriched.userInfo.email)
        assertEquals("Me", enriched.userInfo.givenName)
        assertEquals("Token", enriched.userInfo.familyName)
    }

    private fun fakeJwt(payloadJson: String): String {
        val header = Base64Url.encode("""{"alg":"none"}""".encodeToByteArray())
        val payload = Base64Url.encode(payloadJson.encodeToByteArray())
        return "$header.$payload.sig"
    }
}
