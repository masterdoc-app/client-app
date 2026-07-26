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
    modifier: Modifier = Modifier,
) {
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var selectedSiteId by remember { mutableStateOf<String?>(null) }
    var mapsByAsset by remember { mutableStateOf<Map<String, MaintenanceMapDto>>(emptyMap()) }
    var documentsById by remember { mutableStateOf<Map<String, DocumentMetaDto>>(emptyMap()) }
    var documentsByFolder by remember { mutableStateOf<Map<String, List<DocumentMetaDto>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf<PickedPdf?>(null) }
    var documentId by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<TechnologistJobDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val siteNameById = remember(sites) { sites.associate { it.id to it.name } }

    fun reload() {
        scope.launch {
            loading = true
            try {
                sites = runCatching { repository.listSites().items }.getOrDefault(emptyList())
                if (selectedSiteId == null || sites.none { it.id == selectedSiteId }) {
                    selectedSiteId = sites.firstOrNull()?.id
                }
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
                    "PDF руководства → Технолог собирает карточку оборудования и черновик ППР. " +
                        "После подтверждения карточка попадает в базу.",
                style = AppTextStyle.Label,
            )
            if (sites.isEmpty()) {
                AppText(
                    text = "Нет площадок — создайте в Админ → Площадки, иначе Технолог не привяжет объект.",
                    style = AppTextStyle.Label,
                )
            } else {
                AppText(text = "Площадка для новой карточки", style = AppTextStyle.Label)
                Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                    sites.forEach { site ->
                        AppButton(
                            text = site.name,
                            fillMaxWidth = false,
                            variant =
                                if (site.id == selectedSiteId) {
                                    AppButtonVariant.Primary
                                } else {
                                    AppButtonVariant.Secondary
                                },
                            onClick = { selectedSiteId = site.id },
                        )
                    }
                }
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
                text = if (busy) "Загрузка…" else "Загрузить и запустить Технолога",
                enabled = !busy && picked != null && selectedSiteId != null,
                onClick = {
                    val file = picked ?: return@AppButton
                    val siteId = selectedSiteId ?: return@AppButton
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            val doc = repository.uploadManualPdf(file.bytes, file.filename)
                            documentId = doc.id
                            job = repository.startTechnologist(doc.id, siteId = siteId)
                        } catch (e: GatewayHttpException) {
                            error = e.message
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
            )
            if (documentId.isNotBlank()) {
                AppText(text = "ID документа: $documentId", style = AppTextStyle.Label)
            }
            job?.let { j ->
                AppText(text = "Задача: ${jobStatusLabel(j.status)}")
                j.error?.let { AppText(text = it) }
                if (j.status == "succeeded") {
                    AppText(
                        text = "Черновики готовы. Проверьте карточку ниже и подтвердите или отклоните.",
                        style = AppTextStyle.Label,
                    )
                    AppButton(
                        text = "Подтвердить пакет (оборудование + ППР)",
                        onClick = {
                            scope.launch {
                                try {
                                    repository.confirmPackage(j.id)
                                    reload()
                                    error = null
                                } catch (e: Exception) {
                                    error = e.message
                                }
                            }
                        },
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
                            onConfirm = {
                                scope.launch {
                                    actingId = asset.id
                                    try {
                                        repository.confirmAsset(asset.id)
                                        mapsByAsset[asset.id]?.let { map ->
                                            if (map.status == "draft") {
                                                runCatching { repository.confirmMap(map.id) }
                                            }
                                        }
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
