package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
fun WorkOrderMentorScreen(
    workOrderId: String,
    repository: EquipmentRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var messages by remember(workOrderId) { mutableStateOf<List<WoAssistantMessage>>(emptyList()) }
    var draft by remember(workOrderId) { mutableStateOf("") }
    var sending by remember(workOrderId) { mutableStateOf(false) }
    var error by remember(workOrderId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        scope.launch {
            sending = true
            error = null
            try {
                val reply =
                    repository.askMentor(
                        workOrderId = workOrderId,
                        message = text,
                        history = toMentorHistory(messages),
                    )
                messages =
                    messages +
                        WoAssistantMessage(role = "user", content = text) +
                        WoAssistantMessage(role = "assistant", content = reply.reply)
                draft = ""
            } catch (e: GatewayHttpException) {
                error = e.message ?: "Ошибка наставника"
            } catch (e: Exception) {
                error = e.message ?: "Ошибка наставника"
            } finally {
                sending = false
            }
        }
    }

    AppScaffold(title = "Наставник", modifier = modifier, onNavigateBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
            ) {
                if (messages.isEmpty()) {
                    item {
                        AppText(
                            text = "Задайте вопрос по этой заявке.",
                            style = AppTextStyle.Body,
                        )
                    }
                }
                items(messages) { message ->
                    val label = if (message.role == "user") "Вы" else "Наставник"
                    AppText(text = "$label: ${message.content}", style = AppTextStyle.Body)
                }
            }
            if (error != null) {
                AppText(text = error!!, style = AppTextStyle.Label)
            }
            if (sending) {
                CircularProgressIndicator()
            }
            AppTextField(
                value = draft,
                onValueChange = { if (!sending) draft = it },
                label = "Сообщение",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
            ) {
                AppButton(
                    text = "Назад",
                    onClick = onBack,
                    enabled = !sending,
                    variant = AppButtonVariant.Secondary,
                    fillMaxWidth = false,
                )
                AppButton(
                    text = if (sending) "…" else "Отправить",
                    onClick = ::send,
                    enabled = !sending && draft.trim().isNotEmpty(),
                    fillMaxWidth = false,
                )
            }
        }
    }
}
