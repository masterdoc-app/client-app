# AppNav Pinned Profile + Paparazzi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep trailing Profile always visible in `AppNavBar` and `AppNavRail`; scroll overflow; gate with a narrow `:design-system-paparazzi` suite.

**Architecture:** Reuse `splitPinnedTrailing`. Rail already pins + vertical scroll. Bar switches from scrolling the whole row to scrolling only non-pinned items with Profile fixed on the trailing edge. Restore `:design-system-paparazzi` for AppNav goldens only; wire verify into CI.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Paparazzi 1.3.5, JUnit 4, `ClientTheme` → `FixaverseTheme`

**Spec:** `docs/superpowers/specs/2026-08-05-app-nav-pinned-profile-paparazzi-design.md`

## Global Constraints

- Pin model: trailing count via `splitPinnedTrailing(..., pinnedTrailingCount = 1)` — Profile is last by shell contract; do not hard-code `key == "profile"`.
- UI labels in fixtures: human Russian names («Заявки», «Профиль», …) — never raw ids.
- Paparazzi scope: AppNav overflow/few-item snapshots only — do not resurrect AppButton/typography/loader goldens.
- Theme snapshots stay in `fixaverse-design/:paparazzi`.
- Prefer not to run heavy local Gradle suites; for this task Paparazzi **record/verify** for `:design-system-paparazzi` is required and is an explicit exception. Prefer `./gradlew :design-system:jvmTest` and `:design-system-paparazzi:recordPaparazziDebug` / `verifyPaparazziDebug` over full project builds.
- After ship: commit → push → watch Actions → `/smoke-test` on Fixaverse Smoke (wide + compact nav).

---

### Task 1: Pin Profile in `AppNavBar`

**Files:**
- Modify: `design-system/src/commonMain/kotlin/pro/masterdoc/client/designsystem/components/AppNav.kt`
- Test: existing `design-system/src/commonTest/kotlin/pro/masterdoc/client/designsystem/components/NavItemSplitTest.kt` (no change required — split already covered)

**Interfaces:**
- Consumes: `splitPinnedTrailing(items, pinnedTrailingCount)`, `AppNavItem`, `AppNavButton`, `AppNavButtonLayout.Bottom`
- Produces:

```kotlin
@Composable
fun AppNavBar(
    items: List<AppNavItem>,
    modifier: Modifier = Modifier,
    pinnedTrailingCount: Int = 1,
)
```

- [ ] **Step 1: Update `AppNavBar` to pin trailing items**

Replace the body of `AppNavBar` in `AppNav.kt` so it matches the rail split pattern. Keep the public surface API; add optional `pinnedTrailingCount` defaulting to `1`. Full function:

```kotlin
@Composable
fun AppNavBar(
    items: List<AppNavItem>,
    modifier: Modifier = Modifier,
    pinnedTrailingCount: Int = 1,
) {
    val (scrollable, pinned) = splitPinnedTrailing(items, pinnedTrailingCount)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = ClientSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    scrollable.forEach { item ->
                        AppNavButton(
                            label = item.label,
                            icon = item.icon,
                            selected = item.selected,
                            onClick = item.onClick,
                            layout = AppNavButtonLayout.Bottom,
                        )
                    }
                }
                pinned.forEach { item ->
                    AppNavButton(
                        label = item.label,
                        icon = item.icon,
                        selected = item.selected,
                        onClick = item.onClick,
                        layout = AppNavButtonLayout.Bottom,
                    )
                }
            }
        }
    }
}
```

Update the KDoc above `AppNavRail` (or add a short KDoc on `AppNavBar`) to state that both bar and rail pin the trailing item and scroll the rest.

- [ ] **Step 2: Confirm split unit tests still pass**

Run:

```bash
./gradlew :design-system:jvmTest --tests 'pro.masterdoc.client.designsystem.components.NavItemSplitTest'
```

Expected: BUILD SUCCESSFUL, all `NavItemSplitTest` tests PASS.

- [ ] **Step 3: Commit**

```bash
git add design-system/src/commonMain/kotlin/pro/masterdoc/client/designsystem/components/AppNav.kt
git commit -m "$(cat <<'EOF'
fix(ui): pin Profile on bottom AppNavBar overflow

Scroll only non-trailing items horizontally so Профиль stays visible
on compact widths, matching the rail pin behavior.
EOF
)"
```

