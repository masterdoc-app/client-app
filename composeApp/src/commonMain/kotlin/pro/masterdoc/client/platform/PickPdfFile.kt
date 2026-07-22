package pro.masterdoc.client.platform

data class PickedPdf(
    val filename: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PickedPdf
        return filename == other.filename && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * filename.hashCode() + bytes.contentHashCode()
}

/** Opens a native/system file picker restricted to PDF. Returns null if cancelled. */
expect suspend fun pickPdfFile(): PickedPdf?
