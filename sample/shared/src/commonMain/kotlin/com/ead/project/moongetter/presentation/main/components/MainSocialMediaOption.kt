package com.ead.project.moongetter.presentation.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ead.project.moongetter.app.system.extensions.minus

@Composable
fun MainSocialMediaOption(
    modifier: Modifier = Modifier,
    modifierIcon : Modifier = Modifier,
    painter : Painter,
    contentDescription : String?,
    text : String,
    onClick : () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = modifierIcon,
                painter = painter,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.inverseSurface
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "| $text",
                color = MaterialTheme.colorScheme.inverseSurface,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize.minus(1.sp),
                fontWeight = FontWeight.W400
            )
        }
    }
}