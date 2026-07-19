package pro.masterdoc.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import pro.masterdoc.client.presentation.shell.DefaultRootComponent
import pro.masterdoc.client.presentation.shell.RootComponent
import pro.masterdoc.client.session.ClientSession

@Composable
actual fun rememberRootComponent(
    session: ClientSession,
): RootComponent =
    remember(session) {
        DefaultRootComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            session = session,
        )
    }
