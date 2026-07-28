package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MentorRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun askMentorPostsWorkOrderMessageAndHistory() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var capturedMethod = ""
            var capturedUrl = ""
            var capturedBody = ""
            val http =
                RecordingGatewayHttpClient { method, url, _, body ->
                    capturedMethod = method
                    capturedUrl = url
                    capturedBody = body.orEmpty()
                    GatewayHttpResponse(200, """{"reply":"Check step 2 in the manual"}""")
                }
            val repo = EquipmentRepository(config = config, http = http, tokenStore = tokens)

            val result =
                repo.askMentor(
                    workOrderId = "wo-1",
                    message = "What is next?",
                    history =
                        listOf(
                            MentorHistoryTurn(role = "user", content = "Hi"),
                            MentorHistoryTurn(role = "assistant", content = "Hello"),
                        ),
                )

            assertEquals("POST", capturedMethod)
            assertTrue(capturedUrl.endsWith("/ai/mentor"))
            val jsonBody = Json.parseToJsonElement(capturedBody).jsonObject
            assertEquals("wo-1", jsonBody["workOrderId"]!!.jsonPrimitive.content)
            assertEquals("What is next?", jsonBody["message"]!!.jsonPrimitive.content)
            val history = jsonBody["history"]!!.jsonArray
            assertEquals(2, history.size)
            assertEquals("user", history[0].jsonObject["role"]!!.jsonPrimitive.content)
            assertEquals("assistant", history[1].jsonObject["role"]!!.jsonPrimitive.content)
            assertEquals("Check step 2 in the manual", result.reply)
        }

    @Test
    fun askMentorPropagatesForbiddenStatus() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { _, _, _, _ ->
                    GatewayHttpResponse(403, "Only the work order assignee may use the mentor")
                }
            val repo = EquipmentRepository(config = config, http = http, tokenStore = tokens)

            val ex =
                assertFailsWith<GatewayHttpException> {
                    repo.askMentor(workOrderId = "wo-1", message = "help")
                }
            assertEquals(403, ex.status)
        }
}
