package com.aplicator.jobapplier.data.remote.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AiServiceException(cause: Throwable) : Exception(
    "We couldn’t analyze this job right now. Please try again.",
    cause,
)

@Serializable
data class AnalyzeJdResponse(
    val requirements: List<String> = emptyList(),
    @SerialName("match_score") val matchScore: Int = 0,
    val matched: List<String> = emptyList(),
    val gaps: List<String> = emptyList(),
    val partial: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val error: String? = null,
    @SerialName("raw_text") val rawText: String? = null,
)

@Serializable
data class GenerateContentResponse(
    val content: String = "",
)

@Serializable
data class DetectNewDataResponse(
    @SerialName("detected_skills") val detectedSkills: List<DetectedSkill> = emptyList(),
    @SerialName("detected_experiences") val detectedExperiences: List<DetectedExperience> = emptyList(),
    @SerialName("detected_certifications") val detectedCertifications: List<DetectedCertification> = emptyList(),
)

@Serializable
data class DetectedSkill(
    val name: String,
    val category: String? = null,
    val proficiency: String? = null,
)

@Serializable
data class DetectedExperience(
    val company: String? = null,
    val title: String? = null,
    val description: String? = null,
)

@Serializable
data class DetectedCertification(
    val name: String? = null,
    @SerialName("issuing_org") val issuingOrg: String? = null,
)
