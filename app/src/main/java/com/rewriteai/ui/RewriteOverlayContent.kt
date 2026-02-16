package com.rewriteai.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rewriteai.data.RewriteStyle

@Composable
fun RewriteOverlayContent(
    viewModel: RewriteViewModel,
    fromProcessText: Boolean,
    onDismiss: () -> Unit,
    onReplace: (String) -> Unit,
    onCopyAndClose: () -> Unit,
    onExpandChanged: ((Boolean) -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isExpanded) {
        onExpandChanged?.invoke(state.isExpanded)
    }

    if (state.isExpanded) {
        RewritePanel(
            state = state,
            viewModel = viewModel,
            fromProcessText = fromProcessText,
            onDismiss = {
                if (fromProcessText) onDismiss()
                else viewModel.setExpanded(false)
            },
            onReplace = onReplace,
            onCopyAndClose = onCopyAndClose
        )
    } else {
        BubbleView(
            onClick = { viewModel.setExpanded(true) },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun BubbleView(
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✎",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RewritePanel(
    state: RewriteUiState,
    viewModel: RewriteViewModel,
    fromProcessText: Boolean,
    onDismiss: () -> Unit,
    onReplace: (String) -> Unit,
    onCopyAndClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Rewrite AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        OutlinedTextField(
            value = state.originalText,
            onValueChange = { viewModel.setOriginalText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            label = { Text("Original text") },
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            "Choose a style:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RewriteStyle.entries.forEach { style ->
                TextButton(
                    onClick = { viewModel.rewrite(style) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(style.displayName)
                }
            }
        }

        Column(
            modifier = Modifier.verticalScroll(scrollState)
        ) {
            RewriteStyle.entries.forEach { style ->
                val result = state.results[style] ?: return@forEach
                Spacer(modifier = Modifier.height(8.dp))
                StyleResultCard(
                    style = style,
                    result = result,
                    onReplace = { text -> onReplace(text) },
                    onCopy = {
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("rewritten", it))
                        onCopyAndClose()
                    },
                    onRegenerate = { viewModel.regenerate(style) },
                    showReplace = fromProcessText
                )
            }
        }
    }
}

@Composable
private fun StyleResultCard(
    style: RewriteStyle,
    result: StyleResult,
    onReplace: (String) -> Unit,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
    showReplace: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                style.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (result) {
                is StyleResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                is StyleResult.Success -> {
                    Text(
                        text = result.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (showReplace) {
                            TextButton(onClick = { onReplace(result.text) }) {
                                Text("Replace")
                            }
                        }
                        TextButton(onClick = { onCopy(result.text) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Copy")
                        }
                        TextButton(onClick = onRegenerate) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Regenerate")
                        }
                    }
                }
                is StyleResult.Error -> {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRegenerate) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
