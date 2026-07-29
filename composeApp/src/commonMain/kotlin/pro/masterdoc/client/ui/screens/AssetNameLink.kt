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
 * Clickable equipment name. Inventory is shown as muted secondary text — Material3
 * [TooltipBox]/[PlainTooltip] on Compose Wasm paints an opaque black bar and steals clicks.
 */
@Composable
fun AssetNameLink(
    name: String?,
    inventoryNo: String?,
    assetId: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
    ) {
        AppText(
            text = assetDisplayName(name, assetId),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onOpen(assetId) },
        )
        AppText(
            text = assetInventoryTooltip(inventoryNo),
            style = AppTextStyle.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
