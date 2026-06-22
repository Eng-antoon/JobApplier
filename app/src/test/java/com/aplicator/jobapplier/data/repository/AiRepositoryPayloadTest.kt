package com.aplicator.jobapplier.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class AiRepositoryPayloadTest {

    @Test
    fun `answer question payload includes selected tone`() {
        val payload = buildAnswerQuestionPayload(
            question = "Why do you want to work here?",
            questionType = "why_work_here",
            jobDescription = "Finaira Senior Product Manager",
            userProfile = "AI Product Manager",
            tone = "casual",
        )

        assertEquals("casual", payload["tone"])
        assertEquals("why_work_here", payload["question_type"])
        assertEquals("Finaira Senior Product Manager", payload["job_description"])
    }

    @Test
    fun `generated response fails when persistence fails`() = runBlocking {
        val result = Result.success("generated answer").requireSuccessfulPersistence {
            Result.failure(IllegalStateException("insert failed"))
        }

        assertTrue(result.isFailure)
        assertEquals("insert failed", result.exceptionOrNull()?.message)
    }
}
