package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.DocumentMetaDto
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

/**
 * Карточка единицы оборудования — контейнер для просмотра и (для draft) подтверждения.
 *
 * Иерархия: статус → название → описание «что это» → метаданные → документы → связанный ППР → действия.
 */
@Composable
fun EquipmentCard(
    asset: AssetDto,
    linkedMap: MaintenanceMapDto? = null,
    documents: List<DocumentMetaDto> = emptyList(),
    folderDocuments: List<DocumentMetaDto> = emptyList(),
    acting: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onOpenLinkedPpr: ((MaintenanceMapDto) -> Unit)? = null,
    onOpenStorageFolder: ((String) -> Unit)? = null,
    onOpenDocument: ((DocumentMetaDto) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDraft = asset.status == "draft"
    val storageFolder =
        documents.firstOrNull()?.storageFolder()
            ?: folderDocuments.firstOrNull()?.storageFolder()
            ?: asset.orgId
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = statusLabel(asset.status),
                    style = AppTextStyle.Label,
                    color =
                        if (isDraft) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                AppText(text = sourceLabel(asset.source), style = AppTextStyle.Label)
            }

            AppText(text = asset.name, style = AppTextStyle.Title)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(text = "Что это", style = AppTextStyle.Label)
                AppText(
                    text = asset.description?.takeIf { it.isNotBlank() } ?: fallbackDescription(asset),
                    style = AppTextStyle.Body,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(text = "Паспорт", style = AppTextStyle.Label)
                MetaRow("Категория", categoryLabel(asset.category))
                MetaRow("Площадка", asset.siteId)
                MetaRow("Инв. №", asset.inventoryNo?.takeIf { it.isNotBlank() } ?: "не указан")
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(text = "Документы", style = AppTextStyle.Label)
                LinkRow(
                    label = "Папка в хранилище",
                    value = "$storageFolder/",
                    onClick = onOpenStorageFolder?.let { { it(storageFolder) } },
                )
                when {
                    documents.isNotEmpty() || folderDocuments.isNotEmpty() -> {
                        val shown = (documents + folderDocuments).distinctBy { it.id }
                        shown.forEach { doc ->
                            LinkRow(
                                label = "Файл",
                                value = doc.filename,
                                onClick = onOpenDocument?.let { { it(doc) } },
                            )
                        }
                    }
                    asset.documentIds.isNotEmpty() ->
                        asset.documentIds.forEach { id ->
                            AppText(
                                text = "• $id (метаданные недоступны)",
                                style = AppTextStyle.Label,
                            )
                        }
                    else -> AppText(text = "нет привязанных файлов", style = AppTextStyle.Label)
                }
            }

            if (linkedMap != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppText(text = "Связанный ППР", style = AppTextStyle.Label)
                    val openPpr = onOpenLinkedPpr
                    AppText(
                        text = "${linkedMap.title} · ${mapStatusLabel(linkedMap.status)} · ${linkedMap.items.size} пунктов",
                        style = AppTextStyle.Body,
                        color =
                            if (openPpr != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                ColorUnspecified
                            },
                        modifier =
                            if (openPpr != null) {
                                Modifier.clickable { openPpr(linkedMap) }
                            } else {
                                Modifier
                            },
                    )
                    linkedMap.items.take(3).forEach { item ->
                        AppText(
                            text = "• ${item.title}",
                            style = AppTextStyle.Label,
                        )
                    }
                    if (linkedMap.items.size > 3) {
                        AppText(
                            text = "… ещё ${linkedMap.items.size - 3}",
                            style = AppTextStyle.Label,
                        )
                    }
                    if (openPpr != null) {
                        AppText(
                            text = "Открыть ППР →",
                            style = AppTextStyle.Label,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { openPpr(linkedMap) },
                        )
                    }
                }
            }

            if (isDraft && onConfirm != null && onReject != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = if (acting) "…" else "В базу",
                        enabled = !acting,
                        onClick = onConfirm,
                    )
                    AppButton(
                        text = "Отклонить",
                        enabled = !acting,
                        onClick = onReject,
                    )
                }
            }
        }
    }
}

private val ColorUnspecified = androidx.compose.ui.graphics.Color.Unspecified

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text = label,
            style = AppTextStyle.Label,
            modifier = Modifier.weight(0.35f),
        )
        AppText(
            text = value,
            style = AppTextStyle.Body,
            modifier = Modifier.weight(0.65f),
        )
    }
}

@Composable
private fun LinkRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text = label,
            style = AppTextStyle.Label,
            modifier = Modifier.weight(0.35f),
        )
        AppText(
            text = value,
            style = AppTextStyle.Body,
            color =
                if (onClick != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    ColorUnspecified
                },
            modifier =
                Modifier
                    .weight(0.65f)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        )
    }
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

internal fun fallbackDescription(asset: AssetDto): String {
    val kind = categoryLabel(asset.category).lowercase()
    return when (asset.status) {
        "draft" ->
            "Черновик единицы оборудования ($kind). Описание будет уточнено из руководства; " +
                "после подтверждения карточка попадёт в рабочую базу."
        else ->
            "Единица оборудования ($kind) на площадке ${asset.siteId}."
    }
}
