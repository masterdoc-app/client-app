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

Deploy build that maps roles → features (`technologist` → `charts`/`equipment`, `admin` → `user_invite`, …).

## Web deploy

GitHub Actions workflow `.github/workflows/deploy-app-fixaverse.yml` builds `:composeApp` and rsyncs to:

- `/var/www/app.fixaverse.ru/`

Install script enables nginx + certbot for `app.fixaverse.ru`.

## Caching

Shell bundles keep stable names (`/composeApp.js`). Nginx sends `Cache-Control: no-cache` for HTML/JS/Wasm so the browser revalidates on every load (ETag → 304 if unchanged, fresh body after deploy). Do not long-cache those files.

## Smoke

1. Open `https://app.fixaverse.ru/` → OIDC login.
2. After login → shell with nav items for features from `/me` (e.g. ППР / Оборудование / Профиль).
3. Legacy `/technolog/` redirects to `/`.
4. `curl -sSI https://app.fixaverse.ru/composeApp.js | grep -i cache-control` → `no-cache`.
