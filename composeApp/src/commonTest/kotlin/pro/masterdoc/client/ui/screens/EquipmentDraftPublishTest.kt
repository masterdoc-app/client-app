package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EquipmentDraftPublishTest {
    @Test
    fun launchesAgentWhenNoDraftMap() {
        assertTrue(needsTechnologistForPprDraft(null))
        assertTrue(needsTechnologistForPprDraft(""))
        assertTrue(needsTechnologistForPprDraft("   "))
    }

    @Test
    fun skipsAgentWhenDraftMapAlreadyLinked() {
        assertFalse(needsTechnologistForPprDraft("map-1"))
    }

    @Test
    fun prefersExistingDraftOverJobId() {
        assertEquals(
            "map-linked",
            resolvePprDraftMapId(
                existingDraftMapId = "map-linked",
                technologistDraftMapId = "map-job",
            ),
        )
    }

    @Test
    fun usesJobDraftMapWhenNoneLinked() {
        assertEquals(
            "map-job",
            resolvePprDraftMapId(
                existingDraftMapId = null,
                technologistDraftMapId = "map-job",
            ),
        )
        assertNull(resolvePprDraftMapId(existingDraftMapId = "  ", technologistDraftMapId = ""))
    }
}
