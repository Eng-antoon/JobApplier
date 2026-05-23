package com.aplicator.jobapplier.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.data.event.ProfileRefreshTrigger
import com.aplicator.jobapplier.data.repository.AuthRepository
import com.aplicator.jobapplier.data.repository.ProfileRepository
import com.aplicator.jobapplier.service.BubbleDataProvider
import com.aplicator.jobapplier.ui.snippets.SnippetItem
import com.aplicator.jobapplier.domain.model.Certification
import com.aplicator.jobapplier.domain.model.Education
import com.aplicator.jobapplier.domain.model.Language
import com.aplicator.jobapplier.domain.model.Profile
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.domain.model.WorkExperience
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val skills: List<Skill> = emptyList(),
    val experiences: List<WorkExperience> = emptyList(),
    val educationList: List<Education> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val languages: List<Language> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val bubbleDataProvider: BubbleDataProvider,
    private val profileRefreshTrigger: ProfileRefreshTrigger,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val userId: String?
        get() = authRepository.getCurrentUserId()

    init {
        loadProfile()
        profileRefreshTrigger.refreshEvent
            .onEach { loadProfile() }
            .launchIn(viewModelScope)
    }

    fun loadProfile() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profileResult = async { profileRepository.getProfile(uid) }
            val skillsResult = async { profileRepository.getSkills(uid) }
            val experiencesResult = async { profileRepository.getWorkExperiences(uid) }
            val educationResult = async { profileRepository.getEducation(uid) }
            val certsResult = async { profileRepository.getCertifications(uid) }
            val langsResult = async { profileRepository.getLanguages(uid) }

            val newState = ProfileUiState(
                isLoading = false,
                profile = profileResult.await().getOrNull(),
                skills = skillsResult.await().getOrDefault(emptyList()),
                experiences = experiencesResult.await().getOrDefault(emptyList()),
                educationList = educationResult.await().getOrDefault(emptyList()),
                certifications = certsResult.await().getOrDefault(emptyList()),
                languages = langsResult.await().getOrDefault(emptyList()),
                error = profileResult.await().exceptionOrNull()?.message,
            )
            _uiState.value = newState
            pushSnippetsToBubble(newState)
        }
    }

    fun updateProfile(profile: Profile) {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            profileRepository.updateProfile(uid, profile)
                .onSuccess {
                    analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.PROFILE_UPDATED))
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isSaving = false,
                        saveSuccess = true,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = it.message,
                    )
                }
        }
    }

    fun addSkill(skill: Skill) {
        val uid = userId ?: return
        viewModelScope.launch {
            profileRepository.upsertSkill(uid, skill)
                .onSuccess { loadProfile() }
        }
    }

    fun deleteSkill(skillId: String) {
        viewModelScope.launch {
            profileRepository.deleteSkill(skillId)
                .onSuccess { loadProfile() }
        }
    }

    fun addExperience(experience: WorkExperience) {
        val uid = userId ?: return
        viewModelScope.launch {
            profileRepository.upsertWorkExperience(uid, experience)
                .onSuccess { loadProfile() }
        }
    }

    fun deleteExperience(experienceId: String) {
        viewModelScope.launch {
            profileRepository.deleteWorkExperience(experienceId)
                .onSuccess { loadProfile() }
        }
    }

    fun addEducation(education: Education) {
        val uid = userId ?: return
        viewModelScope.launch {
            profileRepository.upsertEducation(uid, education)
                .onSuccess { loadProfile() }
        }
    }

    fun deleteEducation(educationId: String) {
        viewModelScope.launch {
            profileRepository.deleteEducation(educationId)
                .onSuccess { loadProfile() }
        }
    }

    fun addCertification(certification: Certification) {
        val uid = userId ?: return
        viewModelScope.launch {
            profileRepository.upsertCertification(uid, certification)
                .onSuccess { loadProfile() }
        }
    }

    fun deleteCertification(certificationId: String) {
        viewModelScope.launch {
            profileRepository.deleteCertification(certificationId)
                .onSuccess { loadProfile() }
        }
    }

    fun addLanguage(language: Language) {
        val uid = userId ?: return
        viewModelScope.launch {
            profileRepository.upsertLanguage(uid, language)
                .onSuccess { loadProfile() }
        }
    }

    fun deleteLanguage(languageId: String) {
        viewModelScope.launch {
            profileRepository.deleteLanguage(languageId)
                .onSuccess { loadProfile() }
        }
    }

    fun markOnboarded() {
        val uid = userId ?: return
        viewModelScope.launch {
            profileRepository.markOnboarded(uid)
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun pushSnippetsToBubble(state: ProfileUiState) {
        val snippets = mutableListOf<SnippetItem>()
        val profile = state.profile ?: return

        if (profile.fullName.isNotBlank()) snippets.add(SnippetItem("personal", "Full Name", profile.fullName))
        profile.email?.let { snippets.add(SnippetItem("personal", "Email", it)) }
        profile.phone?.let { snippets.add(SnippetItem("personal", "Phone", it)) }
        profile.location?.let { snippets.add(SnippetItem("personal", "Location", it)) }
        profile.linkedinUrl?.let { snippets.add(SnippetItem("personal", "LinkedIn", it)) }
        profile.desiredRole?.let { snippets.add(SnippetItem("personal", "Desired Role", it)) }

        state.skills.forEach { skill ->
            val value = buildString {
                append(skill.name)
                skill.yearsExperience?.let { append(" - $it years") }
                skill.proficiency?.let { append(" ($it)") }
            }
            snippets.add(SnippetItem("skills", skill.name, value))
        }

        state.experiences.forEach { exp ->
            val dates = "${exp.startDate ?: "N/A"} - ${exp.endDate ?: "Present"}"
            snippets.add(SnippetItem("experience", "${exp.title} at ${exp.company}", "${exp.title} at ${exp.company} ($dates)"))
        }

        state.educationList.forEach { edu ->
            snippets.add(SnippetItem("education", edu.degree, "${edu.degree}${edu.fieldOfStudy?.let { " in $it" } ?: ""} from ${edu.institution}"))
        }

        state.languages.forEach { lang ->
            snippets.add(SnippetItem("languages", lang.name, "${lang.name}${lang.proficiency?.let { " ($it)" } ?: ""}"))
        }

        profile.portfolioUrl?.let { snippets.add(SnippetItem("personal", "Portfolio", it)) }

        if (profile.desiredSalaryMin != null) {
            val salary = "${profile.desiredSalaryMin} - ${profile.desiredSalaryMax ?: "N/A"} ${profile.salaryCurrency}"
            snippets.add(SnippetItem("personal", "Salary Expectation", salary))
        }

        state.certifications.forEach { cert ->
            val value = buildString {
                append(cert.name)
                if (cert.issuingOrg.isNotBlank()) append(" - ${cert.issuingOrg}")
                cert.issueDate?.let { append(" ($it)") }
            }
            snippets.add(SnippetItem("certifications", cert.name, value))
        }

        state.experiences.forEach { exp ->
            exp.description?.let { desc ->
                if (desc.isNotBlank()) {
                    snippets.add(SnippetItem("experience", "${exp.title} Description", desc))
                }
            }
            if (exp.achievements.isNotEmpty()) {
                snippets.add(SnippetItem("experience", "${exp.title} Achievements", exp.achievements.joinToString("\n• ", prefix = "• ")))
            }
        }

        profile.summary?.let { snippets.add(SnippetItem("summary", "Professional Summary", it)) }

        bubbleDataProvider.updateSnippets(snippets)
    }
}
