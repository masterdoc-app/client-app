package pro.masterdoc.client.auth

/**
 * Browser navigation / URL helpers (no-ops or stubs on non-web).
 */
expect object BrowserNav {
    fun currentPath(): String

    fun currentSearch(): String

    fun navigateTo(url: String)

    fun replaceTo(url: String)
}

fun parseQueryParams(search: String): Map<String, String> {
    val raw = search.removePrefix("?")
    if (raw.isBlank()) return emptyMap()
    return raw
        .split('&')
        .mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val eq = part.indexOf('=')
            if (eq < 0) {
                decodeQueryComponent(part) to ""
            } else {
                decodeQueryComponent(part.substring(0, eq)) to
                    decodeQueryComponent(part.substring(eq + 1))
            }
        }.toMap()
}

private fun decodeQueryComponent(value: String): String {
    val withSpaces = value.replace('+', ' ')
    val bytes = ArrayList<Byte>()
    var i = 0
    while (i < withSpaces.length) {
        val c = withSpaces[i]
        if (c == '%' && i + 2 < withSpaces.length) {
            val hi = withSpaces[i + 1].digitToIntOrNull(16)
            val lo = withSpaces[i + 2].digitToIntOrNull(16)
            if (hi != null && lo != null) {
                bytes.add(((hi shl 4) + lo).toByte())
                i += 3
                continue
            }
        }
        bytes.add(c.code.toByte())
        i++
    }
    return bytes.toByteArray().decodeToString()
}
