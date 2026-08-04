package pro.masterdoc.client.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

enum class AppTextStyle {
    Display,
    Title,
    Body,
    Label,
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.Body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val resolvedColor =
        if (color == Color.Unspecified) {
            when (style) {
                AppTextStyle.Label -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
        } else {
            color
        }

    Text(
        text = text,
        modifier = modifier,
        style = style.toTextStyle(),
        color = resolvedColor,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AppTextStyle.toTextStyle(): TextStyle =
    when (this) {
        AppTextStyle.Display -> MaterialTheme.typography.displayLarge
        AppTextStyle.Title -> MaterialTheme.typography.titleLarge
        AppTextStyle.Body -> MaterialTheme.typography.bodyLarge
        AppTextStyle.Label -> MaterialTheme.typography.labelLarge
    }
