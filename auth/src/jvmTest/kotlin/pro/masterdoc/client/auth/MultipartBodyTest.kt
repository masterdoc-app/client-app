package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultipartBodyTest {
    @Test
    fun buildsMultipartWithPdfBytes() {
        val pdf = "%PDF-1.4 hello".encodeToByteArray()
        val part =
            MultipartBody.filePart(
                fieldName = "file",
                filename = "manual.pdf",
                fileContentType = "application/pdf",
                bytes = pdf,
                random = kotlin.random.Random(1),
            )
        assertTrue(part.contentType.startsWith("multipart/form-data; boundary="))
        val text = part.body.decodeToString()
        assertTrue(text.contains("filename=\"manual.pdf\""))
        assertTrue(text.contains("Content-Type: application/pdf"))
        assertTrue(text.contains("%PDF-1.4 hello"))
    }

    @Test
    fun base64RoundTrip() {
        val raw = byteArrayOf(0, 1, 2, 250.toByte(), 255.toByte())
        assertContentEquals(raw, Base64Std.decode(Base64Std.encode(raw)))
        assertEquals("YQ==", Base64Std.encode("a".encodeToByteArray()))
    }
}
