package pro.masterdoc.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.defaultComponentContext
import pro.masterdoc.client.presentation.shell.DefaultRootComponent
import pro.masterdoc.client.presentation.shell.RootComponent
import pro.masterdoc.client.session.ClientSession

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root =
            DefaultRootComponent(
                componentContext = defaultComponentContext(),
                session = ClientSession.stub("engineer"),
            )
        setContent {
            App(root = root)
        }
    }
}

@Composable
actual fun rememberRootComponent(
    session: ClientSession,
): RootComponent {
    val componentContext = defaultComponentContext()
    return remember(session) {
        DefaultRootComponent(
            componentContext = componentContext,
            session = session,
        )
    }
}
