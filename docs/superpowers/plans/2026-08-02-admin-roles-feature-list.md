# Admin roles feature list Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace checkbox grid in Admin → Roles with an assigned-feature list plus dropdown add / remove.

**Architecture:** UI-only change in `RolesTab`. Keep `selectedByRole: Map<roleId, Set<featureId>>` and the same `updateRole` save path. Render assigned features as rows; add via `ExposedDropdownMenu` of catalog minus selected (pattern from `EngineerScopeScreen` / `TicketsScreen`).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material3, existing `AppText` / `AppButton`.

**Spec:** `docs/superpowers/specs/2026-08-02-admin-roles-feature-list-design.md`

## Global Constraints

- User-facing copy: feature `titleRu` only — never raw feature ids
- No backend / API / DTO changes
- Do not change InviteUserScreen role checkboxes
- Commit only task files (RolesTab + this docs if included); leave unrelated dirty/untracked smoke pngs alone
- Do not run heavy local Gradle builds; CI builds on push
- After commit: push `main` and watch Actions (workspace ship gate)

---

## File map

| File | Change |
|------|--------|
| `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/UsersScreen.kt` | Rewrite `RolesTab` UI |
| `docs/superpowers/specs/2026-08-02-admin-roles-feature-list-design.md` | Already written — include in commit |
| `docs/superpowers/plans/2026-08-02-admin-roles-feature-list.md` | This plan — include in commit |

---

### Task 1: RolesTab list + dropdown editor

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/UsersScreen.kt`

- [ ] **Step 1: Replace checkbox block with list + add dropdown**

In `RolesTab`, for each `role`:

1. Keep title + load/save/`selectedByRole` logic.
2. Remove `Checkbox` rows over full `featureCatalog`.
3. Show assigned features only:
   ```kotlin
   val selected = selectedByRole[role.id].orEmpty()
   val assigned = featureCatalog.filter { it.id in selected }
   val available = featureCatalog.filter { it.id !in selected }
   ```
4. Each assigned row: `AppText(feature.titleRu)` + `AppButton("Удалить", … Secondary)`.
   - If `role.id == "admin" && feature.id == "admin"` → do not offer remove (skip button or `enabled = false`).
5. Add section: `ExposedDropdownMenuBox` + readOnly `OutlinedTextField` labeled «Добавить функцию», menu items = `available` by `titleRu`. On pick → add id to `selectedByRole`.
   - If `available.isEmpty()` → disable field / don't expand.
6. «Сохранить» `enabled = savingId == null && selected.isNotEmpty()` (and keep existing save body).
7. Imports: add `ExposedDropdownMenuBox`, `ExposedDropdownMenuDefaults`, `DropdownMenuItem`, `OutlinedTextField`, `Text`, `menuAnchor` as needed; remove unused `Checkbox` import if unused elsewhere in file.
8. Per-role menu expanded state: `var addMenuExpandedByRole by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }` (or local `var expanded` inside the forEach if Compose allows — prefer map keyed by role id).

Reference pattern: `EngineerScopeScreen.kt` ~156–186.

- [ ] **Step 2: Sanity check**

- No checkboxes left in `RolesTab`
- Save still sends `UpdateRoleRequest(features = selected.sorted(), titleRu = role.titleRu)`
- UI shows `titleRu` only

- [ ] **Step 3: Commit**

```bash
git add \
  composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/UsersScreen.kt \
  docs/superpowers/specs/2026-08-02-admin-roles-feature-list-design.md \
  docs/superpowers/plans/2026-08-02-admin-roles-feature-list.md
git commit -m "$(cat <<'EOF'
Replace admin role feature checkboxes with add/remove list.

EOF
)"
```

- [ ] **Step 4: Push and watch CI**

```bash
git push origin HEAD
gh run watch
```

Report run URL + success/failure.
