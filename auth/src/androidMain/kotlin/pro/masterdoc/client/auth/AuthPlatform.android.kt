package pro.masterdoc.client.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

actual object BrowserNav {
    actual fun currentPath(): String {
        val uri = callbackUri ?: return "/"
        return if (uri.scheme == NATIVE_REDIRECT_SCHEME && uri.host == NATIVE_REDIRECT_HOST) {
            "/$NATIVE_REDIRECT_HOST${uri.path.orEmpty()}"
        } else {
            uri.path ?: "/"
        }
    }

    actual fun currentSearch(): String =
        callbackUri?.encodedQuery?.let { "?$it" }.orEmpty()

    actual fun currentHash(): String = ""

    actual fun setHash(hash: String) = Unit

    actual fun navigateTo(url: String) {
        val activity = hostActivity ?: return
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    actual fun replaceTo(url: String) {
        callbackUri = null
        hostActivity?.let { activity ->
            activity.intent = Intent(activity.intent).setData(null)
        }
    }
}

/**
 * Must be called before creating the auth Koin module so browser navigation and
 * persistent PKCE state survive the external OIDC redirect.
 */
fun configureAndroidAuthPlatform(activity: Activity) {
    hostActivity = activity
    callbackUri = activity.intent?.data
}

actual fun createDefaultTokenStore(): TokenStore =
    AndroidTokenStore(requireNotNull(hostActivity) { "Configure Android auth before creating TokenStore" })

actual fun createDefaultPkceSessionStore(): PkceSessionStore =
    AndroidPkceSessionStore(requireNotNull(hostActivity) { "Configure Android auth before creating PkceSessionStore" })

actual fun createDefaultGatewayHttpClient(): GatewayHttpClient = AndroidGatewayHttpClient()

private class AndroidTokenStore(context: Context) : TokenStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): AuthTokens? {
        val access = preferences.getString(KEY_ACCESS, null) ?: return null
        return AuthTokens(
            accessToken = access,
            refreshToken = preferences.getString(KEY_REFRESH, null),
            idToken = preferences.getString(KEY_ID, null),
        )
    }

    override fun write(tokens: AuthTokens) {
        preferences.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putString(KEY_ID, tokens.idToken)
            .commit()
    }

    override fun clear() {
        preferences.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_ID)
            .commit()
    }
}

private class AndroidPkceSessionStore(context: Context) : PkceSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun save(
        verifier: String,
        state: String,
    ) {
        val states = preferences.getStringSet(KEY_PKCE_STATES, emptySet()).orEmpty().toMutableSet()
        states += state
        while (states.size > InMemoryPkceSessionStore.MAX_SESSIONS) {
            states.remove(states.first())
        }
        preferences.edit()
            .putString("$KEY_PKCE_PREFIX$state", verifier)
            .putStringSet(KEY_PKCE_STATES, states)
            .commit()
    }

    override fun consume(state: String): String? {
        val verifier = preferences.getString("$KEY_PKCE_PREFIX$state", null) ?: return null
        val states = preferences.getStringSet(KEY_PKCE_STATES, emptySet()).orEmpty().toMutableSet()
        states.remove(state)
        preferences.edit()
            .remove("$KEY_PKCE_PREFIX$state")
            .putStringSet(KEY_PKCE_STATES, states)
            .commit()
        return verifier
    }

    override fun clear() {
        val editor = preferences.edit()
        preferences.getStringSet(KEY_PKCE_STATES, emptySet()).orEmpty()
            .forEach { state -> editor.remove("$KEY_PKCE_PREFIX$state") }
        editor.remove(KEY_PKCE_STATES).commit()
    }
}

private class AndroidGatewayHttpClient(
    private val client: OkHttpClient = OkHttpClient(),
) : GatewayHttpClient {
    override suspend fun get(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request("GET", url, headers)

    override suspend fun postForm(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request("POST", url, headers, body.encodeToByteArray())

    override suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request("PUT", url, headers, body.encodeToByteArray())

    override suspend fun patch(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request("PATCH", url, headers, body.encodeToByteArray())

    override suspend fun postBytes(
        url: String,
        body: ByteArray,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request("POST", url, headers, body)

    override suspend fun delete(
        url: String,
        headers: Map<String, String>,
    ): GatewayHttpResponse = request("DELETE", url, headers)

    private suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: ByteArray? = null,
    ): GatewayHttpResponse =
        withContext(Dispatchers.IO) {
            val contentType = headers["Content-Type"]?.toMediaTypeOrNull()
            val requestBody = body?.toRequestBody(contentType)
            val request =
                Request.Builder()
                    .url(url)
                    .apply { headers.forEach { (name, value) -> header(name, value) } }
                    .method(method, requestBody)
                    .build()
            client.newCall(request).execute().use { response ->
                GatewayHttpResponse(
                    status = response.code,
                    body = response.body.string(),
                )
            }
        }
}

private var hostActivity: Activity? = null
private var callbackUri: Uri? = null

private const val NATIVE_REDIRECT_SCHEME = "masterdoc"
private const val NATIVE_REDIRECT_HOST = "auth"
private const val PREFERENCES_NAME = "fixaverse.auth"
private const val KEY_ACCESS = "access_token"
private const val KEY_REFRESH = "refresh_token"
private const val KEY_ID = "id_token"
private const val KEY_PKCE_STATES = "pkce_states"
private const val KEY_PKCE_PREFIX = "pkce_verifier."
