# Add Photo Disclosure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide the new-work-order photo source actions behind one «Добавить фото» disclosure button.

**Architecture:** Keep the existing gallery and camera launchers unchanged. Add one pure state transition to `TicketsCreateFlow.kt`, cover it with a common test, and use that transition from local Compose state in `TicketsScreen`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `kotlin.test`

## Global Constraints

- Only the new work order form changes.
- Keep image previews, removal, picker callbacks, upload behavior, errors, and the 10-photo limit unchanged.
- Use the exact user-facing labels «Добавить фото», «С диска», and «Камера».
- Reset the disclosure when the user leaves the form.

---

### Task 1: Add and integrate photo-source disclosure state

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/TicketsCreateFlow.kt`
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/TicketsScreen.kt`
- Test: `composeApp/src/commonTest/kotlin/pro/masterdoc/client/ui/screens/TicketsCreateFlowTest.kt`

**Interfaces:**
- Produces: `fun togglePhotoSourceActions(expanded: Boolean): Boolean`
- Consumes: existing `imagePickers.openGallery`, `imagePickers.openCamera`, `createFormEnabled`, `pendingPhotos`, and `acting`

- [ ] **Step 1: Write the failing state-transition test**

Add these assertions to `TicketsCreateFlowTest`:

```kotlin
@Test
fun photoSourceActionsToggleOpenAndClosed() {
    assertEquals(true, togglePhotoSourceActions(false))
    assertEquals(false, togglePhotoSourceActions(true))
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests pro.masterdoc.client.ui.screens.TicketsCreateFlowTest
```

Expected: compilation fails because `togglePhotoSourceActions` is unresolved.

- [ ] **Step 3: Add the minimal transition**

Add to `TicketsCreateFlow.kt`:

```kotlin
fun togglePhotoSourceActions(expanded: Boolean): Boolean = !expanded
```

- [ ] **Step 4: Integrate the disclosure in the form**

In `TicketsScreen`, add:

```kotlin
var photoSourceActionsExpanded by remember { mutableStateOf(false) }
```

Reset it whenever the active create step is not the form:

```kotlin
LaunchedEffect(createStep) {
    if (createStep != TicketsCreateStep.Form) {
        photoSourceActionsExpanded = false
    }
}
```

Replace the always-visible source row with:

```kotlin
val photoControlsEnabled = createFormEnabled && pendingPhotos.size < 10 && !acting
AppButton(
    text = "Добавить фото",
    variant = AppButtonVariant.Secondary,
    modifier = Modifier.fillMaxWidth(),
    enabled = photoControlsEnabled,
    onClick = {
        photoSourceActionsExpanded = togglePhotoSourceActions(photoSourceActionsExpanded)
    },
)
if (photoSourceActionsExpanded) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        AppButton(
            text = "С диска",
            variant = AppButtonVariant.Secondary,
            modifier = Modifier.weight(1f),
            enabled = photoControlsEnabled,
            onClick = imagePickers.openGallery,
            fillMaxWidth = false,
        )
        AppButton(
            text = "Камера",
            variant = AppButtonVariant.Secondary,
            modifier = Modifier.weight(1f),
            enabled = photoControlsEnabled,
            onClick = imagePickers.openCamera,
            fillMaxWidth = false,
        )
    }
}
```

- [ ] **Step 5: Run the focused test and verify it passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests pro.masterdoc.client.ui.screens.TicketsCreateFlowTest
```

Expected: `TicketsCreateFlowTest` passes.

- [ ] **Step 6: Commit the task**

Stage only the plan, design, test, and implementation files, then commit:

```bash
git commit -m "feat(work-orders): collapse photo source actions"
```
