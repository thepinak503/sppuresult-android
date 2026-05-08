package pinak.sppunotify.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : Screen("home", "Results", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List)
    object Links : Screen("links", "Links", Icons.Outlined.Public, Icons.Filled.Public)
    object About : Screen("about", "About", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val items = listOf(Screen.Home, Screen.Links, Screen.About)
    
    // Hoisted scroll states for each top-level screen
    val homeListState = rememberLazyListState()
    val linksScrollState = rememberLazyListState()  // LinksScreen uses LazyColumn
    val aboutScrollState = rememberScrollState()
    
    // Track last selected route for detecting re-selection
    var lastSelectedRoute by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar on top-level screens
            val showBottomBar = currentDestination?.route in items.map { it.route }
            
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { 
                                AnimatedContent(targetState = selected, label = "icon") { isSelected ->
                                    Icon(if (isSelected) screen.selectedIcon else screen.icon, contentDescription = null)
                                }
                            },
                            label = { Text(screen.label) },
                            selected = selected,
                            onClick = {
                                val currentRoute = screen.route
                                if (lastSelectedRoute == currentRoute) {
                                    // Re-selected the same tab - scroll to top
                                    when (currentRoute) {
                                        Screen.Home.route -> {
                                            coroutineScope.launch {
                                                homeListState.animateScrollToItem(0)
                                            }
                                        }
                                        Screen.Links.route -> {
                                            coroutineScope.launch {
                                                linksScrollState.animateScrollToItem(0)
                                            }
                                        }
                                        Screen.About.route -> {
                                            coroutineScope.launch {
                                                aboutScrollState.animateScrollTo(0)
                                            }
                                        }
                                    }
                                } else {
                                    lastSelectedRoute = currentRoute
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onResultClick = { result ->
                        navController.navigate("details/${result.id}")
                    },
                    listState = homeListState,
                )
            }
            
            composable(Screen.Links.route) {
                LinksScreen(
                    onBackClick = { 
                        // Back not needed for top level bottom nav item
                    },
                    isTopLevel = true,
                    scrollState = linksScrollState,
                )
            }
            
            composable(Screen.About.route) {
                AboutScreen(
                    scrollState = aboutScrollState,
                )
            }
            
            composable(
                route = "details/{resultId}",
                arguments = listOf(navArgument("resultId") { type = NavType.StringType }),
            ) {
                val viewModel: DetailsViewModel = hiltViewModel()
                val resultData by viewModel.result.collectAsState()
                
                resultData?.let { result ->
                    ResultDetailsScreen(
                        result = result,
                        onBackClick = { navController.popBackStack() },
                        onOpenBrowser = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        },
                        onViewInApp = { res ->
                            navController.navigate("resultView/${res.id}")
                        },
                    )
                }
            }
            
            composable(
                route = "resultView/{resultId}",
                arguments = listOf(navArgument("resultId") { type = NavType.StringType }),
            ) {
                val viewModel: ResultViewViewModel = hiltViewModel()
                ResultViewScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                )
            }
        }
    }
}
