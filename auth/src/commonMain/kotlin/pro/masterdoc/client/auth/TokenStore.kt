package pro.masterdoc.client.auth

/**
 * Platform token persistence (browser localStorage / in-memory elsewhere).
 */
interface TokenStore {
    fun read(): AuthTokens?

    fun write(tokens: AuthTokens)

    fun clear()
}

/**
 * Survives the OIDC round-trip (code_verifier + state).
 *
 * Multiple in-flight logins are kept so a second [save] does not invalidate
 * an earlier authorize → callback pair.
 */
interface PkceSessionStore {
    fun save(
        verifier: String,
        state: String,
    )

    /** Remove and return the verifier for [state], or null if unknown. */
    fun consume(state: String): String?

    fun clear()
}

class InMemoryTokenStore : TokenStore {
    private var tokens: AuthTokens? = null

    override fun read(): AuthTokens? = tokens

    override fun write(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override fun clear() {
        tokens = null
    }
}

class InMemoryPkceSessionStore : PkceSessionStore {
    private val sessions = linkedMapOf<String, String>()

    override fun save(
        verifier: String,
        state: String,
    ) {
        sessions[state] = verifier
        while (sessions.size > MAX_SESSIONS) {
            sessions.remove(sessions.keys.first())
        }
    }

    override fun consume(state: String): String? = sessions.remove(state)

    override fun clear() {
        sessions.clear()
    }

    companion object {
        const val MAX_SESSIONS = 8
    }
}
