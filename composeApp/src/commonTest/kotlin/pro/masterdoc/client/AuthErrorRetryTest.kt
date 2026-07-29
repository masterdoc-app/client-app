package pro.masterdoc.client

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
