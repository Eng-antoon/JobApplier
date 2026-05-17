package com.aplicator.jobapplier.data.remote.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParsedResumeResponse(
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    @SerialName("linkedin_url") val linkedinUrl: String? = null,
    val summary: String? = null,
    @SerialName("desired_role") val desiredRole: String? = null,
    val skills: List<ParsedSkill> = emptyList(),
    val experiences: List<ParsedExperience> = emptyList(),
    val education: List<ParsedEducation> = emptyList(),
    val certifications: List<ParsedCertification> = emptyList(),
    val languages: List<ParsedLanguage> = emptyList(),
)

@Serializable
data class ParsedSkill(
    val name: String,
    val category: String? = null,
    val proficiency: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
)

@Serializable
data class ParsedExperience(
    val company: String,
    val title: String,
    val location: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_current") val isCurrent: Boolean = false,
    val description: String? = null,
    val achievements: List<String> = emptyList(),
    @SerialName("technologies_used") val technologiesUsed: List<String> = emptyList(),
)

@Serializable
data class ParsedEducation(
    val institution: String,
    val degree: String,
    @SerialName("field_of_study") val fieldOfStudy: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val gpa: String? = null,
    val description: String? = null,
)

@Serializable
data class ParsedCertification(
    val name: String,
    @SerialName("issuing_org") val issuingOrg: String? = null,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("credential_url") val credentialUrl: String? = null,
)

@Serializable
data class ParsedLanguage(
    val name: String,
    val proficiency: String? = null,
)
