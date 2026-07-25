# client-app

KMP clients for Fixaverse. The **client does not know roles** — only product **features** from `GET /me`.

Roles live in Zitadel (grants/invite). `feature-service` maps roles → union of features. The shell assembles nav and DI from that feature set.

## App (web)

| Path | Module | Behavior |
|------|--------|----------|
| `https://app.fixaverse.ru/` | `:composeApp` | OIDC login → `/me` → feature shell |

## Modules

| Module | Purpose |
|--------|---------|
| `:auth` | OIDC PKCE, token store, `/me` |
| `:design-system` | Shared Fixaverse theme + base Compose UI |
| `:shared` | Feature nav models, session, Decompose shell |
| `:composeApp` | Single entry (Wasm / Desktop / Android) |

## Features

Wire strings align with feature-service (`board`, `user_invite`, `charts`, …). Nav is filtered by `ClientSession.features` (`FeatureId` + `NavCatalog`).

Production features come from `GET https://api.masterdoc.pro/me`.

## Build

```bash
./gradlew :auth:jvmTest :shared:jvmTest
./gradlew :composeApp:wasmJsBrowserDistribution

# OIDC web client id (Zitadel terraform output web_client_id):
./gradlew :composeApp:wasmJsBrowserDistribution -Pfixaverse.oidc.webClientId=YOUR_ID
# or export FIXAVERSE_OIDC_WEB_CLIENT_ID=YOUR_ID
```

## Manual infra (first deploy)

1. DNS: A-record `app` → `91.207.75.72`.
2. On VPS: ensure dir `/var/www/app.fixaverse.ru` (install script creates it).
3. Deploy workflow installs nginx + certbot for `app.fixaverse.ru`.
4. api-gateway env: add `https://app.fixaverse.ru` to `CORS_ORIGINS`, reload gateway.
5. `masterdoc-zitadel` terraform apply (roles + redirect URI `https://app.fixaverse.ru/auth/callback`).
6. Assign product roles to a test user; set GitHub secret `FIXAVERSE_OIDC_WEB_CLIENT_ID`.

## Stack

Kotlin Multiplatform, Compose Multiplatform, Decompose, Koin, Zitadel OIDC via api-gateway.
