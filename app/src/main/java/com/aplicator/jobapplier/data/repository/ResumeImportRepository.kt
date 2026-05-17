package com.aplicator.jobapplier.data.repository

import android.content.Context
import android.net.Uri
import com.aplicator.jobapplier.data.remote.ai.ParsedResumeResponse

interface ResumeImportRepository {
    suspend fun extractText(uri: Uri, context: Context): Result<String>
    suspend fun parseResume(resumeText: String): Result<ParsedResumeResponse>
}
