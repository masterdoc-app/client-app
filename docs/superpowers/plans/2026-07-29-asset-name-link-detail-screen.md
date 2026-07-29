# Asset name links + equipment detail screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace visible equipment UUIDs with names (tooltip = inventory №); clicking opens a dedicated equipment detail screen with Back.

**Architecture:** Extend deep-link/focus pattern used for PPR (`focusedMapId` / `#/ppr/{id}`) with `focusedAssetId` / `#/equipment/{assetId}`. Add reusable `AssetNameLink` and `EquipmentDetailScreen` (`AppScaffold` + `onNavigateBack`). Wire list vs detail in `MainShellContent` when Equipment destination is active.

**Tech Stack:** Compose Multiplatform, Decompose shell, Kotlin commonTest, Material3 (`TooltipBox` / `PlainTooltip` where available; else `Modifier.semantics { contentDescription }` + platform title fallback).

**Spec:** `docs/superpowers/specs/2026-07-29-asset-name-link-detail-screen-design.md`

## Global Constraints

- UI only in `client-app` — no catalog/gateway API changes
- Separate full screen with Back — not modal, not scroll-highlight in list
- Deep link `#/equipment/{assetId}` for detail; `#/equipment` for list
- Replace UUID display only for equipment (not siteId / other ids)
- Prefer `listAssets()` for resolve (no `GET /assets/{id}` yet)
- Follow existing Russian copy style; commit after each task in `client-app` repo
- Do not run heavy local Gradle Wasm/full builds — prefer targeted JVM commonTest / shared tests; CI builds on push

## File map

| File | Role |
|------|------|
| `shared/.../navigation/AppDeepLink.kt` | `EquipmentDetail(assetId)` + parse/toHash/toDestination |
| `shared/.../navigation/AppDeepLinkTest.kt` | deep-link tests |
| `shared/.../presentation/shell/MainShellComponent.kt` | `focusedAssetId`, `navigateTo(..., assetId=)` |
| `shared/.../presentation/shell/MainShellAnalyticsTest.kt` | update navigate signatures if needed |
| `composeApp/.../ui/screens/AssetNameLink.kt` | name + tooltip + onOpen |
| `composeApp/.../ui/screens/AssetDisplay.kt` | pure helpers: `assetDisplayName`, `assetInventoryTooltip` (unit-testable) |
| `composeApp/.../ui/screens/EquipmentDetailScreen.kt` | full-screen detail + Back |
| `composeApp/.../ui/shell/MainShellContent.kt` | focus wire + open callbacks |
| `composeApp/.../ui/screens/ChartsScreen.kt` | AssetNameLink instead of UUID |
| `composeApp/.../ui/screens/WorkOrderDetailScreen.kt` | AssetNameLink instead of UUID |
| `composeApp/.../ui/screens/EquipmentScreen.kt` | optional: pass through if list stays unchanged |
| `composeApp/src/commonTest/.../AssetDisplayTest.kt` | unit tests for display helpers |

---

### Task 1: Deep link `EquipmentDetail` + shell `focusedAssetId`

**Files:**
- Modify: `shared/src/commonMain/kotlin/pro/masterdoc/client/navigation/AppDeepLink.kt`
- Modify: `shared/src/commonTest/kotlin/pro/masterdoc/client/navigation/AppDeepLinkTest.kt`
- Modify: `shared/src/commonMain/kotlin/pro/masterdoc/client/presentation/shell/MainShellComponent.kt`
- Modify: `shared/src/commonTest/kotlin/pro/masterdoc/client/presentation/shell/MainShellAnalyticsTest.kt` (if compile breaks)

**Interfaces:**
- Produces: `AppDeepLink.EquipmentDetail(assetId: String)`; `parseAppDeepLink("#/equipment/abc")` → detail; `MainShellComponent.focusedAssetId: Value<String>`; `navigateTo(destination, mapId: String? = null, assetId: String? = null)` — when `assetId != null` set focused asset and clear map focus (and vice versa when `mapId != null`); clearing both when both null

- [ ] **Step 1: Write failing deep-link tests**

Extend `AppDeepLinkTest.kt`:

```kotlin
@Test
fun parsesEquipmentDetailDeepLink() {
    val link = parseAppDeepLink("#/equipment/asset-42")
    assertIs<AppDeepLink.EquipmentDetail>(link)
    assertEquals("asset-42", link.assetId)
    assertEquals(NavDestinationId.Equipment, link.toDestination())
    assertEquals("#/equipment/asset-42", link.toHash())
}

@Test
fun equipmentListStillParsesWithoutId() {
    assertEquals(AppDeepLink.Equipment, parseAppDeepLink("#/equipment"))
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run (from `client-app`):

```bash
./gradlew :shared:jvmTest --tests "pro.masterdoc.client.navigation.AppDeepLinkTest" -q
```

Expected: FAIL (EquipmentDetail missing / wrong parse)

- [ ] **Step 3: Implement deep link + shell focus**

`AppDeepLink.kt`:

```kotlin
sealed class AppDeepLink {
    data class Ppr(val mapId: String) : AppDeepLink()
    data object Equipment : AppDeepLink()
    data class EquipmentDetail(val assetId: String) : AppDeepLink()
    data object Charts : AppDeepLink()
}

