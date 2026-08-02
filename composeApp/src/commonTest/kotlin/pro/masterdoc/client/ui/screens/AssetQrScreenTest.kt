package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.AssetQrResolveDto
import pro.masterdoc.client.auth.GatewayHttpException

class AssetQrScreenTest {
    @Test
    fun createsEmergencyRequestFromResolvedAssetAndDescription() {
        val description = "${"А".repeat(130)}\nВторая строка"
        val request =
            assetQrWorkOrderRequest(
                asset =
                    AssetQrResolveDto(
                        assetId = "asset-42",
                        name = "Насос",
                        siteId = "site-7",
                    ),
                description = description,
                dueAt = "2026-08-02",
            )

        assertEquals("emergency", request.type)
        assertEquals("А".repeat(120), request.title)
        assertEquals("asset-42", request.assetId)
        assertEquals("site-7", request.siteId)
        assertEquals("2026-08-02", request.dueAt)
        assertEquals(description, request.description)
    }

    @Test
    fun mapsQrResolveAccessErrorsToRequiredCopy() {
        assertEquals(
            "Код не найден или устарел",
            assetQrErrorMessage(GatewayHttpException(404, "raw id-like backend detail")),
        )
        assertEquals(
            "Нет доступа",
            assetQrErrorMessage(GatewayHttpException(403, "raw id-like backend detail")),
        )
    }
}
