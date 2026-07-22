package pro.masterdoc.client.auth

/**
 * Minimal HTTP surface for gateway calls (injectable for tests).
 */
interface GatewayHttpClient {
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): GatewayHttpResponse

    suspend fun postForm(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): GatewayHttpResponse

    suspend fun postBytes(
        url: String,
        body: ByteArray,
        headers: Map<String, String> = emptyMap(),
    ): GatewayHttpResponse
}

data class GatewayHttpResponse(
    val status: Int,
    val body: String,
) {
    val isSuccessful: Boolean get() = status in 200..299
}

class GatewayHttpException(
    val status: Int,
    override val message: String,
) : Exception(message)
