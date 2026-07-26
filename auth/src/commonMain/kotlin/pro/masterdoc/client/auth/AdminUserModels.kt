package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class InviteUserRequest(
    val email: String,
    val givenName: String,
    val familyName: String,
    val features: List<String>,
)

@Serializable
data class AdminUser(
    val id: String,
    val email: String,
    val givenName: String,
    val familyName: String,
    val features: List<String>,
    val state: String,
    val inviteSent: Boolean? = null,
)

@Serializable
data class AdminUserList(
    val items: List<AdminUser>,
    val total: Int,
)

@Serializable
data class FeatureDefinitionDto(
    val id: String,
    val titleRu: String,
)

@Serializable
data class FeaturesCatalog(
    val items: List<FeatureDefinitionDto>,
)

class AdminUsersRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = AuthRepository.defaultJson,
) {
    suspend fun listFeatures(): FeaturesCatalog {
        val access =
            tokenStore.read()?.accessToken
                ?: throw GatewayHttpException(401, "Not authenticated")
        val response =
            http.get(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/features",
                headers = mapOf("Authorization" to "Bearer $access"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "GET /features failed: ${response.body}")
        }
        return json.decodeFromString(FeaturesCatalog.serializer(), response.body)
    }

    suspend fun listUsers(
        limit: Int = 50,
        offset: Int = 0,
    ): AdminUserList {
        val access =
            tokenStore.read()?.accessToken
                ?: throw GatewayHttpException(401, "Not authenticated")
        val response =
            http.get(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/admin/users?limit=$limit&offset=$offset",
                headers = mapOf("Authorization" to "Bearer $access"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "GET /admin/users failed: ${response.body}")
        }
        return json.decodeFromString(AdminUserList.serializer(), response.body)
    }

    suspend fun inviteUser(request: InviteUserRequest): AdminUser {
        val access =
            tokenStore.read()?.accessToken
                ?: throw GatewayHttpException(401, "Not authenticated")
        val response =
            http.postForm(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/admin/users/invites",
                body = json.encodeToString(InviteUserRequest.serializer(), request),
                headers =
                    mapOf(
                        "Authorization" to "Bearer $access",
                        "Content-Type" to "application/json",
                    ),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "POST /admin/users/invites failed: ${response.body}")
        }
        return json.decodeFromString(AdminUser.serializer(), response.body)
    }

    suspend fun deleteUser(userId: String) {
        val access =
            tokenStore.read()?.accessToken
                ?: throw GatewayHttpException(401, "Not authenticated")
        val response =
            http.delete(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/admin/users/$userId",
                headers = mapOf("Authorization" to "Bearer $access"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "DELETE /admin/users/$userId failed: ${response.body}")
        }
    }

    suspend fun listAudit(
        limit: Int = 30,
        offset: Int = 0,
        userId: String? = null,
    ): AuditEventListDto {
        val access =
            tokenStore.read()?.accessToken
                ?: throw GatewayHttpException(401, "Not authenticated")
        val query =
            buildString {
                append("limit=").append(limit)
                append("&offset=").append(offset.coerceAtLeast(0))
                if (!userId.isNullOrBlank()) append("&userId=").append(userId)
            }
        val response =
            http.get(
                url = "${config.gatewayBaseUrl.trimEnd('/')}/admin/audit?$query",
                headers = mapOf("Authorization" to "Bearer $access"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, "GET /admin/audit failed: ${response.body}")
        }
        return json.decodeFromString(AuditEventListDto.serializer(), response.body)
    }
}
