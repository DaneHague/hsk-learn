package com.hsklearn.app.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsklearn.app.data.LevelPreferences
import com.hsklearn.app.data.db.FlashcardEntity
import com.hsklearn.app.data.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizQuestion(
    val card: FlashcardEntity,
    val prompt: String,          // what's shown as the question
    val choices: List<String>,   // 4 options
    val correctIndex: Int,       // which option is right
    val showChinese: Boolean,    // true = Chinese prompt, English choices
)

data class FlashcardUiState(
    val cards: List<FlashcardEntity> = emptyList(),
    val currentIndex: Int = 0,
    val question: QuizQuestion? = null,
    val selectedAnswer: Int? = null, // null = hasn't picked yet
    val dueCount: Int = 0,
    val learnedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionReviewed: Int = 0,
    val sessionCorrect: Int = 0,
) {
    val currentCard: FlashcardEntity? get() = cards.getOrNull(currentIndex)
    val isDone: Boolean get() = cards.isEmpty() || currentIndex >= cards.size
    val hasAnswered: Boolean get() = selectedAnswer != null
    val isCorrect: Boolean get() = selectedAnswer == question?.correctIndex
}

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val levelPreferences: LevelPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        loadCards()
        observeCounts()
    }

    private fun observeCounts() {
        viewModelScope.launch {
            levelPreferences.selectedLevel.collectLatest { level ->
                launch {
                    flashcardRepository.getDueCount(level).collectLatest { count ->
                        _uiState.update { it.copy(dueCount = count) }
                    }
                }
                launch {
                    flashcardRepository.getLearnedCount(level).collectLatest { count ->
                        _uiState.update { it.copy(learnedCount = count) }
                    }
                }
            }
        }
    }

    fun loadCards() {
        val level = levelPreferences.selectedLevel.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                flashcardRepository.seedIfNeeded(level)
                val total = flashcardRepository.getTotalCount(level)
                val due = flashcardRepository.getDueCards(level)
                _uiState.update {
                    it.copy(
                        cards = due,
                        currentIndex = 0,
                        question = null,
                        selectedAnswer = null,
                        totalCount = total,
                        isLoading = false,
                        sessionReviewed = 0,
                        sessionCorrect = 0,
                    )
                }
                if (due.isNotEmpty()) {
                    buildQuestion(due[0], level)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load flashcards")
                }
            }
        }
    }

    private suspend fun buildQuestion(card: FlashcardEntity, level: Int) {
        val showChinese = Math.random() < 0.5
        val distractors = flashcardRepository.getDistractors(level, card.wordId, 3)

        val correctAnswer: String
        val wrongAnswers: List<String>

        if (showChinese) {
            // Show Chinese word, pick the correct English translation
            correctAnswer = card.translation
            wrongAnswers = distractors.map { it.translation }
        } else {
            // Show English translation, pick the correct Chinese word
            correctAnswer = "${card.word} (${card.pinyin})"
            wrongAnswers = distractors.map { "${it.word} (${it.pinyin})" }
        }

        val allChoices = (wrongAnswers + correctAnswer).distinct().toMutableList()
        // If we don't have 4 distinct choices, pad with what we have
        while (allChoices.size < 4) {
            allChoices.add("—")
        }
        allChoices.shuffle()
        val correctIndex = allChoices.indexOf(correctAnswer)

        val prompt = if (showChinese) "${card.word}\n${card.pinyin}" else card.translation

        _uiState.update {
            it.copy(
                question = QuizQuestion(
                    card = card,
                    prompt = prompt,
                    choices = allChoices,
                    correctIndex = correctIndex,
                    showChinese = showChinese,
                ),
                selectedAnswer = null,
            )
        }
    }

    fun selectAnswer(index: Int) {
        if (_uiState.value.hasAnswered) return
        val question = _uiState.value.question ?: return
        val correct = index == question.correctIndex
        val quality = if (correct) 4 else 1

        viewModelScope.launch {
            flashcardRepository.reviewCard(question.card, quality)
        }

        _uiState.update {
            it.copy(
                selectedAnswer = index,
                sessionCorrect = if (correct) it.sessionCorrect + 1 else it.sessionCorrect,
            )
        }
    }

    fun nextCard() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                question = null,
                selectedAnswer = null,
                sessionReviewed = it.sessionReviewed + 1,
            )
        }

        val nextCard = state.cards.getOrNull(nextIndex) ?: return
        val level = levelPreferences.selectedLevel.value
        viewModelScope.launch {
            buildQuestion(nextCard, level)
        }
    }
}
