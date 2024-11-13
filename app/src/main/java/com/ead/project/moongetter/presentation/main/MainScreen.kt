package com.ead.project.moongetter.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ead.project.moongetter.presentation.main.components.TextField
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.state.MainState
import com.ead.test.media3player.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    intent : (MainIntent) -> Unit,
    state: MainState
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "MoonGetter")
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(
                            8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier
                            .weight(1f),
                        value = state.targetExtractTextField.textField,
                        onValueChange = { intent(MainIntent.EnteredTargetSearch(it)) },
                        hint = state.targetExtractTextField.hint,
                        isHintVisible = state.targetExtractTextField.isHintVisible
                    )
                    Icon(
                        modifier = Modifier
                            .padding(start = 8.dp),
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search url to extract icon"
                    )
                }
            }

            state.selectedStream?.let {
                Player(
                    modifier = Modifier
                        .fillMaxSize(),
                    request = it
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(state.streamPlaylist) {
                    Text(
                        text = it.request.url
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        modifier = Modifier
            .fillMaxSize(),
        snackbarHostState = SnackbarHostState(),
        intent = {},
        state = MainState()
    )
}