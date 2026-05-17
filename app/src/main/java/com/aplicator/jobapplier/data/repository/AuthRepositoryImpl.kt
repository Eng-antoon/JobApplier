package com.aplicator.jobapplier.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : AuthRepository {

    override val sessionStatus: Flow<SessionStatus>
        get() = supabaseClient.auth.sessionStatus

    override suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> {
        return runCatching {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("display_name", displayName)
                }
            }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return runCatching {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }

    override suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    override suspend fun awaitCurrentUserId(): String? {
        supabaseClient.auth.awaitInitialization()
        return getCurrentUserId()
    }
}
