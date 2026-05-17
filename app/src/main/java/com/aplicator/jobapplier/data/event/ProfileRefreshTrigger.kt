package com.aplicator.jobapplier.data.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRefreshTrigger @Inject constructor() {
    private val _refreshEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvent: SharedFlow<Unit> = _refreshEvent.asSharedFlow()

    fun requestRefresh() {
        _refreshEvent.tryEmit(Unit)
    }
}
