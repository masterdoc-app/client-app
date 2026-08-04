package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberImagePickerLaunchers(
    onResult: (PickedImage?) -> Unit,
    onError: (String) -> Unit,
): ImagePickerLaunchers {
    val gallery = remember(onResult) {
        {
            val dialog = FileDialog(null as Frame?, "Выберите изображение", FileDialog.LOAD)
            dialog.setFilenameFilter { _, name -> isImageFileName(name) }
            dialog.isMultipleMode = false
            dialog.isVisible = true
            val name = dialog.file
            val directory = dialog.directory
            if (name == null || directory == null) {
                onResult(null)
            } else {
                val file = File(directory, name)
                if (!file.isFile || !isImageFileName(file.name)) {
                    onResult(null)
                } else {
                    onResult(
                        PickedImage(
                            bytes = file.readBytes(),
                            fileName = file.name,
                            contentType = imageContentType(file.name),
                        ),
                    )
                }
            }
        }
    }
    val camera = remember(onError) {
        { onError("Камера недоступна") }
    }
    return remember(gallery, camera) {
        ImagePickerLaunchers(openGallery = gallery, openCamera = camera)
    }
}

private fun isImageFileName(name: String): Boolean =
    name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif")

private fun imageContentType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    else -> "image/jpeg"
}
