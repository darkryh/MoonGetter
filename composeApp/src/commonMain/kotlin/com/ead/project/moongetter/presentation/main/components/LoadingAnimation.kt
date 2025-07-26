package com.ead.project.moongetter.presentation.main.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    spacing : Dp = 8.dp,
    size : Dp = 16.dp
) {
    val scales = listOf(
        remember { Animatable(1f) },
        remember { Animatable(1f) },
        remember { Animatable(1f) },
        remember { Animatable(1f) },
        remember { Animatable(1f) }
    )

    scales.forEachIndexed { index, scale ->
        LaunchedEffect(Unit) {
            delay(index * 100L)
            scale.animateTo(
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        scales.forEach { scale ->
            Spacer(modifier = Modifier.width(spacing))
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(scale.value)
                    .background(Color.Gray, shape = MaterialTheme.shapes.large)
            )
            Spacer(modifier = Modifier.width(spacing))
        }
    }
}