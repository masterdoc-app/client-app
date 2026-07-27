package pro.masterdoc.client.ui.screens

/**
 * Resolves which maintenance-map id to confirm together with the equipment asset
 * on a single «В базу» click. Prefer an already linked draft map; otherwise use
 * the map id returned by the technologist job.
 */
fun mapIdToConfirmWithAsset(
    linkedDraftMapId: String?,
    technologistDraftMapId: String?,
): String? = linkedDraftMapId?.takeIf { it.isNotBlank() } ?: technologistDraftMapId?.takeIf { it.isNotBlank() }
