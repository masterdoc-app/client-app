# Tickets create method picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split Tickets create into list → method picker (QR / list) → form, matching Copilot’s two-step pattern.

**Architecture:** Local navigation state inside `TicketsScreen` (`List` / `Method` / `Form`). Method pane mirrors Copilot: primary QR action centered, secondary «Выбрать из списка» at bottom. Form is the existing emergency create UI extracted from the list. QR reuses `AssetQrPasteDialog` + `onOpenAssetQr`.

**Tech Stack:** Kotlin Multiplatform Compose, existing `AppButton` / `AppScaffold` / `AppText`, commonTest.

## Global Constraints

- UI names only (no raw UUIDs / tokens in copy)
- No camera QR in v1 — paste dialog
- Do not run heavy local Gradle builds; prefer targeted `desktopTest` for unit tests only
- Commit/push to `main` when tasks done; smoke after green CI

---

### Task 1: Create-flow navigation model + tests

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/TicketsScreen.kt` (or small new `TicketsCreateFlow.kt` next to it)
- Modify: `composeApp/src/commonTest/kotlin/pro/masterdoc/client/ui/screens/TicketsPartitionTest.kt` (or new `TicketsCreateFlowTest.kt`)

- [ ] **Step 1: Write failing tests** for `TicketsCreateStep` enum / helpers:
  - default step is `List`
  - `openCreate` → `Method`
  - `chooseList` → `Form`
  - `backFromForm` → `Method`
  - `backFromMethod` → `List`
  - `afterSuccessfulCreate` → `List`
- [ ] **Step 2: Implement** pure navigation helpers (no Compose) until tests pass
- [ ] **Step 3: Commit** `test+feat: tickets create step navigation helpers`

---

### Task 2: Wire TicketsScreen UI to steps (Copilot-like method pane)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/TicketsScreen.kt`

- [ ] **Step 1: List pane** — remove inline create form; top button «Новая заявка» → Method; keep ticket sections + detail navigation
- [ ] **Step 2: Method pane** — `AppScaffold(title = "Новая заявка", onNavigateBack)`:
  - centered primary «Сканировать QR» + hint «Вставьте код или ссылку с наклейки»
  - bottom secondary «Выбрать из списка» + mono/label «или выберите оборудование вручную»
  - QR opens existing paste dialog → `onOpenAssetQr`
- [ ] **Step 3: Form pane** — existing dropdown + description + create; empty-state messages; on success reset fields, reload, go List; back → Method
- [ ] **Step 4: Commit** `feat(tickets): create flow method picker like Copilot`
- [ ] **Step 5: Push main, watch CI** (orchestrator / finishing)

---

## Spec compliance checklist

- [ ] No create form on tickets list
- [ ] Method screen has QR + list actions like Copilot layout intent
- [ ] QR path unchanged functionally
- [ ] List form creates emergency WO as before
- [ ] Back navigation List ← Method ← Form
