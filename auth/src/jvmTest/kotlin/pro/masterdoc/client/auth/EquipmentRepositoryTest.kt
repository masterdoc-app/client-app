package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class EquipmentRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun getAssetFetchesById() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var capturedUrl = ""
            val http =
                RecordingGatewayHttpClient { _, url, _, _ ->
                    capturedUrl = url
                    GatewayHttpResponse(
                        200,
                        """{"id":"5318aaa1-3001-48cf-9642-f26c6bccd5e1","orgId":"o","siteId":"s","name":"Насос","status":"active","source":"ai_generated","documentIds":["d1"]}""",
                    )
                }
            val repo = EquipmentRepository(config = config, http = http, tokenStore = tokens)

            val asset = repo.getAsset("5318aaa1-3001-48cf-9642-f26c6bccd5e1")

            assertTrue(capturedUrl.endsWith("/assets/5318aaa1-3001-48cf-9642-f26c6bccd5e1"))
            assertEquals("Насос", asset.name)
            assertEquals("5318aaa1-3001-48cf-9642-f26c6bccd5e1", asset.id)
        }

    @Test
    fun qrPdfUrlTargetsAssetPdfEndpoint() {
        val repo =
            EquipmentRepository(
                config = config,
                http = RecordingGatewayHttpClient { _, _, _, _ -> error("HTTP must not be called") },
                tokenStore = InMemoryTokenStore(),
            )

        assertEquals("https://api.test/assets/a1/qr.pdf", repo.qrPdfUrl("a1"))
    }

    @Test
    fun resolveQrGetsAssetByToken() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var capturedMethod = ""
            var capturedUrl = ""
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    capturedMethod = method
                    capturedUrl = url
                    GatewayHttpResponse(
                        200,
                        """{"assetId":"a1","name":"Насос","siteId":"s1","siteName":"Цех 1"}""",
                    )
                }
            val repo = EquipmentRepository(config = config, http = http, tokenStore = tokens)

            val response = repo.resolveQr("qr-token")

            assertEquals("GET", capturedMethod)
            assertTrue(capturedUrl.endsWith("/assets/by-qr/qr-token"))
            assertEquals("a1", response.assetId)
            assertEquals("Цех 1", response.siteName)
        }

    @Test
    fun updateAssetPatchesNameAndInventoryNo() =
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
                    GatewayHttpResponse(
                        200,
                        """{"id":"a1","orgId":"o","siteId":"s","name":"Мост-1","inventoryNo":"ИНВ-9","status":"draft","source":"ai_generated","documentIds":["d1"]}""",
                    )
                }
            val repo = EquipmentRepository(config = config, http = http, tokenStore = tokens)

            val updated = repo.updateAsset("a1", UpdateAssetRequest(name = "Мост-1", inventoryNo = "ИНВ-9"))

            assertEquals("PATCH", capturedMethod)
            assertTrue(capturedUrl.endsWith("/assets/a1"))
            val jsonBody = Json.parseToJsonElement(capturedBody).jsonObject
            assertEquals("Мост-1", jsonBody["name"]!!.jsonPrimitive.content)
            assertEquals("ИНВ-9", jsonBody["inventoryNo"]!!.jsonPrimitive.content)
            assertEquals("Мост-1", updated.name)
            assertEquals("ИНВ-9", updated.inventoryNo)
        }

    @Test
    fun updateMapPatchesTitleAndItems() =
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
                    GatewayHttpResponse(
                        200,
                        """{"id":"m1","assetId":"a1","orgId":"o","title":"ППР насоса","status":"active","source":"manual","items":[{"id":"i1","title":"Осмотр","kind":"inspection","interval":{"every":7,"unit":"days"},"criticality":"high"}]}""",
                    )
                }
            val repo = EquipmentRepository(config = config, http = http, tokenStore = tokens)

            val updated =
                repo.updateMap(
                    "m1",
                    UpdateMaintenanceMapRequest(
                        title = "ППР насоса",
                        items =
                            listOf(
                                MaintenanceMapItemInput(
                                    title = "Осмотр",
                                    kind = "inspection",
                                    interval = IntervalDto(every = 7, unit = "days"),
                                    criticality = "high",
                                ),
                            ),
                    ),
                )

            assertEquals("PATCH", capturedMethod)
            assertTrue(capturedUrl.endsWith("/maintenance-maps/m1"))
            val jsonBody = Json.parseToJsonElement(capturedBody).jsonObject
            assertEquals("ППР насоса", jsonBody["title"]!!.jsonPrimitive.content)
            assertEquals(
                7,
                jsonBody["items"]!!.jsonArray[0].jsonObject["interval"]!!.jsonObject["every"]!!.jsonPrimitive.content.toInt(),
            )
            assertEquals("active", updated.status)
        }
}
