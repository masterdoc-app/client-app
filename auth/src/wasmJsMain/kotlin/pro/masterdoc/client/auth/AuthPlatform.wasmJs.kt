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
private const val KEY_VERIFIER = "fixaverse.auth.pkce_verifier"
private const val KEY_STATE = "fixaverse.auth.pkce_state"

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
        localStorage.setItem(KEY_VERIFIER, verifier)
        localStorage.setItem(KEY_STATE, state)
    }

    override fun readVerifier(): String? = localStorage.getItem(KEY_VERIFIER)

    override fun readState(): String? = localStorage.getItem(KEY_STATE)

    override fun clear() {
        localStorage.removeItem(KEY_VERIFIER)
        localStorage.removeItem(KEY_STATE)
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
