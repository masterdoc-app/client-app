package pro.masterdoc.client.auth

private var pendingDeepLink: String? = null

actual object BrowserNav {
    actual fun currentPath(): String = "/"

    actual fun currentSearch(): String = ""

    actual fun currentHash(): String = ""

    actual fun setHash(hash: String) = Unit

    actual fun savePendingDeepLink(hash: String) {
        pendingDeepLink = hash
    }

    actual fun consumePendingDeepLink(): String? =
        pendingDeepLink.also { pendingDeepLink = null }

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
