package com.aplicator.jobapplier.domain.model

data class JobDescription(
    val id: String = "",
    val companyName: String,
    val roleTitle: String,
    val rawText: String,
    val sourceUrl: String? = null,
    val status: String = "draft",
    val matchScore: Int? = null,
    val matchResult: MatchResult? = null,
    val notes: String? = null,
    val appliedAt: String? = null,
    val createdAt: String? = null,
)

data class MatchResult(
    val requirements: List<String> = emptyList(),
    val matchScore: Int = 0,
    val matched: List<String> = emptyList(),
    val gaps: List<String> = emptyList(),
    val partial: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

data class GeneratedContent(
    val id: String = "",
    val jobId: String,
    val contentType: String,
    val content: String,
    val tone: String = "professional",
    val version: Int = 1,
    val isFavorite: Boolean = false,
)

data class UserProfileSnapshot(
    val fullName: String,
    val email: String?,
    val desiredRole: String?,
    val summary: String?,
    val skills: List<Skill>,
    val experiences: List<WorkExperience>,
    val educationList: List<Education>,
    val certifications: List<Certification>,
    val languages: List<Language>,
) {
    fun toPromptText(): String = buildString {
        appendLine("NAME: $fullName")
        email?.let { appendLine("EMAIL: $it") }
        desiredRole?.let { appendLine("TARGET ROLE: $it") }
        summary?.let { appendLine("SUMMARY: $it") }

        if (skills.isNotEmpty()) {
            appendLine("SKILLS: ${skills.joinToString(", ") { s ->
                buildString {
                    append(s.name)
                    s.proficiency?.let { append(" ($it") }
                    s.yearsExperience?.let { append(", ${it}yr") }
                    if (s.proficiency != null) append(")")
                }
            }}")
        }

        experiences.take(3).forEach { exp ->
            appendLine("EXPERIENCE: ${exp.title} at ${exp.company} (${exp.startDate} - ${exp.endDate ?: "Present"})")
            exp.description?.let { appendLine("  $it") }
        }

        educationList.forEach { edu ->
            appendLine("EDUCATION: ${edu.degree}${edu.fieldOfStudy?.let { " in $it" } ?: ""} - ${edu.institution}")
        }

        certifications.forEach { cert ->
            appendLine("CERTIFICATION: ${cert.name} (${cert.issuingOrg})")
        }

        if (languages.isNotEmpty()) {
            appendLine("LANGUAGES: ${languages.joinToString(", ") { l -> "${l.name}${l.proficiency?.let { " ($it)" } ?: ""}" }}")
        }
    }
}
