package com.aplicator.jobapplier.ui.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.data.event.SharedJobTextHolder
import com.aplicator.jobapplier.ui.webextract.WebExtractResult
import com.aplicator.jobapplier.data.remote.ai.AnalyzeJdResponse
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededException
import com.aplicator.jobapplier.data.remote.ai.QuotaExceededResponse
import com.aplicator.jobapplier.data.remote.ai.UserQuotaRow
import com.aplicator.jobapplier.data.repository.AiRepository
import com.aplicator.jobapplier.data.repository.AuthRepository
import com.aplicator.jobapplier.data.repository.JobRepository
import com.aplicator.jobapplier.data.repository.requireSuccessfulPersistence
import com.aplicator.jobapplier.data.repository.ProfileRepository
import com.aplicator.jobapplier.data.repository.QuotaRepository
import com.aplicator.jobapplier.domain.model.GeneratedContent
import com.aplicator.jobapplier.domain.model.JobDescription
import com.aplicator.jobapplier.domain.model.MatchResult
import com.aplicator.jobapplier.domain.model.UserProfileSnapshot
import com.aplicator.jobapplier.service.BubbleContentItem
import com.aplicator.jobapplier.service.BubbleDataProvider
import com.aplicator.jobapplier.service.toBubbleJobItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobListState(
    val isLoading: Boolean = true,
    val jobs: List<JobDescription> = emptyList(),
    val error: String? = null,
)

data class JobDetailState(
    val isLoading: Boolean = true,
    val job: JobDescription? = null,
    val matchResult: MatchResult? = null,
    val generatedContent: List<GeneratedContent> = emptyList(),
    val error: String? = null,
)

data class AddJobState(
    val isAnalyzing: Boolean = false,
    val analyzedJobId: String? = null,
    val error: String? = null,
)

data class GenerateState(
    val isGenerating: Boolean = false,
    val generatedText: String? = null,
    val contentType: String? = null,
    val error: String? = null,
)

sealed interface FetchUrlState {
    data object Idle : FetchUrlState
    data object Loading : FetchUrlState
    data class Success(val title: String?, val company: String?, val description: String?) : FetchUrlState
    data class Failed(val reason: String? = null) : FetchUrlState
}

