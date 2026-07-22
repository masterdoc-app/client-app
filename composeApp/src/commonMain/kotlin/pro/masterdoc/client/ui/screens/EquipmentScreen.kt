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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.TechnologistJobDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.PickedPdf
import pro.masterdoc.client.platform.pickPdfFile

@Composable
fun EquipmentScreen(
    repository: EquipmentRepository,
    modifier: Modifier = Modifier,
) {
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf<PickedPdf?>(null) }
    var documentId by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<TechnologistJobDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            try {
                assets = repository.listAssets().items
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
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
                text = "PDF руководства → Технолог создаёт черновик. После подтверждения оборудование попадает в базу.",
                style = AppTextStyle.Label,
            )
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
                enabled = !busy && picked != null,
                onClick = {
                    val file = picked ?: return@AppButton
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            val doc = repository.uploadManualPdf(file.bytes, file.filename)
                            documentId = doc.id
                            job = repository.startTechnologist(doc.id)
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
                AppText(text = "Document ID: $documentId", style = AppTextStyle.Label)
            }
            job?.let { j ->
                AppText(text = "Job: ${j.status}")
                j.error?.let { AppText(text = it) }
                if (j.status == "succeeded") {
                    AppText(
                        text = "Созданы черновики: оборудование ${j.draftAssetId ?: "—"}, ППР ${j.draftMapId ?: "—"}",
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

            AppText(text = "Черновики оборудования", style = AppTextStyle.Title)
            AppText(
                text = "Подтвердите — попадёт в базу. Отклоните — черновик удалится.",
                style = AppTextStyle.Label,
            )
            when {
                loading && assets.isEmpty() -> CircularProgressIndicator()
                drafts.isEmpty() -> AppText(text = "Нет черновиков", style = AppTextStyle.Label)
                else ->
                    drafts.forEach { asset ->
                        AssetDraftRow(
                            asset = asset,
                            acting = actingId == asset.id,
                            onConfirm = {
                                scope.launch {
                                    actingId = asset.id
                                    try {
                                        repository.confirmAsset(asset.id)
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
                        AppText(
                            text = "${asset.name} · ${asset.source}${asset.inventoryNo?.let { " · №$it" } ?: ""}",
                            style = AppTextStyle.Body,
                        )
                    }
            }
        }
    }
}

@Composable
private fun AssetDraftRow(
    asset: AssetDto,
    acting: Boolean,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppText(
            text = "${asset.name} · ${asset.source}${asset.category?.let { " · $it" } ?: ""}",
            style = AppTextStyle.Body,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(text = if (acting) "…" else "Подтвердить", enabled = !acting, onClick = onConfirm)
            AppButton(text = "Отклонить", enabled = !acting, onClick = onReject)
        }
    }
}
