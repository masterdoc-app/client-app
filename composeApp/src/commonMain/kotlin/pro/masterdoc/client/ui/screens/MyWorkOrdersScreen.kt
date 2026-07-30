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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.EquipmentRepository
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
import pro.masterdoc.client.tracking.LocationTrackingController

@Composable
fun MyWorkOrdersScreen(
    repository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository?,
    currentUserId: String?,
    onOpenEquipment: (String) -> Unit = {},
    locationTrackingController: LocationTrackingController? = null,
    modifier: Modifier = Modifier,
) {
    var items by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var mentorOpen by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(repository, currentUserId, reloadKey) {
        loading = true
        error = null
        if (currentUserId.isNullOrBlank()) {
            items = emptyList()
            error = "Не удалось определить пользователя"
            loading = false
            return@LaunchedEffect
        }
        try {
            items = repository.list(assigneeId = currentUserId)
            locationTrackingController?.onWorkOrdersChanged(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            error = e.message ?: "Ошибка загрузки заявок"
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки заявок"
        } finally {
            loading = false
        }
    }

    if (mentorOpen && selectedId != null && equipmentRepository != null) {
        WorkOrderMentorScreen(
            workOrderId = selectedId!!,
            repository = equipmentRepository,
            onBack = { mentorOpen = false },
            modifier = modifier,
        )
        return
    }

    selectedId?.let { orderId ->
        WorkOrderDetailScreen(
            repository = repository,
            orderId = orderId,
            onBack = {
                mentorOpen = false
                selectedId = null
            },
            onChanged = { reloadKey++ },
            locationTrackingController = locationTrackingController,
            currentUserId = currentUserId,
            equipmentRepository = equipmentRepository,
            onOpenMentor = { mentorOpen = true },
            onOpenEquipment = onOpenEquipment,
            readOnly = false,
            modifier = modifier,
        )
        return
    }

    AppScaffold(title = "Мои заявки", modifier = modifier) { padding ->
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.padding(padding).padding(ClientSpacing.md))
            error != null ->
                AppText(
                    text = error!!,
                    modifier = Modifier.padding(padding).padding(ClientSpacing.md),
                )
            items.isEmpty() ->
                AppText(
                    text = "Нет назначенных заявок",
                    modifier = Modifier.padding(padding).padding(ClientSpacing.md),
                )
            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(ClientSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                ) {
                    items(items, key = { it.id }) { order ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedId = order.id }
                                    .padding(vertical = ClientSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                        ) {
                            AppText(text = order.title, style = AppTextStyle.Title)
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
                        }
                    }
                }
        }
    }
}
