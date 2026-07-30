package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AuditEventDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.presentation.audit.AuditEventDescription
import pro.masterdoc.client.presentation.audit.formatAuditAt

private const val PageSize = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackBoxScreen(
    repository: AdminUsersRepository,
    modifier: Modifier = Modifier,
) {
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<AuditEventDto>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun actorLabel(userId: String): String = formatAssigneeLabel(userId, users)

    fun filterLabel(): String =
        selectedUserId?.let { actorLabel(it) } ?: "Все"

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                val page = repository.listAudit(limit = PageSize, offset = 0, userId = selectedUserId)
                events = page.items
                hasMore = page.items.size == PageSize
            } catch (e: Exception) {
                error = e.message ?: "Ошибка журнала"
                events = emptyList()
                hasMore = false
            } finally {
                loading = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            error = null
            try {
                val page =
                    repository.listAudit(
                        limit = PageSize,
                        offset = events.size,
                        userId = selectedUserId,
                    )
                events = events + page.items
                hasMore = page.items.size == PageSize
            } catch (e: Exception) {
                error = e.message ?: "Ошибка журнала"
            } finally {
                loadingMore = false
            }
        }
    }

    LaunchedEffect(repository) {
        try {
            users = repository.listUsers().items
        } catch (_: Exception) {
            users = emptyList()
        }
        reload()
    }

    AppScaffold(title = "Чёрный ящик", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
            ) {
                OutlinedTextField(
                    value = filterLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Пользователь") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Все") },
                        onClick = {
                            selectedUserId = null
                            menuExpanded = false
                            reload()
                        },
                    )
                    users.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(actorLabel(user.id)) },
                            onClick = {
                                selectedUserId = user.id
                                menuExpanded = false
                                reload()
                            },
                        )
                    }
                }
            }

            AppButton(text = "Обновить", onClick = { reload() }, fillMaxWidth = false)
            error?.let { AppText(text = it) }

            when {
                loading -> CircularProgressIndicator()
                events.isEmpty() -> AppText(text = "Пока нет событий", style = AppTextStyle.Label)
                else -> {
                    events.forEach { e ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            AppText(
                                text =
                                    AuditEventDescription.title(
                                        action = e.action,
                                        method = e.method,
                                        path = e.path,
                                        requestSummary = e.requestSummary,
                                    ),
                            )
                            if (selectedUserId == null) {
                                AppText(text = actorLabel(e.userId), style = AppTextStyle.Label)
                            }
                            AppText(
                                text = formatAuditAt(e.at),
                                style = AppTextStyle.Label,
                            )
                        }
                    }
                    if (hasMore) {
                        AppButton(
                            text = if (loadingMore) "Загрузка…" else "Ещё",
                            onClick = { loadMore() },
                            fillMaxWidth = false,
                        )
                    }
                }
            }
        }
    }
}
