package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class CommentsRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun listGetsCommentsForWorkOrder() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("GET", method)
                    assertEquals("https://api.test/comments?workOrderId=wo-1", url)
                    assertEquals("Bearer at", headers["Authorization"])
                    assertEquals(null, body)
                    GatewayHttpResponse(
                        200,
                        """[{"id":"comment-1","orgId":"org-1","workOrderId":"wo-1","authorId":"user-1","text":"Готово","attachmentId":null,"createdAt":"2026-08-04T10:00:00Z"}]""",
                    )
                }
            val repo = CommentsRepository(config = config, http = http, tokenStore = tokens)

            val comments = repo.list("wo-1")

            assertEquals(1, comments.size)
            assertEquals("comment-1", comments.single().id)
            assertEquals("Готово", comments.single().text)
        }

    @Test
    fun createPostsJsonComment() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("POST", method)
                    assertEquals("https://api.test/comments", url)
                    assertEquals("Bearer at", headers["Authorization"])
                    assertEquals("application/json", headers["Content-Type"])
                    assertEquals(
                        """{"workOrderId":"wo-1","text":"Фото после ремонта","attachmentId":"att-1"}""",
                        body,
                    )
                    GatewayHttpResponse(
                        201,
                        """{"id":"comment-2","orgId":"org-1","workOrderId":"wo-1","authorId":"user-1","text":"Фото после ремонта","attachmentId":"att-1","createdAt":"2026-08-04T10:01:00Z"}""",
                    )
                }
            val repo = CommentsRepository(config = config, http = http, tokenStore = tokens)

            val created =
                repo.create(
                    CreateWorkOrderCommentRequest(
                        workOrderId = "wo-1",
                        text = "Фото после ремонта",
                        attachmentId = "att-1",
                    ),
                )

            assertEquals("comment-2", created.id)
            assertEquals("att-1", created.attachmentId)
        }
}
