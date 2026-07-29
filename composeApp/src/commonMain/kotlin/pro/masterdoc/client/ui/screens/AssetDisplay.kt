package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.SiteDto

enum class AssetLinkLineRole {
    Title,
    Inventory,
}

data class AssetLinkLine(
    val text: String,
    val role: AssetLinkLineRole,
    val isLinkColored: Boolean,
    val sharesClickTarget: Boolean,
)

fun assetDisplayName(name: String?, assetId: String): String {
    val trimmedName = name?.trim().orEmpty()
    if (trimmedName.isNotEmpty()) return trimmedName

    val shortId = assetId.take(8)
    return if (assetId.length > 8) "$shortId…" else shortId
}

/** Human-readable site label — never a raw UUID. */
fun siteDisplayName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    return trimmed.ifEmpty { "—" }
}

fun resolveSiteName(
    sites: List<SiteDto>,
    siteId: String,
): String = siteDisplayName(sites.find { it.id == siteId }?.name)

fun assetInventoryTooltip(inventoryNo: String?): String {
    val trimmedInventoryNo = inventoryNo?.trim().orEmpty()
    return if (trimmedInventoryNo.isNotEmpty()) {
        "Инв. № $trimmedInventoryNo"
    } else {
        "Инв. № не указан"
    }
}

/** Presentation for [AssetNameLink]: both lines are the same primary-colored click target. */
fun assetNameLinkLines(
    name: String?,
    inventoryNo: String?,
    assetId: String,
): List<AssetLinkLine> =
    listOf(
        AssetLinkLine(
            text = assetDisplayName(name, assetId),
            role = AssetLinkLineRole.Title,
            isLinkColored = true,
            sharesClickTarget = true,
        ),
        AssetLinkLine(
            text = assetInventoryTooltip(inventoryNo),
            role = AssetLinkLineRole.Inventory,
            isLinkColored = true,
            sharesClickTarget = true,
        ),
    )
