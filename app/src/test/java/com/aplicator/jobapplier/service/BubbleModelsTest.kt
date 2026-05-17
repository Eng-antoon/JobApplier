package com.aplicator.jobapplier.service

import com.aplicator.jobapplier.domain.model.JobDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleModelsTest {
    @Test
    fun defaultBubblePanelPage_isSmartFill() {
        assertEquals(BubblePanelPage.SmartFill, DefaultBubblePanelPage)
    }

    @Test
    fun defaultBubbleAiActions_includeCraftedJobApplicationContent() {
        val actions = defaultBubbleAiActions()

        assertEquals(
            listOf(
                "cover_email",
                "cover_letter",
                "summary",
                "headline",
                "why_work_here",
                "strengths",
                "motivation",
                "custom_question",
            ),
            actions.map { it.contentType },
        )
        assertTrue(actions.first { it.contentType == "summary" }.question?.contains("summary", ignoreCase = true) == true)
        assertEquals(null, actions.first { it.contentType == "custom_question" }.question)
    }

    @Test
    fun bubbleActionKey_scopesLoadingToJobAndContentType() {
        assertEquals("job-1:cover_email", bubbleActionKey("job-1", "cover_email"))
    }

    @Test
    fun toBubbleJobItem_includesCopyableApplicationData() {
        val job = JobDescription(
            id = "job-1",
            companyName = "Northstar Labs",
            roleTitle = "Android Engineer",
            rawText = "Build Android products with Kotlin.",
            sourceUrl = "https://example.com/jobs/android",
            status = "analyzed",
            matchScore = 86,
        )

        val item = job.toBubbleJobItem()

        assertEquals("job-1", item.jobId)
        assertEquals("Northstar Labs", item.companyName)
        assertEquals("Android Engineer", item.roleTitle)
        assertEquals("Build Android products with Kotlin.", item.rawText)
        assertEquals("https://example.com/jobs/android", item.sourceUrl)
    }

    @Test
    fun applicationCopyItems_exposesStableReadableRows() {
        val item = BubbleJobItem(
            jobId = "job-1",
            companyName = "Northstar Labs",
            roleTitle = "Android Engineer",
            matchScore = 86,
            status = "analyzed",
            sourceUrl = "https://example.com/jobs/android",
            rawText = "Build Android products with Kotlin.",
        )

        val rows = item.applicationCopyItems()

        assertEquals(
            listOf(
                "Company Name",
                "Role Title",
                "Status",
                "Match Score",
                "Source URL",
                "Job Description",
            ),
            rows.map { it.label },
        )
        assertEquals("86%", rows.first { it.label == "Match Score" }.value)
    }

    @Test
    fun expandedBubbleLayout_usesCompactResponsiveSheetAndKeepsOpenAfterCopy() {
        val phoneLayout = expandedBubbleLayout(
            screenWidthDp = 393,
            screenHeightDp = 852,
            statusBarDp = 24,
        )
        val tabletLayout = expandedBubbleLayout(
            screenWidthDp = 840,
            screenHeightDp = 1180,
            statusBarDp = 24,
        )

        assertEquals(361, phoneLayout.widthDp)
        assertEquals(664, phoneLayout.heightDp)
        assertEquals(456, tabletLayout.widthDp)
        assertEquals(880, tabletLayout.heightDp)
        assertEquals(false, BubbleCopyBehavior.dismissPanelAfterCopy)
    }
}
