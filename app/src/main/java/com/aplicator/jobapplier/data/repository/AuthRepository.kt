package com.aplicator.jobapplier.data.repository

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionStatus: Flow<SessionStatus>
    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
    fun getCurrentUserId(): String?
    suspend fun awaitCurrentUserId(): String?
}
