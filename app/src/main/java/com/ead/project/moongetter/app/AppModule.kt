package com.ead.project.moongetter.app

import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.abyss.factory.AbyssFactory
import com.ead.lib.moongetter.doodstream.factory.DoodstreamFactory
import com.ead.lib.moongetter.facebook.factory.FacebookFactory
import com.ead.lib.moongetter.filemoon.factory.FilemoonFactory
import com.ead.lib.moongetter.fireload.factory.FireloadFactory
import com.ead.lib.moongetter.gofile.factory.GofileFactory
import com.ead.lib.moongetter.goodstream.factory.GoodStreamFactory
import com.ead.lib.moongetter.googledrive.factory.GoogleDriveFactory
import com.ead.lib.moongetter.hexload.factory.HexloadFactory
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
import com.ead.lib.moongetter.vidguard.factory.VidguardFactory
import com.ead.lib.moongetter.vihide.factory.VihideFactory
import com.ead.lib.moongetter.voe.factory.VoeFactory
import com.ead.lib.moongetter.xtwitter.factory.XTwitterFactory
import com.ead.lib.moongetter.yourupload.factory.YourUploadFactory
import com.ead.project.moongetter.domain.custom_servers.factory.SenvidModifiedFactory
import com.ead.project.moongetter.presentation.main.MainViewModel
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
                    /**
                     * DefaultServerFactory
                     */
                    AbyssFactory,
                    DoodstreamFactory,
                    FacebookFactory,
                    FilemoonFactory,
                    FireloadFactory,
                    GofileFactory,
                    GoodStreamFactory,
                    GoogleDriveFactory,
                    HexloadFactory,
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
                    VidguardFactory,
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
            .initialize(
                context = get()
            )
            .setTimeout(12000)
            .setEngine(
                engine = get()
            )
    }
    viewModel<MainViewModel> {
        MainViewModel(
            moonFactoryBuilder = get()
        )
    }
}