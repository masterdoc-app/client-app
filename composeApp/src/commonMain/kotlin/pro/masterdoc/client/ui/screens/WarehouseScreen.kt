package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.CreateWarehousePartRequest
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.SiteDto
import pro.masterdoc.client.auth.StockReceiptRequest
import pro.masterdoc.client.auth.WarehousePartDto
import pro.masterdoc.client.auth.WarehouseRepository
import pro.masterdoc.client.auth.formatWarehouseQty
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
fun WarehouseScreen(
    repository: WarehouseRepository,
    equipmentRepository: EquipmentRepository? = null,
    canWrite: Boolean,
    modifier: Modifier = Modifier,
) {
    var parts by remember { mutableStateOf<List<WarehousePartDto>>(emptyList()) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var linkedAssetNames by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var advice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    var dialog by remember { mutableStateOf<WarehouseDialog?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                parts = repository.listParts()
                sites = equipmentRepository?.listSites()?.items.orEmpty()
                val assets =
                    try {
                        equipmentRepository?.listAssets()?.items.orEmpty()
                    } catch (_: Exception) {
                        emptyList<AssetDto>()
                    }
                linkedAssetNames =
                    assets.fold(mutableMapOf<String, MutableList<String>>()) { names, asset ->
                        try {
                            repository.assetParts(asset.id).forEach { link ->
                                val assetName = warehouseAssetLabel(asset.name)
                                names.getOrPut(link.partId) { mutableListOf() }.add(assetName)
                            }
                        } catch (_: Exception) {
                            // Access to equipment links is optional for stock users.
                        }
                        names
                    }
                advice =
                    try {
                        repository.latestAdvice().textRu
                    } catch (e: GatewayHttpException) {
                        if (e.status == 404) null else throw e
                    }
            } catch (e: Exception) {
                error = e.message ?: "Не удалось загрузить склад"
            } finally {
                loading = false
            }
        }
    }
    LaunchedEffect(repository, equipmentRepository) { reload() }

    AppScaffold(title = "Склад", modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(ClientSpacing.md).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                AppButton("Остатки", onClick = { tab = 0 }, variant = if (tab == 0) AppButtonVariant.Primary else AppButtonVariant.Secondary, fillMaxWidth = false)
                AppButton("Рекомендации", onClick = { tab = 1 }, variant = if (tab == 1) AppButtonVariant.Primary else AppButtonVariant.Secondary, fillMaxWidth = false)
            }
            if (loading) {
                CircularProgressIndicator()
            } else if (tab == 0) {
                if (canWrite) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                        AppButton("Добавить запчасть", { dialog = WarehouseDialog.AddPart }, fillMaxWidth = false)
                        AppButton("Приход", { dialog = WarehouseDialog.Receipt }, variant = AppButtonVariant.Secondary, fillMaxWidth = false)
                    }
                }
                if (parts.isEmpty()) AppText("Нет запчастей") else parts.forEach { part ->
                    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs)) {
                        AppText(part.name.ifBlank { "Запчасть" }, style = AppTextStyle.Title)
                        AppText("В наличии: ${formatWarehouseQty(part.onHand)} ${part.uom.ifBlank { "ед." }}")
                        linkedAssetNames[part.id]
                            ?.distinct()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { names -> AppText("Оборудование: ${names.joinToString()}") }
                    }
                }
            } else {
                AppText(advice?.takeIf { it.isNotBlank() } ?: "Пока нет рекомендаций")
            }
            error?.let { AppText(it) }
        }
    }
    when (dialog) {
        WarehouseDialog.AddPart -> AddPartDialog(
            onDismiss = { dialog = null },
            onSubmit = { name, uom, unitCost ->
                scope.launch {
                    try {
                        repository.createPart(CreateWarehousePartRequest(name = name, uom = uom, unitCost = unitCost))
                        dialog = null
                        reload()
                    } catch (e: Exception) {
                        error = e.message ?: "Не удалось добавить запчасть"
                    }
                }
            },
        )
        WarehouseDialog.Receipt -> ReceiptDialog(
            parts = parts,
            sites = sites,
            onDismiss = { dialog = null },
            onSubmit = { part, site, qty ->
                scope.launch {
                    try {
                        repository.receipt(StockReceiptRequest(part.id, site.id, qty.toDouble()))
                        dialog = null
                        reload()
                    } catch (e: Exception) {
                        error = e.message ?: "Не удалось оформить приход"
                    }
                }
            },
        )
        null -> Unit
    }
}

private enum class WarehouseDialog { AddPart, Receipt }

private fun warehouseAssetLabel(name: String): String {
    val value = name.trim()
    val opaqueUuid = Regex("""^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$""")
    return if (value.isBlank() || opaqueUuid.matches(value) || value.matches(Regex("""^\d{10,}$"""))) {
        "Оборудование"
    } else {
        value
    }
}

@Composable
private fun AddPartDialog(onDismiss: () -> Unit, onSubmit: (String, String, Double?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var uom by remember { mutableStateOf("шт") }
    var unitCost by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Новая запчасть") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                AppTextField(name, { name = it }, label = "Название")
                AppTextField(uom, { uom = it }, label = "Единица измерения")
                AppTextField(unitCost, { unitCost = it }, label = "Цена за единицу (необязательно)")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(name.trim(), uom.trim(), unitCost.replace(',', '.').toDoubleOrNull()) },
                enabled = name.isNotBlank() && uom.isNotBlank(),
            ) { AppText("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Отмена") } },
    )
}

@Composable
private fun ReceiptDialog(
    parts: List<WarehousePartDto>,
    sites: List<SiteDto>,
    onDismiss: () -> Unit,
    onSubmit: (WarehousePartDto, SiteDto, Int) -> Unit,
) {
    var partIndex by remember { mutableStateOf(0) }
    var siteIndex by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf("") }
    val part = parts.getOrNull(partIndex)
    val site = sites.getOrNull(siteIndex)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Приход") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                AppButton(part?.name ?: "Нет запчастей", { if (parts.isNotEmpty()) partIndex = (partIndex + 1) % parts.size }, variant = AppButtonVariant.Secondary)
                AppButton(site?.name?.ifBlank { "Площадка" } ?: "Нет площадок", { if (sites.isNotEmpty()) siteIndex = (siteIndex + 1) % sites.size }, variant = AppButtonVariant.Secondary)
                AppTextField(qty, { qty = it.filter(Char::isDigit) }, label = "Количество")
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(part!!, site!!, qty.toInt()) }, enabled = part != null && site != null && qty.toIntOrNull()?.let { it > 0 } == true) { AppText("Оформить") } },
        dismissButton = { TextButton(onClick = onDismiss) { AppText("Отмена") } },
    )
}
