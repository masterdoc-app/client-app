package pro.masterdoc.client.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.theme.ClientSpacing

enum class AppNavButtonLayout {
    /** Compact vertical stack for bottom navigation. */
    Bottom,

    /** Wider vertical stack for side rail / drawer. */
    Rail,
}

/**
 * Shared nav control for [AppNavBar] (bottom) and [AppNavRail] (side menu).
 */
@Composable
fun AppNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AppNavButtonLayout = AppNavButtonLayout.Bottom,
) {
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val iconBackground =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        }
    val iconPadding =
        when (layout) {
            AppNavButtonLayout.Bottom -> ClientSpacing.sm
            AppNavButtonLayout.Rail -> ClientSpacing.md
        }
    val verticalPadding =
        when (layout) {
            AppNavButtonLayout.Bottom -> ClientSpacing.xs
            AppNavButtonLayout.Rail -> ClientSpacing.sm
        }

    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    role = Role.Tab,
                    onClick = onClick,
                )
                .padding(vertical = verticalPadding, horizontal = ClientSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ClientSpacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBackground)
                    .padding(horizontal = iconPadding, vertical = ClientSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = contentColor,
            )
        }
        AppText(
            text = label,
            style = AppTextStyle.Label,
            color = contentColor,
            maxLines = 1,
        )
    }
}
