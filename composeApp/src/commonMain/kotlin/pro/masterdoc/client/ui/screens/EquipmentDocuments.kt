package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.DocumentMetaDto

/** Folder row lists contents; it must never auto-open the first PDF. */
const val folderClickAutoOpensDocument: Boolean = false

fun folderEmptyMessage(
    @Suppress("UNUSED_PARAMETER") folder: String,
): String = "В папке нет документов"

/**
 * Linked document alone when collapsed; full folder listing (plus any linked
 * siblings) when the user opens «Папка в хранилище».
 */
fun equipmentShownDocuments(
    linked: List<DocumentMetaDto>,
    folder: List<DocumentMetaDto>,
    folderExpanded: Boolean,
): List<DocumentMetaDto> {
    if (!folderExpanded) {
        return linked.distinctBy { it.id }.take(1)
    }
    val ordered = LinkedHashMap<String, DocumentMetaDto>()
    folder.forEach { ordered[it.id] = it }
    linked.forEach { doc ->
        if (doc.id !in ordered) ordered[doc.id] = doc
    }
    return ordered.values.toList()
}
