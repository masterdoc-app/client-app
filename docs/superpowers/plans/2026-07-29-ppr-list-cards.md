# PPR list cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign `#/ppr` maintenance map list as EquipmentCard-style DS cards without separate PPR delete.

**Architecture:** Extract `MaintenanceMapCard` composable mirroring `EquipmentCard` shell (Surface + accent strip + chips). Wire it from `ChartsScreen` for drafts and active maps; keep existing reload/confirm/reject and expand helpers.

**Tech Stack:** Kotlin Multiplatform Compose, client-app `:design-system` (`AppText`, `AppStatusChip`, `AppButton`, `ClientSpacing`), existing `EquipmentRepository`.

## Global Constraints

- No «активна» copy; active status chip = «В базе».
- Never show «Документ: не привязан…»; document row only when file exists.
- No «Удалить» on PPR cards — lifecycle tied to equipment.
- Prefer `ClientSpacing` over hardcoded dp.
- TDD for pure label helpers; `./gradlew :composeApp:desktopTest` for touched tests (no full wasm dist locally).
- Commit after each task; push/watch CI only after all tasks (or per workspace commit-push rules at end).

---

## File map

| File | Role |
|------|------|
| `composeApp/.../screens/MaintenanceMapCard.kt` | New card composable |
| `composeApp/.../screens/ChartsScreen.kt` | Use card; drop flat MapSummary/MapDraftRow UI |
| `composeApp/.../commonTest/.../ChartsScreenLabelsTest.kt` | Status chip labels / headline |
| `composeApp/.../commonTest/.../MapItemsExpandTest.kt` | Keep green |
| `docs/superpowers/specs/2026-07-29-ppr-list-cards-design.md` | Spec (already written) |

---

### Task 1: Status chip label helpers (TDD)

**Files:**
- Modify: `ChartsScreen.kt` (or small helpers in `MaintenanceMapCard.kt` / shared)
- Test: `ChartsScreenLabelsTest.kt`

- [ ] **Step 1:** Add failing tests: `pprStatusChipLabel("draft") == "Черновик"`, `pprStatusChipLabel("active") == "В базе"`, `pprSourceChipLabel("ai_generated") == "ИИ"`.
- [ ] **Step 2:** Run `:composeApp:desktopTest --tests '...ChartsScreenLabelsTest'` — RED.
- [ ] **Step 3:** Implement helpers.
- [ ] **Step 4:** Tests GREEN.
- [ ] **Step 5:** Commit `test+feat(ui): PPR status chip labels for map cards`.

---

### Task 2: `MaintenanceMapCard` composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/MaintenanceMapCard.kt`

- [ ] **Step 1:** Implement card shell + chips + title + AssetNameLink + optional doc row + items preview + draft buttons per spec.
- [ ] **Step 2:** Reuse `visibleMapItems` / `mapItemsOverflowLabel` / `ruKind` / `ruIntervalUnit` from ChartsScreen (keep `internal`).
- [ ] **Step 3:** No delete button.
- [ ] **Step 4:** Commit `feat(ui): add MaintenanceMapCard for PPR list`.

---

### Task 3: Wire `ChartsScreen`

**Files:**
- Modify: `ChartsScreen.kt`

- [ ] **Step 1:** Replace `MapDraftRow` / `MapSummary` usage with `MaintenanceMapCard`.
- [ ] **Step 2:** Pass `highlighted = map.id == focusedMapId`, confirm/reject only for drafts.
- [ ] **Step 3:** Use `ClientSpacing` for screen column spacing.
- [ ] **Step 4:** Remove dead private composables if unused; keep helpers needed by card/tests.
- [ ] **Step 5:** Run desktopTest for ChartsScreenLabelsTest + MapItemsExpandTest — GREEN.
- [ ] **Step 6:** Commit `feat(ui): render PPR list with MaintenanceMapCard`.

---

### Task 4: Push and verify CI

- [ ] **Step 1:** Push `main` (or feature branch if not on main).
- [ ] **Step 2:** `gh run watch` Deploy app.fixaverse.ru — report success/failure.
