package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.DowntimeIntervalDto
import pro.masterdoc.client.auth.EngineerWorkloadReport
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.FailureFrequencyReport
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.KpiTrendsReport
import pro.masterdoc.client.auth.ManagerKpis
import pro.masterdoc.client.auth.ReactiveCompletionReport
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

private sealed interface MarketLeaderReport {
    data class KpiTrends(val value: KpiTrendsReport) : MarketLeaderReport

    data class ReactiveCompletion(val value: ReactiveCompletionReport) : MarketLeaderReport

    data class EngineerWorkload(val value: EngineerWorkloadReport) : MarketLeaderReport

    data class FailureFrequency(val value: FailureFrequencyReport) : MarketLeaderReport
}

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
    val names = assets.associate { it.id to it.displayName() }
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
                label = names[assetId] ?: "Оборудование",
                intervals = segments.sortedBy { it.startDay },
            )
        }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun ReportsScreen(
    reportsRepository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    adminUsersRepository: AdminUsersRepository? = null,
    modifier: Modifier = Modifier,
) {
    var selectedReport by remember { mutableStateOf<ReportId?>(null) }

    selectedReport?.let { reportId ->
        ReportDetailScreen(
            reportId = reportId,
            reportsRepository = reportsRepository,
            equipmentRepository = equipmentRepository,
            adminUsersRepository = adminUsersRepository,
            onBack = { selectedReport = null },
            modifier = modifier,
        )
        return
    }

    AppScaffold(title = "Отчёты", modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            items(reportCatalogItems(), key = { it.id.name }) { item ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedReport = item.id }
                            .padding(vertical = ClientSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                ) {
                    AppText(text = item.title, style = AppTextStyle.Title)
                    AppText(text = item.subtitle, style = AppTextStyle.Label)
                }
            }
        }
    }
}

