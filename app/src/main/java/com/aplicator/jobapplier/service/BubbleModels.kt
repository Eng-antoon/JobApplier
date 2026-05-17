package com.aplicator.jobapplier.service

data class BubbleJobItem(
    val jobId: String,
    val companyName: String,
    val roleTitle: String,
    val matchScore: Int?,
    val status: String,
)

data class BubbleContentItem(
    val contentType: String,
    val content: String,
    val tone: String,
    val createdAt: String?,
)
