package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.auth.DocumentMetaDto
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
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
                        MaintenanceMapCard(
                            map = map,
                            asset = assetsById[map.assetId],
                            sourceDocs = sourceDocsByAssetId[map.assetId].orEmpty().toMaintenanceMapSourceDocs(),
                            highlighted = map.id == focusedMapId,
                            onOpenDocument = ::openDocument,
                            onOpenEquipment = onOpenEquipment,
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
