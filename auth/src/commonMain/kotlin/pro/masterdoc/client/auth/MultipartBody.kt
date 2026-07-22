package pro.masterdoc.client.auth

import kotlin.random.Random

data class MultipartBytes(
    val contentType: String,
    val body: ByteArray,
)

object MultipartBody {
    fun filePart(
        fieldName: String,
        filename: String,
        fileContentType: String,
        bytes: ByteArray,
        random: Random = Random.Default,
    ): MultipartBytes {
        val safeName = filename.replace("\"", "").replace("\r", "").replace("\n", "")
        val boundary = "----FixaverseBoundary${random.nextLong().toULong()}"
        val preamble =
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$safeName\"\r\n" +
                "Content-Type: $fileContentType\r\n" +
                "\r\n"
        val epilogue = "\r\n--$boundary--\r\n"
        val head = preamble.encodeToByteArray()
        val tail = epilogue.encodeToByteArray()
        val body = ByteArray(head.size + bytes.size + tail.size)
        head.copyInto(body, 0)
        bytes.copyInto(body, head.size)
        tail.copyInto(body, head.size + bytes.size)
        return MultipartBytes(
            contentType = "multipart/form-data; boundary=$boundary",
            body = body,
        )
    }
}
