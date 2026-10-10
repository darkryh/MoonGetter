package com.ead.project.moongetter.app.system.extensions

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

fun TextUnit.minus(other: TextUnit): TextUnit = this.value.minus(other.value).sp

fun TextUnit.plus(other: TextUnit): TextUnit = this.value.plus(other.value).sp
