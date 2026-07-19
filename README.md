# client-app

KMP web/desktop/Android client for all facility roles. Primary navigation is feature-driven (max 5 items): bottom bar on compact widths, side rail otherwise.

## Modules

| Module | Role |
|--------|------|
| `:design-system` | Colors, typography, shapes, base Compose UI |
| `:design-system-paparazzi` | Paparazzi snapshot tests for the UI kit |
| `:shared` | Nav models, session fixtures, Decompose shell |
| `:composeApp` | App entry (Android, Desktop, Wasm/JS) |

## Role → menu (fixtures)

| Role | Menu |
|------|------|
| engineer | Заявки, Профиль |
| dispatcher | Доска, Карта, Профиль |
| technologist | Графики, Оборудование, Профиль |

`FeatureId.Copilot` is reserved for a future menu item (masterdoc rewrite). `masterdocapp` is not modified from this repo.

## Rules

See `.cursor/rules/`:

- **module-boundaries** — extract cohesive reused class groups into modules
- **class-workflow** — interface → models → test → implementation

## Build

```bash
./gradlew :shared:jvmTest
./gradlew :composeApp:run          # desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
./gradlew :design-system-paparazzi:recordPaparazziDebug
./gradlew :design-system-paparazzi:verifyPaparazziDebug
```

Copy `local.properties.example` → `local.properties` and set `sdk.dir`.

## Stack

Kotlin Multiplatform, Compose Multiplatform, Decompose (`Child Pages`), Koin, Paparazzi.
