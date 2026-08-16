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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import pro.masterdoc.client.auth.AssetDto
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EquipmentWorkOrdersReportScreen(
    reportsRepository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalogItem = reportCatalogItems().first { it.id == ReportId.EquipmentWorkOrders }
    var days by remember { mutableStateOf(30) }
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var selectedAssetId by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var orders by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var assetsLoading by remember { mutableStateOf(true) }
    var assetsError by remember { mutableStateOf<String?>(null) }
    var ordersLoading by remember { mutableStateOf(false) }
    var ordersError by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    val today = localEpochDay()
    val fromDay = today - days + 1
    val selectedAsset = assets.firstOrNull { it.id == selectedAssetId }

    LaunchedEffect(equipmentRepository) {
        assetsLoading = true
        assetsError = null
        try {
            assets = equipmentRepository.listAssets().items.sortedBy { it.displayName().lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            assetsError = "Не удалось загрузить оборудование"
            assets = emptyList()
        } finally {
            assetsLoading = false
        }
    }

    LaunchedEffect(reportsRepository, selectedAssetId, days) {
        val assetId = selectedAssetId
        if (assetId.isNullOrBlank()) {
            orders = emptyList()
            ordersError = null
            ordersLoading = false
            return@LaunchedEffect
        }
        ordersLoading = true
        ordersError = null
        try {
            orders =
                reportsRepository.equipmentWorkOrders(
                    assetId = assetId,
                    from = IsoDates.formatEpochDay(fromDay),
                    to = IsoDates.formatEpochDay(today),
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            ordersError = "Не удалось загрузить отчёт"
            orders = emptyList()
        } catch (e: Exception) {
            ordersError = "Не удалось загрузить отчёт"
            orders = emptyList()
        } finally {
            ordersLoading = false
        }
    }

    selectedOrderId?.let { orderId ->
        WorkOrderDetailScreen(
            repository = reportsRepository,
            orderId = orderId,
            onBack = { selectedOrderId = null },
            equipmentRepository = equipmentRepository,
            attachmentsRepository = null,
            commentsRepository = null,
            readOnly = true,
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
            item {
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedAsset?.displayName() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Оборудование") },
                        placeholder = { Text("Выберите оборудование") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        assets.forEach { asset ->
                            DropdownMenuItem(
                                text = { Text(asset.displayName()) },
                                onClick = {
                                    selectedAssetId = asset.id
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            when {
                assetsLoading -> item { CircularProgressIndicator() }
                assetsError != null -> item { AppText(text = assetsError!!) }
                selectedAssetId == null ->
                    item { AppText(text = "Выберите оборудование", style = AppTextStyle.Label) }
                ordersLoading -> item { CircularProgressIndicator() }
                ordersError != null -> item { AppText(text = ordersError!!) }
                orders.isEmpty() ->
                    item {
                        AppText(
                            text = "Нет заявок по этому оборудованию за выбранный период",
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
                        }
                    }
            }
            item { ReportHelpFooter(text = catalogItem.description) }
        }
    }
}
