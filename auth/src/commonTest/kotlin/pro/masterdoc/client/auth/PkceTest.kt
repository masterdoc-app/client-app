package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PkceTest {
    @Test
    fun verifier_lengthAndAlphabet() {
        val verifier = Pkce.generateVerifier(64)
        assertEquals(64, verifier.length)
        assertTrue(verifier.all { it.isLetterOrDigit() || it in "-._~" })
    }

    @Test
    fun challenge_isBase64UrlWithoutPadding() {
        val challenge = Pkce.challengeS256("test-verifier-value-with-enough-length-123456")
        assertTrue(challenge.isNotBlank())
        assertTrue('=' !in challenge)
        assertTrue('+' !in challenge)
        assertTrue('/' !in challenge)
    }

    @Test
    fun challenge_isDeterministic() {
        val v = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJK"
        assertEquals(Pkce.challengeS256(v), Pkce.challengeS256(v))
    }
}
