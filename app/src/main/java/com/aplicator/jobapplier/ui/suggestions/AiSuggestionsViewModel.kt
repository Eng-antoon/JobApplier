package com.aplicator.jobapplier.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.data.remote.ai.AiSuggestionsResponse
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededException
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededResponse
import com.aplicator.jobapplier.data.repository.AiRepository
import com.aplicator.jobapplier.data.repository.JobRepository
import com.aplicator.jobapplier.data.repository.ProfileRepository
import com.aplicator.jobapplier.data.repository.QuotaRepository
import com.aplicator.jobapplier.domain.model.UserProfileSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SuggestionsState {
    data object Loading : SuggestionsState
    data class Success(val suggestions: AiSuggestionsResponse) : SuggestionsState
    data class Error(val message: String) : SuggestionsState
    data object Empty : SuggestionsState
}

@HiltViewModel
class AiSuggestionsViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val profileRepository: ProfileRepository,
    private val jobRepository: JobRepository,
    private val quotaRepository: QuotaRepository,
    private val supabaseClient: SupabaseClient,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow<SuggestionsState>(SuggestionsState.Loading)
    val state: StateFlow<SuggestionsState> = _state.asStateFlow()

    private val _quotaExceeded = MutableStateFlow<QuotaExceededResponse?>(null)
    val quotaExceeded: StateFlow<QuotaExceededResponse?> = _quotaExceeded.asStateFlow()

    private val _quotaRequestPending = MutableStateFlow(false)
    val quotaRequestPending: StateFlow<Boolean> = _quotaRequestPending.asStateFlow()

    private val _quotaRequestSuccess = MutableStateFlow(false)
    val quotaRequestSuccess: StateFlow<Boolean> = _quotaRequestSuccess.asStateFlow()

    private val _quotaRequestSuccessType = MutableStateFlow("weekly")
    val quotaRequestSuccessType: StateFlow<String> = _quotaRequestSuccessType.asStateFlow()

    init {
        loadCached()
    }

    fun refresh() {
        fetchFromAi()
    }

    private fun loadCached() {
        viewModelScope.launch {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            if (userId == null) {
                _state.value = SuggestionsState.Error("Not authenticated")
                return@launch
            }

            val cached = aiRepository.getCachedSuggestions(userId).getOrNull()
            if (cached != null) {
                _state.value = SuggestionsState.Success(cached)
            } else {
                _state.value = SuggestionsState.Empty
            }
        }
    }

    private fun fetchFromAi() {
        viewModelScope.launch {
            _state.value = SuggestionsState.Loading
            analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.SUGGESTIONS_REFRESHED_STARTED))

            val userId = supabaseClient.auth.currentUserOrNull()?.id
            if (userId == null) {
                _state.value = SuggestionsState.Error("Not authenticated")
                return@launch
            }

            val profileResult = profileRepository.getProfile(userId)
            if (profileResult.isFailure) {
                _state.value = SuggestionsState.Error("Could not load profile")
                return@launch
            }

            val profile = profileResult.getOrNull()
            if (profile == null) {
                _state.value = SuggestionsState.Empty
                return@launch
            }

            val skills = profileRepository.getSkills(userId).getOrNull() ?: emptyList()
            val experiences = profileRepository.getWorkExperiences(userId).getOrNull() ?: emptyList()
            val education = profileRepository.getEducation(userId).getOrNull() ?: emptyList()

            val snapshot = UserProfileSnapshot(
                fullName = profile.fullName,
                email = profile.email,
                desiredRole = profile.desiredRole,
                summary = profile.summary,
                skills = skills,
                experiences = experiences,
                educationList = education,
                certifications = emptyList(),
                languages = emptyList(),
            )

            val jobs = jobRepository.getJobs(userId).getOrNull() ?: emptyList()
            val jobsText = if (jobs.isNotEmpty()) {
                jobs.take(5).joinToString("\n---\n") { job ->
                    "Role: ${job.roleTitle}\nCompany: ${job.companyName}\nDescription: ${job.rawText.take(500)}"
                }
            } else null

            val result = aiRepository.generateSuggestions(
                userProfile = snapshot.toPromptText(),
                jobDescriptions = jobsText,
            )

            _state.value = result.fold(
                onSuccess = { suggestions ->
                    analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.SUGGESTIONS_REFRESHED_SUCCEEDED))
                    aiRepository.saveSuggestions(userId, suggestions)
                    SuggestionsState.Success(suggestions)
                },
                onFailure = { error ->
                    if (error is QuotaExceededException) {
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.QUOTA_LIMIT_SHOWN,
                                mapOf("quota_type" to error.quotaInfo.quotaType, "surface" to "suggestions"),
                            ),
                        )
                        _quotaExceeded.value = error.quotaInfo
                        checkPendingRequest(error.quotaInfo.quotaType)
                        SuggestionsState.Empty
                    } else {
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.SUGGESTIONS_REFRESHED_FAILED,
                                mapOf("reason" to error.javaClass.simpleName),
                            ),
                        )
                        SuggestionsState.Error(error.message ?: "Failed to generate suggestions")
                    }
                },
            )
        }
    }

    fun dismissQuotaDialog() {
        _quotaExceeded.value = null
    }

    fun dismissQuotaRequestSuccess() {
        _quotaRequestSuccess.value = false
    }

    fun requestExtraQuota() {
        val quota = _quotaExceeded.value ?: return
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            quotaRepository.requestExtraQuota(uid, quota.quotaType)
                .onSuccess {
                    _quotaRequestSuccessType.value = quota.quotaType
                    _quotaRequestSuccess.value = true
                    _quotaExceeded.value = null
                    _quotaRequestPending.value = true
                }
        }
    }

    private fun checkPendingRequest(type: String) {
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _quotaRequestPending.value = quotaRepository.hasPendingRequest(uid, type).getOrDefault(false)
        }
    }
}
