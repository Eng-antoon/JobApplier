package com.aplicator.jobapplier.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun sanitizesSensitiveAndUnsupportedProperties() {
        val event = AnalyticsEvent(
            name = AnalyticsEvents.AI_CONTENT_GENERATION_STARTED,
            properties = mapOf(
                "surface" to "app",
                "content_type" to "cover_letter",
                "email" to "person@example.com",
                "password" to "secret",
                "raw_text" to "full job description",
                "generated_content" to "generated answer",
                "url" to "https://example.com/job",
                "clipboard" to "clipboard value",
                "nullable" to null,
                "unsupported" to listOf("a", "b"),
                "duration_ms" to 1200L,
                "success" to true,
            ),
        )

        val sanitized = event.sanitizedProperties()

        assertEquals("app", sanitized["surface"])
        assertEquals("cover_letter", sanitized["content_type"])
        assertEquals(1200L, sanitized["duration_ms"])
        assertEquals(true, sanitized["success"])
        assertFalse(sanitized.containsKey("email"))
        assertFalse(sanitized.containsKey("password"))
        assertFalse(sanitized.containsKey("raw_text"))
        assertFalse(sanitized.containsKey("generated_content"))
        assertFalse(sanitized.containsKey("url"))
        assertFalse(sanitized.containsKey("clipboard"))
        assertFalse(sanitized.containsKey("nullable"))
        assertFalse(sanitized.containsKey("unsupported"))
    }

    @Test
    fun keepsEventNamesStable() {
        assertEquals("screen_viewed", AnalyticsEvents.SCREEN_VIEWED)
        assertEquals("bubble_launched", AnalyticsEvents.BUBBLE_LAUNCHED)
        assertEquals("job_analysis_succeeded", AnalyticsEvents.JOB_ANALYSIS_SUCCEEDED)
        assertEquals("quota_extra_requested", AnalyticsEvents.QUOTA_EXTRA_REQUESTED)
    }

    @Test
    fun rejectsBlankEventNames() {
        val result = runCatching { AnalyticsEvent(name = " ", properties = emptyMap()) }

        assertTrue(result.isFailure)
    }
}
