package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EngineerLocationDto(
    val userId: String,
    val lat: Double,
    val lon: Double,
    val accuracyM: Double? = null,
    val recordedAt: String,
    val displayName: String? = null,
)

@Serializable
data class EngineerLocationListDto(val items: List<EngineerLocationDto>)

@Serializable
data class UpdateEngineerLocationRequest(
    val lat: Double,
    val lon: Double,
    val accuracyM: Double? = null,
    val recordedAt: String? = null,
    val displayName: String? = null,
)

interface EngineerLocationsGateway {
    suspend fun putMe(body: UpdateEngineerLocationRequest): EngineerLocationDto

    suspend fun list(): List<EngineerLocationDto>

    suspend fun deleteMe()
}

class EngineerLocationsRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : EngineerLocationsGateway {
    private fun base() = config.gatewayBaseUrl.trimEnd('/')

    private suspend fun bearer(): String =
        tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Нет сессии")

    private suspend fun authJsonHeaders(): Map<String, String> =
        mapOf(
            "Authorization" to "Bearer ${bearer()}",
            "Content-Type" to "application/json",
        )

    override suspend fun putMe(body: UpdateEngineerLocationRequest): EngineerLocationDto {
        val response =
            http.put(
                url = "${base()}/engineer-locations/me",
                body = json.encodeToString(UpdateEngineerLocationRequest.serializer(), body),
                headers = authJsonHeaders(),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "update engineer location failed" })
        }
        return json.decodeFromString(EngineerLocationDto.serializer(), response.body)
    }

    override suspend fun list(): List<EngineerLocationDto> {
        val response =
            http.get(
                url = "${base()}/engineer-locations",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "list engineer locations failed" })
        }
        return json.decodeFromString(EngineerLocationListDto.serializer(), response.body).items
    }

    override suspend fun deleteMe() {
        val response =
            http.delete(
                url = "${base()}/engineer-locations/me",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "delete engineer location failed" })
        }
    }
}
