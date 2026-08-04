package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAssetQrCameraController(
    active: Boolean,
    onRawQr: (String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
): AssetQrCameraController =
    remember(onRawQr, onError, onCancelled) {
        AssetQrCameraController(
            openCamera = {
                onError("Камера на Android скоро — вставьте код вручную")
            },
        )
    }
