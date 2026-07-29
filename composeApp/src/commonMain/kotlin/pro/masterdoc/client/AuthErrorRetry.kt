package pro.masterdoc.client

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
