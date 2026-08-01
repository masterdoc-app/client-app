package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.navigation.NavDestinationId

@Composable
fun StubDestinationScreen(
    destination: NavDestinationId,
    modifier: Modifier = Modifier,
) {
    val title = destinationTitle(destination)
    AppScaffold(title = title, modifier = modifier) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = title)
        }
    }
}

fun destinationTitle(destination: NavDestinationId): String =
    when (destination) {
        NavDestinationId.Tickets -> "Заявки"
        NavDestinationId.Board -> "Доска"
        NavDestinationId.MyWorkOrders -> "Мои заявки"
        NavDestinationId.Map -> "Карта"
        NavDestinationId.Charts -> "ППР"
        NavDestinationId.Equipment -> "Оборудование"
        NavDestinationId.Profile -> "Профиль"
        NavDestinationId.BlackBox -> "Чёрный ящик"
        NavDestinationId.Ai -> "ИИ"
        NavDestinationId.Users -> "Админ"
    }
