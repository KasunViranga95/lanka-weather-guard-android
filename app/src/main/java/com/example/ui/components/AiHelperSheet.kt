package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiChatMessage
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor

/** Chat with the AI helper about the weather and rivers for the chosen place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHelperSheet(
    cityName: String,
    messages: List<AiChatMessage>,
    isThinking: Boolean,
    onAsk: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isThinking) {
        val count = messages.size + if (isThinking) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    val suggestions = listOf(
        stringResource(R.string.ai_suggest_rain),
        stringResource(R.string.ai_suggest_river),
        stringResource(R.string.ai_suggest_prepare),
        stringResource(R.string.ai_suggest_uv)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AdaptiveSurfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 12.dp)
                .testTag("ai_helper_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.ai_title), style = MaterialTheme.typography.titleLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ai_subtitle, cityName), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.ai_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdaptiveSecondaryText,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            text = if (msg.failed) stringResource(R.string.ai_failed) else msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.failed) AdaptiveSecondaryText else AdaptivePrimaryText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (msg.isUser) AdaptiveCyanAccent.copy(alpha = 0.18f) else AdaptiveElevatedColor)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.ai_thinking), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                        }
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(onClick = { if (!isThinking) onAsk(suggestion) }, label = { Text(suggestion) })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(500) },
                    placeholder = { Text(stringResource(R.string.ai_hint)) },
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input")
                )
                FilledIconButton(
                    onClick = {
                        onAsk(input)
                        input = ""
                    },
                    enabled = input.isNotBlank() && !isThinking
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.ai_send))
                }
            }
        }
    }
}
