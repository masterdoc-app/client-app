package pro.masterdoc.client

import pro.masterdoc.client.auth.AuthConfig

fun appAuthConfig(): AuthConfig {
    val host = currentHostname()
    val local = host.contains("localhost") || host.startsWith("127.")
    return AuthConfig(
        clientId = GeneratedAuthDefaults.WEB_CLIENT_ID,
        redirectUri =
            if (local) {
                AuthConfig.LOCAL_REDIRECT_URI
            } else {
                AuthConfig.DEFAULT_REDIRECT_URI
            },
    )
}

expect fun currentHostname(): String
