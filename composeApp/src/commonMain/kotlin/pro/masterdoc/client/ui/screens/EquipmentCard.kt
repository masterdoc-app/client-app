package pro.masterdoc.client.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.DocumentMetaDto
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppIcon
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

/**
 * Карточка единицы оборудования — scannable CMMS-style layout on Fixaverse Lite DS.
 *
 * Hierarchy (Limble/MaintainX/Fiix pattern): status → identity → description →
 * passport tiles → documents → linked PPR → draft actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentCard(
    asset: AssetDto,
    siteName: String? = null,
    moveTargets: List<Pair<String, String>> = emptyList(),
    linkedMap: MaintenanceMapDto? = null,
    documents: List<DocumentMetaDto> = emptyList(),
    folderDocuments: List<DocumentMetaDto> = emptyList(),
    acting: Boolean = false,
    onConfirm: ((name: String, inventoryNo: String) -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onMove: ((String) -> Unit)? = null,
    onOpenLinkedPpr: ((MaintenanceMapDto) -> Unit)? = null,
    onOpenStorageFolder: ((String) -> Unit)? = null,
    onOpenDocument: ((DocumentMetaDto) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val siteLabel = siteDisplayName(siteName)
    val isDraft = asset.status == "draft"
    var draftName by remember(asset.id, asset.name) { mutableStateOf(asset.name) }
    var draftInventoryNo by remember(asset.id, asset.inventoryNo) {
        mutableStateOf(asset.inventoryNo.orEmpty())
    }
    val storageFolder =
        documents.firstOrNull()?.storageFolder()
            ?: folderDocuments.firstOrNull()?.storageFolder()
            ?: asset.orgId
    // Equipment keeps a single document; ignore folder siblings on the card.
    val shownDocs = documents.distinctBy { it.id }.take(1)
    val accent =
        if (isDraft) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                .animateContentSize(animationSpec = tween(220)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (isDraft) 2.dp else 0.dp,
        tonalElevation = if (isDraft) 1.dp else 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accent),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(ClientSpacing.md),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                ) {
                    AppStatusChip(
                        text = statusLabel(asset.status),
                        tone =
                            if (isDraft) {
                                AppStatusChipTone.Accent
                            } else {
                                AppStatusChipTone.Neutral
                            },
                    )
                    AppStatusChip(
                        text = sourceLabel(asset.source),
                        tone = AppStatusChipTone.Muted,
                        showDot = false,
                    )
                    AppStatusChip(
                        text = categoryLabel(asset.category),
                        tone = AppStatusChipTone.Muted,
                        showDot = false,
                    )
                }

                if (isDraft) {
                    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                        AppTextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            label = "Название оборудования",
                        )
                        AppTextField(
                            value = draftInventoryNo,
                            onValueChange = { draftInventoryNo = it },
                            label = "Инвентарный номер",
                        )
                        AppText(
                            text = siteLabel,
                            style = AppTextStyle.Label,
                        )
                    }
                } else {
                    IdentityHeader(asset = asset, siteLabel = siteLabel)
                }

                DescriptionBlock(asset = asset, siteName = siteName)

                Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                    SectionLabel("Паспорт")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                    ) {
                        PassportTile(
                            label = "Площадка",
                            value = siteLabel,
                            modifier = Modifier.weight(1f),
                        )
                        PassportTile(
                            label = "Инв. №",
                            value =
                                if (isDraft) {
                                    draftInventoryNo.takeIf { it.isNotBlank() } ?: "не указан"
                                } else {
                                    asset.inventoryNo?.takeIf { it.isNotBlank() } ?: "не указан"
                                },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                DocumentsSection(
                    storageFolder = storageFolder,
                    shownDocs = shownDocs,
                    fallbackIds = asset.documentIds.take(1),
                    onOpenStorageFolder = onOpenStorageFolder,
                    onOpenDocument = onOpenDocument,
                )

                if (linkedMap != null) {
                    LinkedPprBlock(
                        linkedMap = linkedMap,
                        onOpenLinkedPpr = onOpenLinkedPpr,
                    )
                }

                if (isDraft && onConfirm != null && onReject != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                        AppButton(
                            text = if (acting) "…" else "В базу",
                            enabled = !acting && draftName.isNotBlank(),
                            onClick = { onConfirm(draftName.trim(), draftInventoryNo.trim()) },
                            variant = AppButtonVariant.Primary,
                            fillMaxWidth = false,
                            modifier = Modifier.weight(1f),
                        )
                        AppButton(
                            text = "Отклонить",
                            enabled = !acting,
                            onClick = onReject,
                            variant = AppButtonVariant.Secondary,
                            fillMaxWidth = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (!isDraft && onDelete != null) {
                    AppButton(
                        text = if (acting) "…" else "Удалить",
                        enabled = !acting,
                        onClick = onDelete,
                        variant = AppButtonVariant.Secondary,
                        fillMaxWidth = false,
                    )
                }

                if (onMove != null && moveTargets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs)) {
                        SectionLabel("Перенести")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                        ) {
                            moveTargets.forEach { (id, label) ->
                                AppButton(
                                    text = label,
                                    enabled = !acting,
                                    fillMaxWidth = false,
                                    variant = AppButtonVariant.Secondary,
                                    onClick = { onMove(id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityHeader(asset: AssetDto, siteLabel: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                imageVector = categoryIcon(asset.category),
                contentDescription = categoryLabel(asset.category),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(text = asset.name, style = AppTextStyle.Title, maxLines = 2)
            val inv = asset.inventoryNo?.takeIf { it.isNotBlank() }
            AppText(
                text =
                    if (inv != null) {
                        "Инв. № $inv · $siteLabel"
                    } else {
                        siteLabel
                    },
                style = AppTextStyle.Label,
            )
        }
    }
}

@Composable
private fun DescriptionBlock(
    asset: AssetDto,
    siteName: String?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(ClientSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
    ) {
        AppText(text = "Что это", style = AppTextStyle.Label)
        AppText(
            text = asset.description?.takeIf { it.isNotBlank() } ?: fallbackDescription(asset, siteName),
            style = AppTextStyle.Body,
        )
    }
}

@Composable
private fun PassportTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .widthIn(min = 120.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = ClientSpacing.sm, vertical = ClientSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AppText(text = label, style = AppTextStyle.Label)
        AppText(text = value, style = AppTextStyle.Body, maxLines = 2)
    }
}

@Composable
private fun DocumentsSection(
    storageFolder: String,
    shownDocs: List<DocumentMetaDto>,
    fallbackIds: List<String>,
    onOpenStorageFolder: ((String) -> Unit)?,
    onOpenDocument: ((DocumentMetaDto) -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
        SectionLabel("Документы")
        DocRow(
            icon = Icons.Filled.FolderOpen,
            title = "Папка в хранилище",
            subtitle = null,
            onClick = onOpenStorageFolder?.let { { it(storageFolder) } },
        )
        when {
            shownDocs.isNotEmpty() ->
                shownDocs.forEach { doc ->
                    DocRow(
                        icon = Icons.Filled.Description,
                        title = doc.filename,
                        subtitle = doc.contentType.ifBlank { "файл" },
                        onClick = onOpenDocument?.let { { it(doc) } },
                    )
                }
            fallbackIds.isNotEmpty() ->
                fallbackIds.forEach { id ->
                    DocRow(
                        icon = Icons.Filled.Description,
                        title = id,
                        subtitle = "метаданные недоступны",
                        onClick = null,
                    )
                }
            else ->
                AppText(text = "нет привязанных файлов", style = AppTextStyle.Label)
        }
    }
}

@Composable
private fun DocRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = ClientSpacing.sm, vertical = ClientSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = null,
            tint =
                if (onClick != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(
                text = title,
                style = AppTextStyle.Body,
                color =
                    if (onClick != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
            )
            if (!subtitle.isNullOrBlank()) {
                AppText(text = subtitle, style = AppTextStyle.Label, maxLines = 1)
            }
        }
        if (onClick != null) {
            AppIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LinkedPprBlock(
    linkedMap: MaintenanceMapDto,
    onOpenLinkedPpr: ((MaintenanceMapDto) -> Unit)?,
) {
    val openPpr = onOpenLinkedPpr
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.medium)
                .then(
                    if (openPpr != null) {
                        Modifier.clickable { openPpr(linkedMap) }
                    } else {
                        Modifier
                    },
                )
                .padding(ClientSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Связанный ППР")
            AppStatusChip(
                text = mapStatusLabel(linkedMap.status),
                tone =
                    if (linkedMap.status == "draft") {
                        AppStatusChipTone.Accent
                    } else {
                        AppStatusChipTone.Neutral
                    },
                showDot = true,
            )
        }
        AppText(text = linkedMap.title, style = AppTextStyle.Body)
        AppText(
            text = "${linkedMap.items.size} пунктов обслуживания",
            style = AppTextStyle.Label,
        )
        linkedMap.items.take(3).forEach { item ->
            AppText(text = "- ${item.title}", style = AppTextStyle.Label)
        }
        if (linkedMap.items.size > 3) {
            AppText(text = "… ещё ${linkedMap.items.size - 3}", style = AppTextStyle.Label)
        }
        if (openPpr != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppText(
                    text = "Открыть ППР",
                    style = AppTextStyle.Label,
                    color = MaterialTheme.colorScheme.primary,
                )
                AppIcon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    AppText(text = text, style = AppTextStyle.Label)
}

private fun categoryIcon(category: String?): ImageVector =
    when (category) {
        "lifting" -> Icons.Filled.PrecisionManufacturing
        "refrigeration" -> Icons.Filled.AcUnit
        else -> Icons.Filled.Build
    }

internal fun statusLabel(status: String): String =
    when (status) {
        "draft" -> "Черновик"
        "active" -> "В базе"
        else -> status
    }

internal fun sourceLabel(source: String): String =
    when (source) {
        "ai_generated" -> "из руководства (AI)"
        "manual" -> "вручную"
        else -> source
    }

internal fun categoryLabel(category: String?): String =
    when (category) {
        "lifting" -> "Грузоподъёмное"
        "refrigeration" -> "Холодильное"
        "general" -> "Общее"
        null, "" -> "не указана"
        else -> category
    }

internal fun mapStatusLabel(status: String): String =
    when (status) {
        "draft" -> "черновик"
        "active" -> "в базе"
        else -> status
    }

internal fun fallbackDescription(
    asset: AssetDto,
    siteName: String? = null,
): String {
    val kind = categoryLabel(asset.category).lowercase()
    return when (asset.status) {
        "draft" ->
            "Черновик единицы оборудования ($kind). Описание будет уточнено из руководства; " +
                "после подтверждения карточка попадёт в рабочую базу."
        else ->
            "Единица оборудования ($kind) на площадке ${siteDisplayName(siteName)}."
    }
}
