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
data class SiteDto(
    val id: String,
    val orgId: String,
    val name: String,
    val address: String? = null,
)

@Serializable
data class SiteListDto(val items: List<SiteDto>)

@Serializable
data class CreateSiteRequest(
    val name: String,
    val address: String? = null,
    val id: String? = null,
)

@Serializable
data class UpdateSiteRequest(
    val name: String? = null,
    val address: String? = null,
)

@Serializable
data class MoveAssetRequest(val siteId: String)

@Serializable
data class UpdateAssetRequest(
    val name: String? = null,
    val inventoryNo: String? = null,
    val category: String? = null,
    val description: String? = null,
    val documentIds: List<String>? = null,
)

@Serializable
data class AuditEventDto(
    val id: String,
    val orgId: String,
    val userId: String,
    val at: String,
    val method: String,
    val path: String,
    val status: Int,
    val action: String? = null,
    val requestSummary: String? = null,
    val responseSummary: String? = null,
)

@Serializable
data class AuditEventListDto(val items: List<AuditEventDto>)

@Serializable
data class DocumentMetaDto(
    val id: String,
    val orgId: String,
    val filename: String,
    val contentType: String,
    val storageKey: String,
    val sha256: String,
    val uploadedBy: String,
) {
    fun storageFolder(): String = storageKey.substringBeforeLast('/', missingDelimiterValue = orgId)
}

@Serializable
data class DocumentListDto(val items: List<DocumentMetaDto> = emptyList())

@Serializable
data class TechnologistJobDto(
    val id: String,
    val orgId: String,
    val documentId: String,
    val siteId: String,
    val assetId: String? = null,
    val status: String,
    val draftAssetId: String? = null,
    val draftMapId: String? = null,
    val error: String? = null,
)

@Serializable
data class StartTechnologistJobRequest(
    val documentId: String,
    val siteId: String? = null,
    val assetId: String,
)

@Serializable
data class DocumentValidationRequest(
    val documentId: String,
    val siteId: String,
    val assetId: String? = null,
)

@Serializable
data class DocumentValidationResponse(
    val status: String,
    val explanation: String? = null,
    val obsoleteDocumentIds: List<String> = emptyList(),
    val draftAssetId: String? = null,
)

@Serializable
data class ConfirmReplaceRequest(
    val documentId: String,
    val obsoleteDocumentIds: List<String>,
)

@Serializable
data class EquipmentCardRequest(
    val documentId: String,
    val siteId: String,
    val assetId: String,
)

@Serializable
data class EquipmentCardResponse(
    val draftAssetId: String,
)

@Serializable
data class MentorHistoryTurn(
    val role: String,
    val content: String,
)

@Serializable
data class MentorRequest(
    val workOrderId: String,
    val message: String,
    val history: List<MentorHistoryTurn> = emptyList(),
)

@Serializable
data class MentorResponse(
    val reply: String,
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

    suspend fun listSites(): SiteListDto {
        val response =
            http.get(
                url = "${base()}/sites",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(SiteListDto.serializer(), response.body)
    }

    suspend fun createSite(request: CreateSiteRequest): SiteDto {
        val response =
            http.postForm(
                url = "${base()}/sites",
                body = json.encodeToString(CreateSiteRequest.serializer(), request),
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(SiteDto.serializer(), response.body)
    }

    suspend fun updateSite(id: String, request: UpdateSiteRequest): SiteDto {
        val response =
            http.put(
                url = "${base()}/sites/$id",
                body = json.encodeToString(UpdateSiteRequest.serializer(), request),
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(SiteDto.serializer(), response.body)
    }

    suspend fun deleteSite(id: String) {
        val response =
            http.delete(
                url = "${base()}/sites/$id",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
    }

    suspend fun listAssets(): AssetListDto {
        val response =
            http.get(
                url = "${base()}/assets",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(AssetListDto.serializer(), response.body)
    }

    suspend fun moveAsset(id: String, siteId: String): AssetDto {
        val response =
            http.postForm(
                url = "${base()}/assets/$id/move",
                body = json.encodeToString(MoveAssetRequest.serializer(), MoveAssetRequest(siteId)),
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(AssetDto.serializer(), response.body)
    }

    suspend fun updateAsset(id: String, request: UpdateAssetRequest): AssetDto {
        val response =
            http.patch(
                url = "${base()}/assets/$id",
                body = json.encodeToString(UpdateAssetRequest.serializer(), request),
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(AssetDto.serializer(), response.body)
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

    suspend fun deleteAsset(id: String) {
        val response =
            http.delete(
                url = "${base()}/assets/$id",
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

    suspend fun getDocument(id: String): DocumentMetaDto {
        val response =
            http.get(
                url = "${base()}/documents/$id",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(DocumentMetaDto.serializer(), response.body)
    }

    suspend fun listDocuments(folder: String? = null): DocumentListDto {
        val q = folder?.let { "?folder=${it.trimEnd('/')}" } ?: ""
        val response =
            http.get(
                url = "${base()}/documents$q",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(DocumentListDto.serializer(), response.body)
    }

    fun documentContentUrl(id: String): String = "${base()}/documents/$id/content"

    suspend fun accessToken(): String = bearer()

    suspend fun startTechnologist(
        documentId: String,
        siteId: String = "default-site",
        assetId: String,
    ): TechnologistJobDto {
        val body =
            json.encodeToString(
                StartTechnologistJobRequest.serializer(),
                StartTechnologistJobRequest(documentId = documentId, siteId = siteId, assetId = assetId),
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

    suspend fun validateDocument(
        documentId: String,
        siteId: String,
        assetId: String? = null,
    ): DocumentValidationResponse {
        val body =
            json.encodeToString(
                DocumentValidationRequest.serializer(),
                DocumentValidationRequest(documentId = documentId, siteId = siteId, assetId = assetId),
            )
        val response =
            http.postForm(
                url = "${base()}/ai/document-validator",
                body = body,
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(DocumentValidationResponse.serializer(), response.body)
    }

    suspend fun confirmReplaceDocuments(documentId: String, obsoleteDocumentIds: List<String>) {
        val body =
            json.encodeToString(
                ConfirmReplaceRequest.serializer(),
                ConfirmReplaceRequest(documentId = documentId, obsoleteDocumentIds = obsoleteDocumentIds),
            )
        val response =
            http.postForm(
                url = "${base()}/ai/document-validator/confirm-replace",
                body = body,
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
    }

    suspend fun createEquipmentCard(documentId: String, siteId: String, assetId: String): EquipmentCardResponse {
        val body =
            json.encodeToString(
                EquipmentCardRequest.serializer(),
                EquipmentCardRequest(documentId = documentId, siteId = siteId, assetId = assetId),
            )
        val response =
            http.postForm(
                url = "${base()}/ai/equipment-card",
                body = body,
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(EquipmentCardResponse.serializer(), response.body)
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

    suspend fun askMentor(
        workOrderId: String,
        message: String,
        history: List<MentorHistoryTurn> = emptyList(),
    ): MentorResponse {
        val body =
            json.encodeToString(
                MentorRequest.serializer(),
                MentorRequest(workOrderId = workOrderId, message = message, history = history),
            )
        val response =
            http.postForm(
                url = "${base()}/ai/mentor",
                body = body,
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) throw GatewayHttpException(response.status, response.body)
        return json.decodeFromString(MentorResponse.serializer(), response.body)
    }
}
