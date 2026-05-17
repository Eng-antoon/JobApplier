package com.aplicator.jobapplier.service

import com.aplicator.jobapplier.domain.model.JobDescription
import com.aplicator.jobapplier.ui.components.displayStatus

data class BubbleJobItem(
    val jobId: String,
    val companyName: String,
    val roleTitle: String,
    val matchScore: Int?,
    val status: String,
    val sourceUrl: String? = null,
    val rawText: String? = null,
)

data class BubbleContentItem(
    val contentType: String,
    val content: String,
    val tone: String,
    val createdAt: String?,
)

data class BubbleCopyItem(
    val label: String,
    val value: String,
)

enum class BubblePanelPage {
    SmartFill,
    QuickCopy,
    Actions,
}

val DefaultBubblePanelPage = BubblePanelPage.SmartFill

data class BubbleAiAction(
    val contentType: String,
    val label: String,
    val question: String?,
)

fun defaultBubbleAiActions(): List<BubbleAiAction> = listOf(
    BubbleAiAction(
        contentType = "cover_email",
        label = "Cover Email",
        question = null,
    ),
    BubbleAiAction(
        contentType = "cover_letter",
        label = "Cover Letter",
        question = null,
    ),
    BubbleAiAction(
        contentType = "summary",
        label = "Summary",
        question = "Write a concise professional summary tailored to this job application.",
    ),
    BubbleAiAction(
        contentType = "headline",
        label = "Headline",
        question = "Write a concise professional headline tailored to this job.",
    ),
    BubbleAiAction(
        contentType = "why_work_here",
        label = "Why This Company",
        question = "Why do I want to work at this company?",
    ),
    BubbleAiAction(
        contentType = "strengths",
        label = "Strengths",
        question = "What strengths should I highlight for this role?",
    ),
    BubbleAiAction(
        contentType = "motivation",
        label = "Motivation",
        question = "Write a strong answer about my motivation for this role.",
    ),
    BubbleAiAction(
        contentType = "custom_question",
        label = "Custom Question",
        question = null,
    ),
)

fun bubbleActionKey(jobId: String, contentType: String): String = "$jobId:$contentType"

fun JobDescription.toBubbleJobItem(): BubbleJobItem = BubbleJobItem(
    jobId = id,
    companyName = companyName,
    roleTitle = roleTitle,
    matchScore = matchScore,
    status = status,
    sourceUrl = sourceUrl,
    rawText = rawText,
)

fun BubbleJobItem.applicationCopyItems(): List<BubbleCopyItem> = buildList {
    add(BubbleCopyItem("Company Name", companyName))
    add(BubbleCopyItem("Role Title", roleTitle))
    add(BubbleCopyItem("Status", displayStatus(status)))
    matchScore?.let { add(BubbleCopyItem("Match Score", "$it%")) }
    sourceUrl?.takeIf { it.isNotBlank() }?.let { add(BubbleCopyItem("Source URL", it)) }
    rawText?.takeIf { it.isNotBlank() }?.let { add(BubbleCopyItem("Job Description", it)) }
}
