package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import pro.masterdoc.client.auth.InviteUserRequest
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.presentation.users.InviteFormError
import pro.masterdoc.client.presentation.users.InviteFormValidator

@Composable
fun UsersScreen(
    repository: AdminUsersRepository,
    currentUserId: String? = null,
    modifier: Modifier = Modifier,
) {
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var featureCatalog by remember { mutableStateOf<List<FeatureDefinitionDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var givenName by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var inviting by remember { mutableStateOf(false) }
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

    LaunchedEffect(repository) { reload() }

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
            AppText(text = "Пригласить")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = givenName,
                onValueChange = { givenName = it },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = familyName,
                onValueChange = { familyName = it },
                label = { Text("Фамилия") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            AppText(text = "Фичи")
            when {
                loading && featureCatalog.isEmpty() -> CircularProgressIndicator()
                featureCatalog.isEmpty() -> AppText(text = "Каталог фич недоступен")
                else ->
                    featureCatalog.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected =
                                            if (feature.id in selected) {
                                                selected - feature.id
                                            } else {
                                                selected + feature.id
                                            }
                                    },
                        ) {
                            Checkbox(
                                checked = feature.id in selected,
                                onCheckedChange = { checked ->
                                    selected =
                                        if (checked) selected + feature.id else selected - feature.id
                                },
                            )
                            AppText(text = feature.titleRu)
                        }
                    }
            }
            AppButton(
                text = if (inviting) "Отправка…" else "Пригласить",
                enabled = !inviting && featureCatalog.isNotEmpty(),
                onClick = {
                    val validation =
                        InviteFormValidator.validate(email, givenName, familyName, selected)
                    if (validation != null) {
                        error = validation.toMessage()
                        return@AppButton
                    }
                    scope.launch {
                        inviting = true
                        error = null
                        try {
                            repository.inviteUser(
                                InviteUserRequest(
                                    email = email.trim(),
                                    givenName = givenName.trim(),
                                    familyName = familyName.trim(),
                                    features = selected.sorted(),
                                ),
                            )
                            email = ""
                            givenName = ""
                            familyName = ""
                            selected = emptySet()
                            users = repository.listUsers().items
                        } catch (e: GatewayHttpException) {
                            error = humanAdminError(e, AdminUserAction.Invite)
                        } catch (e: Exception) {
                            error = e.message ?: "Ошибка приглашения"
                        } finally {
                            inviting = false
                        }
                    }
                },
            )
            error?.let { AppText(text = it) }
            AppText(text = "Список")
            when {
                loading -> CircularProgressIndicator()
                else ->
                    users.forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
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

private fun InviteFormError.toMessage(): String =
    when (this) {
        InviteFormError.EmailInvalid -> "Укажите корректный email"
        InviteFormError.GivenNameRequired -> "Укажите имя"
        InviteFormError.FamilyNameRequired -> "Укажите фамилию"
        InviteFormError.FeaturesRequired -> "Выберите хотя бы одну фичу"
    }

private enum class AdminUserAction {
    Invite,
    Delete,
}

private fun humanAdminError(e: GatewayHttpException, action: AdminUserAction = AdminUserAction.Invite): String =
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
