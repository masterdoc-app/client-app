package pro.masterdoc.client

import kotlinx.browser.window
import pro.masterdoc.client.auth.AuthConfig

actual fun platformAuthClientId(): String = GeneratedAuthDefaults.WEB_CLIENT_ID

actual fun platformAuthRedirectUri(): String {
    val host = window.location.hostname
    return if (host.contains("localhost") || host.startsWith("127.")) {
        AuthConfig.LOCAL_REDIRECT_URI
    } else {
        AuthConfig.DEFAULT_REDIRECT_URI
    }
}

actual fun usesInAppPasswordLogin(): Boolean = false
