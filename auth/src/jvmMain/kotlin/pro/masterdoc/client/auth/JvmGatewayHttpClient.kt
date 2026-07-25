package pro.masterdoc.client.auth

import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

class JvmGatewayHttpClient : GatewayHttpClient {
    override suspend fun get(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = execute("GET", url, headers, null)

    override suspend fun postForm(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = execute("POST", url, headers, body.toByteArray(StandardCharsets.UTF_8))

    override suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = execute("PUT", url, headers, body.toByteArray(StandardCharsets.UTF_8))

    override suspend fun patch(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = executePatch(url, headers, body.toByteArray(StandardCharsets.UTF_8))

    override suspend fun postBytes(
        url: String,
        body: ByteArray,
        headers: Map<String, String>,
    ): GatewayHttpResponse = execute("POST", url, headers, body)

    override suspend fun delete(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = execute("DELETE", url, headers, null)

    /** HttpURLConnection rejects PATCH; use java.net.http.HttpClient. */
    private fun executePatch(
        url: String,
        headers: Map<String, String>,
        body: ByteArray,
    ): GatewayHttpResponse {
        val client = java.net.http.HttpClient.newHttpClient()
        val builder =
            java.net.http.HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(30))
                .method(
                    "PATCH",
                    java.net.http.HttpRequest.BodyPublishers.ofByteArray(body),
                )
        headers.forEach { (k, v) -> builder.header(k, v) }
        val response = client.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        return GatewayHttpResponse(status = response.statusCode(), body = response.body())
    }

    private fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: ByteArray?,
    ): GatewayHttpResponse {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.doInput = true
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Length", body.size.toString())
            connection.outputStream.use { os ->
                os.write(body)
            }
        }
        val status = connection.responseCode
        val stream =
            if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
        val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        return GatewayHttpResponse(status = status, body = text)
    }
}
