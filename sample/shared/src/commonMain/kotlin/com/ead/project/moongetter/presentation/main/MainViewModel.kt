@file:OptIn(ExperimentalFeature::class)

package com.ead.project.moongetter.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ead.project.moongetter.app.network.TimedMoonClient
import com.ead.project.moongetter.app.SampleConfiguration
import com.ead.lib.moongetter.MoonFactory
import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.builder.Engine
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
import com.ead.project.moongetter.presentation.main.event.MainEvent.Notify
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.intent.NetworkIntent
import com.ead.project.moongetter.presentation.main.intent.SelectionIntent
import com.ead.project.moongetter.presentation.main.intent.TextIntent
import com.ead.project.moongetter.presentation.main.model.MainModelState
import com.ead.project.moongetter.presentation.main.state.MainState
import com.ead.project.moongetter.presentation.util.Message.Error
import com.ead.project.moongetter.presentation.util.TextFieldState
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.update
import moongetter.composeapp.generated.resources.Res
import moongetter.composeapp.generated.resources.play_text_field_hint
import org.jetbrains.compose.resources.getString

class MainViewModel(
    private val engine: Engine,
) : ViewModel() {

    private val _state : MutableStateFlow<MainState> = MutableStateFlow(MainState())
    val state : StateFlow<MainState> = _state.asStateFlow()

    private var extractionJob: Job? = null

    private val _event = MutableSharedFlow<MainEvent>()
    val event = _event.asSharedFlow()

    init {
        MoonGetter.start(
            MoonFactory
                .Builder()
                .setClient(
                    client = TimedMoonClient(KtorMoonClient(
                        engineFactory = httpClientEngineFactory,
                        cookieManagement = cookieManagement,
                        trustManager = trustManager
                    ))
                )
                .setTimeout(timeoutMillis = SampleConfiguration.requestTimeoutMillis)
                .setEngine(engine = engine)
        )

        viewModelScope.launch(Dispatchers.Default) {
            _state.value = state.value.copy(
                targetExtractTextField = state.value.targetExtractTextField.copy(
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
            is NetworkIntent.OnGetResult -> extract {
                loadingState()

                MoonGetter
                    .onGetResult<Server>(intent.url ?: throw IllegalArgumentException("Missing URL"))
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamServerName = it.name,
                            streamPlaylist = it.videos,
                            selectedStream = it.videos.firstOrNull()?.request ?: throw IllegalStateException("Provider returned no playable videos"),
                            mainModelState = MainModelState.PLAYING
                        )
                    }
                    .onError { moonGetterError ->
                        loadingState(mainModelState = MainModelState.ERROR_SEARCHING)
                        handleError(moonGetterError)
                    }
            }

            is NetworkIntent.OnGetResults -> extract {
                loadingState()

                MoonGetter
                    .onGetResults<List<Server>>(intent.urls)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamServerName = "Various",
                            streamPlaylist = it.flatMap { it.videos },
                            selectedStream = it.firstOrNull()?.videos?.firstOrNull()?.request ?: throw IllegalStateException("Provider returned no playable videos"),
                            mainModelState = MainModelState.PLAYING
                        )
                    }
                    .onError { moonGetterError ->
                        loadingState(mainModelState = MainModelState.ERROR_SEARCHING)
                        handleError(moonGetterError)
                    }
            }
            is NetworkIntent.OnGetUntilFindNewResult -> extract {
                loadingState()

                MoonGetter
                    .onGetUntilFindResult<Server>(intent.urls)
                    .onSuccess {
                        _state.value = state.value.copy(
                            streamServerName = it.name,
                            streamPlaylist = it.videos,
                            selectedStream = it.videos.firstOrNull()?.request ?: throw IllegalStateException("Provider returned no playable videos"),
                            mainModelState = MainModelState.PLAYING
                        )
                    }
                    .onError { moonGetterError ->
                        loadingState(mainModelState = MainModelState.ERROR_SEARCHING)
                        handleError(moonGetterError)
                    }
            }
            is SelectionIntent.OnSelectedUrl -> {
                _state.value = state.value.copy(
                    selectedStream = intent.request
                )
            }
            is SelectionIntent.Searching -> {
                _state.value = state.value.copy(
                    shouldShowSearchingServer = state.value.shouldShowSearchingServer.not()
                )
            }
            is SelectionIntent.MoreServerInfo -> {
                _state.value = state.value.copy(
                    shouldShowMoreInfoAboutSupportedServer = state.value.shouldShowMoreInfoAboutSupportedServer.not()
                )
            }
            is SelectionIntent.MoreVideoInfo -> {
                _state.value = state.value.copy(
                    shouldShowMoreInfoAboutVideo = state.value.shouldShowMoreInfoAboutVideo.not()
                )
            }
            is SelectionIntent.InfoDetailActivated -> {
                _state.value = state.value.copy(
                    isInfoDetailActivated = state.value.isInfoDetailActivated.not()
                )
            }
        }
    }

    private fun extract(block: suspend () -> Unit) {
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch(Dispatchers.IO) {
            loadingState()
            val started = TimeSource.Monotonic.markNow()
            try {
                for (request in state.value.streamPlaylist.map { it.request }) {
                    if (request.url.startsWith("http://127.0.0.1:")) {
                        com.ead.lib.moongetter.mega.MegaProxy.release(request.url)
                        com.ead.lib.moongetter.abyss.AbyssProxy.release(request.url)
                    }
                }
                withTimeout(SampleConfiguration.requestTimeoutMillis) { block() }
            } catch (_: TimeoutCancellationException) {
                loadingState(MainModelState.ERROR_SEARCHING)
                handleError(MoonGetterError.REQUEST_TIMEOUT)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                loadingState(MainModelState.ERROR_SEARCHING)
                println("MoonGetter extraction failed: ${failure::class.simpleName}")
                handleError(MoonGetterError.UNKNOWN)
            } finally {
                println("MoonGetter extraction duration: ${started.elapsedNow()}")
                if (currentCoroutineContext().isActive) _state.update {
                    if (it.mainModelState == MainModelState.SEARCHING)
                        it.copy(mainModelState = MainModelState.ERROR_SEARCHING)
                    else it
                }
            }
        }
    }

    private suspend fun handleError(moonGetterError: MoonGetterError) {
        when(moonGetterError) {
            MoonGetterError.PROVIDER_VERIFICATION_REQUIRED -> {
                _event.emit(Notify(Error("The provider requires verification before returning a video source.")))
            }
            MoonGetterError.EXTRACTION_FAILED -> {
                _event.emit(Notify(Error("Could not extract the video source. The provider may require verification or an extractor update.")))
            }
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

    override fun onCleared() {
        state.value.streamPlaylist.map { it.request.url }.filter { it.startsWith("http://127.0.0.1:") }
            .forEach { com.ead.lib.moongetter.mega.MegaProxy.releaseAsync(it); com.ead.lib.moongetter.abyss.AbyssProxy.releaseAsync(it) }
        super.onCleared()
    }

    private fun loadingState(mainModelState: MainModelState = MainModelState.SEARCHING) {
        _state.value = state.value.copy(
            mainModelState = mainModelState
        )

        if (mainModelState == MainModelState.SEARCHING) {
            _state.value = state.value.copy(
                shouldShowSearchingServer = false
            )
        }
    }
}