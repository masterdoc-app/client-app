package pro.masterdoc.client.presentation.audit

/** Compact UTC timestamp for journal rows: `2026-07-26 04:51`. */
fun formatAuditAt(raw: String): String {
    val normalized = raw.trim().replace('T', ' ')
    return when {
        normalized.length >= 16 -> normalized.take(16)
        normalized.isNotEmpty() -> normalized
        else -> "—"
    }
}
