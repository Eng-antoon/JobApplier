package com.aplicator.jobapplier.data.repository

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun awaitCurrentUserId_allowsCallersToWaitForAuthInitialization() = runBlocking {
        val repository = FakeAuthRepository(userId = "user-123")

        assertEquals("user-123", repository.awaitCurrentUserId())
    }

    private class FakeAuthRepository(
        private val userId: String?,
    ) : AuthRepository {
        override val sessionStatus: Flow<SessionStatus> = flowOf(SessionStatus.NotAuthenticated(false))

        override suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> = Result.success(Unit)

        override suspend fun signIn(email: String, password: String): Result<Unit> = Result.success(Unit)

        override suspend fun signOut() = Unit

        override fun getCurrentUserId(): String? = userId

        override suspend fun awaitCurrentUserId(): String? = userId
    }
}
