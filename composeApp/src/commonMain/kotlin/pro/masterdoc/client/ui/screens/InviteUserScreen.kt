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
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.InviteUserRequest
import pro.masterdoc.client.auth.ProductRoleDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.presentation.users.InviteFormError
import pro.masterdoc.client.presentation.users.InviteFormValidator

@Composable
fun InviteUserScreen(
    repository: AdminUsersRepository,
    onBack: () -> Unit,
    onInvited: () -> Unit = onBack,
    modifier: Modifier = Modifier,
) {
    var roleCatalog by remember { mutableStateOf<List<ProductRoleDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var givenName by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var inviting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository) {
        loading = true
        error = null
        try {
            roleCatalog = repository.listRoles().items
        } catch (e: GatewayHttpException) {
            error = humanAdminError(e)
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки"
        } finally {
            loading = false
        }
    }

    AppScaffold(
        title = "Пригласить",
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            AppText(text = "Роли")
            when {
                loading && roleCatalog.isEmpty() -> CircularProgressIndicator()
                roleCatalog.isEmpty() -> AppText(text = "Каталог ролей недоступен")
                else ->
                    roleCatalog.forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected =
                                            if (role.id in selected) {
                                                selected - role.id
                                            } else {
                                                selected + role.id
                                            }
                                    },
                        ) {
                            Checkbox(
                                checked = role.id in selected,
                                onCheckedChange = { checked ->
                                    selected =
                                        if (checked) selected + role.id else selected - role.id
                                },
                            )
                            AppText(text = role.titleRu)
                        }
                    }
            }
            AppButton(
                text = if (inviting) "Отправка…" else "Пригласить",
                enabled = !inviting && roleCatalog.isNotEmpty(),
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
                                    roles = selected.sorted(),
                                ),
                            )
                            onInvited()
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
        }
    }
}

internal fun InviteFormError.toMessage(): String =
    when (this) {
        InviteFormError.EmailInvalid -> "Укажите корректный email"
        InviteFormError.GivenNameRequired -> "Укажите имя"
        InviteFormError.FamilyNameRequired -> "Укажите фамилию"
        InviteFormError.RolesRequired -> "Выберите хотя бы одну роль"
    }
