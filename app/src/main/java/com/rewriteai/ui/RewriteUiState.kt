package com.rewriteai.ui

import com.rewriteai.data.RewriteStyle

data class RewriteUiState(
    val originalText: String = "",
    val results: Map<RewriteStyle, StyleResult> = emptyMap(),
    val isExpanded: Boolean = false
)

sealed class StyleResult {
    data object Loading : StyleResult()
    data class Success(val text: String) : StyleResult()
    data class Error(val message: String) : StyleResult()
}
