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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelPalette
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.gonzalocamera.padelcounter.mobile.ui.rating.RatingPromptDialog
import com.gonzalocamera.padelcounter.mobile.ui.rating.RatingViewModel
import com.gonzalocamera.padelcounter.mobile.ui.rating.openPlayStoreListing
import com.gonzalocamera.padelcounter.mobile.ui.scoring.ScoringScreen
import com.gonzalocamera.padelcounter.mobile.ui.scoring.ScoringViewModel
import com.gonzalocamera.padelcounter.mobile.ui.settings.SettingsScreen
import com.gonzalocamera.padelcounter.mobile.ui.settings.SettingsViewModel
import com.gonzalocamera.padelcounter.mobile.ui.stats.StatsScreen
import com.gonzalocamera.padelcounter.mobile.ui.stats.StatsViewModel
import kotlinx.coroutines.delay

/** Primero que vea sus números; el pedido llega después. */
private const val STATS_MOMENT_DELAY_MS = 700L

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
    val context = LocalContext.current

    // Una sola instancia por Activity (el owner acá es la Activity, no un backStackEntry),
    // así el modal se renderiza en un único lugar y no se pueden apilar dos.
    val ratingViewModel: RatingViewModel = viewModel(factory = factory)
    val showRatingPrompt by ratingViewModel.visible.collectAsState()

    // El pedido de calificación se evalúa en un "momento de valor", nunca en el arranque
    // en frío: la espera vive en el LaunchedEffect para que salir de la pantalla la
    // cancele sola — si el usuario rebotó, no hubo momento.
    //
    // Acá solo se cubre Estadísticas, que sí es una ruta. El otro momento —el detalle de un
    // partido— es un pane interno de `HistoryScreen`, no una ruta, así que se detecta allá.
    LaunchedEffect(navBackStackEntry?.id) {
        if (currentDestination?.route == BottomNavItem.Stats.route) {
            delay(STATS_MOMENT_DELAY_MS)
            ratingViewModel.onStatsViewed()
        }
    }

    val showNavChrome = navItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val useRail = shouldUseRail(adaptiveInfo)

    if (useRail && showNavChrome) {
        Row(modifier = modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = PadelPalette.Background,
            ) {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationRailItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                        selected = selected,
                        onClick = { navController.navigateToTab(item.route) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = PadelPalette.Gold,
                            selectedTextColor = PadelPalette.Gold,
                            indicatorColor = PadelPalette.Gold.copy(alpha = 0.12f),
                            unselectedIconColor = PadelPalette.TextFaint,
                            unselectedTextColor = PadelPalette.TextFaint,
                        ),
                    )
                }
            }
            NavHostContent(
                navController = navController,
                factory = factory,
                modifier = Modifier.fillMaxSize(),
                onMatchShared = ratingViewModel::onMatchShared,
                onMatchDetailViewed = ratingViewModel::onMatchDetailViewed,
            )
        }
    } else {
        Scaffold(
            modifier = modifier,
            containerColor = PadelPalette.Background,
            bottomBar = {
                if (showNavChrome) {
                    NavigationBar(
                        containerColor = PadelPalette.Background,
                        modifier = Modifier.topGoldHairline(),
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                selected = selected,
                                onClick = { navController.navigateToTab(item.route) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PadelPalette.Gold,
                                    selectedTextColor = PadelPalette.Gold,
                                    indicatorColor = PadelPalette.Gold.copy(alpha = 0.12f),
                                    unselectedIconColor = PadelPalette.TextFaint,
                                    unselectedTextColor = PadelPalette.TextFaint,
                                ),
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
                onMatchShared = ratingViewModel::onMatchShared,
                onMatchDetailViewed = ratingViewModel::onMatchDetailViewed,
            )
        }
    }

    if (showRatingPrompt) {
        RatingPromptDialog(
            onRate = {
                ratingViewModel.onRate()
                openPlayStoreListing(context)
            },
            onLater = ratingViewModel::onLater,
            onNever = ratingViewModel::onNever,
        )
    }
}

private fun Modifier.topGoldHairline(): Modifier = drawBehind {
    drawLine(
        color = PadelPalette.Gold.copy(alpha = 0.25f),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
        strokeWidth = 1.5f,
    )
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
    onMatchShared: () -> Unit = {},
    onMatchDetailViewed: () -> Unit = {},
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
                onShared = onMatchShared,
                onMatchDetailViewed = onMatchDetailViewed,
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
                onShared = onMatchShared,
            )
        }
    }
}
