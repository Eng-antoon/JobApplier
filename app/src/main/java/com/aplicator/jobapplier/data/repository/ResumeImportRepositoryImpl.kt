package com.aplicator.jobapplier.data.repository

import android.content.Context
import android.net.Uri
import com.aplicator.jobapplier.data.remote.ai.AiProxyRequest
import com.aplicator.jobapplier.data.remote.ai.ParsedResumeResponse
import com.aplicator.jobapplier.data.remote.ai.ParsedSkill
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.apache.poi.xwpf.usermodel.XWPFDocument
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResumeImportRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : ResumeImportRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extractText(uri: Uri, context: Context): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(uri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open file")

            inputStream.use { stream ->
                when {
                    mimeType == "application/pdf" || uri.toString().endsWith(".pdf") -> {
                        val document = PDDocument.load(stream)
                        document.use { doc ->
                            PDFTextStripper().getText(doc)
                        }
                    }
                    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        || uri.toString().endsWith(".docx") -> {
                        val document = XWPFDocument(stream)
                        document.use { doc ->
                            doc.paragraphs.joinToString("\n") { it.text }
                        }
                    }
                    else -> throw IllegalArgumentException("Unsupported file type: $mimeType")
                }
            }
        }
    }

    override suspend fun parseResume(resumeText: String): Result<ParsedResumeResponse> = runCatching {
        withContext(Dispatchers.IO) {
            val truncatedText = if (resumeText.length > MAX_RESUME_TEXT_LENGTH) {
                resumeText.take(MAX_RESUME_TEXT_LENGTH)
            } else {
                resumeText
            }
            val payloadJson = buildJsonObject {
                put("resume_text", truncatedText)
            }
            val request = AiProxyRequest(action = "parse_resume", payload = payloadJson)
            val response = supabaseClient.functions.invoke("ai-proxy", body = request)
            val responseText = response.body<String>()
            if (responseText.contains("\"error\"") && !responseText.contains("\"skills\"")) {
                val errorMsg = try {
                    json.decodeFromString<Map<String, String>>(responseText)["error"]
                } catch (_: Exception) { null }
                throw IllegalStateException(errorMsg ?: "AI proxy returned an error")
            }
            val parsed = json.decodeFromString<ParsedResumeResponse>(responseText)
            enrichSkillsFromExperiences(parsed)
        }
    }

    private fun enrichSkillsFromExperiences(response: ParsedResumeResponse): ParsedResumeResponse {
        val existingSkillNames = response.skills.map { it.name.lowercase().trim() }.toSet()
        val additionalSkills = response.experiences
            .flatMap { it.technologiesUsed }
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .distinctBy { it.lowercase() }
            .filter { it.lowercase() !in existingSkillNames }
            .map { ParsedSkill(name = it, category = "Technology") }

        return if (additionalSkills.isEmpty()) {
            response
        } else {
            response.copy(skills = response.skills + additionalSkills)
        }
    }

    companion object {
        private const val MAX_RESUME_TEXT_LENGTH = 12_000
    }
}
