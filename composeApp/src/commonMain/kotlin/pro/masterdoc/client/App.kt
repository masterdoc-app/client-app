package pro.masterdoc.client

import androidx.compose.runtime.Composable
import pro.masterdoc.client.designsystem.theme.ClientTheme
import pro.masterdoc.client.presentation.shell.RootComponent
import pro.masterdoc.client.session.ClientSession
import pro.masterdoc.client.ui.shell.MainShellContent

@Composable
fun App(root: RootComponent) {
    ClientTheme {
        MainShellContent(component = root.shell)
    }
}

@Composable
fun App() {
    App(root = rememberRootComponent())
}

@Composable
expect fun rememberRootComponent(
    session: ClientSession = ClientSession.stub("engineer"),
): RootComponent
