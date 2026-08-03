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

    @Test
    fun meResponse_decodesOrgName() {
        val body =
            """{"userInfo":{"id":"u1","orgName":"Fixaverse Demo"},"features":["board"]}"""
        val me = json.decodeFromString(MeResponse.serializer(), body)
        assertEquals("Fixaverse Demo", me.userInfo.orgName)
    }

    @Test
    fun meResponse_orgNameAbsentWhenMissing() {
        val body = """{"userInfo":{"id":"u1"},"features":[]}"""
        val me = json.decodeFromString(MeResponse.serializer(), body)
        assertEquals(null, me.userInfo.orgName)
    }
}

class AdminUsersRepositoryTest {
    @Test
    fun inviteUser_postsRolesBody() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("POST", method)
                    assertTrue(url.endsWith("/admin/users/invites"))
                    assertEquals("Bearer at", headers["Authorization"])
                    assertTrue(body!!.contains("\"roles\""))
                    assertTrue(body.contains("manager"))
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
                        roles = listOf("manager"),
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
                        """{"items":[{"id":"board","titleRu":"Доска"},{"id":"admin","titleRu":"Админ"}]}""",
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
    fun listRoles_getsRoleCatalog() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.endsWith("/admin/roles"))
                    assertEquals("Bearer at", headers["Authorization"])
                    GatewayHttpResponse(
                        200,
                        """{"items":[{"id":"manager","titleRu":"Менеджер","features":["board"]}]}""",
                    )
                }
            val repo =
                AdminUsersRepository(
                    config = AuthConfig(clientId = "web"),
                    http = http,
                    tokenStore = tokens,
                )
            val catalog = repo.listRoles()
            assertEquals("manager", catalog.items.single().id)
            assertEquals("Менеджер", catalog.items.single().titleRu)
        }

    @Test
    fun updateRole_putsFeaturesAndTitle() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, body ->
                    assertEquals("PUT", method)
                    assertTrue(url.endsWith("/admin/roles/manager"))
                    assertEquals("Bearer at", headers["Authorization"])
                    assertTrue(body!!.contains("\"features\":[\"board\",\"tickets\"]"))
                    assertTrue(body.contains("\"titleRu\":\"Менеджер\""))
                    GatewayHttpResponse(
                        200,
                        """{"id":"manager","titleRu":"Менеджер","features":["board","tickets"]}""",
                    )
                }
            val repo =
                AdminUsersRepository(
                    config = AuthConfig(clientId = "web"),
                    http = http,
                    tokenStore = tokens,
                )

            val updated =
                repo.updateRole(
                    "manager",
                    UpdateRoleRequest(
                        features = listOf("board", "tickets"),
                        titleRu = "Менеджер",
                    ),
                )

            assertEquals(listOf("board", "tickets"), updated.features)
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

    @Test
    fun listAudit_appendsLimitOffsetAndUserId() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, headers, _ ->
                    assertEquals("GET", method)
                    assertEquals("Bearer at", headers["Authorization"])
                    assertTrue(url.contains("/admin/audit?"))
                    assertTrue(url.contains("limit=30"))
                    assertTrue(url.contains("offset=60"))
                    assertTrue(url.contains("userId=u9"))
                    GatewayHttpResponse(
                        200,
                        """{"items":[{"id":"1","orgId":"o","userId":"u9","at":"2026-07-26T09:00:00Z","method":"GET","path":"/x","status":200}]}""",
                    )
                }
            val repo =
                AdminUsersRepository(
                    config = AuthConfig(clientId = "web"),
                    http = http,
                    tokenStore = tokens,
                )
            val list = repo.listAudit(limit = 30, offset = 60, userId = "u9")
            assertEquals(1, list.items.size)
            assertEquals("u9", list.items[0].userId)
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

    override suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("PUT", url, headers, body)

    override suspend fun patch(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = handler("PATCH", url, headers, body)

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
