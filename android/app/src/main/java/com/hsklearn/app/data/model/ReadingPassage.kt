package com.hsklearn.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReadingPassage(
    val id: Int,
    val topic: String,
    val title: String,
    val titlePinyin: String,
    val passage: String,
    val passagePinyin: String,
    val targetWords: List<String>,
    val questions: List<ComprehensionQuestion>,
)

@Serializable
data class ComprehensionQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)
