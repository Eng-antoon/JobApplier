package com.aplicator.jobapplier.analytics

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun identify(userId: String) = Unit
    override fun reset() = Unit
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserProperties(properties: Map<String, Any?>) = Unit
    override fun flush() = Unit
    override fun optOut() = Unit
    override fun optIn() = Unit
}
