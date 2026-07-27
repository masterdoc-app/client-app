package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class EquipmentDeleteAssetTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun deleteAssetUsesDeleteMethod() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var calledMethod = ""
            var calledUrl = ""
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    calledMethod = method
                    calledUrl = url
                    GatewayHttpResponse(204, "")
                }
            val repo = EquipmentRepository(config = config, http = http, tokenStore = tokens)
            repo.deleteAsset("asset-1")
            assertEquals("DELETE", calledMethod)
            assertTrue(calledUrl.endsWith("/assets/asset-1"))
        }
}
