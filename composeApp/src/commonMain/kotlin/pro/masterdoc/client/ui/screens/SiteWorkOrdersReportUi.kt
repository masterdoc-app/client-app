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
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.CommentsRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.SiteDto
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
internal fun SiteWorkOrdersReportScreen(
    reportsRepository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    attachmentsRepository: AttachmentsRepository? = null,
    commentsRepository: CommentsRepository? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalogItem = reportCatalogItems().first { it.id == ReportId.SiteWorkOrders }
    var days by remember { mutableStateOf(30) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var selectedSiteId by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var orders by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var sitesLoading by remember { mutableStateOf(true) }
    var sitesError by remember { mutableStateOf<String?>(null) }
    var ordersLoading by remember { mutableStateOf(false) }
    var ordersError by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    val today = localEpochDay()
    val fromDay = today - days + 1
    val selectedSite = sites.firstOrNull { it.id == selectedSiteId }

    LaunchedEffect(equipmentRepository) {
        sitesLoading = true
        sitesError = null
        try {
            sites = equipmentRepository.listSites().items.sortedBy { it.name.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sitesError = "Не удалось загрузить площадки"
            sites = emptyList()
        } finally {
            sitesLoading = false
        }
    }

    LaunchedEffect(reportsRepository, selectedSiteId, days) {
        val siteId = selectedSiteId
        if (siteId.isNullOrBlank()) {
            orders = emptyList()
            ordersError = null
            ordersLoading = false
            return@LaunchedEffect
        }
        ordersLoading = true
        ordersError = null
        try {
            orders =
                reportsRepository.siteWorkOrders(
                    siteId = siteId,
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
            item {
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedSite?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Площадка") },
                        placeholder = { Text("Выберите площадку") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name) },
                                onClick = {
                                    selectedSiteId = site.id
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            when {
                sitesLoading -> item { CircularProgressIndicator() }
                sitesError != null -> item { AppText(text = sitesError!!) }
                selectedSiteId == null ->
                    item { AppText(text = "Выберите площадку", style = AppTextStyle.Label) }
                ordersLoading -> item { CircularProgressIndicator() }
                ordersError != null -> item { AppText(text = ordersError!!) }
                orders.isEmpty() ->
                    item {
                        AppText(
                            text = "Нет заявок по этой площадке за выбранный период",
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
