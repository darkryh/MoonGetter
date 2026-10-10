package com.ead.project.moongetter

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chaintech.videoplayer.util.LocalWindowState
import com.ead.project.moongetter.app.initializeKoin
import moongetter.composeapp.generated.resources.Res
import moongetter.composeapp.generated.resources.app_name
import moongetter.composeapp.generated.resources.ic_app_desktop
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    initializeKoin()
    val windowState = rememberWindowState(width = 900.dp, height = 700.dp)
    CompositionLocalProvider(LocalWindowState provides windowState) {
        Window(
            title = stringResource(Res.string.app_name),
            state = windowState,
            onCloseRequest = ::exitApplication
        ) {
            App()
        }
    }
}