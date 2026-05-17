package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.domain.model.GeneratedContent
import com.aplicator.jobapplier.domain.model.JobDescription

interface JobRepository {
    suspend fun getJobs(userId: String): Result<List<JobDescription>>
    suspend fun getJob(jobId: String): Result<JobDescription>
    suspend fun insertJob(userId: String, job: JobDescription): Result<String>
    suspend fun updateJobAnalysis(jobId: String, matchScore: Int, matchDetailsJson: String, requirementsJson: String): Result<Unit>
    suspend fun updateJobStatus(jobId: String, status: String): Result<Unit>
    suspend fun deleteJob(jobId: String): Result<Unit>

    suspend fun getGeneratedContent(jobId: String): Result<List<GeneratedContent>>
    suspend fun insertGeneratedContent(userId: String, content: GeneratedContent): Result<Unit>
}
