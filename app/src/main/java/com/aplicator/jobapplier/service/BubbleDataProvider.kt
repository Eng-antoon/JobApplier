package com.aplicator.jobapplier.service

import com.aplicator.jobapplier.ui.snippets.SnippetItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BubbleDataProvider @Inject constructor() {
    private val _snippets = MutableStateFlow<List<SnippetItem>>(emptyList())
    val snippets: StateFlow<List<SnippetItem>> = _snippets.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _recentJobs = MutableStateFlow<List<BubbleJobItem>>(emptyList())
    val recentJobs: StateFlow<List<BubbleJobItem>> = _recentJobs.asStateFlow()

    private val _generatedContent = MutableStateFlow<Map<String, List<BubbleContentItem>>>(emptyMap())
    val generatedContent: StateFlow<Map<String, List<BubbleContentItem>>> = _generatedContent.asStateFlow()

    fun updateSnippets(items: List<SnippetItem>) {
        _snippets.value = items
    }

    fun updateUserInfo(name: String, avatarUrl: String? = null) {
        _userName.value = name
        _avatarUrl.value = avatarUrl
    }

    fun updateRecentJobs(jobs: List<BubbleJobItem>) {
        _recentJobs.value = jobs
    }

    fun updateGeneratedContent(jobId: String, content: List<BubbleContentItem>) {
        _generatedContent.value = _generatedContent.value.toMutableMap().apply {
            put(jobId, content)
        }
    }
}
