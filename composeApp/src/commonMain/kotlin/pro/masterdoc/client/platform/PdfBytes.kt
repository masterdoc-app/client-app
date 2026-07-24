package pro.masterdoc.client.platform

/**
 * Browser PDF viewers reject the old from-text fixture (`%PDF-1.4` + raw text, no xref/%%EOF).
 */
object PdfBytes {
    fun looksLikeValidPdf(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val headLen = minOf(8, bytes.size)
        val head = bytes.decodeToString(startIndex = 0, endIndex = headLen)
        if (!head.startsWith("%PDF-")) return false
        val tailStart = maxOf(0, bytes.size - 2048)
        val tail = bytes.decodeToString(startIndex = tailStart, endIndex = bytes.size)
        return tail.contains("%%EOF")
    }

    /** Best-effort plain text for legacy fake PDF fixtures. */
    fun textPreviewFromBytes(bytes: ByteArray): String {
        val raw = bytes.decodeToString()
        return if (raw.startsWith("%PDF-")) {
            raw.substringAfter('\n', missingDelimiterValue = raw)
        } else {
            raw
        }
    }
}
