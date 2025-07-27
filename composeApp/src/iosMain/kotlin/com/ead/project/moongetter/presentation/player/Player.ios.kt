package com.ead.project.moongetter.presentation.player

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.ead.lib.moongetter.models.Request
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.play
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIColor
import platform.UIKit.UIView

private const val AVURLAssetHTTPHeaderFieldsKey = "AVURLAssetHTTPHeaderFieldsKey"

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Player(
    modifier: Modifier,
    request: Request
) {
    UIKitView(
        factory = {
            val view = UIView().apply {
                backgroundColor = UIColor.blackColor
            }

            val (player, playerLayer) = createPlayerFromRequest(request)

            playerLayer.frame = CGRectMake(0.0, 0.0, 300.0, 200.0)
            view.layer.addSublayer(playerLayer)

            player.play()
            view
        },
        update = { view ->
        },
        modifier = modifier
            .size(300.dp, 200.dp)
    )
}


private fun createPlayerFromRequest(request: Request): Pair<AVPlayer, AVPlayerLayer> {
    val url = NSURL.URLWithString(request.url) ?: error("Invalid URL")

    val options: Map<Any?, *>? = request.headers?.takeIf { it.isNotEmpty() }?.let { headers ->
        val headersDict = NSMutableDictionary()
        for ((key, value) in headers) {
            headersDict.setObject(value.toNSString(), forKey = key.toNSString())
        }

        @Suppress("UNCHECKED_CAST")
        mapOf(AVURLAssetHTTPHeaderFieldsKey to headersDict) as Map<Any?, *>
    }

    val asset = AVURLAsset(uRL = url, options = options)
    val item = AVPlayerItem(asset = asset)
    val player = AVPlayer(playerItem = item)
    val layer = AVPlayerLayer()
    layer.player = player

    return player to layer
}


// Extension to convert Kotlin String to NSString
@OptIn(BetaInteropApi::class)
private fun String.toNSString(): NSString = NSString.create(string = this)