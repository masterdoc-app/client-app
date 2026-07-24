package pro.masterdoc.client.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Profile claims from the OIDC id_token.
 *
 * Zitadel puts email/name in the id_token when `id_token_userinfo_assertion` is on,
 * but not in the access token that gateway `/me` reads — so the client fills gaps.
 */
data class IdTokenProfile(
    val email: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
) {
    companion object {
        private val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        fun parse(idToken: String?): IdTokenProfile? {
            if (idToken.isNullOrBlank()) return null
            val parts = idToken.split('.')
            if (parts.size < 2) return null
            return runCatching {
                val payload = Base64Url.decode(parts[1]).decodeToString()
                val dto = json.decodeFromString(IdTokenPayloadDto.serializer(), payload)
                val email =
                    dto.email?.trim()?.takeIf { it.isNotEmpty() }
                        ?: dto.preferredUsername?.trim()?.takeIf { it.isNotEmpty() && '@' in it }
                IdTokenProfile(
                    email = email,
                    givenName = dto.givenName?.trim()?.takeIf { it.isNotEmpty() },
                    familyName = dto.familyName?.trim()?.takeIf { it.isNotEmpty() },
                )
            }.getOrNull()
        }
    }
}

@Serializable
private data class IdTokenPayloadDto(
    val email: String? = null,
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    @SerialName("preferred_username") val preferredUsername: String? = null,
)

/** Prefer `/me` fields; fill blanks from id_token (Zitadel userinfo assertion). */
fun MeResponse.withProfileFromIdToken(idToken: String?): MeResponse {
    val profile = IdTokenProfile.parse(idToken) ?: return this
    val info = userInfo
    if (info.email != null && info.givenName != null && info.familyName != null) return this
    return copy(
        userInfo =
            info.copy(
                email = info.email ?: profile.email,
                givenName = info.givenName ?: profile.givenName,
                familyName = info.familyName ?: profile.familyName,
            ),
    )
}
