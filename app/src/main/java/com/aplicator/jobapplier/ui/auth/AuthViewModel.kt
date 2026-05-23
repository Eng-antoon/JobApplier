package com.aplicator.jobapplier.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.data.repository.AuthRepository
import com.aplicator.jobapplier.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionStatus.Initializing)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isOnboarded = MutableStateFlow<Boolean?>(null)
    val isOnboarded: StateFlow<Boolean?> = _isOnboarded.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.LOGIN_STARTED))
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signIn(email, password)
                .onSuccess {
                    val userId = authRepository.getCurrentUserId()
                    userId?.let(analyticsTracker::identify)
                    analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.LOGIN_SUCCEEDED))
                    analyticsTracker.flush()
                    if (userId != null) {
                        launch {
                            profileRepository.getProfile(userId).getOrNull()?.let { profile ->
                                analyticsTracker.setUserProperties(
                                    mapOf(
                                        "display_name" to profile.fullName,
                                        "desired_role" to profile.desiredRole,
                                        "is_onboarded" to profile.isOnboarded,
                                    ),
                                )
                            }
                        }
                    }
                    _uiState.value = AuthUiState()
                }
                .onFailure {
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.LOGIN_FAILED,
                            mapOf("reason" to it.javaClass.simpleName),
                        ),
                    )
                    _uiState.value = AuthUiState(error = it.message)
                }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.SIGNUP_STARTED))
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signUp(email, password, displayName)
                .onSuccess {
                    val userId = authRepository.getCurrentUserId()
                    userId?.let(analyticsTracker::identify)
                    analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.SIGNUP_SUCCEEDED))
                    analyticsTracker.flush()
                    if (userId != null) {
                        launch {
                            profileRepository.getProfile(userId).getOrNull()?.let { profile ->
                                analyticsTracker.setUserProperties(
                                    mapOf(
                                        "display_name" to profile.fullName,
                                        "is_onboarded" to profile.isOnboarded,
                                        "signup_date" to java.time.LocalDate.now().toString(),
                                    ),
                                )
                            }
                        }
                    }
                    _uiState.value = AuthUiState()
                }
                .onFailure {
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.SIGNUP_FAILED,
                            mapOf("reason" to it.javaClass.simpleName),
                        ),
                    )
                    _uiState.value = AuthUiState(error = it.message)
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.LOGOUT_SUCCEEDED))
            analyticsTracker.flush()
            analyticsTracker.reset()
        }
    }

    fun checkOnboardingStatus() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            profileRepository.getProfile(userId)
                .onSuccess { _isOnboarded.value = it.isOnboarded }
                .onFailure { _isOnboarded.value = false }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
