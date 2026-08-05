# Assignee Pick Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the inline assignee picker on Board work-order detail with a dedicated full-screen engineer list (tap current assignee → pick screen → tap engineer → PATCH → back).

**Architecture:** Mirror the Наставник overlay in `BoardScreen`. New `AssigneePickScreen` loads the work order, candidates, and admin users, then shows `AppButton` rows. Detail shows one assignee button that opens the overlay. Reuse `formatAssigneeLabel` and `filterEngineerEligibleAssignees`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `kotlin.test`, existing `AppScaffold` / `AppButton`

**Spec:** `docs/superpowers/specs/2026-08-05-assignee-pick-screen-design.md`

## Global Constraints

- Never show raw UUIDs / user ids in UI — use `formatAssigneeLabel` / generic «Пользователь».
- Pick screen lists **only engineers** (no «не назначен» clear row).
- Editable assignee only on Board dispatcher path (`editableAssignee = true`).
- Prefer `AppButton` for list rows (Wasm-reliable clicks).
- No new `NavDestinationId`.
- Do not run heavy local Gradle builds; after merge to main, CI builds. Local: focused `desktopTest` only if needed for TDD.

---

### Task 1: Create `AssigneePickScreen`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/AssigneePickScreen.kt`
- Test: `composeApp/src/commonTest/kotlin/pro/masterdoc/client/ui/screens/AssigneeLabelTest.kt` (no change required unless a tiny helper is extracted; prefer reusing existing filters)

**Interfaces:**
- Consumes: `WorkOrdersRepository.patch`, `UserScopesRepository.getCandidates`, `AdminUsersRepository.listUsers`, `formatAssigneeLabel`, `filterEngineerEligibleAssignees`
- Produces:

```kotlin
@Composable
fun AssigneePickScreen(
    workOrderId: String,
    repository: WorkOrdersRepository,
    userScopesRepository: UserScopesRepository,
    adminUsersRepository: AdminUsersRepository?,
    hasAdminUsers: Boolean,
    currentUserId: String?,
    onBack: () -> Unit,
    onAssigned: (WorkOrderDto) -> Unit = { onBack() },
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 1: Add `AssigneePickScreen.kt`**

Implement roughly:

```kotlin
package pro.masterdoc.client.ui.screens

// imports: Compose layout, CircularProgressIndicator, AppScaffold, AppButton,
// WorkOrdersRepository, UserScopesRepository, AdminUsersRepository, GatewayHttpException,
// CancellationException, ClientSpacing, etc.

