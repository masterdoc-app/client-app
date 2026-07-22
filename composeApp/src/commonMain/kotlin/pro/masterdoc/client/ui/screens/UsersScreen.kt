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
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.InviteUserRequest
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.presentation.users.GrantableFeatures
import pro.masterdoc.client.presentation.users.InviteFormError
import pro.masterdoc.client.presentation.users.InviteFormValidator

@Composable
fun UsersScreen(
    repository: AdminUsersRepository,
    modifier: Modifier = Modifier,
) {
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var givenName by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var inviting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
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
            GrantableFeatures.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected =
                                    if (feature in selected) selected - feature else selected + feature
                            },
                ) {
                    Checkbox(
                        checked = feature in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + feature else selected - feature
                        },
                    )
                    AppText(text = feature)
                }
            }
            AppButton(
                text = if (inviting) "Отправка…" else "Пригласить",
                enabled = !inviting,
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
                            error = humanAdminError(e)
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
                        AppText(text = "${user.givenName} ${user.familyName} · ${user.email}")
                        AppText(text = user.features.joinToString(", ").ifEmpty { "-" })
                        AppText(text = user.state)
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

private fun humanAdminError(e: GatewayHttpException): String =
    when (e.status) {
        400 -> e.message.ifBlank { "Некорректный запрос" }
        403 -> "Нет доступа (нужна фича user_invite)"
        409 -> "Пользователь с таким email уже есть"
        502 -> "Сервис недоступен"
        else -> e.message.ifBlank { "Ошибка ${e.status}" }
    }
