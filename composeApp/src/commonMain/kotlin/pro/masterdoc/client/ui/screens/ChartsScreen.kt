package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.auth.DocumentMetaDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.openAuthenticatedDocument

@Composable
fun ChartsScreen(
    repository: EquipmentRepository,
    focusedMapId: String? = null,
    modifier: Modifier = Modifier,
) {
    var maps by remember { mutableStateOf<List<MaintenanceMapDto>>(emptyList()) }
    var sourceDocsByAssetId by remember { mutableStateOf<Map<String, List<SourceDocRef>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            try {
                maps = repository.listMaps().items
                val assets = repository.listAssets().items.associateBy { it.id }
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
                                filename = meta?.filename ?: id,
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

    fun openDocument(doc: SourceDocRef) {
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

    LaunchedEffect(repository) { reload() }

    val drafts = maps.filter { it.status == "draft" }
    val active = maps.filter { it.status == "active" }

    AppScaffold(title = "ППР", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        MapDraftRow(
                            map = map,
                            sourceDocs = sourceDocsByAssetId[map.assetId].orEmpty(),
                            acting = actingId == map.id,
                            highlighted = map.id == focusedMapId,
                            onOpenDocument = ::openDocument,
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
                    }
            }

            AppText(text = "ППР в базе", style = AppTextStyle.Title)
            when {
                loading && maps.isEmpty() -> Unit
                active.isEmpty() -> AppText(text = "Пока пусто", style = AppTextStyle.Label)
                else ->
                    active.forEach { map ->
                        MapSummary(
                            map = map,
                            sourceDocs = sourceDocsByAssetId[map.assetId].orEmpty(),
                            highlighted = map.id == focusedMapId,
                            onOpenDocument = ::openDocument,
                        )
                    }
            }
        }
    }
}

private data class SourceDocRef(
    val id: String,
    val filename: String,
    val contentType: String,
)

@Composable
private fun MapDraftRow(
    map: MaintenanceMapDto,
    sourceDocs: List<SourceDocRef>,
    acting: Boolean,
    highlighted: Boolean = false,
    onOpenDocument: (SourceDocRef) -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MapSummary(
            map = map,
            sourceDocs = sourceDocs,
            highlighted = highlighted,
            onOpenDocument = onOpenDocument,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(text = if (acting) "…" else "Подтвердить", enabled = !acting, onClick = onConfirm)
            AppButton(text = "Отклонить", enabled = !acting, onClick = onReject)
        }
    }
}

@Composable
private fun MapSummary(
    map: MaintenanceMapDto,
    sourceDocs: List<SourceDocRef> = emptyList(),
    highlighted: Boolean = false,
    onOpenDocument: ((SourceDocRef) -> Unit)? = null,
) {
    var itemsExpanded by remember(map.id) { mutableStateOf(false) }
    val visibleItems = visibleMapItems(map.items, expanded = itemsExpanded, previewLimit = MAP_ITEMS_PREVIEW_LIMIT)
    val overflowLabel =
        mapItemsOverflowLabel(
            total = map.items.size,
            previewLimit = MAP_ITEMS_PREVIEW_LIMIT,
            expanded = itemsExpanded,
        )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppText(
            text =
                buildString {
                    if (highlighted) append("> ")
                    append("${map.title} · ${ruStatus(map.status)} · ${ruSource(map.source)}")
                },
            style = AppTextStyle.Body,
            color =
                if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    androidx.compose.ui.graphics.Color.Unspecified
                },
        )
        AppText(
            text = "Оборудование: ${map.assetId} · пунктов: ${map.items.size}",
            style = AppTextStyle.Label,
        )
        when {
            sourceDocs.isNotEmpty() ->
                sourceDocs.forEach { doc ->
                    val open = onOpenDocument
                    AppText(
                        text = "Документ: ${doc.filename}",
                        style = AppTextStyle.Label,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            if (open != null) {
                                Modifier.clickable { open(doc) }
                            } else {
                                Modifier
                            },
                    )
                }
            map.source.equals("ai_generated", ignoreCase = true) ->
                AppText(
                    text = "Документ: не привязан к оборудованию",
                    style = AppTextStyle.Label,
                )
        }
        visibleItems.forEach { item ->
            AppText(
                text =
                    "- ${item.title} (${ruKind(item.kind)}, каждые ${item.interval.every} ${ruIntervalUnit(item.interval.every, item.interval.unit)})",
                style = AppTextStyle.Label,
            )
        }
        if (overflowLabel != null) {
            AppText(
                text = overflowLabel,
                style = AppTextStyle.Label,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { itemsExpanded = !itemsExpanded },
            )
        }
    }
}

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

internal fun ruStatus(status: String): String =
    when (status.lowercase()) {
        "draft" -> "черновик"
        "active" -> "активна"
        "rejected" -> "отклонена"
        else -> status
    }

internal fun ruSource(source: String): String =
    when (source.lowercase()) {
        "ai_generated" -> "ИИ"
        "manual" -> "вручную"
        else -> source
    }

internal fun ruKind(kind: String): String =
    when (kind.lowercase()) {
        "inspection" -> "осмотр"
        "service" -> "обслуживание"
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
