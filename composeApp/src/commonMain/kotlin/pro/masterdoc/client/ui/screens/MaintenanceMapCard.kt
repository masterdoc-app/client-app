package pro.masterdoc.client.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
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
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppIcon
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

data class MaintenanceMapSourceDoc(
    val id: String,
    val filename: String,
    val contentType: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceMapCard(
    map: MaintenanceMapDto,
    asset: AssetDto?,
    sourceDocs: List<MaintenanceMapSourceDoc> = emptyList(),
    highlighted: Boolean = false,
    acting: Boolean = false,
    onOpenDocument: ((MaintenanceMapSourceDoc) -> Unit)? = null,
    onOpenEquipment: (String) -> Unit = {},
    onConfirm: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDraft = map.status == "draft"
    var itemsExpanded by remember(map.id) { mutableStateOf(false) }
    val visibleItems =
        visibleMapItems(map.items, expanded = itemsExpanded, previewLimit = MAP_ITEMS_PREVIEW_LIMIT)
    val overflowLabel =
        mapItemsOverflowLabel(
            total = map.items.size,
            previewLimit = MAP_ITEMS_PREVIEW_LIMIT,
            expanded = itemsExpanded,
        )
    val accent =
        if (highlighted || isDraft) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
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
                        text = pprStatusChipLabel(map.status),
                        tone =
                            if (isDraft) {
                                AppStatusChipTone.Accent
                            } else {
                                AppStatusChipTone.Neutral
                            },
                    )
                    AppStatusChip(
                        text = pprSourceChipLabel(map.source),
                        tone = AppStatusChipTone.Muted,
                        showDot = false,
                    )
                }

                AppText(text = map.title, style = AppTextStyle.Title, maxLines = 3)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MaintenanceMapSectionLabel("Оборудование")
                    AssetNameLink(
                        name = asset?.name,
                        inventoryNo = asset?.inventoryNo,
                        assetId = map.assetId,
                        onOpen = onOpenEquipment,
                    )
                }

                if (sourceDocs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                        MaintenanceMapSectionLabel("Документ")
                        sourceDocs.forEach { doc ->
                            MaintenanceMapDocRow(
                                icon = Icons.Filled.Description,
                                title = doc.filename,
                                subtitle = doc.contentType.ifBlank { null },
                                onClick = onOpenDocument?.let { open -> { open(doc) } },
                            )
                        }
                    }
                }

                if (map.items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs)) {
                        MaintenanceMapSectionLabel("Пункты")
                        visibleItems.forEach { item ->
                            AppText(
                                text =
                                    "- ${item.title} (${ruKind(item.kind)}, каждые ${item.interval.every} ${ruIntervalUnit(item.interval.every, item.interval.unit)})",
                                style = AppTextStyle.Label,
                            )
                        }
                        if (overflowLabel != null) {
                            AppText(
                                text = overflowLabel,
                                style = AppTextStyle.Label,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { itemsExpanded = !itemsExpanded },
                            )
                        }
                    }
                }

                if (onConfirm != null && onReject != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                        AppButton(
                            text = if (acting) "…" else "Подтвердить",
                            enabled = !acting,
                            onClick = onConfirm,
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
            }
        }
    }
}

@Composable
private fun MaintenanceMapSectionLabel(text: String) {
    AppText(text = text, style = AppTextStyle.Label)
}

@Composable
private fun MaintenanceMapDocRow(
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
