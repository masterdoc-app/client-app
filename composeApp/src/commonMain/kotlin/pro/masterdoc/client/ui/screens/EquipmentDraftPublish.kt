package pro.masterdoc.client.ui.screens

/**
 * After the user approves equipment («В базу»), we publish the asset immediately and
 * then ensure a PPR draft exists for the Charts / ППР feature.
 *
 * - If a draft map is already linked → open it (no new agent run).
 * - Otherwise → start technologist; use the job's draftMapId.
 */
fun resolvePprDraftMapId(
    existingDraftMapId: String?,
    technologistDraftMapId: String?,
): String? =
    existingDraftMapId?.takeIf { it.isNotBlank() }
        ?: technologistDraftMapId?.takeIf { it.isNotBlank() }

fun needsTechnologistForPprDraft(existingDraftMapId: String?): Boolean =
    existingDraftMapId.isNullOrBlank()
