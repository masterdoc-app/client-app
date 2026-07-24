package pro.masterdoc.client.auth

import org.kotlincrypto.hash.sha2.SHA256
import kotlin.random.Random

/**
 * PKCE (S256) helpers for Authorization Code flow.
 */
object Pkce {
    private val verifierAlphabet =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~".toCharArray()

    fun generateVerifier(length: Int = 64, random: Random = Random.Default): String {
        require(length in 43..128) { "PKCE verifier length must be 43..128" }
        return buildString(length) {
            repeat(length) { append(verifierAlphabet[random.nextInt(verifierAlphabet.size)]) }
        }
    }

    fun challengeS256(verifier: String): String {
        val digest = SHA256().digest(verifier.encodeToByteArray())
        return Base64Url.encode(digest)
    }

    fun generateState(length: Int = 32, random: Random = Random.Default): String =
        generateVerifier(length.coerceIn(43, 128), random).take(length)
}

object Base64Url {
    private val table =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
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
            }
            2 -> {
                val b0 = bytes[i].toInt() and 0xFF
                val b1 = bytes[i + 1].toInt() and 0xFF
                out.append(table[b0 shr 2])
                out.append(table[((b0 and 0x03) shl 4) or (b1 shr 4)])
                out.append(table[(b1 and 0x0F) shl 2])
            }
        }
        return out.toString()
    }

    /** Decode Base64URL (JWT segment); padding optional. */
    fun decode(text: String): ByteArray {
        val padded =
            buildString(text.length + 3) {
                append(text.replace('-', '+').replace('_', '/'))
                while (length % 4 != 0) append('=')
            }
        return Base64Std.decode(padded)
    }
}
