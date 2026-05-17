package com.aplicator.jobapplier.domain.model

data class Profile(
    val id: String,
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    val linkedinUrl: String? = null,
    val portfolioUrl: String? = null,
    val summary: String? = null,
    val desiredRole: String? = null,
    val desiredSalaryMin: Int? = null,
    val desiredSalaryMax: Int? = null,
    val salaryCurrency: String = "USD",
    val isOnboarded: Boolean = false,
)

data class Skill(
    val id: String = "",
    val name: String,
    val category: String? = null,
    val proficiency: String? = null,
    val yearsExperience: Int? = null,
)

data class WorkExperience(
    val id: String = "",
    val company: String,
    val title: String,
    val location: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val description: String? = null,
    val achievements: List<String> = emptyList(),
    val technologiesUsed: List<String> = emptyList(),
    val sortOrder: Int = 0,
)

data class Education(
    val id: String = "",
    val institution: String,
    val degree: String,
    val fieldOfStudy: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val gpa: String? = null,
    val description: String? = null,
)

data class Certification(
    val id: String = "",
    val name: String,
    val issuingOrg: String,
    val issueDate: String? = null,
    val expiryDate: String? = null,
    val credentialUrl: String? = null,
)

data class Language(
    val id: String = "",
    val name: String,
    val proficiency: String? = null,
)
