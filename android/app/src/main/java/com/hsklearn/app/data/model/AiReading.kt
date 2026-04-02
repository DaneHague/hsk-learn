package com.hsklearn.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GeneratedPassage(
    val passage: String,
    val passagePinyin: String,
    val topic: String,
)

@Serializable
data class PassageTranslation(
    val passage: String,
    val passagePinyin: String,
    val translation: String,
    val topic: String,
)

@Serializable
data class DiscussionQuestion(
    val questionChinese: String,
    val questionPinyin: String = "",
    val questionEnglish: String,
    val topic: String,
)

@Serializable
data class SpokenAnswerEvaluation(
    val overallScore: Int,
    val recognisedText: String,
    val grammar: String,
    val content: String,
    val pronunciation: String,
    val suggestions: List<String>,
    val encouragement: String,
)

@Serializable
data class TranslateRequest(
    val passage: String,
    val passagePinyin: String,
    val topic: String,
)

@Serializable
data class GenerateRequest(
    val level: Int,
)

@Serializable
data class EvaluateAnswerRequest(
    val question: String,
    val recognisedText: String,
    val level: Int,
)
