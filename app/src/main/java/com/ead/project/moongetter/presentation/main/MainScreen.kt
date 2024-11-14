package com.ead.project.moongetter.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ead.project.moongetter.R
import com.ead.project.moongetter.presentation.main.components.LoadingAnimation
import com.ead.project.moongetter.presentation.main.components.OptionsResult
import com.ead.project.moongetter.presentation.main.components.TextField
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.intent.NetworkIntent
import com.ead.project.moongetter.presentation.main.intent.TextIntent
import com.ead.project.moongetter.presentation.main.state.MainState
import com.ead.project.moongetter.presentation.theme.MoonGetterTheme
import com.ead.project.moongetter.presentation.util.AboutUs.GITHUB
import com.ead.project.moongetter.presentation.util.IntentUtil
import com.ead.test.media3player.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    intent : (MainIntent) -> Unit,
    state: MainState
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(text = "MoonGetter")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "A lib for android",
                            fontSize = 11.sp,
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { IntentUtil.goIntentTo(context, GITHUB) }
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(30.dp),
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = "Search url to extract icon",
                            tint = MaterialTheme.colorScheme.inverseSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(
                            vertical = 0.dp,
                            horizontal = 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier
                            .weight(1f),
                        value = state.targetExtractTextField.textField,
                        onValueChange = { intent(TextIntent.EnteredTargetSearch(it)) },
                        onFocusChange = { intent(TextIntent.ChangeSearchFocus(it.isFocused)) },
                        hint = state.targetExtractTextField.hint,
                        isHintVisible = state.targetExtractTextField.isHintVisible,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                intent(NetworkIntent.OnGetResult(context, state.targetExtractTextField.textField.text))
                            }
                        ),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.inverseSurface
                        ),
                        hintTextStyle = TextStyle(
                            color = MaterialTheme.colorScheme.inverseSurface
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            intent(NetworkIntent.OnGetResult(context, state.targetExtractTextField.textField.text))
                            keyboardController?.hide()
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(start = 8.dp),
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search url to extract icon",
                            tint = MaterialTheme.colorScheme.inverseSurface
                        )
                    }
                }
            }

            if (state.isLoading) {
                LoadingAnimation(
                    modifier = Modifier
                        .height(128.dp)
                        .padding(16.dp),
                    spacing = 12.dp
                )
            }

            if (!state.isLoading) {
                state.selectedStream?.let {
                    Player(
                        modifier = Modifier,
                        request = it
                    )
                    OptionsResult(
                        modifier = Modifier
                            .fillMaxWidth(),
                        videos = state.streamPlaylist,
                        intent = intent
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MoonGetterTheme {
        MainScreen(
            modifier = Modifier
                .fillMaxSize(),
            snackbarHostState = SnackbarHostState(),
            intent = {},
            state = MainState()
        )
    }
}