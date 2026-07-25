package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class ClientEventsRepositoryTest {
    @Test
    fun trackAsync_postsClientEventsWithBearer() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var posted = false
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("POST", method)
                    assertTrue(url.endsWith("/client-events"))
                    assertEquals("Bearer at", headers["Authorization"])
                    assertTrue(body!!.contains("\"action\":\"ui.shell.nav.select\""))
                    assertTrue(body.contains("\"path\":\"MainShell\""))
                    assertTrue(body.contains("\"destination\":\"users\""))
                    posted = true
                    GatewayHttpResponse(202, "")
                }
            val repo =
                ClientEventsRepository(
                    config = AuthConfig(clientId = "web", gatewayBaseUrl = "https://api.test"),
                    http = http,
                    tokenStore = tokens,
                )
            repo.trackAsync(
                action = "ui.shell.nav.select",
                path = "MainShell",
                props = mapOf("destination" to "users"),
            )
            delay(200)
            assertTrue(posted)
        }

    @Test
    fun trackAsync_swallowsHttpErrors() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { _, _, _, _ ->
                    GatewayHttpResponse(500, "fail")
                }
            val repo =
                ClientEventsRepository(
                    config = AuthConfig(clientId = "web", gatewayBaseUrl = "https://api.test"),
                    http = http,
                    tokenStore = tokens,
                )
            // Must not throw to caller
            repo.trackAsync(action = "ui.shell.open", path = "Root")
            delay(200)
        }

    @Test
    fun trackAsync_swallowsMissingToken() =
        runBlocking {
            var called = false
            val http =
                RecordingGatewayHttpClient { _, _, _, _ ->
                    called = true
                    GatewayHttpResponse(200, "")
                }
            val repo =
                ClientEventsRepository(
                    config = AuthConfig(clientId = "web", gatewayBaseUrl = "https://api.test"),
                    http = http,
                    tokenStore = InMemoryTokenStore(),
                )
            repo.trackAsync(action = "ui.shell.open", path = "Root")
            delay(200)
            assertEquals(false, called)
        }
}
