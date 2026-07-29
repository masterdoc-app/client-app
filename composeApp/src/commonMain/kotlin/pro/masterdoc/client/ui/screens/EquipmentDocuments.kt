package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.DocumentMetaDto

/** Folder row lists contents; it must never auto-open the first PDF. */
const val folderClickAutoOpensDocument: Boolean = false

fun folderEmptyMessage(
    @Suppress("UNUSED_PARAMETER") folder: String,
): String = "В папке нет документов"

/**
 * One equipment → one document (linked only).
 *
 * TODO(multi-doc): restore folder listing when expanded — full storage-folder
 * contents plus linked siblings (see git history of this function). Until then
 * ignore [folder] / [folderExpanded] so «Папка в хранилище» never dumps every
 * PDF in the org folder onto the card.
 */
fun equipmentShownDocuments(
    linked: List<DocumentMetaDto>,
    @Suppress("UNUSED_PARAMETER") folder: List<DocumentMetaDto>,
    @Suppress("UNUSED_PARAMETER") folderExpanded: Boolean,
): List<DocumentMetaDto> = linked.distinctBy { it.id }.take(1)