// parse: "equipment" + second segment → EquipmentDetail, else Equipment
// toHash: EquipmentDetail → "#/equipment/$assetId"
// toDestination: EquipmentDetail → NavDestinationId.Equipment
```

`MainShellComponent`:

```kotlin
val focusedAssetId: Value<String>

fun navigateTo(destination: NavDestinationId, mapId: String? = null, assetId: String? = null)

// applyDeepLinkHash:
// is EquipmentDetail -> navigateTo(Equipment, assetId = link.assetId)
// Equipment -> navigateTo(Equipment, assetId = null)
```

When selecting nav item Equipment manually, clear `focusedAssetId` (empty string).

- [ ] **Step 4: Re-run tests — expect PASS**

```bash
./gradlew :shared:jvmTest --tests "pro.masterdoc.client.navigation.AppDeepLinkTest" --tests "pro.masterdoc.client.presentation.shell.MainShellAnalyticsTest" -q
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/pro/masterdoc/client/navigation/AppDeepLink.kt \
  shared/src/commonTest/kotlin/pro/masterdoc/client/navigation/AppDeepLinkTest.kt \
  shared/src/commonMain/kotlin/pro/masterdoc/client/presentation/shell/MainShellComponent.kt \
  shared/src/commonTest/kotlin/pro/masterdoc/client/presentation/shell/MainShellAnalyticsTest.kt
git commit -m "$(cat <<'EOF'
feat(nav): deep link #/equipment/{assetId} with focusedAssetId

EOF
)"
```

---

### Task 2: `AssetDisplay` helpers + `AssetNameLink`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/AssetDisplay.kt`
- Create: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/AssetNameLink.kt`
- Create: `composeApp/src/commonTest/kotlin/pro/masterdoc/client/ui/screens/AssetDisplayTest.kt`

**Interfaces:**
- Consumes: `AssetDto` from auth (or nullable name/inventoryNo/id fields)
- Produces:
  - `fun assetDisplayName(name: String?, assetId: String): String`
  - `fun assetInventoryTooltip(inventoryNo: String?): String`
  - `@Composable fun AssetNameLink(name: String?, inventoryNo: String?, assetId: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Failing unit tests**

```kotlin
class AssetDisplayTest {
    @Test
    fun prefersNonBlankName() {
        assertEquals("Насос", assetDisplayName("Насос", "uuid-long"))
    }

    @Test
    fun fallsBackToShortId() {
        assertEquals("abcdef12…", assetDisplayName("  ", "abcdef12-3456-7890"))
    }

    @Test
    fun inventoryTooltip() {
        assertEquals("Инв. № INV-1", assetInventoryTooltip("INV-1"))
        assertEquals("Инв. № не указан", assetInventoryTooltip(null))
        assertEquals("Инв. № не указан", assetInventoryTooltip("  "))
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :composeApp:jvmTest --tests "pro.masterdoc.client.ui.screens.AssetDisplayTest" -q
```

(If `composeApp` has no jvmTest target, put helpers+tests in `shared` instead — prefer composeApp commonTest if already used.)

- [ ] **Step 3: Implement helpers + AssetNameLink**

```kotlin
fun assetDisplayName(name: String?, assetId: String): String {
    val n = name?.trim().orEmpty()
    if (n.isNotEmpty()) return n
    val short = assetId.take(8)
    return if (assetId.length > 8) "$short…" else short
}

fun assetInventoryTooltip(inventoryNo: String?): String {
    val inv = inventoryNo?.trim().orEmpty()
    return if (inv.isNotEmpty()) "Инв. № $inv" else "Инв. № не указан"
}
```

`AssetNameLink`: clickable primary-colored `AppText` with Material3 `TooltipBox` + `PlainTooltip` showing inventory tooltip; `onClick = { onOpen(assetId) }`.

- [ ] **Step 4: Tests PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(ui): AssetNameLink with inventory tooltip helpers

EOF
)"
```

---

### Task 3: `EquipmentDetailScreen` + shell wiring (list vs detail)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/EquipmentDetailScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/shell/MainShellContent.kt`
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/EquipmentScreen.kt` only if needed for callbacks

**Interfaces:**
- Consumes: `focusedAssetId`, `EquipmentRepository`, existing `EquipmentCard` actions patterns
- Produces: `@Composable fun EquipmentDetailScreen(assetId: String, repository: EquipmentRepository, onBack: () -> Unit, onOpenLinkedPpr: (MaintenanceMapDto) -> Unit = {}, ...)`

- [ ] **Step 1: Implement EquipmentDetailScreen**

Pattern like `WorkOrderMentorScreen`:

```kotlin
@Composable
fun EquipmentDetailScreen(
    assetId: String,
    repository: EquipmentRepository,
    onBack: () -> Unit,
    onOpenLinkedPpr: (MaintenanceMapDto) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // LaunchedEffect: listAssets + listSites + listMaps(assetId) + docs
    // AppScaffold(title = asset?.name ?: "Оборудование", onNavigateBack = onBack)
    // if null after load → AppText("Не найдено") + back already in scaffold
    // else EquipmentCard(...) without needing list chrome
}
```

Reuse `EquipmentCard` for body (confirm/reject/delete/move as appropriate for status).

- [ ] **Step 2: Wire MainShellContent**

- Subscribe `focusedAssetId`
- Pass `onOpenEquipment = { id -> BrowserNav.setHash(AppDeepLink.EquipmentDetail(id).toHash()); component.navigateTo(Equipment, assetId = id) }` into Board / Charts / MyWorkOrders as needed
- For Equipment destination:

```kotlin
NavDestinationId.Equipment ->
    if (equipmentRepository != null) {
        val focus = focusedAssetId
        if (focus.isNotBlank()) {
            EquipmentDetailScreen(
                assetId = focus,
                repository = equipmentRepository,
                onBack = {
                    BrowserNav.setHash(AppDeepLink.Equipment.toHash())
                    component.navigateTo(NavDestinationId.Equipment, assetId = null)
                },
                onOpenLinkedPpr = onOpenLinkedPpr,
            )
        } else {
            EquipmentScreen(...)
        }
    }
