package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WorkOrderCommentDto(
    val id: String,
    val orgId: String,
    val workOrderId: String,
    val authorId: String,
    val text: String,
    val attachmentId: String? = null,
    val createdAt: String,
)

@Serializable
data class CreateWorkOrderCommentRequest(
    val workOrderId: String,
    val text: String,
    val attachmentId: String? = null,
)

class CommentsRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private fun base() = config.gatewayBaseUrl.trimEnd('/')

    private suspend fun bearer(): String =
        tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Нет сессии")

    private suspend fun authHeaders(): Map<String, String> =
        mapOf("Authorization" to "Bearer ${bearer()}")

    suspend fun list(workOrderId: String): List<WorkOrderCommentDto> {
        val response =
            http.get(
                url = "${base()}/comments?workOrderId=${encodeQuery(workOrderId)}",
                headers = authHeaders(),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "list comments failed" })
        }
        return json.decodeFromString(response.body)
    }

    suspend fun create(request: CreateWorkOrderCommentRequest): WorkOrderCommentDto {
        val response =
            http.postForm(
                url = "${base()}/comments",
                body = json.encodeToString(CreateWorkOrderCommentRequest.serializer(), request),
                headers = authHeaders() + ("Content-Type" to "application/json"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "create comment failed" })
        }
        return json.decodeFromString(WorkOrderCommentDto.serializer(), response.body)
    }
}
