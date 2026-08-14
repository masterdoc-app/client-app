package pro.masterdoc.client.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthUrlResponse(
    val authUrl: String,
)

@Serializable
data class AuthLoginRequest(
    val email: String,
    val password: String,
    @SerialName("client_id") val clientId: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("id_token") val idToken: String? = null,
    val scope: String? = null,
)

@Serializable
data class MeResponse(
    val userInfo: UserInfoDto,
    val features: List<String> = emptyList(),
)

@Serializable
data class UserInfoDto(
    val id: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val email: String? = null,
    val orgName: String? = null,
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
) {
    companion object {
        fun from(response: TokenResponse): AuthTokens =
            AuthTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                idToken = response.idToken,
            )
    }
}
