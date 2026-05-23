package com.aplicator.jobapplier.analytics

class FakeAnalyticsTracker : AnalyticsTracker {
    val trackedEvents = mutableListOf<AnalyticsEvent>()
    val identifiedUsers = mutableListOf<String>()
    val userProperties = mutableListOf<Map<String, Any?>>()
    var resetCount = 0
    var flushCount = 0
    var optedOut = false

    override fun identify(userId: String) { identifiedUsers.add(userId) }
    override fun reset() { resetCount++ }
    override fun track(event: AnalyticsEvent) { trackedEvents.add(event) }
    override fun setUserProperties(properties: Map<String, Any?>) { userProperties.add(properties) }
    override fun flush() { flushCount++ }
    override fun optOut() { optedOut = true }
    override fun optIn() { optedOut = false }

    fun findEvent(name: String): AnalyticsEvent? = trackedEvents.find { it.name == name }
    fun hasEvent(name: String): Boolean = trackedEvents.any { it.name == name }
    fun clear() {
        trackedEvents.clear()
        identifiedUsers.clear()
        userProperties.clear()
        resetCount = 0
        flushCount = 0
        optedOut = false
    }
}
