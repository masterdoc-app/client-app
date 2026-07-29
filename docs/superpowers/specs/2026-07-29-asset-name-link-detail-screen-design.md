# Asset name links + equipment detail screen

**Date:** 2026-07-29  
**Status:** approved  
**Scope:** `client-app` UI only

## Problem

В клиенте пользователю показывают сырой `assetId` (UUID) — в карточке заявки и в сводке ППР. Это нечитаемо. Нужно имя оборудования, инв. № по hover и переход в полноценную карточку единицы.

## Goal

1. Везде в UI, где сейчас виден ID оборудования, показывать **название**.
2. При наведении мыши — **инвентарный номер** (tooltip).
3. По клику на название — **отдельный экран** карточки оборудования с кнопкой **«Назад»** (не модалка, не скролл/подсветка в списке).

## Non-goals

- Менять API catalog / gateway.
- Заменять `siteId` / другие UUID в UI (только оборудование).
- Отдельный пункт бокового меню для детали.
- Touch long-press как обязательный аналог hover (tooltip на desktop/web; на touch инв. № виден на detail-экране).

## Approach

**Deep link + отдельный экран поверх раздела Equipment** (зеркало `#/ppr/{mapId}`).

- Hash: `#/equipment/{assetId}` → shell `focusedAssetId`.
- Когда `focusedAssetId` задан и destination = Equipment → рендерим `EquipmentDetailScreen`, иначе список `EquipmentScreen`.
- «Назад» сбрасывает фокус (`#/equipment`) и возвращает к списку.

## UI contracts

### `AssetNameLink`

Переиспользуемый composable:

| Состояние | Поведение |
|-----------|-----------|
| Текст | `asset.name` если не пусто, иначе короткий fallback (`assetId.take(8)…`) |
| Hover / tooltip | `Инв. № {inventoryNo}` или `Инв. № не указан` |
| Клик | `BrowserNav.setHash(#/equipment/{id})` + `navigateTo(Equipment, assetId=id)` |
| Стиль | как кликабельный label (primary), не кнопка-блок |

Места замены UUID → `AssetNameLink`:

- `WorkOrderDetailScreen` — строка «Оборудование»
- `ChartsScreen` / `MapSummary` — «Оборудование: …»

Экраны подгружают `listAssets()` (или кэш map `id → AssetDto`) для резолва имени/инв. №. Если ассет не найден — fallback на короткий id, клик всё равно ведёт на detail (detail покажет ошибку/пусто).

### `EquipmentDetailScreen`

Отдельный полноэкранный экран (не overlay-dialog):

- App bar / заголовок: название оборудования + кнопка **«Назад»**
- Тело: те же блоки, что `EquipmentCard` для active/draft (статус, источник, категория, описание, паспорт, документы, связанный ППР, действия delete/move/confirm/reject где применимо)
- Загрузка по `assetId` через `listAssets()` / existing repository; 404 → сообщение «Не найдено» + «Назад»
- Опционально: открытие связанного ППР как сейчас (`onOpenLinkedPpr`)

Рефакторинг: вынести содержимое карточки в shared composable / параметры, чтобы список и detail не дублировали вёрстку.

## Navigation

Расширить существующий паттерн shell:

| Сейчас | Добавить |
|--------|----------|
| `focusedMapId` + `#/ppr/{mapId}` | `focusedAssetId` + `#/equipment/{assetId}` |
| `navigateTo(dest, mapId=?)` | `navigateTo(dest, mapId=?, assetId=?)` **или** отдельный `navigateToEquipment(assetId)` — выбрать один API без ломки вызовов ППР |

`AppDeepLink`:

- `Equipment` (список) — `#/equipment`
- `EquipmentDetail(assetId)` — `#/equipment/{assetId}`

Парсер: если второй сегмент есть → detail, иначе список.

## Success criteria

- В заявке и ППР пользователь не видит полный UUID оборудования как основной текст.
- Hover на имени показывает инв. №.
- Клик открывает отдельный экран с «Назад»; hash = `#/equipment/{id}`.
- «Назад» возвращает к списку оборудования (`#/equipment`).
- Deep link `#/equipment/{id}` при открытии приложения/обновлении страницы ведёт на detail (если feature Equipment доступна).

## Out of immediate polish

- Prefetch одного ассета `GET /assets/{id}` если появится в API (сейчас только list).
- Подсветка строки в списке после «Назад».
