package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.data.remote.ai.QuotaRequestRow
import com.aplicator.jobapplier.data.remote.ai.UserQuotaRow

interface QuotaRepository {
    suspend fun getQuotaStatus(userId: String): Result<UserQuotaRow?>
    suspend fun requestExtraQuota(userId: String, type: String): Result<Unit>
    suspend fun hasPendingRequest(userId: String, type: String): Result<Boolean>
}
