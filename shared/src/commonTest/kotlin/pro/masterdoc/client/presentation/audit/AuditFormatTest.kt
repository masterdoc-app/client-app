package pro.masterdoc.client.presentation.audit

import kotlin.test.Test
import kotlin.test.assertEquals

class AuditFormatTest {
    @Test
    fun formatAuditAt_takesDateAndHourMinuteFromIso() {
        assertEquals("2026-07-26 09:20", formatAuditAt("2026-07-26T09:20:33.880003305Z"))
    }

    @Test
    fun formatAuditAt_keepsAlreadySpacedTimestamp() {
        assertEquals("2026-07-26 09:19", formatAuditAt("2026-07-26 09:19"))
    }

    @Test
    fun formatAuditAt_shortNonEmptyPassthrough() {
        assertEquals("short", formatAuditAt("short"))
    }

    @Test
    fun formatAuditAt_blankIsDash() {
        assertEquals("—", formatAuditAt("   "))
        assertEquals("—", formatAuditAt(""))
    }
}
