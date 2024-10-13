package com.ead.project.moongetter.main

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.models.ServerIntegration
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.project.moongetter.domain.custom_servers.SenvidModified
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

    private val _mediaUrlSelected : MutableState<String?> = mutableStateOf(null)
    val mediaUrlSelected : State<String?> = _mediaUrlSelected

    private val your1FichierToken = "Your 1fichier token"

    fun onEvent(event: MainEvent) {
        viewModelScope.launch (Dispatchers.IO) {
            try {

                when(event) {
                    is MainEvent.OnNewResult -> {

                        val serversResults = MoonGetter
                            .initialize(context = event.context)
                            .connect(url = event.url ?: return@launch)
                            .set1FichierToken(your1FichierToken)
                            .setCustomServers(
                                listOf(
                                     ServerIntegration(
                                        serverClass = SenvidModified::class.java,
                                        pattern = """https://custom\.domain\.com/aqua/sv\?url=([^&]+)"""
                                    )
                                )
                            )
                            .get()

                        _messageResult.value = serversResults?.videos ?: emptyList()
                    }

                    is MainEvent.OnUntilFindNewResult -> {

                        val serversResults = MoonGetter
                            .initialize(context = event.context)
                            .connect(urls = event.urls)
                            .set1FichierToken(your1FichierToken)
                            .setCustomServers(
                                listOf(
                                    ServerIntegration(
                                        serverClass = SenvidModified::class.java,
                                        pattern = """https://custom\.domain\.com/aqua/sv\?url=([^&]+)"""
                                    )
                                )
                            )
                            .getUntilFindResource()

                        _messageResult.value = serversResults?.videos ?: emptyList()

                    }

                    is MainEvent.OnNewResults -> {

                        val serversResults = MoonGetter
                            .initialize(context = event.context)
                            .connect(urls = event.urls)
                            .set1FichierToken(your1FichierToken)
                            .setCustomServers(
                                listOf(
                                    ServerIntegration(
                                        serverClass = SenvidModified::class.java,
                                        pattern = """https://custom\.domain\.com/aqua/sv\?url=([^&]+)"""
                                    )
                                )
                            )
                            .getList()

                        _messageResult.value = serversResults
                            .flatMap { it.videos }

                    }

                    is MainEvent.OnSelectedUrl -> {
                        _mediaUrlSelected.value = event.url
                    }
                }

                if (messageResult.value.isNotEmpty() && event !is MainEvent.OnSelectedUrl) {
                   _mediaUrlSelected.value = messageResult.value.first().request.url
                }

            } catch (e : InvalidServerException) {
                _eventFlow.emit(UiEvent.ShowSnackBar(message = e.message ?: "unknown error"))
            }
            catch (e : IOException) {
                _eventFlow.emit(UiEvent.ShowSnackBar(message = e.message ?: "unknown error"))
            }
            catch (e : RuntimeException) {
                _eventFlow.emit(UiEvent.ShowSnackBar(message = e.message ?: "unknown error"))
            }
        }

    }

    sealed class UiEvent {
        data class ShowSnackBar(val message : String,val actionLabel : String?=null ,val duration : SnackbarDuration = SnackbarDuration.Short) : UiEvent()
    }
}