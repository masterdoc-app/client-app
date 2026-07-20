package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.theme.ClientSpacing

enum class AppButtonVariant {
    Primary,
    Secondary,
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: AppButtonVariant = AppButtonVariant.Primary,
) {
    val contentPadding = PaddingValues(horizontal = ClientSpacing.md, vertical = 12.dp)
    val buttonModifier = modifier.fillMaxWidth()

    when (variant) {
        AppButtonVariant.Primary ->
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                contentPadding = contentPadding,
                shape = MaterialTheme.shapes.small,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                Text(text = text, style = MaterialTheme.typography.labelLarge)
            }

        AppButtonVariant.Secondary ->
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                contentPadding = contentPadding,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                Text(text = text, style = MaterialTheme.typography.labelLarge)
            }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = MaterialTheme.shapes.small,
    )
}
