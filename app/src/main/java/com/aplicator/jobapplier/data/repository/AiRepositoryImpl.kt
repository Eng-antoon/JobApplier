package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.data.remote.ai.AiProxyRequest
import com.aplicator.jobapplier.data.remote.ai.AiSuggestionsResponse
import com.aplicator.jobapplier.data.remote.ai.AiServiceException
import com.aplicator.jobapplier.data.remote.ai.AnalyzeJdResponse
import com.aplicator.jobapplier.data.remote.ai.DetectNewDataResponse
import com.aplicator.jobapplier.data.remote.ai.FetchJobUrlResponse
import com.aplicator.jobapplier.data.remote.ai.GenerateContentResponse
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededException
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class AiSuggestionsRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("response_json") val responseJson: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : AiRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun callEdgeFunction(action: String, payload: Map<String, String?>): String {
        val payloadJson = buildJsonObject {
            payload.forEach { (key, value) -> value?.let { put(key, it) } }
        }
        val request = AiProxyRequest(action = action, payload = payloadJson)
        val response: HttpResponse = try {
            supabaseClient.functions.invoke("ai-proxy", body = request)
        } catch (e: RestException) {
            if (e.statusCode == 429) {
                val quotaError = try {
                    json.decodeFromString<QuotaExceededResponse>(e.error)
                } catch (_: Exception) {
                    QuotaExceededResponse(
                        error = "quota_exceeded",
                        quotaType = "weekly",
                        used = 0,
                        limit = 0,
                        extraRemaining = 0,
                        resetsAt = null,
                        canRequestExtra = true,
                    )
                }
                throw QuotaExceededException(quotaError)
            }
            throw AiServiceException(e)
        }
        return response.body<String>()
    }

    override suspend fun analyzeJobDescription(
        jobDescription: String,
        userProfile: String,
    ): Result<AnalyzeJdResponse> = runCatching {
        val responseText = callEdgeFunction(
            "analyze_jd",
            mapOf("job_description" to jobDescription, "user_profile" to userProfile),
        )
        json.decodeFromString<AnalyzeJdResponse>(responseText)
    }

    override suspend fun generateCoverLetter(
        jobDescription: String,
        userProfile: String,
        tone: String,
        additionalInstructions: String?,
    ): Result<GenerateContentResponse> = runCatching {
        val responseText = callEdgeFunction(
            "generate_cover_letter",
            mapOf(
                "job_description" to jobDescription,
                "user_profile" to userProfile,
                "tone" to tone,
                "additional_instructions" to additionalInstructions,
            ),
        )
        json.decodeFromString<GenerateContentResponse>(responseText)
    }

    override suspend fun generateCoverEmail(
        jobDescription: String,
        userProfile: String,
        tone: String,
    ): Result<GenerateContentResponse> = runCatching {
        val responseText = callEdgeFunction(
            "generate_cover_email",
            mapOf(
                "job_description" to jobDescription,
                "user_profile" to userProfile,
                "tone" to tone,
            ),
        )
        json.decodeFromString<GenerateContentResponse>(responseText)
    }

    override suspend fun answerQuestion(
        question: String,
        questionType: String?,
        jobDescription: String,
        userProfile: String,
    ): Result<GenerateContentResponse> = runCatching {
        val responseText = callEdgeFunction(
            "answer_question",
            mapOf(
                "question" to question,
                "question_type" to questionType,
                "job_description" to jobDescription,
                "user_profile" to userProfile,
            ),
        )
        json.decodeFromString<GenerateContentResponse>(responseText)
    }

    override suspend fun detectNewData(
        text: String,
        existingProfile: String,
    ): Result<DetectNewDataResponse> = runCatching {
        val responseText = callEdgeFunction(
            "detect_new_data",
            mapOf("text" to text, "existing_profile" to existingProfile),
        )
        json.decodeFromString<DetectNewDataResponse>(responseText)
    }

    override suspend fun fetchJobFromUrl(url: String): Result<FetchJobUrlResponse> = runCatching {
        val responseText = callEdgeFunction(
            "fetch_job_url",
            mapOf("url" to url),
        )
        json.decodeFromString<FetchJobUrlResponse>(responseText)
    }

    override suspend fun generateSuggestions(
        userProfile: String,
        jobDescriptions: String?,
    ): Result<AiSuggestionsResponse> = runCatching {
        val responseText = callEdgeFunction(
            "generate_suggestions",
            mapOf("user_profile" to userProfile, "job_descriptions" to jobDescriptions),
        )
        json.decodeFromString<AiSuggestionsResponse>(responseText)
    }

    override suspend fun getCachedSuggestions(userId: String): Result<AiSuggestionsResponse?> = runCatching {
        val rows = supabaseClient.postgrest.from("ai_suggestions").select {
            filter { eq("user_id", userId) }
        }.decodeList<AiSuggestionsRow>()
        rows.firstOrNull()?.let { row ->
            json.decodeFromString<AiSuggestionsResponse>(row.responseJson)
        }
    }

    override suspend fun saveSuggestions(userId: String, response: AiSuggestionsResponse): Result<Unit> = runCatching {
        val responseJson = json.encodeToString(AiSuggestionsResponse.serializer(), response)
        supabaseClient.postgrest.from("ai_suggestions").upsert(
            AiSuggestionsRow(userId = userId, responseJson = responseJson),
        )
    }
}
