package pro.masterdoc.client.auth

/**
 * Auth orchestration: login redirect, callback exchange, load /me.
 * Does not interpret IdP grants — callers use [MeResponse.features].
 */
class AuthCoordinator(
    private val authRepository: AuthRepository,
    private val meRepository: MeRepository,
) {
    fun hasSession(): Boolean = authRepository.currentTokens() != null

    suspend fun startLogin(): String = authRepository.buildAuthorizeUrl()

    suspend fun completeCallback(
        code: String,
        state: String?,
    ): MeResponse {
        authRepository.exchangeCode(code = code, returnedState = state)
        println("[auth] token exchange ok — loading /me")
        return meRepository.getMe()
    }

    suspend fun loadMe(): MeResponse {
        println("[auth] session present — loading /me")
        return meRepository.getMe()
    }

    fun logout() {
        authRepository.logout()
    }

    /** Clear local session and return authorize URL with prompt=login (navigate there). */
    suspend fun logoutRedirectUrl(): String = authRepository.logoutRedirectUrl()
}
