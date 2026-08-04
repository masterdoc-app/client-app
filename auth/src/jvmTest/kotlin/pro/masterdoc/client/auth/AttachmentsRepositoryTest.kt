package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class AttachmentsRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun uploadPostsMultipartImage() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("POST", method)
                    assertEquals("https://api.test/attachments", url)
                    assertEquals("multipart/form-data", headers["Content-Type"]?.substringBefore(';'))
                    assertTrue(body.orEmpty().startsWith("<bytes:"))
                    GatewayHttpResponse(
                        200,
                        """{"id":"att-1","orgId":"org-1","contentType":"image/jpeg","sizeBytes":3,"createdAt":"t"}""",
                    )
                }
            val repo = AttachmentsRepository(config = config, http = http, tokenStore = tokens)

            val uploaded = repo.upload(byteArrayOf(1, 2, 3), "photo.jpg", "image/jpeg")

            assertEquals("att-1", uploaded.id)
            assertEquals("image/jpeg", uploaded.contentType)
            assertEquals(3, uploaded.sizeBytes)
        }

    @Test
    fun contentUrlTargetsAttachmentContentEndpoint() {
        val repo =
            AttachmentsRepository(
                config = config,
                http = RecordingGatewayHttpClient { _, _, _, _ -> error("HTTP must not be called") },
                tokenStore = InMemoryTokenStore(),
            )

        assertEquals("https://api.test/attachments/att-1/content", repo.contentUrl("att-1"))
    }
}
