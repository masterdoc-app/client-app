package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable

data class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is PickedImage &&
            fileName == other.fileName &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)

    override fun hashCode(): Int =
        31 * (31 * fileName.hashCode() + contentType.hashCode()) + bytes.contentHashCode()
}

class ImagePickerLaunchers(
    val openGallery: () -> Unit,
    val openCamera: () -> Unit,
)

@Composable
expect fun rememberImagePickerLaunchers(
    onResult: (PickedImage?) -> Unit,
    onError: (String) -> Unit = {},
): ImagePickerLaunchers
