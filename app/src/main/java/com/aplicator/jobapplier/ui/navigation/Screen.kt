package com.aplicator.jobapplier.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Login : Screen
    @Serializable data object SignUp : Screen
    @Serializable data object Onboarding : Screen
    @Serializable data object Dashboard : Screen
    @Serializable data object Profile : Screen
    @Serializable data object Snippets : Screen
    @Serializable data class JobDetail(val jobId: String) : Screen
    @Serializable data object AddJob : Screen
    @Serializable data object ResumeImport : Screen
    @Serializable data object AiSuggestions : Screen
}
