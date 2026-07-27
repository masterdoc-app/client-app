package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class UserScopesRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun getDecodesScope() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.endsWith("/user-scopes/engineer-1"))
                    GatewayHttpResponse(
                        200,
                        """{"userId":"engineer-1","orgId":"org-1","siteIds":["s1"],"assetIds":["a1"]}""",
                    )
                }
            val repo = UserScopesRepository(config = config, http = http, tokenStore = tokens)
            val scope = repo.get("engineer-1")
            assertEquals("engineer-1", scope.userId)
            assertEquals(listOf("s1"), scope.siteIds)
            assertEquals(listOf("a1"), scope.assetIds)
        }

    @Test
    fun putSerializesSiteIdsAndAssetIds() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, _, b ->
                    assertEquals("PUT", method)
                    assertTrue(url.endsWith("/user-scopes/engineer-1"))
                    body = b
                    GatewayHttpResponse(
                        200,
                        """{"userId":"engineer-1","orgId":"org-1","siteIds":["s1","s2"],"assetIds":["a1"]}""",
                    )
                }
            val repo = UserScopesRepository(config = config, http = http, tokenStore = tokens)
            val scope =
                repo.put(
                    "engineer-1",
                    PutUserScopeRequest(siteIds = listOf("s1", "s2"), assetIds = listOf("a1")),
                )
            val json = Json.parseToJsonElement(body!!).jsonObject
            assertEquals(listOf("s1", "s2"), json["siteIds"]!!.jsonArray.map { it.toString().trim('"') })
            assertEquals(listOf("a1"), json["assetIds"]!!.jsonArray.map { it.toString().trim('"') })
            assertEquals(listOf("s1", "s2"), scope.siteIds)
            assertEquals(listOf("a1"), scope.assetIds)
        }

    @Test
    fun putRemovingSiteOmitsRemovedId() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, _, b ->
                    assertEquals("PUT", method)
                    body = b
                    GatewayHttpResponse(
                        200,
                        """{"userId":"engineer-1","orgId":"org-1","siteIds":["s2"],"assetIds":[]}""",
                    )
                }
            val repo = UserScopesRepository(config = config, http = http, tokenStore = tokens)
            repo.put("engineer-1", PutUserScopeRequest(siteIds = listOf("s2"), assetIds = emptyList()))
            val json = Json.parseToJsonElement(body!!).jsonObject
            val siteIds = json["siteIds"]!!.jsonArray.map { it.toString().trim('"') }
            assertEquals(listOf("s2"), siteIds)
            assertTrue("s1" !in siteIds)
        }

    @Test
    fun getCandidatesDecodesUserIds() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.endsWith("/user-scopes/candidates/asset-1"))
                    GatewayHttpResponse(
                        200,
                        """{"userIds":["engineer-1","engineer-2"]}""",
                    )
                }
            val repo = UserScopesRepository(config = config, http = http, tokenStore = tokens)
            val candidates = repo.getCandidates("asset-1")
            assertEquals(listOf("engineer-1", "engineer-2"), candidates)
        }
}
