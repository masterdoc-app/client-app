# PPR list cards — design system redesign

**Date:** 2026-07-29  
**Status:** approved  
**Scope:** `client-app` UI — `ChartsScreen` / maintenance map list

## Problem

Экран `#/ppr` показывает карты ППР плоским текстом (`MapSummary`): без карточного chrome, без `AppStatusChip`, со статусом в строке заголовка. Визуально расходится с `EquipmentCard` (Fixaverse Lite DS).

## Goals

1. Список ППР — карточки в том же языке, что оборудование: `Surface` + border + левая accent-полоска + `ClientSpacing` + чипы.
2. Статус только чипами: черновик → «Черновик» (Accent), active → «В базе» (Neutral). Слово «активна» не использовать.
3. Документ — `DocRow`-подобная строка только если файл есть; никогда «Документ: не привязан…».
4. Жизненный цикл: отдельного «Удалить» у ППР нет — карта удаляется вместе с оборудованием. На черновике только «Подтвердить» / «Отклонить».

## Non-goals

- Новый shared `AppCard` в `:design-system`.
- Отдельный экран детали ППР.
- Каскадное удаление maps на бэкенде (уже вне UI; UI не добавляет delete).
- Редизайн `LinkedPprBlock` на карточке оборудования (можно позже выровнять).

## Card layout (`MaintenanceMapCard`)

Hierarchy (зеркало EquipmentCard):

1. **Shell** — `Surface` (`shapes.large`), `1.dp` `outlineVariant` border, 4dp left accent strip. Draft: лёгкий elevation; highlighted (`focusedMapId`): accent = primary.
2. **Chips** — `AppStatusChip` статус + `AppStatusChip` источник (`ИИ` / `вручную`, Muted, без dot).
3. **Title** — `AppTextStyle.Title` = `map.title` (без inline `· статус · источник`).
4. **Оборудование** — label + `AssetNameLink` → `#/equipment/{id}`.
5. **Документ** — при наличии: кликабельная строка (иконка + filename), как DocRow.
6. **Пункты** — превью через существующие `visibleMapItems` / `mapItemsOverflowLabel` (limit 5).
7. **Draft actions** — внутри карточки: Primary «Подтвердить», Secondary «Отклонить», `fillMaxWidth=false`, `weight(1f)`.

## Screen

`ChartsScreen` сохраняет секции «Черновики ППР» / «ППР в базе» и intro copy. `MapSummary` / `MapDraftRow` заменяются на `MaintenanceMapCard` (отдельный файл или рядом в screens).

## Labels

| Status | Chip |
|--------|------|
| `draft` | Черновик |
| `active` | В базе |
| (иное) | raw status |

`mapHeadline` / `ruStatus("active") → ""` остаются для тестов совместимости или удаляются, если больше не используются в UI.

## Success criteria

- На `#/ppr` карты выглядят как карточки DS, не как plain text list.
- Нет «активна», нет «не привязан».
- Нет кнопки удаления ППР.
- Черновик: подтверждение/отклонение внутри карточки.
- Deep link `#/ppr/{id}` подсвечивает карточку accent-полоской.
- Unit-тесты на label helpers + expand пунктов зелёные; desktopTest для затронутых классов.
