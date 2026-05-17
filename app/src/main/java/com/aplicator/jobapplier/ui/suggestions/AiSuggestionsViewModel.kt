package com.aplicator.jobapplier.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicator.jobapplier.data.remote.ai.AiSuggestionsResponse
import com.aplicator.jobapplier.data.repository.AiRepository
import com.aplicator.jobapplier.data.repository.JobRepository
import com.aplicator.jobapplier.data.repository.ProfileRepository
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
    private val supabaseClient: SupabaseClient,
) : ViewModel() {

    private val _state = MutableStateFlow<SuggestionsState>(SuggestionsState.Loading)
    val state: StateFlow<SuggestionsState> = _state.asStateFlow()

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
                    aiRepository.saveSuggestions(userId, suggestions)
                    SuggestionsState.Success(suggestions)
                },
                onFailure = { SuggestionsState.Error(it.message ?: "Failed to generate suggestions") },
            )
        }
    }
}
