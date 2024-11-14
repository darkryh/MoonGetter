@file:OptIn(ExperimentalFeature::class)

package com.ead.project.moongetter.presentation.main

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
import com.ead.project.moongetter.app.network.util.onError
import com.ead.project.moongetter.app.network.util.onSuccess
import com.ead.project.moongetter.app.system.extensions.onGetResult
import com.ead.project.moongetter.app.system.extensions.onGetResults
import com.ead.project.moongetter.app.system.extensions.onGetUntilFindResult
import com.ead.project.moongetter.domain.custom_servers.factory.SenvidModifiedFactory
import com.ead.project.moongetter.presentation.main.event.MainEvent
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.intent.NetworkIntent
import com.ead.project.moongetter.presentation.main.intent.SelectionIntent
import com.ead.project.moongetter.presentation.main.intent.TextIntent
import com.ead.project.moongetter.presentation.main.state.MainState
import com.ead.project.moongetter.presentation.util.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

        when(intent) {
            is TextIntent.EnteredTargetSearch -> {
                _state.value = state.value.copy(
                    targetExtractTextField = _state.value.targetExtractTextField.copy(
                        textField = intent.value,
                        isHintVisible = intent.value.text.isBlank()
                    )
                )
            }
            is TextIntent.ChangeSearchFocus -> {
                _state.value = state.value.copy(
                    targetExtractTextField = _state.value.targetExtractTextField.copy(
                        isHintVisible = !intent.isFocused && state.value.targetExtractTextField.textField.text.isBlank()
                    )
                )
            }
            is NetworkIntent.OnGetResult -> viewModelScope.launch(Dispatchers.IO) {
                loadingState()

                MoonGetter
                    .initialize(context = intent.context)
                    .setTimeout(12000)
                    .setEngine(engine)
                    .onGetResult<Server>(intent.url ?: return@launch)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamPlaylist = it.videos,
                            selectedStream = it.videos.firstOrNull()?.request ?: return@onSuccess,
                            isLoading = false
                        )
                    }
                    .onError {
                        loadingState(false)
                        _event.emit(MainEvent.Notify(Message.Error(it.toString())))

                    }
            }

            is NetworkIntent.OnGetResults -> viewModelScope.launch(Dispatchers.IO) {
                loadingState()

                MoonGetter
                    .initialize(context = intent.context)
                    .setTimeout(12000)
                    .setEngine(engine)
                    .onGetResults<List<Server>>(intent.urls)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamPlaylist = it.flatMap { it.videos },
                            selectedStream = it.firstOrNull()?.videos?.firstOrNull()?.request ?: return@onSuccess,
                            isLoading = false
                        )
                    }
                    .onError {
                        _event.emit(MainEvent.Notify(Message.Error(it.toString())))
                        loadingState(false)
                    }
            }
            is NetworkIntent.OnGetUntilFindNewResult -> viewModelScope.launch(Dispatchers.IO) {
                loadingState()

                MoonGetter
                    .initialize(context = intent.context)
                    .setTimeout(12000)
                    .setEngine(engine)
                    .onGetUntilFindResult<Server>(intent.urls)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamPlaylist = it.videos,
                            selectedStream = it.videos.firstOrNull()?.request ?: return@onSuccess,
                            isLoading = false
                        )
                    }
                    .onError {
                        _event.emit(MainEvent.Notify(Message.Error(it.toString())))
                        loadingState(false)
                    }
            }
            is SelectionIntent.OnSelectedUrl -> {
                _state.value = state.value.copy(
                    selectedStream = intent.request
                )
            }
        }
    }

    private fun loadingState(value: Boolean = true) {
        _state.value = state.value.copy(
            isLoading = value
        )
    }
}