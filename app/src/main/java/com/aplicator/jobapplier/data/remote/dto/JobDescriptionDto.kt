package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JobDescriptionDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("company_name") val companyName: String,
    @SerialName("role_title") val roleTitle: String,
    @SerialName("raw_text") val rawText: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    val status: String = "draft",
    @SerialName("requirements_extracted") val requirementsExtracted: JsonElement? = null,
    @SerialName("match_score") val matchScore: Int? = null,
    @SerialName("match_details") val matchDetails: JsonElement? = null,
    val notes: String? = null,
    @SerialName("applied_at") val appliedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class JobDescriptionInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("company_name") val companyName: String,
    @SerialName("role_title") val roleTitle: String,
    @SerialName("raw_text") val rawText: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    val status: String = "draft",
    @SerialName("requirements_extracted") val requirementsExtracted: JsonElement? = null,
    @SerialName("match_score") val matchScore: Int? = null,
    @SerialName("match_details") val matchDetails: JsonElement? = null,
)

@Serializable
data class JobDescriptionUpdateDto(
    val status: String? = null,
    @SerialName("requirements_extracted") val requirementsExtracted: JsonElement? = null,
    @SerialName("match_score") val matchScore: Int? = null,
    @SerialName("match_details") val matchDetails: JsonElement? = null,
    val notes: String? = null,
    @SerialName("applied_at") val appliedAt: String? = null,
)
