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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AdminUser
import pro.masterdoc.client.auth.AdminUsersRepository
import pro.masterdoc.client.auth.AiMessageDto
import pro.masterdoc.client.auth.AiMessagesRepository
import pro.masterdoc.client.auth.WorkOrdersRepository
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
    adminUsersRepository: AdminUsersRepository? = null,
    workOrdersRepository: WorkOrdersRepository? = null,
) {
    var messages by remember { mutableStateOf<List<AiMessageDto>>(emptyList()) }
    var workOrderTitles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun resolveLabels(items: List<AiMessageDto>) {
        if (adminUsersRepository != null && users.isEmpty()) {
            users =
                runCatching { adminUsersRepository.listUsers(limit = 200).items }
                    .getOrDefault(emptyList())
        }
        if (workOrdersRepository == null) return
        val missing =
            items.map { it.workOrderId }
                .filter { it.isNotBlank() && it !in workOrderTitles }
                .distinct()
        if (missing.isEmpty()) return
        val resolved =
            coroutineScope {
                missing
                    .map { id ->
                        async {
                            id to
                                runCatching { workOrdersRepository.get(id).title.trim() }
                                    .getOrNull()
                                    ?.takeIf { it.isNotEmpty() }
                        }
                    }.awaitAll()
                    .mapNotNull { (id, title) -> title?.let { id to it } }
                    .toMap()
            }
        if (resolved.isNotEmpty()) {
            workOrderTitles = workOrderTitles + resolved
        }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                val page = repository.list(limit = AiMessagesPageSize, offset = 0)
                messages = page.items
                hasMore = page.items.size == AiMessagesPageSize
                resolveLabels(page.items)
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
                resolveLabels(page.items)
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
                            aiMessageEntityLabels(
                                message = message,
                                workOrderTitleById = workOrderTitles,
                                users = users,
                            )?.let { labels ->
                                AppText(text = labels, style = AppTextStyle.Label)
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

/** Visible meta for AI cards — titles/names only, never raw ids. */
internal fun aiMessageEntityLabels(
    message: AiMessageDto,
    workOrderTitleById: Map<String, String>,
    users: List<AdminUser>,
): String? {
    val parts =
        listOfNotNull(
            message.workOrderId.takeIf { it.isNotBlank() }?.let {
                "Заявка: ${workOrderTitleById[it]?.takeIf { title -> title.isNotBlank() } ?: "без названия"}"
            },
            message.engineerId.takeIf { it.isNotBlank() }?.let {
                "Инженер: ${formatAssigneeLabel(it, users)}"
            },
        )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