@Composable
private fun ReportDetailScreen(
    reportId: ReportId,
    reportsRepository: WorkOrdersRepository,
    equipmentRepository: EquipmentRepository,
    adminUsersRepository: AdminUsersRepository?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalogItem = reportCatalogItems().first { it.id == reportId }
    var days by remember { mutableStateOf(30) }
    var rows by remember { mutableStateOf<List<DowntimeRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var kpis by remember { mutableStateOf<ManagerKpis?>(null) }
    var assets by remember { mutableStateOf<List<AssetDto>>(emptyList()) }
    var kpiLoading by remember { mutableStateOf(true) }
    var kpiError by remember { mutableStateOf<String?>(null) }
    var marketLeaderReport by remember { mutableStateOf<MarketLeaderReport?>(null) }
    var marketLeaderLoading by remember { mutableStateOf(false) }
    var marketLeaderError by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    val today = localEpochDay()
    val fromDay = today - days + 1

    LaunchedEffect(reportsRepository, equipmentRepository, days, reportId) {
        loading = true
        error = null
        rows = emptyList()
        kpiLoading = true
        kpiError = null
        kpis = null
        marketLeaderReport = null
        marketLeaderError = null
        marketLeaderLoading = reportId.isMarketLeaderReport()
        val from = IsoDates.formatEpochDay(fromDay)
        val to = IsoDates.formatEpochDay(today)
        val loadedAssets =
            try {
                equipmentRepository.listAssets().items
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Не удалось загрузить оборудование"
                error = message
                emptyList()
            }
        assets = loadedAssets
        if (reportId.isLegacyKpiReport()) {
            try {
                kpis = reportsRepository.managerKpis(from = from, to = to)
            } catch (e: CancellationException) {
                throw e
            } catch (e: GatewayHttpException) {
                kpiError = e.message ?: "Не удалось загрузить KPI"
            } catch (e: Exception) {
                kpiError = e.message ?: "Не удалось загрузить KPI"
            } finally {
                kpiLoading = false
            }
        } else {
            kpiLoading = false
        }
        if (reportId == ReportId.EquipmentDowntime) {
            try {
                val intervals = reportsRepository.equipmentDowntime(from = from, to = to)
                rows = buildDowntimeRows(intervals, loadedAssets, fromDay, today)
            } catch (e: CancellationException) {
                throw e
            } catch (e: GatewayHttpException) {
                error = e.message ?: "Не удалось загрузить отчёт"
            } catch (e: Exception) {
                error = e.message ?: "Не удалось загрузить отчёт"
            } finally {
                loading = false
            }
        } else {
            loading = false
        }
        if (reportId.isMarketLeaderReport()) {
            try {
                marketLeaderReport =
                    when (reportId) {
                        ReportId.KpiTrends -> MarketLeaderReport.KpiTrends(reportsRepository.kpiTrends(from, to))
                        ReportId.ReactiveCompletion ->
                            MarketLeaderReport.ReactiveCompletion(reportsRepository.reactiveCompletion(from, to))
                        ReportId.EngineerWorkload ->
                            MarketLeaderReport.EngineerWorkload(reportsRepository.engineerWorkload(from, to))
                        ReportId.FailureFrequency ->
                            MarketLeaderReport.FailureFrequency(reportsRepository.failureFrequency(from, to))
                        else -> null
                    }
                if (reportId == ReportId.EngineerWorkload && adminUsersRepository != null) {
                    users = runCatching { adminUsersRepository.listUsers(limit = 200).items }.getOrDefault(emptyList())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GatewayHttpException) {
                marketLeaderError = e.message ?: "Не удалось загрузить отчёт"
            } catch (e: Exception) {
                marketLeaderError = e.message ?: "Не удалось загрузить отчёт"
            } finally {
                marketLeaderLoading = false
            }
        }
    }

    AppScaffold(title = catalogItem.title, onNavigateBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = ClientSpacing.md)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.md),
        ) {
            PeriodSelector(selected = days, onSelected = { days = it })
            if (reportId == ReportId.EquipmentDowntime) {
                AppText(text = "Простои оборудования", style = AppTextStyle.Title)
                when {
                    loading -> CircularProgressIndicator()
                    error != null -> AppText(text = error!!)
                    rows.isEmpty() -> EmptyReportsState()
                    else -> DowntimeTimeline(rows = rows, fromDay = fromDay, toDay = today)
                }
            } else if (reportId.isLegacyKpiReport()) {
                ManagerKpiSections(
                    reportId = reportId,
                    kpis = kpis,
                    assets = assets,
                    loading = kpiLoading,
                    error = kpiError,
                )
            } else {
                MarketLeaderReportSection(
                    report = marketLeaderReport,
                    assets = assets,
                    users = users,
                    loading = marketLeaderLoading,
                    error = marketLeaderError,
                )
            }
            ReportHelpFooter(text = catalogItem.description)
        }
    }
}

@Composable
private fun ReportHelpFooter(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = ClientSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
    ) {
        AppText(text = "Справка", style = AppTextStyle.Label)
        AppText(text = text, style = AppTextStyle.Body)
    }
}

@Composable
private fun ManagerKpiSections(
    reportId: ReportId,
    kpis: ManagerKpis?,
    assets: List<AssetDto>,
    loading: Boolean,
    error: String?,
) {
    val section: @Composable (ManagerKpis) -> Unit = when (reportId) {
        ReportId.KpiSummary -> { kpis -> KpiSummary(kpis) }
        ReportId.PlannedVsEmergency -> { kpis -> KpiPlannedVsEmergency(kpis) }
        ReportId.PprCompliance -> { kpis -> KpiPpr(kpis) }
        ReportId.Backlog -> { kpis -> KpiBacklog(kpis) }
        ReportId.DowntimeRanking -> { kpis -> KpiDowntimeRanking(kpis, assets) }
        ReportId.EquipmentDowntime -> { _ -> }
        else -> { _ -> }
    }
    AppText(
        text = reportCatalogItems().first { it.id == reportId }.title,
        style = AppTextStyle.Title,
    )
    when {
        loading -> CircularProgressIndicator()
        error != null -> AppText(text = error)
        kpis == null -> AppText(text = "Нет данных KPI за выбранный период", style = AppTextStyle.Label)
        else -> {
            AppText(text = "Период: с ${kpis.from} по ${kpis.to}", style = AppTextStyle.Label)
            section(kpis)
        }
    }
}

