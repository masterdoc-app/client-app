# Admin roles: feature list editor

**Status:** approved  
**Date:** 2026-08-02  
**Repo:** client-app

## Problem

Вкладка **Админ → Роли** показывает весь каталог фич галочками. Нужен список назначенных фич с добавлением/удалением.

## Solution

UI-only в `RolesTab` (`UsersScreen.kt`). API без изменений (`PUT /admin/roles/{id}` с полным `features`).

Per role:

1. Заголовок роли (`titleRu`)
2. Список назначенных фич: `titleRu` + «Удалить»
3. «Добавить» → `ExposedDropdownMenu` с фичами каталога, которых ещё нет в роли
4. Если добавить нечего — dropdown disabled / скрыт
5. «Сохранить» → текущий `UpdateRoleRequest`

Guards:

- нельзя сохранить пустой список (disable «Сохранить» или client-side message)
- у роли `admin` нельзя убрать фичу `admin` (кнопка «Удалить» disabled/скрыта)

Copy: только человекочитаемые `titleRu`, без raw feature ids в UI.

## Out of scope

Invite role checkboxes, backend, create/delete product roles.
