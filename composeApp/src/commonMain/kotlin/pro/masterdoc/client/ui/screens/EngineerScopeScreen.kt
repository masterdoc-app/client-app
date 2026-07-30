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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.PutUserScopeRequest
import pro.masterdoc.client.auth.SiteDto
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

internal fun filterUsersForScopeBinding(
    users: List<AdminUser>,
    requiredFeature: String,
): List<AdminUser> = users.filter { requiredFeature in it.features }

internal fun filterEngineersForScopeBinding(users: List<AdminUser>): List<AdminUser> =
    filterUsersForScopeBinding(users, "engineer")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineerScopeScreen(
    userScopesRepository: UserScopesRepository,
    equipmentRepository: EquipmentRepository,
    adminUsersRepository: AdminUsersRepository?,
    hasAdminUsers: Boolean,
    recentAssigneeIds: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    requiredFeature: String = "engineer",
    title: String = "Привязка инженеров",
) {
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var catalogLoading by remember { mutableStateOf(true) }
    var scopeLoading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    var engineerId by remember { mutableStateOf("") }
    var userMenuExpanded by remember { mutableStateOf(false) }
    val engineers = remember(users, requiredFeature) { filterUsersForScopeBinding(users, requiredFeature) }
    var selectedSiteIds by remember { mutableStateOf(setOf<String>()) }
    var selectedAssetIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(equipmentRepository, adminUsersRepository, hasAdminUsers) {
        catalogLoading = true
        error = null
        try {
            sites = equipmentRepository.listSites().items
            assets = equipmentRepository.listAssets().items
            if (hasAdminUsers && adminUsersRepository != null) {
                users = adminUsersRepository.listUsers(limit = 200).items
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            error = e.message ?: "Ошибка загрузки справочников"
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки справочников"
        } finally {
            catalogLoading = false
        }
    }

    fun loadScope(userId: String) {
        if (userId.isBlank()) {
            selectedSiteIds = emptySet()
            selectedAssetIds = emptySet()
            return
        }
        scope.launch {
            scopeLoading = true
            error = null
            savedMessage = null
            try {
                val current = userScopesRepository.get(userId.trim())
                selectedSiteIds = current.siteIds.toSet()
                selectedAssetIds = current.assetIds.toSet()
            } catch (e: CancellationException) {
                throw e
            } catch (e: GatewayHttpException) {
                error = e.message ?: "Ошибка загрузки привязки"
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки привязки"
            } finally {
                scopeLoading = false
            }
        }
    }

    fun userLabel(userId: String): String {
        val user = users.find { it.id == userId }
        if (user == null) return userId
        val name = listOf(user.givenName, user.familyName).filter { it.isNotBlank() }.joinToString(" ")
        return when {
            name.isNotBlank() && user.email.isNotBlank() -> "$name · ${user.email}"
            user.email.isNotBlank() -> user.email
            name.isNotBlank() -> name
            else -> userId
        }
    }

    AppScaffold(
        title = title,
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
                AppText(
                    text =
                        if (requiredFeature == "engineer") {
                            "Выберите инженера и укажите цеха и/или оборудование в его зоне ответственности."
                        } else {
                            "Выберите заказчика и укажите цеха и/или оборудование в его зоне ответственности."
                        },
                style = AppTextStyle.Label,
            )

            when {
                hasAdminUsers && !catalogLoading && engineers.isNotEmpty() -> {
                    ExposedDropdownMenuBox(
                        expanded = userMenuExpanded,
                        onExpandedChange = { userMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = engineerId.takeIf { it.isNotBlank() }?.let(::userLabel) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (requiredFeature == "engineer") "Инженер" else "Заказчик") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userMenuExpanded) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = userMenuExpanded,
                            onDismissRequest = { userMenuExpanded = false },
                        ) {
                            engineers.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(userLabel(user.id)) },
                                    onClick = {
                                        engineerId = user.id
                                        userMenuExpanded = false
                                        loadScope(user.id)
                                    },
                                )
                            }
                        }
                    }
                }
                hasAdminUsers && !catalogLoading && engineers.isEmpty() -> {
                    AppText(
                        text =
                            if (requiredFeature == "engineer") {
                                "Нет пользователей с фичей Инженер"
                            } else {
                                "Нет пользователей с фичей Заказчик"
                            },
                        style = AppTextStyle.Label,
                    )
                }
                !hasAdminUsers -> {
                    OutlinedTextField(
                        value = engineerId,
                        onValueChange = {
                            engineerId = it
                            savedMessage = null
                        },
                        label = { Text(if (requiredFeature == "engineer") "ID инженера" else "ID заказчика") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (recentAssigneeIds.isNotEmpty()) {
                        AppText(text = "Недавние исполнители", style = AppTextStyle.Label)
                        recentAssigneeIds.forEach { id ->
                            AppButton(
                                text = id,
                                onClick = {
                                    engineerId = id
                                    loadScope(id)
                                },
                            )
                        }
                    }
                    AppButton(
                        text = "Загрузить привязку",
                        enabled = engineerId.isNotBlank() && !scopeLoading,
                        onClick = { loadScope(engineerId) },
                    )
                }
            }

            if (catalogLoading || scopeLoading) {
                CircularProgressIndicator()
            }

            if (engineerId.isNotBlank() && !catalogLoading) {
                AppText(text = "Цеха", style = AppTextStyle.Title)
                if (sites.isEmpty()) {
                    AppText(text = "Нет площадок", style = AppTextStyle.Label)
                } else {
                    sites.forEach { site ->
                        ScopeCheckboxRow(
                            label = site.name,
                            checked = site.id in selectedSiteIds,
                            onToggle = { checked ->
                                selectedSiteIds =
                                    if (checked) selectedSiteIds + site.id else selectedSiteIds - site.id
                                savedMessage = null
                            },
                        )
                    }
                }

                AppText(text = "Оборудование", style = AppTextStyle.Title)
                if (assets.isEmpty()) {
                    AppText(text = "Нет оборудования", style = AppTextStyle.Label)
                } else {
                    assets.forEach { asset ->
                        val siteName = sites.find { it.id == asset.siteId }?.name
                        val label =
                            buildString {
                                append(asset.name)
                                if (!asset.inventoryNo.isNullOrBlank()) append(" · ${asset.inventoryNo}")
                                if (siteName != null) append(" ($siteName)")
                            }
                        ScopeCheckboxRow(
                            label = label,
                            checked = asset.id in selectedAssetIds,
                            onToggle = { checked ->
                                selectedAssetIds =
                                    if (checked) selectedAssetIds + asset.id else selectedAssetIds - asset.id
                                savedMessage = null
                            },
                        )
                    }
                }

                AppButton(
                    text = if (saving) "Сохранение…" else "Сохранить",
                    enabled = !saving && engineerId.isNotBlank(),
                    onClick = {
                        scope.launch {
                            saving = true
                            error = null
                            savedMessage = null
                            try {
                                userScopesRepository.put(
                                    engineerId.trim(),
                                    PutUserScopeRequest(
                                        siteIds = selectedSiteIds.sorted(),
                                        assetIds = selectedAssetIds.sorted(),
                                    ),
                                )
                                savedMessage = "Привязка сохранена"
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: GatewayHttpException) {
                                error = e.message ?: "Ошибка сохранения"
                            } catch (e: Exception) {
                                error = e.message ?: "Ошибка сохранения"
                            } finally {
                                saving = false
                            }
                        }
                    },
                )
            }

            savedMessage?.let { AppText(text = it) }
            error?.let { AppText(text = it) }
        }
    }
}

@Composable
private fun ScopeCheckboxRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onToggle(!checked) },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
        )
        AppText(text = label)
    }
}
