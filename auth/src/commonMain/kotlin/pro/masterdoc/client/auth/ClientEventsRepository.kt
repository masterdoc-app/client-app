package pro.masterdoc.client.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ClientEventRequestDto(
    val action: String,
    val path: String? = null,
    val props: Map<String, String> = emptyMap(),
)

/**
 * Fire-and-forget UI events → gateway `POST /client-events` → black-box.
 * Never throws to callers; missing token / HTTP errors are swallowed.
 */
class ClientEventsRepository(
    private val config: AuthConfig,
    private val http: GatewayHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json = AuthRepository.defaultJson,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    fun trackAsync(
        action: String,
        path: String? = null,
        props: Map<String, String> = emptyMap(),
    ) {
        scope.launch {
            runCatching {
                val access = tokenStore.read()?.accessToken ?: return@launch
                http.postForm(
                    url = "${config.gatewayBaseUrl.trimEnd('/')}/client-events",
                    body =
                        json.encodeToString(
                            ClientEventRequestDto.serializer(),
                            ClientEventRequestDto(action = action, path = path, props = props),
                        ),
                    headers =
                        mapOf(
                            "Authorization" to "Bearer $access",
                            "Content-Type" to "application/json",
                        ),
                )
            }
        }
    }
}
