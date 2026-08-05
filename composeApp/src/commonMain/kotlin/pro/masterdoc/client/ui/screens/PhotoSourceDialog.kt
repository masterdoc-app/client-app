package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppButtonVariant
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

@Composable
internal fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onFromDisk: () -> Unit,
    onCamera: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText(text = "Добавить фото", style = AppTextStyle.Title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
            ) {
                AppButton(
                    text = "С диска",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onFromDisk()
                        onDismiss()
                    },
                    fillMaxWidth = false,
                )
                AppButton(
                    text = "Камера",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onCamera()
                        onDismiss()
                    },
                    fillMaxWidth = false,
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Отмена",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary,
                fillMaxWidth = false,
            )
        },
    )
}
