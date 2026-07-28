package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import pro.masterdoc.client.auth.MentorHistoryTurn
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

internal data class WoAssistantMessage(
    val role: String,
    val content: String,
)

/** Show start-assistant control only when the current user is the WO assignee. */
internal fun shouldShowWoAssistant(
    assigneeId: String?,
    currentUserId: String?,
): Boolean {
    val me = currentUserId?.takeIf { it.isNotBlank() } ?: return false
    val assignee = assigneeId?.takeIf { it.isNotBlank() } ?: return false
    return assignee == me
}

internal fun isMentorAssigneeForbidden(status: Int): Boolean = status == 403

internal fun toMentorHistory(messages: List<WoAssistantMessage>): List<MentorHistoryTurn> =
    messages.map { MentorHistoryTurn(role = it.role, content = it.content) }

@Composable
internal fun WorkOrderAssistantDialog(
    workOrderId: String,
    repository: EquipmentRepository,
    onDismiss: () -> Unit,
    onAssigneeForbidden: () -> Unit,
) {
    var messages by remember { mutableStateOf<List<WoAssistantMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        scope.launch {
            sending = true
            error = null
            try {
                val history = toMentorHistory(messages)
                val reply = repository.askMentor(workOrderId = workOrderId, message = text, history = history)
                messages =
                    messages +
                        WoAssistantMessage(role = "user", content = text) +
                        WoAssistantMessage(role = "assistant", content = reply.reply)
                draft = ""
            } catch (e: GatewayHttpException) {
                if (isMentorAssigneeForbidden(e.status)) {
                    onAssigneeForbidden()
                    onDismiss()
                } else {
                    error = e.message ?: "Ошибка ассистента"
                }
            } catch (e: Exception) {
                error = e.message ?: "Ошибка ассистента"
            } finally {
                sending = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { AppText(text = "Ассистент по заявке", style = AppTextStyle.Title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
                ) {
                    if (messages.isEmpty()) {
                        AppText(
                            text = "Задайте вопрос по этой заявке и документации оборудования.",
                            style = AppTextStyle.Body,
                        )
                    }
                    messages.forEach { msg ->
                        val label = if (msg.role == "user") "Вы" else "Ассистент"
                        AppText(text = "$label: ${msg.content}", style = AppTextStyle.Body)
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
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
                modifier = Modifier.padding(bottom = ClientSpacing.xs),
            ) {
                AppButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    enabled = !sending,
                    variant = AppButtonVariant.Secondary,
                    fillMaxWidth = false,
                )
                AppButton(
                    text = if (sending) "…" else "Отправить",
                    onClick = { send() },
                    enabled = !sending && draft.trim().isNotEmpty(),
                    fillMaxWidth = false,
                )
            }
        },
    )
}
