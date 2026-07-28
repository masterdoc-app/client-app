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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.auth.WorkOrderDuration
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
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkOrderDetailScreen(
    repository: WorkOrdersRepository,
    orderId: String,
    onBack: () -> Unit,
    onChanged: () -> Unit = {},
    userScopesRepository: UserScopesRepository? = null,
    adminUsersRepository: AdminUsersRepository? = null,
    equipmentRepository: EquipmentRepository? = null,
    currentUserId: String? = null,
    hasAdminUsers: Boolean = false,
    editableAssignee: Boolean = false,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var order by remember { mutableStateOf<WorkOrderDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var acting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var assistantOpen by remember(orderId) { mutableStateOf(false) }
    var assistantHidden by remember(orderId) { mutableStateOf(false) }
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
                    if (wo.status == "closed" || readOnly) {
                        DetailRow("Длительность, ч", wo.durationHours.toString())
                    } else {
                        var durationDraft by remember(wo.id, wo.durationHours) {
                            mutableStateOf(wo.durationHours.toString())
                        }
                        AppTextField(
                            value = durationDraft,
                            onValueChange = { durationDraft = it.filter { ch -> ch.isDigit() } },
                            label = "Длительность, ч",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val parsedDuration = durationDraft.toIntOrNull()
                        if (parsedDuration != null && parsedDuration > WorkOrderDuration.MAX_DURATION_HOURS) {
                            AppText(text = "Длительность не может превышать ${WorkOrderDuration.MAX_DURATION_HOURS} ч")
                        }
                        if (
                            parsedDuration != null &&
                            parsedDuration in 1..WorkOrderDuration.MAX_DURATION_HOURS &&
                            parsedDuration != wo.durationHours
                        ) {
                            AppButton(
                                text = if (acting) "…" else "Сохранить длительность",
                                onClick = {
                                    scope.launch {
                                        acting = true
                                        error = null
                                        try {
                                            order = repository.patch(wo.id, durationHours = parsedDuration)
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
                    DetailRow("Начало", wo.dueAt)
                    DetailRow("Площадка", wo.siteId)
                    DetailRow("Оборудование", wo.assetId)
                    if (editableAssignee && userScopesRepository != null) {
                        AssigneePickerRow(
                            workOrder = wo,
                            repository = repository,
                            userScopesRepository = userScopesRepository,
                            adminUsersRepository = adminUsersRepository,
                            hasAdminUsers = hasAdminUsers,
                            acting = acting,
                            onActingChange = { acting = it },
                            onError = { error = it },
                            onUpdated = { updated ->
                                order = updated
                                onChanged()
                            },
                        )
                    } else {
                        DetailRow("Исполнитель", wo.assigneeId ?: "не назначен")
                    }
                    DetailRow("Источник", wo.source)
                    if (wo.type == "ppr") {
                        DetailRow("ППР", wo.maintenanceMapId.orEmpty())
                        DetailRow("Пункт ППР", wo.maintenanceMapItemId.orEmpty())
                    }
                    if (error != null) {
                        AppText(text = error!!)
                    }
                    val showAssistant =
                        !assistantHidden &&
                            equipmentRepository != null &&
                            shouldShowWoAssistant(wo.assigneeId, currentUserId)
                    if (showAssistant) {
                        AppButton(
                            text = "Ассистент",
                            onClick = { assistantOpen = true },
                            variant = AppButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (assistantOpen && equipmentRepository != null) {
                        WorkOrderAssistantDialog(
                            workOrderId = wo.id,
                            repository = equipmentRepository,
                            onDismiss = { assistantOpen = false },
                            onAssigneeForbidden = {
                                assistantHidden = true
                                assistantOpen = false
                                reload()
                            },
                        )
                    }
                    if (!readOnly) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssigneePickerRow(
    workOrder: WorkOrderDto,
    repository: WorkOrdersRepository,
    userScopesRepository: UserScopesRepository,
    adminUsersRepository: AdminUsersRepository?,
    hasAdminUsers: Boolean,
    acting: Boolean,
    onActingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onUpdated: (WorkOrderDto) -> Unit,
) {
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var candidatesLoading by remember { mutableStateOf(true) }
    var candidatesError by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userScopesRepository, workOrder.assetId) {
        candidatesLoading = true
        candidatesError = null
        try {
            candidates = userScopesRepository.getCandidates(workOrder.assetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            candidatesError = e.message ?: "Ошибка загрузки кандидатов"
        } catch (e: Exception) {
            candidatesError = e.message ?: "Ошибка загрузки кандидатов"
        } finally {
            candidatesLoading = false
        }
    }

    LaunchedEffect(adminUsersRepository, hasAdminUsers) {
        if (hasAdminUsers && adminUsersRepository != null) {
            try {
                users = adminUsersRepository.listUsers(limit = 200).items
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                users = emptyList()
            }
        } else {
            users = emptyList()
        }
    }

    fun userLabel(userId: String): String = formatAssigneeLabel(userId, users)

    fun assignAssignee(userId: String?) {
        scope.launch {
            onActingChange(true)
            onError(null)
            try {
                val updated =
                    if (userId == null) {
                        repository.patch(workOrder.id, clearAssignee = true)
                    } else {
                        repository.patch(workOrder.id, assigneeId = userId)
                    }
                onUpdated(updated)
            } catch (e: GatewayHttpException) {
                onError(e.message ?: "Не удалось назначить исполнителя")
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось назначить исполнителя")
            } finally {
                onActingChange(false)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(text = "Исполнитель", style = AppTextStyle.Label)
        when {
            candidatesLoading -> CircularProgressIndicator()
            candidatesError != null -> AppText(text = candidatesError!!)
            else -> {
                val currentAssignee = workOrder.assigneeId?.takeIf { it.isNotBlank() }
                val displayValue =
                    when {
                        acting -> "…"
                        currentAssignee != null -> userLabel(currentAssignee)
                        else -> "не назначен"
                    }
                val eligibleCandidates = filterEquipmentEligibleAssignees(candidates, users)
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { if (!acting) menuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = displayValue,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !acting,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("не назначен") },
                            onClick = {
                                menuExpanded = false
                                if (currentAssignee != null) {
                                    assignAssignee(null)
                                }
                            },
                        )
                        eligibleCandidates.forEach { userId ->
                            DropdownMenuItem(
                                text = { Text(userLabel(userId)) },
                                onClick = {
                                    menuExpanded = false
                                    if (userId != currentAssignee) {
                                        assignAssignee(userId)
                                    }
                                },
                            )
                        }
                    }
                }
                if (currentAssignee != null && currentAssignee !in eligibleCandidates) {
                    AppText(
                        text = "Текущий исполнитель вне зоны ответственности для этого оборудования",
                        style = AppTextStyle.Label,
                    )
                }
            }
        }
    }
}

internal fun formatAssigneeLabel(
    userId: String,
    users: List<AdminUser>,
): String {
    val user = users.find { it.id == userId } ?: return userId
    val name = listOf(user.givenName, user.familyName).filter { it.isNotBlank() }.joinToString(" ")
    return when {
        name.isNotBlank() && user.email.isNotBlank() -> "$name · ${user.email}"
        user.email.isNotBlank() -> user.email
        name.isNotBlank() -> name
        else -> userId
    }
}

/**
 * When admin user directory is available, hide board-only candidates (no `equipment`).
 * Unknown ids (not in [users]) are kept — server hard-enforces on PATCH.
 * If [users] is empty (no admin access), return candidates unchanged.
 */
internal fun filterEquipmentEligibleAssignees(
    candidates: List<String>,
    users: List<AdminUser>,
): List<String> {
    if (users.isEmpty()) return candidates
    val byId = users.associateBy { it.id }
    return candidates.filter { id ->
        val user = byId[id] ?: return@filter true
        "equipment" in user.features
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
