package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class MeResponseDecodeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun meResponse_decodesWithoutRoles() {
        val body =
            """{"userInfo":{"id":"u1","givenName":"I","familyName":"P","email":"i@e.com"},"features":["board"]}"""
        val me = json.decodeFromString(MeResponse.serializer(), body)
        assertEquals(listOf("board"), me.features)
        assertEquals("u1", me.userInfo.id)
        assertEquals("i@e.com", me.userInfo.email)
    }
}

class AdminUsersRepositoryTest {
    @Test
    fun inviteUser_postsFeaturesBody() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("POST", method)
                    assertTrue(url.endsWith("/admin/users/invites"))
                    assertEquals("Bearer at", headers["Authorization"])
                    assertTrue(body!!.contains("\"features\""))
                    assertTrue(body.contains("board"))
                    assertTrue(body.contains("\"email\":\"a@b.com\""))
                    GatewayHttpResponse(
                        201,
                        """{"id":"1","email":"a@b.com","givenName":"A","familyName":"B","features":["board"],"state":"invited","inviteSent":true}""",
                    )
                }
            val repo =
                AdminUsersRepository(
                    config = AuthConfig(clientId = "web"),
                    http = http,
                    tokenStore = tokens,
                )
            val user =
                repo.inviteUser(
                    InviteUserRequest(
                        email = "a@b.com",
                        givenName = "A",
                        familyName = "B",
                        features = listOf("board"),
                    ),
                )
            assertEquals("1", user.id)
            assertEquals(listOf("board"), user.features)
        }


    @Test
    fun listFeatures_getsCatalogWithRussianTitles() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.endsWith("/features"))
                    assertEquals("Bearer at", headers["Authorization"])
                    GatewayHttpResponse(
                        200,
                        """{"items":[{"id":"board","titleRu":"Доска"},{"id":"user_invite","titleRu":"Пользователи"}]}""",
                    )
                }
            val repo =
                AdminUsersRepository(
                    config = AuthConfig(clientId = "web"),
                    http = http,
                    tokenStore = tokens,
                )
            val catalog = repo.listFeatures()
            assertEquals(2, catalog.items.size)
            assertEquals("board", catalog.items[0].id)
            assertEquals("Доска", catalog.items[0].titleRu)
        }

    @Test
    fun listUsers_getsItems() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.contains("/admin/users"))
                    GatewayHttpResponse(
                        200,
                        """{"items":[{"id":"1","email":"a@b.com","givenName":"A","familyName":"B","features":["board"],"state":"invited"}],"total":1}""",
                    )
                }
            val repo =
                AdminUsersRepository(
                    config = AuthConfig(clientId = "web"),
                    http = http,
                    tokenStore = tokens,
                )
            val list = repo.listUsers()
            assertEquals(1, list.total)
            assertEquals("a@b.com", list.items[0].email)
        }

    @Test
    fun deleteUser_deletesUser() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("DELETE", method)
                    assertTrue(url.endsWith("/admin/users/u-1"))
                    assertEquals("Bearer at", headers["Authorization"])
                    assertEquals(null, body)
                    GatewayHttpResponse(204, "")
                }
            val repo =
                AdminUsersRepository(
                    config = AuthConfig(clientId = "web"),
                    http = http,
                    tokenStore = tokens,
                )
            repo.deleteUser("u-1")
        }
}

internal class RecordingGatewayHttpClient(
    private val handler: (
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ) -> GatewayHttpResponse,
) : GatewayHttpClient {
    override suspend fun get(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("GET", url, headers, null)

    override suspend fun postForm(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("POST", url, headers, body)

    override suspend fun postBytes(
        url: String,
        body: ByteArray,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("POST", url, headers, "<bytes:${body.size}>")

    override suspend fun delete(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("DELETE", url, headers, null)
}
