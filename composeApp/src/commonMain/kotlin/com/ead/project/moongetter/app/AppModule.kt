package com.ead.project.moongetter.app

import com.ead.lib.moongetter.MoonFactory
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.lib.moongetter.server.bundle.serverBundle
import com.ead.lib.moongetter.server.robot.bundle.serverRobotBundle
import com.ead.project.moongetter.app.lib.cookieManagement
import com.ead.project.moongetter.app.lib.httpClientEngineFactory
import com.ead.project.moongetter.app.lib.trustManager
import com.ead.project.moongetter.domain.custom_servers.sendvid_modified.factory.SenvidModifiedFactory
import com.ead.project.moongetter.domain.custom_servers.test.las_estrellas.factory.LasEstrellasFactory
import com.ead.project.moongetter.presentation.main.MainViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single <Engine> {
        Engine.Builder()
            .onCore(
                engines = arrayOf(
                    /**
                     * CustomServerFactory
                     */
                    SenvidModifiedFactory,
                    LasEstrellasFactory
                ) + serverBundle + serverRobotBundle
            )
            .build()
    }
    viewModel<MainViewModel> {
        MainViewModel(
            engine = get()
        )
    }
}

fun initializeKoin() {
    startKoin { modules(appModule) }
}