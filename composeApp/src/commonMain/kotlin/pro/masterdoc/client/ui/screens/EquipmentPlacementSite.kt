package pro.masterdoc.client.ui.screens

import pro.masterdoc.client.auth.SiteDto

/** Default catalog site id seeded for empty orgs (`Цех 1`). */
const val DEFAULT_EQUIPMENT_PLACEMENT_SITE_ID = "ceh-1"

/**
 * Site is only a container for equipment — never a document upload target.
 * New cards from PDF are placed on the default container; relocate via move on the card.
 */
fun defaultEquipmentPlacementSiteId(sites: List<SiteDto>): String? {
    if (sites.isEmpty()) return null
    return sites.firstOrNull { it.id == DEFAULT_EQUIPMENT_PLACEMENT_SITE_ID }?.id
        ?: sites.first().id
}