---

### Task 2: Restore `:design-system-paparazzi` (AppNav only) + CI

**Files:**
- Create: `design-system-paparazzi/build.gradle.kts`
- Create: `design-system-paparazzi/src/androidMain/AndroidManifest.xml`
- Create: `design-system-paparazzi/src/androidUnitTest/kotlin/pro/masterdoc/client/designsystem/paparazzi/AppNavOverflowSnapshotTest.kt`
- Create: goldens under `design-system-paparazzi/src/test/snapshots/images/` (via record)
- Modify: `settings.gradle.kts` — `include(":design-system-paparazzi")`
- Modify: `build.gradle.kts` — `alias(libs.plugins.paparazzi) apply false`
- Modify: `gradle/libs.versions.toml` — paparazzi version + plugin
- Modify: `.github/workflows/deploy-app-fixaverse.yml` — test job runs design-system + paparazzi verify

**Interfaces:**
- Consumes: `AppNavBar`, `AppNavRail`, `AppNavItem`, `ClientTheme`, Material Icons
- Produces: JUnit tests `navRail_manyItems_profilePinned`, `navBar_manyItems_profilePinned`, `navRail_fewItems`, `navBar_fewItems` + recorded PNGs

- [ ] **Step 1: Add Paparazzi to version catalog and root plugins**

In `gradle/libs.versions.toml` under `[versions]` add:

```toml
paparazzi = "1.3.5"
```

Under `[plugins]` add:

```toml
paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
```

In root `build.gradle.kts` add inside `plugins { }`:

```kotlin
alias(libs.plugins.paparazzi) apply false
```

- [ ] **Step 2: Include module in settings**

In `settings.gradle.kts` after existing includes add:

```kotlin
include(":design-system-paparazzi")
```

- [ ] **Step 3: Create module `build.gradle.kts`**

Create `design-system-paparazzi/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.paparazzi)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(projects.designSystem)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(compose.materialIconsExtended)
        }
    }
}

android {
    namespace = "pro.masterdoc.client.designsystem.paparazzi"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

Create `design-system-paparazzi/src/androidMain/AndroidManifest.xml`:

```xml
<manifest />
```

- [ ] **Step 4: Write `AppNavOverflowSnapshotTest`**

Create `design-system-paparazzi/src/androidUnitTest/kotlin/pro/masterdoc/client/designsystem/paparazzi/AppNavOverflowSnapshotTest.kt`:

```kotlin
package pro.masterdoc.client.designsystem.paparazzi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test
import pro.masterdoc.client.designsystem.components.AppNavBar
import pro.masterdoc.client.designsystem.components.AppNavItem
import pro.masterdoc.client.designsystem.components.AppNavRail
import pro.masterdoc.client.designsystem.theme.ClientTheme

class AppNavOverflowSnapshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5.copy(softButtons = false),
            theme = "android:Theme.Material.Light.NoActionBar",
            renderingMode = SessionParams.RenderingMode.SHRINK,
        )

    @Test
    fun navRail_manyItems_profilePinned() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.height(360.dp)) {
                    AppNavRail(items = manyItems(selectedKey = "profile"))
                }
            }
        }
    }

    @Test
    fun navBar_manyItems_profilePinned() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.width(320.dp)) {
                    AppNavBar(items = manyItems(selectedKey = "profile"))
                }
            }
        }
    }

    @Test
    fun navRail_fewItems() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.height(640.dp)) {
                    AppNavRail(items = fewItems(selectedKey = "tickets"))
                }
            }
        }
    }

    @Test
    fun navBar_fewItems() {
        paparazzi.snapshot {
            ClientTheme {
                Box(Modifier.width(400.dp)) {
                    AppNavBar(items = fewItems(selectedKey = "tickets"))
                }
            }
        }
    }
}

private fun manyItems(selectedKey: String): List<AppNavItem> {
    val defs =
        listOf(
            "tickets" to ("Заявки" to Icons.Filled.Assignment),
            "board" to ("Доска" to Icons.Filled.ViewKanban),
            "charts" to ("Отчёты" to Icons.Filled.ShowChart),
            "equipment" to ("Оборудование" to Icons.Filled.Build),
            "maps" to ("Карты" to Icons.Filled.Map),
            "ai" to ("ИИ" to Icons.Filled.SmartToy),
            "admin" to ("Админ" to Icons.Filled.Settings),
            "dashboard" to ("Дашборд" to Icons.Filled.Dashboard),
            "profile" to ("Профиль" to Icons.Filled.Person),
        )
    return defs.map { (key, labelIcon) ->
        val (label, icon) = labelIcon
        navItem(key, label, icon, selected = key == selectedKey)
    }
}

