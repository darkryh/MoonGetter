package com.ead.project.moongetter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ead.lib.moongetter.models.File
import com.ead.project.moongetter.main.MainEvent
import com.ead.project.moongetter.main.MainViewModel
import com.ead.project.moongetter.presentation.theme.MoonGetterTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel : MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoonGetterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(key1 = true) {
                        viewModel.eventFlow.collectLatest { event ->
                            when(event) {
                                is MainViewModel.UiEvent.ShowSnackBar ->
                                    snackbarHostState.showSnackbar(
                                        message = event.message,
                                        actionLabel = event.actionLabel,
                                        duration = event.duration
                                    )
                            }
                        }
                    }

                    LaunchedEffect(key1 = true) {
                        viewModel.onEvent(
                            event = MainEvent.OnNewResult(
                                context = this@MainActivity as Context,
                                url = "https://streamtape.com/e/vLOarxD7LzsD0J"
                            )
                        )
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize(),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->

                        MessageResult(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize(),
                            files = viewModel.messageResult.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageResult(files : List<File>, modifier: Modifier = Modifier) {

    if (files.isEmpty()) {
        Text(
            text = "Loading Resource..."
        )
        return
    }

    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
    ) {
        item {
            Spacer(
                modifier = Modifier.height(32.dp)
            )
        }
        items(files) { file ->
            Text(
                text = "title : " + file.title
            )
            Spacer(
                modifier = Modifier.height(8.dp)
            )
            Text(
                modifier = Modifier
                    .clickable {

                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val clip = ClipData.newPlainText("Text copied", file.url)

                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                text = "downloadUrl : " + file.url,
                maxLines = 4
            )
            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MoonGetterTheme {
        MessageResult(emptyList())
    }
}