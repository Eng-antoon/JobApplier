package com.aplicator.jobapplier.ui.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicator.jobapplier.data.event.SharedJobTextHolder
import com.aplicator.jobapplier.data.remote.ai.AnalyzeJdResponse
import com.aplicator.jobapplier.data.repository.AiRepository
import com.aplicator.jobapplier.data.repository.AuthRepository
import com.aplicator.jobapplier.data.repository.JobRepository
import com.aplicator.jobapplier.data.repository.ProfileRepository
import com.aplicator.jobapplier.domain.model.GeneratedContent
import com.aplicator.jobapplier.domain.model.JobDescription
import com.aplicator.jobapplier.domain.model.MatchResult
import com.aplicator.jobapplier.domain.model.UserProfileSnapshot
import com.aplicator.jobapplier.service.BubbleContentItem
import com.aplicator.jobapplier.service.BubbleDataProvider
import com.aplicator.jobapplier.service.BubbleJobItem
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
    val error: String? = null,
)

sealed interface FetchUrlState {
    data object Idle : FetchUrlState
    data object Loading : FetchUrlState
    data class Success(val title: String?, val company: String?, val description: String?) : FetchUrlState
    data object Failed : FetchUrlState
}

@HiltViewModel
class JobViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val jobRepository: JobRepository,
    private val aiRepository: AiRepository,
    private val sharedJobTextHolder: SharedJobTextHolder,
    private val bubbleDataProvider: BubbleDataProvider,
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

    private val userId: String?
        get() = authRepository.getCurrentUserId()

    val sharedJobText: StateFlow<String?> = sharedJobTextHolder.sharedText

    private var cachedProfileSnapshot: UserProfileSnapshot? = null

    init {
        loadJobs()
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
                    loadJobs()
                }
                .onFailure {
                    _addJobState.value = AddJobState(error = it.message)
                }
        }
    }

    fun generateContent(jobId: String, contentType: String, tone: String, question: String? = null) {
        val uid = userId ?: return
        viewModelScope.launch {
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
                    aiRepository.answerQuestion(question ?: contentType, contentType, job.rawText, profile.toPromptText())
                else -> aiRepository.generateCoverLetter(job.rawText, profile.toPromptText(), tone, null)
            }

            result
                .onSuccess { response ->
                    _generateState.value = GenerateState(generatedText = response.content)
                    jobRepository.insertGeneratedContent(
                        uid,
                        GeneratedContent(
                            jobId = jobId,
                            contentType = contentType,
                            content = response.content,
                            tone = tone,
                        )
                    )
                    loadJobDetail(jobId)
                }
                .onFailure {
                    _generateState.value = GenerateState(error = it.message)
                }
        }
    }

    fun updateJobStatus(jobId: String, status: String) {
        viewModelScope.launch {
            jobRepository.updateJobStatus(jobId, status)
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
        viewModelScope.launch {
            _fetchUrlState.value = FetchUrlState.Loading
            val result = aiRepository.fetchJobFromUrl(url)
            result
                .onSuccess { response ->
                    if (response.success) {
                        _fetchUrlState.value = FetchUrlState.Success(
                            title = response.title,
                            company = response.company,
                            description = response.description,
                        )
                    } else {
                        _fetchUrlState.value = FetchUrlState.Failed
                    }
                }
                .onFailure {
                    _fetchUrlState.value = FetchUrlState.Failed
                }
        }
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
        val bubbleJobs = jobs.take(5).map { job ->
            BubbleJobItem(
                jobId = job.id,
                companyName = job.companyName,
                roleTitle = job.roleTitle,
                matchScore = job.matchScore,
                status = job.status,
            )
        }
        bubbleDataProvider.updateRecentJobs(bubbleJobs)
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
}
