package com.hsklearn.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VocabularyResponse(
    val words: List<HskWord>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
)
