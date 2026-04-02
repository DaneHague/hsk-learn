package com.hsklearn.app.data.repository

import com.hsklearn.app.data.db.FlashcardDao
import com.hsklearn.app.data.db.FlashcardEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

class FlashcardRepository @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val vocabularyRepository: VocabularyRepository,
) {
    fun getDueCount(level: Int): Flow<Int> =
        flashcardDao.getDueCount(level, System.currentTimeMillis())

    fun getLearnedCount(level: Int): Flow<Int> =
        flashcardDao.getLearnedCount(level)

    suspend fun getTotalCount(level: Int): Int =
        flashcardDao.getTotalCount(level)

    suspend fun getDueCards(level: Int, limit: Int = 20): List<FlashcardEntity> =
        flashcardDao.getDueCards(level, System.currentTimeMillis(), limit)

    suspend fun getDistractors(level: Int, excludeId: Int, count: Int = 3): List<FlashcardEntity> =
        flashcardDao.getRandomCards(level, excludeId, count)

    /**
     * Load vocabulary from API and seed the local database for the given level.
     * Only inserts words not already present (IGNORE conflict strategy).
     */
    suspend fun seedIfNeeded(level: Int) {
        if (flashcardDao.getTotalCount(level) > 0) return

        // Fetch all words for this level in pages
        val allWords = mutableListOf<FlashcardEntity>()
        var page = 1
        while (true) {
            val response = vocabularyRepository.getVocabulary(page = page, pageSize = 100, level = level)
            allWords += response.words.map { w ->
                FlashcardEntity(
                    wordId = w.id,
                    word = w.word,
                    pinyin = w.pinyin,
                    translation = w.translation,
                    partOfSpeech = w.partOfSpeech,
                    exampleSentence = w.exampleSentence,
                    examplePinyin = w.examplePinyin,
                    exampleTranslation = w.exampleTranslation,
                    hskLevel = w.hskLevel,
                    nextReviewDate = System.currentTimeMillis(), // due immediately
                )
            }
            if (page * 100 >= response.totalCount) break
            page++
        }

        if (allWords.isNotEmpty()) {
            flashcardDao.insertAll(allWords)
        }
    }

    /**
     * Apply SM-2 algorithm and update the card.
     * @param quality 0-5 rating (0-2 = fail, 3 = hard, 4 = good, 5 = easy)
     */
    suspend fun reviewCard(card: FlashcardEntity, quality: Int) {
        val q = quality.coerceIn(0, 5)
        val now = System.currentTimeMillis()

        val newEF = max(
            1.3,
            card.easinessFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)),
        )

        val newReps: Int
        val newInterval: Int

        if (q < 3) {
            // Failed — reset
            newReps = 0
            newInterval = 1
        } else {
            newReps = card.repetitions + 1
            newInterval = when (newReps) {
                1 -> 1
                2 -> 6
                else -> (card.interval * newEF).roundToInt()
            }
        }

        val nextReview = now + newInterval * 24L * 60 * 60 * 1000

        flashcardDao.update(
            card.copy(
                repetitions = newReps,
                easinessFactor = newEF,
                interval = newInterval,
                nextReviewDate = nextReview,
                lastReviewDate = now,
            ),
        )
    }
}
