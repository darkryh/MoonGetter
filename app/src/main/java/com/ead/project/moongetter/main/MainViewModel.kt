package com.ead.project.moongetter.main

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.models.File
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _messageResult : MutableState<List<File>> = mutableStateOf(emptyList())
    val messageResult : State<List<File>> = _messageResult

    private val _eventFlow : MutableSharedFlow<UiEvent> = MutableSharedFlow()
    val eventFlow : SharedFlow<UiEvent> = _eventFlow

    fun onEvent(event: MainEvent) {
        when(event) {
            is MainEvent.OnNewResult -> {
                viewModelScope.launch (Dispatchers.IO) {

                    try {
                        val serverResult = MoonGetter
                            .initialize(context = event.context)
                            .connect(url = event.url)
                            .set1FichierToken("gBCZMnGJuFEdfX6kPkITMSS1hw=ieE_i")
                            .get()

                        _messageResult.value = serverResult.files

                    } catch (e : InvalidServerException) {
                        _eventFlow.emit(UiEvent.ShowSnackBar(message = e.message ?: "unknown error"))
                    }
                }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackBar(val message : String,val actionLabel : String?=null ,val duration : SnackbarDuration = SnackbarDuration.Short) : UiEvent()
    }
}