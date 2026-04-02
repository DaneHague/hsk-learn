package com.hsklearn.app.ui.reading

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hsklearn.app.data.LevelPreferences
import com.hsklearn.app.data.model.DiscussionQuestion
import com.hsklearn.app.data.model.GeneratedPassage
import com.hsklearn.app.data.model.PassageTranslation
import com.hsklearn.app.data.model.ReadingPassage
import com.hsklearn.app.data.model.SpeechAssessmentResult
import com.hsklearn.app.data.model.SpokenAnswerEvaluation
import com.hsklearn.app.data.repository.HskRepository
import com.hsklearn.app.ui.components.AudioPlayerHelper
import com.hsklearn.app.ui.speaking.AudioRecorderHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReadingTab { AI_READING, SPOKEN_QA, COMPREHENSION }

data class ReadingUiState(
    val selectedTab: ReadingTab = ReadingTab.AI_READING,
    // AI Reading tab
    val generatedPassage: GeneratedPassage? = null,
    val translation: PassageTranslation? = null,
    val isTranslationRevealed: Boolean = false,
    // Spoken Q&A tab
    val question: DiscussionQuestion? = null,
    val isRecording: Boolean = false,
    val isAssessing: Boolean = false,
    val spokenResult: SpokenAnswerEvaluation? = null,
    val speechResult: SpeechAssessmentResult? = null,
    // Comprehension quiz tab
    val passage: ReadingPassage? = null,
    val showPinyin: Boolean = false,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isSubmitted: Boolean = false,
    val score: Int = 0,
    // Read-aloud overlay
    val showReadAloud: Boolean = false,
    val isSynthesizing: Boolean = false,
    val pronunciationResult: SpeechAssessmentResult? = null,
    // Shared
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ReadingViewModel @Inject constructor(
    application: Application,
    private val repository: HskRepository,
    private val levelPreferences: LevelPreferences,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private val recorder = AudioRecorderHelper(application.cacheDir)
    private val audioPlayer = AudioPlayerHelper(application.cacheDir)
    private var recordingJob: Job? = null

    private val level: Int get() = levelPreferences.selectedLevel.value

    init {
        generatePassage()
    }

    fun selectTab(tab: ReadingTab) {
        _uiState.update { it.copy(selectedTab = tab, error = null) }
        when (tab) {
            ReadingTab.AI_READING -> if (_uiState.value.generatedPassage == null) generatePassage()
            ReadingTab.SPOKEN_QA -> if (_uiState.value.question == null) loadQuestion()
            ReadingTab.COMPREHENSION -> if (_uiState.value.passage == null) loadPassage()
        }
    }

    // ==================== AI READING ====================

    fun generatePassage() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null, generatedPassage = null,
                    translation = null, isTranslationRevealed = false)
            }
            try {
                val passage = repository.generatePassage(level)
                _uiState.update { it.copy(generatedPassage = passage, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to generate passage")
                }
            }
        }
    }

    fun revealTranslation() {
        val gp = _uiState.value.generatedPassage ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val translation = repository.translatePassage(gp.passage, gp.passagePinyin, gp.topic)
                _uiState.update {
                    it.copy(translation = translation, isTranslationRevealed = true, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Translation failed")
                }
            }
        }
    }

    // ==================== SPOKEN Q&A ====================

    fun loadQuestion() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null, question = null,
                    spokenResult = null, speechResult = null)
            }
            try {
                val question = repository.getDiscussionQuestion(level)
                _uiState.update { it.copy(question = question, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load question")
                }
            }
        }
    }

    fun startQaRecording() {
        recorder.startRecording()
        _uiState.update { it.copy(isRecording = true, spokenResult = null, speechResult = null, error = null) }
        recordingJob = viewModelScope.launch { recorder.recordLoop() }
    }

    fun stopQaRecording() {
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

        val question = _uiState.value.question ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAssessing = true, error = null) }
            try {
                // First: speech-to-text via free-speak assessment
                val speechResult = repository.assessSpeech(audioBytes)
                _uiState.update { it.copy(speechResult = speechResult) }

                // Then: evaluate the answer content via AI
                if (speechResult.recognisedText.isNotBlank()) {
                    val questionText = question.questionChinese
                    val evaluation = repository.evaluateSpokenAnswer(
                        questionText, speechResult.recognisedText, level,
                    )
                    _uiState.update { it.copy(spokenResult = evaluation, isAssessing = false) }

                    // Record progress
                    viewModelScope.launch {
                        try { repository.recordProgress("speaking", questionText, evaluation.overallScore.toDouble()) }
                        catch (_: Exception) {}
                    }
                } else {
                    _uiState.update {
                        it.copy(isAssessing = false, error = "没有识别到语音。No speech recognised.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isAssessing = false, error = e.message ?: "Assessment failed")
                }
            }
        }
    }

    // ==================== COMPREHENSION QUIZ ====================

    fun loadPassage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val passage = repository.getReadingPassage(level = level)
                _uiState.update {
                    it.copy(passage = passage, isLoading = false, selectedAnswers = emptyMap(),
                        isSubmitted = false, score = 0)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load passage")
                }
            }
        }
    }

    fun togglePinyin() {
        _uiState.update { it.copy(showPinyin = !it.showPinyin) }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        if (_uiState.value.isSubmitted) return
        _uiState.update {
            it.copy(selectedAnswers = it.selectedAnswers + (questionIndex to optionIndex))
        }
    }

    fun submitAnswers() {
        val state = _uiState.value
        val passage = state.passage ?: return
        if (state.isSubmitted) return

        var score = 0
        passage.questions.forEachIndexed { index, question ->
            if (state.selectedAnswers[index] == question.correctIndex) score++
        }
        _uiState.update { it.copy(isSubmitted = true, score = score) }

        val pct = (score.toDouble() / passage.questions.size * 100).toInt()
        viewModelScope.launch {
            try { repository.recordProgress("reading", passage.id.toString(), pct.toDouble()) }
            catch (_: Exception) {}
        }
    }

    // ==================== READ-ALOUD OVERLAY ====================

    fun showReadAloud() {
        _uiState.update { it.copy(showReadAloud = true, pronunciationResult = null) }
    }

    fun hideReadAloud() {
        audioPlayer.stop()
        if (_uiState.value.isRecording) {
            recorder.stopRecording(); recordingJob?.cancel(); recordingJob = null
        }
        _uiState.update {
            it.copy(showReadAloud = false, isRecording = false,
                isSynthesizing = false, isAssessing = false, pronunciationResult = null)
        }
    }

    fun listenToPassage() {
        val text = _uiState.value.passage?.passage
            ?: _uiState.value.generatedPassage?.passage ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSynthesizing = true) }
            try {
                val audioBytes = repository.synthesizeSpeech(text)
                audioPlayer.play(audioBytes)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "TTS failed") }
            } finally {
                _uiState.update { it.copy(isSynthesizing = false) }
            }
        }
    }

    fun startPassageRecording() {
        recorder.startRecording()
        _uiState.update { it.copy(isRecording = true, pronunciationResult = null, error = null) }
        recordingJob = viewModelScope.launch { recorder.recordLoop() }
    }

    fun stopPassageRecording() {
        val file = recorder.stopRecording()
        recordingJob?.cancel(); recordingJob = null
        _uiState.update { it.copy(isRecording = false) }

        if (file == null || !file.exists() || file.length() <= 44) {
            _uiState.update { it.copy(error = "录音失败 Recording failed") }
            return
        }
        val audioBytes = file.readBytes(); file.delete()

        val passageText = _uiState.value.passage?.passage
            ?: _uiState.value.generatedPassage?.passage ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAssessing = true, error = null) }
            try {
                val result = repository.assessPassage(audioBytes, passageText)
                _uiState.update { it.copy(pronunciationResult = result, isAssessing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAssessing = false, error = e.message ?: "Assessment failed") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.release()
        audioPlayer.release()
    }
}
