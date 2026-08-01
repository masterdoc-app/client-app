package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import pro.masterdoc.client.auth.AssetDto
import pro.masterdoc.client.auth.DowntimeIntervalDto
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.localEpochDay

private const val TimelineDayWidth = 20
private const val EquipmentLabelWidth = 148
private val ClosedBarColor = Color(0xFF4F8A75)
private val OpenBarColor = Color(0xFFD47A4A)

internal data class DowntimeRow(
    val assetId: String,
    val label: String,
    val intervals: List<DowntimeSegment>,
)

internal data class DowntimeSegment(
    val startDay: Long,
    val endDay: Long,
    val open: Boolean,
)

internal fun buildDowntimeRows(
    intervals: List<DowntimeIntervalDto>,
    assets: List<AssetDto>,
    fromDay: Long,
    toDay: Long,
): List<DowntimeRow> {
    val names = assets.associate { it.id to (it.name.ifBlank { it.inventoryNo ?: it.id }) }
    return intervals
        .mapNotNull { interval ->
            val start = IsoDates.parseToEpochDay(interval.startedAt.take(10)) ?: return@mapNotNull null
            val end =
                (interval.closedAt?.take(10)?.let(IsoDates::parseToEpochDay) ?: toDay)
                    .coerceAtLeast(start)
            val clippedStart = start.coerceAtLeast(fromDay)
            val clippedEnd = end.coerceAtMost(toDay)
            if (clippedStart > clippedEnd) null
            else {
                interval.assetId to
                    DowntimeSegment(
                        startDay = clippedStart,
                        endDay = clippedEnd,
                        open = interval.closedAt == null || interval.status.equals("in_progress", ignoreCase = true),
                    )
            }
        }
        .groupBy({ it.first }, { it.second })
        .map { (assetId, segments) ->
            DowntimeRow(
                assetId = assetId,
                label = names[assetId] ?: assetId,
                intervals = segments.sortedBy { it.startDay },
            )
        }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun ReportsScreen(
    reportsRepository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    modifier: Modifier = Modifier,
) {
    var days by remember { mutableStateOf(30) }
    var rows by remember { mutableStateOf<List<DowntimeRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val today = localEpochDay()
    val fromDay = today - days + 1

    LaunchedEffect(reportsRepository, equipmentRepository, days) {
        loading = true
        error = null
        try {
            val assets = equipmentRepository.listAssets().items
            val intervals =
                reportsRepository.equipmentDowntime(
                    from = IsoDates.formatEpochDay(fromDay),
                    to = IsoDates.formatEpochDay(today),
                )
            rows = buildDowntimeRows(intervals, assets, fromDay, today)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            error = e.message ?: "Не удалось загрузить отчёт"
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить отчёт"
        } finally {
            loading = false
        }
    }

    AppScaffold(title = "Отчёты", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            AppText(text = "Простои оборудования", style = AppTextStyle.Title)
            PeriodSelector(selected = days, onSelected = { days = it })
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> AppText(text = error!!)
                rows.isEmpty() -> EmptyReportsState()
                else -> DowntimeTimeline(rows = rows, fromDay = fromDay, toDay = today)
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
        listOf(7, 30, 90).forEach { period ->
            AppButton(
                text = "$period дней",
                onClick = { onSelected(period) },
                variant = if (selected == period) AppButtonVariant.Primary else AppButtonVariant.Secondary,
                fillMaxWidth = false,
            )
        }
    }
}

@Composable
private fun EmptyReportsState() {
    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs)) {
        AppText(text = "Нет простоев за выбранный период", style = AppTextStyle.Label)
        AppText(text = "Когда появится ремонт, его интервал будет показан на шкале.", style = AppTextStyle.Label)
    }
}

@Composable
private fun DowntimeTimeline(rows: List<DowntimeRow>, fromDay: Long, toDay: Long) {
    val scrollState = rememberScrollState()
    val days = (toDay - fromDay + 1).toInt()
    val timelineWidth = (days * TimelineDayWidth).dp
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            verticalAlignment = Alignment.Bottom,
        ) {
            Spacer(Modifier.width(EquipmentLabelWidth.dp))
            Box(Modifier.width(timelineWidth).height(30.dp)) {
                (0 until days step labelStep(days)).forEach { index ->
                    AppText(
                        text = shortDate(IsoDates.formatEpochDay(fromDay + index)),
                        style = AppTextStyle.Label,
                        modifier = Modifier.padding(start = (index * TimelineDayWidth).dp),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            Column(modifier = Modifier.width(EquipmentLabelWidth.dp)) {
                rows.forEach { row ->
                    Box(Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        AppText(text = row.label, style = AppTextStyle.Label)
                    }
                }
            }
            Column(modifier = Modifier.width(timelineWidth)) {
                rows.forEach { row ->
                    TimelineRow(row = row, fromDay = fromDay, days = days)
                }
            }
        }
        Spacer(Modifier.height(ClientSpacing.sm))
        Legend()
    }
}

private fun labelStep(days: Int): Int = when {
    days <= 14 -> 2
    days <= 45 -> 7
    else -> 14
}

private fun shortDate(iso: String): String = "${iso.substring(8, 10)}.${iso.substring(5, 7)}"

@Composable
private fun TimelineRow(row: DowntimeRow, fromDay: Long, days: Int) {
    Box(
        modifier =
            Modifier.height(48.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        row.intervals.forEach { segment ->
            val start = (segment.startDay - fromDay).toInt().coerceIn(0, days)
            val width = (segment.endDay - segment.startDay + 1).toInt().coerceAtLeast(1)
            Box(
                modifier =
                    Modifier.padding(vertical = 12.dp)
                        .padding(start = (start * TimelineDayWidth).dp)
                        .width((width * TimelineDayWidth).dp)
                        .height(24.dp)
                        .background(if (segment.open) OpenBarColor else ClosedBarColor),
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(color = ClosedBarColor, label = "Закрыт")
        LegendItem(color = OpenBarColor, label = "В работе")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(12.dp).height(12.dp).background(color))
        AppText(text = label, style = AppTextStyle.Label)
    }
}
