package pro.masterdoc.client.auth

import kotlinx.serialization.json.Json

class AuthRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val pkceStore: PkceSessionStore,
    private val json: Json = defaultJson,
) {
    suspend fun buildAuthorizeUrl(prompt: String? = null): String {
        val authorizeBase = fetchAuthorizeBase().trimEnd('/')
        val verifier = Pkce.generateVerifier()
        val state = Pkce.generateState()
        pkceStore.save(verifier = verifier, state = state)
        val challenge = Pkce.challengeS256(verifier)
        return buildString {
            append(authorizeBase)
            append("?response_type=code")
            append("&client_id=").append(encodeQuery(config.clientId))
            append("&redirect_uri=").append(encodeQuery(config.redirectUri))
            append("&scope=").append(encodeQuery(config.scopes))
            append("&code_challenge=").append(encodeQuery(challenge))
            append("&code_challenge_method=S256")
            append("&state=").append(encodeQuery(state))
            if (prompt != null) {
                append("&prompt=").append(encodeQuery(prompt))
            }
        }
    }

    suspend fun exchangeCode(
        code: String,
        returnedState: String?,
    ): AuthTokens {
        val state =
            returnedState?.takeIf { it.isNotBlank() }
                ?: throw GatewayHttpException(400, "Missing OIDC state")
        val verifier =
            pkceStore.consume(state)
                ?: throw GatewayHttpException(400, "OIDC state mismatch")
        val form =
            buildString {
                append("grant_type=authorization_code")
                append("&code=").append(encodeForm(code))
                append("&redirect_uri=").append(encodeForm(config.redirectUri))
                append("&client_id=").append(encodeForm(config.clientId))
                append("&code_verifier=").append(encodeForm(verifier))
            }
        val response =
            http.postForm(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/auth/token",
                body = form,
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "Token exchange failed: ${response.body}")
        }
        val tokens = AuthTokens.from(json.decodeFromString<TokenResponse>(response.body))
        tokenStore.write(tokens)
        pkceStore.clear()
        return tokens
    }

    suspend fun refresh(): AuthTokens {
        val refresh =
            tokenStore.read()?.refreshToken
                ?: throw GatewayHttpException(401, "No refresh token")
        val form =
            buildString {
                append("grant_type=refresh_token")
                append("&refresh_token=").append(encodeForm(refresh))
                append("&client_id=").append(encodeForm(config.clientId))
            }
        val response =
            http.postForm(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/auth/token",
                body = form,
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "Refresh failed: ${response.body}")
        }
        val tokens = AuthTokens.from(json.decodeFromString<TokenResponse>(response.body))
        tokenStore.write(tokens)
        return tokens
    }

    fun logout() {
        tokenStore.clear()
        pkceStore.clear()
    }

    /**
     * Local logout + authorize URL with `prompt=login` so Zitadel SSO cannot
     * silently re-issue a code (previous flow cleared tokens then called
     * bootstrap → startLogin without prompt → instant re-login).
     */
    suspend fun logoutRedirectUrl(): String {
        logout()
        return buildAuthorizeUrl(prompt = "login")
    }

    fun currentTokens(): AuthTokens? = tokenStore.read()

    private suspend fun fetchAuthorizeBase(): String {
        val response = http.get("${config.gatewayBaseUrl.trimEnd('/')}/auth/url")
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "auth/url failed: ${response.body}")
        }
        return json.decodeFromString<AuthUrlResponse>(response.body).authUrl
    }

    companion object {
        val defaultJson: Json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
    }
}

class MeRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = AuthRepository.defaultJson,
) {
    suspend fun getMe(): MeResponse {
        val access =
            tokenStore.read()?.accessToken
                ?: throw GatewayHttpException(401, "Not authenticated")
        val response =
            http.get(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/me",
                headers = mapOf("Authorization" to "Bearer $access"),
            )
        if (!response.isSuccessful) {
            println("[auth] GET /me failed status=${response.status} body=${response.body}")
            throw GatewayHttpException(response.status, "GET /me failed: ${response.body}")
        }
        val me =
            json
                .decodeFromString<MeResponse>(response.body)
                .withProfileFromIdToken(tokenStore.read()?.idToken)
        println(
            "[auth] GET /me ok id=${me.userInfo.id} email=${me.userInfo.email} " +
                "features=${me.features} raw=${response.body}",
        )
        return me
    }
}

internal fun encodeQuery(value: String): String = encodeForm(value)

internal fun encodeForm(value: String): String =
    buildString(value.length) {
        for (ch in value) {
            when {
                ch.isAsciiLetterOrDigit() || ch in "-._~" -> append(ch)
                ch == ' ' -> append('+')
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) {
                        append('%')
                        append(hex[(b.toInt() shr 4) and 0xF])
                        append(hex[b.toInt() and 0xF])
                    }
                }
            }
        }
    }

private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

private val hex = "0123456789ABCDEF".toCharArray()
