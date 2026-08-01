package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.DocumentMetaDto
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.IntervalDto
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.auth.MaintenanceMapItemInput
import pro.masterdoc.client.auth.UpdateMaintenanceMapRequest
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.openAuthenticatedDocument

@Composable
fun ChartsScreen(
    repository: EquipmentRepository,
    focusedMapId: String? = null,
    onOpenEquipment: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var maps by remember { mutableStateOf<List<MaintenanceMapDto>>(emptyList()) }
    var assetsById by remember { mutableStateOf<Map<String, AssetDto>>(emptyMap()) }
    var sourceDocsByAssetId by remember { mutableStateOf<Map<String, List<SourceDocRef>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actingId by remember { mutableStateOf<String?>(null) }
    var editingMapId by remember { mutableStateOf<String?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingItems by remember { mutableStateOf<List<EditableMapItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            try {
                maps = repository.listMaps().items
                val assets = repository.listAssets().items.associateBy { it.id }
                assetsById = assets
                val metaById = linkedMapOf<String, DocumentMetaDto>()
                val byAsset = linkedMapOf<String, List<SourceDocRef>>()
                maps.map { it.assetId }.distinct().forEach { assetId ->
                    val ids = assets[assetId]?.documentIds.orEmpty()
                    if (ids.isEmpty()) return@forEach
                    byAsset[assetId] =
                        ids.map { id ->
                            val meta =
                                metaById[id]
                                    ?: runCatching { repository.getDocument(id) }
                                        .onSuccess { metaById[id] = it }
                                        .getOrNull()
                            SourceDocRef(
                                id = id,
                                filename = documentDisplayName(meta?.filename, id),
                                contentType = meta?.contentType.orEmpty(),
                            )
                        }
                }
                sourceDocsByAssetId = byAsset.toMap()
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun openDocument(doc: MaintenanceMapSourceDoc) {
        scope.launch {
            try {
                openAuthenticatedDocument(
                    url = repository.documentContentUrl(doc.id),
                    bearerToken = repository.accessToken(),
                    filename = doc.filename,
                    mimeType = doc.contentType.ifBlank { "application/pdf" },
                )
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Не удалось открыть документ"
            }
        }
    }

    fun startEditing(map: MaintenanceMapDto) {
        editingMapId = map.id
        editingTitle = map.title
        editingItems =
            map.items.map {
                EditableMapItem(
                    title = it.title,
                    kind = it.kind,
                    intervalEvery = it.interval.every.toString(),
                    intervalUnit = it.interval.unit,
                    criticality = it.criticality,
                    sourceRef = it.sourceRef,
                )
            }
    }

    fun saveEditing(id: String) {
        val items =
            editingItems.map {
                MaintenanceMapItemInput(
                    title = it.title,
                    kind = it.kind,
                    interval = IntervalDto(every = it.intervalEvery.toInt(), unit = it.intervalUnit),
                    criticality = it.criticality,
                    sourceRef = it.sourceRef,
                )
            }
        scope.launch {
            actingId = id
            try {
                repository.updateMap(id, UpdateMaintenanceMapRequest(title = editingTitle, items = items))
                editingMapId = null
                reload()
            } catch (e: Exception) {
                error = e.message
            } finally {
                actingId = null
            }
        }
    }

    LaunchedEffect(repository) { reload() }

    val drafts = maps.filter { it.status == "draft" }
    val active = maps.filter { it.status == "active" }

    AppScaffold(title = "ППР", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = ClientSpacing.md)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            AppText(
                text = "Черновики карт обслуживания появляются после работы Технолога. После подтверждения ППР попадает в базу.",
                style = AppTextStyle.Label,
            )
            error?.let { AppText(text = it) }

            AppText(text = "Черновики ППР", style = AppTextStyle.Title)
            when {
                loading && maps.isEmpty() -> CircularProgressIndicator()
                drafts.isEmpty() -> AppText(text = "Нет черновиков", style = AppTextStyle.Label)
                else ->
                    drafts.forEach { map ->
                        MaintenanceMapCard(
                            map = map,
                            asset = assetsById[map.assetId],
                            sourceDocs = sourceDocsByAssetId[map.assetId].orEmpty().toMaintenanceMapSourceDocs(),
                            highlighted = map.id == focusedMapId,
                            acting = actingId == map.id,
                            onOpenDocument = ::openDocument,
                            onOpenEquipment = onOpenEquipment,
                            onEdit = { startEditing(map) },
                            onConfirm = {
                                scope.launch {
                                    actingId = map.id
                                    try {
                                        repository.confirmMap(map.id)
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        actingId = null
                                    }
                                }
                            },
                            onReject = {
                                scope.launch {
                                    actingId = map.id
                                    try {
                                        repository.rejectMap(map.id)
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        actingId = null
                                    }
                                }
                            },
                        )
                        if (editingMapId == map.id) {
                            MaintenanceMapEditor(
                                title = editingTitle,
                                items = editingItems,
                                saving = actingId == map.id,
                                onTitleChange = { editingTitle = it },
                                onItemsChange = { editingItems = it },
                                onSave = { saveEditing(map.id) },
                                onCancel = { editingMapId = null },
                            )
                        }
                    }
            }

            AppText(text = "ППР в базе", style = AppTextStyle.Title)
            when {
                loading && maps.isEmpty() -> Unit
                active.isEmpty() -> AppText(text = "Пока пусто", style = AppTextStyle.Label)
                else ->
                    active.forEach { map ->
                        MaintenanceMapCard(
                            map = map,
                            asset = assetsById[map.assetId],
                            sourceDocs = sourceDocsByAssetId[map.assetId].orEmpty().toMaintenanceMapSourceDocs(),
                            highlighted = map.id == focusedMapId,
                            onOpenDocument = ::openDocument,
                            onOpenEquipment = onOpenEquipment,
                            acting = actingId == map.id,
                            onEdit = { startEditing(map) },
                        )
                        if (editingMapId == map.id) {
                            MaintenanceMapEditor(
                                title = editingTitle,
                                items = editingItems,
                                saving = actingId == map.id,
                                onTitleChange = { editingTitle = it },
                                onItemsChange = { editingItems = it },
                                onSave = { saveEditing(map.id) },
                                onCancel = { editingMapId = null },
                            )
                        }
                    }
            }
        }
    }
}

private data class EditableMapItem(
    val title: String,
    val kind: String,
    val intervalEvery: String,
    val intervalUnit: String,
    val criticality: String,
    val sourceRef: String? = null,
)

@Composable
private fun MaintenanceMapEditor(
    title: String,
    items: List<EditableMapItem>,
    saving: Boolean,
    onTitleChange: (String) -> Unit,
    onItemsChange: (List<EditableMapItem>) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val canSave =
        !saving &&
            title.isNotBlank() &&
            items.isNotEmpty() &&
            items.all { it.title.isNotBlank() && (it.intervalEvery.toIntOrNull() ?: 0) >= 1 }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        AppText(text = "Редактирование ППР", style = AppTextStyle.Title)
        AppTextField(value = title, onValueChange = onTitleChange, label = "Название карты", enabled = !saving)
        items.forEachIndexed { index, item ->
            Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs)) {
                AppText(text = "Пункт ${index + 1}", style = AppTextStyle.Label)
                AppTextField(
                    value = item.title,
                    onValueChange = { value -> onItemsChange(items.replaceAt(index, item.copy(title = value))) },
                    label = "Название пункта",
                    enabled = !saving,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                    AppButton(
                        text = "Вид: ${ruKind(item.kind)}",
                        onClick = {
                            onItemsChange(items.replaceAt(index, item.copy(kind = item.kind.nextKind())))
                        },
                        enabled = !saving,
                        variant = AppButtonVariant.Secondary,
                        fillMaxWidth = false,
                    )
                    AppButton(
                        text = "Единица: ${ruIntervalUnit(1, item.intervalUnit)}",
                        onClick = {
                            onItemsChange(items.replaceAt(index, item.copy(intervalUnit = item.intervalUnit.nextUnit())))
                        },
                        enabled = !saving,
                        variant = AppButtonVariant.Secondary,
                        fillMaxWidth = false,
                    )
                    AppButton(
                        text = "Критичность: ${ruCriticality(item.criticality)}",
                        onClick = {
                            onItemsChange(items.replaceAt(index, item.copy(criticality = item.criticality.nextCriticality())))
                        },
                        enabled = !saving,
                        variant = AppButtonVariant.Secondary,
                        fillMaxWidth = false,
                    )
                }
                AppTextField(
                    value = item.intervalEvery,
                    onValueChange = { value -> onItemsChange(items.replaceAt(index, item.copy(intervalEvery = value))) },
                    label = "Интервал (каждые)",
                    enabled = !saving,
                )
                AppButton(
                    text = "Удалить",
                    onClick = { onItemsChange(items.filterIndexed { itemIndex, _ -> itemIndex != index }) },
                    enabled = !saving,
                    variant = AppButtonVariant.Secondary,
                    fillMaxWidth = false,
                )
            }
        }
        AppButton(
            text = "Добавить пункт",
            onClick = {
                onItemsChange(
                    items +
                        EditableMapItem(
                            title = "",
                            kind = "inspection",
                            intervalEvery = "1",
                            intervalUnit = "days",
                            criticality = "medium",
                        ),
                )
            },
            enabled = !saving,
            variant = AppButtonVariant.Secondary,
            fillMaxWidth = false,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
            AppButton(
                text = if (saving) "…" else "Сохранить",
                onClick = onSave,
                enabled = canSave,
                fillMaxWidth = false,
            )
            AppButton(
                text = "Отмена",
                onClick = onCancel,
                enabled = !saving,
                variant = AppButtonVariant.Secondary,
                fillMaxWidth = false,
            )
        }
    }
}

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    mapIndexed { itemIndex, item -> if (itemIndex == index) value else item }

