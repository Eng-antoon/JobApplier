package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.data.remote.ai.AiSuggestionsResponse
import com.aplicator.jobapplier.data.remote.ai.AnalyzeJdResponse
import com.aplicator.jobapplier.data.remote.ai.DetectNewDataResponse
import com.aplicator.jobapplier.data.remote.ai.FetchJobUrlResponse
import com.aplicator.jobapplier.data.remote.ai.GenerateContentResponse

interface AiRepository {
    suspend fun analyzeJobDescription(jobDescription: String, userProfile: String): Result<AnalyzeJdResponse>
    suspend fun generateCoverLetter(jobDescription: String, userProfile: String, tone: String, additionalInstructions: String?): Result<GenerateContentResponse>
    suspend fun generateCoverEmail(jobDescription: String, userProfile: String, tone: String): Result<GenerateContentResponse>
    suspend fun answerQuestion(question: String, questionType: String?, jobDescription: String, userProfile: String, tone: String): Result<GenerateContentResponse>
    suspend fun detectNewData(text: String, existingProfile: String): Result<DetectNewDataResponse>
    suspend fun fetchJobFromUrl(url: String): Result<FetchJobUrlResponse>
    suspend fun generateSuggestions(userProfile: String, jobDescriptions: String?): Result<AiSuggestionsResponse>
    suspend fun getCachedSuggestions(userId: String): Result<AiSuggestionsResponse?>
    suspend fun saveSuggestions(userId: String, response: AiSuggestionsResponse): Result<Unit>
}
