package pro.masterdoc.client

import pro.masterdoc.client.auth.AuthConfig

fun appAuthConfig(): AuthConfig {
    return AuthConfig(
        clientId = platformAuthClientId(),
        redirectUri = platformAuthRedirectUri(),
    )
}

expect fun platformAuthClientId(): String

expect fun platformAuthRedirectUri(): String

expect fun usesInAppPasswordLogin(): Boolean
