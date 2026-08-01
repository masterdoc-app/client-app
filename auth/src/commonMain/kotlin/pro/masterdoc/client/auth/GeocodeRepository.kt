package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GeocodeSuggestItem(
    val label: String,
    val lat: Double,
    val lon: Double,
)

@Serializable
private data class GeocodeSuggestResponse(
    val items: List<GeocodeSuggestItem>,
)

class GeocodeRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun suggest(
        query: String,
        limit: Int = DEFAULT_LIMIT,
    ): List<GeocodeSuggestItem> {
        val token = tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Нет сессии")
        val response =
            http.get(
                url =
                    "${config.gatewayBaseUrl.trimEnd('/')}/geocode/suggest" +
                        "?q=${encodeQuery(query)}&limit=$limit",
                headers = mapOf("Authorization" to "Bearer $token"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "geocode suggest failed" })
        }
        return json.decodeFromString(GeocodeSuggestResponse.serializer(), response.body).items
    }

    private companion object {
        const val DEFAULT_LIMIT = 5
    }
}
