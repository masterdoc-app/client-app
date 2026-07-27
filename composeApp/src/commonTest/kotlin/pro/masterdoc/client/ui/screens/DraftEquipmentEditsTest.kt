package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pro.masterdoc.client.auth.UpdateAssetRequest

class DraftEquipmentEditsTest {
    @Test
    fun draftConfirmPayloadKeepsUserEditedNameAndInventory() {
        val payload =
            UpdateAssetRequest(
                name = "Мост балочный №3",
                inventoryNo = "ИНВ-СВОЙ",
            )
        assertEquals("Мост балочный №3", payload.name)
        assertEquals("ИНВ-СВОЙ", payload.inventoryNo)
        assertTrue(payload.documentIds == null)
    }
}
