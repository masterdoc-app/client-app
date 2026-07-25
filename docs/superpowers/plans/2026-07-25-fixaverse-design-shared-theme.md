# Shared Fixaverse Lite theme (`fixaverse-design`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a light-only Compose Multiplatform theme library (`pro.fixaverse:design-theme`) matching fixaverse.ru / copilot Lite tokens, move theme Paparazzi out of `client-app`, and wire both `client-app` and `masterdocapp` to the artifact.

**Architecture:** New GitHub repo `masterdoc-app/fixaverse-design` with `:theme` (published KMP) and `:paparazzi` (Android snapshot tests, not published). Consumers resolve the library from GitHub Packages. Product UI components stay in each app; only tokens + `FixaverseTheme` are shared.

**Tech Stack:** Kotlin 2.2.21, Compose Multiplatform 1.7.3, AGP 8.7.3, Material3, Paparazzi 1.3.5, GitHub Packages Maven, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-07-25-fixaverse-design-shared-theme-design.md` (in `client-app`)

## Global Constraints

- Light theme only — no `darkTheme` parameter, no dark `ColorScheme`
- Package: `pro.fixaverse.design.theme`
- Maven: `pro.fixaverse:design-theme:<semver>`
- Raw token object name: `FixaverseLiteTokens` (exact hexes from spec)
- Material3: `primary` = Flare `#1A6FFF`, `onPrimary` = Paper; `background`/`surface` = Paper; `onSurface` = Ink `#0D1B3A`
- KMP targets on `:theme`: `android`, `jvm`, `wasmJs`, `iosX64`, `iosArm64`, `iosSimulatorArm64` (must satisfy `masterdocapp`)
- Do not publish `:paparazzi`; do not move `App*` components into the shared repo
- Do not run heavy local Wasm/production distribution builds — rely on GitHub Actions after push
- Smoke tenant: Fixaverse Smoke (`383177088934346755`) only

---

## File map

### New repo `fixaverse-design` (create under `/Users/antonbutov/Documents/MYPROJECTS/fixaverse/fixaverse-design`)

| Path | Responsibility |
|------|----------------|
| `settings.gradle.kts` | Root name, include `:theme`, `:paparazzi` |
| `build.gradle.kts` | Plugin aliases |
| `gradle/libs.versions.toml` | Align Kotlin/CMP/AGP/Paparazzi with consumers |
| `gradle.properties` | AndroidX, JVM args |
| `theme/build.gradle.kts` | KMP library + `maven-publish` to GitHub Packages |
| `theme/src/commonMain/.../FixaverseLiteTokens.kt` | Raw colors |
| `theme/src/commonMain/.../FixaverseColorScheme.kt` | `fixaverseLightColorScheme()` |
| `theme/src/commonMain/.../FixaverseTypography.kt` | Sans Material3 scale |
| `theme/src/commonMain/.../FixaverseShapes.kt` | Shapes + `FixaverseSpacing` |
| `theme/src/commonMain/.../FixaverseTheme.kt` | `FixaverseTheme { }` |
| `paparazzi/build.gradle.kts` | Android library + Paparazzi plugin |
| `paparazzi/src/androidUnitTest/.../ThemeSnapshotTest.kt` | Token/theme snapshots + unit asserts |
| `paparazzi/src/test/snapshots/images/` | Recorded goldens |
| `.github/workflows/ci.yml` | Build + `verifyPaparazziDebug` |
| `.github/workflows/publish.yml` | Tag `v*` → publish |
| `README.md` | Coordinates, consumer Gradle snippet, token table |

### `client-app`

