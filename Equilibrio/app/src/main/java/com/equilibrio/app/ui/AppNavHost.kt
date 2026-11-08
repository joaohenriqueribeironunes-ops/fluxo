package com.equilibrio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.equilibrio.app.ui.screens.AccuracyScreen
import com.equilibrio.app.ui.screens.CalmScreen
import com.equilibrio.app.ui.screens.CheckInScreen
import com.equilibrio.app.ui.screens.CrisisHistoryScreen
import com.equilibrio.app.ui.screens.DebriefScreen
import com.equilibrio.app.ui.screens.ForecastScreen
import com.equilibrio.app.ui.screens.HomeScreen
import com.equilibrio.app.ui.screens.InsightsScreen
import com.equilibrio.app.ui.screens.OnboardingScreen
import com.equilibrio.app.ui.screens.ReportScreen
import com.equilibrio.app.ui.screens.SettingsScreen
import com.equilibrio.app.ui.screens.TimelineScreen
import com.equilibrio.core.ui.glass.LiquidBackground
import com.equilibrio.core.ui.navigation.GlassNavItem
import com.equilibrio.core.ui.navigation.GlassPillNav

@Composable
fun AppNavHost(
    viewModel: MainViewModel,
    startRoute: String,
    pendingRoute: String?,
    onPendingConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbar.showSnackbar(message) }
    }

    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        runCatching { navController.navigate(route) { launchSingleTop = true } }
        onPendingConsumed()
    }

    val navItems = remember {
        listOf(
            GlassNavItem(Routes.HOME, "Início", Icons.Outlined.Home),
            GlassNavItem(Routes.TIMELINE, "Linha", Icons.Outlined.Timeline),
            GlassNavItem(Routes.CHECKIN, "Check-in", Icons.Outlined.AddCircleOutline),
            GlassNavItem(Routes.FORECAST, "Previsão", Icons.Outlined.Psychology),
            GlassNavItem(Routes.INSIGHTS, "Insights", Icons.Outlined.Insights)
        )
    }

    Box(Modifier.fillMaxSize()) {
        LiquidBackground()
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (currentRoute != null && currentRoute in Routes.mainDestinations) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        GlassPillNav(
                            items = navItems,
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onDone = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenCheckIn = { navController.navigate(Routes.CHECKIN) },
                        onOpenForecast = { navController.navigate(Routes.FORECAST) },
                        onOpenCalm = { navController.navigate(Routes.CALM) },
                        onOpenInsights = { navController.navigate(Routes.INSIGHTS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }
                composable(Routes.CHECKIN) {
                    CheckInScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.HOME)
                            }
                        }
                    )
                }
                composable(Routes.TIMELINE) {
                    TimelineScreen(
                        viewModel = viewModel,
                        onOpenCrises = { navController.navigate(Routes.CRISES) },
                        onOpenReport = { navController.navigate(Routes.REPORT) }
                    )
                }
                composable(Routes.FORECAST) {
                    ForecastScreen(
                        viewModel = viewModel,
                        onOpenCalm = { navController.navigate(Routes.CALM) },
                        onOpenInsights = { navController.navigate(Routes.INSIGHTS) }
                    )
                }
                composable(Routes.INSIGHTS) {
                    InsightsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenAccuracy = { navController.navigate(Routes.ACCURACY) },
                        onOpenReport = { navController.navigate(Routes.REPORT) }
                    )
                }
                composable(Routes.CRISES) {
                    CrisisHistoryScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenDebrief = { crisisId ->
                            navController.navigate(Routes.debrief(crisisId))
                        }
                    )
                }
                composable(Routes.REPORT) {
                    ReportScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenReport = { navController.navigate(Routes.REPORT) },
                        onOpenAccuracy = { navController.navigate(Routes.ACCURACY) }
                    )
                }
                composable(Routes.ACCURACY) {
                    AccuracyScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.CALM) {
                    CalmScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.DEBRIEF_PATTERN,
                    arguments = listOf(
                        navArgument(Routes.ARG_CRISIS_ID) { type = NavType.LongType }
                    )
                ) { entry ->
                    DebriefScreen(
                        viewModel = viewModel,
                        crisisId = entry.arguments?.getLong(Routes.ARG_CRISIS_ID) ?: -1L,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
