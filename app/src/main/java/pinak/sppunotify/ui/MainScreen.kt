package pinak.sppunotify.ui

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pinak.sppunotify.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : Screen("home", "Results", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List)
    object Revaluation : Screen("reval", "Reval", Icons.Outlined.Refresh, Icons.Filled.Refresh)
    object Links : Screen("links", "Links", Icons.Outlined.Public, Icons.Filled.Public)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    object About : Screen("about", "About", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    // ORDER: Home -> Revaluation -> Links -> Settings -> About
    val items = listOf(Screen.Home, Screen.Revaluation, Screen.Links, Screen.Settings, Screen.About)
    
    val homeListState = rememberLazyListState()
    val linksScrollState = rememberLazyListState()
    val revalScrollState = rememberLazyListState()
    val settingsScrollState = rememberScrollState()
    val aboutScrollState = rememberScrollState()
    
    var lastSelectedRoute by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        SharedTransitionLayout {
            Box(modifier = Modifier.fillMaxSize().consumeWindowInsets(innerPadding)) {
                // Content Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isLandscape) Modifier.padding(start = 112.dp, end = 24.dp) else Modifier
                        )
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            val targetRoute = targetState.destination.route ?: ""
                            val tabRoutes = items.map { it.route }
                            
                            if (initialRoute in tabRoutes && targetRoute in tabRoutes) {
                                val initialIndex = tabRoutes.indexOf(initialRoute)
                                val targetIndex = tabRoutes.indexOf(targetRoute)
                                val direction = if (targetIndex > initialIndex) 1 else -1
                                slideInHorizontally(
                                    initialOffsetX = { it * direction },
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                                ) + fadeIn(animationSpec = tween(300))
                            } else {
                                scaleIn(initialScale = 0.85f, animationSpec = tween(400, easing = EaseOutCubic)) + fadeIn(animationSpec = tween(400))
                            }
                        },
                        exitTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            val targetRoute = targetState.destination.route ?: ""
                            val tabRoutes = items.map { it.route }

                            if (initialRoute in tabRoutes && targetRoute in tabRoutes) {
                                val initialIndex = tabRoutes.indexOf(initialRoute)
                                val targetIndex = tabRoutes.indexOf(targetRoute)
                                val direction = if (targetIndex > initialIndex) -1 else 1
                                slideOutHorizontally(targetOffsetX = { it * direction }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300))
                            } else {
                                scaleOut(targetScale = 1.15f, animationSpec = tween(400, easing = EaseOutCubic)) + fadeOut(animationSpec = tween(400))
                            }
                        },
                        popEnterTransition = {
                            scaleIn(initialScale = 1.15f, animationSpec = tween(400, easing = EaseOutCubic)) + fadeIn(animationSpec = tween(400))
                        },
                        popExitTransition = {
                            scaleOut(targetScale = 0.85f, animationSpec = tween(400, easing = EaseOutCubic)) + fadeOut(animationSpec = tween(400))
                        }
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = hiltViewModel(),
                                onResultClick = { res -> navController.navigate("details/${res.id}") },
                                listState = homeListState,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable
                            )
                        }
                        
                        composable(Screen.Links.route) {
                            LinksScreen(onBackClick = {}, isTopLevel = true, scrollState = linksScrollState)
                        }
                        
                        composable(Screen.Revaluation.route) {
                            RevaluationScreen(listState = revalScrollState)
                        }
                        
                        composable(Screen.Settings.route) {
                            SettingsScreen(scrollState = settingsScrollState)
                        }

                        composable(Screen.About.route) {
                            AboutScreen(scrollState = aboutScrollState)
                        }
                        
                        composable(
                            route = "details/{resultId}",
                            arguments = listOf(navArgument("resultId") { type = NavType.StringType }),
                        ) {
                            val viewModel: DetailsViewModel = hiltViewModel()
                            val resultData by viewModel.result.collectAsState()
                            resultData?.let { res ->
                                ResultDetailsScreen(
                                    result = res,
                                    onBackClick = { navController.popBackStack() },
                                    onOpenBrowser = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                                    onViewInApp = { r -> navController.navigate("resultView/${r.id}") },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable
                                )
                            }
                        }
                        
                        composable(
                            route = "resultView/{resultId}",
                            arguments = listOf(navArgument("resultId") { type = NavType.StringType }),
                        ) {
                            ResultViewScreen(viewModel = hiltViewModel(), onBackClick = { navController.popBackStack() })
                        }
                    }
                }

                // --- NAVIGATION BAR / SIDEBAR ---
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = currentDestination?.route in items.map { it.route }

                if (showBottomBar) {
                    val navEnter = if (isLandscape) slideInHorizontally { -it } else slideInVertically { it }
                    val navExit = if (isLandscape) slideOutHorizontally { -it } else slideOutVertically { it }

                    AnimatedVisibility(
                        visible = true,
                        enter = navEnter + fadeIn(),
                        exit = navExit + fadeOut(),
                        modifier = Modifier.align(if (isLandscape) Alignment.CenterStart else Alignment.BottomCenter)
                    ) {
                        if (isLandscape) {
                            // LANDSCAPE SIDEBAR
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .windowInsetsPadding(WindowInsets.displayCutout)
                                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.width(72.dp).fillMaxHeight(),
                                    shape = RoundedCornerShape(36.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    tonalElevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceEvenly,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        items.forEach { screen ->
                                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (selected) 1.25f else 1f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "iconScale"
                                            )

                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                                tooltip = { PlainTooltip { Text(screen.label) } },
                                                state = rememberTooltipState()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                                        .clickable {
                                                            navigateToTab(navController, screen, lastSelectedRoute, coroutineScope, homeListState, linksScrollState, revalScrollState, settingsScrollState, aboutScrollState)
                                                            lastSelectedRoute = screen.route
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) screen.selectedIcon else screen.icon,
                                                        contentDescription = screen.label,
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // PORTRAIT BOTTOM BAR
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f).height(64.dp),
                                    shape = RoundedCornerShape(32.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    tonalElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items.take(4).forEach { screen ->
                                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (selected) 1.25f else 1f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "iconScale"
                                            )

                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                                tooltip = { PlainTooltip { Text(screen.label) } },
                                                state = rememberTooltipState()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .clickable {
                                                            navigateToTab(navController, screen, lastSelectedRoute, coroutineScope, homeListState, linksScrollState, revalScrollState, settingsScrollState, aboutScrollState)
                                                            lastSelectedRoute = screen.route
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) screen.selectedIcon else screen.icon,
                                                        contentDescription = screen.label,
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val aboutScreen = Screen.About
                                val isAboutSelected = currentDestination?.hierarchy?.any { it.route == aboutScreen.route } == true
                                val aboutScale by animateFloatAsState(
                                    targetValue = if (isAboutSelected) 1.2f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "aboutScale"
                                )
                                
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text(aboutScreen.label) } },
                                    state = rememberTooltipState()
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .clickable {
                                                navigateToTab(navController, aboutScreen, lastSelectedRoute, coroutineScope, homeListState, linksScrollState, revalScrollState, settingsScrollState, aboutScrollState)
                                                lastSelectedRoute = aboutScreen.route
                                            },
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isAboutSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                        tonalElevation = 8.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isAboutSelected) aboutScreen.selectedIcon else aboutScreen.icon,
                                                contentDescription = aboutScreen.label,
                                                tint = if (isAboutSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .graphicsLayer(scaleX = aboutScale, scaleY = aboutScale)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun navigateToTab(
    navController: androidx.navigation.NavHostController,
    screen: Screen,
    lastSelectedRoute: String,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    homeListState: LazyListState,
    linksScrollState: LazyListState,
    revalScrollState: LazyListState,
    settingsScrollState: androidx.compose.foundation.ScrollState,
    aboutScrollState: androidx.compose.foundation.ScrollState
) {
    val route = screen.route
    if (lastSelectedRoute == route) {
        when (route) {
            Screen.Home.route -> coroutineScope.launch { homeListState.animateScrollToItem(0) }
            Screen.Links.route -> coroutineScope.launch { linksScrollState.animateScrollToItem(0) }
            Screen.Revaluation.route -> coroutineScope.launch { revalScrollState.animateScrollToItem(0) }
            Screen.Settings.route -> coroutineScope.launch { settingsScrollState.animateScrollTo(0) }
            Screen.About.route -> coroutineScope.launch { aboutScrollState.animateScrollTo(0) }
        }
    } else {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