| Path | Change |
|------|--------|
| `settings.gradle.kts` | Remove `:design-system-paparazzi` |
| `build.gradle.kts` / CI | Drop paparazzi job references if any |
| `settings` / `dependencyResolutionManagement` | GitHub Packages repo for `masterdoc-app` |
| `gradle/libs.versions.toml` | `fixaverse-design-theme` version |
| `design-system/build.gradle.kts` | Depend on `design-theme`; drop local theme sources if deleted |
| `design-system/.../theme/*` | Delete Graphite theme files; keep thin `ClientTheme` alias → `FixaverseTheme` + `ClientSpacing` typealias if needed |
| `design-system/.../components/*` | Keep; ensure colors via `MaterialTheme` |
| `composeApp/.../App.kt` | Call `FixaverseTheme` or alias without `darkTheme` |
| Delete module `design-system-paparazzi/` | Entire module |

### `masterdocapp`

| Path | Change |
|------|--------|
| `settings` / repos | GitHub Packages |
| `composeApp/build.gradle.kts` | `implementation("pro.fixaverse:design-theme:…")` |
| `.../FixaverseLiteTokens.kt` | Delete; import shared |
| `.../Theme.kt` | Use shared `FixaverseTheme` / `fixaverseLightColorScheme` |
| `.../FixaverseColors.kt` / `FixaversePalette` | Keep semantic aliases; point at shared tokens |
| Chat-only shapes / typography / fonts | Stay local |

---

### Task 1: Scaffold `fixaverse-design` repo

**Files:**
- Create: entire `fixaverse-design/` tree listed above (stubs first)
- Create: GitHub repo `masterdoc-app/fixaverse-design` (private or public matching sibling repos)

**Interfaces:**
- Produces: empty KMP `:theme` that compiles; local path `/Users/antonbutov/Documents/MYPROJECTS/fixaverse/fixaverse-design`

- [ ] **Step 1: Create GitHub repository**

```bash
cd /Users/antonbutov/Documents/MYPROJECTS/fixaverse
gh repo create masterdoc-app/fixaverse-design --private --description "Fixaverse Lite Compose theme (tokens + FixaverseTheme)" --clone=false
mkdir -p fixaverse-design && cd fixaverse-design
git init
git remote add origin https://github.com/masterdoc-app/fixaverse-design.git
```

If the org requires `--public`, match `client-app` visibility via `gh repo view masterdoc-app/client-app --json visibility`.

- [ ] **Step 2: Copy Gradle wrapper from `client-app`**

```bash
cp -R ../client-app/gradle ./gradle
cp ../client-app/gradlew ../client-app/gradlew.bat .
chmod +x gradlew
```

- [ ] **Step 3: Write version catalog `gradle/libs.versions.toml`**

```toml
[versions]
kotlin = "2.2.21"
agp = "8.7.3"
composeMultiplatform = "1.7.3"
paparazzi = "1.3.5"
junit = "4.13.2"

[libraries]
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
androidLibrary = { id = "com.android.library", version.ref = "agp" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
mavenPublish = { id = "com.vanniktech.maven.publish", version = "0.30.0" }
```

Prefer vanniktech maven-publish **or** plain `maven-publish` — pick plain `maven-publish` if vanniktech adds friction; both are fine as long as GH Packages works.

- [ ] **Step 4: Root `settings.gradle.kts` + `build.gradle.kts` + `gradle.properties`**

`settings.gradle.kts`:

```kotlin
rootProject.name = "fixaverse-design"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":theme")
include(":paparazzi")
```

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

Root `build.gradle.kts`: apply plugin aliases `apply false` for KMP, Compose, Android library, Paparazzi.

- [ ] **Step 5: Stub `:theme/build.gradle.kts` with all KMP targets**

Include `androidTarget`, `jvm()`, `wasmJs { browser() }`, and three iOS targets with static framework name `FixaverseDesignTheme` (library — frameworks optional; targets required for metadata).

- [ ] **Step 6: Verify stub compiles on CI-relevant targets (Android + JVM only locally if iOS SDK missing)**

```bash
./gradlew :theme:compileKotlinJvm :theme:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL (empty commonMain ok).

- [ ] **Step 7: Initial commit + push**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle gradlew gradlew.bat theme paparazzi
git commit -m "$(cat <<'EOF'
chore: scaffold fixaverse-design KMP theme repo

EOF
)"
git push -u origin HEAD:main
```

