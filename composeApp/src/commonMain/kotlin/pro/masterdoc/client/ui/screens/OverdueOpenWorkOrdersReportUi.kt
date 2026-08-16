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
import kotlinx.coroutines.CancellationException
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.CommentsRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
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

@Composable
internal fun OverdueOpenWorkOrdersReportScreen(
    reportsRepository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    adminUsersRepository: AdminUsersRepository? = null,
    attachmentsRepository: AttachmentsRepository? = null,
    commentsRepository: CommentsRepository? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalogItem = reportCatalogItems().first { it.id == ReportId.OverdueOpenWorkOrders }
    var orders by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(adminUsersRepository) {
        users =
            adminUsersRepository
                ?.let { runCatching { it.listUsers(limit = 200).items }.getOrDefault(emptyList()) }
                ?: emptyList()
    }

    LaunchedEffect(reportsRepository) {
        loading = true
        error = null
        try {
            orders = reportsRepository.overdueOpenWorkOrders()
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
            when {
                loading -> item { CircularProgressIndicator() }
                error != null -> item { AppText(text = error!!) }
                orders.isEmpty() ->
                    item {
                        AppText(
                            text = "Нет просроченных открытых заявок",
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
                                text = formatOverdueAssigneeLabel(order.assigneeId, users),
                                style = AppTextStyle.Label,
                            )
                        }
                    }
            }
            item { ReportHelpFooter(text = catalogItem.description) }
        }
    }
}

private fun formatOverdueAssigneeLabel(assigneeId: String?, users: List<AdminUser>): String {
    if (assigneeId.isNullOrBlank()) return "не назначен"
    return formatAssigneeLabel(assigneeId, users)
}
