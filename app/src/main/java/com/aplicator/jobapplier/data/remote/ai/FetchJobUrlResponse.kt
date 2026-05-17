package com.aplicator.jobapplier.data.remote.ai

import kotlinx.serialization.Serializable

@Serializable
data class FetchJobUrlResponse(
    val success: Boolean,
    val title: String? = null,
    val company: String? = null,
    val description: String? = null,
    val requirements: List<String> = emptyList(),
    val reason: String? = null,
)
