package com.aplicator.jobapplier.data.remote.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class AiProxyRequest(
    val action: String,
    val payload: JsonObject,
)
