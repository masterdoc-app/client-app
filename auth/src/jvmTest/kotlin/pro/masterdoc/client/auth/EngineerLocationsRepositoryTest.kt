package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class EngineerLocationsRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test/",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun putMeSendsAuthenticatedLocationPayload() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, headers, requestBody ->
                    assertEquals("PUT", method)
                    assertEquals("https://api.test/engineer-locations/me", url)
                    assertEquals("Bearer at", headers["Authorization"])
                    body = requestBody
                    GatewayHttpResponse(
                        200,
                        """{"userId":"engineer-1","lat":55.75,"lon":37.62,"accuracyM":8.5,"recordedAt":"2026-07-30T12:00:00Z","displayName":"Иван Петров"}""",
                    )
                }
            val repository = EngineerLocationsRepository(config, http, tokens)

            val location =
                repository.putMe(
                    UpdateEngineerLocationRequest(
                        lat = 55.75,
                        lon = 37.62,
                        accuracyM = 8.5,
                        recordedAt = "2026-07-30T12:00:00Z",
                        displayName = "Иван Петров",
                    ),
                )

            val json = Json.parseToJsonElement(body!!).jsonObject
            assertEquals("Иван Петров", json["displayName"]!!.toString().trim('"'))
            assertEquals("engineer-1", location.userId)
        }

    @Test
    fun listDecodesLocationItems() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals("https://api.test/engineer-locations", url)
                    GatewayHttpResponse(
                        200,
                        """{"items":[{"userId":"engineer-1","lat":55.75,"lon":37.62,"recordedAt":"2026-07-30T12:00:00Z"}]}""",
                    )
                }
            val repository = EngineerLocationsRepository(config, http, tokens)

            val locations = repository.list()

            assertEquals(listOf("engineer-1"), locations.map { it.userId })
        }

    @Test
    fun deleteMeDeletesCurrentEngineerLocation() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("DELETE", method)
                    assertEquals("https://api.test/engineer-locations/me", url)
                    GatewayHttpResponse(204, "")
                }
            val repository = EngineerLocationsRepository(config, http, tokens)

            repository.deleteMe()
        }
}
