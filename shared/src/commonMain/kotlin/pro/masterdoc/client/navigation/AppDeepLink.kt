package pro.masterdoc.client.navigation

/**
 * In-app deep links (URL hash). Examples:
 * - `#/ppr/{mapId}` → ППР screen focused on map
 * - `#/equipment` → Оборудование
 * - `#/equipment/{assetId}` → Оборудование focused on asset
 */
sealed class AppDeepLink {
    data class Ppr(val mapId: String) : AppDeepLink()

    data object Equipment : AppDeepLink()

    data class EquipmentDetail(val assetId: String) : AppDeepLink()

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
        "equipment" -> {
            val assetId = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
            if (assetId != null) AppDeepLink.EquipmentDetail(assetId) else AppDeepLink.Equipment
        }
        else -> null
    }
}

fun AppDeepLink.toHash(): String =
    when (this) {
        is AppDeepLink.Ppr -> "#/ppr/${mapId}"
        AppDeepLink.Equipment -> "#/equipment"
        is AppDeepLink.EquipmentDetail -> "#/equipment/${assetId}"
        AppDeepLink.Charts -> "#/ppr"
    }

fun AppDeepLink.toDestination(): NavDestinationId =
    when (this) {
        is AppDeepLink.Ppr, AppDeepLink.Charts -> NavDestinationId.Charts
        AppDeepLink.Equipment, is AppDeepLink.EquipmentDetail -> NavDestinationId.Equipment
    }
