package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var picked by remember { mutableStateOf<PickedPdf?>(null) }
    var documentId by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<TechnologistJobDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            try {
                assets = repository.listAssets().items
            } catch (e: Exception) {
                error = e.message
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
                text = "Выберите PDF файл ИЭ / руководства. Агент создаёт только draft.",
                style = AppTextStyle.Label,
            )
            AppButton(
                text = "Выбрать PDF",
                enabled = !busy,
                onClick = {
                    scope.launch {
                        error = null
                        val file = pickPdfFile()
                        if (file == null) {
                            return@launch
                        }
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
                    AppText(text = "Draft asset: ${j.draftAssetId}")
                    AppText(text = "Draft map: ${j.draftMapId}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            text = "Подтвердить пакет",
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
            }
            error?.let { AppText(text = it) }
            AppText(text = "Активы", style = AppTextStyle.Title)
            assets.forEach { asset ->
                AppText(text = "${asset.name} · ${asset.status} · ${asset.source}")
                if (asset.status == "draft") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            text = "Подтвердить",
                            onClick = {
                                scope.launch {
                                    try {
                                        repository.confirmAsset(asset.id)
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    }
                                }
                            },
                        )
                        AppButton(
                            text = "Отклонить",
                            onClick = {
                                scope.launch {
                                    try {
                                        repository.rejectAsset(asset.id)
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
