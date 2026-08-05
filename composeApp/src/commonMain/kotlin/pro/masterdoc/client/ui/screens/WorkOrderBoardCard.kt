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
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.workOrderStatusLabelRu
import pro.masterdoc.client.auth.workOrderTypeLabelRu
import pro.masterdoc.client.designsystem.components.AppStatusChip
import pro.masterdoc.client.designsystem.components.AppStatusChipTone
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

/** Compact work-order card for the board with status and assignee metadata. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkOrderBoardCard(
    order: WorkOrderDto,
    onClick: () -> Unit,
    users: List<AdminUser> = emptyList(),
    currentUserId: String? = null,
    modifier: Modifier = Modifier,
) {
    val accent =
        when (order.type) {
            "emergency" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(
                modifier = Modifier.padding(ClientSpacing.sm).weight(1f),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
            ) {
                AppText(
                    text = order.title,
                    style = AppTextStyle.Body,
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
                        AppStatusChip(
                            text = workOrderTypeLabelRu(order.type),
                            tone =
                                if (order.type == "emergency") {
                                    AppStatusChipTone.Accent
                                } else {
                                    AppStatusChipTone.Muted
                                },
                            showDot = false,
                        )
                        AppStatusChip(
                            text = workOrderStatusLabelRu(order.status),
                            tone =
                                when (order.status) {
                                    "new" -> AppStatusChipTone.Accent
                                    "in_progress" -> AppStatusChipTone.Neutral
                                    else -> AppStatusChipTone.Muted
                                },
                            showDot = false,
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
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (assigned) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainer
                                        },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppText(
                                text = initials,
                                style = AppTextStyle.Label,
                                color =
                                    if (assigned) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                maxLines = 1,
                            )
                        }
                        AppText(
                            text = label,
                            modifier = Modifier.weight(1f, fill = false),
                            style = AppTextStyle.Label,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
