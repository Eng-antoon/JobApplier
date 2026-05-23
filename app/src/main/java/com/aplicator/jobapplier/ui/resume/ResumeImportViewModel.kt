package com.aplicator.jobapplier.ui.resume

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicator.jobapplier.data.event.ProfileRefreshTrigger
import com.aplicator.jobapplier.data.remote.ai.ParsedCertification
import com.aplicator.jobapplier.data.remote.ai.ParsedEducation
import com.aplicator.jobapplier.data.remote.ai.ParsedExperience
import com.aplicator.jobapplier.data.remote.ai.ParsedLanguage
import com.aplicator.jobapplier.data.remote.ai.ParsedResumeResponse
import com.aplicator.jobapplier.data.remote.ai.ParsedSkill
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededException
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededResponse
import com.aplicator.jobapplier.data.repository.ProfileRepository
import com.aplicator.jobapplier.data.repository.QuotaRepository
import com.aplicator.jobapplier.data.repository.ResumeImportRepository
import com.aplicator.jobapplier.domain.model.Certification
import com.aplicator.jobapplier.domain.model.Education
import com.aplicator.jobapplier.domain.model.Language
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.domain.model.WorkExperience
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

sealed interface ResumeImportState {
    data object Idle : ResumeImportState
    data object Extracting : ResumeImportState
    data object Parsing : ResumeImportState
    data class Preview(val data: ResumePreviewData) : ResumeImportState
    data object Importing : ResumeImportState
    data object Done : ResumeImportState
    data class Error(val message: String) : ResumeImportState
}

data class ResumePreviewData(
    val response: ParsedResumeResponse,
    val selectedSkills: List<Boolean>,
    val selectedExperiences: List<Boolean>,
    val selectedEducation: List<Boolean>,
    val selectedCertifications: List<Boolean>,
    val selectedLanguages: List<Boolean>,
    val importPersonalInfo: Boolean = true,
) {
    val selectedCount: Int
        get() = (if (importPersonalInfo) 1 else 0) +
            selectedSkills.count { it } +
            selectedExperiences.count { it } +
            selectedEducation.count { it } +
            selectedCertifications.count { it } +
            selectedLanguages.count { it }
}

