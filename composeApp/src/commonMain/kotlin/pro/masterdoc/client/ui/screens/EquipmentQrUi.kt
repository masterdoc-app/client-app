package pro.masterdoc.client.ui.screens

private const val ASSET_QR_URL_PREFIX = "https://app.fixaverse.ru/#/qr/"

internal fun shouldShowEquipmentQr(
    canManageQr: Boolean,
    assetStatus: String,
): Boolean = canManageQr && assetStatus == "active"

internal fun equipmentQrUrl(qrToken: String?): String? =
    qrToken
        ?.takeIf { it.isNotBlank() }
        ?.let { "$ASSET_QR_URL_PREFIX$it" }

internal fun equipmentQrActionLabel(): String = "Открыть PDF"
