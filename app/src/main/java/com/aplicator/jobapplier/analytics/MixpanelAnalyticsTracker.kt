package com.aplicator.jobapplier.analytics

import android.content.Context
import com.aplicator.jobapplier.BuildConfig
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mixpanel.android.sessionreplay.MPSessionReplay
import com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig
import com.mixpanel.android.sessionreplay.models.RemoteSettingsMode
import com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView
import org.json.JSONObject

class MixpanelAnalyticsTracker(
    context: Context,
) : AnalyticsTracker {

    private val mixpanel: MixpanelAPI = MixpanelAPI.getInstance(
        context,
        BuildConfig.MIXPANEL_TOKEN,
        false,
    ).apply {
        setUseIpAddressForGeolocation(false)
        setEnableLogging(BuildConfig.DEBUG)
    }

    init {
        MPSessionReplay.initialize(
            context,
            BuildConfig.MIXPANEL_TOKEN,
            mixpanel.distinctId,
            MPSessionReplayConfig(
                wifiOnly = false,
                flushInterval = 10_000L,
                autoStartRecording = true,
                recordingSessionsPercent = if (BuildConfig.DEBUG) 100.0 else 10.0,
                autoMaskedViews = setOf(
                    AutoMaskedView.Text,
                    AutoMaskedView.Image,
                    AutoMaskedView.Web,
                ),
                enableLogging = BuildConfig.DEBUG,
                remoteSettingsMode = RemoteSettingsMode.FALLBACK,
                debugOptions = null,
                serverUrl = "",
            ),
        )
    }

    override fun identify(userId: String) {
        if (userId.isBlank()) return
        mixpanel.identify(userId)
        mixpanel.people.identify(userId)
    }

    override fun reset() {
        mixpanel.reset()
    }

    override fun track(event: AnalyticsEvent) {
        mixpanel.track(event.name, JSONObject(event.sanitizedProperties()))
    }

    override fun setUserProperties(properties: Map<String, Any?>) {
        val sanitized = AnalyticsEvent("user_properties", properties).sanitizedProperties()
        if (sanitized.isNotEmpty()) {
            mixpanel.people.setMap(sanitized)
        }
    }

    override fun flush() {
        mixpanel.flush()
    }

    override fun optOut() {
        mixpanel.optOutTracking()
    }

    override fun optIn() {
        mixpanel.optInTracking()
    }
}
