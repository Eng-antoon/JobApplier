package com.aplicator.jobapplier.data.remote.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AiServiceExceptionTest {

    @Test
    fun `transport diagnostics are replaced with a safe user message`() {
        val diagnostic = "Headers Authorization=Bearer secret-session-token apikey=public-key"

        val error = AiServiceException(IllegalStateException(diagnostic))

        assertEquals("We couldn’t analyze this job right now. Please try again.", error.message)
        assertFalse(error.message.orEmpty().contains("Bearer"))
        assertFalse(error.message.orEmpty().contains("apikey"))
        assertEquals(diagnostic, error.cause?.message)
    }
}
