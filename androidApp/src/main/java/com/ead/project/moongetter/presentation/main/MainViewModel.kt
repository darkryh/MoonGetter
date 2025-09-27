@file:OptIn(ExperimentalFeature::class)

package com.ead.project.moongetter.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.project.moongetter.presentation.main.event.MainEvent
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.intent.NetworkIntent
import com.ead.project.moongetter.presentation.main.intent.SelectionIntent
import com.ead.project.moongetter.presentation.main.intent.TextIntent
import com.ead.project.moongetter.presentation.main.state.MainState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    val moonFactoryBuilder : Factory.Builder,
) : ViewModel() {

    private val _state : MutableStateFlow<MainState> = MutableStateFlow(MainState())
    val state : StateFlow<MainState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<MainEvent>()
    val event = _event.asSharedFlow()

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
            }

            is NetworkIntent.OnGetResults -> viewModelScope.launch(Dispatchers.IO) {
                loadingState()

            }
            is NetworkIntent.OnGetUntilFindNewResult -> viewModelScope.launch(Dispatchers.IO) {
                loadingState()

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