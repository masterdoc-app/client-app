package pro.masterdoc.client

import pro.masterdoc.client.auth.AuthConfig

actual fun platformAuthRedirectUri(): String = AuthConfig.LOCAL_REDIRECT_URI
