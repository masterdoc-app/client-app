package pro.masterdoc.client.platform

import java.awt.Desktop
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files

actual suspend fun openAuthenticatedDocument(
    url: String,
    bearerToken: String,
    filename: String,
    mimeType: String,
) {
    val client = HttpClient.newHttpClient()
    val request =
        HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer $bearerToken")
            .GET()
            .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
    if (response.statusCode() !in 200..299) {
        throw IllegalStateException("HTTP ${response.statusCode()}")
    }
    val bytes = response.body()
    val safeBase = filename.ifBlank { "document.pdf" }.replace('/', '_')
    val (safeName, payload) =
        if (PdfBytes.looksLikeValidPdf(bytes)) {
            safeBase to bytes
        } else {
            val textName =
                if (safeBase.endsWith(".pdf", ignoreCase = true)) {
                    safeBase.dropLast(4) + ".txt"
                } else {
                    "$safeBase.txt"
                }
            textName to PdfBytes.textPreviewFromBytes(bytes).encodeToByteArray()
        }
    val temp = Files.createTempFile("fixaverse-", "-$safeName")
    Files.write(temp, payload)
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(temp.toFile())
    }
}
