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
import pro.masterdoc.client.designsystem.components.AppButton
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
    onOpenAssetQr: (String) -> Unit = {},
    locationTrackingController: LocationTrackingController? = null,
    modifier: Modifier = Modifier,
) {
    var items by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var mentorOpen by remember { mutableStateOf(false) }
    var qrDialogOpen by remember { mutableStateOf(false) }
    var pendingQrToken by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(qrDialogOpen, pendingQrToken) {
        if (!qrDialogOpen) {
            pendingQrToken?.let { token ->
                pendingQrToken = null
                onOpenAssetQr(token)
            }
        }
    }

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

    if (qrDialogOpen) {
        AssetQrPasteDialog(
            onDismiss = { qrDialogOpen = false },
            onOpen = { token ->
                pendingQrToken = token
                qrDialogOpen = false
            },
        )
    }
    AppScaffold(title = "Мои заявки", modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            item {
                AppButton(
                    text = "Сканировать QR",
                    onClick = { qrDialogOpen = true },
                )
            }
            when {
                loading -> item { CircularProgressIndicator() }
                error != null -> item { AppText(text = error!!) }
                items.isEmpty() -> item { AppText(text = "Нет назначенных заявок") }
                else ->
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