@HiltViewModel
class JobViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val jobRepository: JobRepository,
    private val aiRepository: AiRepository,
    private val quotaRepository: QuotaRepository,
    private val sharedJobTextHolder: SharedJobTextHolder,
    private val bubbleDataProvider: BubbleDataProvider,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _jobListState = MutableStateFlow(JobListState())
    val jobListState: StateFlow<JobListState> = _jobListState.asStateFlow()

    private val _jobDetailState = MutableStateFlow(JobDetailState())
    val jobDetailState: StateFlow<JobDetailState> = _jobDetailState.asStateFlow()

    private val _addJobState = MutableStateFlow(AddJobState())
    val addJobState: StateFlow<AddJobState> = _addJobState.asStateFlow()

    private val _generateState = MutableStateFlow(GenerateState())
    val generateState: StateFlow<GenerateState> = _generateState.asStateFlow()

    private val _fetchUrlState = MutableStateFlow<FetchUrlState>(FetchUrlState.Idle)
    val fetchUrlState: StateFlow<FetchUrlState> = _fetchUrlState.asStateFlow()

    private val _quotaExceeded = MutableStateFlow<QuotaExceededResponse?>(null)
    val quotaExceeded: StateFlow<QuotaExceededResponse?> = _quotaExceeded.asStateFlow()

    private val _quotaRequestPending = MutableStateFlow(false)
    val quotaRequestPending: StateFlow<Boolean> = _quotaRequestPending.asStateFlow()

    private val _quotaRequestSuccess = MutableStateFlow(false)
    val quotaRequestSuccess: StateFlow<Boolean> = _quotaRequestSuccess.asStateFlow()

    private val _quotaRequestSuccessType = MutableStateFlow("weekly")
    val quotaRequestSuccessType: StateFlow<String> = _quotaRequestSuccessType.asStateFlow()

    private val _quotaStatus = MutableStateFlow<UserQuotaRow?>(null)
    val quotaStatus: StateFlow<UserQuotaRow?> = _quotaStatus.asStateFlow()

    private val _linkedInUrlToExtract = MutableStateFlow<String?>(null)
    val linkedInUrlToExtract: StateFlow<String?> = _linkedInUrlToExtract.asStateFlow()

    private val userId: String?
        get() = authRepository.getCurrentUserId()

    val sharedJobText: StateFlow<String?> = sharedJobTextHolder.sharedText

    private var cachedProfileSnapshot: UserProfileSnapshot? = null

    init {
        loadJobs()
        loadQuotaStatus()
    }

    fun consumeSharedText(): String? = sharedJobTextHolder.consume()

    fun loadJobs() {
        val uid = userId ?: return
        viewModelScope.launch {
            _jobListState.value = JobListState(isLoading = true)
            jobRepository.getJobs(uid)
                .onSuccess { jobs ->
                    _jobListState.value = JobListState(isLoading = false, jobs = jobs)
                    pushJobsToBubble(jobs)
                    preloadBubbleGeneratedContent(jobs)
                }
                .onFailure { _jobListState.value = JobListState(isLoading = false, error = it.message) }
        }
    }

    fun loadJobDetail(jobId: String) {
        viewModelScope.launch {
            _jobDetailState.value = JobDetailState(isLoading = true)
            val jobResult = jobRepository.getJob(jobId)
            val contentResult = jobRepository.getGeneratedContent(jobId)

            jobResult
                .onSuccess { job ->
                    val contentList = contentResult.getOrDefault(emptyList())
                    _jobDetailState.value = JobDetailState(
                        isLoading = false,
                        job = job,
                        matchResult = job.matchResult,
                        generatedContent = contentList,
                    )
                    pushContentToBubble(jobId, contentList)
                }
                .onFailure {
                    _jobDetailState.value = JobDetailState(isLoading = false, error = it.message)
                }
        }
    }

    fun analyzeJob(companyName: String, roleTitle: String, rawText: String, sourceUrl: String?) {
        val uid = userId ?: return
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.JOB_ANALYSIS_STARTED,
                    mapOf(
                        "source_type" to sourceType(sourceUrl, rawText),
                        "description_length_bucket" to lengthBucket(rawText.length),
                    ),
                ),
            )
            _addJobState.value = AddJobState(isAnalyzing = true)

            val job = JobDescription(
                companyName = companyName,
                roleTitle = roleTitle,
                rawText = rawText,
                sourceUrl = sourceUrl,
            )

            val insertResult = jobRepository.insertJob(uid, job)
            if (insertResult.isFailure) {
                _addJobState.value = AddJobState(error = insertResult.exceptionOrNull()?.message)
                return@launch
            }
            val jobId = insertResult.getOrThrow()

            val profile = getProfileSnapshot()
            if (profile == null) {
                _addJobState.value = AddJobState(error = "Failed to load profile")
                return@launch
            }

            val analyzeResult = aiRepository.analyzeJobDescription(rawText, profile.toPromptText())
            analyzeResult
                .onSuccess { response ->
                    val matchDetailsJson = Json.encodeToString(AnalyzeJdResponse.serializer(), response)
                    val reqList = response.requirements
                    val requirementsJson = Json.encodeToString(kotlinx.serialization.json.JsonArray(reqList.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                    jobRepository.updateJobAnalysis(jobId, response.matchScore, matchDetailsJson, requirementsJson)
                    _addJobState.value = AddJobState(analyzedJobId = jobId)
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.JOB_ANALYSIS_SUCCEEDED,
                            mapOf("match_score_bucket" to scoreBucket(response.matchScore)),
                        ),
                    )
                    loadJobs()
                    loadQuotaStatus()
                }
                .onFailure { error ->
                    if (error is QuotaExceededException) {
                        _quotaExceeded.value = error.quotaInfo
                        checkPendingRequest(error.quotaInfo.quotaType)
                        _addJobState.value = AddJobState()
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.JOB_ANALYSIS_FAILED,
                                mapOf(
                                    "reason" to "quota_exceeded",
                                    "quota_type" to error.quotaInfo.quotaType,
                                ),
                            ),
                        )
                    } else {
                        _addJobState.value = AddJobState(error = error.message)
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.JOB_ANALYSIS_FAILED,
                                mapOf("reason" to error.javaClass.simpleName),
                            ),
                        )
                    }
                }
        }
    }

    fun generateContent(jobId: String, contentType: String, tone: String, question: String? = null) {
        val uid = userId ?: return
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.AI_CONTENT_GENERATION_STARTED,
                    mapOf(
                        "surface" to "app",
                        "content_type" to contentType,
                        "tone" to tone,
                    ),
                ),
            )
            _generateState.value = GenerateState(isGenerating = true)

            val job = jobRepository.getJob(jobId).getOrNull()
            if (job == null) {
                _generateState.value = GenerateState(error = "Job not found")
                return@launch
            }

            val profile = getProfileSnapshot()
            if (profile == null) {
                _generateState.value = GenerateState(error = "Failed to load profile")
                return@launch
            }

            val result = when (contentType) {
                "cover_letter" -> aiRepository.generateCoverLetter(job.rawText, profile.toPromptText(), tone, null)
                "cover_email" -> aiRepository.generateCoverEmail(job.rawText, profile.toPromptText(), tone)
                "headline", "custom_question", "why_work_here", "strengths", "weaknesses", "motivation" ->
                    aiRepository.answerQuestion(question ?: contentType, contentType, job.rawText, profile.toPromptText(), tone)
                else -> aiRepository.generateCoverLetter(job.rawText, profile.toPromptText(), tone, null)
            }

            result.requireSuccessfulPersistence { response ->
                jobRepository.insertGeneratedContent(
                    uid,
                    GeneratedContent(
                        jobId = jobId,
                        contentType = contentType,
                        content = response.content,
                        tone = tone,
                    ),
                )
            }
                .onSuccess { response ->
                    _generateState.value = GenerateState(generatedText = response.content, contentType = contentType)
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.AI_CONTENT_GENERATION_SUCCEEDED,
                            mapOf(
                                "surface" to "app",
                                "content_type" to contentType,
                                "tone" to tone,
                            ),
                        ),
                    )
                    loadQuotaStatus()
                    loadJobDetail(jobId)
                }
                .onFailure { error ->
                    if (error is QuotaExceededException) {
                        _quotaExceeded.value = error.quotaInfo
                        checkPendingRequest(error.quotaInfo.quotaType)
                        _generateState.value = GenerateState()
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.AI_CONTENT_GENERATION_FAILED,
                                mapOf(
                                    "surface" to "app",
                                    "content_type" to contentType,
                                    "reason" to "quota_exceeded",
                                    "quota_type" to error.quotaInfo.quotaType,
                                ),
                            ),
                        )
                    } else {
                        _generateState.value = GenerateState(error = error.message)
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.AI_CONTENT_GENERATION_FAILED,
                                mapOf(
                                    "surface" to "app",
                                    "content_type" to contentType,
                                    "reason" to error.javaClass.simpleName,
                                ),
                            ),
                        )
                    }
                }
        }
    }

    fun updateJobStatus(jobId: String, status: String) {
        viewModelScope.launch {
            val oldStatus = _jobDetailState.value.job?.status
            jobRepository.updateJobStatus(jobId, status)
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.JOB_STATUS_CHANGED,
                    mapOf(
                        "from_status" to (oldStatus ?: "unknown"),
                        "to_status" to status,
                    ),
                ),
            )
            loadJobDetail(jobId)
            loadJobs()
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            jobRepository.deleteJob(jobId)
            loadJobs()
        }
    }

    fun clearAddJobState() {
        _addJobState.value = AddJobState()
    }

    fun clearGenerateState() {
        _generateState.value = GenerateState()
    }

    fun fetchJobFromUrl(url: String) {
        val type = urlSourceType(url)
        analyticsTracker.track(
            AnalyticsEvent(
                AnalyticsEvents.JOB_SOURCE_FETCH_STARTED,
                mapOf("source_type" to type),
            ),
        )
        if (url.lowercase().contains("linkedin.com/job")) {
            _linkedInUrlToExtract.value = url
            return
        }
        viewModelScope.launch {
            _fetchUrlState.value = FetchUrlState.Loading
            val result = aiRepository.fetchJobFromUrl(url)
            result
                .onSuccess { response ->
                    if (response.success) {
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.JOB_SOURCE_FETCH_SUCCEEDED,
                                mapOf(
                                    "source_type" to type,
                                    "has_company" to !response.company.isNullOrBlank(),
                                    "has_title" to !response.title.isNullOrBlank(),
                                    "has_description" to !response.description.isNullOrBlank(),
                                ),
                            ),
                        )
                        _fetchUrlState.value = FetchUrlState.Success(
                            title = response.title,
                            company = response.company,
                            description = response.description,
                        )
                    } else {
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.JOB_SOURCE_FETCH_FAILED,
                                mapOf(
                                    "source_type" to type,
                                    "reason" to (response.reason ?: "unknown"),
                                ),
                            ),
                        )
                        _fetchUrlState.value = FetchUrlState.Failed(reason = response.reason)
                    }
                }
                .onFailure { error ->
                    if (error is QuotaExceededException) {
                        _quotaExceeded.value = error.quotaInfo
                        checkPendingRequest(error.quotaInfo.quotaType)
                        _fetchUrlState.value = FetchUrlState.Idle
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.JOB_SOURCE_FETCH_FAILED,
                                mapOf(
                                    "source_type" to type,
                                    "reason" to "quota_exceeded",
                                    "quota_type" to error.quotaInfo.quotaType,
                                ),
                            ),
                        )
                    } else {
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.JOB_SOURCE_FETCH_FAILED,
                                mapOf(
                                    "source_type" to type,
                                    "reason" to error.javaClass.simpleName,
                                ),
                            ),
                        )
                        _fetchUrlState.value = FetchUrlState.Failed(reason = error.message)
                    }
                }
        }
    }

    fun onWebExtractResult(result: WebExtractResult?) {
        _linkedInUrlToExtract.value = null
        if (result != null && (result.title != null || result.description != null)) {
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.JOB_SOURCE_FETCH_SUCCEEDED,
                    mapOf(
                        "source_type" to "linkedin",
                        "has_company" to !result.company.isNullOrBlank(),
                        "has_title" to !result.title.isNullOrBlank(),
                        "has_description" to !result.description.isNullOrBlank(),
                    ),
                ),
            )
            _fetchUrlState.value = FetchUrlState.Success(
                title = result.title,
                company = result.company,
                description = result.description,
            )
        } else {
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.JOB_SOURCE_FETCH_FAILED,
                    mapOf("source_type" to "linkedin", "reason" to "extraction_failed"),
                ),
            )
            _fetchUrlState.value = FetchUrlState.Failed(reason = "extraction_failed")
        }
    }

    fun clearLinkedInExtractRequest() {
        _linkedInUrlToExtract.value = null
    }

    fun clearFetchState() {
        _fetchUrlState.value = FetchUrlState.Idle
    }

    private suspend fun getProfileSnapshot(): UserProfileSnapshot? {
        cachedProfileSnapshot?.let { return it }
        val uid = userId ?: return null
        val profile = profileRepository.getProfile(uid).getOrNull() ?: return null
        val skills = profileRepository.getSkills(uid).getOrDefault(emptyList())
        val experiences = profileRepository.getWorkExperiences(uid).getOrDefault(emptyList())
        val education = profileRepository.getEducation(uid).getOrDefault(emptyList())
        val certs = profileRepository.getCertifications(uid).getOrDefault(emptyList())
        val langs = profileRepository.getLanguages(uid).getOrDefault(emptyList())

        return UserProfileSnapshot(
            fullName = profile.fullName,
            email = profile.email,
            desiredRole = profile.desiredRole,
            summary = profile.summary,
            skills = skills,
            experiences = experiences,
            educationList = education,
            certifications = certs,
            languages = langs,
        ).also { cachedProfileSnapshot = it }
    }

    private fun pushJobsToBubble(jobs: List<JobDescription>) {
        val bubbleJobs = jobs.take(5).map { it.toBubbleJobItem() }
        bubbleDataProvider.updateRecentJobs(bubbleJobs)
    }

    private suspend fun preloadBubbleGeneratedContent(jobs: List<JobDescription>) {
        jobs.take(5)
            .filter { it.id.isNotBlank() }
            .forEach { job ->
                val content = jobRepository.getGeneratedContent(job.id).getOrDefault(emptyList())
                pushContentToBubble(job.id, content)
            }
    }

    private fun pushContentToBubble(jobId: String, content: List<GeneratedContent>) {
        val bubbleContent = content.map { item ->
            BubbleContentItem(
                contentType = item.contentType,
                content = item.content,
                tone = item.tone,
                createdAt = null,
            )
        }
        bubbleDataProvider.updateGeneratedContent(jobId, bubbleContent)
    }

    fun loadQuotaStatus() {
        val uid = userId ?: return
        viewModelScope.launch {
            quotaRepository.getQuotaStatus(uid)
                .onSuccess { _quotaStatus.value = it }
        }
    }

    fun dismissQuotaDialog() {
        _quotaExceeded.value = null
    }

    fun dismissQuotaRequestSuccess() {
        _quotaRequestSuccess.value = false
        loadQuotaStatus()
    }

    fun requestExtraQuota() {
        val quota = _quotaExceeded.value ?: return
        val uid = userId ?: return
        analyticsTracker.track(
            AnalyticsEvent(
                AnalyticsEvents.QUOTA_EXTRA_REQUESTED,
                mapOf("quota_type" to quota.quotaType, "surface" to "app"),
            ),
        )
        viewModelScope.launch {
            quotaRepository.requestExtraQuota(uid, quota.quotaType)
                .onSuccess {
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.QUOTA_EXTRA_REQUEST_SUCCEEDED,
                            mapOf("quota_type" to quota.quotaType, "surface" to "app"),
                        ),
                    )
                    _quotaRequestSuccessType.value = quota.quotaType
                    _quotaRequestSuccess.value = true
                    _quotaExceeded.value = null
                    _quotaRequestPending.value = true
                    loadQuotaStatus()
                }
                .onFailure {
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.QUOTA_EXTRA_REQUEST_FAILED,
                            mapOf("quota_type" to quota.quotaType, "surface" to "app"),
                        ),
                    )
                }
        }
    }

    private fun checkPendingRequest(type: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            _quotaRequestPending.value = quotaRepository.hasPendingRequest(uid, type).getOrDefault(false)
        }
    }

    private fun sourceType(sourceUrl: String?, rawText: String): String {
        return when {
            !sourceUrl.isNullOrBlank() -> urlSourceType(sourceUrl)
            rawText.isNotBlank() -> "manual_paste"
            else -> "unknown"
        }
    }

    private fun urlSourceType(url: String): String {
        return if (url.lowercase().contains("linkedin.com")) "linkedin" else "manual_url"
    }

    private fun lengthBucket(length: Int): String {
        return when {
            length < 500 -> "short"
            length < 2_000 -> "medium"
            else -> "long"
        }
    }

    private fun scoreBucket(score: Int): String {
        return when {
            score < 40 -> "low"
            score < 75 -> "medium"
            else -> "high"
        }
    }
}
