package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkExperienceDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val company: String,
    val title: String,
    val location: String? = null,
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_current") val isCurrent: Boolean = false,
    val description: String? = null,
    val achievements: List<String>? = null,
    @SerialName("technologies_used") val technologiesUsed: List<String>? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)
