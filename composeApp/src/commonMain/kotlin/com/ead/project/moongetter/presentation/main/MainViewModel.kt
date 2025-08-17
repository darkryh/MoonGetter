@file:OptIn(ExperimentalFeature::class)

package com.ead.project.moongetter.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ead.lib.moongetter.MoonFactory
import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.project.moongetter.app.lib.cookieManagement
import com.ead.project.moongetter.app.lib.httpClientEngineFactory
import com.ead.project.moongetter.app.lib.trustManager
import com.ead.project.moongetter.app.network.util.MoonGetterError
import com.ead.project.moongetter.app.network.util.onError
import com.ead.project.moongetter.app.network.util.onSuccess
import com.ead.project.moongetter.app.system.extensions.onGetResult
import com.ead.project.moongetter.app.system.extensions.onGetResults
import com.ead.project.moongetter.app.system.extensions.onGetUntilFindResult
import com.ead.project.moongetter.presentation.main.event.MainEvent
import com.ead.project.moongetter.presentation.main.event.MainEvent.*
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.intent.NetworkIntent
import com.ead.project.moongetter.presentation.main.intent.SelectionIntent
import com.ead.project.moongetter.presentation.main.intent.TextIntent
import com.ead.project.moongetter.presentation.main.state.MainState
import com.ead.project.moongetter.presentation.util.Message.*
import com.ead.project.moongetter.presentation.util.TextFieldState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moongetter.composeapp.generated.resources.Res
import moongetter.composeapp.generated.resources.play_text_field_hint
import org.jetbrains.compose.resources.getString

class MainViewModel(
    private val engine: Engine,
) : ViewModel() {

    private val _state : MutableStateFlow<MainState> = MutableStateFlow(MainState())
    val state : StateFlow<MainState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<MainEvent>()
    val event = _event.asSharedFlow()

    init {
        MoonGetter.start(
            MoonFactory
                .Builder()
                .setClient(
                    client = KtorMoonClient(
                        engineFactory = httpClientEngineFactory,
                        cookieManagement = cookieManagement,
                        trustManager = trustManager
                    )
                )
                .setTimeout(timeoutMillis = 12000)
                .setEngine(engine = engine)
        )

        viewModelScope.launch(Dispatchers.Default) {
            _state.value = state.value.copy(
                targetExtractTextField = TextFieldState(
                    hint = getString(Res.string.play_text_field_hint)
                )
            )
        }
    }

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
                    .onGetResult<Server>(intent.url ?: return@launch)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamPlaylist = it.videos,
                            selectedStream = it.videos.firstOrNull()?.request ?: return@onSuccess,
                            isLoading = false
                        )
                    }
                    .onError { moonGetterError ->
                        handleError(moonGetterError)
                        loadingState(false)
                    }
            }

            is NetworkIntent.OnGetResults -> viewModelScope.launch(Dispatchers.IO) {
                loadingState()

                MoonGetter
                    .onGetResults<List<Server>>(intent.urls)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamPlaylist = it.flatMap { it.videos },
                            selectedStream = it.firstOrNull()?.videos?.firstOrNull()?.request ?: return@onSuccess,
                            isLoading = false
                        )
                    }
                    .onError { moonGetterError ->
                        handleError(moonGetterError)
                        loadingState(false)
                    }
            }
            is NetworkIntent.OnGetUntilFindNewResult -> viewModelScope.launch(Dispatchers.IO) {
                loadingState()

                MoonGetter
                    .onGetUntilFindResult<Server>(intent.urls)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamPlaylist = it.videos,
                            selectedStream = it.videos.firstOrNull()?.request ?: return@onSuccess,
                            isLoading = false
                        )
                    }
                    .onError { moonGetterError ->
                        handleError(moonGetterError)
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

    private suspend fun handleError(moonGetterError: MoonGetterError) {
        when(moonGetterError) {
            MoonGetterError.RESOURCE_TAKEN_DOWN -> {
                _event.emit(Notify(Error("Resource taken down")))
            }
            MoonGetterError.INVALID_PARAMETERS_IN_BUILDER -> {
                _event.emit(Notify(Error("Invalid parameters in builder")))
            }
            MoonGetterError.NO_PARAMETERS_TO_WORK -> {
                _event.emit(Notify(Error("The textfield doesn't need to be empty, insert embed url video from streaming site")))
            }
            MoonGetterError.SERVERS_RESPONSE_WENT_WRONG -> {
                _event.emit(Notify(Error("Servers response went wrong, try again")))
            }
            MoonGetterError.SERVER_ERROR -> {
                _event.emit(Notify(Error("Server error, try another time")))
            }
            MoonGetterError.REQUEST_TIMEOUT -> {
                _event.emit(Notify(Error("Request timeout, check internet connection")))
            }
            MoonGetterError.UNAUTHORIZED -> {
                _event.emit(Notify(Error("Unauthorized request")))
            }
            MoonGetterError.CONFLICT -> {
                _event.emit(Notify(Error("Conflict connexion")))
            }
            MoonGetterError.TOO_MANY_REQUESTS -> {
                _event.emit(Notify(Error("Too many requests detected, try another time")))
            }
            MoonGetterError.NO_INTERNET -> {
                _event.emit(Notify(Error("No internet connexion")))
            }
            MoonGetterError.PAYLOAD_TOO_LARGE -> {
                _event.emit(Notify(Error("Payload too large")))
            }
            MoonGetterError.NOT_RECOGNIZED_URL -> {
                _event.emit(Notify(Error("Embed url not supported or invalid")))

            }
            MoonGetterError.UNKNOWN -> {
                _event.emit(Notify(Error("Unknown error")))
            }
        }
    }

    private fun loadingState(value: Boolean = true) {
        _state.value = state.value.copy(
            isLoading = value
        )
    }
}