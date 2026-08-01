package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class GeocodeRepositoryTest {
    @Test
    fun suggestEncodesQueryAndDecodesItems() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, _ ->
                    assertEquals("GET", method)
                    assertEquals(
                        "https://api.test/geocode/suggest?q=%D0%A2%D0%B2%D0%B5%D1%80%D1%81%D0%BA%D0%B0%D1%8F+1%2F2&limit=3",
                        url,
                    )
                    assertEquals("Bearer at", headers["Authorization"])
                    GatewayHttpResponse(
                        200,
                        """{"items":[{"label":"Россия, Москва, улица Тверская, 1","lat":55.757,"lon":37.615}]}""",
                    )
                }
            val repository =
                GeocodeRepository(
                    config =
                        AuthConfig(
                            gatewayBaseUrl = "https://api.test/",
                            clientId = "c",
                            redirectUri = "https://app.test/callback",
                        ),
                    http = http,
                    tokenStore = tokens,
                )

            val items = repository.suggest("Тверская 1/2", limit = 3)

            assertEquals("Россия, Москва, улица Тверская, 1", items.single().label)
            assertEquals(55.757, items.single().lat)
            assertEquals(37.615, items.single().lon)
        }
}
