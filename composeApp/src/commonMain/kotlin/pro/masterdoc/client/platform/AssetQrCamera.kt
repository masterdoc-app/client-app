package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable

/**
 * Opens device camera to read an equipment QR (copilot-style getUserMedia on Wasm).
 * [openCamera] must be called from a user gesture.
 */
class AssetQrCameraController(
    val openCamera: () -> Unit,
)

@Composable
expect fun rememberAssetQrCameraController(
    active: Boolean,
    onRawQr: (String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit = {},
): AssetQrCameraController