---

### Task 2: Lite tokens + light ColorScheme (TDD via JVM unit test in `:paparazzi` or `:theme`)

**Files:**
- Create: `theme/src/commonMain/kotlin/pro/fixaverse/design/theme/FixaverseLiteTokens.kt`
- Create: `theme/src/commonMain/kotlin/pro/fixaverse/design/theme/FixaverseColorScheme.kt`
- Create: `theme/src/jvmTest/kotlin/pro/fixaverse/design/theme/FixaverseLiteTokensTest.kt` (or androidUnitTest if jvmTest awkward)

**Interfaces:**
- Produces:
  - `object FixaverseLiteTokens` with `Paper`, `Paper2`, `Paper3`, `Paper4`, `Rule`, `Rule2`, `Ink`, `Ink2`, `Ink3`, `Flare`, `FlareDim`, `FlareBorder`, `FlareTint`, `FlareSoft`, `Forest`, `ForestDim`, `ForestSoft`, `WarmRed`, `WarmRedDim`, `Marian`, `InkDark`, `OnPaper`, `PhoneChrome`, `QrBackdrop` — hex values identical to current `masterdocapp` `FixaverseLiteTokens.kt`
  - `fun fixaverseLightColorScheme(): ColorScheme`

- [ ] **Step 1: Write failing token tests**

```kotlin
package pro.fixaverse.design.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FixaverseLiteTokensTest {
    @Test
    fun paperAndFlareMatchBrandCss() {
        assertEquals(Color(0xFFFFFFFF), FixaverseLiteTokens.Paper)
        assertEquals(Color(0xFF1A6FFF), FixaverseLiteTokens.Flare)
        assertEquals(Color(0xFF0D1B3A), FixaverseLiteTokens.Ink)
        assertEquals(Color(0xFFF9FAFB), FixaverseLiteTokens.Paper2)
        assertEquals(Color(0xFFEEF3FF), FixaverseLiteTokens.FlareTint)
    }

    @Test
    fun lightScheme_usesFlarePrimaryAndPaperCanvas() {
        val s = fixaverseLightColorScheme()
        assertEquals(FixaverseLiteTokens.Flare, s.primary)
        assertEquals(FixaverseLiteTokens.Paper, s.onPrimary)
        assertEquals(FixaverseLiteTokens.Paper, s.background)
        assertEquals(FixaverseLiteTokens.Paper, s.surface)
        assertEquals(FixaverseLiteTokens.Ink, s.onBackground)
        assertEquals(FixaverseLiteTokens.Ink, s.onSurface)
        assertEquals(FixaverseLiteTokens.Ink2, s.onSurfaceVariant)
        assertEquals(FixaverseLiteTokens.Rule, s.outline)
        assertEquals(FixaverseLiteTokens.WarmRed, s.error)
        // Reject Material3 warm neutrals
        assertNotEquals(Color(0xFFFEF7FF), s.background)
        assertNotEquals(Color(0xFFF3EDF7), s.surfaceContainer)
    }

    @Test
    fun lightScheme_rejectsGraphiteCobalt() {
        val s = fixaverseLightColorScheme()
        assertNotEquals(Color(0xFFE8EDF3), s.background)
        assertNotEquals(Color(0xFF1F4B99), s.primary)
    }
}
```

Use `kotlin.test` or JUnit4 consistent with module setup.

- [ ] **Step 2: Run tests — expect FAIL (symbols missing)**

```bash
./gradlew :theme:jvmTest
```

Expected: compilation failure or missing symbols.

- [ ] **Step 3: Implement `FixaverseLiteTokens.kt`**

Copy values from `masterdocapp/composeApp/src/commonMain/kotlin/pro/fixaverse/app/ui/theme/FixaverseLiteTokens.kt` into package `pro.fixaverse.design.theme`. Keep comments pointing at fixaverse.ru CSS.

