package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pro.masterdoc.client.designsystem.components.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetNameLink(
    name: String?,
    inventoryNo: String?,
    assetId: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                AppText(text = assetInventoryTooltip(inventoryNo))
            }
        },
        state = rememberTooltipState(),
    ) {
        AppText(
            text = assetDisplayName(name, assetId),
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.clickable { onOpen(assetId) },
        )
    }
}
