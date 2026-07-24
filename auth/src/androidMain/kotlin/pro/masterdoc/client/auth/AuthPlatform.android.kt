package pro.masterdoc.client.auth

actual object BrowserNav {
    actual fun currentPath(): String = "/"

    actual fun currentSearch(): String = ""

    actual fun currentHash(): String = ""

    actual fun setHash(hash: String) = Unit

    actual fun navigateTo(url: String) = Unit

    actual fun replaceTo(url: String) = Unit
}

actual fun createDefaultTokenStore(): TokenStore = InMemoryTokenStore()

actual fun createDefaultPkceSessionStore(): PkceSessionStore = InMemoryPkceSessionStore()

actual fun createDefaultGatewayHttpClient(): GatewayHttpClient =
    object : GatewayHttpClient {
        override suspend fun get(
            url: String,
            headers: Map<String, String>,
        ): GatewayHttpResponse = GatewayHttpResponse(501, "HTTP not wired on Android auth actual")

        override suspend fun postForm(
            url: String,
            body: String,
            headers: Map<String, String>,
        ): GatewayHttpResponse = GatewayHttpResponse(501, "HTTP not wired on Android auth actual")

        override suspend fun put(
            url: String,
            body: String,
            headers: Map<String, String>,
        ): GatewayHttpResponse = GatewayHttpResponse(501, "HTTP not wired on Android auth actual")

        override suspend fun postBytes(
            url: String,
            body: ByteArray,
            headers: Map<String, String>,
        ): GatewayHttpResponse = GatewayHttpResponse(501, "HTTP not wired on Android auth actual")

        override suspend fun delete(
            url: String,
            headers: Map<String, String>,
        ): GatewayHttpResponse = GatewayHttpResponse(501, "HTTP not wired on Android auth actual")
    }
