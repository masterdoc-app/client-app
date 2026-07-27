# Web text selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Wasm web users select and copy visible text via pointer + Ctrl/Cmd+C, without changing Android/Desktop behavior.

**Architecture:** Add `expect`/`actual` composable `AppTextSelection` that wraps content in Compose `SelectionContainer` on Wasm only and is a no-op on Android/Desktop. Wire it at both `App` and `AuthenticatedApp` entry points so loading/error/shell screens are covered.

**Tech Stack:** Compose Multiplatform 1.7.x, Kotlin Multiplatform (android / desktop / wasmJs), Material3.

**Spec:** `docs/superpowers/specs/2026-07-24-web-text-selection-design.md`

## Global Constraints

- Wasm: wrap content in Compose `SelectionContainer`
- Android and Desktop: render content unchanged (identity wrapper)
- Both `App` and `AuthenticatedApp` must use the wrapper
- Do not change individual `Text` / `AppText` call sites
- Do not use CSS `user-select` (Compose Wasm text is canvas, not HTML)
- Android and Desktop behavior must remain unchanged
- Keep the change minimal (YAGNI): one expect + three actuals + two call sites

---

## File map

| File | Role |
|------|------|
| `composeApp/src/commonMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.kt` | `expect fun AppTextSelection` |
| `composeApp/src/wasmJsMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.wasmJs.kt` | `SelectionContainer` actual |
| `composeApp/src/androidMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.android.kt` | identity actual |
| `composeApp/src/desktopMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.desktop.kt` | identity actual |
| `composeApp/src/commonMain/kotlin/pro/masterdoc/client/App.kt` | wrap `App` + `AuthenticatedApp` |
| `composeApp/src/commonTest/kotlin/pro/masterdoc/client/platform/AppTextSelectionHostTest.kt` | host-side smoke that wrapper invokes content |

---

### Task 1: AppTextSelection expect/actual + entry wiring

**Files:**
- Create: files in File map above (except App.kt modify)
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/App.kt`
- Test: `composeApp/src/commonTest/kotlin/pro/masterdoc/client/platform/AppTextSelectionHostTest.kt`

- [ ] **Step 1: Write failing host test**

Create `AppTextSelectionHostTest.kt` that uses Compose UI testing (or a tiny host helper) to prove `AppTextSelection` invokes its content lambda. If the project has no compose UI test infra on commonTest, instead add a package-visible test double pattern:

Preferred approach matching this repo (see existing `commonTest` style): write a unit-level test of a tiny shared host:

```kotlin
package pro.masterdoc.client.platform

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Documents the contract: AppTextSelection must invoke [content] exactly once
 * when composed. Platform selection behavior is verified in browser smoke.
 */
class AppTextSelectionHostTest {
    @Test
    fun appTextSelection_contract_is_documented() {
        // Compile-time: expect AppTextSelection exists in platform package.
        // Runtime selection is Wasm-only (SelectionContainer) — see design spec.
        assertTrue(true) // placeholder replaced in Step 2 with real composable host if feasible
    }
}
```

Better: if `org.jetbrains.compose.ui:ui-test` is already on commonTest, use:

```kotlin
@Test
fun content_is_invoked() {
    // Compose test: setContent { AppTextSelection { Text("sel") } }
    // assert exists node with text "sel"
}
```

Check `composeApp/build.gradle.kts` for ui-test; if absent, **do not add** a new test dependency — skip UI test and rely on compile of all targets + browser smoke (document that in the report). Still create the expect/actual and wire App.kt.

- [ ] **Step 2: Add expect declaration**

```kotlin
package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable

/** Wasm: SelectionContainer. Android/Desktop: identity. */
@Composable
expect fun AppTextSelection(content: @Composable () -> Unit)
```

- [ ] **Step 3: Add Wasm actual**

```kotlin
package pro.masterdoc.client.platform

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable

@Composable
actual fun AppTextSelection(content: @Composable () -> Unit) {
    SelectionContainer(content = content)
}
```

- [ ] **Step 4: Add Android + Desktop identity actuals**

```kotlin
package pro.masterdoc.client.platform

import androidx.compose.runtime.Composable

@Composable
actual fun AppTextSelection(content: @Composable () -> Unit) {
    content()
}
```

(same for desktop)

- [ ] **Step 5: Wire both entry points in App.kt**

Import `pro.masterdoc.client.platform.AppTextSelection`.

Wrap bodies:

```kotlin
@Composable
fun App(root: RootComponent) {
    AppTextSelection {
        ClientTheme {
            MainShellContent(component = root.shell)
        }
    }
}

@Composable
fun AuthenticatedApp(...) {
    AppTextSelection {
        ClientTheme {
            // existing state / when block unchanged
        }
    }
}
```

- [ ] **Step 6: Verify compile of common sources**

Do **not** run heavy Gradle Wasm distribution builds (repo rule: CI builds). Prefer a light check if available, e.g. existing unit tests:

```bash
./gradlew :composeApp:desktopTest --tests 'pro.masterdoc.client.platform.*' 
# or existing commonTest task the project already uses
```

If that is too heavy / blocked by sandbox, run the project's lightest existing test task that already covers composeApp commonTest. Report the command and result.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.kt \
  composeApp/src/wasmJsMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.wasmJs.kt \
  composeApp/src/androidMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.android.kt \
  composeApp/src/desktopMain/kotlin/pro/masterdoc/client/platform/AppTextSelection.desktop.kt \
  composeApp/src/commonMain/kotlin/pro/masterdoc/client/App.kt \
  composeApp/src/commonTest/kotlin/pro/masterdoc/client/platform/AppTextSelectionHostTest.kt \
  docs/superpowers/plans/2026-07-27-web-text-selection.md

git commit -m "$(cat <<'EOF'
Enable pointer text selection on Wasm via SelectionContainer.

EOF
)"
```

Only stage files that belong to this task. Do not push (controller pushes).

---

### Task 2: Controller-only verification (no implementer)

After Task 1 review is clean, the **controller** (not implementer):

1. Push branch / merge to main per repo conventions (`commit-push-immediately` on default branch workflow for this monorepo: push the feature branch or main as appropriate)
2. `gh run watch` until test+deploy succeed
3. Smoke on `https://app.fixaverse.ru/` (or local `wasmJsBrowserRun` if not yet deployed): drag-select heading/body, Ctrl/Cmd+C, buttons still clickable, console clean

This task has no code changes.
