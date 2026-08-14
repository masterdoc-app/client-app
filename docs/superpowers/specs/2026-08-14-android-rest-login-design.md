# Android REST login (RuStore) — design

**Date:** 2026-08-14  
**Status:** approved (conversation; continue without further gates)  
**Repos:** `client-app` (Android), `api-gateway-service`, docs in `masterdoc-zitadel`

## Problem

RuStore отклонил `1.0.1(10001)`: «Приложение перебрасывает в браузер». Android сейчас стартует OIDC Authorization Code + PKCE через `Intent.ACTION_VIEW` на Zitadel (`AuthCoordinator.startLogin` → `BrowserNav.navigateTo`). Для стора нужен вход **внутри приложения** без внешнего браузера / Custom Tabs.

## Goals

1. Android: экран email+пароль → `POST /auth/login` на gateway → те же JWT (`access` / `refresh` / `id`), что после OIDC callback.
2. После логина — существующий `GET /me` и feature shell без ветвления по IdP grants.
3. Web/Wasm: без изменений (браузерный OIDC).
4. Пароли по-прежнему **только** в Zitadel; gateway не хранит пароли.

## Non-goals

- iOS / desktop native password UI (desktop остаётся на OIDC, если используется).
- Замена web OIDC UI на свою форму.
- MFA / passwordless / social.
- Resource Owner Password Credentials (ROPC) как публичный grant у OIDC app.
- Смена модели токенов / отказ от Zitadel.

## Decisions (locked)

| Тема | Выбор |
|------|--------|
| Платформа | Только Android |
| Транспорт | Gateway BFF `POST /auth/login` |
| IdP механика | Zitadel **Session API** + finalize OIDC auth request → code → token |
| Web | OIDC + браузер как сейчас |
| Logout Android | Локальный clear → `LoginScreen` (без браузера) |
| Refresh | Без изменений: `POST /auth/token` + `refresh_token` |

## Architecture

```text
LoginScreen (Compose)
  → POST https://api.masterdoc.pro/auth/login
       { email, password, client_id }
  → gateway BFF (server-side, no device browser):
       1. PKCE + GET {issuer}/oauth/v2/authorize (no follow) → authRequestId from Location
       2. POST /v2/sessions  checks: user.loginName + password
          Authorization: Bearer ZITADEL_MGMT_TOKEN (must include IAM_LOGIN_CLIENT)
       3. POST /v2/oidc/auth_requests/{id}  { session: { sessionId, sessionToken } }
          → callbackUrl with ?code=
       4. POST /oauth/v2/token  authorization_code + PKCE (existing ZitadelTokenClient)
  ← TokenResponse (same shape as /auth/token)
  → TokenStore + GET /me → shell
```

Клиент **не** открывает `auth.fixaverse.ru`. Клиент знает только gateway.

### Why Session API (not ROPC)

- Terraform native app уже: `authorization_code` + `refresh_token` only.
- Zitadel рекомендует Session + finalize для custom/native login UI.
- Пароль не оформляется как OAuth password grant наружу.

## API contract

### `POST /auth/login` (public)

Request JSON:

```json
{
  "email": "user@company.ru",
  "password": "…",
  "client_id": "<native OIDC client id>"
}
```

- `redirect_uri` клиент **не** передаёт: gateway подставляет `masterdoc://auth/callback`.
- PKCE генерируется и живёт только внутри одного запроса на gateway.

Response **200**: тело как у Zitadel token endpoint / текущего `POST /auth/token`  
(`access_token`, `refresh_token`, `id_token`, `token_type`, `expires_in`, …).

Errors:

| Status | Когда |
|--------|--------|
| 400 | Пустые `email`/`password`/`client_id` |
| 401 | Неверный логин/пароль / session check failed (одно сообщение, без «email не найден») |
| 502 | Zitadel недоступен / неожиданный upstream |

Обновить: `api-gateway-service/openapi.yaml`, `docs/AUTH.md`.  
В `masterdoc-zitadel/docs/AUTHORIZATION.md`: заменить «не делаем POST /auth/login» на «пароли не храним; Android BFF `POST /auth/login` проксирует Session API».

## Android UI

- `LoginScreen`: бренд, email, пароль (`AppTextField`), «Войти» (`AppButton`), ошибка, loader.
- Нет WebView / Custom Tabs / `ACTION_VIEW` на authorize при bootstrap без сессии.
- Bootstrap Android: нет сессии → `LoginScreen`; web: как сейчас `startLogin()` → browser.
- Logout Android: `coordinator.logout()` → `LoginScreen`; не `logoutRedirectUrl()`.

## Client wiring

- `AuthRepository.loginWithPassword(email, password)` → JSON `POST /auth/login` с `config.clientId`.
- `AuthCoordinator.loginWithPassword` → tokens + `meRepository.getMe()`.
- Android `appAuthConfig()`: **native** OIDC `client_id` (`GeneratedAuthDefaults.NATIVE_CLIENT_ID` / `FIXAVERSE_OIDC_NATIVE_CLIENT_ID`), redirect `masterdoc://auth/callback`.
- Web/desktop: по-прежнему web client id + свои redirect.

## Gateway internals

Новые типы (имена ориентир):

- `ZitadelLoginClient` — session create + auth request start/finalize (PAT = `config.zitadelMgmtToken`).
- `AuthLoginRoutes` — `POST /auth/login`.
- Переиспользовать `ZitadelTokenClient.exchange` для code→tokens.

Ops: `ZITADEL_MGMT_TOKEN` обязан иметь роль **`IAM_LOGIN_CLIENT`** (bootstrap `login-client` PAT уже может её иметь — проверить; если нет — выдать роль / отдельный PAT и задокументировать в `SECRETS_AND_DOMAINS.md`).

Scopes authorize (gateway): те же, что клиент:  
`openid profile email offline_access urn:zitadel:iam:user:resourceowner`.

## Error handling (UX)

- Сеть / 502 → «Сервис входа временно недоступен».
- 401 → «Неверный email или пароль».
- 400 → «Проверьте email и пароль».
- Не логировать password; в логах gateway — только `email` hash/prefix или requestId + статус upstream.

## Testing

**Gateway (unit / testApplication):**

- 200: fake login client + token client → тело с `access_token`.
- 400: blank fields.
- 401: session failure.
- 502: upstream unavailable.

**Client (`AuthRepositoryTest`):**

- `loginWithPassword` шлёт JSON на `/auth/login`, пишет `TokenStore`.
- 401 → `GatewayHttpException`.

**Smoke (после deploy):** Android / эмулятор или instrumented path: логин `mail+rustore@…` без открытия браузера; `GET /me` ок. Ручной чеклист RuStore: нет редиректа в браузер на холодном старте.

## Out of scope / later

- In-app WebView fallback.
- Unified password UI for web.
- Separate `ZITADEL_LOGIN_CLIENT_TOKEN` env (пока reuse MGMT PAT).

## Success criteria

1. Холодный старт Android без сессии показывает форму, **не** системный браузер.
2. Успешный логин даёт тот же доступ к API, что OIDC.
3. Web login не регрессирует.
4. RuStore-модерация не видит «перебрасывает в браузер» на auth.
