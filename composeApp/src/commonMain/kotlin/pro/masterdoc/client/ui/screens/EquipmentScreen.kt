package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.DocumentMetaDto
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.auth.SiteDto
import pro.masterdoc.client.auth.TechnologistJobDto
import pro.masterdoc.client.auth.UpdateAssetRequest
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.PickedPdf
import pro.masterdoc.client.platform.openAuthenticatedDocument
import pro.masterdoc.client.platform.pickPdfFile

@Composable
fun EquipmentScreen(
    repository: EquipmentRepository,
    onOpenLinkedPpr: (MaintenanceMapDto) -> Unit = {},
    onPprDraftReady: (mapId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var mapsByAsset by remember { mutableStateOf<Map<String, MaintenanceMapDto>>(emptyMap()) }
    var documentsById by remember { mutableStateOf<Map<String, DocumentMetaDto>>(emptyMap()) }
    var documentsByFolder by remember { mutableStateOf<Map<String, List<DocumentMetaDto>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf<PickedPdf?>(null) }
    var documentId by remember { mutableStateOf("") }
    var pendingDocumentId by remember { mutableStateOf<String?>(null) }
    var pendingDraftAssetId by remember { mutableStateOf<String?>(null) }
    var replaceExplanation by remember { mutableStateOf<String?>(null) }
    var obsoleteDocumentIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var job by remember { mutableStateOf<TechnologistJobDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actingId by remember { mutableStateOf<String?>(null) }
    var statusHint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val siteNameById = remember(sites) { sites.associate { it.id to it.name } }

    fun reload() {
        scope.launch {
            loading = true
            try {
                sites = runCatching { repository.listSites().items }.getOrDefault(emptyList())
                assets = repository.listAssets().items
                val maps = repository.listMaps().items
                mapsByAsset =
                    maps
                        .groupBy { it.assetId }
                        .mapValues { (_, list) ->
                            list.firstOrNull { it.status == "draft" }
                                ?: list.firstOrNull { it.status == "active" }
                                ?: list.first()
                        }
                val docIds = assets.flatMap { it.documentIds }.distinct()
                val resolved = linkedMapOf<String, DocumentMetaDto>()
                docIds.forEach { id ->
                    runCatching { repository.getDocument(id) }
                        .onSuccess { resolved[id] = it }
                }
                documentsById = resolved.toMap()
                val folders =
                    (resolved.values.map { it.storageFolder() } + assets.map { it.orgId })
                        .distinct()
                val byFolder = linkedMapOf<String, List<DocumentMetaDto>>()
                folders.forEach { folder ->
                    runCatching { repository.listDocuments(folder) }
                        .onSuccess { byFolder[folder] = it.items }
                }
                documentsByFolder = byFolder.toMap()
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    suspend fun pollJob(id: String): TechnologistJobDto {
        var current = repository.getJob(id)
        repeat(60) {
            if (current.status == "succeeded" || current.status == "failed") return current
            delay(1_000)
            current = repository.getJob(id)
        }
        return current
    }

    suspend fun runEquipmentCard(docId: String, siteId: String, assetId: String) {
        statusHint = "Уточняем черновик оборудования…"
        val card = repository.createEquipmentCard(docId, siteId, assetId)
        documentId = docId
        statusHint = "Черновик оборудования готов (${card.draftAssetId.take(8)}…). Нажмите «В базу» — затем сформируем черновик ППР."
        reload()
    }

    suspend fun runValidateThenCard(docId: String, siteId: String, assetId: String? = null) {
        statusHint = "Проверяем документ…"
        val validation = repository.validateDocument(docId, siteId, assetId)
        when (validation.status) {
            "ok" -> {
                val draftId = validation.draftAssetId
                if (draftId.isNullOrBlank()) {
                    error = "Валидатор не вернул draftAssetId"
                    statusHint = null
                } else {
                    runEquipmentCard(docId, siteId, draftId)
                }
            }
            "reject" -> {
                error = validation.explanation ?: "Документ отклонён валидатором"
                statusHint = null
            }
            "needs_replace" -> {
                pendingDocumentId = docId
                pendingDraftAssetId = validation.draftAssetId
                replaceExplanation = validation.explanation
                obsoleteDocumentIds = validation.obsoleteDocumentIds
                statusHint = "Найдены устаревшие документы — подтвердите замену"
            }
            else -> {
                error = "Неизвестный ответ валидатора: ${validation.status}"
                statusHint = null
            }
        }
    }

    fun openDocument(doc: DocumentMetaDto) {
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

    fun openFolder(folder: String) {
        scope.launch {
            try {
                val items =
                    documentsByFolder[folder]
                        ?: repository.listDocuments(folder).items.also {
                            documentsByFolder = documentsByFolder + (folder to it)
                        }
                if (items.isEmpty()) {
                    error = "В папке $folder/ нет документов"
                    return@launch
                }
                error = null
                openDocument(items.first())
            } catch (e: Exception) {
                error = e.message ?: "Не удалось открыть папку"
            }
        }
    }

    LaunchedEffect(repository) { reload() }

    LaunchedEffect(job?.id, job?.status) {
        val current = job ?: return@LaunchedEffect
        if (current.status == "queued" || current.status == "running") {
            delay(800)
            runCatching { repository.getJob(current.id) }
                .onSuccess { job = it }
                .onFailure { error = it.message }
        } else if (current.status == "succeeded") {
            reload()
        }
    }

    val drafts = assets.filter { it.status == "draft" }
    val active = assets.filter { it.status == "active" }

    AppScaffold(title = "Оборудование", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppText(text = "Загрузка руководства", style = AppTextStyle.Title)
            AppText(
                text =
                    "PDF прикрепляется к карточке оборудования (не к площадке). " +
                        "Площадка — только контейнер: размещение меняется на карточке. " +
                        "У оборудования один документ; чтобы загрузить PDF заново — удалите оборудование. " +
                        "После «В базу» формируется черновик ППР.",
                style = AppTextStyle.Label,
            )
            if (sites.isEmpty()) {
                AppText(
                    text = "Нет площадок — создайте контейнер в Админ > Площадки, иначе карточку некуда разместить.",
                    style = AppTextStyle.Label,
                )
            }
            AppButton(
                text = "Выбрать PDF",
                enabled = !busy,
                onClick = {
                    scope.launch {
                        error = null
                        val file = pickPdfFile() ?: return@launch
                        if (!file.filename.endsWith(".pdf", ignoreCase = true)) {
                            error = "Нужен файл PDF"
                            picked = null
                            return@launch
                        }
                        picked = file
                    }
                },
            )
            picked?.let { file ->
                AppText(
                    text = "Файл: ${file.filename} (${file.bytes.size} байт)",
                    style = AppTextStyle.Label,
                )
            }
            AppButton(
                text = if (busy) "Загрузка…" else "Загрузить PDF и создать черновик",
                enabled = !busy && picked != null && defaultEquipmentPlacementSiteId(sites) != null,
                onClick = {
                    val file = picked ?: return@AppButton
                    val siteId = defaultEquipmentPlacementSiteId(sites) ?: return@AppButton
                    scope.launch {
                        busy = true
                        error = null
                        replaceExplanation = null
                        obsoleteDocumentIds = emptyList()
                        pendingDocumentId = null
                        try {
                            val doc = repository.uploadManualPdf(file.bytes, file.filename)
                            documentId = doc.id
                            runValidateThenCard(doc.id, siteId)
                        } catch (e: GatewayHttpException) {
                            error = e.message
                            statusHint = null
                        } catch (e: Exception) {
                            error = e.message
                            statusHint = null
                        } finally {
                            busy = false
                        }
                    }
                },
            )
            if (replaceExplanation != null && pendingDocumentId != null) {
                AppText(text = replaceExplanation!!, style = AppTextStyle.Label)
                Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                    AppButton(
                        text = "Заменить старые документы",
                        enabled = !busy,
                        onClick = {
                            val docId = pendingDocumentId ?: return@AppButton
                            val siteId = defaultEquipmentPlacementSiteId(sites) ?: return@AppButton
                            scope.launch {
                                busy = true
                                error = null
                                try {
                                    val draftId = pendingDraftAssetId
                                    repository.confirmReplaceDocuments(docId, obsoleteDocumentIds)
                                    replaceExplanation = null
                                    obsoleteDocumentIds = emptyList()
                                    pendingDocumentId = null
                                    pendingDraftAssetId = null
                                    if (draftId.isNullOrBlank()) {
                                        error = "Нет draftAssetId после валидации — загрузите документ снова"
                                    } else {
                                        runEquipmentCard(docId, siteId, draftId)
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    )
                    AppButton(
                        text = "Отмена",
                        variant = AppButtonVariant.Secondary,
                        enabled = !busy,
                        onClick = {
                            pendingDocumentId = null
                            pendingDraftAssetId = null
                            replaceExplanation = null
                            obsoleteDocumentIds = emptyList()
                            statusHint = null
                        },
                    )
                }
            }
            if (documentId.isNotBlank()) {
                AppText(text = "ID документа: $documentId", style = AppTextStyle.Label)
            }
            statusHint?.let { AppText(text = it, style = AppTextStyle.Label) }
            job?.let { j ->
                AppText(text = "Задача ППР: ${jobStatusLabel(j.status)}")
                j.error?.let { AppText(text = it) }
                if (j.status == "succeeded") {
                    AppText(
                        text = "Черновик ППР готов — смотрите в разделе ППР.",
                        style = AppTextStyle.Label,
                    )
                }
            }

            error?.let { AppText(text = it) }

            AppText(text = "Черновики", style = AppTextStyle.Title)
            when {
                loading && assets.isEmpty() -> CircularProgressIndicator()
                drafts.isEmpty() -> AppText(text = "Нет черновиков — загрузите руководство.", style = AppTextStyle.Label)
                else ->
                    drafts.forEach { asset ->
                        EquipmentCard(
                            asset = asset,
                            siteName = siteNameById[asset.siteId],
                            moveTargets =
                                sites
                                    .filter { it.id != asset.siteId }
                                    .map { it.id to it.name },
                            linkedMap = mapsByAsset[asset.id],
                            documents = asset.documentIds.mapNotNull { documentsById[it] },
                            folderDocuments = documentsByFolder[asset.orgId].orEmpty(),
                            acting = actingId == asset.id,
                            onOpenLinkedPpr = onOpenLinkedPpr,
                            onOpenStorageFolder = ::openFolder,
                            onOpenDocument = ::openDocument,
                            onMove = { targetSiteId ->
                                scope.launch {
                                    actingId = asset.id
                                    try {
                                        repository.moveAsset(asset.id, targetSiteId)
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        actingId = null
                                    }
                                }
                            },
                            onConfirm = { editedName, editedInventoryNo ->
                                scope.launch {
                                    actingId = asset.id
                                    try {
                                        repository.updateAsset(
                                            asset.id,
                                            UpdateAssetRequest(
                                                name = editedName,
                                                inventoryNo = editedInventoryNo,
                                            ),
                                        )
                                        repository.confirmAsset(asset.id)
                                        statusHint = "Оборудование в базе — формируем черновик ППР…"
                                        reload()

                                        val existingDraftId =
                                            runCatching { repository.listMaps(asset.id).items }
                                                .getOrDefault(emptyList())
                                                .firstOrNull { it.status == "draft" }
                                                ?.id
                                                ?: mapsByAsset[asset.id]?.takeIf { it.status == "draft" }?.id

                                        var technologistMapId: String? = null
                                        if (needsTechnologistForPprDraft(existingDraftId)) {
                                            val docId =
                                                asset.documentIds.firstOrNull()
                                                    ?: documentId.takeIf { it.isNotBlank() }
                                                    ?: throw IllegalStateException("Нет documentId для технолога")
                                            val started =
                                                repository.startTechnologist(
                                                    documentId = docId,
                                                    siteId = asset.siteId,
                                                    assetId = asset.id,
                                                )
                                            val done = pollJob(started.id)
                                            job = done
                                            if (done.status == "failed") {
                                                throw IllegalStateException(
                                                    done.error ?: "Технолог завершился с ошибкой",
                                                )
                                            }
                                            technologistMapId = done.draftMapId
                                        }

                                        val draftMapId =
                                            resolvePprDraftMapId(
                                                existingDraftMapId = existingDraftId,
                                                technologistDraftMapId = technologistMapId,
                                            )
                                                ?: throw IllegalStateException(
                                                    "Технолог не вернул draftMapId — черновик ППР не создан",
                                                )
                                        statusHint = "Черновик ППР готов — открываем раздел ППР."
                                        onPprDraftReady(draftMapId)
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
                                    actingId = asset.id
                                    try {
                                        mapsByAsset[asset.id]?.let { map ->
                                            if (map.status == "draft") {
                                                runCatching { repository.rejectMap(map.id) }
                                            }
                                        }
                                        repository.rejectAsset(asset.id)
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

            AppText(text = "В базе", style = AppTextStyle.Title)
            when {
                loading && assets.isEmpty() -> Unit
                active.isEmpty() -> AppText(text = "Пока пусто", style = AppTextStyle.Label)
                else ->
                    active.forEach { asset ->
                        EquipmentCard(
                            asset = asset,
                            siteName = siteNameById[asset.siteId],
                            moveTargets =
                                sites
                                    .filter { it.id != asset.siteId }
                                    .map { it.id to it.name },
                            linkedMap = mapsByAsset[asset.id],
                            documents = asset.documentIds.mapNotNull { documentsById[it] },
                            folderDocuments = documentsByFolder[asset.orgId].orEmpty(),
                            acting = actingId == asset.id,
                            onOpenLinkedPpr = onOpenLinkedPpr,
                            onOpenStorageFolder = ::openFolder,
                            onOpenDocument = ::openDocument,
                            onDelete = {
                                scope.launch {
                                    actingId = asset.id
                                    try {
                                        repository.deleteAsset(asset.id)
                                        statusHint = "Оборудование удалено. Можно загрузить PDF заново."
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        actingId = null
                                    }
                                }
                            },
                            onMove = { targetSiteId ->
                                scope.launch {
                                    actingId = asset.id
                                    try {
                                        repository.moveAsset(asset.id, targetSiteId)
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
        }
    }
}

private fun jobStatusLabel(status: String): String =
    when (status) {
        "queued" -> "в очереди"
        "running" -> "выполняется"
        "succeeded" -> "завершена"
        "failed" -> "ошибка"
        else -> status
    }
