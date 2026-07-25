# Shared Fixaverse design theme (`fixaverse-design`)

**Date:** 2026-07-25  
**Status:** Approved for planning (awaiting final spec review)  
**Consumers:** `client-app`, `masterdocapp` (copilot)  
**Brand sources:** [fixaverse.ru](https://fixaverse.ru), [copilot.fixaverse.ru](https://copilot.fixaverse.ru), `masterdoc-toir/landing` CSS

## Problem

`client-app` ships a Graphite + Cobalt Material3 palette (`#E8EDF3` canvas, `#1F4B99` primary, system SansSerif) that does not match the live Fixaverse brand. Copilot (`masterdocapp`) already follows Lite tokens (white paper, navy ink, `#1A6FFF` flare) aligned with the marketing site. Paparazzi for the client design system lives inside `client-app` and is coupled to that outdated theme.

## Goal

One published light-only Compose theme library that both apps depend on. Visual tokens match the site/copilot. Theme Paparazzi moves out of `client-app` into the design repo. Product UI components (`AppButton`, etc.) stay in each app for now.

## Non-goals (v1)

- Shared UI component library (`App*`, chat bubbles, phone chrome)
- Dark theme
- Custom font binaries / serif italic display faces from copilot chat
- Extracting a monorepo composite instead of Maven publish
- Component-level Paparazzi gallery for client `App*` widgets

## Decisions (locked)

| Topic | Choice |
|-------|--------|
| Scope | Tokens + thin Compose theme only |
| Packaging | New GitHub repo + GitHub Packages (Maven) |
| Dark mode | Light only |
| Paparazzi | Theme/token snapshots in the new repo; remove `:design-system-paparazzi` from `client-app` |
| Component snapshots | Out of scope for this work (optional follow-up in `client-app`) |

## Repository layout

**Repo:** `masterdoc-app/fixaverse-design` (same org as `client-app` / `masterdocapp`)

```
fixaverse-design/
  settings.gradle.kts
  gradle/
  :theme/          # published KMP library
  :paparazzi/      # Android unitTest snapshots; not published
  .github/workflows/
    ci.yml         # build + paparazzi verify
    publish.yml    # on tag v* → GitHub Packages
```

### Artifact

- Group: `pro.fixaverse`
- Artifact: `design-theme`
- Coordinates: `pro.fixaverse:design-theme:<semver>`
- Targets: at minimum `android`, `jvm` / whatever `client-app` and `masterdocapp` already need for Compose Multiplatform (match consumer targets during implementation; do not invent unused targets).

### Public API (`:theme`)

Package: `pro.fixaverse.design.theme`

- `FixaverseLiteTokens` — raw colors (same names as today’s copilot module)
- `fixaverseLightColorScheme()` — Material3 `ColorScheme`
- `FixaverseTypography` — sans scale (no serif italic display in v1)
- `FixaverseShapes` / `FixaverseSpacing`
- `@Composable fun FixaverseTheme(content)` — light only; no `darkTheme` parameter

Optional temporary typealiases in consumers (`ClientTheme` → `FixaverseTheme`) during migration only.

## Token table (source of truth)

Aligned with `:root` / `.copilot-floor` CSS and current `FixaverseLiteTokens`:

| Token | Hex | Role |
|-------|-----|------|
| Paper | `#FFFFFF` | background, surface, on-accent |
| Paper2 | `#F9FAFB` | muted surface |
| Paper3 / FlareTint | `#EEF3FF` | accent tint surface |
| FlareSoft / Paper4 | `#DBE8FF` | soft accent fill |
| Rule | `#E5E7EB` | outline |
| Rule2 | `#C7D8F5` | strong / accent-leaning border |
| Ink | `#0D1B3A` | primary text |
| Ink2 | `#334155` | secondary text |
| Ink3 | `#64748B` | tertiary / muted text |
| Flare | `#1A6FFF` | primary CTA / accent |
| Forest | `#16A34A` | success |
| WarmRed | `#DC2626` | error |

### Material3 mapping

- `primary` = Flare, `onPrimary` = Paper  
- `background` / `surface` = Paper, `surfaceVariant` / containers from Paper2–Paper3 as needed  
- `onBackground` / `onSurface` = Ink, `onSurfaceVariant` = Ink2  
- `outline` = Rule, `outlineVariant` = Rule2  
- `error` = WarmRed  
- `surfaceTint` = transparent or Flare at low emphasis (prefer transparent to avoid M3 tint wash)

**Removed from client:** Graphite canvas `#E8EDF3`, Cobalt `#1F4B99`, cool-blue-bias unit tests that require `blue > red` on background (Paper is white; replace with Lite equality checks).

## Paparazzi (`:paparazzi`)

Move intent from `client-app/design-system-paparazzi`, then reshape:

**Keep / rewrite for theme:**
- Color swatches for Lite tokens
- Light scheme assertions vs Material warm neutrals (`#FEF7FF`, `#F3EDF7`) — ensure we never ship M3 default creams
- Surface hierarchy samples (Paper / Paper2 / tint)

**Do not move as-is:**
- Snapshots of `AppButton`, `AppScaffold`, `AppNav*`, etc. (those composables stay in `client-app`)

CI: `./gradlew :paparazzi:verifyPaparazziDebug` (or project’s Paparazzi task name) on PR; record only via intentional workflow/local.

## Consumer migration

### `client-app`

1. Add GitHub Packages repo + dependency `pro.fixaverse:design-theme`.
2. Replace `ClientColors` / `clientLightColorScheme` / `clientDarkColorScheme` / `ClientTheme(darkTheme=…)` with shared theme.
3. Keep `:design-system` **components** module (or rename later); wire colors only through `MaterialTheme` / shared tokens.
4. Delete `:design-system-paparazzi` module and its CI job entries.
5. Delete dark theme entry points.

### `masterdocapp`

1. Same Maven dependency.
2. Delete local `FixaverseLiteTokens` / duplicate palette objects; re-export or switch imports to `pro.fixaverse.design.theme`.
3. Keep local copilot components and serif/chat-specific text styles in-app (not in shared v1).

## Publish & auth

- GitHub Actions uses `GITHUB_TOKEN` / `packages: write` for the design repo.
- Consumers need a read token (or org SSO) for `maven { url = uri("https://maven.pkg.github.com/masterdoc-app/fixaverse-design") }` in CI and local `~/.gradle/gradle.properties` / env — document in README of `fixaverse-design`.
- Version bumps: tag `vX.Y.Z` → publish; consumers bump catalog/version explicitly (no floating `latest`).

## Success criteria

1. Single Lite hex set used by both apps for theme roles listed above.  
2. `fixaverse-design` CI green including Paparazzi verify.  
3. Artifact published and resolvable by `client-app` and `masterdocapp`.  
4. `client-app` no longer contains `:design-system-paparazzi` or Graphite/Cobalt theme.  
5. Smoke (Playwright on deployed or local client): white paper background, `#1A6FFF`-class primary actions, navy body text — not graphite canvas.

## Follow-ups (explicitly later)

- Shared component library + component Paparazzi  
- Optional dark tokens  
- Shared font assets (Inter / JetBrains Mono / serif display)  
- Sync marketing CSS generation from the same token source

## Risks

| Risk | Mitigation |
|------|------------|
| GH Packages auth friction in CI | Document org secret; fail CI with clear message if missing |
| Target mismatch (wasm vs android) | Mirror current consumer KMP targets before first publish |
| Visual regression in admin UI | Smoke + paparazzi theme; bump version only after verify |
| Half-migrated aliases | Prefer delete Graphite API in same PR as dependency bump |
