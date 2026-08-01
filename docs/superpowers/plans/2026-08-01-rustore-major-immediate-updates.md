# RuStore major IMMEDIATE updates + first publish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish `client-app` (`pro.masterdoc.client`) to RuStore and force IMMEDIATE in-app updates when the available major version is higher than installed; use SILENT for minor/patch.

**Architecture:** Pure Kotlin policy (`versionCode` major math + update type selection) in common/JVM-testable code; Android-only RuStore App Update SDK wrapper wired from `MainActivity`. Release pipeline copied from KkalScan (`rustore-release.yml` + `rustore_publish.py` + `store/rustore/`), package swapped to `pro.masterdoc.client`.

**Tech Stack:** KMP Compose (`client-app`), RuStore SDK `ru.rustore.sdk:appupdate:10.5.0`, GitHub Actions, Python RuStore Public API (KkalScan script), release keystore via secrets.

**Reference (copy patterns):** `/Users/antonbutov/Documents/MYPROJECTS/kkalscan/mobile` — signing, workflow, `scripts/rustore_publish.py`, `store/rustore/`. KkalScan has **no** in-app update SDK; that part is new for client-app.

**Spec:** `docs/superpowers/specs/2026-08-01-rustore-major-immediate-updates-design.md`

## Global Constraints

