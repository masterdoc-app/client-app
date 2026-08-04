package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.navigation.parseAssetQrInput

/** Map camera/paste raw payload to opaque asset QR token, or null if not a Fixaverse sticker. */
fun resolveScannedAssetQrToken(raw: String): String? = parseAssetQrInput(raw)
