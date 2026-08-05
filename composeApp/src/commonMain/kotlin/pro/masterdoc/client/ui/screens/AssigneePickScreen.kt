package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
fun AssigneePickScreen(
    workOrderId: String,
    repository: WorkOrdersRepository,
    userScopesRepository: UserScopesRepository,
    adminUsersRepository: AdminUsersRepository?,
    hasAdminUsers: Boolean,
    currentUserId: String?,
    onBack: () -> Unit,
    onAssigned: (WorkOrderDto) -> Unit = { onBack() },
    modifier: Modifier = Modifier,
) {
    var order by remember { mutableStateOf<WorkOrderDto?>(null) }
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var assigning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, workOrderId, userScopesRepository, adminUsersRepository, hasAdminUsers) {
        loading = true
        error = null
        try {
            val wo = repository.get(workOrderId)
            order = wo
            candidates = userScopesRepository.getCandidates(wo.assetId)
            users =
                if (hasAdminUsers && adminUsersRepository != null) {
                    try {
                        adminUsersRepository.listUsers(limit = 200).items
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки"
        } finally {
            loading = false
        }
    }

    fun pick(userId: String) {
        if (assigning) return
        scope.launch {
            assigning = true
            error = null
            try {
                val updated = repository.patch(workOrderId, assigneeId = userId)
                onAssigned(updated)
            } catch (e: Exception) {
                error = e.message ?: "Не удалось назначить исполнителя"
            } finally {
                assigning = false
            }
        }
    }

    AppScaffold(title = "Исполнитель", modifier = modifier, onNavigateBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(ClientSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            when {
                loading -> CircularProgressIndicator()
                error != null && order == null -> {
                    AppText(text = error!!)
                    AppButton(text = "Назад", onClick = onBack)
                }
                else -> {
                    val wo = order!!
                    val current = wo.assigneeId?.takeIf { it.isNotBlank() }
                    val eligible = filterEngineerEligibleAssignees(candidates, users)
                    if (error != null) AppText(text = error!!)
                    if (eligible.isEmpty()) {
                        AppText(text = "Нет инженеров в зоне ответственности")
                    }
                    eligible.forEach { userId ->
                        AppButton(
                            text = formatAssigneeLabel(userId, users, currentUserId),
                            onClick = { if (userId != current) pick(userId) },
                            enabled = !assigning,
                            variant =
                                if (userId == current) {
                                    AppButtonVariant.Primary
                                } else {
                                    AppButtonVariant.Secondary
                                },
                        )
                    }
                }
            }
        }
    }
}
