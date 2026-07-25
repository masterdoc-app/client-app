package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.auth.workOrderStatusLabelRu
import pro.masterdoc.client.auth.workOrderTypeLabelRu
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkOrderDetailScreen(
    repository: WorkOrdersRepository,
    orderId: String,
    onBack: () -> Unit,
    onChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var order by remember { mutableStateOf<WorkOrderDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var acting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                order = repository.get(orderId)
            } catch (e: GatewayHttpException) {
                error = e.message ?: "Ошибка загрузки"
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(repository, orderId) {
        reload()
    }

    AppScaffold(
        title = "Заявка",
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
                loading && order == null -> CircularProgressIndicator()
                error != null && order == null -> AppText(text = error!!)
                order != null -> {
                    val wo = order!!
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                    ) {
                        AppStatusChip(
                            text = workOrderTypeLabelRu(wo.type),
                            tone =
                                if (wo.type == "emergency") {
                                    AppStatusChipTone.Accent
                                } else {
                                    AppStatusChipTone.Muted
                                },
                        )
                        AppStatusChip(
                            text = workOrderStatusLabelRu(wo.status),
                            tone =
                                when (wo.status) {
                                    "new" -> AppStatusChipTone.Accent
                                    "in_progress" -> AppStatusChipTone.Neutral
                                    else -> AppStatusChipTone.Muted
                                },
                        )
                    }
                    AppText(text = wo.title, style = AppTextStyle.Title)
                    DetailRow("Срок", wo.dueAt)
                    DetailRow("Площадка", wo.siteId)
                    DetailRow("Оборудование", wo.assetId)
                    DetailRow("Исполнитель", wo.assigneeId ?: "не назначен")
                    DetailRow("Источник", wo.source)
                    if (wo.type == "ppr") {
                        DetailRow("ППР", wo.maintenanceMapId.orEmpty())
                        DetailRow("Пункт ППР", wo.maintenanceMapItemId.orEmpty())
                    }
                    if (error != null) {
                        AppText(text = error!!)
                    }
                    when (wo.status) {
                        "new" ->
                            AppButton(
                                text = if (acting) "…" else "В работу",
                                onClick = {
                                    scope.launch {
                                        acting = true
                                        error = null
                                        try {
                                            order = repository.patch(wo.id, status = "in_progress")
                                            onChanged()
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            acting = false
                                        }
                                    }
                                },
                                enabled = !acting,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        "in_progress" ->
                            AppButton(
                                text = if (acting) "…" else "Закрыть",
                                onClick = {
                                    scope.launch {
                                        acting = true
                                        error = null
                                        try {
                                            order = repository.patch(wo.id, status = "closed")
                                            onChanged()
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            acting = false
                                        }
                                    }
                                },
                                enabled = !acting,
                                variant = AppButtonVariant.Secondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(text = label, style = AppTextStyle.Label)
        AppText(text = value, style = AppTextStyle.Body)
    }
}
