# RuStore: IMMEDIATE на major + первая публикация приложения инженера

**Date:** 2026-08-01  
**Status:** approved (conversation)  
**Scope:** `client-app` Android (`pro.masterdoc.client`) — in-app updates + RuStore listing/release

## Problem

Инженеры ставят Android-сборку client-app вне стора (debug/sideload). Нет канала доставки новых версий и нет обязательного обновления при несовместимых (major) релизах. В RuStore приложения Fixaverse ещё нет (в аккаунте только KkalScan).

## Goals

1. Опубликовать **приложение инженера** (`client-app`) в RuStore под `pro.masterdoc.client`.
2. При старте (и возврате в foreground) проверять обновление через **RuStore In-App Updates SDK**.
3. Если доступное обновление поднимает **major** относительно установленной версии → запуск **IMMEDIATE**; отмена флоу → выход из приложения.
4. Если обновление только minor/patch → **SILENT** (фон + системный диалог установки).
5. CI: release AAB → загрузка черновика/модерация RuStore (по аналогии с существующим RuStore MCP/flow для других приложений).

## Non-goals

- iOS / App Store / TestFlight.
- Google Play In-App Updates.
- `masterdocapp` (другой продукт).
- Wasm/web auto-refresh (уже отдельный deploy на `app.fixaverse.ru`).
- Тихий install без системного диалога Android (недостижимо без спец. привилегий).
- Backend `minVersion` API в этой итерации (можно добавить позже как усиление).

## Approach (chosen)

**RuStore SDK + сравнение major по схеме `versionCode`.**

RuStore `AppUpdateInfo` даёт доступность обновления и `availableVersionCode` (не полноценный SemVer с сервера). Поэтому:

| Поле | Правило |
|------|---------|
| `versionName` | SemVer `MAJOR.MINOR.PATCH` (человекочитаемо, whatsNew) |
| `versionCode` | `MAJOR * 10_000 + MINOR * 100 + PATCH` (KkalScan scheme; монотонно, из него извлекаем major) |

Пример: `1.0.0` → `10000`; `1.0.20` → `10020`; `2.1.3` → `20103`.

```text
installedMajor = versionCode / 10_000
availableMajor = availableVersionCode / 10_000

if update available:
  if availableMajor > installedMajor → IMMEDIATE
  else → SILENT
```

Первый релиз в сторе: `versionName = 1.0.0`, `versionCode = 10000` (сменить с текущего `0.1.0` / `1` при подготовке release).

## Architecture

```text
App start / onResume (Android)
        │
        ▼
RuStoreAppUpdateManager.getAppUpdateInfo()
        │
        ├─ no update / SDK unavailable → продолжить обычный UI
        │
        └─ UPDATE_AVAILABLE
                │
                ├─ major↑ → startUpdateFlow(IMMEDIATE)
                │              └─ RESULT_CANCELED / ошибка → Activity.finish()
                │
                └─ иначе → startUpdateFlow(SILENT)
                              └─ DOWNLOADED → completeUpdate(SILENT)
```

- Интеграция **только в `androidMain`** (expect/actual или Android entrypoint wrapper вокруг `MainActivity`).
- Common UI не зависит от RuStore; при недоступности SDK пользователь просто работает (нет стора / нет RuStore на устройстве) — для первой волны ок; позже можно добавить soft banner «установите из RuStore».
- Подпись debug для локальной проверки обновлений должна совпадать с upload-подписью, прошедшей модерацию (требование RuStore).

## RuStore publication

### One-time (console)

1. Создать приложение в [RuStore Console](https://console.rustore.ru) с package `pro.masterdoc.client` (API черновиков работает для уже существующего appId; создание пакета — в консоли).
2. Заполнить карточку: название (Fixaverse / приложение инженера), short/full description, категория, возраст 0+, иконка 512×512, скриншоты.
3. Завести release keystore; хранить секреты только в GitHub Secrets / локально вне git.

### Recurring (CI / agent)

1. `bundleRelease` → AAB (на GitHub Actions, не локально по `ci-build-not-local`).
2. Upload AAB + whatsNew → moderation (`INSTANTLY` или `MANUAL` — выбрать при первом прогоне; по умолчанию `INSTANTLY` после успешной модерации истории).
3. После публикации major: инженеры при следующем открытии получают IMMEDIATE.

### Store copy (MVP)

- **Имя:** Fixaverse  
- **Short:** Мобильный клиент инженера Fixaverse — заявки, карта, оборудование.  
- **Full:** короткое описание продукта без сырых UUID в UI-копирайте.  
- Ассеты: положить в `client-app/store/` (icon, screenshots) по образцу других приложений аккаунта.

## Versioning policy

- **Major** — ломающие изменения API/контракта, обязательная миграция клиента, критичный security. Только major включает IMMEDIATE.
- **Minor/Patch** — фичи и фиксы; SILENT, без блокировки входа.
- Не публиковать «фейковый» major ради форса; для экстренного форса без major — отдельная follow-up (backend flag) вне этого спека.

## Error handling

| Ситуация | Поведение |
|----------|-----------|
| RuStore не установлен / старая версия | лог; продолжить работу |
| Нет сети / getAppUpdateInfo fail | лог; продолжить работу |
| IMMEDIATE canceled | `finish()` приложения |
| SILENT fail / user declines install | продолжить; повторить при следующем resume |
| Подпись не совпадает | обновление недоступно; чинить signing, не маскировать |

## Testing

- Unit: парсинг/извлечение major из `versionCode`; выбор IMMEDIATE vs SILENT.
- Instrumented / ручной: сборка с той же подписью, что в консоли; тестовая версия после модерации; сценарии major и patch.
- Не гонять полный release-build локально в агент-сессии без нужды — сборка AAB в CI.

## Success criteria

- Приложение `pro.masterdoc.client` видно в RuStore (после модерации) и ставится на устройство.
- При публикации `2.0.0` поверх `1.x` пользователь не попадает в основной UI без прохождения IMMEDIATE (или выхода).
- При публикации `1.1.0` поверх `1.0.0` — SILENT, без блокирующего флоу.
- Отмена IMMEDIATE закрывает приложение.
- CI умеет залить AAB в RuStore draft/moderation.
- Документация в `client-app/docs` или README: как бампить major и где secrets.

## Open points resolved in conversation

- Канал: **RuStore**, не Play/свой APK-сервер.
- Режим форса: **IMMEDIATE только на major**.
- Приложение: **client-app** (инженер).
- iOS: позже, вне скоупа.
