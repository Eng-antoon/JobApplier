package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkillDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val category: String? = null,
    val proficiency: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
