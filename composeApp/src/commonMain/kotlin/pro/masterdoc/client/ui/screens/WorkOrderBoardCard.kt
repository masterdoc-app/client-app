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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.workOrderStatusLabelRu
import pro.masterdoc.client.auth.workOrderTypeLabelRu
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

/** Compact work-order card for the board with themed status and assignee metadata. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkOrderBoardCard(
    order: WorkOrderDto,
    onClick: () -> Unit,
    users: List<AdminUser> = emptyList(),
    currentUserId: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val emergency = order.type == "emergency"
    val closed = order.status == "closed"
    val inProgress = order.status == "in_progress"

    val accent =
        when {
            emergency -> colors.error
            else -> colors.primary
        }
    val cardColor =
        when {
            emergency && !closed -> colors.errorContainer
            closed -> colors.tertiaryContainer
            inProgress -> colors.primaryContainer
            else -> colors.surfaceContainerLow
        }
    val borderColor =
        when {
            emergency && !closed -> colors.error.copy(alpha = 0.35f)
            closed -> colors.tertiary.copy(alpha = 0.25f)
            inProgress -> colors.primary.copy(alpha = 0.35f)
            else -> colors.outline
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = cardColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
            Column(
                modifier = Modifier.padding(ClientSpacing.sm).weight(1f),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
            ) {
                AppText(
                    text = order.title,
                    style = AppTextStyle.Body,
                    color = colors.onSurface,
                    maxLines = 2,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                    ) {
                        BoardMetaChip(
                            text = workOrderTypeLabelRu(order.type),
                            container =
                                if (emergency) {
                                    colors.error.copy(alpha = 0.12f)
                                } else {
                                    colors.primaryContainer
                                },
                            content =
                                if (emergency) {
                                    colors.error
                                } else {
                                    colors.onPrimaryContainer
                                },
                            dot = accent,
                        )
                        BoardMetaChip(
                            text = workOrderStatusLabelRu(order.status),
                            container =
                                when (order.status) {
                                    "new" -> colors.primaryContainer
                                    "in_progress" -> colors.secondaryContainer
                                    "closed" -> colors.tertiaryContainer
                                    else -> colors.surfaceContainer
                                },
                            content =
                                when (order.status) {
                                    "new" -> colors.primary
                                    "in_progress" -> colors.onSecondaryContainer
                                    "closed" -> colors.onTertiaryContainer
                                    else -> colors.onSurfaceVariant
                                },
                            dot =
                                when (order.status) {
                                    "new" -> colors.primary
                                    "in_progress" -> colors.primary
                                    "closed" -> colors.tertiary
                                    else -> colors.onSurfaceVariant
                                },
                        )
                    }
                    val assigneeId = order.assigneeId?.takeIf { it.isNotBlank() }
                    val assigned = assigneeId != null
                    val initials =
                        assigneeId?.let { assigneeInitials(it, users, currentUserId) } ?: "?"
                    val label =
                        assigneeId?.let { formatAssigneeShortLabel(it, users, currentUserId) }
                            ?: "Не назначен"
                    Row(
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .padding(start = ClientSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (assigned) {
                                            if (emergency) {
                                                colors.error.copy(alpha = 0.15f)
                                            } else {
                                                colors.primary
                                            }
                                        } else {
                                            colors.surfaceContainerHighest
                                        },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppText(
                                text = initials,
                                style = AppTextStyle.Label,
                                color =
                                    if (assigned) {
                                        if (emergency) colors.error else colors.onPrimary
                                    } else {
                                        colors.onSurfaceVariant
                                    },
                                maxLines = 1,
                            )
                        }
                        AppText(
                            text = label,
                            modifier = Modifier.weight(1f, fill = false),
                            style = AppTextStyle.Label,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardMetaChip(
    text: String,
    container: Color,
    content: Color,
    dot: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(container)
                .padding(horizontal = ClientSpacing.sm, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dot),
        )
        AppText(text = text, style = AppTextStyle.Label, color = content, maxLines = 1)
    }
}