- [ ] **Step 4: Implement `fixaverseLightColorScheme()`**

```kotlin
fun fixaverseLightColorScheme(): ColorScheme = lightColorScheme(
    primary = FixaverseLiteTokens.Flare,
    onPrimary = FixaverseLiteTokens.Paper,
    primaryContainer = FixaverseLiteTokens.FlareSoft,
    onPrimaryContainer = FixaverseLiteTokens.Ink,
    secondary = FixaverseLiteTokens.Ink,
    onSecondary = FixaverseLiteTokens.Paper,
    secondaryContainer = FixaverseLiteTokens.Paper2,
    onSecondaryContainer = FixaverseLiteTokens.Ink,
    tertiary = FixaverseLiteTokens.Marian,
    onTertiary = FixaverseLiteTokens.Paper,
    tertiaryContainer = FixaverseLiteTokens.ForestSoft,
    onTertiaryContainer = FixaverseLiteTokens.Forest,
    background = FixaverseLiteTokens.Paper,
    onBackground = FixaverseLiteTokens.Ink,
    surface = FixaverseLiteTokens.Paper,
    onSurface = FixaverseLiteTokens.Ink,
    surfaceVariant = FixaverseLiteTokens.Paper2,
    onSurfaceVariant = FixaverseLiteTokens.Ink2,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = FixaverseLiteTokens.Paper,
    surfaceContainerLow = FixaverseLiteTokens.Paper2,
    surfaceContainer = FixaverseLiteTokens.Paper2,
    surfaceContainerHigh = FixaverseLiteTokens.Paper3,
    surfaceContainerHighest = FixaverseLiteTokens.FlareSoft,
    outline = FixaverseLiteTokens.Rule,
    outlineVariant = FixaverseLiteTokens.Rule2,
    error = FixaverseLiteTokens.WarmRed,
    onError = FixaverseLiteTokens.Paper,
    errorContainer = FixaverseLiteTokens.WarmRedDim,
    onErrorContainer = FixaverseLiteTokens.WarmRed,
    inverseSurface = FixaverseLiteTokens.Ink,
    inverseOnSurface = FixaverseLiteTokens.Paper,
    inversePrimary = FixaverseLiteTokens.FlareSoft,
    scrim = Color(0xFF000000),
)
```

Note: this **differs** from current `masterdocapp` Theme (`primary = Ink`). Spec wins; Task 5 fixes call sites that assumed Ink primary.

- [ ] **Step 5: Run tests — expect PASS**

```bash
./gradlew :theme:jvmTest
```

- [ ] **Step 6: Commit**

```bash
git add theme/
git commit -m "$(cat <<'EOF'
feat(theme): add FixaverseLiteTokens and light ColorScheme

EOF
)"
git push
```

---

### Task 3: Typography, shapes, spacing, `FixaverseTheme`

**Files:**
- Create: `theme/src/commonMain/kotlin/pro/fixaverse/design/theme/FixaverseTypography.kt`
- Create: `theme/src/commonMain/kotlin/pro/fixaverse/design/theme/FixaverseShapes.kt`
- Create: `theme/src/commonMain/kotlin/pro/fixaverse/design/theme/FixaverseTheme.kt`
- Modify: token tests if needed (optional assert shapes radii)

**Interfaces:**
- Produces:
  - `val FixaverseTypography: Typography` — SansSerif scale (sizes aligned with client `ClientTypography`; no serif italic)
  - `object FixaverseSpacing` — `xs=4.dp`, `sm=8.dp`, `md=16.dp`, `lg=24.dp`, `xl=32.dp`
  - `val FixaverseShapes: Shapes` — 4 / 8 / 12 / 16 / 24.dp
  - `@Composable fun FixaverseTheme(content: @Composable () -> Unit)` — light only

