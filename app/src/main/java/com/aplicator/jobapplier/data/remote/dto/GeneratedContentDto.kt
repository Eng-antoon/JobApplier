package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeneratedContentDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("job_id") val jobId: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("prompt_hash") val promptHash: String? = null,
    @SerialName("prompt_input") val promptInput: String? = null,
    val content: String,
    val tone: String = "professional",
    val version: Int = 1,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class GeneratedContentInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("prompt_hash") val promptHash: String? = null,
    @SerialName("prompt_input") val promptInput: String? = null,
    val content: String,
    val tone: String = "professional",
    val version: Int = 1,
)
