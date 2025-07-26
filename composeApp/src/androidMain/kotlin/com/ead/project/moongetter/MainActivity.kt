package com.ead.project.moongetter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ead.project.moongetter.presentation.main.MainScreen
import com.ead.project.moongetter.presentation.main.MainViewModel
import com.ead.project.moongetter.presentation.main.event.MainEvent
import com.ead.project.moongetter.ui.theme.MoonGetterTheme
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    private val customUrl = "https://custom.domain.com/aqua/sv?url=https://sendvid.com/k555oewr"

    private val forTesting = listOf(
        "https://mixdrop.ps/e/gjz01w7qu77zlx"
    )

    private val specialVideos = listOf(
        "https://www.mp4upload.com/embed-a1oxgwvtpj04.html",
        "https://uqload.net/embed-px8mfh4wqk43.html",
        "https://luluvdo.com/e/wnt28htk42pg",
        "https://filemoon.sx/e/cvnd9zqj2i9w",
    )

    private val exampleCollectedVideosFromInternet = listOf(
        customUrl,
        "https://www.yourupload.com/embed/34MqL2YtFbUM",
        "https://dood.li/e/8v2r6gb9xxmh",
        "https://streamtape.com/e/0V8ALXqzvVsb1vg",
        "https://sendvid.com/k555oewr",
        "https://hexload.com/embed-4jq8u2fwodsm.html",
        "https://flaswish.com/e/dhfe39jcywcr",
        //"https://1cloudfile.com/L92p",
        //"https://goodstream.one/video/embed/0g936frff64s",
        "https://listeamed.net/e/Ro7kOVMVgkEZWBn",
        "https://voe.sx/e/bfydr4fmm7jp",
        "https://pixeldrain.com/u/VbW82s5W",
        "https://drive.google.com/file/d/0B-bHILyvaZQQNXo5cWpXMjRhZ2M/view?usp=drive_link&resourcekey=0-e4kDmIKPCj24fnzZC0fX0A",
        "https://www.mediafire.com/file/6gwdbflmrgxe3ta",
        "http://ok.ru/videoembed/1411667855965",
        "https://x.com/_BestVideos/status/1795579616243184035",
        "https://www.facebook.com/lassomusica/videos/el-video-mas-lindo-que-veras-hoy%EF%B8%8F/488188289893329/",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoonGetterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { App() }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MoonGetterTheme {
        Greeting("Android")
    }
}