- [ ] **Step 1: Implement typography + shapes + spacing** (mirror client `ClientTypography` / `ClientShapes` / `ClientSpacing`, rename)

- [ ] **Step 2: Implement theme**

```kotlin
@Composable
fun FixaverseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = fixaverseLightColorScheme(),
        typography = FixaverseTypography,
        shapes = FixaverseShapes,
        content = content,
    )
}
```

- [ ] **Step 3: Compile**

```bash
./gradlew :theme:compileKotlinJvm
```

Expected: SUCCESS

- [ ] **Step 4: Commit + push**

```bash
git commit -am "$(cat <<'EOF'
feat(theme): add typography, shapes, and FixaverseTheme

EOF
)"
git push
```

---

### Task 4: Paparazzi theme snapshots + CI

**Files:**
- Create: `paparazzi/build.gradle.kts` (depends on `:theme`, Paparazzi plugin)
- Create: `paparazzi/src/androidUnitTest/kotlin/pro/fixaverse/design/paparazzi/ThemeSnapshotTest.kt`
- Create: goldens under `paparazzi/src/test/snapshots/images/` via record
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `FixaverseTheme`, `FixaverseLiteTokens`, `fixaverseLightColorScheme`
- Produces: verified snapshots for swatches + scaffold paper background; **no** `AppButton` snapshots

- [ ] **Step 1: Wire `:paparazzi` module** (namespace `pro.fixaverse.design.paparazzi`, minSdk 26, compileSdk 35)

- [ ] **Step 2: Write `ThemeSnapshotTest`**

Include at least:
- `colors_swatches` — Paper, Paper2, FlareTint, Flare, Ink, Rule, Forest, WarmRed
- `lightScheme_rejectsMaterialWarmNeutrals` — unit asserts (from Task 2)
- `theme_scaffoldPaper` — `FixaverseTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) }` snapshot
- `primary_swatch` — Flare rectangle (documents CTA color)

Use `SessionParams.RenderingMode.SHRINK` like client-app paparazzi.

- [ ] **Step 3: Record goldens on CI-compatible machine (or local Android SDK)**

```bash
./gradlew :paparazzi:recordPaparazziDebug
```

- [ ] **Step 4: Verify**

```bash
./gradlew :paparazzi:verifyPaparazziDebug
```

Expected: PASS

- [ ] **Step 5: Add `.github/workflows/ci.yml`**

```yaml
name: CI
on:
  push:
    branches: [main]
  pull_request:
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - run: chmod +x gradlew
      - run: ./gradlew :theme:jvmTest :paparazzi:verifyPaparazziDebug
```

- [ ] **Step 6: Commit goldens + CI + push; watch Action**

```bash
git add paparazzi .github
git commit -m "$(cat <<'EOF'
test(paparazzi): theme swatches and paper scaffold goldens

EOF
)"
git push
gh run watch
```

---

### Task 5: Maven publish workflow

**Files:**
- Modify: `theme/build.gradle.kts` — `maven-publish` + group/version
- Create: `.github/workflows/publish.yml`
- Create: `README.md` consumer instructions

**Interfaces:**
- Produces: artifact `pro.fixaverse:design-theme:0.1.0` on GitHub Packages after tag `v0.1.0`

- [ ] **Step 1: Publishing block on `:theme`**

```kotlin
group = "pro.fixaverse"
version = System.getenv("VERSION") ?: "0.1.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"]) // or android + multiplatform publications per KMP docs
            artifactId = "design-theme"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/masterdoc-app/fixaverse-design")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Follow current Kotlin Multiplatform publishing docs for the exact `publications` setup (often `withType<MavenPublication>` after `kotlin` plugin). Verify with a dry-run publish to local Maven:

```bash
./gradlew :theme:publishToMavenLocal
ls ~/.m2/repository/pro/fixaverse/design-theme/
```

Expected: artifacts present.

- [ ] **Step 2: `publish.yml` on `push: tags: ['v*']`**

Extract version from tag (`v0.1.0` → `0.1.0`), set `VERSION`, `permissions: packages: write`, run `./gradlew :theme:publish`.

- [ ] **Step 3: Tag and publish**

```bash
git tag v0.1.0
git push origin v0.1.0
gh run watch
```

Expected: package visible at `https://github.com/masterdoc-app/fixaverse-design/packages`