@HiltViewModel
class ResumeImportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val resumeImportRepository: ResumeImportRepository,
    private val profileRepository: ProfileRepository,
    private val quotaRepository: QuotaRepository,
    private val supabaseClient: SupabaseClient,
    private val profileRefreshTrigger: ProfileRefreshTrigger,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow<ResumeImportState>(ResumeImportState.Idle)
    val state: StateFlow<ResumeImportState> = _state.asStateFlow()

    private val _quotaExceeded = MutableStateFlow<QuotaExceededResponse?>(null)
    val quotaExceeded: StateFlow<QuotaExceededResponse?> = _quotaExceeded.asStateFlow()

    private val _quotaRequestPending = MutableStateFlow(false)
    val quotaRequestPending: StateFlow<Boolean> = _quotaRequestPending.asStateFlow()

    private val _quotaRequestSuccess = MutableStateFlow(false)
    val quotaRequestSuccess: StateFlow<Boolean> = _quotaRequestSuccess.asStateFlow()

    private val _quotaRequestSuccessType = MutableStateFlow("resume")
    val quotaRequestSuccessType: StateFlow<String> = _quotaRequestSuccessType.asStateFlow()

    private val _showResumeParseWarning = MutableStateFlow(false)
    val showResumeParseWarning: StateFlow<Boolean> = _showResumeParseWarning.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        savedStateHandle.get<String>(PARSED_RESPONSE_KEY)?.let { jsonString ->
            try {
                val response = json.decodeFromString<ParsedResumeResponse>(jsonString)
                _state.value = ResumeImportState.Preview(
                    ResumePreviewData(
                        response = response,
                        selectedSkills = List(response.skills.size) { true },
                        selectedExperiences = List(response.experiences.size) { true },
                        selectedEducation = List(response.education.size) { true },
                        selectedCertifications = List(response.certifications.size) { true },
                        selectedLanguages = List(response.languages.size) { true },
                    ),
                )
            } catch (_: Exception) {
                savedStateHandle.remove<String>(PARSED_RESPONSE_KEY)
            }
        }
    }

    fun selectFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.RESUME_IMPORT_STARTED))
            _state.value = ResumeImportState.Extracting
            Log.d("ResumeImport", "Starting text extraction for: $uri")

            val textResult = resumeImportRepository.extractText(uri, context)
            if (textResult.isFailure) {
                Log.e("ResumeImport", "Extraction failed", textResult.exceptionOrNull())
                _state.value = ResumeImportState.Error(
                    textResult.exceptionOrNull()?.message ?: "Failed to extract text",
                )
                return@launch
            }

            val text = textResult.getOrThrow()
            Log.d("ResumeImport", "Extracted ${text.length} chars")
            if (text.isBlank()) {
                _state.value = ResumeImportState.Error(
                    "No text found in file. Scanned PDFs without text layers are not supported.",
                )
                return@launch
            }

            _state.value = ResumeImportState.Parsing
            analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.RESUME_PARSE_STARTED))
            Log.d("ResumeImport", "Calling AI proxy for parse_resume...")

            val parseResult = resumeImportRepository.parseResume(text)
            if (parseResult.isFailure) {
                val error = parseResult.exceptionOrNull()
                Log.e("ResumeImport", "Parse failed", error)
                if (error is QuotaExceededException) {
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.QUOTA_LIMIT_SHOWN,
                            mapOf("quota_type" to error.quotaInfo.quotaType, "surface" to "resume_import"),
                        ),
                    )
                    _quotaExceeded.value = error.quotaInfo
                    checkPendingRequest("resume")
                    _state.value = ResumeImportState.Idle
                } else {
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.RESUME_PARSE_FAILED,
                            mapOf("reason" to (error?.javaClass?.simpleName ?: "unknown")),
                        ),
                    )
                    _state.value = ResumeImportState.Error(
                        error?.message ?: "Failed to parse resume",
                    )
                }
                return@launch
            }

            val response = parseResult.getOrThrow()
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.RESUME_PARSE_SUCCEEDED,
                    mapOf(
                        "skills_count" to response.skills.size,
                        "experiences_count" to response.experiences.size,
                    ),
                ),
            )
            Log.d("ResumeImport", "Parse success: ${response.skills.size} skills, ${response.experiences.size} experiences")
            if (response.quotaWarning == "last_resume_parse") {
                _showResumeParseWarning.value = true
            }
            savedStateHandle[PARSED_RESPONSE_KEY] = json.encodeToString(ParsedResumeResponse.serializer(), response)
            _state.value = ResumeImportState.Preview(
                ResumePreviewData(
                    response = response,
                    selectedSkills = List(response.skills.size) { true },
                    selectedExperiences = List(response.experiences.size) { true },
                    selectedEducation = List(response.education.size) { true },
                    selectedCertifications = List(response.certifications.size) { true },
                    selectedLanguages = List(response.languages.size) { true },
                ),
            )
        }
    }

    fun toggleSkill(index: Int) {
        val current = (_state.value as? ResumeImportState.Preview) ?: return
        val updated = current.data.selectedSkills.toMutableList()
        updated[index] = !updated[index]
        _state.value = ResumeImportState.Preview(current.data.copy(selectedSkills = updated))
    }

    fun toggleExperience(index: Int) {
        val current = (_state.value as? ResumeImportState.Preview) ?: return
        val updated = current.data.selectedExperiences.toMutableList()
        updated[index] = !updated[index]
        _state.value = ResumeImportState.Preview(current.data.copy(selectedExperiences = updated))
    }

    fun toggleEducation(index: Int) {
        val current = (_state.value as? ResumeImportState.Preview) ?: return
        val updated = current.data.selectedEducation.toMutableList()
        updated[index] = !updated[index]
        _state.value = ResumeImportState.Preview(current.data.copy(selectedEducation = updated))
    }

    fun toggleCertification(index: Int) {
        val current = (_state.value as? ResumeImportState.Preview) ?: return
        val updated = current.data.selectedCertifications.toMutableList()
        updated[index] = !updated[index]
        _state.value = ResumeImportState.Preview(current.data.copy(selectedCertifications = updated))
    }

    fun toggleLanguage(index: Int) {
        val current = (_state.value as? ResumeImportState.Preview) ?: return
        val updated = current.data.selectedLanguages.toMutableList()
        updated[index] = !updated[index]
        _state.value = ResumeImportState.Preview(current.data.copy(selectedLanguages = updated))
    }

    fun togglePersonalInfo() {
        val current = (_state.value as? ResumeImportState.Preview) ?: return
        _state.value = ResumeImportState.Preview(
            current.data.copy(importPersonalInfo = !current.data.importPersonalInfo),
        )
    }

    fun confirmImport() {
        val current = (_state.value as? ResumeImportState.Preview) ?: return
        val data = current.data
        val response = data.response

        viewModelScope.launch {
            _state.value = ResumeImportState.Importing
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: run {
                _state.value = ResumeImportState.Error("Not authenticated")
                return@launch
            }

            var failures = 0

            try {
                if (data.importPersonalInfo) {
                    val currentProfile = profileRepository.getProfile(userId).getOrNull()
                    if (currentProfile != null) {
                        val updated = currentProfile.copy(
                            fullName = response.fullName ?: currentProfile.fullName,
                            email = response.email ?: currentProfile.email,
                            phone = response.phone ?: currentProfile.phone,
                            location = response.location ?: currentProfile.location,
                            linkedinUrl = response.linkedinUrl ?: currentProfile.linkedinUrl,
                            summary = response.summary ?: currentProfile.summary,
                            desiredRole = response.desiredRole ?: currentProfile.desiredRole,
                        )
                        profileRepository.updateProfile(userId, updated).onFailure { failures++ }
                    }
                }

                coroutineScope {
                    val skillJobs = response.skills.mapIndexedNotNull { index, skill ->
                        if (!data.selectedSkills[index]) return@mapIndexedNotNull null
                        async {
                            profileRepository.upsertSkill(
                                userId,
                                Skill(
                                    id = UUID.randomUUID().toString(),
                                    name = skill.name,
                                    category = skill.category,
                                    proficiency = skill.proficiency,
                                    yearsExperience = skill.yearsExperience,
                                ),
                            )
                        }
                    }

                    val expJobs = response.experiences.mapIndexedNotNull { index, exp ->
                        if (!data.selectedExperiences[index]) return@mapIndexedNotNull null
                        async {
                            profileRepository.upsertWorkExperience(
                                userId,
                                WorkExperience(
                                    id = UUID.randomUUID().toString(),
                                    company = exp.company,
                                    title = exp.title,
                                    location = exp.location,
                                    startDate = normalizeDate(exp.startDate) ?: "1900-01-01",
                                    endDate = normalizeDate(exp.endDate),
                                    isCurrent = exp.isCurrent,
                                    description = exp.description,
                                    achievements = exp.achievements,
                                    technologiesUsed = exp.technologiesUsed,
                                    sortOrder = index,
                                ),
                            )
                        }
                    }

                    val eduJobs = response.education.mapIndexedNotNull { index, edu ->
                        if (!data.selectedEducation[index]) return@mapIndexedNotNull null
                        async {
                            profileRepository.upsertEducation(
                                userId,
                                Education(
                                    id = UUID.randomUUID().toString(),
                                    institution = edu.institution,
                                    degree = edu.degree,
                                    fieldOfStudy = edu.fieldOfStudy,
                                    startDate = normalizeDate(edu.startDate),
                                    endDate = normalizeDate(edu.endDate),
                                    gpa = edu.gpa,
                                    description = edu.description,
                                ),
                            )
                        }
                    }

                    val certJobs = response.certifications.mapIndexedNotNull { index, cert ->
                        if (!data.selectedCertifications[index]) return@mapIndexedNotNull null
                        async {
                            profileRepository.upsertCertification(
                                userId,
                                Certification(
                                    id = UUID.randomUUID().toString(),
                                    name = cert.name,
                                    issuingOrg = cert.issuingOrg ?: "",
                                    issueDate = normalizeDate(cert.issueDate),
                                    expiryDate = normalizeDate(cert.expiryDate),
                                    credentialUrl = cert.credentialUrl,
                                ),
                            )
                        }
                    }

                    val langJobs = response.languages.mapIndexedNotNull { index, lang ->
                        if (!data.selectedLanguages[index]) return@mapIndexedNotNull null
                        async {
                            profileRepository.upsertLanguage(
                                userId,
                                Language(
                                    id = UUID.randomUUID().toString(),
                                    name = lang.name,
                                    proficiency = lang.proficiency,
                                ),
                            )
                        }
                    }

                    val allResults = (skillJobs + expJobs + eduJobs + certJobs + langJobs).awaitAll()
                    failures += allResults.count { it.isFailure }
                }

                if (failures > 0) {
                    Log.w("ResumeImport", "$failures items failed to import")
                }
                profileRefreshTrigger.requestRefresh()
                savedStateHandle.remove<String>(PARSED_RESPONSE_KEY)
                analyticsTracker.track(
                    AnalyticsEvent(
                        AnalyticsEvents.RESUME_IMPORT_COMPLETED,
                        mapOf("items_imported" to data.selectedCount, "failures" to failures),
                    ),
                )
                _state.value = ResumeImportState.Done
            } catch (e: Exception) {
                Log.e("ResumeImport", "Import failed", e)
                _state.value = ResumeImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun retry() {
        savedStateHandle.remove<String>(PARSED_RESPONSE_KEY)
        _state.value = ResumeImportState.Idle
    }

    private fun normalizeDate(date: String?): String? {
        if (date.isNullOrBlank()) return null
        val trimmed = date.trim()
        return when {
            trimmed.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) -> trimmed
            trimmed.matches(Regex("^\\d{4}-\\d{2}$")) -> "$trimmed-01"
            trimmed.matches(Regex("^\\d{4}$")) -> "$trimmed-01-01"
            trimmed.matches(Regex("^\\d{2}/\\d{4}$")) -> {
                val parts = trimmed.split("/")
                "${parts[1]}-${parts[0]}-01"
            }
            trimmed.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$")) -> {
                val parts = trimmed.split("/")
                "${parts[2]}-${parts[0]}-${parts[1]}"
            }
            else -> null
        }
    }

    fun dismissResumeParseWarning() {
        _showResumeParseWarning.value = false
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

    companion object {
        private const val PARSED_RESPONSE_KEY = "parsed_resume_response"
    }
}
