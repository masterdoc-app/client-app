package pro.masterdoc.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthErrorRetryTest {
    @Test
    fun callbackPath_retriesByNavigatingHome() {
        assertEquals(
            AuthErrorRetryMode.NavigateHome,
            authErrorRetryMode("/auth/callback"),
        )
        assertEquals(
            AuthErrorRetryMode.NavigateHome,
            authErrorRetryMode("/auth/callback?code=x&state=y"),
        )
    }

    @Test
    fun otherPaths_retryBootstrap() {
        assertEquals(AuthErrorRetryMode.RetryBootstrap, authErrorRetryMode("/"))
        assertEquals(AuthErrorRetryMode.RetryBootstrap, authErrorRetryMode("/board"))
    }

    @Test
    fun preservesOnlySupportedDeepLinkBeforeLogin() {
        assertEquals("#/qr/opaque-token", pendingDeepLinkHash("#/qr/opaque-token"))
        assertNull(pendingDeepLinkHash("#/unknown"))
        assertNull(pendingDeepLinkHash(""))
    }

    @Test
    fun restoresPendingDeepLinkAfterCallback() {
        assertEquals("/#/qr/opaque-token", postLoginLocation("#/qr/opaque-token"))
        assertEquals("/", postLoginLocation(null))
        assertEquals("/", postLoginLocation("#/unknown"))
    }
}