private fun ReportId.isLegacyKpiReport(): Boolean =
    this in
        setOf(
            ReportId.KpiSummary,
            ReportId.PlannedVsEmergency,
            ReportId.PprCompliance,
            ReportId.Backlog,
            ReportId.DowntimeRanking,
        )

private fun ReportId.isMarketLeaderReport(): Boolean =
    this in
        setOf(
            ReportId.KpiTrends,
            ReportId.ReactiveCompletion,
            ReportId.EngineerWorkload,
            ReportId.FailureFrequency,
        )

@Composable
private fun MarketLeaderReportSection(
    report: MarketLeaderReport?,
    assets: List<AssetDto>,
    users: List<AdminUser>,
    loading: Boolean,
    error: String?,
) {
    when {
        loading -> CircularProgressIndicator()
        error != null -> AppText(text = error)
        report == null -> EmptyKpiChartState()
        else ->
            when (report) {
                is MarketLeaderReport.KpiTrends -> KpiTrends(report.value)
                is MarketLeaderReport.ReactiveCompletion -> ReactiveCompletion(report.value)
                is MarketLeaderReport.EngineerWorkload -> EngineerWorkload(report.value, users)
                is MarketLeaderReport.FailureFrequency -> FailureFrequency(report.value, assets)
            }
    }
}

@Composable
private fun KpiTrends(report: KpiTrendsReport) {
    AppText(text = "Динамика MTTR", style = AppTextStyle.Title)
    val last = report.points.lastOrNull()
    KpiValue("Последний MTTR", last?.let { formatManagerKpiMetric(it.mttrHours, it.mttrSampleSize, " ч") } ?: "н/д")
    KpiValue("Последний MTBF", last?.let { formatManagerKpiMetric(it.mtbfHours, it.mtbfSampleSize, " ч") } ?: "н/д")
    KpiValue("Последняя готовность", last?.let { formatPercent(it.availabilityPercent) } ?: "н/д")
    val points = kpiTrendChartPoints(report.points)
    if (points.isEmpty()) {
        EmptyKpiChartState()
    } else {
        ReportColumnChart(points = points, modifier = Modifier.fillMaxWidth().height(220.dp))
    }
}

@Composable
private fun ReactiveCompletion(report: ReactiveCompletionReport) {
    AppText(text = "Реактивность и закрытие", style = AppTextStyle.Title)
    KpiValue("Создано", "${report.createdCount} заявок")
    KpiValue("Закрыто", "${report.closedCount} заявок")
    KpiValue(
        "Доля закрытых",
        if (report.createdCount == 0) "н/д" else formatPercent(report.completionRatePercent),
    )
    KpiValue(
        "Доля аварийных",
        if (report.emergencyCount + report.plannedCount == 0) "н/д" else formatPercent(report.reactivePercent),
    )
}

@Composable
private fun EngineerWorkload(report: EngineerWorkloadReport, users: List<AdminUser>) {
    AppText(text = "Нагрузка инженеров", style = AppTextStyle.Title)
    val points = engineerWorkloadChartPoints(report.engineers) { userId ->
        formatAssigneeLabel(userId, users).takeUnless { it == "Пользователь" } ?: "Инженер"
    }
    if (points.isEmpty()) {
        EmptyKpiChartState()
    } else {
        ReportHorizontalBarChart(points = points, modifier = Modifier.fillMaxWidth().height(220.dp))
        report.engineers.zip(points).forEach { (row, point) ->
            KpiValue(point.label, "${row.closedCount} заявок · ${formatHours(row.hours)}")
        }
    }
}

