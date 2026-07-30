package pro.masterdoc.client

import pro.masterdoc.client.auth.AuthConfig

fun appAuthConfig(): AuthConfig {
    return AuthConfig(
        clientId = GeneratedAuthDefaults.WEB_CLIENT_ID,
        redirectUri = platformAuthRedirectUri(),
    )
}

expect fun platformAuthRedirectUri(): String
