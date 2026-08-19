package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.AttachmentsRepository
import pro.masterdoc.client.auth.CommentsRepository
import pro.masterdoc.client.auth.CreateWorkOrderCommentRequest
import pro.masterdoc.client.auth.EngineerLocationSnapshot
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.auth.SiteDto
import pro.masterdoc.client.auth.WorkOrderCommentDto
import pro.masterdoc.client.auth.WorkOrderDuration
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.auth.WarehousePartDto
import pro.masterdoc.client.auth.WarehouseRepository
import pro.masterdoc.client.auth.formatWarehouseQty
import pro.masterdoc.client.auth.AssetPartDto
import pro.masterdoc.client.auth.StockIssueRequest
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
import pro.masterdoc.client.platform.PickedImage
import pro.masterdoc.client.platform.decodePickedImage
import pro.masterdoc.client.platform.rememberImagePickerLaunchers
import pro.masterdoc.client.tracking.LocationTrackingController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkOrderDetailScreen(
    repository: WorkOrdersRepository,
    orderId: String,
    onBack: () -> Unit,
    onChanged: () -> Unit = {},
    adminUsersRepository: AdminUsersRepository? = null,
    equipmentRepository: EquipmentRepository? = null,
    currentUserId: String? = null,
    onOpenMentor: (() -> Unit)? = null,
    onOpenAssigneePick: (() -> Unit)? = null,
    onOpenEquipment: (String) -> Unit = {},
    hasAdminUsers: Boolean = false,
    editableAssignee: Boolean = false,
    locationTrackingController: LocationTrackingController? = null,
    readOnly: Boolean = false,
    allowMediaMutations: Boolean = true,
    attachmentsRepository: AttachmentsRepository? = null,
    commentsRepository: CommentsRepository? = null,
    warehouseRepository: WarehouseRepository? = null,
    modifier: Modifier = Modifier,
) {
    var order by remember { mutableStateOf<WorkOrderDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var acting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var asset by remember { mutableStateOf<AssetDto?>(null) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var directoryUsers by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var pprMap by remember { mutableStateOf<MaintenanceMapDto?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var photoSourceDialogOpen by remember { mutableStateOf(false) }
    var commentPhotoSourceDialogOpen by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<WorkOrderCommentDto>>(emptyList()) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var pendingCommentPhoto by remember { mutableStateOf<PickedImage?>(null) }
    var sendingComment by remember { mutableStateOf(false) }
    var pickingCommentPhoto by remember { mutableStateOf(false) }
    var compatibleParts by remember { mutableStateOf<List<AssetPartDto>>(emptyList()) }
    var warehouseParts by remember { mutableStateOf<List<WarehousePartDto>>(emptyList()) }
    var warehouseVisible by remember { mutableStateOf(warehouseRepository != null) }
    val scope = rememberCoroutineScope()
    val imagePickers =
        rememberImagePickerLaunchers(
            onResult = { picked ->
                if (pickingCommentPhoto) {
                    pickingCommentPhoto = false
                    if (picked != null && !sendingComment) {
                        pendingCommentPhoto = picked
                    }
                } else if (picked != null && !uploadingPhoto && (order?.attachmentIds?.size ?: 0) < 10) {
                    scope.launch {
                        uploadingPhoto = true
                        error = null
                        try {
                            val attachment =
                                attachmentsRepository?.upload(
                                    bytes = picked.bytes,
                                    filename = picked.fileName,
                                    contentType = picked.contentType,
                                ) ?: return@launch
                            repository.attach(orderId, listOf(attachment.id))
                            order = repository.get(orderId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Не удалось добавить фото"
                        } finally {
                            uploadingPhoto = false
                        }
                    }
                }
            },
            onError = { error = it },
        )

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                order = repository.get(orderId)
            } catch (e: GatewayHttpException) {
                error = e.message ?: "Ошибка загрузки"
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(repository, orderId) {
        reload()
    }

    LaunchedEffect(commentsRepository, orderId) {
        val commentsRepo = commentsRepository ?: return@LaunchedEffect
        commentsLoading = true
        try {
            comments = commentsRepo.list(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить комментарии"
        } finally {
            commentsLoading = false
        }
    }

    LaunchedEffect(equipmentRepository, order?.assetId) {
        asset = null
        val assetId = order?.assetId ?: return@LaunchedEffect
        val assets = equipmentRepository ?: return@LaunchedEffect
        asset =
            try {
                findAssetById(assets.listAssets().items, assetId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
    }

    LaunchedEffect(adminUsersRepository, hasAdminUsers) {
        directoryUsers =
            if (hasAdminUsers && adminUsersRepository != null) {
                try {
                    adminUsersRepository.listUsers(limit = 200).items
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
    }

    LaunchedEffect(equipmentRepository, order?.maintenanceMapId) {
        pprMap = null
        val mapId = order?.maintenanceMapId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val equipment = equipmentRepository ?: return@LaunchedEffect
        pprMap =
            try {
                equipment.listMaps().items.find { it.id == mapId }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
    }

    LaunchedEffect(equipmentRepository) {
        sites =
            try {
                equipmentRepository?.listSites()?.items.orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
    }

    LaunchedEffect(warehouseRepository, order?.assetId) {
        val warehouse = warehouseRepository ?: return@LaunchedEffect
        val assetId = order?.assetId ?: return@LaunchedEffect
        try {
            compatibleParts = warehouse.assetParts(assetId)
            warehouseParts = warehouse.listParts()
            warehouseVisible = true
        } catch (e: GatewayHttpException) {
            if (e.status == 403) warehouseVisible = false else error = e.message
        } catch (_: Exception) {
            warehouseVisible = false
        }
    }

    AppScaffold(
        title = "Заявка",
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
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            when {
                loading && order == null -> CircularProgressIndicator()
                error != null && order == null -> AppText(text = error!!)
                order != null -> {
                    val wo = order!!
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                    ) {
                        AppStatusChip(
                            text = workOrderTypeLabelRu(wo.type),
                            tone =
                                if (wo.type == "emergency") {
                                    AppStatusChipTone.Accent
                                } else {
                                    AppStatusChipTone.Muted
                                },
                        )
                        AppStatusChip(
                            text = workOrderStatusLabelRu(wo.status),
                            tone =
                                when (wo.status) {
                                    "new" -> AppStatusChipTone.Accent
                                    "in_progress" -> AppStatusChipTone.Neutral
                                    else -> AppStatusChipTone.Muted
                                },
                        )
                    }
                    AppText(text = formatWorkOrderDisplayTitle(wo.title), style = AppTextStyle.Title)
                    wo.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        AppText(text = "Описание", style = AppTextStyle.Label)
                        AppText(text = desc)
                    }
                    if (wo.attachmentIds.isNotEmpty() || attachmentsRepository != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                            AppText(text = "Фото", style = AppTextStyle.Label)
                            if (wo.attachmentIds.isEmpty()) {
                                AppText(text = "Нет фото")
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                                ) {
                                    wo.attachmentIds.forEachIndexed { index, _ ->
                                        AppText(text = "Фото ${index + 1}", style = AppTextStyle.Body)
                                    }
                                }
                            }
                            if (attachmentsRepository != null && allowMediaMutations && wo.attachmentIds.size < 10) {
                                AppButton(
                                    text = if (uploadingPhoto) "Загрузка…" else "Добавить фото",
                                    onClick = {
                                        photoSourceDialogOpen = togglePhotoSourceActions(photoSourceDialogOpen)
                                    },
                                    enabled = !uploadingPhoto,
                                    variant = AppButtonVariant.Secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (photoSourceDialogOpen) {
                                    PhotoSourceDialog(
                                        onDismiss = { photoSourceDialogOpen = false },
                                        onFromDisk = {
                                            pickingCommentPhoto = false
                                            imagePickers.openGallery()
                                        },
                                        onCamera = {
                                            pickingCommentPhoto = false
                                            imagePickers.openCamera()
                                        },
                                    )
                                }
                            }
                        }
                    }
                    commentsRepository?.let { commentsRepo ->
                        WorkOrderCommentsSection(
                            comments = comments,
                            loading = commentsLoading,
                            users = directoryUsers,
                            currentUserId = currentUserId,
                            composeEnabled = allowMediaMutations,
                            draft = commentDraft,
                            pendingPhoto = pendingCommentPhoto,
                            sending = sendingComment,
                            onDraftChange = { commentDraft = it.take(2000) },
                            onRemovePhoto = { pendingCommentPhoto = null },
                            onAddPhoto = {
                                commentPhotoSourceDialogOpen = togglePhotoSourceActions(commentPhotoSourceDialogOpen)
                            },
                            onSubmit = {
                                if (!canSubmitWorkOrderComment(commentDraft, sendingComment)) return@WorkOrderCommentsSection
                                scope.launch {
                                    sendingComment = true
                                    error = null
                                    try {
                                        val attachmentId =
                                            pendingCommentPhoto?.let { photo ->
                                                val attachments = attachmentsRepository
                                                    ?: error("Не удалось добавить фото")
                                                attachments.upload(
                                                    bytes = photo.bytes,
                                                    filename = photo.fileName,
                                                    contentType = photo.contentType,
                                                ).id
                                            }
                                        commentsRepo.create(
                                            CreateWorkOrderCommentRequest(
                                                workOrderId = wo.id,
                                                text = commentDraft.trim(),
                                                attachmentId = attachmentId,
                                            ),
                                        )
                                        commentDraft = ""
                                        pendingCommentPhoto = null
                                        comments = commentsRepo.list(wo.id)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        error = e.message ?: "Не удалось отправить комментарий"
                                    } finally {
                                        sendingComment = false
                                    }
                                }
                            },
                        )
                        if (commentPhotoSourceDialogOpen) {
                            PhotoSourceDialog(
                                onDismiss = { commentPhotoSourceDialogOpen = false },
                                onFromDisk = {
                                    pickingCommentPhoto = true
                                    imagePickers.openGallery()
                                },
                                onCamera = {
                                    pickingCommentPhoto = true
                                    imagePickers.openCamera()
                                },
                            )
                        }
                    }
                    if (wo.status == "closed" || readOnly) {
                        DetailRow("Длительность, ч", wo.durationHours.toString())
                    } else {
                        var durationDraft by remember(wo.id, wo.durationHours) {
                            mutableStateOf(wo.durationHours.toString())
                        }
                        AppTextField(
                            value = durationDraft,
                            onValueChange = { durationDraft = it.filter { ch -> ch.isDigit() } },
                            label = "Длительность, ч",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val parsedDuration = durationDraft.toIntOrNull()
                        if (parsedDuration != null && parsedDuration > WorkOrderDuration.MAX_DURATION_HOURS) {
                            AppText(text = "Длительность не может превышать ${WorkOrderDuration.MAX_DURATION_HOURS} ч")
                        }
                        if (
                            parsedDuration != null &&
                            parsedDuration in 1..WorkOrderDuration.MAX_DURATION_HOURS &&
                            parsedDuration != wo.durationHours
                        ) {
                            AppButton(
                                text = if (acting) "…" else "Сохранить длительность",
                                onClick = {
                                    scope.launch {
                                        acting = true
                                        error = null
                                        try {
                                            order = repository.patch(wo.id, durationHours = parsedDuration)
                                            onChanged()
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            acting = false
                                        }
                                    }
                                },
                                enabled = !acting,
                                variant = AppButtonVariant.Secondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    DetailRow("Начало", wo.dueAt)
                    DetailRow("Площадка", resolveSiteName(sites, wo.siteId))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AppText(text = "Оборудование", style = AppTextStyle.Label)
                        AssetNameLink(
                            name = asset?.name,
                            inventoryNo = asset?.inventoryNo,
                            assetId = wo.assetId,
                            onOpen = onOpenEquipment,
                        )
                    }
                    if (warehouseVisible && warehouseRepository != null && compatibleParts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                            AppText(text = "Запчасти", style = AppTextStyle.Label)
                            compatibleParts.forEach { link ->
                                val part = warehouseParts.find { it.id == link.partId } ?: return@forEach
                                Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs)) {
                                    AppText(text = part.name.ifBlank { "Запчасть" })
                                    AppText(text = "В наличии: ${formatWarehouseQty(part.onHand)} ${part.uom.ifBlank { "ед." }}")
                                    if (!readOnly && part.onHand > 0) {
                                        AppButton(
                                            text = "Взять",
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        warehouseRepository.issue(
                                                            StockIssueRequest(
                                                                partId = part.id,
                                                                siteId = wo.siteId,
                                                                qty = 1.0,
                                                                workOrderId = wo.id,
                                                                assetId = wo.assetId,
                                                            ),
                                                        )
                                                        warehouseParts = warehouseRepository.listParts()
                                                    } catch (e: GatewayHttpException) {
                                                        if (e.status == 403) warehouseVisible = false else error = e.message
                                                    } catch (e: Exception) {
                                                        error = e.message ?: "Не удалось выдать запчасть"
                                                    }
                                                }
                                            },
                                            variant = AppButtonVariant.Secondary,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (editableAssignee && onOpenAssigneePick != null) {
                        val assigneeLabel =
                            wo.assigneeId?.takeIf { it.isNotBlank() }?.let { id ->
                                formatAssigneeLabel(id, directoryUsers, currentUserId)
                            } ?: "не назначен"
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            AppText(text = "Исполнитель", style = AppTextStyle.Label)
                            AppButton(
                                text = assigneeLabel,
                                onClick = onOpenAssigneePick,
                                variant =
                                    if (wo.assigneeId.isNullOrBlank()) {
                                        AppButtonVariant.Secondary
                                    } else {
                                        AppButtonVariant.Primary
                                    },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        val assignee =
                            wo.assigneeId?.takeIf { it.isNotBlank() }?.let { id ->
                                formatAssigneeLabel(id, directoryUsers, currentUserId)
                            } ?: "не назначен"
                        DetailRow("Исполнитель", assignee)
                    }
                    val creator =
                        wo.createdBy?.takeIf { it.isNotBlank() }?.let { id ->
                            formatAssigneeLabel(id, directoryUsers, currentUserId)
                        } ?: "не указан"
                    DetailRow("Создатель", creator)
                    if (wo.type == "ppr") {
                        val itemTitle =
                            wo.maintenanceMapItemId
                                ?.takeIf { it.isNotBlank() }
                                ?.let { itemId -> pprMap?.items?.find { it.id == itemId }?.title }
                        val (mapLabel, itemLabel) =
                            resolvePprLabels(
                                mapTitle = pprMap?.title,
                                itemTitle = itemTitle,
                                mapId = wo.maintenanceMapId,
                                itemId = wo.maintenanceMapItemId,
                            )
                        // Skip when value is only the generic fallback (label ≡ value).
                        if (mapLabel != "ППР" && mapLabel != "—") {
                            DetailRow("ППР", mapLabel)
                        }
                        if (itemLabel != "Пункт ППР" && itemLabel != "—") {
                            DetailRow("Пункт ППР", itemLabel)
                        }
                    }
                    if (error != null) {
                        AppText(text = error!!)
                    }
                    val openMentor = onOpenMentor
                    val showMentor =
                        openMentor != null &&
                            equipmentRepository != null &&
                            shouldShowWoAssistant(wo.assigneeId, currentUserId)
                    if (showMentor) {
                        AppButton(
                            text = "Наставник",
                            onClick = openMentor,
                            variant = AppButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (!readOnly) {
                        when (wo.status) {
                            "new" ->
                                AppButton(
                                    text = if (acting) "…" else "В работу",
                                    onClick = {
                                        scope.launch {
                                            acting = true
                                            error = null
                                            try {
                                                val location =
                                                    try {
                                                        locationTrackingController
                                                            ?.currentLocation()
                                                            ?.let {
                                                                EngineerLocationSnapshot(
                                                                    lat = it.lat,
                                                                    lon = it.lon,
                                                                    accuracyM = it.accuracyM,
                                                                )
                                                            }
                                                    } catch (e: CancellationException) {
                                                        throw e
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                order =
                                                    repository.patch(
                                                        wo.id,
                                                        status = "in_progress",
                                                        location = location,
                                                    )
                                                locationTrackingController?.onStartedInProgress()
                                                onChanged()
                                            } catch (e: Exception) {
                                                error = e.message
                                            } finally {
                                                acting = false
                                            }
                                        }
                                    },
                                    enabled = !acting,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            "in_progress" ->
                                AppButton(
                                    text = if (acting) "…" else "Закрыть",
                                    onClick = {
                                        scope.launch {
                                            acting = true
                                            error = null
                                            try {
                                                order = repository.patch(wo.id, status = "closed")
                                                onChanged()
                                            } catch (e: Exception) {
                                                error = e.message
                                            } finally {
                                                acting = false
                                            }
                                        }
                                    },
                                    enabled = !acting,
                                    variant = AppButtonVariant.Secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                        }
                    }
                }
            }
        }
    }
}

internal fun findAssetById(
    assets: List<AssetDto>,
    assetId: String,
): AssetDto? = assets.find { it.id == assetId }

internal fun formatAssigneeLabel(
    userId: String,
    users: List<AdminUser>,
    currentUserId: String? = null,
): String {
    if (currentUserId != null && userId == currentUserId) return "Вы"
    val user = users.find { it.id == userId }
    if (user == null) return "Пользователь"
    val name = listOf(user.givenName, user.familyName).filter { it.isNotBlank() }.joinToString(" ")
    return when {
        name.isNotBlank() && user.email.isNotBlank() -> "$name · ${user.email}"
        user.email.isNotBlank() -> user.email
        name.isNotBlank() -> name
        else -> "Пользователь"
    }
}

internal fun formatAssigneeShortLabel(
    userId: String,
    users: List<AdminUser>,
    currentUserId: String? = null,
): String {
    if (currentUserId != null && userId == currentUserId) return "Вы"
    val user = users.find { it.id == userId } ?: return "Пользователь"
    val name = listOf(user.givenName, user.familyName).filter { it.isNotBlank() }.joinToString(" ")
    return when {
        name.isNotBlank() -> name
        user.email.isNotBlank() -> user.email
        else -> "Пользователь"
    }
}

private val trailingUuid =
    Regex(
        """\s*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\s*$""",
    )
private val trailingLongNumericId = Regex("""\s+\d{10,}\s*$""")
private val onlyUuid =
    Regex(
        """^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$""",
    )
private val onlyLongNumericId = Regex("""^\d{10,}$""")

/**
 * Human work-order title for UI cards — strips trailing opaque ids/timestamps,
 * never shows a bare UUID/snowflake as the label.
 */
internal fun formatWorkOrderDisplayTitle(title: String): String {
    var text = title.trim()
    text = text.replace(trailingUuid, "").trim()
    text = text.replace(trailingLongNumericId, "").trim()
    if (text.isBlank() || onlyUuid.matches(text) || onlyLongNumericId.matches(text)) {
        return "Заявка"
    }
    return text
}

internal fun assigneeInitials(
    userId: String,
    users: List<AdminUser>,
    currentUserId: String? = null,
): String {
    val user = users.find { it.id == userId } ?: return "?"
    val nameParts = listOf(user.givenName, user.familyName).filter { it.isNotBlank() }
    val initialsFromName =
        nameParts
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .take(2)
    return when {
        initialsFromName.isNotBlank() -> initialsFromName
        user.email.isNotBlank() -> user.email.first().uppercaseChar().toString()
        currentUserId != null && userId == currentUserId -> "В"
        else -> "?"
    }
}

/** Human-readable ППР labels — never raw map/item ids. */
internal fun resolvePprLabels(
    mapTitle: String?,
    itemTitle: String?,
    mapId: String?,
    itemId: String?,
): Pair<String, String> {
    val mapLabel =
        mapTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (mapId.isNullOrBlank()) "—" else "ППР"
    val itemLabel =
        itemTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (itemId.isNullOrBlank()) "—" else "Пункт ППР"
    return mapLabel to itemLabel
}

/**
 * When admin user directory is available, keep only known users with `engineer`.
 * Unknown ids (not in [users]) are dropped — they cannot be labeled and PATCH rejects
 * non-engineers / out-of-org scope ghosts.
 * If [users] is empty (no admin access), return candidates unchanged (server still enforces).
 */
internal fun filterEngineerEligibleAssignees(
    candidates: List<String>,
    users: List<AdminUser>,
): List<String> {
    if (users.isEmpty()) return candidates
    val byId = users.associateBy { it.id }
    return candidates.filter { id ->
        val user = byId[id] ?: return@filter false
        "engineer" in user.features
    }
}

internal fun canSubmitWorkOrderComment(
    text: String,
    sending: Boolean,
): Boolean = text.isNotBlank() && !sending

internal fun shouldShowWorkOrderCommentComposer(composeEnabled: Boolean): Boolean = composeEnabled

@Composable
private fun WorkOrderCommentsSection(
    comments: List<WorkOrderCommentDto>,
    loading: Boolean,
    users: List<AdminUser>,
    currentUserId: String?,
    composeEnabled: Boolean,
    draft: String,
    pendingPhoto: PickedImage?,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onRemovePhoto: () -> Unit,
    onAddPhoto: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
        AppText(text = "Комментарии", style = AppTextStyle.Label)
        when {
            loading -> CircularProgressIndicator()
            comments.isEmpty() -> AppText(text = "Нет комментариев")
            else ->
                comments.forEach { comment ->
                    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs)) {
                        AppText(
                            text = "${formatAssigneeLabel(comment.authorId, users, currentUserId)} · ${comment.createdAt}",
                            style = AppTextStyle.Label,
                        )
                        AppText(text = comment.text)
                        if (comment.attachmentId != null) {
                            AppText(text = "Фото", style = AppTextStyle.Label)
                        }
                    }
                }
        }
        if (shouldShowWorkOrderCommentComposer(composeEnabled)) {
            AppTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = "Комментарий",
                singleLine = false,
                enabled = !sending,
                modifier = Modifier.fillMaxWidth(),
            )
            pendingPhoto?.let { photo ->
                Box(modifier = Modifier.size(96.dp)) {
                    val bitmap = remember(photo) { decodePickedImage(photo.bytes) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Фото",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).padding(ClientSpacing.xs),
                        )
                    } else {
                        AppText(text = "Фото", modifier = Modifier.padding(ClientSpacing.sm))
                    }
                    IconButton(onClick = onRemovePhoto, enabled = !sending) {
                        AppText(text = "×")
                    }
                }
            }
            AppButton(
                text = "Добавить фото",
                variant = AppButtonVariant.Secondary,
                onClick = onAddPhoto,
                enabled = !sending,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = if (sending) "Отправка…" else "Отправить",
                onClick = onSubmit,
                enabled = canSubmitWorkOrderComment(draft, sending),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(text = label, style = AppTextStyle.Label)
        AppText(text = value, style = AppTextStyle.Body)
    }
}
