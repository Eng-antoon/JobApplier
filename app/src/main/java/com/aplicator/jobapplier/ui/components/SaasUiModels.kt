package com.aplicator.jobapplier.ui.components

import com.aplicator.jobapplier.data.remote.ai.UserQuotaRow
import com.aplicator.jobapplier.domain.model.JobDescription
import com.aplicator.jobapplier.ui.profile.ProfileUiState
import kotlin.math.roundToInt

data class DashboardMetrics(
    val totalApplications: Int,
    val averageMatch: Int,
    val activeApplications: Int,
    val draftApplications: Int,
)

data class ProfileCompleteness(
    val percent: Int,
    val completedItems: Int,
    val totalItems: Int,
)

data class AiFillAction(
    val contentType: String,
    val label: String,
    val question: String? = null,
)

data class QuotaUsageSummary(
    val weeklyUsage: String,
    val weeklyExtra: String?,
    val resumeUsage: String,
    val resumeExtra: String?,
)

const val DefaultUseDarkTheme: Boolean = false

fun buildDashboardMetrics(jobs: List<JobDescription>): DashboardMetrics {
    val scores = jobs.mapNotNull { it.matchScore }
    val activeStatuses = setOf("applied", "interviewing", "offer")
    return DashboardMetrics(
        totalApplications = jobs.size,
        averageMatch = if (scores.isEmpty()) 0 else scores.average().toInt(),
        activeApplications = jobs.count { it.status.lowercase() in activeStatuses },
        draftApplications = jobs.count { it.status.equals("draft", ignoreCase = true) },
    )
}

fun filterJobs(jobs: List<JobDescription>, query: String): List<JobDescription> {
    val normalized = query.trim()
    if (normalized.isBlank()) return jobs
    return jobs.filter { job ->
        job.companyName.contains(normalized, ignoreCase = true) ||
            job.roleTitle.contains(normalized, ignoreCase = true) ||
            job.status.contains(normalized, ignoreCase = true)
    }
}

fun calculateProfileCompleteness(state: ProfileUiState): ProfileCompleteness {
    val profile = state.profile
    val checks = listOf(
        !profile?.fullName.isNullOrBlank(),
        !profile?.email.isNullOrBlank(),
        !profile?.phone.isNullOrBlank(),
        !profile?.location.isNullOrBlank(),
        !profile?.linkedinUrl.isNullOrBlank(),
        !profile?.summary.isNullOrBlank(),
        !profile?.desiredRole.isNullOrBlank(),
        state.skills.isNotEmpty(),
        state.experiences.isNotEmpty(),
    )
    val completed = checks.count { it }
    return ProfileCompleteness(
        percent = ((completed.toFloat() / checks.size) * 100).roundToInt(),
        completedItems = completed,
        totalItems = checks.size,
    )
}

fun displayStatus(status: String): String =
    status.replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        .ifBlank { "Draft" }

fun companyInitials(companyName: String): String =
    companyName.split(' ', '-', '.', '&')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "J" }

fun generatedContentLabel(contentType: String): String = when (contentType) {
    "cover_letter" -> "Cover Letter"
    "cover_email" -> "Cover Email"
    "headline" -> "Professional Headline"
    "why_work_here" -> "Why Work Here"
    "strengths" -> "Strengths"
    "motivation" -> "Tell Me About Yourself"
    "custom_question" -> "Custom Question"
    else -> displayStatus(contentType)
}

fun defaultAiFillActions(): List<AiFillAction> = listOf(
    AiFillAction("cover_email", "Cover Email"),
    AiFillAction("cover_letter", "Cover Letter"),
    AiFillAction(
        contentType = "headline",
        label = "Professional Headline",
        question = "Write a concise professional headline tailored to this job application.",
    ),
    AiFillAction("why_work_here", "Why this company?", "Why do you want to work here?"),
    AiFillAction("strengths", "Strengths", "What are your strengths?"),
    AiFillAction("motivation", "Tell me about yourself", "Tell me about yourself"),
)

fun buildQuotaUsageSummary(quota: UserQuotaRow): QuotaUsageSummary = QuotaUsageSummary(
    weeklyUsage = "${quota.weeklyUsageCount} / ${quota.weeklyAiLimit} weekly AI actions used",
    weeklyExtra = quota.extraQuotaRemaining.takeIf { it > 0 }?.let {
        "$it extra weekly ${if (it == 1) "action" else "actions"} available"
    },
    resumeUsage = "${quota.resumeParseCount} / ${quota.resumeParseLimit} resume parses used",
    resumeExtra = quota.extraResumeParseRemaining.takeIf { it > 0 }?.let {
        "$it extra resume ${if (it == 1) "parse" else "parses"} available"
    },
)

fun quotaRequestActionLabel(quotaType: String): String = when (quotaType) {
    "resume" -> "Request 2 Resume Parses"
    else -> "Request 10 Weekly AI Actions"
}

fun quotaRequestSuccessMessage(quotaType: String): String = when (quotaType) {
    "resume" -> "Your request for 2 extra resume parses was sent to the app owner. Once approved, close this message and retry the resume import."
    else -> "Your request for 10 extra weekly AI actions was sent to the app owner. Once approved, close this message and retry the AI action."
}
