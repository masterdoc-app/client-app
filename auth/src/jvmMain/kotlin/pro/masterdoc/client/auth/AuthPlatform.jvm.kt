package pro.masterdoc.client.auth

actual object BrowserNav {
    actual fun currentPath(): String = "/"

    actual fun currentSearch(): String = ""

    actual fun navigateTo(url: String) {
        // no-op on JVM/Android unit hosts
    }

    actual fun replaceTo(url: String) {
        // no-op on JVM/Android unit hosts
    }
}

actual fun createDefaultTokenStore(): TokenStore = InMemoryTokenStore()

actual fun createDefaultPkceSessionStore(): PkceSessionStore = InMemoryPkceSessionStore()

actual fun createDefaultGatewayHttpClient(): GatewayHttpClient = JvmGatewayHttpClient()