@Composable
fun AssigneePickScreen(
    workOrderId: String,
    repository: WorkOrdersRepository,
    userScopesRepository: UserScopesRepository,
    adminUsersRepository: AdminUsersRepository?,
    hasAdminUsers: Boolean,
    currentUserId: String?,
    onBack: () -> Unit,
    onAssigned: (WorkOrderDto) -> Unit = { onBack() },
    modifier: Modifier = Modifier,
) {
    var order by remember { mutableStateOf<WorkOrderDto?>(null) }
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var assigning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, workOrderId, userScopesRepository, adminUsersRepository, hasAdminUsers) {
        loading = true
        error = null
        try {
            val wo = repository.get(workOrderId)
            order = wo
            candidates = userScopesRepository.getCandidates(wo.assetId)
            users =
                if (hasAdminUsers && adminUsersRepository != null) {
                    try {
                        adminUsersRepository.listUsers(limit = 200).items
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки"
        } finally {
            loading = false
        }
    }

    fun pick(userId: String) {
        if (assigning) return
        scope.launch {
            assigning = true
            error = null
            try {
                val updated = repository.patch(workOrderId, assigneeId = userId)
                onAssigned(updated)
            } catch (e: Exception) {
                error = e.message ?: "Не удалось назначить исполнителя"
            } finally {
                assigning = false
            }
        }
    }

    AppScaffold(title = "Исполнитель", modifier = modifier, onNavigateBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(ClientSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            when {
                loading -> CircularProgressIndicator()
                error != null && order == null -> {
                    AppText(text = error!!)
                    AppButton(text = "Назад", onClick = onBack)
                }
                else -> {
                    val wo = order!!
                    val current = wo.assigneeId?.takeIf { it.isNotBlank() }
                    val eligible = filterEngineerEligibleAssignees(candidates, users)
                    if (error != null) AppText(text = error!!)
                    if (eligible.isEmpty()) {
                        AppText(text = "Нет инженеров в зоне ответственности")
                    }
                    eligible.forEach { userId ->
                        AppButton(
                            text = formatAssigneeLabel(userId, users, currentUserId),
                            onClick = { if (userId != current) pick(userId) },
                            enabled = !assigning,
                            variant =
                                if (userId == current) {
                                    AppButtonVariant.Primary
                                } else {
                                    AppButtonVariant.Secondary
                                },
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/AssigneePickScreen.kt
git commit -m "feat(board): add AssigneePickScreen for engineer selection"
```

---

### Task 2: Replace inline picker on detail with open button

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/WorkOrderDetailScreen.kt`

**Interfaces:**
- Consumes: `formatAssigneeLabel`, `directoryUsers`, `editableAssignee`
- Produces: new parameter `onOpenAssigneePick: (() -> Unit)? = null`
- Removes: private `AssigneePickerRow`, `AssigneeOptionRow` (and unused imports tied only to them)

- [ ] **Step 1: Add callback parameter**

On `WorkOrderDetailScreen`:

```kotlin
onOpenMentor: (() -> Unit)? = null,
onOpenAssigneePick: (() -> Unit)? = null,
onOpenEquipment: (String) -> Unit = {},
```

- [ ] **Step 2: Replace assignee UI block**

Replace the `if (editableAssignee && userScopesRepository != null) { AssigneePickerRow(...) } else { DetailRow(...) }` block with:

```kotlin
if (editableAssignee && onOpenAssigneePick != null) {
    val assigneeLabel =
        wo.assigneeId?.takeIf { it.isNotBlank() }?.let { id ->
            formatAssigneeLabel(id, directoryUsers, currentUserId)
        } ?: "не назначен"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(text = "Исполнитель", style = AppTextStyle.Label)
        AppButton(
            text = assigneeLabel,
            onClick = onOpenAssigneePick,
            variant =
                if (wo.assigneeId.isNullOrBlank()) {
                    AppButtonVariant.Secondary
                } else {
                    AppButtonVariant.Primary
                },
            modifier = Modifier.fillMaxWidth(),
        )
    }
} else {
    val assignee =
        wo.assigneeId?.takeIf { it.isNotBlank() }?.let { id ->
            formatAssigneeLabel(id, directoryUsers, currentUserId)
        } ?: "не назначен"
    DetailRow("Исполнитель", assignee)
}
```

- [ ] **Step 3: Delete `AssigneePickerRow` and `AssigneeOptionRow`**

Remove both private composables entirely. Keep `formatAssigneeLabel`, `filterEngineerEligibleAssignees`, and related helpers (still used by pick screen / tests / board cards).

Clean unused imports that only served the deleted composables (`UserScopesRepository` may become unused in this file if only picker used it — check call sites; if unused, remove the parameter only if nothing else needs it; prefer leave `userScopesRepository` param if still required by callers for binary compat, or remove from detail signature if unused).

If `userScopesRepository` is unused after deletion, remove it from `WorkOrderDetailScreen` parameters and update all call sites in Task 3.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/WorkOrderDetailScreen.kt
git commit -m "feat(board): open assignee pick screen from detail button"
```

---

### Task 3: Wire `BoardScreen` overlay like Наставник

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/BoardScreen.kt`
- Modify call sites of `WorkOrderDetailScreen` if Task 2 removed `userScopesRepository` from detail: `TicketsScreen.kt`, `MyWorkOrdersScreen.kt` (compile fixes only)

**Interfaces:**
- Consumes: `AssigneePickScreen`, existing board repositories
- Produces: `assigneePickOpen` state; detail `onOpenAssigneePick = { assigneePickOpen = true }`

- [ ] **Step 1: Add overlay state**

Next to `mentorOpen`:

```kotlin
var assigneePickOpen by remember { mutableStateOf(false) }
```

Reset on back from detail:

```kotlin
onBack = {
    mentorOpen = false
    assigneePickOpen = false
    selectedId = null
},
```

- [ ] **Step 2: Render pick screen above detail**

After the mentor block (same pattern), before detail:

```kotlin
if (assigneePickOpen && selectedId != null && userScopesRepository != null) {
    AssigneePickScreen(
        workOrderId = selectedId!!,
        repository = repository,
        userScopesRepository = userScopesRepository,
        adminUsersRepository = adminUsersRepository,
        hasAdminUsers = hasAdminUsers,
        currentUserId = currentUserId,
        onBack = { assigneePickOpen = false },
        onAssigned = {
            assigneePickOpen = false
            reloadKey++
        },
        modifier = modifier,
    )
    return
}
```

Pass into detail:

```kotlin
onOpenAssigneePick =
    if (dispatcherMode && userScopesRepository != null) {
        { assigneePickOpen = true }
    } else {
        null
    },
```

Ensure `editableAssignee` stays `dispatcherMode && userScopesRepository != null`.

- [ ] **Step 3: Fix compile at other `WorkOrderDetailScreen` call sites**

If Task 2 removed unused params, update `TicketsScreen` / `MyWorkOrdersScreen` accordingly. Do not enable assignee pick outside Board.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/BoardScreen.kt
# plus any call-site fixes
git commit -m "feat(board): wire AssigneePickScreen overlay from Board"
```

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| Single assignee button on detail | Task 2 |
| Pick screen title «Исполнитель» | Task 1 |
| Engineers only, no clear row | Task 1 |
| AppButton rows + Primary current | Task 1 |
| PATCH then back | Task 1 |
| Board overlay like mentor | Task 3 |
| No new NavDestination | Task 3 |
| Labels never UUID | Task 1 (reuse helpers) |

## After all tasks

Push branch, open/merge to main per finishing-a-development-branch, watch CI deploy, run `/smoke-test` on Fixaverse Smoke: Доска → заявка → кнопка исполнителя → список → клик инженера → имя на деталке.
