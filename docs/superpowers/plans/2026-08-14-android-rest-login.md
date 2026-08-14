# Android REST login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android logs in via in-app email/password → `POST /auth/login` (Zitadel Session API BFF) without opening a browser, so RuStore moderation passes.

**Architecture:** Gateway creates PKCE + authorize authRequest, Session API password check, finalizes OIDC auth request, exchanges code for tokens; Android shows `LoginScreen` and stores the same JWT shape as today’s `/auth/token`. Web keeps browser OIDC.

**Tech Stack:** Ktor gateway (Kotlin), Zitadel Session/OIDC APIs, KMP Compose client (`client-app`), OkHttp Android HTTP.

**Spec:** `client-app/docs/superpowers/specs/2026-08-14-android-rest-login-design.md`

## Global Constraints

- Android only for password UI; web/Wasm OIDC unchanged.
- Passwords never stored in our DB; only forwarded to Zitadel Session API.
- Client talks only to `api.masterdoc.pro` (no direct IdP calls from the device for login).
- UI: names not IDs (existing product rule; auth errors are generic).
- CI builds on GitHub — no heavy local Gradle/Docker builds; tiny unit tests OK.
- After ship: commit → push → watch Actions → `/smoke-test` (Android REST login path).

---

### Task 1: Gateway — `ZitadelLoginClient` + `POST /auth/login` (TDD)

**Repo:** `api-gateway-service`

**Files:**
- Create: `src/main/kotlin/pro/masterdoc/gateway/ZitadelLoginClient.kt`
- Create: `src/main/kotlin/pro/masterdoc/gateway/AuthLoginRoutes.kt`
- Create: `src/test/kotlin/pro/masterdoc/gateway/AuthLoginRoutesTest.kt`
- Modify: `src/main/kotlin/pro/masterdoc/gateway/Application.kt` (wire deps + install routes)
- Modify: `src/main/kotlin/pro/masterdoc/gateway/GatewayConfig.kt` — add `nativeRedirectUri` default `masterdoc://auth/callback` and `oidcScopes` default matching client
- Modify: `openapi.yaml`, `docs/AUTH.md`, `docs/SECRETS_AND_DOMAINS.md` (IAM_LOGIN_CLIENT note)

**Interfaces:**
- Consumes: `ZitadelTokenClient.exchange(formBody)`, `GatewayConfig.zitadelIssuer`, `GatewayConfig.zitadelMgmtToken`
- Produces: `POST /auth/login` JSON in → token JSON out; `fun interface ZitadelLoginClient` for tests

- [ ] **Step 1: Write failing route tests**

Create `AuthLoginRoutesTest.kt`:

