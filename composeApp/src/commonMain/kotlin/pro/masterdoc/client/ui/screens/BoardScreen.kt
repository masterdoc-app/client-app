package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import pro.masterdoc.client.auth.BoardWeekDto
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.WeekClip
import pro.masterdoc.client.auth.WorkOrderDto
import pro.masterdoc.client.auth.WorkOrderDuration
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.UserScopesRepository
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppIcon
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.localEpochDay

internal data class BoardLaneItem(
    val order: WorkOrderDto,
    val clip: WeekClip,
    val lane: Int,
)

internal fun assignLanes(items: List<Pair<WorkOrderDto, WeekClip>>): List<BoardLaneItem> {
    val sorted =
        items.sortedWith(
            compareBy<Pair<WorkOrderDto, WeekClip>>(
                { it.second.startColumn },
                { -it.second.spanColumns },
                { it.first.id },
            ),
        )
    val laneEnds = mutableListOf<Int>()
    return sorted.map { (order, clip) ->
        val end = clip.startColumn + clip.spanColumns
        val reusableLane = laneEnds.indexOfFirst { it <= clip.startColumn }
        val lane =
            if (reusableLane < 0) {
                laneEnds.add(end)
                laneEnds.lastIndex
            } else {
                laneEnds[reusableLane] = end
                reusableLane
            }
        BoardLaneItem(order = order, clip = clip, lane = lane)
    }
}

@Composable
fun BoardScreen(
    repository: WorkOrdersRepository,
    userScopesRepository: UserScopesRepository? = null,
    equipmentRepository: EquipmentRepository? = null,
    adminUsersRepository: AdminUsersRepository? = null,
    hasAdminUsers: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var weeks by remember { mutableStateOf<List<BoardWeekDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showScopeEditor by remember { mutableStateOf(false) }
    var weekMonday by remember { mutableStateOf(currentMondayIso()) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(repository, weekMonday, reloadKey) {
        loading = true
        error = null
        weeks = emptyList()
        try {
            weeks = repository.getBoard(weekStart = weekMonday, weeks = 1).weeks
            loading = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: GatewayHttpException) {
            error = e.message ?: "Ошибка загрузки доски"
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки доски"
            loading = false
        }
    }

    val detailId = selectedId
    if (detailId != null) {
        WorkOrderDetailScreen(
            repository = repository,
            orderId = detailId,
            onBack = { selectedId = null },
            onChanged = { reloadKey++ },
            modifier = modifier,
        )
        return
    }

    if (showScopeEditor && userScopesRepository != null && equipmentRepository != null) {
        val recentAssignees =
            weeks
                .flatMap { it.items }
                .mapNotNull { it.assigneeId?.takeIf { id -> id.isNotBlank() } }
                .distinct()
                .take(8)
        EngineerScopeScreen(
            userScopesRepository = userScopesRepository,
            equipmentRepository = equipmentRepository,
            adminUsersRepository = adminUsersRepository,
            hasAdminUsers = hasAdminUsers,
            recentAssigneeIds = recentAssignees,
            onBack = { showScopeEditor = false },
            modifier = modifier,
        )
        return
    }

    AppScaffold(title = "Доска", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            when {
                loading && weeks.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null && weeks.isEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                        AppText(text = error!!)
                        AppButton(text = "Повторить", onClick = { reloadKey++ })
                    }
                }
                else -> {
                    if (error != null) {
                        AppText(text = error!!)
                    }
                    if (userScopesRepository != null && equipmentRepository != null) {
                        AppButton(
                            text = "Привязка инженеров",
                            variant = AppButtonVariant.Secondary,
                            onClick = { showScopeEditor = true },
                        )
                    }
                    WeekNavigation(
                        weekMonday = weekMonday,
                        onPrevious = { weekMonday = shiftWeek(weekMonday, -7) },
                        onNext = { weekMonday = shiftWeek(weekMonday, 7) },
                    )
                    val items = weeks.firstOrNull()?.items.orEmpty()
                    val laneItems =
                        assignLanes(
                            items.mapNotNull { order ->
                                val occupied = WorkOrderDuration.occupiedDates(order.dueAt, order.durationHours)
                                val clip = WorkOrderDuration.clipToWeek(occupied, weekMonday)
                                clip?.let { order to it }
                            },
                        )
                    WeekGrid(
                        weekMonday = weekMonday,
                        laneItems = laneItems,
                        onOpen = { selectedId = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekNavigation(
    weekMonday: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            AppIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Предыдущая неделя",
            )
        }
        AppText(
            text = "$weekMonday — ${shiftWeek(weekMonday, 6)}",
            style = AppTextStyle.Title,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            AppIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Следующая неделя",
            )
        }
    }
}

@Composable
private fun WeekGrid(
    weekMonday: String,
    laneItems: List<BoardLaneItem>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        DayHeaders(weekMonday)
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(Modifier.fillMaxSize()) {
                repeat(7) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = ClientSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
            ) {
                laneItems
                    .groupBy { it.lane }
                    .toList()
                    .sortedBy { (laneIndex, _) -> laneIndex }
                    .forEach { (_, lane) ->
                        BoardLane(lane.sortedBy { it.clip.startColumn }, onOpen)
                    }
            }
        }
    }
}

@Composable
private fun DayHeaders(weekMonday: String) {
    val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEachIndexed { column, weekday ->
            val date = shiftWeek(weekMonday, column)
            AppText(
                text = "$weekday ${date.takeLast(2)}",
                style = AppTextStyle.Label,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BoardLane(
    items: List<BoardLaneItem>,
    onOpen: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        var nextColumn = 0
        items.forEach { item ->
            val gap = item.clip.startColumn - nextColumn
            if (gap > 0) {
                Spacer(Modifier.weight(gap.toFloat()))
            }
            WorkOrderBoardCard(
                order = item.order,
                onClick = { onOpen(item.order.id) },
                modifier =
                    Modifier
                        .weight(item.clip.spanColumns.toFloat())
                        .padding(horizontal = 2.dp),
            )
            nextColumn = item.clip.startColumn + item.clip.spanColumns
        }
        val trailingColumns = 7 - nextColumn
        if (trailingColumns > 0) {
            Spacer(Modifier.weight(trailingColumns.toFloat()))
        }
    }
}

private fun currentMondayIso(): String {
    return mondayIsoForEpochDay(localEpochDay())
}

internal fun mondayIsoForEpochDay(epochDay: Long): String {
    val daysSinceMonday = IsoDates.dayOfWeekIso(epochDay) - 1
    return IsoDates.formatEpochDay(epochDay - daysSinceMonday)
}

private fun shiftWeek(
    isoDate: String,
    days: Int,
): String {
    val epochDay = IsoDates.parseToEpochDay(isoDate) ?: return isoDate
    return IsoDates.formatEpochDay(epochDay + days)
}
