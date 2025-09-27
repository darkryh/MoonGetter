package com.ead.project.moongetter.app.system.extensions

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Modifier.onHideKeyboard(
    softKeyboardController : SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
) : Modifier {
    return this
        .clickable(
            onClick = { softKeyboardController?.hide() },
            indication = null,
            interactionSource = null
        )
}

@Composable
fun Modifier.shimmerEffect(
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant
    ),
    durationMillis: Int = 1200,
    shimmerAngle: Float = 20f,
    shape: Shape = RectangleShape
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition()
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis)
        )
    )

    val angleRad = shimmerAngle * (PI / 180).toFloat()
    val dx = cos(angleRad) * size.width
    val dy = sin(angleRad) * size.height

    this
        .onGloballyPositioned { size = it.size }
        .clip(shape)
        .drawWithContent {
            drawContent()
            if (size.width > 0 && size.height > 0) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(startOffsetX, -dy),
                        end = Offset(startOffsetX + dx, size.height.toFloat() + dy)
                    ),
                    size = size.toSize()
                )
            }
        }
}

fun Modifier.onFocusOutOfContext(focusManager: FocusManager): Modifier = composed {
    clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
        focusManager.clearFocus()
    }
}
