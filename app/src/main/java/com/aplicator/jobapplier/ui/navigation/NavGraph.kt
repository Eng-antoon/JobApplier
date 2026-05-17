package com.aplicator.jobapplier.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aplicator.jobapplier.ui.auth.AuthViewModel
import com.aplicator.jobapplier.ui.auth.LoginScreen
import com.aplicator.jobapplier.ui.auth.SignUpScreen
import com.aplicator.jobapplier.ui.dashboard.DashboardScreen
import com.aplicator.jobapplier.ui.job.AddJobScreen
import com.aplicator.jobapplier.ui.job.JobDetailScreen
import com.aplicator.jobapplier.ui.job.JobViewModel
import com.aplicator.jobapplier.ui.profile.ProfileScreen
import com.aplicator.jobapplier.ui.profile.ProfileViewModel
import com.aplicator.jobapplier.ui.resume.ResumeImportScreen
import com.aplicator.jobapplier.ui.snippets.SnippetsScreen
import com.aplicator.jobapplier.ui.suggestions.AiSuggestionsScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home, Screen.Dashboard),
    BottomNavItem("Profile", Icons.Default.Person, Screen.Profile),
    BottomNavItem("Snippets", Icons.Default.ContentCopy, Screen.Snippets),
    BottomNavItem("AI", Icons.Default.AutoAwesome, Screen.AiSuggestions),
)

private const val NAV_ANIM_DURATION = 300

@Composable
fun AuthNavGraph(
    authViewModel: AuthViewModel,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login,
        enterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { it } },
        exitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { -it } },
        popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } },
    ) {
        composable<Screen.Login> {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Screen.SignUp) },
            )
        }
        composable<Screen.SignUp> {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
fun MainNavGraph(
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val jobViewModel: JobViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val sharedText by jobViewModel.sharedJobText.collectAsState()

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            navController.navigate(Screen.AddJob) {
                launchSingleTop = true
            }
        }
    }

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hasRoute(item.screen::class) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hasRoute(item.screen::class) == true,
                            onClick = {
                                navController.navigate(item.screen) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
        ) {
            composable<Screen.Dashboard> {
                DashboardScreen(
                    viewModel = jobViewModel,
                    onAddJob = { navController.navigate(Screen.AddJob) },
                    onJobClick = { jobId -> navController.navigate(Screen.JobDetail(jobId)) },
                )
            }
            composable<Screen.Profile> {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToResumeImport = { navController.navigate(Screen.ResumeImport) },
                    onLogout = { authViewModel.signOut() },
                )
            }
            composable<Screen.Snippets> {
                SnippetsScreen(profileViewModel = profileViewModel)
            }
            composable<Screen.AiSuggestions> {
                AiSuggestionsScreen()
            }
            composable<Screen.AddJob>(
                enterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { it } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } },
            ) {
                AddJobScreen(
                    viewModel = jobViewModel,
                    onBack = { navController.popBackStack() },
                    onJobAnalyzed = { jobId ->
                        navController.navigate(Screen.JobDetail(jobId)) {
                            popUpTo(Screen.Dashboard) { inclusive = false }
                        }
                    },
                )
            }
            composable<Screen.JobDetail>(
                enterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { it } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } },
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.JobDetail>()
                JobDetailScreen(
                    jobId = route.jobId,
                    viewModel = jobViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Screen.ResumeImport>(
                enterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { it } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } },
            ) {
                ResumeImportScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
