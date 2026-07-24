package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.FeatureDefinitionDto
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
fun UsersScreen(
    repository: AdminUsersRepository,
    currentUserId: String? = null,
    modifier: Modifier = Modifier,
) {
    var showInvite by remember { mutableStateOf(false) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var featureCatalog by remember { mutableStateOf<List<FeatureDefinitionDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val titleById = remember(featureCatalog) { featureCatalog.associate { it.id to it.titleRu } }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                featureCatalog = repository.listFeatures().items
                users = repository.listUsers().items
            } catch (e: GatewayHttpException) {
                error = humanAdminError(e)
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(repository, showInvite) {
        if (!showInvite) reload()
    }

    if (showInvite) {
        InviteUserScreen(
            repository = repository,
            onBack = { showInvite = false },
            onInvited = { showInvite = false },
            modifier = modifier,
        )
        return
    }

    AppScaffold(title = "Пользователи", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppButton(
                text = "Пригласить",
                onClick = { showInvite = true },
            )
            error?.let { AppText(text = it) }
            AppText(text = "Список")
            when {
                loading -> CircularProgressIndicator()
                else ->
                    users.forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ClientSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                AppText(text = "${user.givenName} ${user.familyName} · ${user.email}")
                                AppText(
                                    text =
                                        user.features
                                            .map { id -> titleById[id] ?: id }
                                            .joinToString(", ")
                                            .ifEmpty { "-" },
                                )
                                AppText(text = user.state)
                            }
                            if (currentUserId == null || user.id != currentUserId) {
                                AppButton(
                                    text = if (deletingId == user.id) "…" else "Удалить",
                                    enabled = deletingId == null,
                                    fillMaxWidth = false,
                                    onClick = {
                                        scope.launch {
                                            deletingId = user.id
                                            error = null
                                            try {
                                                repository.deleteUser(user.id)
                                                users = repository.listUsers().items
                                            } catch (e: GatewayHttpException) {
                                                error = humanAdminError(e, AdminUserAction.Delete)
                                            } catch (e: Exception) {
                                                error = e.message ?: "Ошибка удаления"
                                            } finally {
                                                deletingId = null
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
            }
        }
    }
}

internal enum class AdminUserAction {
    Invite,
    Delete,
}

internal fun humanAdminError(
    e: GatewayHttpException,
    action: AdminUserAction = AdminUserAction.Invite,
): String =
    when (e.status) {
        400 -> e.message.ifBlank { "Некорректный запрос" }
        403 -> "Нет доступа (нужна фича user_invite)"
        404 -> "Пользователь не найден"
        409 ->
            when (action) {
                AdminUserAction.Invite -> "Пользователь с таким email уже зарегистрирован"
                AdminUserAction.Delete -> "Нельзя удалить себя"
            }
        502 -> "Сервис недоступен"
        else -> e.message.ifBlank { "Ошибка ${e.status}" }
    }
