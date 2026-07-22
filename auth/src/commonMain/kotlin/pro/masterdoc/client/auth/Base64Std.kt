package pro.masterdoc.client.auth

/** Standard Base64 (RFC 4648) for binary HTTP bodies over string JS bridges. */
object Base64Std {
    private val table =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()
    private val decode =
        IntArray(128) { -1 }.also { map ->
            table.forEachIndexed { i, c -> map[c.code] = i }
        }

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i + 2 < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            out.append(table[b0 shr 2])
            out.append(table[((b0 and 0x03) shl 4) or (b1 shr 4)])
            out.append(table[((b1 and 0x0F) shl 2) or (b2 shr 6)])
            out.append(table[b2 and 0x3F])
            i += 3
        }
        when (bytes.size - i) {
            1 -> {
                val b0 = bytes[i].toInt() and 0xFF
                out.append(table[b0 shr 2])
                out.append(table[(b0 and 0x03) shl 4])
                out.append('=')
                out.append('=')
            }
            2 -> {
                val b0 = bytes[i].toInt() and 0xFF
                val b1 = bytes[i + 1].toInt() and 0xFF
                out.append(table[b0 shr 2])
                out.append(table[((b0 and 0x03) shl 4) or (b1 shr 4)])
                out.append(table[(b1 and 0x0F) shl 2])
                out.append('=')
            }
        }
        return out.toString()
    }

    fun decode(text: String): ByteArray {
        val clean = text.filter { it != '\n' && it != '\r' && it != ' ' }
        require(clean.length % 4 == 0) { "Invalid base64 length" }
        val pad = clean.takeLastWhile { it == '=' }.length
        val out = ByteArray(clean.length / 4 * 3 - pad)
        var o = 0
        var i = 0
        while (i < clean.length) {
            val c0 = value(clean[i])
            val c1 = value(clean[i + 1])
            val c2 = if (clean[i + 2] == '=') 0 else value(clean[i + 2])
            val c3 = if (clean[i + 3] == '=') 0 else value(clean[i + 3])
            val n = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
            if (o < out.size) out[o++] = (n shr 16).toByte()
            if (o < out.size) out[o++] = (n shr 8).toByte()
            if (o < out.size) out[o++] = n.toByte()
            i += 4
        }
        return out
    }

    private fun value(ch: Char): Int {
        require(ch.code < decode.size && decode[ch.code] >= 0) { "Invalid base64 char: $ch" }
        return decode[ch.code]
    }
}