private fun String.nextKind(): String =
    when (this) {
        "inspection" -> "service"
        "service" -> "overhaul"
        else -> "inspection"
    }

private fun String.nextUnit(): String =
    when (this) {
        "days" -> "hours"
        "hours" -> "cycles"
        else -> "days"
    }

private fun String.nextCriticality(): String =
    when (this) {
        "low" -> "medium"
        "medium" -> "high"
        else -> "low"
    }

private fun ruCriticality(criticality: String): String =
    when (criticality) {
        "low" -> "низкая"
        "medium" -> "средняя"
        "high" -> "высокая"
        else -> criticality
    }

private data class SourceDocRef(
    val id: String,
    val filename: String,
    val contentType: String,
)

private fun SourceDocRef.toMaintenanceMapSourceDoc(): MaintenanceMapSourceDoc =
    MaintenanceMapSourceDoc(id = id, filename = filename, contentType = contentType)

private fun List<SourceDocRef>.toMaintenanceMapSourceDocs(): List<MaintenanceMapSourceDoc> =
    map { it.toMaintenanceMapSourceDoc() }

internal const val MAP_ITEMS_PREVIEW_LIMIT = 5

internal fun <T> visibleMapItems(
    items: List<T>,
    expanded: Boolean,
    previewLimit: Int = MAP_ITEMS_PREVIEW_LIMIT,
): List<T> = if (expanded || items.size <= previewLimit) items else items.take(previewLimit)

