@file:Suppress("UNUSED")

package com.ead.project.moongetter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ead.lib.moongetter.models.Video
import com.ead.project.moongetter.presentation.main.MainEvent
import com.ead.project.moongetter.presentation.main.MainViewModel
import com.ead.project.moongetter.presentation.theme.MoonGetterTheme
import com.ead.test.media3player.Player
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel : MainViewModel by viewModels()

    private val customUrl = "https://custom.domain.com/aqua/sv?url=https://sendvid.com/k555oewr"

    private val exampleCollectedVideosFromInternet = listOf(
        customUrl,
        "https://x.com/_BestVideos/status/1795579616243184035",
        "https://www.facebook.com/lassomusica/videos/el-video-mas-lindo-que-veras-hoy%EF%B8%8F/488188289893329/",
        "https://sendvid.com/k555oewr",
        "https://hexload.com/embed-4jq8u2fwodsm.html",
        "https://flaswish.com/e/dhfe39jcywcr",
        "https://1cloudfile.com/L92p",
        //"https://goodstream.one/video/embed/0g936frff64s",
        "https://filemoon.sx/e/cvnd9zqj2i9w",
        "https://listeamed.net/e/Ro7kOVMVgkEZWBn",
        "https://streamtape.com/e/0V8ALXqzvVsb1vg",
        "https://vidhidepro.com/v/41n252u058ws",
        "https://voe.sx/e/bfydr4fmm7jp",
        "https://pixeldrain.com/u/VbW82s5W",
        "https://www.mediafire.com/file/6gwdbflmrgxe3ta",
        "http://ok.ru/videoembed/1411667855965",
        "https://drive.google.com/file/d/0B-bHILyvaZQQNXo5cWpXMjRhZ2M/view?usp=drive_link&resourcekey=0-e4kDmIKPCj24fnzZC0fX0A"
    )


    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
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
                        /**
                         * Example use case to find the first resource possible or null
                         */
                        /*viewModel.onEvent(
                            event = MainEvent.OnUntilFindNewResult(
                                context = this@MainActivity as Context,
                                urls = exampleCollectedVideosFromInternet
                            )
                        )*/

                        /**
                         * Example use case to find resources from a specific url
                         */
                        viewModel.onEvent(
                            event = MainEvent.OnNewResult(
                                context = this@MainActivity as Context,
                                url = "https://flaswish.com/e/dhfe39jcywcr"
                            )
                        )

                        /**
                         * Example use case to find all possible resources from a list of urls
                         */
                        /*viewModel.onEvent(
                            event = MainEvent.OnNewResults(
                                context = this@MainActivity as Context,
                                urls = exampleCollectedVideosFromInternet
                            )
                        )*/
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize(),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            stickyHeader {
                                Player(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    request = viewModel.mediaUrlSelected.value
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            item {
                                MessageResult(
                                    videos = viewModel.messageResult.value,
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    event = viewModel::onEvent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageResult(videos : List<Video>, modifier: Modifier = Modifier, event: (MainEvent) -> Unit) {

    if (videos.isEmpty()) {
        Text(
            text = "Loading Resource..."
        )
        return
    }

    val context = LocalContext.current

    Column(
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = "Click Any Option to Reproduce",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        videos.forEach { file ->
            Text(
                text = if(file.quality != null) {
                    "Quality : " + file.quality
                } else {
                    "Quality unknown"
                }
            )
            Spacer(
                modifier = Modifier.height(8.dp)
            )
            Text(
                modifier = Modifier
                    .clickable {

                        /**
                         * Copy url to the clipboard
                         */

                        val clipboard: ClipboardManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.getSystemService(ClipboardManager::class.java)
                        } else {
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        }
                        val clip = ClipData.newPlainText("Text copied", file.request.url)

                        clipboard.setPrimaryClip(clip)
                        event(MainEvent.OnSelectedUrl(request = file.request))
                    },
                text = "downloadUrl : " + file.request.url,
                maxLines = 4
            )
            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
        Text(text = "developed by Darkryh")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MoonGetterTheme {
        MessageResult(
            emptyList(),
            event = {}
        )
    }
}