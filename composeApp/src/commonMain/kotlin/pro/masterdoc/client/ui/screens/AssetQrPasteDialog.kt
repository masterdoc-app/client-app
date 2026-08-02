package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextField
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.navigation.parseAssetQrInput

@Composable
internal fun AssetQrPasteDialog(
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val token = parseAssetQrInput(input)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText(text = "Сканировать QR", style = AppTextStyle.Title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
            ) {
                AppText(text = "Вставьте ссылку с QR-кода или код вручную.")
                AppTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "Ссылка или код",
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm)) {
                AppButton(
                    text = "Отмена",
                    onClick = onDismiss,
                    variant = AppButtonVariant.Secondary,
                    fillMaxWidth = false,
                )
                AppButton(
                    text = "Открыть",
                    onClick = { token?.let(onOpen) },
                    enabled = token != null,
                    fillMaxWidth = false,
                )
            }
        },
    )
}
