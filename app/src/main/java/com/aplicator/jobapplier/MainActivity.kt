package com.aplicator.jobapplier

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.data.event.SharedJobTextHolder
import com.aplicator.jobapplier.ui.auth.AuthViewModel
import com.aplicator.jobapplier.ui.components.AnimatedSplashContent
import com.aplicator.jobapplier.ui.navigation.AuthNavGraph
import com.aplicator.jobapplier.ui.navigation.MainNavGraph
import com.aplicator.jobapplier.ui.onboarding.OnboardingChoiceScreen
import com.aplicator.jobapplier.ui.onboarding.OnboardingScreen
import com.aplicator.jobapplier.ui.profile.ProfileViewModel
import com.aplicator.jobapplier.ui.resume.ResumeImportScreen
import com.aplicator.jobapplier.ui.theme.JobApplierTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.status.SessionStatus
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sharedJobTextHolder: SharedJobTextHolder

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.APP_OPENED))
        enableEdgeToEdge()
        handleShareIntent(intent)

        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        setContent {
            JobApplierTheme {
                AppRoot(
                    analyticsTracker = analyticsTracker,
                    onReady = { isReady = true },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                sharedJobTextHolder.setSharedText(text)
                analyticsTracker.track(
                    AnalyticsEvent(
                        AnalyticsEvents.JOB_ADD_STARTED,
                        mapOf("entry_point" to "android_share_sheet"),
                    ),
                )
            }
        }
    }
}

private enum class AppState {
    Loading, Auth, Onboarding, Main
}

@Composable
private fun AppRoot(
    analyticsTracker: AnalyticsTracker,
    onReady: () -> Unit,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    val isOnboarded by authViewModel.isOnboarded.collectAsState()

    val rawAppState = when (sessionStatus) {
        is SessionStatus.Initializing -> AppState.Loading
        is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> AppState.Auth
        is SessionStatus.Authenticated -> when (isOnboarded) {
            null -> AppState.Loading
            false -> AppState.Onboarding
            true -> AppState.Main
        }
    }

    // Prevent Main→Loading→Main oscillation on resume (session re-init / onboarding re-check)
    // which would destroy and recreate MainNavGraph, losing all navigation state
    var stableState by rememberSaveable { mutableStateOf<AppState?>(null) }
    val appState = if (rawAppState == AppState.Loading && (stableState == AppState.Main || stableState == AppState.Onboarding)) {
        stableState!!
    } else {
        rawAppState
    }
    if (appState != AppState.Loading) {
        stableState = appState
    }

    var onboardingRoute by rememberSaveable { mutableStateOf("choice") }
    var previousAppState by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(appState) {
        if (appState != AppState.Loading) onReady()
        if (appState != AppState.Loading) {
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.SCREEN_VIEWED,
                    mapOf("screen" to appState.name.lowercase()),
                ),
            )
        }
        if ((appState == AppState.Main || appState == AppState.Auth)
            && previousAppState == "Onboarding"
        ) {
            onboardingRoute = "choice"
        }
        if (appState != AppState.Loading) {
            previousAppState = appState.name
        }
    }

    LaunchedEffect(sessionStatus) {
        // Drop any persisted UI state from a previous user so a prior user's
        // Main/Onboarding cannot mask the new user's Loading/Main window.
        if (sessionStatus is SessionStatus.NotAuthenticated) {
            stableState = null
        }
        if (sessionStatus is SessionStatus.Authenticated) {
            authViewModel.checkOnboardingStatus()
        }
    }

    AnimatedContent(
        targetState = appState,
        transitionSpec = {
            (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 16 })
                .togetherWith(fadeOut(tween(200)))
        },
        label = "AppRootTransition",
    ) { state ->
        when (state) {
            AppState.Loading -> {
                AnimatedSplashContent(modifier = Modifier.fillMaxSize())
            }
            AppState.Auth -> {
                AuthNavGraph(authViewModel = authViewModel)
            }
            AppState.Onboarding -> {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                when (onboardingRoute) {
                    "choice" -> OnboardingChoiceScreen(
                        onResumeImport = {
                            analyticsTracker.track(
                                AnalyticsEvent(
                                    AnalyticsEvents.ONBOARDING_CHOICE_SELECTED,
                                    mapOf("choice" to "resume_import"),
                                ),
                            )
                            onboardingRoute = "resume_import"
                        },
                        onManualFill = {
                            analyticsTracker.track(
                                AnalyticsEvent(
                                    AnalyticsEvents.ONBOARDING_CHOICE_SELECTED,
                                    mapOf("choice" to "manual"),
                                ),
                            )
                            onboardingRoute = "manual"
                        },
                    )
                    "resume_import" -> ResumeImportScreen(
                        onBack = { onboardingRoute = "manual" },
                    )
                    "manual" -> OnboardingScreen(
                        viewModel = profileViewModel,
                        onComplete = {
                            analyticsTracker.track(
                                AnalyticsEvent(
                                    AnalyticsEvents.ONBOARDING_COMPLETED,
                                    mapOf("method" to "manual"),
                                ),
                            )
                            authViewModel.checkOnboardingStatus()
                        },
                    )
                }
            }
            AppState.Main -> {
                MainNavGraph(analyticsTracker = analyticsTracker)
            }
        }
    }
}
