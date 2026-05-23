package com.aplicator.jobapplier.analytics

data class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any?> = emptyMap(),
) {
    init {
        require(name.isNotBlank()) { "Analytics event name cannot be blank." }
    }

    fun sanitizedProperties(): Map<String, Any> {
        return properties
            .filterKeys { it !in SensitivePropertyKeys }
            .mapNotNull { (key, value) ->
                val sanitized = when (value) {
                    is String -> value.take(MAX_STRING_PROPERTY_LENGTH)
                    is Boolean -> value
                    is Int -> value
                    is Long -> value
                    is Float -> value
                    is Double -> value
                    else -> null
                }
                sanitized?.let { key to it }
            }
            .toMap()
    }

    companion object {
        private const val MAX_STRING_PROPERTY_LENGTH = 120
        private val SensitivePropertyKeys = setOf(
            "clipboard",
            "custom_question",
            "email",
            "generated_content",
            "job_description",
            "password",
            "raw_text",
            "resume_text",
            "source_url",
            "url",
        )
    }
}
