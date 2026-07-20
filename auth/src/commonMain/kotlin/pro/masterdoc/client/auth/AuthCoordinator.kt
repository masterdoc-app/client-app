package pro.masterdoc.client.auth

/**
 * Portal-facing auth orchestration: login redirect, callback exchange, role routing.
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
    ): RoleRoute {
        authRepository.exchangeCode(code = code, returnedState = state)
        val me = meRepository.getMe()
        return RoleRouter.resolve(me.userInfo.roles)
    }

    suspend fun resolveRouteForCurrentSession(): RoleRoute {
        val me = meRepository.getMe()
        return RoleRouter.resolve(me.userInfo.roles)
    }

    suspend fun loadMe(): MeResponse = meRepository.getMe()

    fun logout() {
        authRepository.logout()
    }
}
