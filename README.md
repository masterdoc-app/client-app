# client-app

KMP clients for Fixaverse facility roles.

## Apps (web)

| Path | Module | Role |
|------|--------|------|
| `https://app.fixaverse.ru/` | `:portalApp` | Auth portal → OIDC → redirect by role |
| `https://app.fixaverse.ru/technolog/` | `:technologApp` | `technologist` — Charts, Equipment, Profile |

## Modules

| Module | Role |
|--------|------|
| `:auth` | OIDC PKCE, token store, `/me`, role→path router |
| `:design-system` | Colors, typography, shapes, base Compose UI |
| `:design-system-paparazzi` | Paparazzi snapshot tests |
| `:shared` | Nav models, session, Decompose shell |
| `:portalApp` | Login + callback + role redirect |
| `:technologApp` | Technologist shell (Wasm base `/technolog/`) |
| `:composeApp` | Legacy multi-role stub (desktop/Android/Wasm) |

## Role → path

| Zitadel role | Web path |
|--------------|----------|
| `technologist` | `/technolog/` |
| other | portal shows «нет web-клиента» |

Production features come from `GET https://api.masterdoc.pro/me`.

## Build

```bash
./gradlew :auth:jvmTest :shared:jvmTest
./gradlew :portalApp:wasmJsBrowserDistribution
./gradlew :technologApp:wasmJsBrowserDistribution

# OIDC web client id (Zitadel terraform output web_client_id):
./gradlew :portalApp:wasmJsBrowserDistribution -Pfixaverse.oidc.webClientId=YOUR_ID
# or export FIXAVERSE_OIDC_WEB_CLIENT_ID=YOUR_ID
```

## Manual infra (first deploy)

1. DNS: A-record `app` → `91.207.75.72`.
2. On VPS: ensure dirs `/var/www/app.fixaverse.ru/{portal,technolog}` (install script creates them).
3. Deploy workflow installs nginx + certbot for `app.fixaverse.ru`.
4. api-gateway env: add `https://app.fixaverse.ru` to `CORS_ORIGINS`, reload gateway.
5. `masterdoc-zitadel` terraform apply (role `technologist` + redirect URIs).
6. Assign role `technologist` to a test user; set GitHub secret `FIXAVERSE_OIDC_WEB_CLIENT_ID`.

## Stack

Kotlin Multiplatform, Compose Multiplatform, Decompose, Koin, Zitadel OIDC via api-gateway.
