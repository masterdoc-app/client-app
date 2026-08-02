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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.CreateSiteRequest
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.FeatureDefinitionDto
import pro.masterdoc.client.auth.GeocodeRepository
import pro.masterdoc.client.auth.GeocodeSuggestItem
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.ProductRoleDto
import pro.masterdoc.client.auth.SiteDto
import pro.masterdoc.client.auth.UpdateSiteRequest
import pro.masterdoc.client.auth.UpdateRoleRequest
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.navigation.FeatureId
import pro.masterdoc.client.navigation.titleRu

private enum class AdminTab { Users, Sites, Roles }

@Composable
fun UsersScreen(
    repository: AdminUsersRepository,
    equipmentRepository: EquipmentRepository? = null,
    userScopesRepository: UserScopesRepository? = null,
    geocodeRepository: GeocodeRepository? = null,
    currentUserId: String? = null,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(AdminTab.Users) }
    var showInvite by remember { mutableStateOf(false) }
    var showCustomerScopeBinding by remember { mutableStateOf(false) }
    var showEngineerScopeBinding by remember { mutableStateOf(false) }
    val canBindScopes = equipmentRepository != null && userScopesRepository != null

    if (showInvite) {
        InviteUserScreen(
            repository = repository,
            onBack = { showInvite = false },
            onInvited = { showInvite = false },
            modifier = modifier,
        )
        return
    }

    if (showCustomerScopeBinding && canBindScopes) {
        EngineerScopeScreen(
            userScopesRepository = userScopesRepository!!,
            equipmentRepository = equipmentRepository!!,
            adminUsersRepository = repository,
            hasAdminUsers = true,
            onBack = { showCustomerScopeBinding = false },
            requiredFeature = "tickets",
            title = "Привязка заказчиков",
            modifier = modifier,
        )
        return
    }

    if (showEngineerScopeBinding && canBindScopes) {
        EngineerScopeScreen(
            userScopesRepository = userScopesRepository!!,
            equipmentRepository = equipmentRepository!!,
            adminUsersRepository = repository,
            hasAdminUsers = true,
            onBack = { showEngineerScopeBinding = false },
            requiredFeature = "engineer",
            title = "Привязка инженеров",
            modifier = modifier,
        )
        return
    }

    AppScaffold(title = "Админ", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                AdminTab.entries.forEach { t ->
                    AppButton(
                        text =
                            when (t) {
                                AdminTab.Users -> "Пользователи"
                                AdminTab.Sites -> "Площадки"
                                AdminTab.Roles -> "Роли"
                            },
                        onClick = { tab = t },
                        variant = if (tab == t) AppButtonVariant.Primary else AppButtonVariant.Secondary,
                        fillMaxWidth = false,
                    )
                }
            }
            when (tab) {
                AdminTab.Users ->
                    UsersTab(
                        repository = repository,
                        currentUserId = currentUserId,
                        onInvite = { showInvite = true },
                        onOpenCustomerScopeBinding = { showCustomerScopeBinding = true },
                        onOpenEngineerScopeBinding = { showEngineerScopeBinding = true },
                        canBindScopes = canBindScopes,
                    )
                AdminTab.Sites ->
                    if (equipmentRepository != null) {
                        SitesTab(
                            equipmentRepository = equipmentRepository,
                            geocodeRepository = geocodeRepository,
                        )
                    } else {
                        AppText(text = "Каталог площадок недоступен")
                    }
                AdminTab.Roles -> RolesTab(repository = repository)
            }
        }
    }
}

@Composable
private fun UsersTab(
    repository: AdminUsersRepository,
    currentUserId: String?,
    onInvite: () -> Unit,
    onOpenCustomerScopeBinding: () -> Unit,
    onOpenEngineerScopeBinding: () -> Unit,
    canBindScopes: Boolean,
) {
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

    LaunchedEffect(repository) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
            AppButton(text = "Пригласить", onClick = onInvite)
            if (canBindScopes) {
                AppButton(
                    text = "Привязка инженеров",
                    onClick = onOpenEngineerScopeBinding,
                    variant = AppButtonVariant.Secondary,
                )
                AppButton(
                    text = "Привязка заказчиков",
                    onClick = onOpenCustomerScopeBinding,
                    variant = AppButtonVariant.Secondary,
                )
            }
        }
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
                                        .map { id ->
                                            titleById[id] ?: FeatureId.fromWire(id)?.titleRu() ?: "Фича"
                                        }
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

