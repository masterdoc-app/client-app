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
import pro.masterdoc.client.auth.SiteDto
import pro.masterdoc.client.auth.UpdateSiteRequest
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

private enum class AdminTab { Users, Sites }

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
    var editingId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(address, geocodeRepository) {
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
                                address = suggestion.label
                                addressSuggestions = emptyList()
                            },
                )
            }
        }
        AppTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = "Широта (опционально)",
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = "Долгота (опционально)",
            modifier = Modifier.fillMaxWidth(),
        )
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
                latitude.isBlank() != longitude.isBlank() -> "Укажите и широту, и долготу"
                latitude.isNotBlank() && parsedLatitude == null -> "Некорректная широта"
                longitude.isNotBlank() && parsedLongitude == null -> "Некорректная долгота"
                parsedLatitude != null && parsedLatitude !in -90.0..90.0 -> "Широта должна быть от -90 до 90"
                parsedLongitude != null && parsedLongitude !in -180.0..180.0 -> "Долгота должна быть от -180 до 180"
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
        403 -> "Нет доступа (нужна фича admin)"
        404 -> "Пользователь не найден"
        409 ->
            when (action) {
                AdminUserAction.Invite -> "Пользователь с таким email уже зарегистрирован"
                AdminUserAction.Delete -> "Нельзя удалить себя"
            }
        502 -> "Сервис недоступен"
        else -> e.message.ifBlank { "Ошибка ${e.status}" }
    }
