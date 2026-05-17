package com.aplicator.jobapplier.data.remote.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiSuggestionsResponse(
    @SerialName("headline_suggestions") val headlineSuggestions: List<String> = emptyList(),
    @SerialName("summary_rewrites") val summaryRewrites: List<String> = emptyList(),
    @SerialName("skill_gaps") val skillGaps: List<SkillGapSuggestion> = emptyList(),
    @SerialName("cover_email_templates") val coverEmailTemplates: List<CoverEmailTemplate> = emptyList(),
    @SerialName("general_tips") val generalTips: List<String> = emptyList(),
)

@Serializable
data class SkillGapSuggestion(
    val skill: String,
    val reason: String,
    val priority: String,
)

@Serializable
data class CoverEmailTemplate(
    @SerialName("job_id") val jobId: String,
    val template: String,
)
