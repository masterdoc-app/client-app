# Task 2 Report: AssetDisplay helpers + AssetNameLink

## Status

Implemented and verified.

## Changes

- Added `assetDisplayName(name, assetId)` with trimmed-name preference and an eight-character ID fallback.
- Added `assetInventoryTooltip(inventoryNo)` with the required Russian labels for present and missing inventory numbers.
- Added `AssetNameLink`, which renders the display name as a primary-colored `AppText`, opens the supplied asset ID when clicked, and displays the inventory number in a Material3 `PlainTooltip`.
- Added common tests covering all helper behaviors from the task brief.

## TDD verification

The initial `desktopTest` run failed at test compilation because the helper functions did not exist yet. After implementation, the target passed:

```text
./gradlew :composeApp:desktopTest --tests "pro.masterdoc.client.ui.screens.AssetDisplayTest" -q
BUILD SUCCESS
```

## Scope

Charts, work-order screens, and shell wiring were intentionally left unchanged for Tasks 3 and 4.

## Commit

`feat(ui): AssetNameLink with inventory tooltip helpers`
# Task 2 Report: Lite tokens + light ColorScheme

**Status:** Complete  
**Repo:** `fixaverse-design` @ `main`  
**Commit:** `ba5eae2` — `feat(theme): add FixaverseLiteTokens and light ColorScheme`

## TDD flow

| Step | Result |
|------|--------|
| 1. Failing tests written | `FixaverseLiteTokensTest.kt` (3 tests, verbatim from brief) |
| 2. RED — `./gradlew :theme:jvmTest` | Compilation failed: unresolved `FixaverseLiteTokens`, `fixaverseLightColorScheme` |
| 3–4. Implementation | `FixaverseLiteTokens.kt` (copied from masterdocapp), `FixaverseColorScheme.kt` |
| 5. GREEN — `./gradlew :theme:jvmTest` | BUILD SUCCESSFUL, 3/3 tests pass |

## Files created

- `theme/src/commonMain/kotlin/pro/fixaverse/design/theme/FixaverseLiteTokens.kt`
- `theme/src/commonMain/kotlin/pro/fixaverse/design/theme/FixaverseColorScheme.kt`
- `theme/src/jvmTest/kotlin/pro/fixaverse/design/theme/FixaverseLiteTokensTest.kt`

## Files modified

- `theme/build.gradle.kts` — added `jvmTest` dependency on `kotlin("test")`

## Spec compliance

- **primary = Flare `#1A6FFF`** (not Ink)
- **background/surface = Paper `#FFFFFF`**
- Rejects Graphite `#E8EDF3` and Cobalt `#1F4B99`
- Rejects Material3 warm neutrals (`#FEF7FF`, `#F3EDF7`)
- All 24 token values match masterdocapp `FixaverseLiteTokens.kt`

## Self-review

- Token hex values byte-for-byte identical to source; package/comments preserved.
- `fixaverseLightColorScheme()` matches brief exactly; `surfaceTint = Color.Transparent`.
- No Task 3 scope (typography/composable theme) or paparazzi snapshots added.
- Tests use `kotlin.test` on JVM target — minimal, fast, no Android emulator.

## CI

No GitHub Actions workflow in repo at push time; verification was local `./gradlew :theme:jvmTest`.

## Concerns / follow-ups

- Task 5 will need call-site updates where masterdocapp assumed Ink primary.
- Consider adding CI workflow (Task 1 scaffold did not include one).
