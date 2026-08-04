package pro.masterdoc.client.platform

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePickerLaunchers(
    onResult: (PickedImage?) -> Unit,
    onError: (String) -> Unit,
): ImagePickerLaunchers {
    val context = LocalContext.current
    val onResultState = rememberUpdatedState(onResult)
    val onErrorState = rememberUpdatedState(onError)
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) {
            onResultState.value(null)
        } else {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("empty image")
                PickedImage(
                    bytes = bytes,
                    fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                        ?: "photo.jpg",
                    contentType = context.contentResolver.getType(uri) ?: "image/jpeg",
                )
            }.onSuccess { onResultState.value(it) }
                .onFailure { onErrorState.value("Не удалось прочитать изображение") }
        }
    }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            onResultState.value(null)
            return@rememberLauncherForActivityResult
        }
        val bitmap = result.data?.extras?.get("data") as? Bitmap
        if (bitmap == null) {
            onErrorState.value("Камера недоступна")
            onResultState.value(null)
        } else {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            onResultState.value(
                PickedImage(output.toByteArray(), "photo.jpg", "image/jpeg"),
            )
        }
    }
    return ImagePickerLaunchers(
        openGallery = { gallery.launch("image/*") },
        openCamera = {
            camera.launch(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
        },
    )
}
