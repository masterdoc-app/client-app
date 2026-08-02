@file:OptIn(ExperimentalWasmJsInterop::class)

package pro.masterdoc.client.auth

import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual object BrowserNav {
    actual fun currentPath(): String = window.location.pathname

    actual fun currentSearch(): String = window.location.search

    actual fun currentHash(): String = window.location.hash

    actual fun setHash(hash: String) {
        val normalized = if (hash.isBlank() || hash == "#") "" else if (hash.startsWith("#")) hash else "#$hash"
        window.location.hash = normalized
    }

    actual fun savePendingDeepLink(hash: String) {
        localStorage.setItem(KEY_PENDING_DEEP_LINK, hash)
    }

    actual fun consumePendingDeepLink(): String? =
        localStorage.getItem(KEY_PENDING_DEEP_LINK)?.also {
            localStorage.removeItem(KEY_PENDING_DEEP_LINK)
        }

    actual fun navigateTo(url: String) {
        window.location.href = url
    }

    actual fun replaceTo(url: String) {
        window.location.replace(url)
    }
}

actual fun createDefaultTokenStore(): TokenStore = BrowserTokenStore()

actual fun createDefaultPkceSessionStore(): PkceSessionStore = BrowserPkceSessionStore()

actual fun createDefaultGatewayHttpClient(): GatewayHttpClient = WasmGatewayHttpClient()

private const val KEY_ACCESS = "fixaverse.auth.access_token"
private const val KEY_REFRESH = "fixaverse.auth.refresh_token"
private const val KEY_ID = "fixaverse.auth.id_token"
private const val KEY_SESSIONS = "fixaverse.auth.pkce_sessions"
private const val KEY_PENDING_DEEP_LINK = "fixaverse.pending_deep_link"
/** Legacy single-slot keys — migrated on first read. */
private const val KEY_VERIFIER_LEGACY = "fixaverse.auth.pkce_verifier"
private const val KEY_STATE_LEGACY = "fixaverse.auth.pkce_state"

class BrowserTokenStore : TokenStore {
    override fun read(): AuthTokens? {
        val access = localStorage.getItem(KEY_ACCESS) ?: return null
        return AuthTokens(
            accessToken = access,
            refreshToken = localStorage.getItem(KEY_REFRESH),
            idToken = localStorage.getItem(KEY_ID),
        )
    }

    override fun write(tokens: AuthTokens) {
        localStorage.setItem(KEY_ACCESS, tokens.accessToken)
        val refresh = tokens.refreshToken
        if (refresh != null) {
            localStorage.setItem(KEY_REFRESH, refresh)
        } else {
            localStorage.removeItem(KEY_REFRESH)
        }
        val id = tokens.idToken
        if (id != null) {
            localStorage.setItem(KEY_ID, id)
        } else {
            localStorage.removeItem(KEY_ID)
        }
    }

    override fun clear() {
        localStorage.removeItem(KEY_ACCESS)
        localStorage.removeItem(KEY_REFRESH)
        localStorage.removeItem(KEY_ID)
    }
}

class BrowserPkceSessionStore : PkceSessionStore {
    override fun save(
        verifier: String,
        state: String,
    ) {
        val sessions = readSessions().toMutableMap()
        sessions[state] = verifier
        while (sessions.size > InMemoryPkceSessionStore.MAX_SESSIONS) {
            val oldest = sessions.keys.first()
            sessions.remove(oldest)
        }
        writeSessions(sessions)
    }

    override fun consume(state: String): String? {
        val sessions = readSessions().toMutableMap()
        val verifier = sessions.remove(state) ?: return null
        writeSessions(sessions)
        return verifier
    }

    override fun clear() {
        localStorage.removeItem(KEY_SESSIONS)
        localStorage.removeItem(KEY_VERIFIER_LEGACY)
        localStorage.removeItem(KEY_STATE_LEGACY)
    }

    private fun readSessions(): Map<String, String> {
        val raw = localStorage.getItem(KEY_SESSIONS)
        if (raw != null) {
            return parseSessionsJson(raw)
        }
        val legacyVerifier = localStorage.getItem(KEY_VERIFIER_LEGACY)
        val legacyState = localStorage.getItem(KEY_STATE_LEGACY)
        if (legacyVerifier != null && legacyState != null) {
            val migrated = mapOf(legacyState to legacyVerifier)
            writeSessions(migrated)
            localStorage.removeItem(KEY_VERIFIER_LEGACY)
            localStorage.removeItem(KEY_STATE_LEGACY)
            return migrated
        }
        return emptyMap()
    }

    private fun writeSessions(sessions: Map<String, String>) {
        if (sessions.isEmpty()) {
            localStorage.removeItem(KEY_SESSIONS)
            return
        }
        localStorage.setItem(KEY_SESSIONS, sessionsToJson(sessions))
    }
}

