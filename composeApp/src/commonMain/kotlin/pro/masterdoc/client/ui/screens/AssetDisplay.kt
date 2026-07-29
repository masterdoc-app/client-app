package pro.masterdoc.client.ui.screens

fun assetDisplayName(name: String?, assetId: String): String {
    val trimmedName = name?.trim().orEmpty()
    if (trimmedName.isNotEmpty()) return trimmedName

    val shortId = assetId.take(8)
    return if (assetId.length > 8) "$shortId…" else shortId
}

fun assetInventoryTooltip(inventoryNo: String?): String {
    val trimmedInventoryNo = inventoryNo?.trim().orEmpty()
    return if (trimmedInventoryNo.isNotEmpty()) {
        "Инв. № $trimmedInventoryNo"
    } else {
        "Инв. № не указан"
    }
}
