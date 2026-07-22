package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class AuthRepositoryTest {
    @Test
    fun buildAuthorizeUrl_storesPkceAndIncludesChallenge() =
        runBlocking {
            val http =
                FakeGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.endsWith("/auth/url"))
                    GatewayHttpResponse(
                        200,
                        """{"authUrl":"https://auth.fixaverse.ru/oauth/v2/authorize"}""",
                    )
                }
            val tokens = InMemoryTokenStore()
            val pkce = InMemoryPkceSessionStore()
            val repo =
                AuthRepository(
                    config =
                        AuthConfig(
                            clientId = "web-client",
                            redirectUri = AuthConfig.LOCAL_REDIRECT_URI,
                        ),
                    http = http,
                    tokenStore = tokens,
                    pkceStore = pkce,
                )

            val url = repo.buildAuthorizeUrl()
            assertTrue(url.startsWith("https://auth.fixaverse.ru/oauth/v2/authorize?"))
            assertTrue(url.contains("response_type=code"))
            assertTrue(url.contains("client_id=web-client"))
            assertTrue(url.contains("code_challenge_method=S256"))
            assertTrue(url.contains("code_challenge="))
            assertTrue(pkce.readVerifier() != null)
            assertTrue(pkce.readState() != null)
        }

    @Test
    fun exchangeCode_persistsTokens() =
        runBlocking {
            val http =
                FakeGatewayHttpClient { method, url, _, body ->
                    assertEquals("POST", method)
                    assertTrue(url.endsWith("/auth/token"))
                    assertTrue(body!!.contains("grant_type=authorization_code"))
                    assertTrue(body.contains("code=abc"))
                    GatewayHttpResponse(
                        200,
                        """{"access_token":"at","refresh_token":"rt","token_type":"Bearer"}""",
                    )
                }
            val tokens = InMemoryTokenStore()
            val pkce = InMemoryPkceSessionStore()
            pkce.save(verifier = "verifier-value", state = "state-1")
            val repo =
                AuthRepository(
                    config = AuthConfig(clientId = "web-client"),
                    http = http,
                    tokenStore = tokens,
                    pkceStore = pkce,
                )

            val result = repo.exchangeCode(code = "abc", returnedState = "state-1")
            assertEquals("at", result.accessToken)
            assertEquals("rt", result.refreshToken)
            assertEquals("at", tokens.read()?.accessToken)
            assertEquals(null, pkce.readVerifier())
        }

    @Test
    fun logoutRedirectUrl_clearsSessionAndForcesLoginPrompt() =
        runBlocking {
            val http =
                FakeGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.endsWith("/auth/url"))
                    GatewayHttpResponse(
                        200,
                        """{"authUrl":"https://auth.formaverse.ru/oauth/v2/authorize"}""",
                    )
                }
            val tokens = InMemoryTokenStore()
            tokens.write(
                AuthTokens(
                    accessToken = "at",
                    refreshToken = "rt",
                    idToken = "id.jwt",
                ),
            )
            val pkce = InMemoryPkceSessionStore()
            pkce.save(verifier = "v", state = "s")
            val repo =
                AuthRepository(
                    config =
                        AuthConfig(
                            clientId = "web-client",
                            redirectUri = AuthConfig.LOCAL_REDIRECT_URI,
                        ),
                    http = http,
                    tokenStore = tokens,
                    pkceStore = pkce,
                )

            val url = repo.logoutRedirectUrl()

            assertTrue(url.startsWith("https://auth.formaverse.ru/oauth/v2/authorize?"))
            assertTrue(url.contains("prompt=login"))
            assertTrue(url.contains("client_id=web-client"))
            assertEquals(null, tokens.read())
            assertTrue(pkce.readVerifier() != null)
        }
}

private class FakeGatewayHttpClient(
    private val handler: (
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ) -> GatewayHttpResponse,
) : GatewayHttpClient {
    override suspend fun get(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("GET", url, headers, null)

    override suspend fun postForm(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("POST", url, headers, body)

    override suspend fun postBytes(
        url: String,
        body: ByteArray,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("POST", url, headers, "<bytes:${body.size}>")

    override suspend fun delete(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("DELETE", url, headers, null)
}
