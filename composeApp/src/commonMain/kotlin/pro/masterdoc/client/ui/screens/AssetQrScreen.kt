package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pro.masterdoc.client.auth.AssetQrResolveDto
import pro.masterdoc.client.auth.CreateWorkOrderRequest
import pro.masterdoc.client.auth.EquipmentRepository
import pro.masterdoc.client.auth.GatewayHttpException
import pro.masterdoc.client.auth.IsoDates
import pro.masterdoc.client.auth.WorkOrdersRepository
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.platform.localEpochDay

fun assetQrWorkOrderRequest(
    asset: AssetQrResolveDto,
    description: String,
    dueAt: String,
): CreateWorkOrderRequest =
    CreateWorkOrderRequest(
        type = "emergency",
        title = description.lineSequence().first().trim().take(120).ifBlank { "Заявка" },
        assetId = asset.assetId,
        siteId = asset.siteId,
        dueAt = dueAt,
        description = description,
    )

fun assetQrErrorMessage(error: Throwable): String =
    when ((error as? GatewayHttpException)?.status) {
        404 -> "Код не найден или устарел"
        403 -> "Нет доступа"
        else -> "Не удалось открыть код"
    }

@Composable
fun AssetQrScreen(
    token: String,
    equipmentRepository: EquipmentRepository,
    workOrdersRepository: WorkOrdersRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var asset by remember(token) { mutableStateOf<AssetQrResolveDto?>(null) }
    var description by remember(token) { mutableStateOf("") }
    var loading by remember(token) { mutableStateOf(true) }
    var acting by remember(token) { mutableStateOf(false) }
    var resolveError by remember(token) { mutableStateOf<String?>(null) }
    var actionError by remember(token) { mutableStateOf<String?>(null) }
    var created by remember(token) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token, equipmentRepository) {
        loading = true
        resolveError = null
        try {
            asset = equipmentRepository.resolveQr(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            resolveError = assetQrErrorMessage(e)
        } finally {
            loading = false
        }
    }

    AppScaffold(title = "Заявка по QR", modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(ClientSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
        ) {
            AppButton(text = "Закрыть", onClick = onClose)
            when {
                loading -> CircularProgressIndicator()
                resolveError != null -> AppText(text = resolveError.orEmpty())
                asset != null -> {
                    val resolved = asset ?: return@Column
                    AppText(
                        text = resolved.name.ifBlank { "Оборудование" },
                        style = AppTextStyle.Title,
                    )
                    resolved.siteName?.takeIf { it.isNotBlank() }?.let { AppText(text = it) }
                    AppTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            created = false
                            actionError = null
                        },
                        label = "Описание проблемы",
                        singleLine = false,
                        enabled = !acting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppButton(
                        text = if (acting) "Создание…" else "Создать заявку",
                        enabled = !acting && description.isNotBlank(),
                        onClick = {
                            scope.launch {
                                acting = true
                                actionError = null
                                try {
                                    workOrdersRepository.create(
                                        assetQrWorkOrderRequest(
                                            asset = resolved,
                                            description = description,
                                            dueAt = IsoDates.formatEpochDay(localEpochDay()),
                                        ),
                                    )
                                    created = true
                                    description = ""
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    actionError = "Ошибка создания заявки"
                                } finally {
                                    acting = false
                                }
                            }
                        },
                    )
                    if (created) AppText(text = "Заявка создана")
                    actionError?.let { AppText(text = it) }
                }
            }
        }
    }
}
