package com.ead.project.moongetter.presentation.main.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ead.lib.moongetter.models.Video
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.intent.SelectionIntent

@Composable
fun OptionsResult(videos : List<Video>, modifier: Modifier = Modifier, intent: (MainIntent) -> Unit) {

    LazyColumn(
        modifier = modifier
            .padding(
                horizontal = 8.dp
            )
    ) {
        item{
            Spacer(
                modifier = Modifier.height(16.dp)
            )
            Text(
                text = "Click Any Option to Reproduce",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(videos) { file ->
            Card(
                modifier = Modifier,
                onClick = {
                    intent(SelectionIntent.OnSelectedUrl(request = file.request))
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = if(file.quality != null) { "QUALITY - " + file.quality } else { "QUALITY - UNKNOWN" },
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                    Text(
                        text = file.request.url,
                        maxLines = 3,
                        color = MaterialTheme.colorScheme.inverseSurface
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = 16.dp
                    ),
                text = "Developed by Darkryh"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
