package com.hsklearn.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsklearn.app.data.LevelPreferences
import com.hsklearn.app.data.model.ProgressSummary
import com.hsklearn.app.data.repository.HskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val selectedLevel: Int = 4,
    val summary: ProgressSummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HskRepository,
    val levelPreferences: LevelPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            levelPreferences.selectedLevel.collect { level ->
                _uiState.update { it.copy(selectedLevel = level) }
                loadSummary()
            }
        }
    }

    fun setLevel(level: Int) {
        levelPreferences.setLevel(level)
    }

    fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val summary = repository.getProgressSummary()
                _uiState.update { it.copy(summary = summary, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load progress")
                }
            }
        }
    }
}
