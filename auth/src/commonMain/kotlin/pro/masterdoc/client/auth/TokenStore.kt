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
 */
interface PkceSessionStore {
    fun save(verifier: String, state: String)

    fun readVerifier(): String?

    fun readState(): String?

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
    private var verifier: String? = null
    private var state: String? = null

    override fun save(verifier: String, state: String) {
        this.verifier = verifier
        this.state = state
    }

    override fun readVerifier(): String? = verifier

    override fun readState(): String? = state

    override fun clear() {
        verifier = null
        state = null
    }
}
