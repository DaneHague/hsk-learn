package com.hsklearn.app.ui.writing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsklearn.app.data.LevelPreferences
import com.hsklearn.app.data.model.CompositionEvaluationResult
import com.hsklearn.app.data.model.WritingPrompt
import com.hsklearn.app.data.repository.HskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WritingUiState(
    val prompt: WritingPrompt? = null,
    val compositionResult: CompositionEvaluationResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class WritingViewModel @Inject constructor(
    private val hskRepository: HskRepository,
    private val levelPreferences: LevelPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WritingUiState())
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    init {
        loadPrompt()
    }

    fun loadPrompt() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, compositionResult = null) }
            try {
                val prompt = hskRepository.getWritingPrompt(levelPreferences.selectedLevel.value)
                _uiState.update { it.copy(prompt = prompt, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load prompt")
                }
            }
        }
    }

    fun submitComposition(imageBytes: ByteArray) {
        val prompt = _uiState.value.prompt ?: return

        val promptText = "${prompt.promptChinese}\n${prompt.promptEnglish}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val level = levelPreferences.selectedLevel.value
                val result = hskRepository.evaluateComposition(imageBytes, promptText, level)
                _uiState.update { it.copy(compositionResult = result, isLoading = false) }

                viewModelScope.launch {
                    try {
                        hskRepository.recordProgress(
                            "writing", "composition:${prompt.topic}", result.overallScore.toDouble(),
                        )
                    } catch (_: Exception) { /* best-effort */ }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Evaluation failed")
                }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(compositionResult = null, error = null) }
    }
}
