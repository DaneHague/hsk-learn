package com.hsklearn.app.data.repository

import com.hsklearn.app.data.api.HskApiService
import com.hsklearn.app.data.model.DiscussionQuestion
import com.hsklearn.app.data.model.EvaluateAnswerRequest
import com.hsklearn.app.data.model.GenerateRequest
import com.hsklearn.app.data.model.GeneratedPassage
import com.hsklearn.app.data.model.PassageTranslation
import com.hsklearn.app.data.model.SpokenAnswerEvaluation
import com.hsklearn.app.data.model.TranslateRequest
import com.hsklearn.app.data.model.CompositionEvaluationResult
import com.hsklearn.app.data.model.PracticeSentence
import com.hsklearn.app.data.model.ProgressSummary
import com.hsklearn.app.data.model.ReadingPassage
import com.hsklearn.app.data.model.RecordProgressRequest
import com.hsklearn.app.data.model.SpeechAssessmentResult
import com.hsklearn.app.data.model.WeakWord
import com.hsklearn.app.data.model.WritingEvaluationResult
import com.hsklearn.app.data.model.WritingPrompt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class HskRepository @Inject constructor(
    private val apiService: HskApiService,
) {
    suspend fun getReadingPassage(topic: String? = null, level: Int = 4): ReadingPassage =
        apiService.getReadingPassage(topic, level)

    suspend fun getReadingPassageById(id: Int): ReadingPassage =
        apiService.getReadingPassageById(id)

    suspend fun getReadingTopics(level: Int = 4): List<String> =
        apiService.getReadingTopics(level)

    suspend fun generatePassage(level: Int): GeneratedPassage =
        apiService.generatePassage(GenerateRequest(level))

    suspend fun translatePassage(passage: String, pinyin: String, topic: String): PassageTranslation =
        apiService.translatePassage(TranslateRequest(passage, pinyin, topic))

    suspend fun getDiscussionQuestion(level: Int = 4): DiscussionQuestion =
        apiService.getDiscussionQuestion(level)

    suspend fun evaluateSpokenAnswer(question: String, recognisedText: String, level: Int): SpokenAnswerEvaluation =
        apiService.evaluateSpokenAnswer(EvaluateAnswerRequest(question, recognisedText, level))

    suspend fun assessSpeech(
        audioBytes: ByteArray,
        referenceText: String? = null,
    ): SpeechAssessmentResult {
        val audioPart = MultipartBody.Part.createFormData(
            "audio", "audio.wav",
            audioBytes.toRequestBody("audio/wav".toMediaType()),
        )
        val textBody = referenceText?.toRequestBody("text/plain".toMediaType())
        return apiService.assessSpeech(audioPart, textBody)
    }

    suspend fun assessPassage(
        audioBytes: ByteArray,
        referenceText: String,
    ): SpeechAssessmentResult {
        val audioPart = MultipartBody.Part.createFormData(
            "audio", "audio.wav",
            audioBytes.toRequestBody("audio/wav".toMediaType()),
        )
        val textBody = referenceText.toRequestBody("text/plain".toMediaType())
        return apiService.assessPassage(audioPart, textBody)
    }

    suspend fun synthesizeSpeech(text: String): ByteArray {
        val json = """{"text":"${text.replace("\"", "\\\"")}"}"""
        val body = json.toRequestBody("application/json".toMediaType())
        val response = apiService.synthesizeSpeech(body)
        return response.bytes()
    }

    suspend fun getPracticeSentence(level: Int = 4): PracticeSentence =
        apiService.getPracticeSentence(level)

    suspend fun evaluateWriting(
        imageBytes: ByteArray,
        targetCharacter: String,
        pinyin: String,
    ): WritingEvaluationResult {
        val imagePart = MultipartBody.Part.createFormData(
            "image", "image.png",
            imageBytes.toRequestBody("image/png".toMediaType()),
        )
        val charBody = targetCharacter.toRequestBody("text/plain".toMediaType())
        val pinyinBody = pinyin.toRequestBody("text/plain".toMediaType())
        return apiService.evaluateWriting(imagePart, charBody, pinyinBody)
    }

    suspend fun evaluateComposition(imageBytes: ByteArray, prompt: String, level: Int = 4): CompositionEvaluationResult {
        val imagePart = MultipartBody.Part.createFormData(
            "image", "composition.png",
            imageBytes.toRequestBody("image/png".toMediaType()),
        )
        val promptBody = prompt.toRequestBody("text/plain".toMediaType())
        val levelBody = level.toString().toRequestBody("text/plain".toMediaType())
        return apiService.evaluateComposition(imagePart, promptBody, levelBody)
    }

    suspend fun getWritingPrompt(level: Int = 4): WritingPrompt =
        apiService.getWritingPrompt(level)

    suspend fun recordProgress(type: String, itemId: String, score: Double) {
        apiService.recordProgress(RecordProgressRequest(type, itemId, score))
    }

    suspend fun getProgressSummary(): ProgressSummary =
        apiService.getProgressSummary()

    suspend fun getWeakWords(count: Int = 5): List<WeakWord> =
        apiService.getWeakWords(count)
}
