package pro.masterdoc.client

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthBootstrapTest {
    @Test
    fun passwordLoginEntersPasswordForm() {
        assertEquals(
            UnauthenticatedEntry.PasswordForm,
            unauthenticatedEntry(usesInAppPasswordLogin = true),
        )
    }

    @Test
    fun browserLoginEntersOidcRedirect() {
        assertEquals(
            UnauthenticatedEntry.OidcRedirect,
            unauthenticatedEntry(usesInAppPasswordLogin = false),
        )
    }
}