```

- [ ] **Step 3: Smoke compile check (targeted)**

```bash
./gradlew :shared:compileKotlinJvm :composeApp:compileKotlinJvm -q
```

Expected: BUILD SUCCESSFUL (skip wasm)

- [ ] **Step 4: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(ui): EquipmentDetailScreen with back and deep-link focus

EOF
)"
```

---

### Task 4: Replace UUID in Charts + WorkOrder detail with AssetNameLink

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/ChartsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/WorkOrderDetailScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/shell/MainShellContent.kt` (pass `onOpenEquipment`)
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/BoardScreen.kt` / `MyWorkOrdersScreen.kt` if they host detail

**Interfaces:**
- Consumes: `AssetNameLink`, `onOpenEquipment: (String) -> Unit`, assets map from `listAssets()`

- [ ] **Step 1: ChartsScreen**

`ChartsScreen` already loads `assets` map in LaunchedEffect — pass name/inventory into `MapSummary`:

```kotlin
val asset = assetsById[map.assetId]
// replace AppText UUID line with:
AssetNameLink(
    name = asset?.name,
    inventoryNo = asset?.inventoryNo,
    assetId = map.assetId,
    onOpen = onOpenEquipment,
)
AppText(text = " · пунктов: ${map.items.size}", style = AppTextStyle.Label) // or single row Row
```

Add `onOpenEquipment: (String) -> Unit = {}` param to `ChartsScreen` / `MapSummary`.

- [ ] **Step 2: WorkOrderDetailScreen**

Load assets once (or receive `assetName`/`inventoryNo` from parent). Prefer parent passes resolved fields OR screen loads:

```kotlin
LaunchedEffect(equipmentRepository, wo.assetId) {
    val a = equipmentRepository?.listAssets()?.items?.find { it.id == wo.assetId }
    // state
}
// Detail row: label "Оборудование" + AssetNameLink(...)
```

If `DetailRow` is text-only, change that row to a custom Row with label + `AssetNameLink`.

Pass `onOpenEquipment` from shell through Board/MyWorkOrders into detail.

- [ ] **Step 3: Compile JVM**

```bash
./gradlew :composeApp:compileKotlinJvm :shared:jvmTest -q
```

- [ ] **Step 4: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(ui): show equipment names with link to detail in WO and PPR

EOF
)"
```

---

### Task 5: Spec doc commit + branch review readiness

**Files:**
- Add: `docs/superpowers/specs/2026-07-29-asset-name-link-detail-screen-design.md` (if untracked)
- Add: `docs/superpowers/plans/2026-07-29-asset-name-link-detail-screen.md`

- [ ] **Step 1: Commit docs**

```bash
git add docs/superpowers/specs/2026-07-29-asset-name-link-detail-screen-design.md \
  docs/superpowers/plans/2026-07-29-asset-name-link-detail-screen.md
git commit -m "$(cat <<'EOF'
docs: asset name link and equipment detail screen spec/plan

EOF
)"
```

- [ ] **Step 2: Self-check against success criteria in spec**

Manual checklist (no heavy Wasm):
- [ ] `#/equipment/{id}` focuses detail
- [ ] Back → `#/equipment` list
- [ ] WO + PPR show name not UUID
- [ ] Tooltip text from `assetInventoryTooltip`

---

## Spec coverage (self-review)

| Spec requirement | Task |
|------------------|------|
| Name instead of UUID | 2, 4 |
| Hover = inventory № | 2 |
| Click → separate screen + Back | 3 |
| Deep link `#/equipment/{id}` | 1, 3 |
| WO + Charts call sites | 4 |
| No API change | Global |
