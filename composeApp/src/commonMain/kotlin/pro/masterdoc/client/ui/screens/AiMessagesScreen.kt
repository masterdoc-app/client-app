package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AiMessageDto
import pro.masterdoc.client.auth.AiMessagesRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

private const val AiMessagesPageSize = 30

@Composable
fun AiMessagesScreen(
    repository: AiMessagesRepository,
    modifier: Modifier = Modifier,
) {
    var messages by remember { mutableStateOf<List<AiMessageDto>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                val page = repository.list(limit = AiMessagesPageSize, offset = 0)
                messages = page.items
                hasMore = page.items.size == AiMessagesPageSize
            } catch (e: Exception) {
                messages = emptyList()
                hasMore = false
                error = e.message ?: "Не удалось загрузить сообщения ИИ"
            } finally {
                loading = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            error = null
            try {
                val page = repository.list(limit = AiMessagesPageSize, offset = messages.size)
                messages = messages + page.items
                hasMore = page.items.size == AiMessagesPageSize
            } catch (e: Exception) {
                error = e.message ?: "Не удалось загрузить сообщения ИИ"
            } finally {
                loadingMore = false
            }
        }
    }

    LaunchedEffect(repository) { reload() }

    AppScaffold(title = "ИИ", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppButton(text = "Обновить", onClick = ::reload, fillMaxWidth = false)
            error?.let { AppText(text = it) }
            when {
                loading -> CircularProgressIndicator()
                messages.isEmpty() -> AppText(text = "Пока нет сообщений", style = AppTextStyle.Label)
                else -> {
                    messages.forEach { message ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            AppText(text = message.title)
                            AppText(text = message.body, style = AppTextStyle.Body)
                            AppText(text = message.createdAt, style = AppTextStyle.Label)
                            listOf(
                                message.workOrderId.takeIf { it.isNotBlank() }?.let { "Заявка: $it" },
                                message.engineerId.takeIf { it.isNotBlank() }?.let { "Инженер: $it" },
                            ).filterNotNull().takeIf { it.isNotEmpty() }?.let {
                                AppText(text = it.joinToString(" · "), style = AppTextStyle.Label)
                            }
                        }
                    }
                    if (hasMore) {
                        AppButton(
                            text = if (loadingMore) "Загрузка…" else "Ещё",
                            onClick = ::loadMore,
                            fillMaxWidth = false,
                        )
                    }
                }
            }
        }
    }
}
