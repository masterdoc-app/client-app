package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing

/**
 * Equipment name + inventory as one primary-colored click target.
 * Material3 [TooltipBox]/[PlainTooltip] on Compose Wasm paints a black bar and steals clicks.
 */
@Composable
fun AssetNameLink(
    name: String?,
    inventoryNo: String?,
    assetId: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = assetNameLinkLines(name, inventoryNo, assetId)
    val linkColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.clickable { onOpen(assetId) },
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
    ) {
        lines.forEach { line ->
            AppText(
                text = line.text,
                style =
                    when (line.role) {
                        AssetLinkLineRole.Title -> AppTextStyle.Body
                        AssetLinkLineRole.Inventory -> AppTextStyle.Label
                    },
                color = if (line.isLinkColored) linkColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
