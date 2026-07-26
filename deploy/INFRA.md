# First-time ops for https://app.fixaverse.ru

## DNS

Create A-record: `app.fixaverse.ru` → `91.207.75.72` (web VPS).

## Zitadel (masterdoc-zitadel)

1. Apply terraform (roles + redirect URI `https://app.fixaverse.ru/auth/callback`).
2. Note output `web_client_id` → GitHub secret `FIXAVERSE_OIDC_WEB_CLIENT_ID` on `client-app`.
3. Grant product roles to a test user (features come from feature-service union).

## api-gateway

On VPS `/etc/masterdoc-api-gateway/.env` (or equivalent), set:

```bash
CORS_ORIGINS=https://app.fixaverse.ru,https://copilot.fixaverse.ru,https://copilot.masterdoc.pro,http://localhost:8080
```

Redeploy / restart gateway so CORS picks up the origin.

## feature-service

Deploy build that maps roles → features (`technologist` → `charts`/`equipment`, grant `admin` for Админ, …).

## Web deploy

GitHub Actions workflow `.github/workflows/deploy-app-fixaverse.yml` builds `:composeApp` and rsyncs to:

- `/var/www/app.fixaverse.ru/`

Install script enables nginx + certbot for `app.fixaverse.ru`.

## Caching

- HTML + `/composeApp.js` (stable names): `Cache-Control: no-cache` — revalidate every load.
- Content-hashed `*.wasm` (webpack filenames): `public, max-age=2592000, immutable` — cold return visits skip re-download of ~11 MB Skiko+app.

Do not long-cache `composeApp.js`; do not `no-cache` hashed wasm.

## Smoke

1. Open `https://app.fixaverse.ru/` → OIDC login.
2. After login → shell with nav items for features from `/me` (e.g. ППР / Оборудование / Профиль).
3. Legacy `/technolog/` redirects to `/`.
4. `curl -sSI https://app.fixaverse.ru/composeApp.js | grep -i cache-control` → `no-cache`.
5. `curl -sSI https://app.fixaverse.ru/<hashed>.wasm | grep -i cache-control` → `immutable` (or `max-age=2592000`).
