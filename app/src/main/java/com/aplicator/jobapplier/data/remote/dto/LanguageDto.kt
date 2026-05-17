package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LanguageDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val proficiency: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
