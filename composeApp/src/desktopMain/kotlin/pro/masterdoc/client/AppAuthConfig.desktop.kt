package pro.masterdoc.client

import pro.masterdoc.client.auth.AuthConfig

actual fun platformAuthClientId(): String = GeneratedAuthDefaults.WEB_CLIENT_ID

actual fun platformAuthRedirectUri(): String = AuthConfig.LOCAL_REDIRECT_URI

actual fun usesInAppPasswordLogin(): Boolean = false