- [ ] **Step 4: README** — coordinates, Gradle repo auth (`GITHUB_TOKEN` / PAT `read:packages`), version pin `0.1.0`

- [ ] **Step 5: Commit README if needed + push**

---

### Task 6: Migrate `client-app` to `design-theme`

**Files:**
- Modify: `client-app/settings.gradle.kts` — remove paparazzi include; add GH Packages repo
- Modify: `client-app/gradle/libs.versions.toml` — library entry
- Modify: `client-app/design-system/build.gradle.kts` — dependency
- Delete: `client-app/design-system/src/.../ClientColors.kt`, `clientDark` scheme, Graphite comments
- Modify: `ClientTheme.kt` → thin wrapper
- Modify: `ClientTypography.kt` / `ClientShapes.kt` → re-export or delete in favor of shared
- Keep: `ClientSpacing` as `typealias` / `val ClientSpacing = FixaverseSpacing` for fewer call-site edits
- Delete: entire `design-system-paparazzi/`
- Modify: CI workflows that reference paparazzi module
- Modify: `composeApp/.../App.kt` if signature changes

**Interfaces:**
- Consumes: `pro.fixaverse:design-theme:0.1.0`
- Produces: admin app themed with Paper + Flare; no `:design-system-paparazzi`

- [ ] **Step 1: Add GitHub Packages to `dependencyResolutionManagement.repositories`**

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/masterdoc-app/fixaverse-design")
    credentials {
        username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
        password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
    }
}
```

Document that CI must pass `GITHUB_TOKEN` with `packages: read` (same org usually works).

- [ ] **Step 2: Add catalog dependency and wire `:design-system`**

```toml
fixaverse-design-theme = { module = "pro.fixaverse:design-theme", version = "0.1.0" }
```

```kotlin
commonMain.dependencies {
    api(libs.fixaverse.design.theme) // so composeApp sees theme types if needed
    // existing compose deps...
}
```

- [ ] **Step 3: Replace theme implementation**

```kotlin
// ClientTheme.kt
@Composable
fun ClientTheme(content: @Composable () -> Unit) {
    FixaverseTheme(content)
}
```

Remove `darkTheme` parameter (update call sites in `App.kt`).

Map `ClientSpacing` → `FixaverseSpacing` without breaking imports (typealias or object delegation).

- [ ] **Step 4: Delete Graphite files and `:design-system-paparazzi`**

```bash
git rm -r design-system-paparazzi
# delete ClientColors.kt, clientDarkColorScheme, etc.
```

Update `settings.gradle.kts` `include` list; remove paparazzi plugin from root if unused.

- [ ] **Step 5: Commit + push `client-app`; watch Deploy Action**

```bash
git add -A # only after listing files; prefer explicit paths
git commit -m "$(cat <<'EOF'
feat(ui): adopt shared Fixaverse Lite design-theme

Replace Graphite/Cobalt ClientTheme with pro.fixaverse:design-theme
and remove local design-system-paparazzi module.
EOF
)"
git push
gh run watch
```

Do **not** run local `wasmJsBrowserDistribution`.

- [ ] **Step 6: Smoke (Playwright)** against `https://app.fixaverse.ru/` after deploy

Checklist:
1. Shell loads in Smoke org
2. Page background ≈ white Paper (not `#E8EDF3`) — sample pixels / body
3. Primary buttons ≈ `#1A6FFF` (not `#1F4B99`)
4. Equipment / nav still work
5. Compare against `fixaverse-design` paparazzi swatches conceptually

---

### Task 7: Migrate `masterdocapp` to `design-theme`

