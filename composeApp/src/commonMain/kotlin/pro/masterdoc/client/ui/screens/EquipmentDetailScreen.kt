package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
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
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.openAuthenticatedDocument

@Composable
fun EquipmentDetailScreen(
    assetId: String,
    repository: EquipmentRepository,
    canManageQr: Boolean,
    onBack: () -> Unit,
    onOpenLinkedPpr: (MaintenanceMapDto) -> Unit = {},
    onPprDraftReady: (mapId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var asset by remember(assetId) { mutableStateOf<AssetDto?>(null) }
    var sites by remember(assetId) { mutableStateOf<List<SiteDto>>(emptyList()) }
    var linkedMap by remember(assetId) { mutableStateOf<MaintenanceMapDto?>(null) }
    var documents by remember(assetId) { mutableStateOf<List<DocumentMetaDto>>(emptyList()) }
    var folderDocuments by remember(assetId) { mutableStateOf<List<DocumentMetaDto>>(emptyList()) }
    var loading by remember(assetId) { mutableStateOf(true) }
    var acting by remember(assetId) { mutableStateOf(false) }
    var error by remember(assetId) { mutableStateOf<String?>(null) }
    var qrUrl by remember(assetId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = true
        try {
            val loadedSites = runCatching { repository.listSites().items }.getOrDefault(emptyList())
            val loadedAsset =
                runCatching { repository.getAsset(assetId) }
                    .getOrElse { e ->
                        if (e is GatewayHttpException && e.status == 404) {
                            null
                        } else {
                            throw e
                        }
                    }
            val loadedMaps =
                if (loadedAsset == null) {
                    emptyList()
                } else {
                    runCatching { repository.listMaps(assetId).items }.getOrDefault(emptyList())
                }
            val loadedDocuments =
                loadedAsset
                    ?.documentIds
                    .orEmpty()
                    .mapNotNull { id -> runCatching { repository.getDocument(id) }.getOrNull() }
            val loadedFolderDocuments =
                loadedAsset
                    ?.let { current ->
                        val folder = loadedDocuments.firstOrNull()?.storageFolder() ?: current.orgId
                        runCatching { repository.listDocuments(folder).items }.getOrDefault(emptyList())
                    }.orEmpty()

            sites = loadedSites
            asset = loadedAsset
            qrUrl = equipmentQrUrl(loadedAsset?.qrToken)
            linkedMap =
                loadedMaps.firstOrNull { it.status == "draft" }
                    ?: loadedMaps.firstOrNull { it.status == "active" }
                    ?: loadedMaps.firstOrNull()
            documents = loadedDocuments
            folderDocuments = loadedFolderDocuments
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить оборудование"
        } finally {
            loading = false
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

    fun openDocument(document: DocumentMetaDto) {
        scope.launch {
            try {
                openAuthenticatedDocument(
                    url = repository.documentContentUrl(document.id),
                    bearerToken = repository.accessToken(),
                    filename = document.filename,
                    mimeType = document.contentType.ifBlank { "application/pdf" },
                )
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Не удалось открыть документ"
            }
        }
    }

    fun openQrPdf(current: AssetDto) {
        scope.launch {
            acting = true
            try {
                val assetName = current.name.ifBlank { "Оборудование" }
                openAuthenticatedDocument(
                    url = repository.qrPdfUrl(current.id),
                    bearerToken = repository.accessToken(),
                    filename = "$assetName QR.pdf",
                    mimeType = "application/pdf",
                )
                error = null
                reload()
            } catch (e: Exception) {
                val assetName = current.name.ifBlank { "Оборудование" }
                error = e.message ?: "Не удалось открыть PDF для «$assetName»"
            } finally {
                acting = false
            }
        }
    }

    fun openFolder(folder: String) {
        scope.launch {
            try {
                val items =
                    if (folderDocuments.isNotEmpty()) {
                        folderDocuments
                    } else {
                        repository.listDocuments(folder).items.also { folderDocuments = it }
                    }
                error =
                    if (items.isEmpty()) {
                        folderEmptyMessage(folder)
                    } else {
                        null
                    }
            } catch (e: Exception) {
                error = e.message ?: "Не удалось открыть папку"
            }
        }
    }

    LaunchedEffect(assetId, repository) { reload() }

    AppScaffold(
        title = asset?.name ?: "Оборудование",
        modifier = modifier,
        onNavigateBack = onBack,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            when {
                loading -> CircularProgressIndicator()
                asset == null -> {
                    AppText(text = "Не найдено", style = AppTextStyle.Title)
                    error?.let { AppText(text = it, style = AppTextStyle.Label) }
                }
                else -> {
                    val current = requireNotNull(asset)
                    error?.let { AppText(text = it, style = AppTextStyle.Label) }
                    EquipmentCard(
                        asset = current,
                        siteName = sites.firstOrNull { it.id == current.siteId }?.name,
                        moveTargets =
                            sites
                                .filter { it.id != current.siteId }
                                .map { it.id to it.name },
                        linkedMap = linkedMap,
                        documents = documents,
                        folderDocuments = folderDocuments,
                        acting = acting,
                        onOpenLinkedPpr = onOpenLinkedPpr,
                        onOpenStorageFolder = ::openFolder,
                        onOpenDocument = ::openDocument,
                        onMove = { targetSiteId ->
                            scope.launch {
                                acting = true
                                try {
                                    repository.moveAsset(current.id, targetSiteId)
                                    reload()
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    acting = false
                                }
                            }
                        },
                        onConfirm =
                            if (current.status == "draft") {
                                { name, inventoryNo ->
                                    scope.launch {
                                        acting = true
                                        try {
                                            repository.updateAsset(
                                                current.id,
                                                UpdateAssetRequest(
                                                    name = name,
                                                    inventoryNo = inventoryNo,
                                                ),
                                            )
                                            repository.confirmAsset(current.id)

                                            val existingDraftId =
                                                runCatching { repository.listMaps(current.id).items }
                                                    .getOrDefault(emptyList())
                                                    .firstOrNull { it.status == "draft" }
                                                    ?.id
                                                    ?: linkedMap?.takeIf { it.status == "draft" }?.id

                                            var technologistMapId: String? = null
                                            if (needsTechnologistForPprDraft(existingDraftId)) {
                                                val documentId =
                                                    current.documentIds.firstOrNull()
                                                        ?: throw IllegalStateException(
                                                            "Нет documentId для технолога",
                                                        )
                                                val started =
                                                    repository.startTechnologist(
                                                        documentId = documentId,
                                                        siteId = current.siteId,
                                                        assetId = current.id,
                                                    )
                                                val done = pollJob(started.id)
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
                                            onPprDraftReady(draftMapId)
                                            reload()
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            acting = false
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                        onReject =
                            if (current.status == "draft") {
                                {
                                    scope.launch {
                                        acting = true
                                        try {
                                            linkedMap?.takeIf { it.status == "draft" }?.let {
                                                runCatching { repository.rejectMap(it.id) }
                                            }
                                            repository.rejectAsset(current.id)
                                            reload()
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            acting = false
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                        onDelete =
                            if (current.status != "draft") {
                                {
                                    scope.launch {
                                        acting = true
                                        try {
                                            repository.deleteAsset(current.id)
                                            reload()
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            acting = false
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                    )
                    if (shouldShowEquipmentQr(canManageQr, current.status)) {
                        EquipmentQrBlock(
                            assetName = current.name.ifBlank { "Оборудование" },
                            qrUrl = qrUrl,
                            acting = acting,
                            onOpenPdf = { openQrPdf(current) },
                        )
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun EquipmentQrBlock(
    assetName: String,
    qrUrl: String?,
    acting: Boolean,
    onOpenPdf: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(qrUrl) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(ClientSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            AppText(text = "QR-код оборудования", style = AppTextStyle.Title)
            AppText(text = assetName, style = AppTextStyle.Body)
            if (qrUrl != null) {
                Image(
                    painter = rememberQrCodePainter(qrUrl),
                    contentDescription = "QR-код оборудования «$assetName»",
                    modifier =
                        Modifier
                            .size(240.dp)
                            .background(Color.White)
                            .padding(ClientSpacing.sm),
                )
                if (copied) {
                    AppText(text = "Ссылка скопирована", style = AppTextStyle.Label)
                }
            } else {
                AppText(
                    text = "Откройте PDF — QR-код будет создан автоматически.",
                    style = AppTextStyle.Body,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
            ) {
                AppButton(
                    text = if (acting) "…" else equipmentQrActionLabel(),
                    enabled = !acting,
                    fillMaxWidth = false,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPdf,
                )
                if (qrUrl != null) {
                    AppButton(
                        text = "Скопировать ссылку",
                        enabled = !acting,
                        variant = AppButtonVariant.Secondary,
                        fillMaxWidth = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            clipboard.setText(AnnotatedString(qrUrl))
                            copied = true
                        },
                    )
                }
            }
        }
    }
}
