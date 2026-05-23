package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.data.remote.ai.QuotaRequestRow
import com.aplicator.jobapplier.data.remote.ai.UserQuotaRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotaRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : QuotaRepository {

    override suspend fun getQuotaStatus(userId: String): Result<UserQuotaRow?> = runCatching {
        supabaseClient.postgrest.from("user_quotas").select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<UserQuotaRow>()
    }

    override suspend fun requestExtraQuota(userId: String, type: String): Result<Unit> = runCatching {
        supabaseClient.postgrest.from("quota_requests").insert(
            QuotaRequestRow(userId = userId, requestType = type)
        )
    }

    override suspend fun hasPendingRequest(userId: String, type: String): Result<Boolean> = runCatching {
        val rows = supabaseClient.postgrest.from("quota_requests").select {
            filter {
                eq("user_id", userId)
                eq("status", "pending")
                eq("request_type", type)
            }
        }.decodeList<QuotaRequestRow>()
        rows.isNotEmpty()
    }
}
