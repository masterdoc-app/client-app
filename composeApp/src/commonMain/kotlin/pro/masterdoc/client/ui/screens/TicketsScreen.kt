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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.auth.CreateWorkOrderRequest
import pro.masterdoc.client.auth.workOrderStatusLabelRu
import pro.masterdoc.client.auth.workOrderTypeLabelRu
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.localEpochDay

fun partitionCustomerTickets(orders: List<WorkOrderDto>): Pair<List<WorkOrderDto>, List<WorkOrderDto>> {
    val active = orders.filter { it.status == "new" || it.status == "in_progress" }
    val done = orders.filter { it.status == "closed" }
    return active to done
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    repository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    currentUserId: String?,
    modifier: Modifier = Modifier,
) {
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var orders by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var selectedAsset by remember { mutableStateOf<AssetDto?>(null) }
    var description by remember { mutableStateOf("") }
    var assetMenuExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var acting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, equipmentRepository, currentUserId, reloadKey) {
        loading = true
        error = null
        if (currentUserId.isNullOrBlank()) {
            error = "Не удалось определить пользователя"
            loading = false
            return@LaunchedEffect
        }
        try {
            assets = equipmentRepository.listAssets().items
            orders = repository.list(createdBy = currentUserId)
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

    selectedOrderId?.let { orderId ->
        WorkOrderDetailScreen(
            repository = repository,
            orderId = orderId,
            onBack = { selectedOrderId = null },
            currentUserId = currentUserId,
            readOnly = true,
            modifier = modifier,
        )
        return
    }

    val (active, done) = partitionCustomerTickets(orders)
    AppScaffold(title = "Заявки", modifier = modifier) { padding ->
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).padding(ClientSpacing.md))
            return@AppScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                    AppText(text = "Новая аварийная заявка", style = AppTextStyle.Title)
                    ExposedDropdownMenuBox(
                        expanded = assetMenuExpanded,
                        onExpandedChange = { assetMenuExpanded = !assetMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedAsset?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { androidx.compose.material3.Text("Оборудование") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetMenuExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = assetMenuExpanded,
                            onDismissRequest = { assetMenuExpanded = false },
                        ) {
                            assets.forEach { asset ->
                                DropdownMenuItem(
                                    text = { androidx.compose.material3.Text(asset.name) },
                                    onClick = {
                                        selectedAsset = asset
                                        assetMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    AppTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Описание проблемы",
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppButton(
                        text = if (acting) "Создание…" else "Создать",
                        enabled = !acting && selectedAsset != null && description.isNotBlank(),
                        onClick = {
                            val asset = selectedAsset ?: return@AppButton
                            scope.launch {
                                acting = true
                                error = null
                                try {
                                    val title =
                                        description.lineSequence().first().trim().take(120).ifBlank { "Заявка" }
                                    repository.create(
                                        CreateWorkOrderRequest(
                                            type = "emergency",
                                            title = title,
                                            assetId = asset.id,
                                            siteId = asset.siteId,
                                            dueAt = IsoDates.formatEpochDay(localEpochDay()),
                                            description = description,
                                        ),
                                    )
                                    description = ""
                                    selectedAsset = null
                                    reloadKey++
                                } catch (e: Exception) {
                                    error = e.message ?: "Ошибка создания заявки"
                                } finally {
                                    acting = false
                                }
                            }
                        },
                    )
                    error?.let { AppText(text = it) }
                }
            }
            ticketSection("Активные", active) { selectedOrderId = it }
            ticketSection("Завершённые", done) { selectedOrderId = it }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ticketSection(
    title: String,
    orders: List<WorkOrderDto>,
    onClick: (String) -> Unit,
) {
    item { AppText(text = title, style = AppTextStyle.Title) }
    if (orders.isEmpty()) {
        item { AppText(text = "Нет заявок") }
    } else {
        items(orders, key = { it.id }) { order ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClick(order.id) }
                        .padding(vertical = ClientSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
            ) {
                AppText(text = order.title, style = AppTextStyle.Title)
                AppText(text = "${workOrderTypeLabelRu(order.type)} · ${order.dueAt}")
                AppStatusChip(
                    text = workOrderStatusLabelRu(order.status),
                    tone = if (order.status == "new") AppStatusChipTone.Accent else AppStatusChipTone.Muted,
                )
            }
        }
    }
}