internal fun mapItemsOverflowLabel(
    total: Int,
    previewLimit: Int = MAP_ITEMS_PREVIEW_LIMIT,
    expanded: Boolean,
): String? =
    when {
        total <= previewLimit -> null
        expanded -> "Свернуть"
        else -> "… ещё ${total - previewLimit}"
    }

internal fun mapHeadline(
    title: String,
    status: String,
    source: String,
): String =
    listOfNotNull(
        title.takeIf { it.isNotBlank() },
        ruStatus(status).takeIf { it.isNotBlank() },
        ruSource(source).takeIf { it.isNotBlank() },
    ).joinToString(" · ")

/** Equipment is created from its document — never show an «unbound» state. */
internal fun pprDocumentLines(filenames: List<String>): List<String> =
    filenames.map { "Документ: $it" }

internal fun ruStatus(status: String): String =
    when (status.lowercase()) {
        "draft" -> "черновик"
        // «активна» omitted — section «ППР в базе» already conveys status
        "active" -> ""
        "rejected" -> "отклонена"
        else -> status
    }

internal fun pprStatusChipLabel(status: String): String =
    when (status.lowercase()) {
        "draft" -> "Черновик"
        "active" -> "В базе"
        else -> ruStatus(status).ifBlank { status }
    }

internal fun ruSource(source: String): String =
    when (source.lowercase()) {
        "ai_generated" -> "ИИ"
        "manual" -> "вручную"
        else -> source
    }

internal fun pprSourceChipLabel(source: String): String = ruSource(source)

internal fun ruKind(kind: String): String =
    when (kind.lowercase()) {
        "inspection" -> "осмотр"
        "service" -> "обслуживание"
        "overhaul" -> "капитальный ремонт"
        "repair" -> "ремонт"
        "replacement" -> "замена"
        "calibration" -> "калибровка"
        else -> kind
    }

internal fun ruIntervalUnit(every: Int, unit: String): String =
    when (unit.lowercase()) {
        "days", "day" -> russianPlural(every, "день", "дня", "дней")
        "weeks", "week" -> russianPlural(every, "неделя", "недели", "недель")
        "months", "month" -> russianPlural(every, "месяц", "месяца", "месяцев")
        "hours", "hour" -> russianPlural(every, "час", "часа", "часов")
        "cycles", "cycle" -> russianPlural(every, "цикл", "цикла", "циклов")
        "years", "year" -> russianPlural(every, "год", "года", "лет")
        else -> unit
    }

internal fun russianPlural(n: Int, one: String, few: String, many: String): String {
    val abs = kotlin.math.abs(n)
    val mod100 = abs % 100
    val mod10 = abs % 10
    val form =
        when {
            mod100 in 11..14 -> many
            mod10 == 1 -> one
            mod10 in 2..4 -> few
            else -> many
        }
    return form
}
