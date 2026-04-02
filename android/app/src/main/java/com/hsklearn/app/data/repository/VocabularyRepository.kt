package com.hsklearn.app.data.repository

import com.hsklearn.app.data.api.HskApiService
import com.hsklearn.app.data.model.HskWord
import com.hsklearn.app.data.model.VocabularyResponse
import javax.inject.Inject

class VocabularyRepository @Inject constructor(
    private val apiService: HskApiService,
) {
    suspend fun getVocabulary(
        page: Int = 1,
        pageSize: Int = 20,
        partOfSpeech: String? = null,
        level: Int? = null,
    ): VocabularyResponse = apiService.getVocabulary(page, pageSize, partOfSpeech, level)

    suspend fun getRandomWords(count: Int = 5, level: Int? = null): List<HskWord> =
        apiService.getRandomWords(count, level)

    suspend fun getWordById(id: Int): HskWord =
        apiService.getWordById(id)

    suspend fun search(query: String, level: Int? = null): List<HskWord> =
        apiService.searchWords(query, level)
}
