# Tickets create flow — method picker (like Copilot)

date: 2026-08-03  
repo: `client-app`

## Goal

Разбить создание аварийной заявки на шаги, как на `copilot.fixaverse.ru`: сначала выбор способа (QR / список), потом форма. Список заявок не смешивать с формой.

## Product decisions

| Decision | Choice |
| --- | --- |
| Entry | Вариант **A**: отдельный экран после «Новая заявка» |
| Reference UX | Copilot ScanMainPane: hero QR сверху/по центру, список — secondary внизу |
| QR v1 | Существующий paste-диалог (`AssetQrPasteDialog`) → `AssetQrScreen` (камера out of scope) |
| List path | Текущая форма: dropdown оборудования + описание + «Создать» |
| Empty states | Без scope / без оборудования — сообщения на шаге формы (и при необходимости на выборе) |

## Flow

```text
Заявки (list)
  [Новая заявка]
  Активные / Завершённые
       │
       ▼
Новая заявка (method)
  center: [Сканировать QR] + hint «Вставьте код или ссылку с наклейки»
  bottom: [Выбрать из списка] + «или выберите оборудование вручную»
  back → list
       │
       ├─ QR → AssetQrPasteDialog → AssetQrScreen
       └─ List → форма (оборудование, описание, Создать)
                 back → method
                 success → list (reload)
```

## UI names

- Только названия оборудования / заявок; без id в copy.
- Fallback: «Оборудование», «Заявка».

## Out of scope

- Камера QR (как hero на copilot)
- Изменение API создания WO
- Engineer «Мои заявки» create path

## Success

1. На списке «Заявок» нет inline-формы создания.
2. «Новая заявка» → экран с двумя действиями QR / список.
3. QR и список ведут к созданию emergency WO как сейчас.
4. Назад по шагам работает.
