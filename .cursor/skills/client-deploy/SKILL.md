---
name: client-deploy
description: >-
  Деплой client-app через git→CI: добавить файлы, коммит, пуш в main, дождаться
  тестов и деплоя, затем мок/smoke тесты нового и регрессии. Триггер: /client-deploy,
  client-deploy, задеплой клиент, выкати app.
---

# /client-deploy

Репозиторий: `client-app`. Сборка **только в GitHub Actions** — локально `./gradlew` / Wasm не гонять.

## Checklist

```
- [ ] 1. Все новые/изменённые файлы в git
- [ ] 2. Коммит
- [ ] 3. Пуш в main (мастер)
- [ ] 4. CI тесты зелёные (иначе разобрать)
- [ ] 5. Деплой клиента завершён
- [ ] 6. Мок/smoke тесты: новое + регрессия
```

## 1. Git: add → commit → push main

Из корня `client-app`:

1. `git status` / `git diff` / `git log -5 --oneline` — понять scope
2. Добавить **все** новые и нужные изменённые файлы (`git add`), кроме секретов (`.env`, ключи)
3. Коммит с сообщением по стилю репо (why > what), через HEREDOC
4. Пуш в **main** (в разговоре — «мастер»; default branch репо = `main`):

```bash
git push -u origin HEAD:main
# или merge/PR → main, если работали на feature-ветке и нужен review
```

Если сейчас не на `main`: либо merge в `main` и push, либо PR + merge — затем ждать CI на `main`. Не пушить force в `main`.

## 2. Дождаться тестов; если fail — разобраться

```bash
gh run list --branch main --limit 5
gh run watch <run-id>   # или следить за последним run после push
gh run view <run-id> --log-failed
```

- Fail → читать лог, чинить, **новый** коммит + push, снова ждать. Не пропускать.
- Нет отдельного test workflow — считать «тестами» job `build` (и любые check runs на commit).

## 3. Дождаться деплоя клиента

Workflow: `.github/workflows/deploy-app-fixaverse.yml`  
Triggers: push на `main` / `trunk`, `workflow_dispatch`.

Jobs: `Build composeApp Wasm` → `Deploy to app.fixaverse.ru`.

```bash
gh run list --workflow=deploy-app-fixaverse.yml --branch main --limit 3
gh run watch <run-id>
```

Успех = оба job зелёные.  
Типичные блокеры deploy: нет secrets `DEPLOY_SSH_PRIVATE_KEY`, `DEPLOY_USER`, `FIXAVERSE_OIDC_WEB_CLIENT_ID`.

Прод: `https://app.fixaverse.ru/` (единый feature shell).

## 4. Мок / smoke тесты после деплоя

По **diff этого коммита** (и связанных коммитов до merge в main):

1. Список нового поведения из коммита
2. Список соседнего функционала, который могли сломать (auth, `/me`, feature nav)

Прогнать в браузере (Playwright / cursor-ide-browser) против прода:

| Зона | Smoke |
|------|--------|
| App | `/` грузится, OIDC login стартует |
| Auth | OIDC redirect / callback не 500; после логина shell по features из `/me` |
| Shell | nav пункты соответствуют features (не ролям) |
| Legacy | `/technolog/` → `/` |
| Регрессия | CORS, `/me`, feature flags |

«Мок» = без полной E2E-инфры: ручной/browser сценарий с тестовым юзером или заглушками там, где секреты/пароль недоступны; зафиксировать что проверено и что заблокировано (например нет пароля).

## Анти-паттерны

- Локальная Wasm/Gradle production-сборка «чтобы быстрее задеплоить»
- Пуш без ожидания CI
- «Деплой ок» без проверки URL и сценариев коммита
- Коммит `.env` / SSH ключей / client secrets в репо
