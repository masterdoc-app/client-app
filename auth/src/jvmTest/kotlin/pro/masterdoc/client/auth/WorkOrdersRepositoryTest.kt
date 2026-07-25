package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class WorkOrdersRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun getBoardDecodesWeeks() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.contains("/work-orders/board"))
                    GatewayHttpResponse(
                        200,
                        """{"weeks":[{"weekStart":"2026-07-20","items":[]}]}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            val board = repo.getBoard(weeks = 4)
            assertEquals(1, board.weeks.size)
            assertEquals("2026-07-20", board.weeks[0].weekStart)
        }

    @Test
    fun patchSendsNullAssignee() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, _, b ->
                    assertEquals("PATCH", method)
                    assertTrue(url.endsWith("/work-orders/wo-1"))
                    body = b
                    GatewayHttpResponse(
                        200,
                        """{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"T","assetId":"a","siteId":"s","dueAt":"2026-07-22","assigneeId":null,"source":"api","createdAt":"t","updatedAt":"t"}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            repo.patch("wo-1", clearAssignee = true)
            val json = Json.parseToJsonElement(body!!).jsonObject
            assertTrue(json.containsKey("assigneeId"))
            assertEquals("null", json["assigneeId"].toString())
        }
}
