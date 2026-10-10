package com.ead.project.moongetter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ead.project.moongetter.presentation.main.MainScreen
import com.ead.project.moongetter.presentation.main.MainViewModel
import com.ead.project.moongetter.presentation.main.event.MainEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val viewModel = koinViewModel<MainViewModel>()

        LaunchedEffect(Unit) {
            viewModel.event.collectLatest { event ->
                when (event) {
                    is MainEvent.Notify -> {
                        snackbarHostState.showSnackbar(
                            message = event.message.data,
                            duration = SnackbarDuration.Indefinite,
                            withDismissAction = true
                        )
                    }
                }
            }
        }

        val mainScreen = viewModel.state.collectAsStateWithLifecycle()

        MainScreen(
            modifier = Modifier.fillMaxSize(),
            snackbarHostState = snackbarHostState,
            intent = viewModel::onIntent,
            state = mainScreen.value
        )
    }
}