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
    val createdBy: String? = null,
    val description: String? = null,
    val maintenanceMapId: String? = null,
    val maintenanceMapItemId: String? = null,
    val source: String,
    val createdAt: String,
    val updatedAt: String,
    val startedAt: String? = null,
    val closedAt: String? = null,
    val attachmentIds: List<String> = emptyList(),
)

@Serializable
data class DowntimeIntervalDto(
    val assetId: String,
    val workOrderId: String,
    val title: String,
    val startedAt: String,
    val closedAt: String? = null,
    val status: String,
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
    val description: String? = null,
    val maintenanceMapId: String? = null,
    val maintenanceMapItemId: String? = null,
    val source: String = "manual",
    val attachmentIds: List<String>? = null,
)

@Serializable
data class EngineerLocationSnapshot(
    val lat: Double,
    val lon: Double,
    val accuracyM: Double? = null,
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

    suspend fun list(
        assigneeId: String? = null,
        createdBy: String? = null,
    ): List<WorkOrderDto> {
        val query = workOrdersListQuery(assigneeId, createdBy)
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

    suspend fun equipmentDowntime(from: String, to: String): List<DowntimeIntervalDto> {
        val response =
            http.get(
                url = "${base()}/reports/equipment-downtime?from=$from&to=$to",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "equipment downtime report failed" })
        }
        return json.decodeFromString(response.body)
    }

    suspend fun managerKpis(from: String, to: String): ManagerKpis {
        val response =
            http.get(
                url = "${base()}/reports/manager-kpis?from=$from&to=$to",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "manager KPI report failed" })
        }
        return json.decodeFromString(ManagerKpis.serializer(), response.body)
    }

    suspend fun kpiTrends(from: String, to: String): KpiTrendsReport =
        getReport(
            path = "kpi-trends",
            from = from,
            to = to,
            serializer = KpiTrendsReport.serializer(),
            failureMessage = "KPI trends report failed",
        )

    suspend fun reactiveCompletion(from: String, to: String): ReactiveCompletionReport =
        getReport(
            path = "reactive-completion",
            from = from,
            to = to,
            serializer = ReactiveCompletionReport.serializer(),
            failureMessage = "reactive completion report failed",
        )

    suspend fun engineerWorkload(from: String, to: String): EngineerWorkloadReport =
        getReport(
            path = "engineer-workload",
            from = from,
            to = to,
            serializer = EngineerWorkloadReport.serializer(),
            failureMessage = "engineer workload report failed",
        )

    suspend fun failureFrequency(from: String, to: String): FailureFrequencyReport =
        getReport(
            path = "failure-frequency",
            from = from,
            to = to,
            serializer = FailureFrequencyReport.serializer(),
            failureMessage = "failure frequency report failed",
        )

    suspend fun equipmentWorkOrders(assetId: String, from: String, to: String): List<WorkOrderDto> {
        val response =
            http.get(
                url = "${base()}/reports/equipment-work-orders?assetId=$assetId&from=$from&to=$to",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(
                response.status,
                response.body.ifBlank { "equipment work orders report failed" },
            )
        }
        return json.decodeFromString(response.body)
    }

    private suspend fun <T> getReport(
        path: String,
        from: String,
        to: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        failureMessage: String,
    ): T {
        val response =
            http.get(
                url = "${base()}/reports/$path?from=$from&to=$to",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { failureMessage })
        }
        return json.decodeFromString(serializer, response.body)
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

    suspend fun attach(orderId: String, attachmentIds: List<String>): WorkOrderDto {
        val body =
            buildJsonObject {
                put(
                    "attachmentIds",
                    kotlinx.serialization.json.buildJsonArray {
                        attachmentIds.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }
        val response =
            http.postForm(
                url = "${base()}/work-orders/$orderId/attachments",
                body = body.toString(),
                headers = authJsonHeaders(),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "attach work order attachments failed" })
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
        location: EngineerLocationSnapshot? = null,
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
                location?.let { snapshot ->
                    put(
                        "location",
                        buildJsonObject {
                            put("lat", JsonPrimitive(snapshot.lat))
                            put("lon", JsonPrimitive(snapshot.lon))
                            snapshot.accuracyM?.let { put("accuracyM", JsonPrimitive(it)) }
                        },
                    )
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

fun workOrdersListQuery(
    assigneeId: String?,
    createdBy: String?,
): String {
    val params =
        buildList {
            assigneeId?.takeIf { it.isNotBlank() }?.let { add("assigneeId=$it") }
            createdBy?.takeIf { it.isNotBlank() }?.let { add("createdBy=$it") }
        }
    return if (params.isEmpty()) "" else params.joinToString("&", prefix = "?")
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
