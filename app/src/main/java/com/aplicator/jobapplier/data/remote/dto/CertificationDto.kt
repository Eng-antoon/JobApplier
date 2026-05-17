package com.aplicator.jobapplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CertificationDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    @SerialName("issuing_org") val issuingOrg: String,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("credential_url") val credentialUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
