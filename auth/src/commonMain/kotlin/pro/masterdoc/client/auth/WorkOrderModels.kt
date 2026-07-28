package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class WorkOrderDto(
    val id: String,
    val orgId: String,
    val type: String,
    val status: String,
    val title: String,
    val assetId: String,
    val siteId: String,
    val dueAt: String,
    val durationHours: Int = 8,
    val assigneeId: String? = null,
    val maintenanceMapId: String? = null,
    val maintenanceMapItemId: String? = null,
    val source: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BoardWeekDto(
    val weekStart: String,
    val items: List<WorkOrderDto>,
)

@Serializable
data class BoardResponseDto(val weeks: List<BoardWeekDto>)

@Serializable
data class CreateWorkOrderRequest(
    val type: String,
    val title: String,
    val assetId: String,
    val siteId: String,
    val dueAt: String,
    val durationHours: Int? = null,
    val maintenanceMapId: String? = null,
    val maintenanceMapItemId: String? = null,
    val source: String = "manual",
)

class WorkOrdersRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private fun base() = config.gatewayBaseUrl.trimEnd('/')

    private suspend fun bearer(): String =
        tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Нет сессии")

    private suspend fun authJsonHeaders(): Map<String, String> =
        mapOf(
            "Authorization" to "Bearer ${bearer()}",
            "Content-Type" to "application/json",
        )

    suspend fun getBoard(weekStart: String? = null, weeks: Int = 4): BoardResponseDto {
        val q =
            buildString {
                append("weeks=$weeks")
                if (!weekStart.isNullOrBlank()) append("&weekStart=$weekStart")
            }
        val response =
            http.get(
                url = "${base()}/work-orders/board?$q",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "board failed" })
        }
        return json.decodeFromString(BoardResponseDto.serializer(), response.body)
    }

    suspend fun list(assigneeId: String? = null): List<WorkOrderDto> {
        val query = assigneeId?.takeIf { it.isNotBlank() }?.let { "?assigneeId=$it" }.orEmpty()
        val response =
            http.get(
                url = "${base()}/work-orders$query",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "list work orders failed" })
        }
        return json.decodeFromString(response.body)
    }

    suspend fun get(id: String): WorkOrderDto {
        val response =
            http.get(
                url = "${base()}/work-orders/$id",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "get work order failed" })
        }
        return json.decodeFromString(WorkOrderDto.serializer(), response.body)
    }

    suspend fun create(request: CreateWorkOrderRequest): WorkOrderDto {
        val response =
            http.postForm(
                url = "${base()}/work-orders",
                body = json.encodeToString(CreateWorkOrderRequest.serializer(), request),
                headers = authJsonHeaders(),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "create work order failed" })
        }
        return json.decodeFromString(WorkOrderDto.serializer(), response.body)
    }

    /**
     * @param clearAssignee when true, sends `"assigneeId": null` even if [assigneeId] is null.
     */
    suspend fun patch(
        id: String,
        status: String? = null,
        title: String? = null,
        dueAt: String? = null,
        durationHours: Int? = null,
        assigneeId: String? = null,
        clearAssignee: Boolean = false,
    ): WorkOrderDto {
        val body =
            buildJsonObject {
                if (status != null) put("status", JsonPrimitive(status))
                if (title != null) put("title", JsonPrimitive(title))
                if (dueAt != null) put("dueAt", JsonPrimitive(dueAt))
                if (durationHours != null) put("durationHours", JsonPrimitive(durationHours))
                when {
                    clearAssignee -> put("assigneeId", JsonNull)
                    assigneeId != null -> put("assigneeId", JsonPrimitive(assigneeId))
                }
            }
        val response =
            http.patch(
                url = "${base()}/work-orders/$id",
                body = body.toString(),
                headers = authJsonHeaders(),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "patch work order failed" })
        }
        return json.decodeFromString(WorkOrderDto.serializer(), response.body)
    }
}

fun workOrderTypeLabelRu(type: String): String =
    when (type) {
        "ppr" -> "ППР"
        "emergency" -> "Аварийная"
        else -> type
    }

fun workOrderStatusLabelRu(status: String): String =
    when (status) {
        "new" -> "Новая"
        "in_progress" -> "В работе"
        "closed" -> "Закрыта"
        else -> status
    }
