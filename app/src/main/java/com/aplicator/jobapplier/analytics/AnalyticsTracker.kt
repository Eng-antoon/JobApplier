package com.aplicator.jobapplier.analytics

interface AnalyticsTracker {
    fun identify(userId: String)
    fun reset()
    fun track(event: AnalyticsEvent)
    fun setUserProperties(properties: Map<String, Any?>)
    fun flush()
    fun optOut()
    fun optIn()
}
