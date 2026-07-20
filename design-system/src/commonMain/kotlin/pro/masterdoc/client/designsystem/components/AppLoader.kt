package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.theme.ClientSpacing

enum class AppLoaderSize(
    val indicatorSize: Dp,
    val strokeWidth: Dp,
) {
    Sm(indicatorSize = 24.dp, strokeWidth = 2.dp),
    Md(indicatorSize = 40.dp, strokeWidth = 3.dp),
    Lg(indicatorSize = 56.dp, strokeWidth = 4.dp),
}

/**
 * Brand loader. Pass [progress] (0f..1f) for determinate / snapshot-stable frames;
 * omit for indeterminate.
 */
@Composable
fun AppLoader(
    modifier: Modifier = Modifier,
    size: AppLoaderSize = AppLoaderSize.Md,
    label: String? = null,
    progress: Float? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.sm),
    ) {
        if (progress == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.indicatorSize),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = size.strokeWidth,
            )
        } else {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(size.indicatorSize),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = size.strokeWidth,
            )
        }
        if (label != null) {
            AppText(
                text = label,
                style = AppTextStyle.Label,
            )
        }
    }
}
