package com.gonzalocamera.padelcounter.mobile.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.window.core.layout.WindowWidthSizeClass
import com.gonzalocamera.padelcounter.mobile.ui.calculator.CalculatorScreen
import com.gonzalocamera.padelcounter.mobile.ui.history.HistoryScreen
import com.gonzalocamera.padelcounter.mobile.ui.history.HistoryViewModel
import com.gonzalocamera.padelcounter.mobile.ui.history.MatchDetailScreen
import com.gonzalocamera.padelcounter.mobile.ui.scoring.ScoringScreen
import com.gonzalocamera.padelcounter.mobile.ui.scoring.ScoringViewModel
import com.gonzalocamera.padelcounter.mobile.ui.settings.SettingsScreen
import com.gonzalocamera.padelcounter.mobile.ui.settings.SettingsViewModel
import com.gonzalocamera.padelcounter.mobile.ui.stats.StatsScreen
import com.gonzalocamera.padelcounter.mobile.ui.stats.StatsViewModel

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Scoring : BottomNavItem("scoring", "Marcador", Icons.Default.SportsScore)
    data object History : BottomNavItem("history", "Historial", Icons.Default.History)
    data object Stats : BottomNavItem("stats", "Estadísticas", Icons.Default.Leaderboard)
    data object Settings : BottomNavItem("settings", "Ajustes", Icons.Default.Settings)
}

private val navItems = listOf(
    BottomNavItem.Scoring,
    BottomNavItem.History,
    BottomNavItem.Stats,
    BottomNavItem.Settings,
)

@Composable
fun NavGraph(
    factory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val adaptiveInfo = currentWindowAdaptiveInfo()

    val showNavChrome = navItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val useRail = shouldUseRail(adaptiveInfo)

    if (useRail && showNavChrome) {
        Row(modifier = modifier.fillMaxSize()) {
            NavigationRail {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationRailItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = { navController.navigateToTab(item.route) },
                    )
                }
            }
            NavHostContent(navController = navController, factory = factory, modifier = Modifier.fillMaxSize())
        }
    } else {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                if (showNavChrome) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = selected,
                                onClick = { navController.navigateToTab(item.route) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHostContent(
                navController = navController,
                factory = factory,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

private fun shouldUseRail(info: WindowAdaptiveInfo): Boolean {
    val widthClass = info.windowSizeClass.windowWidthSizeClass
    return widthClass != WindowWidthSizeClass.COMPACT
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun NavHostContent(
    navController: NavHostController,
    factory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
) {
    val fadeDuration = 220
    NavHost(
        navController = navController,
        startDestination = "scoring",
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(fadeDuration)) },
        exitTransition = { fadeOut(animationSpec = tween(fadeDuration)) },
        popEnterTransition = { fadeIn(animationSpec = tween(fadeDuration)) },
        popExitTransition = { fadeOut(animationSpec = tween(fadeDuration)) },
    ) {
        composable("scoring") {
            val vm: ScoringViewModel = viewModel(factory = factory)
            ScoringScreen(viewModel = vm)
        }
        composable("history") {
            val vm: HistoryViewModel = viewModel(factory = factory)
            HistoryScreen(
                viewModel = vm,
                onMatchClick = { matchId -> navController.navigate("match_detail/$matchId") },
                onPlayMatch = { navController.navigateToTab("scoring") },
            )
        }
        composable("stats") {
            val vm: StatsViewModel = viewModel(factory = factory)
            StatsScreen(viewModel = vm)
        }
        composable("settings") {
            val vm: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = vm,
                onOpenCalculator = { navController.navigate("calculator") },
            )
        }
        composable("calculator") {
            CalculatorScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "match_detail/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            val parent = remember(backStackEntry) { navController.getBackStackEntry("history") }
            val vm: HistoryViewModel = viewModel(
                viewModelStoreOwner = parent,
                factory = factory,
            )
            MatchDetailScreen(
                matchId = matchId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
