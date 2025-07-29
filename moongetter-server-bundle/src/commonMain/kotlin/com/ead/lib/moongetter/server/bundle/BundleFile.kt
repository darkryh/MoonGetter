package com.ead.lib.moongetter.server.bundle

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
import com.ead.lib.moongetter.mp4upload.factory.Mp4UploadFactory
import com.ead.lib.moongetter.okru.factory.OkruFactory
import com.ead.lib.moongetter.pixeldrain.factory.PixeldrainFactory
import com.ead.lib.moongetter.senvid.factory.SenvidFactory
import com.ead.lib.moongetter.streamtape.factory.StreamtapeFactory
import com.ead.lib.moongetter.streamwish.factory.StreamwishFactory
import com.ead.lib.moongetter.uqload.factory.UqloadFactory
import com.ead.lib.moongetter.vidguard.factory.VidguardFactory
import com.ead.lib.moongetter.vihide.factory.VihideFactory
import com.ead.lib.moongetter.vk.factory.VKFactory
import com.ead.lib.moongetter.voe.factory.VoeFactory
import com.ead.lib.moongetter.xtwitter.factory.XTwitterFactory
import com.ead.lib.moongetter.yourupload.factory.YourUploadFactory

/**
 * bundle of server factories
 */
val serverBundle = arrayOf(
    DoodstreamFactory,
    FacebookFactory,
    FilemoonFactory,
    GoodStreamFactory,
    GoogleDriveFactory,
    HexloadFactory,
    LaMovieFactory,
    LulustreamFactory,
    MediafireFactory,
    MixdropFactory,
    Mp4UploadFactory,
    OkruFactory,
    PixeldrainFactory,
    SenvidFactory,
    StreamtapeFactory,
    StreamwishFactory,
    UqloadFactory,
    VKFactory,
    VihideFactory,
    VoeFactory,
    XTwitterFactory,
    VidguardFactory,
    YourUploadFactory
)