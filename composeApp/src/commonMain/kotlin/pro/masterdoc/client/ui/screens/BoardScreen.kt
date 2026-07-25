package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.BoardWeekDto
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
fun BoardScreen(
    repository: WorkOrdersRepository,
    modifier: Modifier = Modifier,
) {
    var weeks by remember { mutableStateOf<List<BoardWeekDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                weeks = repository.getBoard(weeks = 4).weeks
            } catch (e: GatewayHttpException) {
                error = e.message ?: "Ошибка загрузки доски"
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки доски"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(repository) {
        reload()
    }

    val detailId = selectedId
    if (detailId != null) {
        WorkOrderDetailScreen(
            repository = repository,
            orderId = detailId,
            onBack = { selectedId = null },
            onChanged = { reload() },
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
                        AppButton(text = "Повторить", onClick = { reload() })
                    }
                }
                else -> {
                    if (error != null) {
                        AppText(text = error!!)
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ClientSpacing.md),
                    ) {
                        weeks.forEach { week ->
                            WeekColumn(
                                week = week,
                                onOpen = { selectedId = it },
                                modifier = Modifier.width(280.dp).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekColumn(
    week: BoardWeekDto,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        AppText(
            text = "Нед. ${week.weekStart}",
            style = AppTextStyle.Title,
        )
        AppText(
            text = "${week.items.size} заявок",
            style = AppTextStyle.Label,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            if (week.items.isEmpty()) {
                AppText(text = "Нет заявок", style = AppTextStyle.Body)
            } else {
                week.items.forEach { order ->
                    WorkOrderBoardCard(
                        order = order,
                        onClick = { onOpen(order.id) },
                    )
                }
            }
        }
    }
}
