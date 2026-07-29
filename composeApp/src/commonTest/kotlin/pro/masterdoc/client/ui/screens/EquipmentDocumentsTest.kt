package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import pro.masterdoc.client.auth.DocumentMetaDto

class EquipmentDocumentsTest {
    private fun doc(
        id: String,
        filename: String = "$id.pdf",
        storageKey: String = "org/$id.pdf",
    ) = DocumentMetaDto(
        id = id,
        orgId = "org",
        filename = filename,
        contentType = "application/pdf",
        storageKey = storageKey,
        sha256 = "sha",
        uploadedBy = "u",
    )

    @Test
    fun collapsedShowsOnlyLinkedDocument() {
        val linked = listOf(doc("linked"))
        val folder = listOf(doc("a"), doc("b"), doc("linked"))
        assertEquals(
            listOf("linked"),
            equipmentShownDocuments(linked, folder, folderExpanded = false).map { it.id },
        )
    }

    @Test
    fun expandedListsFolderContentsWithoutDroppingLinked() {
        val linked = listOf(doc("linked"))
        val folder = listOf(doc("a"), doc("b"))
        assertEquals(
            listOf("a", "b", "linked"),
            equipmentShownDocuments(linked, folder, folderExpanded = true).map { it.id },
        )
    }

    @Test
    fun expandedDedupesLinkedAlreadyInFolder() {
        val linked = listOf(doc("a"))
        val folder = listOf(doc("a"), doc("b"))
        assertEquals(
            listOf("a", "b"),
            equipmentShownDocuments(linked, folder, folderExpanded = true).map { it.id },
        )
    }

    @Test
    fun folderClickDoesNotAutoOpenDocument() {
        assertFalse(folderClickAutoOpensDocument)
    }

    @Test
    fun emptyFolderMessageHidesRawPath() {
        assertEquals("В папке нет документов", folderEmptyMessage("383177088934346755"))
        assertFalse(folderEmptyMessage("383177088934346755").contains("383177088934346755"))
    }
}
