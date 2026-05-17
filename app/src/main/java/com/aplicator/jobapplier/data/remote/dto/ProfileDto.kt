package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    @SerialName("linkedin_url") val linkedinUrl: String? = null,
    @SerialName("portfolio_url") val portfolioUrl: String? = null,
    val summary: String? = null,
    @SerialName("desired_role") val desiredRole: String? = null,
    @SerialName("desired_salary_min") val desiredSalaryMin: Int? = null,
    @SerialName("desired_salary_max") val desiredSalaryMax: Int? = null,
    @SerialName("salary_currency") val salaryCurrency: String? = "USD",
    @SerialName("is_onboarded") val isOnboarded: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ProfileUpdateDto(
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    @SerialName("linkedin_url") val linkedinUrl: String? = null,
    @SerialName("portfolio_url") val portfolioUrl: String? = null,
    val summary: String? = null,
    @SerialName("desired_role") val desiredRole: String? = null,
    @SerialName("desired_salary_min") val desiredSalaryMin: Int? = null,
    @SerialName("desired_salary_max") val desiredSalaryMax: Int? = null,
    @SerialName("salary_currency") val salaryCurrency: String? = null,
    @SerialName("is_onboarded") val isOnboarded: Boolean? = null,
)
