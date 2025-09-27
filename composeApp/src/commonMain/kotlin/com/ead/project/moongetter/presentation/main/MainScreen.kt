package com.ead.project.moongetter.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ead.project.moongetter.app.system.extensions.shimmerEffect
import com.ead.project.moongetter.app.system.extensions.spacedHorizontal
import com.ead.project.moongetter.presentation.main.components.MainSocialMediaOption
import com.ead.project.moongetter.presentation.main.components.TextField
import com.ead.project.moongetter.presentation.main.intent.MainIntent
import com.ead.project.moongetter.presentation.main.intent.NetworkIntent
import com.ead.project.moongetter.presentation.main.intent.SelectionIntent
import com.ead.project.moongetter.presentation.main.intent.TextIntent
import com.ead.project.moongetter.presentation.main.model.MainModelState
import com.ead.project.moongetter.presentation.main.state.MainState
import com.ead.project.moongetter.presentation.player.Player
import com.ead.project.moongetter.presentation.theme.MoonGetterTheme
import com.ead.project.moongetter.presentation.util.AboutUs.GITHUB
import com.ead.project.moongetter.presentation.util.AboutUs.TWITTER
import com.ead.project.moongetter.presentation.util.ClipboardUtil
import com.ead.project.moongetter.presentation.util.DataUtil
import com.ead.project.moongetter.presentation.util.IntentUtil
import moongetter.composeapp.generated.resources.Res
import moongetter.composeapp.generated.resources.app_name
import moongetter.composeapp.generated.resources.copy_headers_to_clipboard
import moongetter.composeapp.generated.resources.copy_url_to_clipboard
import moongetter.composeapp.generated.resources.github
import moongetter.composeapp.generated.resources.headers
import moongetter.composeapp.generated.resources.ic_github
import moongetter.composeapp.generated.resources.ic_sad_face
import moongetter.composeapp.generated.resources.ic_twitter
import moongetter.composeapp.generated.resources.known
import moongetter.composeapp.generated.resources.method_variable
import moongetter.composeapp.generated.resources.more_info
import moongetter.composeapp.generated.resources.quality
import moongetter.composeapp.generated.resources.quantity_options
import moongetter.composeapp.generated.resources.search_from_url_option
import moongetter.composeapp.generated.resources.server_placeholder
import moongetter.composeapp.generated.resources.supported_servers
import moongetter.composeapp.generated.resources.there_is_nothing_playing_at_the_moment
import moongetter.composeapp.generated.resources.unknown_quality
import moongetter.composeapp.generated.resources.video_info
import moongetter.composeapp.generated.resources.video_qualities
import moongetter.composeapp.generated.resources.video_type
import moongetter.composeapp.generated.resources.x_twitter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    intent: (MainIntent) -> Unit,
    state: MainState
) {

    val keyboardController = LocalSoftwareKeyboardController.current

    val copiedUrlLabel = stringResource(Res.string.copy_url_to_clipboard)
    val copiedHeadersLabel = stringResource(Res.string.copy_headers_to_clipboard)

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            if (
                state.mainModelState == MainModelState.INITIALIZED ||
                (state.mainModelState == MainModelState.SEARCHING && state.selectedStream != null) ||
                state.mainModelState == MainModelState.ERROR_SEARCHING
                ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stringResource(Res.string.app_name),
                                fontWeight = FontWeight.W400
                            )
                        }
                    },
                    actions = {
                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.extraLarge
                                ),
                            onClick = { intent(SelectionIntent.Searching) }
                        ) {
                            Icon(
                                modifier = Modifier,
                                imageVector = if (state.shouldShowSearchingServer) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = "Search url to extract icon",
                                tint = MaterialTheme.colorScheme.inverseSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        actionColor = MaterialTheme.colorScheme.secondary
                    )
                }
            )

            when(state.mainModelState) {
                MainModelState.INITIALIZED ->{

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(Res.drawable.ic_sad_face),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                contentDescription = "Sad Face Icon"
                            )

                            Text(
                                text = stringResource(Res.string.there_is_nothing_playing_at_the_moment),
                                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                    }
                }
                MainModelState.SEARCHING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .aspectRatio(16f / 9f)
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            spacedHorizontal()
                            item {
                                Box(
                                    modifier = modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .size(width = 84.dp, height = 24.dp)
                                        .shimmerEffect()
                                )
                            }

                            item {
                                Box(
                                    modifier = modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .size(width = 84.dp, height = 24.dp)
                                        .shimmerEffect()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .height(70.dp)
                            .shimmerEffect()
                    )
                }
                MainModelState.PLAYING -> {
                    state.selectedStream?.let {
                        Player(
                            modifier = Modifier,
                            request = it
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                spacedHorizontal(0.dp)

                                item {
                                    MainSocialMediaOption(
                                        modifierIcon = Modifier
                                            .size(20.dp),
                                        painter = painterResource(Res.drawable.ic_github),
                                        contentDescription = "Option to send user to github repository",
                                        text = stringResource(Res.string.github),
                                        onClick = { IntentUtil.goIntentTo(GITHUB) }
                                    )
                                }

                                item {
                                    MainSocialMediaOption(
                                        modifierIcon = Modifier
                                            .size(16.dp),
                                        painter = painterResource(Res.drawable.ic_twitter),
                                        contentDescription = "Option to send user to twitter site",
                                        text = stringResource(Res.string.x_twitter),
                                        onClick = { IntentUtil.goIntentTo(TWITTER) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable(onClick = { intent(SelectionIntent.MoreVideoInfo) }),
                                imageVector = if (state.shouldShowMoreInfoAboutVideo) Icons.Outlined.Close else Icons.Outlined.Settings,
                                contentDescription = "Search url to extract",
                                tint = MaterialTheme.colorScheme.inverseSurface
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Icon(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable(onClick = { intent(SelectionIntent.Searching) }),
                                imageVector = if (state.shouldShowSearchingServer) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = "Search url to extract",
                                tint = MaterialTheme.colorScheme.inverseSurface
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.W400,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) { append(stringResource(Res.string.video_type)) }
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.W400,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ) { append("${state.streamServerName}\n") }

                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.W500,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) { append(stringResource(Res.string.quantity_options)) }
                                    append("${state.streamPlaylist.size}\n")

                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.W500,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) { append(stringResource(Res.string.quality)) }
                                    append(stringResource(Res.string.known))
                                },
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            )
                        }
                    }
                }
                MainModelState.ERROR_SEARCHING -> Unit
            }
        }


        if (state.shouldShowSearchingServer) {
            ModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDismissRequest = { intent(SelectionIntent.Searching) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                                .padding(vertical = 16.dp, horizontal = 16.dp)

                        ) {
                            TextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = state.targetExtractTextField.textField,
                                onValueChange = { intent(TextIntent.EnteredTargetSearch(it)) },
                                onFocusChange = { intent(TextIntent.ChangeSearchFocus(it.isFocused)) },
                                hint = state.targetExtractTextField.hint,
                                isHintVisible = state.targetExtractTextField.isHintVisible,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        intent(NetworkIntent.OnGetResult(state.targetExtractTextField.textField.text))
                                    }
                                ),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.inverseSurface
                                ),
                                hintTextStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.inverseSurface
                                ),
                                singleLine = true
                            )

                        }

                        IconButton(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                                .padding(
                                    horizontal = 4.dp
                                ),
                            onClick = {
                                if (state.mainModelState != MainModelState.SEARCHING) {
                                    intent(NetworkIntent.OnGetResult(state.targetExtractTextField.textField.text))
                                    keyboardController?.hide()
                                }
                            }
                        ) {
                            Icon(
                                modifier = Modifier,
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search url to extract",
                                tint = MaterialTheme.colorScheme.inverseSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            modifier = Modifier
                                .clickable(
                                    onClick = { intent(SelectionIntent.MoreServerInfo) },
                                    indication = null,
                                    interactionSource = null
                                ),
                            text = buildAnnotatedString {
                                append("${stringResource(Res.string.search_from_url_option)} ")

                                withStyle(
                                    style = SpanStyle(
                                        textDecoration = TextDecoration.Underline,
                                        color = Color(0xFF81D4FA)
                                    )
                                ) { append(stringResource(Res.string.more_info)) }
                            },
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (state.shouldShowMoreInfoAboutSupportedServer) {
            ModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDismissRequest = { intent(SelectionIntent.MoreServerInfo) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.supported_servers),
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        fontWeight = FontWeight.W500
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DataUtil.serversNames) { server ->
                            Text(
                                text = stringResource(Res.string.server_placeholder, server),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                fontWeight = FontWeight.W400
                            )
                        }
                    }
                }
            }
        }

        if (state.shouldShowMoreInfoAboutVideo) {
            ModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                onDismissRequest = { intent(SelectionIntent.MoreVideoInfo) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.isInfoDetailActivated) stringResource(Res.string.video_info) else stringResource(Res.string.video_qualities),
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.W500
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (state.isInfoDetailActivated) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(onClick = { intent(SelectionIntent.InfoDetailActivated) })
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Outlined.AddLink,
                                contentDescription = "Info detail activation option",
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(state.streamPlaylist) { video ->
                            if (state.isInfoDetailActivated) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = video.quality ?: stringResource(Res.string.unknown_quality),
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                            fontWeight = FontWeight.W400
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        Text(
                                            text = stringResource(Res.string.method_variable, video.request.method),
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                            fontWeight = FontWeight.W400
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = MaterialTheme.shapes.medium
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = video.request.url,
                                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                                fontWeight = FontWeight.W400,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = MaterialTheme.shapes.medium
                                                )
                                                .padding(8.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable(onClick = { ClipboardUtil.copy(video.request.headers.toString(), copiedHeadersLabel) } )
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.headers),
                                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                                fontWeight = FontWeight.W400
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = MaterialTheme.shapes.medium
                                                )
                                                .padding(8.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable(onClick = {
                                                    ClipboardUtil.copy(video.request.url, copiedUrlLabel) }
                                                )
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(20.dp),
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "Copy url to clipboard"
                                            )
                                        }

                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    HorizontalDivider()
                                }

                            }
                            else {
                                Row(
                                    modifier = Modifier
                                        .clickable(
                                            onClick = {
                                                intent(SelectionIntent.OnSelectedUrl(video.request))
                                                intent(SelectionIntent.MoreVideoInfo)
                                            }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Text(
                                        modifier = Modifier
                                            .weight(1f),
                                        text = video.quality ?: stringResource(Res.string.unknown_quality),
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        fontWeight = FontWeight.W400
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Suppress("unused")
@Composable
fun MainScreenPreview() {
    MoonGetterTheme {
        MainScreen(
            modifier = Modifier
                .fillMaxSize(),
            snackbarHostState = SnackbarHostState(),
            intent = {},
            state = MainState()
        )
    }
}