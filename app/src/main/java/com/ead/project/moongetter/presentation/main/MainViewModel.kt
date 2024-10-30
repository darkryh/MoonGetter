package com.ead.project.moongetter.presentation.main

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.models.Request
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel : ViewModel() {

    private val _messageResult : MutableState<List<Video>> = mutableStateOf(emptyList())
    val messageResult : State<List<Video>> = _messageResult

    private val _eventFlow : MutableSharedFlow<UiEvent> = MutableSharedFlow()
    val eventFlow : SharedFlow<UiEvent> = _eventFlow

    private val _mediaUrlSelected : MutableState<Request?> = mutableStateOf(null)
    val mediaUrlSelected : State<Request?> = _mediaUrlSelected

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

    fun onEvent(event: MainEvent) {
        viewModelScope.launch (Dispatchers.IO) {
            try {

                when(event) {
                    is MainEvent.OnNewResult -> {

                        val serversResults : Server? = MoonGetter
                            .initialize(context = event.context)
                            .setTimeout(12000)
                            .setEngine(engine)
                            .setHeaders(
                                mapOf(
                                    "User-Agent" to "Mozilla/5.0"
                                )
                            )
                            .get(event.url ?: return@launch)

                        _messageResult.value = serversResults?.videos ?: emptyList()
                    }

                    is MainEvent.OnUntilFindNewResult -> {

                        val serversResults : Server? = MoonGetter
                            .initialize(context = event.context)
                            .setTimeout(12000)
                            .setEngine(engine)
                            .setHeaders(
                                mapOf(
                                    "User-Agent" to "Mozilla/5.0"
                                )
                            )
                            .getUntilFindResource(event.urls)

                        _messageResult.value = serversResults?.videos ?: emptyList()
                    }

                    is MainEvent.OnNewResults -> {

                        val serversResults : List<Server> = MoonGetter
                            .initialize(context = event.context)
                            .setTimeout(12000)
                            .setEngine(engine)
                            .setHeaders(
                                mapOf(
                                    "User-Agent" to "Mozilla/5.0"
                                )
                            )
                            .get(event.urls)

                        _messageResult.value = serversResults
                            .flatMap { it.videos }
                    }

                    is MainEvent.OnSelectedUrl -> {
                        _mediaUrlSelected.value = event.request
                    }
                }

                if (messageResult.value.isNotEmpty() && event !is MainEvent.OnSelectedUrl) {
                    _mediaUrlSelected.value = messageResult.value.first().request
                }

            } catch (e : InvalidServerException) {
                Log.d("MOON_ERROR", e.message ?: "unknown error")
                _eventFlow.emit(UiEvent.ShowSnackBar(message = e.message ?: "unknown error"))
            }
            catch (e : IOException) {
                Log.d("MOON_ERROR", e.message ?: "unknown error")
                _eventFlow.emit(UiEvent.ShowSnackBar(message = e.message ?: "unknown error"))
            }
            catch (e : RuntimeException) {
                Log.d("MOON_ERROR", e.message ?: "unknown error")
                _eventFlow.emit(UiEvent.ShowSnackBar(message = e.message ?: "unknown error"))
            }
        }

    }

    sealed class UiEvent {
        data class ShowSnackBar(val message : String,val actionLabel : String?=null ,val duration : SnackbarDuration = SnackbarDuration.Short) : UiEvent()
    }
}