/** Minimal JSON object `{"state":"verifier",...}` — values are URL-safe PKCE strings. */
internal fun sessionsToJson(sessions: Map<String, String>): String =
    sessions.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
        "\"${escapeJsonString(k)}\":\"${escapeJsonString(v)}\""
    }

internal fun parseSessionsJson(raw: String): Map<String, String> {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return emptyMap()
    val body = trimmed.substring(1, trimmed.lastIndex).trim()
    if (body.isEmpty()) return emptyMap()
    val out = linkedMapOf<String, String>()
    var i = 0
    while (i < body.length) {
        while (i < body.length && (body[i] == ',' || body[i].isWhitespace())) i++
        if (i >= body.length) break
        if (body[i] != '"') break
        val keyEnd = findClosingQuote(body, i + 1) ?: break
        val key = unescapeJsonString(body.substring(i + 1, keyEnd))
        i = keyEnd + 1
        while (i < body.length && body[i].isWhitespace()) i++
        if (i >= body.length || body[i] != ':') break
        i++
        while (i < body.length && body[i].isWhitespace()) i++
        if (i >= body.length || body[i] != '"') break
        val valEnd = findClosingQuote(body, i + 1) ?: break
        val value = unescapeJsonString(body.substring(i + 1, valEnd))
        out[key] = value
        i = valEnd + 1
    }
    return out
}

private fun findClosingQuote(
    s: String,
    from: Int,
): Int? {
    var i = from
    while (i < s.length) {
        when (s[i]) {
            '\\' -> i += 2
            '"' -> return i
            else -> i++
        }
    }
    return null
}

private fun escapeJsonString(value: String): String =
    buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
    }

private fun unescapeJsonString(value: String): String =
    buildString(value.length) {
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> append('\\')
                    '"' -> append('"')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    else -> append(value[i + 1])
                }
                i += 2
            } else {
                append(ch)
                i++
            }
        }
    }

class WasmGatewayHttpClient : GatewayHttpClient {
    override suspend fun get(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request(method = "GET", url = url, headers = headers, bodyBase64 = null)

    override suspend fun postForm(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse =
        request(
            method = "POST",
            url = url,
            headers = headers,
            bodyBase64 = Base64Std.encode(body.encodeToByteArray()),
        )

    override suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse =
        request(
            method = "PUT",
            url = url,
            headers = headers,
            bodyBase64 = Base64Std.encode(body.encodeToByteArray()),
        )

    override suspend fun patch(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse =
        request(
            method = "PATCH",
            url = url,
            headers = headers,
            bodyBase64 = Base64Std.encode(body.encodeToByteArray()),
        )

    override suspend fun postBytes(
        url: String,
        body: ByteArray,
        headers: Map<String, String>,
    ): GatewayHttpResponse =
        request(
            method = "POST",
            url = url,
            headers = headers,
            bodyBase64 = Base64Std.encode(body),
        )

    override suspend fun delete(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request(method = "DELETE", url = url, headers = headers, bodyBase64 = null)

    private suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        bodyBase64: String?,
    ): GatewayHttpResponse =
        suspendCoroutine { cont ->
            val headerJson =
                headers.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
                    "\"${escapeJs(k)}\":\"${escapeJs(v)}\""
                }
            fetchHttpJs(
                method = method,
                url = url,
                headerJson = headerJson,
                bodyBase64 = bodyBase64 ?: "",
                onSuccess = { payload ->
                    val nl = payload.indexOf('\n')
                    if (nl < 0) {
                        cont.resumeWithException(Exception("Malformed fetch payload"))
                    } else {
                        val status = payload.substring(0, nl).toIntOrNull() ?: 0
                        val text = payload.substring(nl + 1)
                        cont.resume(GatewayHttpResponse(status = status, body = text))
                    }
                },
                onFailure = { message ->
                    cont.resumeWithException(Exception(message))
                },
            )
        }

    private fun escapeJs(value: String): String =
        buildString(value.length) {
            for (ch in value) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    else -> append(ch)
                }
            }
        }
}

@JsFun(
    """
    (method, url, headerJson, bodyBase64, onSuccess, onFailure) => {
      (async () => {
        try {
          const headers = JSON.parse(headerJson);
          const init = { method: method, headers: headers };
          if (method !== 'GET' && method !== 'HEAD' && bodyBase64 !== '') {
            const binary = atob(bodyBase64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
              bytes[i] = binary.charCodeAt(i);
            }
            init.body = bytes;
          }
          const response = await fetch(url, init);
          const text = await response.text();
          onSuccess(String(response.status) + '\n' + text);
        } catch (e) {
          onFailure(String(e));
        }
      })();
    }
    """,
)
private external fun fetchHttpJs(
    method: String,
    url: String,
    headerJson: String,
    bodyBase64: String,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
)
