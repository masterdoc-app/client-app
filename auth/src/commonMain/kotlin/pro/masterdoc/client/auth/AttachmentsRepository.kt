package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AttachmentMetaDto(
    val id: String,
    val orgId: String,
    val contentType: String,
    val sizeBytes: Long,
    val createdAt: String,
)

class AttachmentsRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private fun base() = config.gatewayBaseUrl.trimEnd('/')

    private suspend fun bearer(): String =
        tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Нет сессии")

    suspend fun upload(
        bytes: ByteArray,
        filename: String,
        contentType: String,
    ): AttachmentMetaDto {
        require(bytes.isNotEmpty()) { "Attachment file is empty" }
        val part =
            MultipartBody.filePart(
                fieldName = "file",
                filename = filename,
                fileContentType = contentType,
                bytes = bytes,
            )
        val response =
            http.postBytes(
                url = "${base()}/attachments",
                body = part.body,
                headers =
                    mapOf(
                        "Authorization" to "Bearer ${bearer()}",
                        "Content-Type" to part.contentType,
                    ),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "upload attachment failed" })
        }
        return json.decodeFromString(AttachmentMetaDto.serializer(), response.body)
    }

    fun contentUrl(id: String): String = "${base()}/attachments/$id/content"
}
