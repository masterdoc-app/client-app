package pro.masterdoc.client.navigation

/**
 * In-app deep links (URL hash). Examples:
 * - `#/ppr/{mapId}` → ППР screen focused on map
 * - `#/equipment` → Оборудование
 */
sealed class AppDeepLink {
    data class Ppr(val mapId: String) : AppDeepLink()

    data object Equipment : AppDeepLink()

    data object Charts : AppDeepLink()
}

fun parseAppDeepLink(hash: String): AppDeepLink? {
    val raw = hash.trim().removePrefix("#").removePrefix("/")
    if (raw.isBlank()) return null
    val parts = raw.split('/').filter { it.isNotBlank() }
    if (parts.isEmpty()) return null
    return when (parts[0].lowercase()) {
        "ppr", "charts" -> {
            val mapId = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
            if (mapId != null) AppDeepLink.Ppr(mapId) else AppDeepLink.Charts
        }
        "equipment" -> AppDeepLink.Equipment
        else -> null
    }
}

fun AppDeepLink.toHash(): String =
    when (this) {
        is AppDeepLink.Ppr -> "#/ppr/${mapId}"
        AppDeepLink.Equipment -> "#/equipment"
        AppDeepLink.Charts -> "#/ppr"
    }

fun AppDeepLink.toDestination(): NavDestinationId =
    when (this) {
        is AppDeepLink.Ppr, AppDeepLink.Charts -> NavDestinationId.Charts
        AppDeepLink.Equipment -> NavDestinationId.Equipment
    }
