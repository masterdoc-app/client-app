@file:OptIn(kotlin.time.ExperimentalTime::class)

package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.CommentsRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.auth.workOrderStatusLabelRu
import pro.masterdoc.client.auth.workOrderTypeLabelRu
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.localEpochDay

@Composable
internal fun TimeToFirstActionReportScreen(
    reportsRepository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    attachmentsRepository: AttachmentsRepository? = null,
    commentsRepository: CommentsRepository? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalogItem = reportCatalogItems().first { it.id == ReportId.TimeToFirstAction }
    var days by remember { mutableStateOf(30) }
    var orders by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    val today = localEpochDay()
    val fromDay = today - days + 1

    LaunchedEffect(reportsRepository, days) {
        loading = true
        error = null
        try {
            orders =
                reportsRepository.timeToFirstAction(
                    from = IsoDates.formatEpochDay(fromDay),
                    to = IsoDates.formatEpochDay(today),
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            error = "Не удалось загрузить отчёт"
            orders = emptyList()
        } catch (e: Exception) {
            error = "Не удалось загрузить отчёт"
            orders = emptyList()
        } finally {
            loading = false
        }
    }

    selectedOrderId?.let { orderId ->
        WorkOrderDetailScreen(
            repository = reportsRepository,
            orderId = orderId,
            onBack = { selectedOrderId = null },
            equipmentRepository = equipmentRepository,
            attachmentsRepository = attachmentsRepository,
            commentsRepository = commentsRepository,
            readOnly = true,
            allowMediaMutations = false,
            modifier = modifier,
        )
        return
    }

    AppScaffold(title = catalogItem.title, onNavigateBack = onBack, modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            item {
                PeriodSelector(selected = days, onSelected = { days = it })
            }
            when {
                loading -> item { CircularProgressIndicator() }
                error != null -> item { AppText(text = error!!) }
                orders.isEmpty() ->
                    item {
                        AppText(
                            text = "Нет заявок за выбранный период",
                            style = AppTextStyle.Label,
                        )
                    }
                else ->
                    items(orders, key = { it.id }) { order ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOrderId = order.id }
                                    .padding(vertical = ClientSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                        ) {
                            AppText(
                                text = formatWorkOrderDisplayTitle(order.title),
                                style = AppTextStyle.Title,
                            )
                            AppText(text = "${workOrderTypeLabelRu(order.type)} · ${order.dueAt}")
                            AppStatusChip(
                                text = workOrderStatusLabelRu(order.status),
                                tone =
                                    if (order.status == "new") {
                                        AppStatusChipTone.Accent
                                    } else {
                                        AppStatusChipTone.Muted
                                    },
                            )
                            AppText(
                                text = formatTimeToFirstActionLabel(order.createdAt, order.startedAt),
                                style = AppTextStyle.Label,
                            )
                        }
                    }
            }
            item { ReportHelpFooter(text = catalogItem.description) }
        }
    }
}

internal fun formatTimeToFirstActionLabel(createdAt: String, startedAt: String?): String {
    if (startedAt.isNullOrBlank()) return "ещё не в работе"
    val createdMillis = parseIsoInstantMillis(createdAt) ?: return "ещё не в работе"
    val startedMillis = parseIsoInstantMillis(startedAt) ?: return "ещё не в работе"
    val diffMillis = startedMillis - createdMillis
    if (diffMillis <= 0L) return "0 ч до работы"
    val hours = (diffMillis / 3_600_000.0).roundToInt().coerceAtLeast(1)
    return "$hours ч до работы"
}

private fun parseIsoInstantMillis(value: String): Long? =
    runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull()
