package com.ead.project.moongetter.app

import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.doodstream.factory.DoodstreamFactory
import com.ead.lib.moongetter.facebook.factory.FacebookFactory
import com.ead.lib.moongetter.filemoon.factory.FilemoonFactory
import com.ead.lib.moongetter.goodstream.factory.GoodStreamFactory
import com.ead.lib.moongetter.googledrive.factory.GoogleDriveFactory
import com.ead.lib.moongetter.hexload.factory.HexloadFactory
import com.ead.lib.moongetter.lamovie.factory.LaMovieFactory
import com.ead.lib.moongetter.lulustream.factory.LulustreamFactory
import com.ead.lib.moongetter.mediafire.factory.MediafireFactory
import com.ead.lib.moongetter.mixdrop.factory.MixdropFactory
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.lib.moongetter.mp4upload.factory.Mp4UploadFactory
import com.ead.lib.moongetter.okru.factory.OkruFactory
import com.ead.lib.moongetter.onecloudfile.factory.OneCloudFileFactory
import com.ead.lib.moongetter.pixeldrain.factory.PixeldrainFactory
import com.ead.lib.moongetter.senvid.factory.SenvidFactory
import com.ead.lib.moongetter.streamtape.factory.StreamtapeFactory
import com.ead.lib.moongetter.streamwish.factory.StreamwishFactory
import com.ead.lib.moongetter.uqload.factory.UqloadFactory
import com.ead.lib.moongetter.vihide.factory.VihideFactory
import com.ead.lib.moongetter.vk.factory.VKFactory
import com.ead.lib.moongetter.voe.factory.VoeFactory
import com.ead.lib.moongetter.xtwitter.factory.XTwitterFactory
import com.ead.lib.moongetter.yourupload.factory.YourUploadFactory
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
                    LasEstrellasFactory,
                    /**
                     * DefaultServerFactory
                     */
                    DoodstreamFactory,
                    FacebookFactory,
                    FilemoonFactory,
                    //FireloadFactory,
                    GoodStreamFactory,
                    GoogleDriveFactory,
                    HexloadFactory,
                    LaMovieFactory,
                    LulustreamFactory,
                    MediafireFactory,
                    MixdropFactory,
                    Mp4UploadFactory,
                    OkruFactory,
                    OneCloudFileFactory,
                    PixeldrainFactory,
                    SenvidFactory,
                    StreamtapeFactory,
                    StreamwishFactory,
                    UqloadFactory,
                    VKFactory,
                    //VidguardFactory,
                    VihideFactory,
                    VoeFactory,
                    XTwitterFactory,
                    YourUploadFactory
                )
            )
            .build()
    }
    single<Factory.Builder> {
        MoonGetter
            .Builder()
            .setClient(
                client = KtorMoonClient(
                    engineFactory = httpClientEngineFactory,
                    cookieManagement = cookieManagement,
                    trustManager = trustManager
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

fun initializeKoin() {
    startKoin { modules(appModule) }
}