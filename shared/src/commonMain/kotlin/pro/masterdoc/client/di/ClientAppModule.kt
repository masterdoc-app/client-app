package pro.masterdoc.client.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pro.masterdoc.client.navigation.DefaultNavMenuBuilder
import pro.masterdoc.client.navigation.NavMenuBuilder
import pro.masterdoc.client.session.ClientSession

fun clientAppModule(
    session: ClientSession = ClientSession.stub("engineer"),
): Module =
    module {
        single<NavMenuBuilder> { DefaultNavMenuBuilder() }
        single { session }
    }
