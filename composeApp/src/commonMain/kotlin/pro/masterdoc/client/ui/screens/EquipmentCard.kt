package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.border
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
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

/**
 * Карточка единицы оборудования — контейнер для просмотра и (для draft) подтверждения.
 *
 * Иерархия: статус → название → описание «что это» → метаданные → связанный ППР → действия.
 */
@Composable
fun EquipmentCard(
    asset: AssetDto,
    linkedMap: MaintenanceMapDto? = null,
    acting: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDraft = asset.status == "draft"
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
                MetaRow(
                    "Документы",
                    if (asset.documentIds.isEmpty()) {
                        "нет"
                    } else {
                        "${asset.documentIds.size} шт."
                    },
                )
            }

            if (linkedMap != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppText(text = "Связанный ППР", style = AppTextStyle.Label)
                    AppText(
                        text = "${linkedMap.title} · ${mapStatusLabel(linkedMap.status)} · ${linkedMap.items.size} пунктов",
                        style = AppTextStyle.Body,
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
