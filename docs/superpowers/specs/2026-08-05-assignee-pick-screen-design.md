# Assignee pick screen

date: 2026-08-05  
repo: `client-app`

## Goal

Replace the inline Wasm-fragile assignee picker on work-order detail with a dedicated full-screen list of engineers. Dispatcher taps current assignee → opens list → taps engineer → PATCH and return.

## Problem

Inline `RadioButton` / nested clickables (and even crowded button lists) inside the scrollable detail screen miss taps on Compose Wasm. A separate screen with a simple list of `AppButton` rows matches patterns that already work (Наставник overlay, «В работу»).

## UI behavior

### Detail (Board, dispatcher only)

- When `editableAssignee` is true, show a single full-width `AppButton` under «Исполнитель»:
  - Label = current assignee display name (`formatAssigneeLabel`) or «не назначен».
  - Variant: Primary if assigned, Secondary if unassigned.
- Tap opens the pick screen. No inline engineer list.
- When `editableAssignee` is false, keep read-only `DetailRow` as today.

### Pick screen (`AssigneePickScreen`)

- Title: «Исполнитель»; back returns to detail without PATCH.
- Load candidates via `userScopesRepository.getCandidates(assetId)` and filter with `filterEngineerEligibleAssignees` (same as today).
- List **only engineers** — no «не назначен» row (clearing assignee is out of scope for this screen).
- Each row: `AppButton` with `formatAssigneeLabel`; current assignee uses Primary, others Secondary.
- Tap engineer → `repository.patch(workOrderId, assigneeId = id)` → on success call `onPicked(updated)` / `onBack` so detail refreshes.
- Loading: progress indicator; error: message + optional retry.

## Navigation

Same overlay pattern as Наставник in `BoardScreen`:

1. `assigneePickOpen` state next to `mentorOpen`.
2. When open and `selectedId != null`, render `AssigneePickScreen` and return (above detail).
3. Pass `workOrderId`, `assetId` (from loaded order), repositories, `currentUserId`, `hasAdminUsers`.
4. On success: close pick screen, bump `reloadKey` / detail reload so the button label updates.

No new `NavDestinationId`.

## Scope

- In: Board dispatcher assignee flow (`BoardScreen` + `WorkOrderDetailScreen` + new screen).
- Out: Tickets / MyWorkOrders (read-only assignee); clearing assignee from UI; shell-level routing.

## Testing

- Keep / extend unit tests for `filterEngineerEligibleAssignees` and labels (no UUID in UI).
- Smoke (Fixaverse Smoke): Доска → open WO → tap assignee button → pick screen → tap RuStore → detail shows name; API `assigneeId` matches.
