# Profile: show organization name

**Status:** approved (approach 1)  
**Date:** 2026-08-03  
**Repos:** feature-service, client-app

## Problem

На экране **Профиль** видны email / имя / фамилия, но не ясно, в какой организации пользователь (Demo vs Smoke и т.п.). Название уже есть в JWT (`urn:zitadel:iam:user:resourceowner:name`), в UI не попадает.

## Solution

### API — `GET /me` (feature-service)

1. `JwtUserExtractor` читает claim `urn:zitadel:iam:user:resourceowner:name` (trim, blank → null).
2. `UserInfoDto` / `MeResponse` получают optional `orgName: String?`.
3. Gateway без изменений (проксирует body as-is).
4. Тест: JWT с claim → `$.userInfo.orgName`; без claim → поле отсутствует / null (не 500).

Не отдаём `orgId` в профильный UI (правило names-not-ids). Id по-прежнему только в API/логах при необходимости — **в этом изменении `orgId` в `/me` не добавляем**.

### Client

1. `UserInfoDto` / `SessionUser` — optional `orgName`.
2. `ClientSession.fromMe` мапит `orgName`.
3. `ProfileScreen` → `ProfileRow(label = "Организация", value = orgName)` рядом с email/именем; если null/blank — строку не показывать.
4. Тесты: decode `/me` с `orgName`; profile row when present / hidden when absent.

## Out of scope

- Смена организации / multi-org switcher
- Показ org id
- Отдельный org directory API
