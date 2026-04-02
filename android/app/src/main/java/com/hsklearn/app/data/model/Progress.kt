package com.hsklearn.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProgressSummary(
    val totalWordsLearned: Int,
    val totalWords: Int,
    val reading: TaskSummary,
    val speaking: TaskSummary,
    val writing: TaskSummary,
    val weakWords: List<WeakWord>,
)

@Serializable
data class TaskSummary(
    val itemsCompleted: Int,
    val averageScore: Double,
)

@Serializable
data class WeakWord(
    val word: String,
    val pinyin: String,
    val averageScore: Double,
)

@Serializable
data class RecordProgressRequest(
    val type: String,
    val itemId: String,
    val score: Double,
)
