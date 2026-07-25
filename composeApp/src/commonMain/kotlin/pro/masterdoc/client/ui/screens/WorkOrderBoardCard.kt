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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.workOrderStatusLabelRu
import pro.masterdoc.client.auth.workOrderTypeLabelRu
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

/**
 * Compact work-order card for a weekly board column.
 * Pattern mirrors [EquipmentCard]: Surface + accent strip + chips + title.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkOrderBoardCard(
    order: WorkOrderDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpen = order.status != "closed"
    val accent =
        when (order.type) {
            "emergency" -> MaterialTheme.colorScheme.error
            else ->
                if (isOpen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
        }
    val statusTone =
        when (order.status) {
            "new" -> AppStatusChipTone.Accent
            "in_progress" -> AppStatusChipTone.Neutral
            else -> AppStatusChipTone.Muted
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (order.status == "new") 2.dp else 0.dp,
        tonalElevation = if (order.status == "new") 1.dp else 0.dp,
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
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                ) {
                    AppStatusChip(
                        text = workOrderTypeLabelRu(order.type),
                        tone =
                            if (order.type == "emergency") {
                                AppStatusChipTone.Accent
                            } else {
                                AppStatusChipTone.Muted
                            },
                    )
                    AppStatusChip(
                        text = workOrderStatusLabelRu(order.status),
                        tone = statusTone,
                    )
                }
                AppText(
                    text = order.title,
                    style = AppTextStyle.Title,
                )
                AppText(
                    text = "Срок: ${order.dueAt}",
                    style = AppTextStyle.Label,
                )
                if (order.assigneeId != null) {
                    AppText(
                        text = "Исполнитель: ${order.assigneeId}",
                        style = AppTextStyle.Label,
                    )
                }
            }
        }
    }
}
