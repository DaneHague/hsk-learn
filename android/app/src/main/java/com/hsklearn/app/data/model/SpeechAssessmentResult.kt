package com.hsklearn.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SpeechAssessmentResult(
    val overallScore: Double,
    val accuracyScore: Double,
    val fluencyScore: Double,
    val completenessScore: Double,
    val recognisedText: String,
    val words: List<WordResult>,
)

@Serializable
data class WordResult(
    val word: String,
    val accuracyScore: Double,
    val errorType: String,
    val phonemes: List<PhonemeResult>? = null,
)

@Serializable
data class PhonemeResult(
    val phoneme: String,
    val accuracyScore: Double,
)

@Serializable
data class PracticeSentence(
    val sentence: String,
    val pinyin: String,
    val translation: String,
)