**Files:**
- Modify: repos + `composeApp/build.gradle.kts` dependency
- Delete: local `FixaverseLiteTokens.kt`
- Modify: `Theme.kt` to call shared `FixaverseTheme`
- Modify: `FixaverseColors.kt` / `FixaversePalette` — import shared tokens
- Audit: any `MaterialTheme.colorScheme.primary` that assumed Ink → use `onSurface` / `FixaverseLiteTokens.Ink` / `secondary` as appropriate

**Interfaces:**
- Consumes: `pro.fixaverse:design-theme:0.1.0`
- Produces: copilot still light Lite look; semantic chat colors unchanged

- [ ] **Step 1: Add dependency + Packages repo (same as client)**

- [ ] **Step 2: Delete local tokens file; fix imports to `pro.fixaverse.design.theme.FixaverseLiteTokens`

- [ ] **Step 3: Slim `Theme.kt`**

```kotlin
@Composable
fun FixaverseTheme(content: @Composable () -> Unit) {
    val fonts = rememberFixaverseFontFamilies()
    CompositionLocalProvider(LocalFixaverseFontFamilies provides fonts) {
        // Option A: wrap shared theme then override typography with font-loaded one
        MaterialTheme(
            colorScheme = fixaverseLightColorScheme(),
            typography = fixaverseTypography(fonts), // local serif/sans with fonts
            shapes = fixaverseShapes(), // keep chat radii locally
            content = content,
        )
    }
}
```

Rename carefully to avoid clash: either import shared theme as `DesignFixaverseTheme` or keep local function name and only import `fixaverseLightColorScheme` + tokens. **Preferred:** local `FixaverseTheme` stays; it uses shared `fixaverseLightColorScheme()` + shared tokens; does **not** call shared composable if local typography/shapes must differ.

- [ ] **Step 4: Fix primary=Ink regressions**

Search `colorScheme.primary` usages; where navy was intended, switch to `FixaverseLiteTokens.Ink` or `colorScheme.secondary` / `onSurface`. Where CTA blue intended, Flare primary is now correct.

- [ ] **Step 5: Commit + push `masterdocapp`; watch CI/deploy**

- [ ] **Step 6: Smoke** `https://copilot.fixaverse.ru/` — chat shell loads; accent still Flare; no Graphite canvas

---

### Task 8: Final verification + docs cross-link

**Files:**
- Modify: `client-app/docs/superpowers/specs/2026-07-25-fixaverse-design-shared-theme-design.md` status → Implemented
- Optionally: short note in `fixaverse-design/README.md` linking consumers

- [ ] **Step 1: Confirm packages + both apps on `0.1.0`**

- [ ] **Step 2: Smoke report** (client-app + paparazzi mental compare)

Must include org Smoke id; PASS on Paper background + Flare primary vs previous FAIL baseline.

- [ ] **Step 3: Commit doc status if changed**

---

## Spec coverage checklist

| Spec requirement | Task |
|------------------|------|
| New repo + GH Packages | 1, 5 |
| Tokens Lite hex table | 2 |
| Light-only `FixaverseTheme` | 3 |
| Theme Paparazzi in design repo | 4 |
| No shared App* components | 4 (explicit non-goal) |
| client-app migration + delete paparazzi module | 6 |
| masterdocapp migration | 7 |
| Success criteria / smoke | 6.6, 7.6, 8 |

## Self-review notes

- Primary mapping change for masterdocapp is intentional (spec); Task 7 Step 4 is mandatory.
- Chat bubble shapes stay in `masterdocapp` — shared shapes are for admin Material defaults.
- iOS targets required on `:theme` even if local Mac publish uses JVM/Android only — CI may need macOS for iOS compile if we verify those targets; minimum publish must include ios metadata or consumers fail. If linux CI cannot compile iOS, use `linkDebugFrameworkIos*` only on macOS job or publish from macOS runner for release tags.
