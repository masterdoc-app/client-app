package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.CreateWorkOrderRequest
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.UserScopeDto
import pro.masterdoc.client.auth.UserScopesRepository
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
import pro.masterdoc.client.platform.localEpochDay
import pro.masterdoc.client.platform.PickedImage
import pro.masterdoc.client.platform.rememberImagePickerLaunchers
import pro.masterdoc.client.platform.rememberAssetQrCameraController
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import androidx.compose.ui.graphics.toComposeImageBitmap

fun partitionCustomerTickets(orders: List<WorkOrderDto>): Pair<List<WorkOrderDto>, List<WorkOrderDto>> {
    val active = orders.filter { it.status == "new" || it.status == "in_progress" }
    val done = orders.filter { it.status == "closed" }
    return active to done
}

enum class TicketsEmptyState {
    NoScope,
    NoEquipment,
}

fun hasAssignedScope(scope: UserScopeDto?): Boolean =
    scope != null && (scope.siteIds.isNotEmpty() || scope.assetIds.isNotEmpty())

fun resolveTicketsEmptyState(
    scope: UserScopeDto?,
    assets: List<AssetDto>,
    scopesLoaded: Boolean,
): TicketsEmptyState? {
    if (assets.isNotEmpty()) return null
    if (scopesLoaded) {
        // Scope unknown/denied but gateway returned no assets → ask admin to bind цех.
        if (!hasAssignedScope(scope)) return TicketsEmptyState.NoScope
        return TicketsEmptyState.NoEquipment
    }
    return TicketsEmptyState.NoEquipment
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    repository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    attachmentsRepository: AttachmentsRepository,
    currentUserId: String?,
    userScopesRepository: UserScopesRepository? = null,
    onOpenAssetQr: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var userScope by remember { mutableStateOf<UserScopeDto?>(null) }
    var scopesLoaded by remember { mutableStateOf(userScopesRepository == null) }
    var orders by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var selectedAsset by remember { mutableStateOf<AssetDto?>(null) }
    var description by remember { mutableStateOf("") }
    var pendingPhotos by remember { mutableStateOf<List<PickedImage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var acting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var qrDialogOpen by remember { mutableStateOf(false) }
    var pendingQrToken by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var createStep by remember { mutableStateOf(defaultTicketsCreateStep()) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val imagePickers =
        rememberImagePickerLaunchers(
            onResult = { picked ->
                if (picked != null && pendingPhotos.size < 10) {
                    pendingPhotos = pendingPhotos + picked
                }
            },
            onError = { error = it },
        )
    val qrCamera =
        rememberAssetQrCameraController(
            active = createStep == TicketsCreateStep.Method,
            onRawQr = { raw ->
                val token = resolveScannedAssetQrToken(raw)
                if (token != null) {
                    cameraError = null
                    onOpenAssetQr(token)
                } else {
                    cameraError = "Код не найден или устарел"
                    qrDialogOpen = true
                }
            },
            onError = { message ->
                cameraError = message
                qrDialogOpen = true
            },
            onCancelled = { },
        )

    LaunchedEffect(qrDialogOpen, pendingQrToken) {
        if (!qrDialogOpen) {
            pendingQrToken?.let { token ->
                pendingQrToken = null
                onOpenAssetQr(token)
            }
        }
    }

    LaunchedEffect(repository, equipmentRepository, userScopesRepository, currentUserId, reloadKey) {
        loading = true
        error = null
        scopesLoaded = userScopesRepository == null
        userScope = null
        if (currentUserId.isNullOrBlank()) {
            error = "Не удалось определить пользователя"
            loading = false
            return@LaunchedEffect
        }
        try {
            if (userScopesRepository != null) {
                try {
                    userScope = userScopesRepository.get(currentUserId)
                } catch (e: GatewayHttpException) {
                    // Self-scope may be denied for some roles; gateway still filters /assets.
                    if (e.status != 403) throw e
                    userScope = null
                }
                scopesLoaded = true
            }
            assets = equipmentRepository.listAssets().items
            orders = repository.list(createdBy = currentUserId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            error = e.message ?: "Ошибка загрузки заявок"
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки заявок"
        } finally {
            loading = false
        }
    }

    selectedOrderId?.let { orderId ->
        WorkOrderDetailScreen(
            repository = repository,
            orderId = orderId,
            onBack = { selectedOrderId = null },
            equipmentRepository = equipmentRepository,
            currentUserId = currentUserId,
            readOnly = true,
            modifier = modifier,
        )
        return
    }

    val (active, done) = partitionCustomerTickets(orders)
    val emptyState = resolveTicketsEmptyState(userScope, assets, scopesLoaded)
    val createFormEnabled = emptyState == null
    if (qrDialogOpen) {
        AssetQrPasteDialog(
            onDismiss = { qrDialogOpen = false },
            onOpen = { token ->
                pendingQrToken = token
                qrDialogOpen = false
            },
        )
    }

    when (createStep) {
        TicketsCreateStep.List ->
            AppScaffold(title = "Заявки", modifier = modifier) { padding ->
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(padding).padding(ClientSpacing.md))
                    return@AppScaffold
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(ClientSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                            AppButton(
                                text = "Новая заявка",
                                onClick = { createStep = openCreate(createStep) },
                            )
                            error?.let { AppText(text = it) }
                        }
                    }
                    ticketSection("Активные", active) { selectedOrderId = it }
                    ticketSection("Завершённые", done) { selectedOrderId = it }
                }
            }

        TicketsCreateStep.Method ->
            AppScaffold(
                title = "Новая заявка",
                onNavigateBack = {
                    createStep = backFromMethod(createStep)
                },
                modifier = modifier,
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = ClientSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TicketsCameraHeroButton(
                            onClick = {
                                cameraError = null
                                qrCamera.openCamera()
                            },
                        )
                        Spacer(modifier = Modifier.height(ClientSpacing.md))
                        AppText(
                            text = "Сканировать QR или шильдик",
                            style = AppTextStyle.Title,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AppText(
                            text = "Нажмите, чтобы открыть камеру",
                            style = AppTextStyle.Label,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = ClientSpacing.xs).fillMaxWidth(),
                        )
                        cameraError?.let { message ->
                            Spacer(modifier = Modifier.height(ClientSpacing.sm))
                            AppText(text = message, style = AppTextStyle.Label)
                        }
                    }
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = ClientSpacing.md,
                                    vertical = ClientSpacing.md,
                                ),
                        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                    ) {
                        AppButton(
                            text = "Список оборудования",
                            variant = AppButtonVariant.Secondary,
                            onClick = { createStep = chooseList(createStep) },
                        )
                        AppText(
                            text = "или выберите оборудование вручную",
                            style = AppTextStyle.Label,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }

        TicketsCreateStep.EquipmentList ->
            AppScaffold(
                title = "Новая заявка",
                onNavigateBack = { createStep = backFromEquipmentList(createStep) },
                modifier = modifier,
            ) { padding ->
                if (assets.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(ClientSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        when (emptyState) {
                            TicketsEmptyState.NoScope ->
                                AppText(text = "Обратитесь к администратору для привязки к цеху")
                            TicketsEmptyState.NoEquipment ->
                                AppText(text = "Нет оборудования в вашем цехе")
                            null -> CircularProgressIndicator()
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(ClientSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                    ) {
                        items(assets, key = { it.id }) { asset ->
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAsset = asset
                                            createStep = selectEquipment(createStep)
                                        }
                                        .padding(vertical = ClientSpacing.sm),
                                verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                            ) {
                                AppText(
                                    text = asset.name.takeIf { it.isNotBlank() } ?: "Оборудование",
                                    style = AppTextStyle.Title,
                                )
                                AppText(text = "Выбрать оборудование", style = AppTextStyle.Label)
                            }
                        }
                    }
                }
            }

        TicketsCreateStep.Form ->
            AppScaffold(
                title = "Новая заявка",
                onNavigateBack = { createStep = backFromForm(createStep) },
                modifier = modifier,
            ) { padding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(ClientSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                ) {
                    when (emptyState) {
                        TicketsEmptyState.NoScope ->
                            AppText(text = "Обратитесь к администратору для привязки к цеху")
                        TicketsEmptyState.NoEquipment ->
                            AppText(text = "Нет оборудования в вашем цехе")
                        null -> Unit
                    }
                    AppText(
                        text = selectedAsset?.name?.takeIf { it.isNotBlank() } ?: "Оборудование",
                        style = AppTextStyle.Title,
                    )
                    AppTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Описание проблемы",
                        singleLine = false,
                        enabled = createFormEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (pendingPhotos.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                        ) {
                            pendingPhotos.forEachIndexed { index, photo ->
                                Box(modifier = Modifier.size(96.dp)) {
                                    val bitmap = remember(photo) {
                                        runCatching { makeFromEncoded(photo.bytes).toComposeImageBitmap() }.getOrNull()
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = photo.fileName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(96.dp).padding(ClientSpacing.xs),
                                        )
                                    } else {
                                        AppText(text = photo.fileName, modifier = Modifier.padding(ClientSpacing.sm))
                                    }
                                    IconButton(
                                        onClick = {
                                            pendingPhotos = pendingPhotos.filterIndexed { photoIndex, _ -> photoIndex != index }
                                        },
                                    ) {
                                        AppText(text = "×")
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                    ) {
                        AppButton(
                            text = "С диска",
                            variant = AppButtonVariant.Secondary,
                            enabled = createFormEnabled && pendingPhotos.size < 10 && !acting,
                            onClick = imagePickers.openGallery,
                        )
                        AppButton(
                            text = "Камера",
                            variant = AppButtonVariant.Secondary,
                            enabled = createFormEnabled && pendingPhotos.size < 10 && !acting,
                            onClick = imagePickers.openCamera,
                        )
                    }
                    AppButton(
                        text = if (acting) "Создание…" else "Создать",
                        enabled = createFormEnabled && !acting && selectedAsset != null && description.isNotBlank(),
                        onClick = {
                            val asset = selectedAsset ?: return@AppButton
                            scope.launch {
                                acting = true
                                error = null
                                try {
                                    val attachmentIds =
                                        pendingPhotos.map { photo ->
                                            attachmentsRepository.upload(
                                                bytes = photo.bytes,
                                                filename = photo.fileName,
                                                contentType = photo.contentType,
                                            ).id
                                        }
                                    val title =
                                        description.lineSequence().first().trim().take(120).ifBlank { "Заявка" }
                                    repository.create(
                                        CreateWorkOrderRequest(
                                            type = "emergency",
                                            title = title,
                                            assetId = asset.id,
                                            siteId = asset.siteId,
                                            dueAt = IsoDates.formatEpochDay(localEpochDay()),
                                            description = description,
                                            attachmentIds = attachmentIds.ifEmpty { null },
                                        ),
                                    )
                                    description = ""
                                    selectedAsset = null
                                    pendingPhotos = emptyList()
                                    reloadKey++
                                    createStep = afterSuccessfulCreate(createStep)
                                } catch (e: Exception) {
                                    error = e.message ?: "Ошибка создания заявки"
                                } finally {
                                    acting = false
                                }
                            }
                        },
                    )
                    error?.let { AppText(text = it) }
                }
            }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ticketSection(
    title: String,
    orders: List<WorkOrderDto>,
    onClick: (String) -> Unit,
) {
    item { AppText(text = title, style = AppTextStyle.Title) }
    if (orders.isEmpty()) {
        item { AppText(text = "Нет заявок") }
    } else {
        items(orders, key = { it.id }) { order ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClick(order.id) }
                        .padding(vertical = ClientSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
            ) {
                AppText(text = order.title, style = AppTextStyle.Title)
                AppText(text = "${workOrderTypeLabelRu(order.type)} · ${order.dueAt}")
                AppStatusChip(
                    text = workOrderStatusLabelRu(order.status),
                    tone = if (order.status == "new") AppStatusChipTone.Accent else AppStatusChipTone.Muted,
                )
            }
        }
    }
}
