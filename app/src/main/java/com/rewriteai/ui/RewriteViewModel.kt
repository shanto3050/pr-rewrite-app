package com.rewriteai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rewriteai.data.RewriteRepository
import com.rewriteai.data.RewriteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RewriteViewModel(
    private val repository: RewriteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RewriteUiState())
    val state: StateFlow<RewriteUiState> = _state.asStateFlow()

    fun setOriginalText(text: String) {
        _state.update { it.copy(originalText = text) }
    }

    fun rewrite(style: RewriteStyle, regenerate: Boolean = false) {
        val text = _state.value.originalText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _state.update { state ->
                state.copy(results = state.results + (style to StyleResult.Loading))
            }
            val result = repository.rewrite(text, style, regenerate)
            _state.update { state ->
                val newResult = when {
                    result.isSuccess -> StyleResult.Success(result.getOrNull() ?: "")
                    else -> StyleResult.Error(result.exceptionOrNull()?.message ?: "Failed")
                }
                state.copy(results = state.results + (style to newResult))
            }
        }
    }

    fun regenerate(style: RewriteStyle) {
        rewrite(style, regenerate = true)
    }

    fun setExpanded(expanded: Boolean) {
        _state.update { it.copy(isExpanded = expanded) }
    }
}
