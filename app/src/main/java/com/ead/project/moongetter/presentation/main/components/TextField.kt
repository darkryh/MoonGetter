package com.ead.project.moongetter.presentation.main.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun TextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    hint: String,
    isHintVisible: Boolean = true,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChange: (FocusState) -> Unit = {},
    textStyle: TextStyle = TextStyle(),
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null
) {
    Box(
        modifier = modifier
    ) {
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged(onFocusChange),
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource
        )

        if(isHintVisible)
            Text(text = hint, style = textStyle, color = Color.DarkGray)
    }
}

@Composable
@Preview(showBackground = true)
fun TextFieldPreview() {
    TextField(
        modifier = Modifier
            .fillMaxWidth(),
        value = TextFieldValue("Custom text"),
        hint = "selecting",
        isHintVisible = false,
        onValueChange = {},
        onFocusChange = {}
    )
}