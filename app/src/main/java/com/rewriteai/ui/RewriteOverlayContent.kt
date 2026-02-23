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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000)),
            contentAlignment = Alignment.Center
        ) {
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
        }
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
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Open rewrite",
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

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
    val hasAnyResult = state.results.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rewrite",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Section: Original text
            Text(
                text = "Original text",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            OutlinedTextField(
                value = state.originalText,
                onValueChange = { viewModel.setOriginalText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = { Text("Paste or type text to rewrite") },
                placeholder = { Text("Enter or paste text here…") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // Section: Choose tone (2x2 grid)
            Text(
                text = "Choose tone",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
            val stylesChunked = RewriteStyle.entries.chunked(2)
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stylesChunked.forEach { rowStyles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowStyles.forEach { style ->
                            Box(modifier = Modifier.weight(1f)) {
                                FilledTonalButton(
                                    onClick = { viewModel.rewrite(style) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                        // Pad row if odd number (e.g. 4 items = 2+2, no pad needed)
                        if (rowStyles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Section: Results
            if (hasAnyResult) {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (!hasAnyResult) {
                    Text(
                        text = "Select a tone above to rewrite your text.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    RewriteStyle.entries.forEach { style ->
                        val result = state.results[style] ?: return@forEach
                        Spacer(modifier = Modifier.height(12.dp))
                        StyleResultCard(
                            style = style,
                            result = result,
                            onReplace = { text -> onReplace(text) },
                            onCopy = {
                                clipboard?.setPrimaryClip(
                                    android.content.ClipData.newPlainText("rewritten", it)
                                )
                                onCopyAndClose()
                            },
                            onRegenerate = { viewModel.regenerate(style) },
                            showReplace = fromProcessText
                        )
                    }
                }
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
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = style.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (result) {
                is StyleResult.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = "Rewriting…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showReplace) {
                            FilledTonalButton(
                                onClick = { onReplace(result.text) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Replace")
                            }
                            Spacer(modifier = Modifier.padding(4.dp))
                        }
                        IconButton(onClick = { onCopy(result.text) }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy"
                            )
                        }
                        IconButton(onClick = onRegenerate) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regenerate"
                            )
                        }
                    }
                }
                is StyleResult.Error -> {
                    Text(
                        text = "Something went wrong. Please try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = onRegenerate,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
