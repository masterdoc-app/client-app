package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AiMessageDto(
    val id: String,
    val orgId: String,
    val kind: String,
    val workOrderId: String,
    val siteId: String,
    val engineerId: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val distanceM: Double? = null,
    val radiusM: Int? = null,
    val engineerLat: Double? = null,
    val engineerLon: Double? = null,
    val siteLat: Double? = null,
    val siteLon: Double? = null,
    val accuracyM: Double? = null,
)

@Serializable
data class AiMessageListDto(val items: List<AiMessageDto> = emptyList())

class AiMessagesRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = AuthRepository.defaultJson,
) {
    private fun base() = config.gatewayBaseUrl.trimEnd('/')

    private suspend fun bearer(): String =
        tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Нет сессии")

    suspend fun list(limit: Int = 30, offset: Int = 0): AiMessageListDto {
        val response =
            http.get(
                url = "${base()}/ai-messages?limit=$limit&offset=$offset",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "Не удалось загрузить сообщения ИИ" })
        }
        return json.decodeFromString(AiMessageListDto.serializer(), response.body)
    }
}
