package pro.masterdoc.client.di

import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import pro.masterdoc.client.session.ClientSession

fun initClientKoin(session: ClientSession = ClientSession.stub("engineer")) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    startKoin {
        modules(clientAppModule(session))
    }
}
