package com.hsklearn.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val wordId: Int,
    val word: String,
    val pinyin: String,
    val translation: String,
    val partOfSpeech: String,
    val exampleSentence: String? = null,
    val examplePinyin: String? = null,
    val exampleTranslation: String? = null,
    val hskLevel: Int,
    // SM-2 fields
    val repetitions: Int = 0,
    val easinessFactor: Double = 2.5,
    val interval: Int = 0, // days
    val nextReviewDate: Long = 0L, // epoch millis
    val lastReviewDate: Long = 0L,
)