private fun fewItems(selectedKey: String): List<AppNavItem> =
    listOf(
        navItem("tickets", "Заявки", Icons.Filled.Assignment, selected = selectedKey == "tickets"),
        navItem("board", "Доска", Icons.Filled.ViewKanban, selected = selectedKey == "board"),
        navItem("profile", "Профиль", Icons.Filled.Person, selected = selectedKey == "profile"),
    )

private fun navItem(
    key: String,
    label: String,
    icon: ImageVector,
    selected: Boolean,
): AppNavItem =
    AppNavItem(
        key = key,
        label = label,
        icon = icon,
        selected = selected,
        onClick = {},
    )
```

If an icon import fails on the Compose Material Icons set used by the project, swap to icons already used in `MainShellContent.kt` (`Icons.Filled.*` there).

- [ ] **Step 5: Record goldens**

Run:

```bash
./gradlew :design-system-paparazzi:recordPaparazziDebug
```

Expected: BUILD SUCCESSFUL; PNG files appear under `design-system-paparazzi/src/test/snapshots/images/` for the four tests.

Then verify:

```bash
./gradlew :design-system-paparazzi:verifyPaparazziDebug
```

Expected: BUILD SUCCESSFUL (no diff).

Visually spot-check the two `*_manyItems_profilePinned` PNGs: Profile selected (primary pill) visible at rail bottom / bar trailing edge.

- [ ] **Step 6: Wire CI test job**

In `.github/workflows/deploy-app-fixaverse.yml`, under the `test` job step that runs Gradle, extend the command to include:

```bash
./gradlew \
  :auth:jvmTest \
  :shared:jvmTest \
  :design-system:jvmTest \
  :design-system-paparazzi:verifyPaparazziDebug \
  :composeApp:desktopTest \
  :composeApp:assembleDebug \
  --stacktrace
```

Keep existing checkout of `fixaverse-design` for `includeBuild`.

- [ ] **Step 7: Commit**

```bash
git add \
  design-system-paparazzi \
  settings.gradle.kts \
  build.gradle.kts \
  gradle/libs.versions.toml \
  .github/workflows/deploy-app-fixaverse.yml
git commit -m "$(cat <<'EOF'
test(ui): Paparazzi gate for AppNav pinned Profile

Restore a narrow design-system-paparazzi module with rail/bar
overflow and few-item goldens; verify in CI.
EOF
)"
```

- [ ] **Step 8: Push and watch CI**

```bash
git push origin HEAD
gh run watch
```

Expected: Test job green including `verifyPaparazziDebug`.

---

### Task 3: Smoke check (post-deploy)

**Files:** none (runtime poke)

**Interfaces:** none

- [ ] **Step 1: After green deploy, run `/smoke-test`**

Follow `~/.cursor/skills/smoke-test/SKILL.md` on **Fixaverse Smoke**.

Checklist:

- Wide viewport: many nav items → Profile visible at rail bottom without scrolling it off.
- Compact viewport: many nav items → Profile visible on bottom bar trailing edge; other items scroll horizontally.
- Open Profile from both layouts.

- [ ] **Step 2: Report PASS/FAIL/PARTIAL** in the task closing message (org + URL + verdict).

---

## Plan self-review

| Spec requirement | Task |
|------------------|------|
| Bar pins trailing + horizontal scroll | Task 1 |
| Rail keeps pin + vertical scroll | Task 1 (no change; covered by paparazzi in Task 2) |
| Narrow `:design-system-paparazzi` AppNav-only | Task 2 |
| Four snapshot cases (many×2, few×2) | Task 2 Step 4–5 |
| CI verify | Task 2 Step 6 |
| No full old DS gallery | Task 2 (only AppNav test file) |
| Smoke after ship | Task 3 |
| UI names not ids | Task 2 fixtures |

No TBD/placeholder steps. Signatures match `AppNavItem` / existing rail API.
