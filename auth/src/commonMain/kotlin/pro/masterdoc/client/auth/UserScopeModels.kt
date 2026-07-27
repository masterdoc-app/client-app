package pro.masterdoc.client.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserScopeDto(
    val userId: String,
    val orgId: String,
    val siteIds: List<String> = emptyList(),
    val assetIds: List<String> = emptyList(),
)

@Serializable
data class PutUserScopeRequest(
    val siteIds: List<String> = emptyList(),
    val assetIds: List<String> = emptyList(),
)

@Serializable
data class ScopeCandidatesResponse(
    val userIds: List<String> = emptyList(),
)

class UserScopesRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = AuthRepository.defaultJson,
) {
    private fun base() = config.gatewayBaseUrl.trimEnd('/')

    private suspend fun bearer(): String =
        tokenStore.read()?.accessToken ?: throw GatewayHttpException(401, "Not authenticated")

    private suspend fun authJsonHeaders(): Map<String, String> =
        mapOf(
            "Authorization" to "Bearer ${bearer()}",
            "Content-Type" to "application/json",
        )

    suspend fun get(userId: String): UserScopeDto {
        val response =
            http.get(
                url = "${base()}/user-scopes/$userId",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "get user scope failed" })
        }
        return json.decodeFromString(UserScopeDto.serializer(), response.body)
    }

    suspend fun put(userId: String, request: PutUserScopeRequest): UserScopeDto {
        val response =
            http.put(
                url = "${base()}/user-scopes/$userId",
                body = json.encodeToString(PutUserScopeRequest.serializer(), request),
                headers = authJsonHeaders(),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "put user scope failed" })
        }
        return json.decodeFromString(UserScopeDto.serializer(), response.body)
    }

    suspend fun getCandidates(assetId: String): List<String> {
        val response =
            http.get(
                url = "${base()}/user-scopes/candidates/$assetId",
                headers = mapOf("Authorization" to "Bearer ${bearer()}"),
            )
        if (!response.isSuccessful) {
            throw GatewayHttpException(response.status, response.body.ifBlank { "get scope candidates failed" })
        }
        return json.decodeFromString(ScopeCandidatesResponse.serializer(), response.body).userIds
    }
}
