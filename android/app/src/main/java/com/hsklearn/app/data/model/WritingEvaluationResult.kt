package com.hsklearn.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WritingEvaluationResult(
    val overallScore: Int,
    val strokeOrder: String,
    val proportion: String,
    val similarity: String,
    val recognisedMeaning: String = "",
    val suggestions: List<String>,
    val encouragement: String,
)

@Serializable
data class CompositionEvaluationResult(
    val overallScore: Int,
    val transcription: String = "",
    val grammar: String,
    val vocabulary: String,
    val structure: String,
    val content: String,
    val corrections: List<String>,
    val suggestions: List<String>,
    val encouragement: String,
)

@Serializable
data class WritingPrompt(
    val promptChinese: String,
    val promptEnglish: String,
    val topic: String,
)
