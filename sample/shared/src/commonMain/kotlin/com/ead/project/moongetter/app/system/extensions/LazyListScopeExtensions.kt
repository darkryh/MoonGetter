package com.ead.project.moongetter.app.system.extensions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LazyListScope Extensions
 */

fun LazyListScope.spacedVertical(space : Dp = 8.dp) { item { Spacer(modifier = Modifier.height(space)) } }

fun LazyListScope.spacedHorizontal(space : Dp = 8.dp) { item { Spacer(modifier = Modifier.width(space)) } }
