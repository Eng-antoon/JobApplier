package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EducationDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val institution: String,
    val degree: String,
    @SerialName("field_of_study") val fieldOfStudy: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val gpa: String? = null,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
