package com.aplicator.jobapplier.data.event

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedJobTextHolder @Inject constructor() {
    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText: StateFlow<String?> = _sharedText.asStateFlow()

    fun setSharedText(text: String) {
        _sharedText.value = text
    }

    fun consume(): String? {
        val text = _sharedText.value
        _sharedText.value = null
        return text
    }
}
