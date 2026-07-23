package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.MaintenanceMapDto
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
fun ChartsScreen(
    repository: EquipmentRepository,
    focusedMapId: String? = null,
    modifier: Modifier = Modifier,
) {
    var maps by remember { mutableStateOf<List<MaintenanceMapDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            try {
                maps = repository.listMaps().items
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(repository) { reload() }

    val drafts = maps.filter { it.status == "draft" }
    val active = maps.filter { it.status == "active" }
    val focused = focusedMapId?.let { id -> maps.firstOrNull { it.id == id } }

    AppScaffold(title = "ППР", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppText(
                text = "Черновики карт обслуживания появляются после работы Технолога. После подтверждения ППР попадает в базу.",
                style = AppTextStyle.Label,
            )
            error?.let { AppText(text = it) }

            if (focused != null) {
                AppText(text = "Открыто по ссылке", style = AppTextStyle.Title)
                MapSummary(map = focused, highlighted = true)
            }

            AppText(text = "Черновики ППР", style = AppTextStyle.Title)
            when {
                loading && maps.isEmpty() -> CircularProgressIndicator()
                drafts.isEmpty() -> AppText(text = "Нет черновиков", style = AppTextStyle.Label)
                else ->
                    drafts.forEach { map ->
                        MapDraftRow(
                            map = map,
                            acting = actingId == map.id,
                            highlighted = map.id == focusedMapId,
                            onConfirm = {
                                scope.launch {
                                    actingId = map.id
                                    try {
                                        repository.confirmMap(map.id)
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        actingId = null
                                    }
                                }
                            },
                            onReject = {
                                scope.launch {
                                    actingId = map.id
                                    try {
                                        repository.rejectMap(map.id)
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        actingId = null
                                    }
                                }
                            },
                        )
                    }
            }

            AppText(text = "ППР в базе", style = AppTextStyle.Title)
            when {
                loading && maps.isEmpty() -> Unit
                active.isEmpty() -> AppText(text = "Пока пусто", style = AppTextStyle.Label)
                else ->
                    active.forEach { map ->
                        MapSummary(map = map, highlighted = map.id == focusedMapId)
                    }
            }
        }
    }
}

@Composable
private fun MapDraftRow(
    map: MaintenanceMapDto,
    acting: Boolean,
    highlighted: Boolean = false,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MapSummary(map = map, highlighted = highlighted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(text = if (acting) "…" else "Подтвердить", enabled = !acting, onClick = onConfirm)
            AppButton(text = "Отклонить", enabled = !acting, onClick = onReject)
        }
    }
}

@Composable
private fun MapSummary(
    map: MaintenanceMapDto,
    highlighted: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppText(
            text =
                buildString {
                    if (highlighted) append("→ ")
                    append("${map.title} · ${ruStatus(map.status)} · ${ruSource(map.source)}")
                },
            style = AppTextStyle.Body,
            color =
                if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    androidx.compose.ui.graphics.Color.Unspecified
                },
        )
        AppText(
            text = "Оборудование: ${map.assetId} · пунктов: ${map.items.size}",
            style = AppTextStyle.Label,
        )
        map.items.take(5).forEach { item ->
            AppText(
                text =
                    "• ${item.title} (${ruKind(item.kind)}, каждые ${item.interval.every} ${ruIntervalUnit(item.interval.every, item.interval.unit)})",
                style = AppTextStyle.Label,
            )
        }
        if (map.items.size > 5) {
            AppText(text = "… ещё ${map.items.size - 5}", style = AppTextStyle.Label)
        }
    }
}

internal fun ruStatus(status: String): String =
    when (status.lowercase()) {
        "draft" -> "черновик"
        "active" -> "активна"
        "rejected" -> "отклонена"
        else -> status
    }

internal fun ruSource(source: String): String =
    when (source.lowercase()) {
        "ai_generated" -> "ИИ"
        "manual" -> "вручную"
        else -> source
    }

internal fun ruKind(kind: String): String =
    when (kind.lowercase()) {
        "inspection" -> "осмотр"
        "service" -> "обслуживание"
        "repair" -> "ремонт"
        "replacement" -> "замена"
        "calibration" -> "калибровка"
        else -> kind
    }

internal fun ruIntervalUnit(every: Int, unit: String): String =
    when (unit.lowercase()) {
        "days", "day" -> russianPlural(every, "день", "дня", "дней")
        "weeks", "week" -> russianPlural(every, "неделя", "недели", "недель")
        "months", "month" -> russianPlural(every, "месяц", "месяца", "месяцев")
        "hours", "hour" -> russianPlural(every, "час", "часа", "часов")
        "years", "year" -> russianPlural(every, "год", "года", "лет")
        else -> unit
    }

internal fun russianPlural(n: Int, one: String, few: String, many: String): String {
    val abs = kotlin.math.abs(n)
    val mod100 = abs % 100
    val mod10 = abs % 10
    val form =
        when {
            mod100 in 11..14 -> many
            mod10 == 1 -> one
            mod10 in 2..4 -> few
            else -> many
        }
    return form
}
