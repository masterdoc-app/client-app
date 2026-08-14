package pro.masterdoc.client

import pro.masterdoc.client.auth.AuthConfig

actual fun platformAuthClientId(): String = GeneratedAuthDefaults.NATIVE_CLIENT_ID

actual fun platformAuthRedirectUri(): String = AuthConfig.ANDROID_REDIRECT_URI

actual fun usesInAppPasswordLogin(): Boolean = true
