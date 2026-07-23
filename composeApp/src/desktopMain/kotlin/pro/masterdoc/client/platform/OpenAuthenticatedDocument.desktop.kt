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
    val safeName = filename.ifBlank { "document.pdf" }.replace('/', '_')
    val temp = Files.createTempFile("fixaverse-", "-$safeName")
    Files.write(temp, response.body())
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(temp.toFile())
    }
}
