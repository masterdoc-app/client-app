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
 * Clickable equipment name + inventory. Both lines use link color and share one click
 * target — Material3 [TooltipBox]/[PlainTooltip] on Compose Wasm paints an opaque
 * black bar and steals clicks, so inventory is inline text instead of a tooltip.
 */
@Composable
fun AssetNameLink(
    name: String?,
    inventoryNo: String?,
    assetId: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.clickable { onOpen(assetId) },
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
    ) {
        AppText(
            text = assetDisplayName(name, assetId),
            color = linkColor,
        )
        AppText(
            text = assetInventoryTooltip(inventoryNo),
            style = AppTextStyle.Label,
            color = linkColor,
        )
    }
}
