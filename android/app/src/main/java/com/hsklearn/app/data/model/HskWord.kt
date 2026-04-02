package com.hsklearn.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HskWord(
    val id: Int,
    val word: String,
    val pinyin: String,
    val partOfSpeech: String,
    val translation: String,
    val hskLevel: Int = 4,
    val exampleSentence: String? = null,
    val examplePinyin: String? = null,
    val exampleTranslation: String? = null,
)
