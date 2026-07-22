package pro.masterdoc.client.auth

/**
 * OIDC / gateway settings for the feature shell (:composeApp).
 *
 * [clientId] comes from Zitadel `masterdoc-kmp-web` (terraform output `web_client_id`).
 */
data class AuthConfig(
    val gatewayBaseUrl: String = DEFAULT_GATEWAY_BASE_URL,
    val clientId: String,
    val redirectUri: String = DEFAULT_REDIRECT_URI,
    val scopes: String = DEFAULT_SCOPES,
) {
    companion object {
        const val DEFAULT_GATEWAY_BASE_URL = "https://api.masterdoc.pro"
        const val DEFAULT_REDIRECT_URI = "https://app.fixaverse.ru/auth/callback"
        // resourceowner → JWT claims urn:zitadel:iam:user:resourceowner:* (tenant org id)
        const val DEFAULT_SCOPES =
            "openid profile email offline_access urn:zitadel:iam:user:resourceowner"
        const val LOCAL_REDIRECT_URI = "http://localhost:8080/auth/callback"
    }
}
