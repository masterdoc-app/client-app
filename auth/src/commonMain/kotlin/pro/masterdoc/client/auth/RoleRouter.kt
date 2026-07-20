package pro.masterdoc.client.auth

/**
 * Maps Zitadel project roles to web app base paths under app.fixaverse.ru.
 */
sealed interface RoleRoute {
    data class App(val path: String) : RoleRoute

    data class NoWebApp(val roles: List<String>) : RoleRoute
}

object RoleRouter {
    const val TECHNOLOGIST_PATH = "/technolog/"

    fun resolve(roles: List<String>): RoleRoute {
        val normalized = roles.map { it.lowercase() }.toSet()
        return when {
            "technologist" in normalized -> RoleRoute.App(TECHNOLOGIST_PATH)
            else -> RoleRoute.NoWebApp(roles)
        }
    }
}
