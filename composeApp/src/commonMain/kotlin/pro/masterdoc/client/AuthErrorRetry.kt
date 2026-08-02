package pro.masterdoc.client

import pro.masterdoc.client.navigation.parseAppDeepLink
import pro.masterdoc.client.navigation.toHash

enum class AuthErrorRetryMode {
    /** Re-run bootstrap on the current URL (e.g. retry /me). */
    RetryBootstrap,

    /** Leave /auth/callback and start a clean login (do not re-exchange code). */
    NavigateHome,
}

fun authErrorRetryMode(path: String): AuthErrorRetryMode =
    if (path.contains("/auth/callback")) {
        AuthErrorRetryMode.NavigateHome
    } else {
        AuthErrorRetryMode.RetryBootstrap
    }

fun pendingDeepLinkHash(hash: String): String? =
    parseAppDeepLink(hash)?.toHash()

fun postLoginLocation(pendingHash: String?): String =
    pendingHash
        ?.let(::parseAppDeepLink)
        ?.toHash()
        ?.let { "/$it" }
        ?: "/"
