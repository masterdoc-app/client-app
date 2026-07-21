package pro.masterdoc.client.auth

/**
 * Auth orchestration: login redirect, callback exchange, load /me.
 * Does not interpret roles — callers use [MeResponse.features].
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
        return meRepository.getMe()
    }

    suspend fun loadMe(): MeResponse = meRepository.getMe()

    fun logout() {
        authRepository.logout()
    }
}
