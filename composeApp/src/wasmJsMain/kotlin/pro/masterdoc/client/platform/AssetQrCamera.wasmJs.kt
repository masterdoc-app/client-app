package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.js.JsAny
import kotlin.js.JsName

@Composable
actual fun rememberAssetQrCameraController(
    active: Boolean,
    onRawQr: (String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
): AssetQrCameraController {
    val onRawState = rememberUpdatedState(onRawQr)
    val onErrorState = rememberUpdatedState(onError)
    val onCancelledState = rememberUpdatedState(onCancelled)

    DisposableEffect(active) {
        if (active) {
            fixaverseActivateAssetQrCamera(
                onSuccess = { raw ->
                    onRawState.value(raw.toString())
                },
                onError = { error ->
                    val message = error.toString()
                    if (message == "cancelled") {
                        onCancelledState.value()
                    } else {
                        onErrorState.value(cameraErrorMessage(message))
                    }
                },
            )
        } else {
            fixaverseDeactivateAssetQrCamera()
        }
        onDispose {
            fixaverseDeactivateAssetQrCamera()
        }
    }

    return remember {
        AssetQrCameraController(
            openCamera = {
                if (!fixaverseOpenAssetQrCamera()) {
                    onErrorState.value("Камера не готова")
                }
            },
        )
    }
}

private fun cameraErrorMessage(raw: String): String =
    when {
        raw.contains("NotAllowedError", ignoreCase = true) ||
            raw.contains("Permission", ignoreCase = true) ->
            "Нет доступа к камере"
        raw.contains("NotFoundError", ignoreCase = true) ||
            raw.contains("DevicesNotFound", ignoreCase = true) ->
            "Камера не найдена"
        raw.contains("insecure-context", ignoreCase = true) ->
            "Камера доступна только по HTTPS"
        raw.contains("mediaDevices", ignoreCase = true) ->
            "Камера не поддерживается в этом браузере"
        else -> "Не удалось открыть камеру"
    }

@JsName("fixaverseActivateAssetQrCamera")
private external fun fixaverseActivateAssetQrCamera(
    onSuccess: (JsAny) -> Unit,
    onError: (JsAny) -> Unit,
)

@JsName("fixaverseDeactivateAssetQrCamera")
private external fun fixaverseDeactivateAssetQrCamera()

@JsName("fixaverseOpenAssetQrCamera")
private external fun fixaverseOpenAssetQrCamera(): Boolean
