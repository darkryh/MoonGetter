package com.ead.project.moongetter.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ead.lib.moongetter.models.Request

@Composable
expect fun Player(
    modifier: Modifier = Modifier,
    request: Request
)