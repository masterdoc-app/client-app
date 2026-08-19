package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

class WarehouseRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun listGetsParts() = runBlocking {
        val repo = repository { method, url, _, _ ->
            assertEquals("GET", method)
            assertEquals("https://api.test/warehouse/parts", url)
            GatewayHttpResponse(200, """[{"id":"part-1","orgId":"org-1","name":"Подшипник","uom":"шт","onHand":4}]""")
        }

        assertEquals("Подшипник", repo.listParts().single().name)
    }

    @Test
    fun listDecodesCompactPartJson() = runBlocking {
        val repo = repository { _, _, _, _ ->
            GatewayHttpResponse(200, """[{"id":"part-1","name":"Подшипник"}]""")
        }

        val part = repo.listParts().single()

        assertEquals("Подшипник", part.name)
        assertEquals("шт", part.uom)
    }

    @Test
    fun receiptPostsJson() = runBlocking {
        val repo = repository { method, url, headers, body ->
            assertEquals("POST", method)
            assertEquals("https://api.test/warehouse/stock/receipt", url)
            assertEquals("application/json", headers["Content-Type"])
            assertEquals("""{"partId":"part-1","siteId":"site-1","qty":3}""", body)
            GatewayHttpResponse(201, """{"id":"movement-1","partId":"part-1","siteId":"site-1","type":"receipt","qty":3}""")
        }

        assertEquals(3, repo.receipt(StockReceiptRequest("part-1", "site-1", 3)).qty)
    }

    @Test
    fun issuePostsJson() = runBlocking {
        val repo = repository { method, url, _, body ->
            assertEquals("POST", method)
            assertEquals("https://api.test/warehouse/stock/issue", url)
            assertEquals(
                """{"partId":"part-1","siteId":"site-1","qty":2,"workOrderId":"wo-1","assetId":"asset-1"}""",
                body,
            )
            GatewayHttpResponse(201, """{"id":"movement-2","partId":"part-1","siteId":"site-1","type":"issue","qty":2}""")
        }

        assertEquals("issue", repo.issue(StockIssueRequest("part-1", "site-1", 2, "wo-1", "asset-1")).type)
    }

    @Test
    fun advicePropagatesNotFound() = runBlocking {
        val repo = repository { _, _, _, _ -> GatewayHttpResponse(404, "not found") }

        val error = assertFailsWith<GatewayHttpException> { repo.latestAdvice() }

        assertEquals(404, error.status)
    }

    @Test
    fun adviceDecodesCompactJson() = runBlocking {
        val repo = repository { _, _, _, _ -> GatewayHttpResponse(200, """{"textRu":"Пополните подшипники"}""") }

        assertEquals("Пополните подшипники", repo.latestAdvice().textRu)
    }

    @Test
    fun replaceAssetPartsPutsItemsWrapper() = runBlocking {
        val repo = repository { method, url, headers, body ->
            assertEquals("PUT", method)
            assertEquals("https://api.test/warehouse/assets/asset-1/parts", url)
            assertEquals("application/json", headers["Content-Type"])
            assertEquals("""{"items":[{"partId":"part-1","qtyHint":2,"critical":true}]}""", body)
            GatewayHttpResponse(200, """[{"partId":"part-1","qtyHint":2,"critical":true}]""")
        }

        assertEquals(
            "part-1",
            repo.replaceAssetParts("asset-1", listOf(AssetPartDto("part-1", qtyHint = 2, critical = true))).single().partId,
        )
    }

    private suspend fun repository(
        handler: (String, String, Map<String, String>, String?) -> GatewayHttpResponse,
    ): WarehouseRepository {
        val tokens = InMemoryTokenStore()
        tokens.write(AuthTokens(accessToken = "at"))
        return WarehouseRepository(config, RecordingGatewayHttpClient(handler), tokens)
    }
}
