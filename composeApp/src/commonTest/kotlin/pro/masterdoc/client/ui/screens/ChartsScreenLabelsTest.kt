package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartsScreenLabelsTest {
    @Test
    fun mapHeadline_omitsActiveStatus() {
        assertEquals(
            "ППР · ИИ",
            mapHeadline(title = "ППР", status = "active", source = "ai_generated"),
        )
    }

    @Test
    fun mapHeadline_keepsDraftStatus() {
        assertEquals(
            "ППР · черновик · ИИ",
            mapHeadline(title = "ППР", status = "draft", source = "ai_generated"),
        )
    }

    @Test
    fun ruStatus_activeIsBlank() {
        assertEquals("", ruStatus("active"))
        assertEquals("", ruStatus("ACTIVE"))
    }

    @Test
    fun pprStatusChipLabel_draft() {
        assertEquals("Черновик", pprStatusChipLabel("draft"))
    }

    @Test
    fun pprStatusChipLabel_active() {
        assertEquals("В базе", pprStatusChipLabel("active"))
    }

    @Test
    fun pprSourceChipLabel_aiGenerated() {
        assertEquals("ИИ", pprSourceChipLabel("ai_generated"))
    }

    @Test
    fun pprSourceChipLabel_manual() {
        assertEquals("вручную", pprSourceChipLabel("manual"))
    }

    @Test
    fun pprDocumentLines_emptyWhenNoDocs_neverUnbound() {
        assertTrue(pprDocumentLines(emptyList()).isEmpty())
        assertTrue(
            pprDocumentLines(emptyList()).none { it.contains("не привязан", ignoreCase = true) },
        )
    }

    @Test
    fun pprDocumentLines_listsFilenames() {
        assertEquals(
            listOf("Документ: bridgeDoc.pdf", "Документ: passport.pdf"),
            pprDocumentLines(listOf("bridgeDoc.pdf", "passport.pdf")),
        )
    }
}
