package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EquipmentDraftPublishTest {
    @Test
    fun prefersExistingDraftMapOverTechnologistId() {
        assertEquals(
            "map-linked",
            mapIdToConfirmWithAsset(
                linkedDraftMapId = "map-linked",
                technologistDraftMapId = "map-job",
            ),
        )
    }

    @Test
    fun usesTechnologistMapWhenNoLinkedDraft() {
        assertEquals(
            "map-job",
            mapIdToConfirmWithAsset(
                linkedDraftMapId = null,
                technologistDraftMapId = "map-job",
            ),
        )
    }

    @Test
    fun blankIdsAreIgnored() {
        assertNull(mapIdToConfirmWithAsset(linkedDraftMapId = "  ", technologistDraftMapId = ""))
        assertEquals(
            "map-job",
            mapIdToConfirmWithAsset(linkedDraftMapId = "", technologistDraftMapId = "map-job"),
        )
    }
}
