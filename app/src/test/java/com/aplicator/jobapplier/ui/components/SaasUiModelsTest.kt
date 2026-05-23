package com.aplicator.jobapplier.ui.components

import com.aplicator.jobapplier.domain.model.JobDescription
import com.aplicator.jobapplier.domain.model.Profile
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.domain.model.WorkExperience
import com.aplicator.jobapplier.data.remote.ai.UserQuotaRow
import com.aplicator.jobapplier.ui.profile.ProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class SaasUiModelsTest {
    @Test
    fun dashboardMetrics_ignoreMissingScoresAndCountActiveStatuses() {
        val jobs = listOf(
            job("1", "Acme", "Android Engineer", "applied", 82),
            job("2", "Beta", "Mobile Lead", "interviewing", 68),
            job("3", "Core", "Product Engineer", "draft", null),
        )

        val metrics = buildDashboardMetrics(jobs)

        assertEquals(3, metrics.totalApplications)
        assertEquals(75, metrics.averageMatch)
        assertEquals(2, metrics.activeApplications)
        assertEquals(1, metrics.draftApplications)
    }

    @Test
    fun filterJobs_matchesCompanyRoleAndStatusCaseInsensitively() {
        val jobs = listOf(
            job("1", "Northstar Labs", "Android Engineer", "applied", 80),
            job("2", "SignalWorks", "Backend Engineer", "draft", 44),
        )

        assertEquals(listOf("1"), filterJobs(jobs, "north").map { it.id })
        assertEquals(listOf("2"), filterJobs(jobs, "BACKEND").map { it.id })
        assertEquals(listOf("1"), filterJobs(jobs, "Applied").map { it.id })
        assertEquals(listOf("1", "2"), filterJobs(jobs, "").map { it.id })
    }

    @Test
    fun profileCompleteness_usesCurrentFrontendData() {
        val state = ProfileUiState(
            profile = Profile(
                id = "p1",
                fullName = "Maya Chen",
                email = "maya@example.com",
                phone = "555",
                summary = "Product-minded mobile engineer.",
                desiredRole = "Senior Android Engineer",
            ),
            skills = listOf(Skill(name = "Kotlin")),
            experiences = listOf(WorkExperience(company = "Acme", title = "Engineer")),
        )

        assertEquals(78, calculateProfileCompleteness(state).percent)
    }

    @Test
    fun themePolicy_usesBrightModeByDefault() {
        assertEquals(false, DefaultUseDarkTheme)
    }

    @Test
    fun generatedContentLabels_areReadableForAiFillActions() {
        assertEquals("Cover Letter", generatedContentLabel("cover_letter"))
        assertEquals("Cover Email", generatedContentLabel("cover_email"))
        assertEquals("Professional Headline", generatedContentLabel("headline"))
        assertEquals("Custom Question", generatedContentLabel("custom_question"))
    }

    @Test
    fun aiFillActions_includeHeadlineAndPresetQuestions() {
        assertEquals(
            listOf(
                "cover_email",
                "cover_letter",
                "headline",
                "why_work_here",
                "strengths",
                "motivation",
            ),
            defaultAiFillActions().map { it.contentType },
        )
        assertEquals(
            "Write a concise professional headline tailored to this job application.",
            defaultAiFillActions().first { it.contentType == "headline" }.question,
        )
    }

    @Test
    fun quotaUsageSummary_showsWeeklyAndResumeExtrasSeparately() {
        val quota = UserQuotaRow(
            userId = "user-1",
            weeklyAiLimit = 15,
            weeklyUsageCount = 15,
            extraQuotaRemaining = 7,
            resumeParseLimit = 2,
            resumeParseCount = 2,
            extraResumeParseRemaining = 1,
        )

        val summary = buildQuotaUsageSummary(quota)

        assertEquals("15 / 15 weekly AI actions used", summary.weeklyUsage)
        assertEquals("7 extra weekly actions available", summary.weeklyExtra)
        assertEquals("2 / 2 resume parses used", summary.resumeUsage)
        assertEquals("1 extra resume parse available", summary.resumeExtra)
    }

    @Test
    fun quotaRequestLabels_areSpecificToRequestType() {
        assertEquals("Request 10 Weekly AI Actions", quotaRequestActionLabel("weekly"))
        assertEquals("Request 2 Resume Parses", quotaRequestActionLabel("resume"))
    }

    private fun job(
        id: String,
        company: String,
        role: String,
        status: String,
        score: Int?,
    ) = JobDescription(
        id = id,
        companyName = company,
        roleTitle = role,
        rawText = "Build products",
        status = status,
        matchScore = score,
    )
}
