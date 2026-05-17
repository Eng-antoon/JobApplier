package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.data.remote.dto.GeneratedContentDto
import com.aplicator.jobapplier.data.remote.dto.GeneratedContentInsertDto
import com.aplicator.jobapplier.data.remote.dto.JobDescriptionDto
import com.aplicator.jobapplier.data.remote.dto.JobDescriptionInsertDto
import com.aplicator.jobapplier.data.remote.dto.JobDescriptionUpdateDto
import com.aplicator.jobapplier.domain.model.GeneratedContent
import com.aplicator.jobapplier.domain.model.JobDescription
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : JobRepository {

    private val db get() = supabaseClient.postgrest
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getJobs(userId: String): Result<List<JobDescription>> = runCatching {
        db.from("job_descriptions").select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<JobDescriptionDto>().map { it.toDomain() }
    }

    override suspend fun getJob(jobId: String): Result<JobDescription> = runCatching {
        db.from("job_descriptions").select {
            filter { eq("id", jobId) }
        }.decodeSingle<JobDescriptionDto>().toDomain()
    }

    override suspend fun insertJob(userId: String, job: JobDescription): Result<String> = runCatching {
        val result = db.from("job_descriptions").insert(
            JobDescriptionInsertDto(
                userId = userId,
                companyName = job.companyName,
                roleTitle = job.roleTitle,
                rawText = job.rawText,
                sourceUrl = job.sourceUrl,
            )
        ) { select() }.decodeSingle<JobDescriptionDto>()
        result.id ?: throw IllegalStateException("Insert did not return ID")
    }

    override suspend fun updateJobAnalysis(
        jobId: String,
        matchScore: Int,
        matchDetailsJson: String,
        requirementsJson: String,
    ): Result<Unit> = runCatching {
        db.from("job_descriptions").update(
            JobDescriptionUpdateDto(
                status = "analyzed",
                matchScore = matchScore,
                matchDetails = json.parseToJsonElement(matchDetailsJson),
                requirementsExtracted = json.parseToJsonElement(requirementsJson),
            )
        ) {
            filter { eq("id", jobId) }
        }
    }

    override suspend fun updateJobStatus(jobId: String, status: String): Result<Unit> = runCatching {
        db.from("job_descriptions").update(
            JobDescriptionUpdateDto(status = status)
        ) {
            filter { eq("id", jobId) }
        }
    }

    override suspend fun deleteJob(jobId: String): Result<Unit> = runCatching {
        db.from("job_descriptions").delete { filter { eq("id", jobId) } }
    }

    override suspend fun getGeneratedContent(jobId: String): Result<List<GeneratedContent>> = runCatching {
        db.from("generated_content").select {
            filter { eq("job_id", jobId) }
        }.decodeList<GeneratedContentDto>().map { it.toDomain() }
    }

    override suspend fun insertGeneratedContent(userId: String, content: GeneratedContent): Result<Unit> = runCatching {
        db.from("generated_content").insert(
            GeneratedContentInsertDto(
                userId = userId,
                jobId = content.jobId,
                contentType = content.contentType,
                content = content.content,
                tone = content.tone,
                version = content.version,
            )
        )
    }
}

private fun JobDescriptionDto.toDomain() = JobDescription(
    id = id ?: "",
    companyName = companyName,
    roleTitle = roleTitle,
    rawText = rawText,
    sourceUrl = sourceUrl,
    status = status,
    matchScore = matchScore,
    notes = notes,
    appliedAt = appliedAt,
    createdAt = createdAt,
)

private fun GeneratedContentDto.toDomain() = GeneratedContent(
    id = id ?: "",
    jobId = jobId,
    contentType = contentType,
    content = content,
    tone = tone,
    version = version,
    isFavorite = isFavorite,
)
