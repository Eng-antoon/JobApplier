package com.aplicator.jobapplier.data.remote.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuotaExceededResponse(
    val error: String,
    @SerialName("quota_type") val quotaType: String,
    val used: Int,
    val limit: Int,
    @SerialName("extra_remaining") val extraRemaining: Int,
    @SerialName("weekly_extra_remaining") val weeklyExtraRemaining: Int? = null,
    @SerialName("resume_extra_remaining") val resumeExtraRemaining: Int? = null,
    @SerialName("resets_at") val resetsAt: String? = null,
    @SerialName("can_request_extra") val canRequestExtra: Boolean,
)

class QuotaExceededException(val quotaInfo: QuotaExceededResponse) : Exception("Quota exceeded: ${quotaInfo.quotaType}")

@Serializable
data class UserQuotaRow(
    @SerialName("user_id") val userId: String,
    @SerialName("weekly_ai_limit") val weeklyAiLimit: Int = 15,
    @SerialName("resume_parse_limit") val resumeParseLimit: Int = 2,
    @SerialName("extra_quota_remaining") val extraQuotaRemaining: Int = 0,
    @SerialName("extra_resume_parse_remaining") val extraResumeParseRemaining: Int = 0,
    @SerialName("week_start") val weekStart: String = "",
    @SerialName("weekly_usage_count") val weeklyUsageCount: Int = 0,
    @SerialName("resume_parse_count") val resumeParseCount: Int = 0,
)

@Serializable
data class QuotaRequestRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val status: String = "pending",
    @SerialName("request_type") val requestType: String = "weekly",
    @SerialName("created_at") val createdAt: String? = null,
)
