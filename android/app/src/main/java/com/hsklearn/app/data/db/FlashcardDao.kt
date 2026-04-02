package com.hsklearn.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    @Query(
        """
        SELECT * FROM flashcards
        WHERE hskLevel = :level AND nextReviewDate <= :now
        ORDER BY nextReviewDate ASC
        LIMIT :limit
        """
    )
    suspend fun getDueCards(level: Int, now: Long, limit: Int = 20): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards WHERE hskLevel = :level AND nextReviewDate <= :now")
    fun getDueCount(level: Int, now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE hskLevel = :level")
    suspend fun getTotalCount(level: Int): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE hskLevel = :level AND repetitions > 0")
    fun getLearnedCount(level: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<FlashcardEntity>)

    @Update
    suspend fun update(card: FlashcardEntity)

    @Query(
        """
        SELECT * FROM flashcards
        WHERE hskLevel = :level AND wordId != :excludeId
        ORDER BY RANDOM()
        LIMIT :count
        """
    )
    suspend fun getRandomCards(level: Int, excludeId: Int, count: Int = 3): List<FlashcardEntity>

    @Query("DELETE FROM flashcards WHERE hskLevel = :level")
    suspend fun deleteByLevel(level: Int)
}
