package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WarehousePartDto(
    val id: String,
    val name: String,
    val uom: String = "шт",
    val sku: String? = null,
    val vendorCode: String? = null,
    val unitCost: Double? = null,
    val onHand: Int? = null,
)

@Serializable
data class CreateWarehousePartRequest(
    val name: String,
    val uom: String = "шт",
    val sku: String? = null,
    val vendorCode: String? = null,
    val unitCost: Double? = null,
)

@Serializable
data class StockBalanceDto(
    val orgId: String,
    val siteId: String,
    val partId: String,
    val onHand: Int,
)

@Serializable
data class StockReceiptRequest(
    val partId: String,
    val siteId: String,
    val qty: Int,
)

@Serializable
data class StockIssueRequest(
    val partId: String,
    val siteId: String,
    val qty: Int,
    val workOrderId: String,
    val assetId: String,
)

@Serializable
data class StockMovementDto(
    val id: String,
    val partId: String,
    val siteId: String,
    val type: String,
    val qty: Int,
)

@Serializable
data class AssetPartDto(
    val partId: String,
    val qtyHint: Int? = null,
    val critical: Boolean = false,
)

@Serializable
data class AssetPartsUpdateRequest(val items: List<AssetPartDto>)

@Serializable
data class ReplenishAdviceDto(
    val textRu: String,
    val partIds: List<String> = emptyList(),
    val computedAt: String? = null,
)

class WarehouseRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private fun base() = config.gatewayBaseUrl.trimEnd('/') + "/warehouse"

    private suspend fun authHeaders(): Map<String, String> {
        val token = tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Нет сессии")
        return mapOf("Authorization" to "Bearer $token")
    }

    private fun requireSuccess(response: GatewayHttpResponse, fallback: String): String {
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body.ifBlank { fallback })
        return response.body
    }

    suspend fun listParts(): List<WarehousePartDto> =
        json.decodeFromString(requireSuccess(http.get("${base()}/parts", authHeaders()), "list parts failed"))

    suspend fun createPart(request: CreateWarehousePartRequest): WarehousePartDto =
        json.decodeFromString(
            requireSuccess(
                http.postForm(
                    "${base()}/parts",
                    json.encodeToString(CreateWarehousePartRequest.serializer(), request),
                    authHeaders() + ("Content-Type" to "application/json"),
                ),
                "create part failed",
            ),
        )

    suspend fun stock(siteId: String? = null): List<StockBalanceDto> {
        val suffix = siteId?.let { "?siteId=${encodeQuery(it)}" }.orEmpty()
        return json.decodeFromString(requireSuccess(http.get("${base()}/stock$suffix", authHeaders()), "list stock failed"))
    }

    suspend fun receipt(request: StockReceiptRequest): StockMovementDto =
        movement("/stock/receipt", StockReceiptRequest.serializer(), request, "receipt failed")

    suspend fun issue(request: StockIssueRequest): StockMovementDto =
        movement("/stock/issue", StockIssueRequest.serializer(), request, "issue failed")

    suspend fun assetParts(assetId: String): List<AssetPartDto> =
        json.decodeFromString(
            requireSuccess(http.get("${base()}/assets/${encodeQuery(assetId)}/parts", authHeaders()), "list asset parts failed"),
        )

    suspend fun replaceAssetParts(assetId: String, parts: List<AssetPartDto>): List<AssetPartDto> {
        val response =
            http.put(
                "${base()}/assets/${encodeQuery(assetId)}/parts",
                json.encodeToString(AssetPartsUpdateRequest(items = parts)),
                authHeaders() + ("Content-Type" to "application/json"),
            )
        return json.decodeFromString(requireSuccess(response, "update asset parts failed"))
    }

    suspend fun latestAdvice(): ReplenishAdviceDto =
        json.decodeFromString(requireSuccess(http.get("${base()}/advice/latest", authHeaders()), "advice failed"))

    private suspend fun <T> movement(
        path: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        request: T,
        fallback: String,
    ): StockMovementDto =
        json.decodeFromString(
            requireSuccess(
                http.postForm(
                    "${base()}$path",
                    json.encodeToString(serializer, request),
                    authHeaders() + ("Content-Type" to "application/json"),
                ),
                fallback,
            ),
        )
}
