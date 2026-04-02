package com.hsklearn.app.ui.speaking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hsklearn.app.data.LevelPreferences
import com.hsklearn.app.data.model.DiscussionQuestion
import com.hsklearn.app.data.model.PracticeSentence
import com.hsklearn.app.data.model.SpeechAssessmentResult
import com.hsklearn.app.data.model.SpokenAnswerEvaluation
import com.hsklearn.app.data.repository.HskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SpeakingTab { READ_ALOUD, FREE_TALK }

data class SpeakingUiState(
    val selectedTab: SpeakingTab = SpeakingTab.READ_ALOUD,
    // Read Aloud
    val sentence: PracticeSentence? = null,
    val showPinyin: Boolean = true,
    // Free Talk
    val freeQuestion: DiscussionQuestion? = null,
    val aiEvaluation: SpokenAnswerEvaluation? = null,
    // Shared
    val isRecording: Boolean = false,
    val isLoading: Boolean = false,
    val result: SpeechAssessmentResult? = null,
    val error: String? = null,
)

@HiltViewModel
class SpeakingViewModel @Inject constructor(
    application: Application,
    private val repository: HskRepository,
    private val levelPreferences: LevelPreferences,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SpeakingUiState())
    val uiState: StateFlow<SpeakingUiState> = _uiState.asStateFlow()

    private val recorder = AudioRecorderHelper(application.cacheDir)
    private var recordingJob: Job? = null

    private val level: Int get() = levelPreferences.selectedLevel.value

    init {
        loadSentence()
    }

    fun selectTab(tab: SpeakingTab) {
        _uiState.update {
            it.copy(selectedTab = tab, result = null, aiEvaluation = null, error = null)
        }
        when (tab) {
            SpeakingTab.READ_ALOUD -> if (_uiState.value.sentence == null) loadSentence()
            SpeakingTab.FREE_TALK -> if (_uiState.value.freeQuestion == null) loadFreeQuestion()
        }
    }

    fun loadSentence() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }
            try {
                val sentence = repository.getPracticeSentence(level)
                _uiState.update { it.copy(sentence = sentence, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load sentence")
                }
            }
        }
    }

    fun loadFreeQuestion() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null, freeQuestion = null,
                    result = null, aiEvaluation = null)
            }
            try {
                val question = repository.getDiscussionQuestion(level)
                _uiState.update { it.copy(freeQuestion = question, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load question")
                }
            }
        }
    }

    fun togglePinyin() {
        _uiState.update { it.copy(showPinyin = !it.showPinyin) }
    }

    fun startRecording() {
        recorder.startRecording()
        _uiState.update {
            it.copy(isRecording = true, result = null, aiEvaluation = null, error = null)
        }
        recordingJob = viewModelScope.launch { recorder.recordLoop() }
    }

    fun stopRecording() {
        val file = recorder.stopRecording()
        recordingJob?.cancel()
        recordingJob = null
        _uiState.update { it.copy(isRecording = false) }

        if (file == null || !file.exists() || file.length() <= 44) {
            _uiState.update { it.copy(error = "录音失败 Recording failed") }
            return
        }

        val audioBytes = file.readBytes()
        file.delete()
        submitForAssessment(audioBytes)
    }

    private fun submitForAssessment(audioBytes: ByteArray) {
        val state = _uiState.value
        val isReadAloud = state.selectedTab == SpeakingTab.READ_ALOUD
        val referenceText = if (isReadAloud) state.sentence?.sentence else null

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Step 1: Speech assessment (pronunciation)
                val result = repository.assessSpeech(audioBytes, referenceText)
                _uiState.update { it.copy(result = result) }

                // Step 2: For Free Talk, also get AI content evaluation
                if (!isReadAloud && result.recognisedText.isNotBlank()) {
                    val question = state.freeQuestion?.questionChinese ?: ""
                    try {
                        val evaluation = repository.evaluateSpokenAnswer(
                            question, result.recognisedText, level,
                        )
                        _uiState.update {
                            it.copy(aiEvaluation = evaluation, isLoading = false)
                        }
                    } catch (e: Exception) {
                        // Still show speech result even if AI evaluation fails
                        _uiState.update {
                            it.copy(isLoading = false,
                                error = "AI评估失败 AI evaluation failed: ${e.message}")
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }

                // Record progress
                val itemId = referenceText ?: state.freeQuestion?.questionChinese ?: "free_talk"
                val score = if (!isReadAloud && _uiState.value.aiEvaluation != null) {
                    _uiState.value.aiEvaluation!!.overallScore.toDouble()
                } else {
                    result.overallScore
                }
                viewModelScope.launch {
                    try { repository.recordProgress("speaking", itemId, score) }
                    catch (_: Exception) {}
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Assessment failed")
                }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null, aiEvaluation = null, error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.release()
    }
}
