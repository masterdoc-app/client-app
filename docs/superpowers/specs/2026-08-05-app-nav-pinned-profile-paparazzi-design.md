# AppNav: pinned Profile + overflow scroll + Paparazzi

**Date:** 2026-08-05  
**Status:** Approved (design)  
**Repo:** `client-app`  
**Components:** `AppNavBar`, `AppNavRail`, `splitPinnedTrailing`

## Problem

With many granted features (e.g. smoke admin), nav destinations overflow:

- **Rail (wide):** trailing Profile must stay visible while other items scroll vertically. Partially done (`splitPinnedTrailing` + `verticalScroll`).
- **Bar (compact):** all items share one `horizontalScroll`, so Profile can scroll off-screen.

Component Paparazzi was removed from `client-app` when theme snapshots moved to `fixaverse-design`. There is no visual gate for this nav behavior.

## Goal

1. Profile (trailing nav item) is **always visible** in both orientations.
2. Overflowing primary items scroll: **vertical** in rail, **horizontal** in bottom bar.
3. Restore a **narrow** `:design-system-paparazzi` module whose goldens lock this behavior only.

## Non-goals

- Restoring the old full DS gallery (AppButton, typography, loaders, etc.)
- Changing Profile visual style, labels, or nav item order
- Moving `AppNav*` into `fixaverse-design`
- Pinning by hard-coded `key == "profile"` (order contract stays: Profile is last)

## Decisions (locked)

| Topic | Choice |
|-------|--------|
| Pin model | Trailing count via existing `splitPinnedTrailing` (`pinnedTrailingCount = 1`) |
| Rail | Keep: scrollable column `weight(1f)` + pinned trailing below |
| Bar | Same split: scrollable row `weight(1f)` + pinned trailing on the **right** |
| Few items | Scroll inactive; Profile still trailing (bar right / rail bottom) |
| Paparazzi scope | AppNav overflow snapshots only |
| Paparazzi home | `:design-system-paparazzi` in `client-app` (depends on `:design-system`) |
| Theme snapshots | Remain in `fixaverse-design/:paparazzi` — do not duplicate |

## Behavior

### Shared split

Reuse `splitPinnedTrailing(items, pinnedTrailingCount = 1)`:

- `scrollable` — all but last item
- `pinned` — last item (Profile by shell contract)

### `AppNavRail`

Unchanged intent:

```
Column(88.dp, fillMaxHeight)
  Column(weight(1f), verticalScroll) { scrollable buttons }
  Column { pinned buttons }          // always on screen
VerticalDivider
```

### `AppNavBar`

Change from “scroll entire row” to pin trailing:

```
Column
  HorizontalDivider
  Row(fillMaxWidth, height 64.dp, padding)
    Row(weight(1f), horizontalScroll) { scrollable buttons }
    pinned buttons                   // always on screen, trailing edge
```

Selected Profile keeps current `AppNavButton` selected styling (primary container pill).

## Paparazzi module

### Layout

```
design-system-paparazzi/
  build.gradle.kts          # androidLibrary + paparazzi; depends on :design-system
  src/androidMain/AndroidManifest.xml
  src/androidUnitTest/.../AppNavOverflowSnapshotTest.kt
  src/test/snapshots/images/   # recorded goldens
```

### Wiring

- `settings.gradle.kts`: `include(":design-system-paparazzi")`
- Root / version catalog: re-add `paparazzi` plugin (`app.cash.paparazzi` ~1.3.5, same as `fixaverse-design`)
- CI (`deploy-app-fixaverse.yml` test job): add `:design-system-paparazzi:verifyPaparazziDebug` (and `:design-system:jvmTest` if not already covered for `NavItemSplitTest`)

### Snapshot cases

Use `ClientTheme` / Fixaverse theme wrapper consistent with current design-system consumers. Fixture icons from Material Icons. Labels human-readable (e.g. «Заявки», «Профиль») — no raw ids.

| Test | Viewport intent | Assertion via golden |
|------|-----------------|----------------------|
| `navRail_manyItems_profilePinned` | Short height (~320–400dp), ~8–10 rail items | Profile visible at bottom; upper items may clip into scroll |
| `navBar_manyItems_profilePinned` | Narrow width (~320dp), many bottom items | Profile visible at trailing edge; leading items in scroll region |
| Optional: `navRail_fewItems` / `navBar_fewItems` | Comfortable size, 3–4 items | No visual regression; Profile still trailing |

Rendering: `SessionParams.RenderingMode.SHRINK` (same pattern as former client / current `fixaverse-design` paparazzi).

Record via intentional local/CI `:design-system-paparazzi:recordPaparazziDebug`; PR/main verify only.

## Testing beyond Paparazzi

- Keep existing `NavItemSplitTest` JVM unit tests.
- Smoke after ship: wide + compact shell with multi-feature session (Fixaverse Smoke) — Profile always reachable without scrolling it into view.

## Success criteria

- [ ] `AppNavBar` pins trailing item; overflow scrolls horizontally for the rest
- [ ] `AppNavRail` still pins trailing; overflow scrolls vertically
- [ ] `:design-system-paparazzi` verifies AppNav overflow goldens in CI
- [ ] No resurrected non-nav DS snapshot suite

## Out of scope follow-ups

- Broader component Paparazzi gallery
- Accessibility scroll affordances / fade edges (optional polish later)