- Package: `pro.masterdoc.client`
- Version scheme (align with KkalScan, amends design's `*1_000_000`): `versionName=X.Y.Z` → `versionCode = X*10000 + Y*100 + Z`; major = `versionCode / 10000`
- First store version: `VERSION_NAME=1.0.0`, `VERSION_CODE=10000`
- Major↑ → IMMEDIATE; else → SILENT; IMMEDIATE cancel → `finish()`
- No local heavy Gradle release builds in agent session — AAB on GitHub Actions
- UI copy: names not raw IDs
- Do not commit keystores or RuStore private keys

## File map

| Path | Role |
|------|------|
| `gradle.properties` | `VERSION_NAME` / `VERSION_CODE` SSOT |
| `composeApp/build.gradle.kts` | versions from properties, release signing, RuStore dep |
| `settings.gradle.kts` | VK Partner Maven for RuStore SDK |
| `gradle/libs.versions.toml` | `rustore-appupdate` version alias |
| `composeApp/src/commonMain/.../update/AppUpdatePolicy.kt` | major + type selection (pure) |
| `composeApp/src/commonTest/.../update/AppUpdatePolicyTest.kt` | unit tests |
| `composeApp/src/androidMain/.../update/RuStoreAppUpdater.kt` | SDK wrapper |
| `composeApp/src/androidMain/.../MainActivity.kt` | call updater onCreate/onResume |
| `scripts/rustore_publish.py` | copy from KkalScan, default package |
| `scripts/requirements-rustore.txt` | `pycryptodome` |
| `.github/workflows/rustore-release.yml` | signed AAB + publish |
| `store/rustore/copy.md` + icon/screenshots + README | listing assets |
| Spec + short README note | version scheme amendment + release how-to |

---

### Task 1: Version SSOT + `AppUpdatePolicy` (TDD)

**Files:**
- Modify: `gradle.properties`
- Modify: `composeApp/build.gradle.kts` (defaultConfig version from properties)
- Modify: `docs/superpowers/specs/2026-08-01-rustore-major-immediate-updates-design.md` (versionCode formula → KkalScan)
- Create: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/update/AppUpdatePolicy.kt`
- Create: `composeApp/src/commonTest/kotlin/pro/masterdoc/client/update/AppUpdatePolicyTest.kt`

**Interfaces:**
- Produces:
  - `enum class AppUpdateFlow { Immediate, Silent }`
  - `fun majorFromVersionCode(versionCode: Int): Int` → `versionCode / 10_000`
  - `fun versionCodeFromSemVer(major: Int, minor: Int, patch: Int): Int` → `major*10000 + minor*100 + patch`
  - `fun selectUpdateFlow(installedVersionCode: Int, availableVersionCode: Int): AppUpdateFlow?` — `null` if `available <= installed`; Immediate if available major > installed major; else Silent

- [ ] **Step 1: Write failing tests**

```kotlin
package pro.masterdoc.client.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppUpdatePolicyTest {
    @Test
    fun versionCodeFromSemVer_matchesKkalScanScheme() {
        assertEquals(10000, versionCodeFromSemVer(1, 0, 0))
        assertEquals(10020, versionCodeFromSemVer(1, 0, 20))
        assertEquals(20103, versionCodeFromSemVer(2, 1, 3))
    }

    @Test
    fun majorFromVersionCode_dividesBy10000() {
        assertEquals(1, majorFromVersionCode(10020))
        assertEquals(2, majorFromVersionCode(20000))
    }

    @Test
    fun selectUpdateFlow_nullWhenNoNewer() {
        assertNull(selectUpdateFlow(10000, 10000))
        assertNull(selectUpdateFlow(10020, 10000))
    }

    @Test
    fun selectUpdateFlow_immediateOnMajorBump() {
        assertEquals(AppUpdateFlow.Immediate, selectUpdateFlow(10020, 20000))
    }

    @Test
    fun selectUpdateFlow_silentOnMinorOrPatch() {
        assertEquals(AppUpdateFlow.Silent, selectUpdateFlow(10000, 10100))
        assertEquals(AppUpdateFlow.Silent, selectUpdateFlow(10000, 10001))
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `./gradlew :composeApp:desktopTest --tests "pro.masterdoc.client.update.AppUpdatePolicyTest"`  
(or the project’s usual JVM test task if desktopTest is not the commonTest host — use the same task CI uses for JVM tests in `deploy-app-fixaverse.yml`)  
Expected: compilation failure / unresolved references

- [ ] **Step 3: Implement policy + wire versions**

`AppUpdatePolicy.kt`:

```kotlin
package pro.masterdoc.client.update

enum class AppUpdateFlow { Immediate, Silent }

fun versionCodeFromSemVer(major: Int, minor: Int, patch: Int): Int =
    major * 10_000 + minor * 100 + patch

fun majorFromVersionCode(versionCode: Int): Int = versionCode / 10_000

fun selectUpdateFlow(installedVersionCode: Int, availableVersionCode: Int): AppUpdateFlow? {
    if (availableVersionCode <= installedVersionCode) return null
    return if (majorFromVersionCode(availableVersionCode) > majorFromVersionCode(installedVersionCode)) {
        AppUpdateFlow.Immediate
    } else {
        AppUpdateFlow.Silent
    }
}
```

In `gradle.properties` add:

```properties
VERSION_NAME=1.0.0
VERSION_CODE=10000
```

In `composeApp/build.gradle.kts` `defaultConfig`:

```kotlin
versionCode = (findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 10000
versionName = findProperty("VERSION_NAME") as String? ?: "1.0.0"
```

Amend design spec table: formula `X*10000+Y*100+Z`, first release `1.0.0` / `10000`, major = `versionCode/10000`.

- [ ] **Step 4: Run tests — expect PASS**

Same Gradle command as Step 2. Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gradle.properties composeApp/build.gradle.kts \
  composeApp/src/commonMain/kotlin/pro/masterdoc/client/update/AppUpdatePolicy.kt \
  composeApp/src/commonTest/kotlin/pro/masterdoc/client/update/AppUpdatePolicyTest.kt \
  docs/superpowers/specs/2026-08-01-rustore-major-immediate-updates-design.md
git commit -m "feat(android): AppUpdatePolicy and SemVer versionCode SSOT"
```

---

### Task 2: RuStore SDK dependency + Android updater

**Files:**
- Modify: `settings.gradle.kts` — add VK Partner Maven in `dependencyResolutionManagement.repositories`
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts` — `androidMain` dep + release `signingConfigs` (env-based, KkalScan pattern)
- Create: `composeApp/src/androidMain/kotlin/pro/masterdoc/client/update/RuStoreAppUpdater.kt`
- Modify: `composeApp/src/androidMain/kotlin/pro/masterdoc/client/MainActivity.kt`

**Interfaces:**
- Consumes: `selectUpdateFlow`, `AppUpdateFlow`
- Produces: `class RuStoreAppUpdater(activity: ComponentActivity)` with `fun checkAndStart()` — safe no-op on SDK failure

- [ ] **Step 1: Add Maven + library**

`settings.gradle.kts` inside `dependencyResolutionManagement.repositories` after `mavenCentral()`:

```kotlin
maven {
    url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven")
}
```

`libs.versions.toml`:

```toml
# [versions]
rustoreAppupdate = "10.5.0"

# [libraries]
rustore-appupdate = { module = "ru.rustore.sdk:appupdate", version.ref = "rustoreAppupdate" }
```

`composeApp/build.gradle.kts` `androidMain.dependencies`:

```kotlin
implementation(libs.rustore.appupdate)
```

Signing block (same as KkalScan `mobile/composeApp/build.gradle.kts`): `signingConfigs.release` from `ANDROID_KEYSTORE_*` env; `buildTypes.release.signingConfig` when path set. Also add empty `buildTypes { debug {}; release { isMinifyEnabled = false } }` if missing.

- [ ] **Step 2: Implement `RuStoreAppUpdater`**

Use RuStore 10.x API (`RuStoreAppUpdateManagerFactory`, `getAppUpdateInfo`, `startUpdateFlow`, `AppUpdateOptions`, `AppUpdateType.IMMEDIATE` / `SILENT`, `completeUpdate` for SILENT after `InstallStatus.DOWNLOADED`).

```kotlin
package pro.masterdoc.client.update

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability

class RuStoreAppUpdater(private val activity: ComponentActivity) {
    private val manager = RuStoreAppUpdateManagerFactory.create(activity)
    private var checking = false

    fun checkAndStart() {
        if (checking) return
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        checking = true
        val installed = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode.toInt()
        } catch (t: Throwable) {
            Log.w(TAG, "versionCode read failed", t)
            checking = false
            return
        }
        manager.getAppUpdateInfo()
            .addOnSuccessListener { info ->
                try {
                    if (info.updateAvailability != UpdateAvailability.UPDATE_AVAILABLE) {
                        return@addOnSuccessListener
                    }
                    val available = info.availableVersionCode
                    val flow = selectUpdateFlow(installed, available) ?: return@addOnSuccessListener
                    when (flow) {
                        AppUpdateFlow.Immediate -> startImmediate(info)
                        AppUpdateFlow.Silent -> startSilent(info)
                    }
                } finally {
                    checking = false
                }
            }
            .addOnFailureListener { t ->
                Log.i(TAG, "getAppUpdateInfo unavailable", t)
                checking = false
            }
    }

    private fun startImmediate(info: ru.rustore.sdk.appupdate.model.AppUpdateInfo) {
        val options = AppUpdateOptions.Builder().appUpdateType(AppUpdateType.IMMEDIATE).build()
        manager.startUpdateFlow(info, options)
            .addOnSuccessListener { resultCode ->
                if (resultCode == Activity.RESULT_CANCELED || resultCode == ACTIVITY_NOT_FOUND) {
                    activity.finish()
                }
            }
            .addOnFailureListener {
                activity.finish()
            }
    }

    private fun startSilent(info: ru.rustore.sdk.appupdate.model.AppUpdateInfo) {
        manager.registerListener { state ->
            if (state.installStatus == InstallStatus.DOWNLOADED) {
                val options = AppUpdateOptions.Builder().appUpdateType(AppUpdateType.SILENT).build()
                manager.completeUpdate(options)
            }
        }
        val options = AppUpdateOptions.Builder().appUpdateType(AppUpdateType.SILENT).build()
        manager.startUpdateFlow(info, options)
            .addOnFailureListener { t -> Log.i(TAG, "silent update failed", t) }
    }

    companion object {
        private const val TAG = "RuStoreAppUpdater"
        private const val ACTIVITY_NOT_FOUND = 2
    }
}
```

Adjust imports/method names if SDK 10.5 differs slightly — match [RuStore Kotlin docs 10.x](https://www.rustore.ru/help/sdk/updates/kotlin-java/10-2-0). Prefer compile-fixing against actual SDK types.

Wire in `MainActivity`:

```kotlin
private lateinit var appUpdater: RuStoreAppUpdater

override fun onCreate(...) {
    ...
    appUpdater = RuStoreAppUpdater(this)
    appUpdater.checkAndStart()
    setContent { ... }
}

override fun onResume() {
    super.onResume()
    if (::appUpdater.isInitialized) appUpdater.checkAndStart()
}
```

- [ ] **Step 3: Compile Android debug (CI will verify; locally only if cheap)**

Prefer push and let CI `:composeApp:assembleDebug` run. If running locally is required for TDD of compile:  
`./gradlew :composeApp:compileDebugKotlinAndroid` — only if user/environment allows; otherwise rely on CI after commit.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts gradle/libs.versions.toml composeApp/build.gradle.kts \
  composeApp/src/androidMain/kotlin/pro/masterdoc/client/update/RuStoreAppUpdater.kt \
  composeApp/src/androidMain/kotlin/pro/masterdoc/client/MainActivity.kt
git commit -m "feat(android): RuStore in-app updater with major IMMEDIATE"
```

---

### Task 3: Store listing assets + publish scripts

**Files:**
- Create: `store/rustore/copy.md` (Fixaverse engineer copy; sections matching KkalScan parser headings)
- Create: `store/rustore/README.md`
- Create: `store/rustore/console-checklist.md`
- Create: `store/rustore/icon-512.png` (export from existing app icon / design; 512×512 PNG)
- Create: `store/rustore/upload/` — at least 2 portrait screenshots 1080×1920 (from Wasm UI or emulator captures; no UUID labels)
- Create: `scripts/rustore_publish.py` — copy from KkalScan, change defaults:
  - `PACKAGE` default `pro.masterdoc.client`
  - categories: use `business` or `tools` (not `health`); `ageLegal`: `0+`
  - default whatsNew text for Fixaverse
- Create: `scripts/requirements-rustore.txt` with `pycryptodome`

**copy.md required sections** (parser in script):

```markdown
## Название (≤50 символов, ASO)

```
Fixaverse — заявки для инженера
```

## Краткое описание

```
Мобильный клиент инженера: заявки, карта, оборудование и ППР.
```

## Полное описание

```
Fixaverse помогает инженеру работать с заявками на объекте: список работ, карта коллег, оборудование и карты ППР.

Что внутри:
• Заявки и статусы работ
• Карта инженеров на линии
• Оборудование и документы
• ППР и связанные материалы

Требуется аккаунт организации Fixaverse.
```

## Комментарий модератору

```
Корпоративное приложение для инженеров обслуживающих организаций. Вход по корпоративной учётной записи.
```
```

In `load_listing_from_copy()` set `"categories": ["tools"]` (or `"business"` if tools rejected) and `"ageLegal": "0+"`.

- [ ] **Step 1: Create directory tree + copy.md + scripts**
- [ ] **Step 2: Add icon + ≥2 screenshots** (generate/capture; keep under `store/rustore/`)
- [ ] **Step 3: Commit**

```bash
git add store/rustore scripts/rustore_publish.py scripts/requirements-rustore.txt
git commit -m "chore(rustore): listing assets and publish script for client-app"
```

---

### Task 4: GitHub Actions `rustore-release` workflow

**Files:**
- Create: `.github/workflows/rustore-release.yml` — adapt from KkalScan; differences:
  - `RUSTORE_PACKAGE_NAME: pro.masterdoc.client`
  - Checkout `fixaverse-design` sibling (same as deploy workflow) before Gradle
  - Java 21 (match deploy workflow)
  - Artifact names `client-app-release-aab` / `client-app-release-apk`
  - Default whatsNew: `Fixaverse ${{ steps.version.outputs.name }}`
  - Keep `workflow_dispatch` + tag `v*` + release published

**Secrets required on `masterdoc-app/client-app`:**
- `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`
- `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` (same API key as KkalScan account is OK)

- [ ] **Step 1: Write workflow file** (full content based on kkalscan `rustore-release.yml` + design checkout step from `deploy-app-fixaverse.yml`)
- [ ] **Step 2: Ensure keystore secrets exist**

If missing: generate release keystore locally **once** (do not commit), base64 it, `gh secret set` on the repo. Document alias/password in password manager only.

```bash
keytool -genkeypair -v -keystore client-app-release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias fixaverse -storepass '…' -keypass '…' -dname "CN=Fixaverse, O=Fixaverse, C=RU"
base64 -i client-app-release.keystore | gh secret set ANDROID_KEYSTORE_BASE64
# set other ANDROID_* and RUSTORE_* secrets similarly
```

Copy `RUSTORE_KEY_ID` / `RUSTORE_PRIVATE_KEY` from KkalScan mobile repo secrets if same RuStore company account (`gh secret list` / known values from kkalscan env — do not print private key in chat logs).

- [ ] **Step 3: Commit workflow**

```bash
git add .github/workflows/rustore-release.yml
git commit -m "ci: RuStore release workflow for engineer Android app"
```

- [ ] **Step 4: Push and watch CI** for the feature commits on `main` (deploy workflow). Do **not** dispatch rustore-release until Task 5 console app exists.

---

### Task 5: RuStore Console bootstrap + first publish

**Prerequisite (manual / browser):** Create application in [console.rustore.ru](https://console.rustore.ru) with package **`pro.masterdoc.client`**. API cannot create a new package — only drafts for existing apps. Upload icon/screenshots via console or `setup_store_listing` MCP after draft exists.

- [ ] **Step 1: Create app in RuStore Console** (package `pro.masterdoc.client`, type MAIN, age 0+)
- [ ] **Step 2: Verify API sees the app**

```bash
# via MCP list_applications or:
# ensure RUSTORE_* env points at company key; package must appear
```

- [ ] **Step 3: Dispatch first release**

```bash
gh workflow run rustore-release.yml -f version_name=1.0.0 -f version_code=10000 -f whats_new="Первый релиз Fixaverse для инженеров."
gh run watch
```

- [ ] **Step 4: Upload store graphics** if not in draft (icon 512, screenshots) via console or RuStore MCP `upload_icon` / `upload_screenshot`
- [ ] **Step 5: Confirm moderation / ACTIVE version in console**
- [ ] **Step 6: Document in `store/rustore/README.md` + short section in `client-app/README.md` — how to bump major vs patch, workflow dispatch
- [ ] **Step 7: Commit docs if changed; push; watch Actions**

```bash
git commit -m "docs(rustore): first-publish checklist and release notes"
git push
gh run watch
```

---

### Task 6: Final branch review

- [ ] Confirm unit tests for `AppUpdatePolicy` in CI Test job
- [ ] Confirm `assembleDebug` still green with RuStore dependency
- [ ] Confirm RuStore draft/moderation succeeded for `1.0.0`
- [ ] Smoke: install from RuStore on device with RuStore app logged in (manual if device available)
- [ ] No secrets in git history

---

## Self-review (plan vs spec)

| Spec requirement | Task |
|------------------|------|
| Publish engineer app to RuStore | 3, 4, 5 |
| IMMEDIATE on major | 1, 2 |
| SILENT on minor/patch | 1, 2 |
| Cancel IMMEDIATE → exit | 2 |
| versionCode scheme | 1 (KkalScan-aligned) |
| CI AAB upload | 4, 5 |
| No iOS / Play | out of scope |
| Android-only SDK | 2 |

No TBD placeholders. Types: `AppUpdateFlow`, `selectUpdateFlow`, `RuStoreAppUpdater.checkAndStart` consistent across tasks.
