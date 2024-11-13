@file:OptIn(ExperimentalFeature::class)

package com.ead.project.moongetter.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.abyss.factory.AbyssFactory
import com.ead.lib.moongetter.core.ExperimentalFeature
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
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
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
import com.ead.project.moongetter.presentation.main.event.MainEvent
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.state.MainState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

class MainViewModel() : ViewModel() {

    private val _state : MutableStateFlow<MainState> = MutableStateFlow(MainState())
    val state : StateFlow<MainState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<MainEvent>()
    val event = _event.asSharedFlow()

    private val engine : Engine = Engine.Builder()
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

    fun onIntent(intent: MainIntent) {
        try {
            when(intent) {
                is MainIntent.EnteredTargetSearch -> {
                    _state.value = state.value.copy(
                        targetExtractTextField = _state.value.targetExtractTextField.copy(
                            textField = intent.value,
                            isHintVisible = intent.value.text.isBlank()
                        )
                    )
                }
                is MainIntent.OnGetResult -> viewModelScope.launch(Dispatchers.IO) {
                    val serversResults : Server? = MoonGetter
                        .initialize(context = intent.context)
                        .setTimeout(12000)
                        .setEngine(engine)
                        .get(intent.url ?: return@launch)

                    _state.value = state.value.copy(
                        streamPlaylist = serversResults?.videos ?: throw InvalidServerException(
                            "No videos found"
                        )
                    )
                }

                is MainIntent.OnGetResults -> viewModelScope.launch(Dispatchers.IO) {
                    val serversResults : List<Server> = MoonGetter
                        .initialize(context = intent.context)
                        .setTimeout(12000)
                        .setEngine(engine)
                        .get(intent.urls)

                    _state.value = state.value.copy(
                        streamPlaylist = serversResults.flatMap { it.videos }
                    )
                }
                is MainIntent.OnGetUntilFindNewResult -> viewModelScope.launch(Dispatchers.IO) {
                    val serversResults : Server? = MoonGetter
                        .initialize(context = intent.context)
                        .setTimeout(12000)
                        .setEngine(engine)
                        .getUntilFindResource(intent.urls)

                    _state.value = state.value.copy(
                        streamPlaylist = serversResults?.videos ?: throw InvalidServerException(
                            "No videos found"
                        )
                    )
                }
                is MainIntent.OnSelectedUrl -> {
                    _state.value = state.value.copy(
                        selectedStream = intent.request
                    )
                }
            }
        } catch (e : InvalidServerException) {
            Log.d("MOON_ERROR", e.message ?: "unknown error")
        }
        catch (e : IOException) {
            Log.d("MOON_ERROR", e.message ?: "unknown error")
        }
        catch (e : RuntimeException) {
            Log.d("MOON_ERROR", e.message ?: "unknown error")
        }

    }
}