package com.ead.project.moongetter.app

import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.android.robot.AndroidRobot
import com.ead.lib.moongetter.client.cookie.java.net.JavaNetCookieManagement
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.trust.manager.java.net.JavaMoonTrustManager
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.lib.moongetter.server.bundle.serverBundle
import com.ead.lib.moongetter.server.robot.bundle.serverRobotBundle
import com.ead.project.moongetter.domain.custom_servers.sendvid_modified.factory.SenvidModifiedFactory
import com.ead.project.moongetter.domain.custom_servers.test.las_estrellas.factory.LasEstrellasFactory
import com.ead.project.moongetter.presentation.main.MainViewModel
import io.ktor.client.engine.cio.CIO
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single <Engine> {
        Engine.Builder()
            .onCore(
                engines =
                    // custom servers
                    arrayOf(SenvidModifiedFactory, LasEstrellasFactory)
                + serverBundle
                + serverRobotBundle
            )
            .onRobot(
                AndroidRobot
                    .Builder()
                    .onContext(
                        context = get()
                    )
                    .build()
            )
            .build()
    }
    single<Factory.Builder> {
        MoonGetter
            .Builder()
            .setClient(
                client = KtorMoonClient(
                    engineFactory = CIO,
                    cookieManagement = JavaNetCookieManagement(),
                    trustManager = JavaMoonTrustManager
                )
            )
            .setTimeout(timeoutMillis = 12000)
            .setEngine(engine = get())
    }
    viewModel<MainViewModel> {
        MainViewModel(
            moonFactoryBuilder = get()
        )
    }
}