package pro.masterdoc.client.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodePickedImage(bytes: ByteArray): ImageBitmap?
