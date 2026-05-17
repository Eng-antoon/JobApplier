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

    fun updateSnippets(items: List<SnippetItem>) {
        _snippets.value = items
    }

    fun updateUserInfo(name: String, avatarUrl: String? = null) {
        _userName.value = name
        _avatarUrl.value = avatarUrl
    }
}