```kotlin
package pro.masterdoc.gateway

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthLoginRoutesTest {
    @Test
    fun `POST auth login returns tokens on success`() = testApplication {
        application {
            module(
                GatewayConfig.testDefaults(),
                GatewayDeps(
                    featureClient = FeatureServiceClient { error("unused") },
                    backendClient = BackendProxyClient { _, _, _, _ -> error("unused") },
                    tokenValidator = TokenValidator.rejecting(),
                    zitadelTokenClient =
                        ZitadelTokenClient {
                            UpstreamResult(
                                HttpStatusCode.OK,
                                "application/json",
                                """{"access_token":"at","refresh_token":"rt","token_type":"Bearer","expires_in":3600,"id_token":"id"}"""
                                    .toByteArray(),
                            )
                        },
                    zitadelLoginClient =
                        ZitadelLoginClient { _, _, _ ->
                            ZitadelLoginResult.Code(
                                code = "auth-code",
                                codeVerifier = "verifier",
                                redirectUri = "masterdoc://auth/callback",
                            )
                        },
                ),
            )
        }
        val response =
            client.post("/auth/login") {
                setBody(
                    TextContent(
                        """{"email":"a@b.c","password":"secret","client_id":"native"}""",
                        ContentType.Application.Json,
                    ),
                )
            }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("at", body["access_token"]!!.jsonPrimitive.content)
        assertEquals("rt", body["refresh_token"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST auth login blank fields returns 400`() = testApplication {
        application {
            module(
                GatewayConfig.testDefaults(),
                GatewayDeps(
                    featureClient = FeatureServiceClient { error("unused") },
                    backendClient = BackendProxyClient { _, _, _, _ -> error("unused") },
                    tokenValidator = TokenValidator.rejecting(),
                ),
            )
        }
        val response =
            client.post("/auth/login") {
                setBody(
                    TextContent(
                        """{"email":"","password":"x","client_id":"native"}""",
                        ContentType.Application.Json,
                    ),
                )
            }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST auth login invalid credentials returns 401`() = testApplication {
        application {
            module(
                GatewayConfig.testDefaults(),
                GatewayDeps(
                    featureClient = FeatureServiceClient { error("unused") },
                    backendClient = BackendProxyClient { _, _, _, _ -> error("unused") },
                    tokenValidator = TokenValidator.rejecting(),
                    zitadelLoginClient =
                        ZitadelLoginClient { _, _, _ ->
                            ZitadelLoginResult.InvalidCredentials
                        },
                ),
            )
        }
        val response =
            client.post("/auth/login") {
                setBody(
                    TextContent(
                        """{"email":"a@b.c","password":"bad","client_id":"native"}""",
                        ContentType.Application.Json,
                    ),
                )
            }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
```

Adjust imports/constructors to match existing `GatewayDeps` / `GatewayConfig.testDefaults()` patterns in sibling tests.

- [ ] **Step 2: Run tests — expect FAIL (route missing)**

Run (from `api-gateway-service`): `./gradlew test --tests pro.masterdoc.gateway.AuthLoginRoutesTest`  
Expected: compile/fail — `/auth/login` or types missing.

- [ ] **Step 3: Implement client + route**

`ZitadelLoginClient.kt` (sketch):

```kotlin
sealed interface ZitadelLoginResult {
    data class Code(val code: String, val codeVerifier: String, val redirectUri: String) : ZitadelLoginResult
    data object InvalidCredentials : ZitadelLoginResult
}

fun interface ZitadelLoginClient {
    suspend fun loginWithPassword(email: String, password: String, clientId: String): ZitadelLoginResult

    companion object {
        fun http(config: GatewayConfig): ZitadelLoginClient = HttpZitadelLoginClient(config)
        fun unconfigured(): ZitadelLoginClient =
            ZitadelLoginClient { _, _, _ -> error("Zitadel login client not configured") }
    }
}
```

`HttpZitadelLoginClient`:
1. Generate PKCE verifier/challenge + state (copy algorithm from client `Pkce` or minimal SHA-256 base64url).
2. `GET {issuer}/oauth/v2/authorize?...` with `HttpClient` `followRedirects=false`; parse `authRequest` (or `id`) from `Location`.
3. `POST {issuer}/v2/sessions` with Bearer `zitadelMgmtToken`, body:
   `{"checks":{"user":{"loginName":email},"password":{"password":password}}}`
   — non-2xx with auth failure → `InvalidCredentials`.
4. `POST {issuer}/v2/oidc/auth_requests/{authRequestId}` with session id/token → parse `callbackUrl` → extract `code` query param.
5. Return `ZitadelLoginResult.Code`.

`AuthLoginRoutes.kt`: receive JSON `AuthLoginRequest(email, password, clientId)`; validate blank → 400; call login client; on Code build form for `grant_type=authorization_code` and `zitadelTokenClient.exchange`; proxy bytes; on InvalidCredentials → 401 JSON/text; on `UpstreamUnavailableException` → 502.

Wire `zitadelLoginClient` into `GatewayDeps.live`.

- [ ] **Step 4: Run tests — expect PASS**

`./gradlew test --tests pro.masterdoc.gateway.AuthLoginRoutesTest`

- [ ] **Step 5: Update OpenAPI + AUTH.md + SECRETS note**

Document `POST /auth/login`, IAM_LOGIN_CLIENT requirement for `ZITADEL_MGMT_TOKEN`.

- [ ] **Step 6: Commit + push (api-gateway-service)**

```bash
git add src openapi.yaml docs
git commit -m "$(cat <<'EOF'
feat(auth): add POST /auth/login via Zitadel Session API

EOF
)"
git push
```

Watch `gh run watch` until success (deploy).

---

### Task 2: Docs — Zitadel AUTHORIZATION.md

**Repo:** `masterdoc-zitadel` (if git present; else skip commit and only edit if tree is a repo)

**Files:**
- Modify: `docs/AUTHORIZATION.md` — passwords row + Android BFF note

- [ ] **Step 1: Update canon text**

Replace «не делает `POST /auth/login`» with: gateway may expose `POST /auth/login` for Android; passwords stay in Zitadel; Session API + OIDC tokens.

- [ ] **Step 2: Commit + push if this is a separate git repo**

---

### Task 3: Client — `loginWithPassword` in auth module (TDD)

**Repo:** `client-app`

**Files:**
- Modify: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AuthModels.kt` — `AuthLoginRequest`
- Modify: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AuthRepository.kt`
- Modify: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AuthCoordinator.kt`
- Modify: `auth/src/jvmTest/kotlin/pro/masterdoc/client/auth/AuthRepositoryTest.kt`

**Interfaces:**
- Consumes: `GatewayHttpClient.postBytes`, `AuthConfig.clientId` / `gatewayBaseUrl`
- Produces: `suspend fun loginWithPassword(email: String, password: String): AuthTokens`; `AuthCoordinator.loginWithPassword` → `MeResponse`

- [ ] **Step 1: Failing tests**

```kotlin
@Test
fun loginWithPassword_postsJsonAndPersistsTokens() =
    runBlocking {
        val http =
            FakeGatewayHttpClient { method, url, headers, body ->
                assertEquals("POST", method)
                assertTrue(url.endsWith("/auth/login"))
                assertEquals("application/json", headers["Content-Type"])
                assertTrue(body!!.decodeToString().contains("\"email\":\"a@b.c\""))
                GatewayHttpResponse(
                    200,
                    """{"access_token":"at","refresh_token":"rt","id_token":"id","token_type":"Bearer"}""",
                )
            }
        val tokens = InMemoryTokenStore()
        val repo =
            AuthRepository(
                config = AuthConfig(clientId = "native-client"),
                http = http,
                tokenStore = tokens,
                pkceStore = InMemoryPkceSessionStore(),
            )
        val result = repo.loginWithPassword("a@b.c", "secret")
        assertEquals("at", result.accessToken)
        assertEquals("at", tokens.read()?.accessToken)
    }
```

Extend `FakeGatewayHttpClient` if needed so `postBytes` hits the same lambda (mirror existing fake at bottom of `AuthRepositoryTest.kt`).

- [ ] **Step 2: Run test — FAIL**

`./gradlew :auth:jvmTest --tests pro.masterdoc.client.auth.AuthRepositoryTest.loginWithPassword_postsJsonAndPersistsTokens`  
(or project’s equivalent; if local Gradle discouraged, write test + impl together and rely on CI — prefer running single jvmTest if fast).

- [ ] **Step 3: Implement**

```kotlin
@Serializable
data class AuthLoginRequest(
    val email: String,
    val password: String,
    @SerialName("client_id") val clientId: String,
)
```

```kotlin
// AuthRepository
suspend fun loginWithPassword(email: String, password: String): AuthTokens {
    val payload =
        json.encodeToString(
            AuthLoginRequest(
                email = email.trim(),
                password = password,
                clientId = config.clientId,
            ),
        )
    val response =
        http.postBytes(
            url = "${config.gatewayBaseUrl.trimEnd('/')}/auth/login",
            body = payload.encodeToByteArray(),
            headers = mapOf("Content-Type" to "application/json"),
        )
    if (!response.isSuccessful) {
        throw GatewayHttpException(response.status, "Login failed: ${response.body}")
    }
    val tokens = AuthTokens.from(json.decodeFromString<TokenResponse>(response.body))
    tokenStore.write(tokens)
    return tokens
}
```

```kotlin
// AuthCoordinator
suspend fun loginWithPassword(email: String, password: String): MeResponse {
    authRepository.loginWithPassword(email, password)
    return meRepository.getMe()
}
```

- [ ] **Step 4: Tests PASS**

- [ ] **Step 5: Commit** (client-app; push can wait until Task 4 if same session continues)

---

### Task 4: Android — native client id + LoginScreen + bootstrap

**Repo:** `client-app`

**Files:**
- Modify: `composeApp/build.gradle.kts` — generate `NATIVE_CLIENT_ID`
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/AppAuthConfig.kt` — expect/actual or platform client id
- Create: `composeApp/src/androidMain/kotlin/pro/masterdoc/client/ui/LoginScreen.kt` (or `commonMain` if preferred, used only from Android bootstrap)
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/App.kt` — `AuthenticatedApp` Android branch
- Modify: `local.properties.example` + CI secrets docs if any for `FIXAVERSE_OIDC_NATIVE_CLIENT_ID`

**Interfaces:**
- Consumes: `AuthCoordinator.loginWithPassword`, `AuthCoordinator.logout`, `AuthCoordinator.hasSession`
- Produces: cold start without `BrowserNav.navigateTo(authorizeUrl)` on Android

- [ ] **Step 1: Native client id in GeneratedAuthDefaults**

```kotlin
// generateAuthDefaults doLast
internal object GeneratedAuthDefaults {
    const val WEB_CLIENT_ID: String = "$oidcWebClientId"
    const val NATIVE_CLIENT_ID: String = "$oidcNativeClientId"
}
```

```kotlin
val oidcNativeClientId: String =
    (findProperty("fixaverse.oidc.nativeClientId") as String?)
        ?: System.getenv("FIXAVERSE_OIDC_NATIVE_CLIENT_ID")
        ?: "unset-native-client-id"
```

Android `appAuthConfig` / `platformAuthClientId()` returns `NATIVE_CLIENT_ID`; web/desktop keep `WEB_CLIENT_ID`.

- [ ] **Step 2: LoginScreen UI**

Use `AppTextField`, `AppButton`, `AppText`, `ClientTheme`. Fields: email, password; primary «Войти»; show error string; disable button while loading.

- [ ] **Step 3: Bootstrap branch**

Introduce `expect fun usesInAppPasswordLogin(): Boolean` — android `true`, others `false`.

In `bootstrap` / `AuthenticatedApp` when `usesInAppPasswordLogin()`:
- no session → `ShellUiState.Login` (new state) rendering `LoginScreen`
- on submit → `coordinator.loginWithPassword` → Ready
- logout → clear + Login state (not `logoutRedirectUrl`)

When false: existing OIDC flow.

- [ ] **Step 4: Ensure Android `navigateTo` is not called for authorize on cold start**

Grep: no `startLoginOrError` path on Android password mode.

- [ ] **Step 5: Commit + push client-app; watch CI / Android release pipeline as applicable**

---

### Task 5: Ops verify + smoke

**Files:** runbooks only if gaps found

- [ ] **Step 1:** Confirm production `ZITADEL_MGMT_TOKEN` can call Session + finalize (IAM_LOGIN_CLIENT). If 403 in gateway logs after deploy — grant role or rotate PAT from login-client; document.

- [ ] **Step 2:** After green deploy of gateway + client Android artifact: smoke-test skill — login as RuStore test user without browser redirect; screenshot login form + home.

- [ ] **Step 3:** Final report PASS/FAIL with org + URL/APK notes.

---

## Self-review (plan vs spec)

| Spec item | Task |
|-----------|------|
| `POST /auth/login` Session BFF | Task 1 |
| OpenAPI / AUTH.md / AUTHORIZATION | Task 1–2 |
| Android LoginScreen, no browser | Task 4 |
| `loginWithPassword` client | Task 3 |
| Web unchanged | Task 4 expect/actual |
| Native client_id | Task 4 |
| IAM_LOGIN_CLIENT ops | Task 1 docs + Task 5 |
| Refresh unchanged | (no task — existing) |
| Smoke / RuStore | Task 5 |
