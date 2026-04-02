package com.hsklearn.app.data.api

import com.hsklearn.app.data.model.DiscussionQuestion
import com.hsklearn.app.data.model.EvaluateAnswerRequest
import com.hsklearn.app.data.model.GenerateRequest
import com.hsklearn.app.data.model.GeneratedPassage
import com.hsklearn.app.data.model.PassageTranslation
import com.hsklearn.app.data.model.SpokenAnswerEvaluation
import com.hsklearn.app.data.model.TranslateRequest
import com.hsklearn.app.data.model.CompositionEvaluationResult
import com.hsklearn.app.data.model.HskWord
import com.hsklearn.app.data.model.PracticeSentence
import com.hsklearn.app.data.model.ProgressSummary
import com.hsklearn.app.data.model.ReadingPassage
import com.hsklearn.app.data.model.RecordProgressRequest
import com.hsklearn.app.data.model.SpeechAssessmentResult
import com.hsklearn.app.data.model.VocabularyResponse
import com.hsklearn.app.data.model.WeakWord
import com.hsklearn.app.data.model.WritingEvaluationResult
import com.hsklearn.app.data.model.WritingPrompt
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface HskApiService {

    @GET("api/v1/vocabulary")
    suspend fun getVocabulary(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pos") partOfSpeech: String? = null,
        @Query("level") level: Int? = null,
    ): VocabularyResponse

    @GET("api/v1/vocabulary/random")
    suspend fun getRandomWords(
        @Query("count") count: Int = 5,
        @Query("level") level: Int? = null,
    ): List<HskWord>

    @GET("api/v1/vocabulary/{id}")
    suspend fun getWordById(
        @Path("id") id: Int,
    ): HskWord

    @GET("api/v1/vocabulary/search")
    suspend fun searchWords(
        @Query("q") query: String,
        @Query("level") level: Int? = null,
    ): List<HskWord>

    @GET("api/v1/reading/passage")
    suspend fun getReadingPassage(
        @Query("topic") topic: String? = null,
        @Query("level") level: Int = 4,
    ): ReadingPassage

    @GET("api/v1/reading/passage/{id}")
    suspend fun getReadingPassageById(
        @Path("id") id: Int,
    ): ReadingPassage

    @POST("api/v1/reading/generate")
    suspend fun generatePassage(
        @Body request: GenerateRequest,
    ): GeneratedPassage

    @POST("api/v1/reading/translate")
    suspend fun translatePassage(
        @Body request: TranslateRequest,
    ): PassageTranslation

    @GET("api/v1/reading/question")
    suspend fun getDiscussionQuestion(
        @Query("level") level: Int = 4,
    ): DiscussionQuestion

    @POST("api/v1/reading/evaluate-answer")
    suspend fun evaluateSpokenAnswer(
        @Body request: EvaluateAnswerRequest,
    ): SpokenAnswerEvaluation

    @GET("api/v1/reading/topics")
    suspend fun getReadingTopics(
        @Query("level") level: Int = 4,
    ): List<String>

    @Multipart
    @POST("api/v1/speech/assess")
    suspend fun assessSpeech(
        @Part audio: MultipartBody.Part,
        @Part("referenceText") referenceText: RequestBody? = null,
    ): SpeechAssessmentResult

    @Multipart
    @POST("api/v1/speech/assess-passage")
    suspend fun assessPassage(
        @Part audio: MultipartBody.Part,
        @Part("referenceText") referenceText: RequestBody,
    ): SpeechAssessmentResult

    @POST("api/v1/speech/synthesize")
    suspend fun synthesizeSpeech(
        @Body body: RequestBody,
    ): ResponseBody

    @GET("api/v1/speech/sentences")
    suspend fun getPracticeSentence(
        @Query("level") level: Int = 4,
    ): PracticeSentence

    @Multipart
    @POST("api/v1/writing/evaluate")
    suspend fun evaluateWriting(
        @Part image: MultipartBody.Part,
        @Part("targetCharacter") targetCharacter: RequestBody,
        @Part("pinyin") pinyin: RequestBody,
    ): WritingEvaluationResult

    @Multipart
    @POST("api/v1/writing/evaluate-composition")
    suspend fun evaluateComposition(
        @Part image: MultipartBody.Part,
        @Part("prompt") prompt: RequestBody,
        @Part("level") level: RequestBody,
    ): CompositionEvaluationResult

    @GET("api/v1/writing/prompts")
    suspend fun getWritingPrompt(
        @Query("level") level: Int = 4,
    ): WritingPrompt

    @POST("api/v1/progress/record")
    suspend fun recordProgress(
        @Body request: RecordProgressRequest,
    )

    @GET("api/v1/progress/summary")
    suspend fun getProgressSummary(): ProgressSummary

    @GET("api/v1/progress/weak-words")
    suspend fun getWeakWords(
        @Query("count") count: Int = 5,
    ): List<WeakWord>
}