@Composable
private fun SitesTab(
    equipmentRepository: EquipmentRepository,
    geocodeRepository: GeocodeRepository?,
) {
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var geofenceRadiusM by remember { mutableStateOf("") }
    var addressSuggestions by remember { mutableStateOf<List<GeocodeSuggestItem>>(emptyList()) }
    /** Skip one suggest cycle after picking a Photon result (address+coords already filled). */
    var skipNextAddressSuggest by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(address, geocodeRepository) {
        if (skipNextAddressSuggest) {
            skipNextAddressSuggest = false
            addressSuggestions = emptyList()
            return@LaunchedEffect
        }
        val query = address.trim()
        if (geocodeRepository == null || query.length < 3) {
            addressSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        addressSuggestions = runCatching { geocodeRepository.suggest(query) }.getOrDefault(emptyList())
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                sites = equipmentRepository.listSites().items
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки площадок"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(equipmentRepository) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text =
                if (editingId == null) {
                    "Новая площадка"
                } else {
                    "Редактирование: ${sites.find { it.id == editingId }?.name?.takeIf { it.isNotBlank() } ?: name.ifBlank { "площадка" }}"
                },
            style = AppTextStyle.Title,
        )
        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = "Название",
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTextField(
                value = address,
                onValueChange = {
                    address = it
                    latitude = ""
                    longitude = ""
                    addressSuggestions = emptyList()
                },
                label = "Адрес (опционально)",
                modifier = Modifier.fillMaxWidth(),
            )
            addressSuggestions.forEach { suggestion ->
                AppText(
                    text = suggestion.label,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Address + coords belong to the site (цех), not equipment.
                                // Lat/lon are derived from the selected address — not edited manually.
                                skipNextAddressSuggest = true
                                address = suggestion.label
                                latitude = suggestion.lat.toString()
                                longitude = suggestion.lon.toString()
                                if (geofenceRadiusM.isBlank()) {
                                    geofenceRadiusM = "200"
                                }
                                addressSuggestions = emptyList()
                            },
                )
            }
            if (latitude.isNotBlank() && longitude.isNotBlank()) {
                AppText(
                    text = "Координаты из адреса: $latitude, $longitude",
                    style = AppTextStyle.Label,
                )
            }
        }
        AppTextField(
            value = geofenceRadiusM,
            onValueChange = { geofenceRadiusM = it },
            label = "Радиус геозоны, м (опционально)",
            modifier = Modifier.fillMaxWidth(),
        )
        val parsedLatitude = latitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val parsedLongitude = longitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val parsedRadius = geofenceRadiusM.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val geofenceError =
            when {
                geofenceRadiusM.isNotBlank() && (parsedRadius == null || parsedRadius <= 0) ->
                    "Радиус должен быть положительным числом"
                else -> null
            }
        geofenceError?.let { AppText(text = it, style = AppTextStyle.Label) }
        AppButton(
            text = if (editingId == null) "Создать площадку" else "Сохранить",
            enabled = !busy && name.isNotBlank() && geofenceError == null,
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        if (editingId == null) {
                            val id =
                                name
                                    .lowercase()
                                    .replace(' ', '-')
                                    .replace("№", "")
                                    .ifBlank { null }
                            equipmentRepository.createSite(
                                CreateSiteRequest(
                                    name = name.trim(),
                                    address = address.trim().takeIf { it.isNotEmpty() },
                                    id = id,
                                    lat = parsedLatitude,
                                    lon = parsedLongitude,
                                    geofenceRadiusM = parsedRadius,
                                ),
                            )
                        } else {
                            equipmentRepository.updateSite(
                                editingId!!,
                                UpdateSiteRequest(
                                    name = name.trim(),
                                    address = address.trim().takeIf { it.isNotEmpty() },
                                    lat = parsedLatitude,
                                    lon = parsedLongitude,
                                    geofenceRadiusM = parsedRadius,
                                ),
                            )
                        }
                        name = ""
                        address = ""
                        latitude = ""
                        longitude = ""
                        geofenceRadiusM = ""
                        editingId = null
                        reload()
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            },
        )
        if (editingId != null) {
            AppButton(
                text = "Отмена",
                variant = AppButtonVariant.Secondary,
                onClick = {
                    editingId = null
                    name = ""
                    address = ""
                    latitude = ""
                    longitude = ""
                    geofenceRadiusM = ""
                },
            )
        }
        error?.let { AppText(text = it) }
        AppText(text = "Площадки", style = AppTextStyle.Title)
        when {
            loading -> CircularProgressIndicator()
            sites.isEmpty() -> AppText(text = "Пока нет площадок", style = AppTextStyle.Label)
            else ->
                sites.forEach { site ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(text = site.name)
                            AppText(
                                text =
                                    listOfNotNull(
                                        site.address?.takeIf { it.isNotBlank() },
                                        site.lat?.let { lat ->
                                            site.lon?.let { lon -> "Координаты: $lat, $lon" }
                                        },
                                        site.geofenceRadiusM?.let { "Радиус: $it м" },
                                    ).joinToString(" · ").ifBlank { "—" },
                                style = AppTextStyle.Label,
                            )
                        }
                        AppButton(
                            text = "Изменить",
                            fillMaxWidth = false,
                            variant = AppButtonVariant.Secondary,
                            onClick = {
                                editingId = site.id
                                name = site.name
                                address = site.address.orEmpty()
                                latitude = site.lat?.toString().orEmpty()
                                longitude = site.lon?.toString().orEmpty()
                                geofenceRadiusM = site.geofenceRadiusM?.toString().orEmpty()
                            },
                        )
                        AppButton(
                            text = "Удалить",
                            fillMaxWidth = false,
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    error = null
                                    try {
                                        equipmentRepository.deleteSite(site.id)
                                        reload()
                                    } catch (e: GatewayHttpException) {
                                        error =
                                            when (e.status) {
                                                409 -> "На площадке есть оборудование — сначала перенесите"
                                                else -> e.message
                                            }
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        )
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RolesTab(repository: AdminUsersRepository) {
    var roles by remember { mutableStateOf<List<ProductRoleDto>>(emptyList()) }
    var featureCatalog by remember { mutableStateOf<List<FeatureDefinitionDto>>(emptyList()) }
    var selectedByRole by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var addMenuExpandedByRole by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var savingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            success = null
            try {
                val catalog = repository.listFeatures().items
                val loadedRoles = repository.listRoles().items
                featureCatalog = catalog
                roles = loadedRoles
                selectedByRole = loadedRoles.associate { it.id to it.features.toSet() }
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        error?.let { AppText(text = it) }
        success?.let { AppText(text = it) }
        when {
            loading -> CircularProgressIndicator()
            roles.isEmpty() -> AppText(text = "Роли недоступны")
            featureCatalog.isEmpty() -> AppText(text = "Каталог функций недоступен")
            else ->
                roles.forEach { role ->
                    val selected = selectedByRole[role.id].orEmpty()
                    val assigned = featureCatalog.filter { it.id in selected }
                    val available = featureCatalog.filter { it.id !in selected }
                    val addMenuExpanded = addMenuExpandedByRole[role.id] == true
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AppText(text = role.titleRu, style = AppTextStyle.Title)
                        assigned.forEach { feature ->
                            val isProtectedAdminFeature = role.id == "admin" && feature.id == "admin"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppText(text = feature.titleRu)
                                AppButton(
                                    text = "Удалить",
                                    variant = AppButtonVariant.Secondary,
                                    fillMaxWidth = false,
                                    enabled = !isProtectedAdminFeature,
                                    onClick = {
                                        selectedByRole =
                                            selectedByRole + (role.id to (selected - feature.id))
                                    },
                                )
                            }
                        }
                        ExposedDropdownMenuBox(
                            expanded = addMenuExpanded && available.isNotEmpty(),
                            onExpandedChange = {
                                if (available.isNotEmpty()) {
                                    addMenuExpandedByRole = addMenuExpandedByRole + (role.id to it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                readOnly = true,
                                enabled = available.isNotEmpty(),
                                label = { Text("Добавить функцию") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = addMenuExpanded)
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = addMenuExpanded,
                                onDismissRequest = {
                                    addMenuExpandedByRole = addMenuExpandedByRole + (role.id to false)
                                },
                            ) {
                                available.forEach { feature ->
                                    DropdownMenuItem(
                                        text = { Text(feature.titleRu) },
                                        onClick = {
                                            selectedByRole =
                                                selectedByRole + (role.id to (selected + feature.id))
                                            addMenuExpandedByRole =
                                                addMenuExpandedByRole + (role.id to false)
                                        },
                                    )
                                }
                            }
                        }
                        AppButton(
                            text = if (savingId == role.id) "Сохранение…" else "Сохранить",
                            enabled = savingId == null && selected.isNotEmpty(),
                            onClick = {
                                scope.launch {
                                    savingId = role.id
                                    error = null
                                    success = null
                                    try {
                                        val updated =
                                            repository.updateRole(
                                                role.id,
                                                UpdateRoleRequest(
                                                    features = selected.sorted(),
                                                    titleRu = role.titleRu,
                                                ),
                                            )
                                        roles = roles.map { if (it.id == updated.id) updated else it }
                                        selectedByRole = selectedByRole + (role.id to updated.features.toSet())
                                        success = "Роль «${updated.titleRu}» сохранена"
                                    } catch (e: GatewayHttpException) {
                                        error = humanAdminError(e, AdminUserAction.Role)
                                    } catch (e: Exception) {
                                        error = e.message ?: "Ошибка сохранения роли"
                                    } finally {
                                        savingId = null
                                    }
                                }
                            },
                        )
                    }
                }
        }
    }
}

internal enum class AdminUserAction {
    Invite,
    Delete,
    Role,
}

internal fun humanAdminError(
    e: GatewayHttpException,
    action: AdminUserAction = AdminUserAction.Invite,
): String =
    when (e.status) {
        400 -> e.message.ifBlank { "Некорректный запрос" }
        403 -> "Нет доступа (нужна фича admin)"
        404 ->
            when (action) {
                AdminUserAction.Role -> "Роль не найдена"
                else -> "Пользователь не найден"
            }
        409 ->
            when (action) {
                AdminUserAction.Invite -> "Пользователь с таким email уже зарегистрирован"
                AdminUserAction.Delete -> "Нельзя удалить себя"
                AdminUserAction.Role -> "Роль не может быть сохранена"
            }
        502 -> "Сервис недоступен"
        else -> e.message.ifBlank { "Ошибка ${e.status}" }
    }
