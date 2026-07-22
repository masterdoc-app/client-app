package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AssetDto(
    val id: String,
    val orgId: String,
    val siteId: String,
    val name: String,
    val inventoryNo: String? = null,
    val category: String? = null,
    val description: String? = null,
    val status: String,
    val source: String,
    val documentIds: List<String> = emptyList(),
)

@Serializable
data class AssetListDto(val items: List<AssetDto>)

@Serializable
data class DocumentMetaDto(
    val id: String,
    val orgId: String,
    val filename: String,
    val contentType: String,
    val storageKey: String,
    val sha256: String,
    val uploadedBy: String,
)

@Serializable
data class TechnologistJobDto(
    val id: String,
    val orgId: String,
    val documentId: String,
    val siteId: String,
    val status: String,
    val draftAssetId: String? = null,
    val draftMapId: String? = null,
    val error: String? = null,
)

@Serializable
data class StartTechnologistJobRequest(
    val documentId: String,
    val siteId: String? = null,
)

@Serializable
data class IntervalDto(
    val every: Int,
    val unit: String,
)

@Serializable
data class MaintenanceMapItemDto(
    val id: String,
    val title: String,
    val kind: String,
    val interval: IntervalDto,
    val criticality: String,
    val sourceRef: String? = null,
)

@Serializable
data class MaintenanceMapDto(
    val id: String,
    val assetId: String,
    val orgId: String,
    val title: String,
    val status: String,
    val source: String,
    val items: List<MaintenanceMapItemDto> = emptyList(),
)

@Serializable
data class MaintenanceMapListDto(val items: List<MaintenanceMapDto>)

class EquipmentRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = AuthRepository.defaultJson,
) {
    private fun base() = config.gatewayBaseUrl.trimEnd('/')

    private suspend fun bearer(): String =
        tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Not authenticated")

    suspend fun listAssets(): AssetListDto {
        val response =
            http.get(
                url = "${base()}/assets",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(AssetListDto.serializer(), response.body)
    }

    suspend fun confirmAsset(id: String): AssetDto {
        val response =
            http.postForm(
                url = "${base()}/assets/$id/confirm",
                body = "",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(AssetDto.serializer(), response.body)
    }

    suspend fun rejectAsset(id: String) {
        val response =
            http.postForm(
                url = "${base()}/assets/$id/reject",
                body = "",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
    }

    suspend fun listMaps(assetId: String? = null): MaintenanceMapListDto {
        val q = assetId?.let { "?assetId=$it" } ?: ""
        val response =
            http.get(
                url = "${base()}/maintenance-maps$q",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(MaintenanceMapListDto.serializer(), response.body)
    }

    suspend fun confirmMap(id: String): MaintenanceMapDto {
        val response =
            http.postForm(
                url = "${base()}/maintenance-maps/$id/confirm",
                body = "",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(MaintenanceMapDto.serializer(), response.body)
    }

    suspend fun rejectMap(id: String) {
        val response =
            http.postForm(
                url = "${base()}/maintenance-maps/$id/reject",
                body = "",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
    }

    suspend fun uploadManualPdf(
        bytes: ByteArray,
        filename: String,
    ): DocumentMetaDto {
        require(filename.endsWith(".pdf", ignoreCase = true)) { "Only PDF allowed" }
        require(bytes.isNotEmpty()) { "PDF file is empty" }
        val part =
            MultipartBody.filePart(
                fieldName = "file",
                filename = filename,
                fileContentType = "application/pdf",
                bytes = bytes,
            )
        val response =
            http.postBytes(
                url = "${base()}/documents",
                body = part.body,
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to part.contentType,
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(DocumentMetaDto.serializer(), response.body)
    }

    suspend fun startTechnologist(documentId: String, siteId: String = "default-site"): TechnologistJobDto {
        val body =
            json.encodeToString(
                StartTechnologistJobRequest.serializer(),
                StartTechnologistJobRequest(documentId = documentId, siteId = siteId),
            )
        val response =
            http.postForm(
                url = "${base()}/ai/technologist",
                body = body,
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(TechnologistJobDto.serializer(), response.body)
    }

    suspend fun getJob(id: String): TechnologistJobDto {
        val response =
            http.get(
                url = "${base()}/ai/technologist/jobs/$id",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(TechnologistJobDto.serializer(), response.body)
    }

    suspend fun confirmPackage(jobId: String) {
        val response =
            http.postForm(
                url = "${base()}/ai/technologist/jobs/$jobId/confirm-package",
                body = "",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
    }
}