@Composable
private fun FailureFrequency(report: FailureFrequencyReport, assets: List<AssetDto>) {
    AppText(text = "Частота отказов", style = AppTextStyle.Title)
    val points = failureFrequencyChartPoints(report.assets, assets)
    if (points.isEmpty()) {
        EmptyKpiChartState()
    } else {
        ReportHorizontalBarChart(points = points, modifier = Modifier.fillMaxWidth().height(220.dp))
        report.assets.zip(points).forEach { (row, point) ->
            KpiValue(point.label, "${row.emergencyCount} аварийных заявок")
        }
    }
}

@Composable
private fun KpiSummary(kpis: ManagerKpis) {
    KpiValue("MTTR", formatManagerKpiMetric(kpis.mttrHours, kpis.mttrSampleSize, " ч"))
    KpiValue("MTBF", formatManagerKpiMetric(kpis.mtbfHours, kpis.mtbfSampleSize, " ч"))
    KpiValue("Готовность", formatPercent(kpis.availabilityPercent))
    val points = kpiSummaryChartPoints(kpis)
    if (hasNonZeroChartSeries(points)) {
        ReportColumnChart(points = points, modifier = Modifier.fillMaxWidth().height(220.dp))
    } else {
        EmptyKpiChartState()
    }
}

@Composable
private fun KpiPlannedVsEmergency(kpis: ManagerKpis) {
    AppText(text = "Плановые vs аварийные", style = AppTextStyle.Title)
    KpiValue("Плановые", "${kpis.plannedCount} заявок · ${formatHours(kpis.plannedHours)}")
    KpiValue("Аварийные", "${kpis.emergencyCount} заявок · ${formatHours(kpis.emergencyHours)}")
    val points = plannedVsEmergencyChartPoints(kpis)
    if (hasNonZeroChartSeries(points)) {
        ReportColumnChart(points = points, modifier = Modifier.fillMaxWidth().height(220.dp))
    } else {
        EmptyKpiChartState()
    }
}

@Composable
private fun KpiPpr(kpis: ManagerKpis) {
    AppText(text = "Выполнение ППР", style = AppTextStyle.Title)
    KpiValue("Выполнено вовремя", kpis.pprOnTime.toString())
    KpiValue("Выполнено с опозданием", kpis.pprLate.toString())
    KpiValue("Открытые просроченные", kpis.pprOpenOverdue.toString())
    KpiValue("Открытые в срок", kpis.pprOpenPending.toString())
}

@Composable
private fun KpiBacklog(kpis: ManagerKpis) {
    AppText(text = "Очередь заявок", style = AppTextStyle.Title)
    KpiValue("Младше 7 дней", kpis.backlogUnder7d.toString())
    KpiValue("От 7 до 30 дней", kpis.backlog7to30d.toString())
    KpiValue("Старше 30 дней", kpis.backlogOver30d.toString())
    KpiValue("Просроченные", kpis.backlogOverdue.toString())
    val points = backlogChartPoints(kpis)
    if (hasNonZeroChartSeries(points)) {
        ReportColumnChart(points = points, modifier = Modifier.fillMaxWidth().height(220.dp))
    } else {
        EmptyKpiChartState()
    }
}

@Composable
private fun KpiDowntimeRanking(kpis: ManagerKpis, assets: List<AssetDto>) {
    AppText(text = "Рейтинг простоев", style = AppTextStyle.Title)
    val rows = formatManagerKpiDowntimeRows(kpis, assets)
    val points = downtimeRankingChartPoints(kpis, assets)
    if (points.isEmpty()) {
        AppText(text = "Нет простоев за выбранный период", style = AppTextStyle.Label)
    } else {
        ReportHorizontalBarChart(points = points, modifier = Modifier.fillMaxWidth().height(220.dp))
        rows.forEach { row ->
            KpiValue(
                row.label,
                "${formatHours(row.downtimeHours)} · открытых интервалов: ${row.openIntervals}",
            )
        }
    }
}

@Composable
private fun EmptyKpiChartState() {
    AppText(text = "Нет данных за выбранный период", style = AppTextStyle.Label)
}

@Composable
private fun KpiValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(text = label, style = AppTextStyle.Label)
        AppText(text = value)
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
