package pinak.sppunotify.ui

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import pinak.sppunotify.util.safeStartActivity
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
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.Event
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
import android.net.Uri
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
    object Circulars : Screen("circulars", "Circulars", Icons.Outlined.Description, Icons.Default.Description)
    object ExamDates : Screen("exam_dates", "Exam Dates", Icons.Outlined.Event, Icons.Filled.Event)
    object Calculator : Screen("calculator", "Calc", Icons.Outlined.Calculate, Icons.Filled.Calculate)
    object Vault : Screen("vault", "Vault", Icons.Outlined.FolderZip, Icons.Filled.FolderZip)
    object Links : Screen("links", "Links", Icons.Outlined.Public, Icons.Filled.Public)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    object About : Screen("about", "About", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val useRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val items = listOf(Screen.Home, Screen.Revaluation, Screen.Circulars, Screen.ExamDates, Screen.Calculator, Screen.Vault, Screen.Links, Screen.About)
    val barItems = listOf(Screen.Home, Screen.Revaluation, Screen.Circulars, Screen.ExamDates, Screen.Calculator, Screen.Vault, Screen.Links)

    val homeListState = rememberLazyListState()
    val linksScrollState = rememberLazyListState()
    val revalScrollState = rememberLazyListState()
    val circularsScrollState = rememberLazyListState()
    val examDatesScrollState = rememberLazyListState()
    val settingsScrollState = rememberScrollState()
    val aboutScrollState = rememberScrollState()

    var lastSelectedRoute by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showNav = currentDestination?.route in items.map { it.route }

    Scaffold { contentPadding ->
        if (useRail) {
            // Tablet/Landscape/Freeform: rail and content in a Row
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = showNav,
                    enter = slideInHorizontally { -it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut(),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.displayCutout)
                            .padding(start = 20.dp, top = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.width(72.dp).weight(1f),
                            shape = RoundedCornerShape(36.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            tonalElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.SpaceEvenly,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                barItems.forEach { screen ->
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
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    navigateToTab(navController, screen, lastSelectedRoute, coroutineScope, homeListState, linksScrollState, revalScrollState, circularsScrollState, examDatesScrollState, settingsScrollState, aboutScrollState)
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

                        val about = Screen.About
                        val isAboutSelected = currentDestination?.hierarchy?.any { it.route == about.route } == true
                        val aboutScale by animateFloatAsState(
                            targetValue = if (isAboutSelected) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "aboutScale"
                        )
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(about.label) } },
                            state = rememberTooltipState()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        navigateToTab(navController, about, lastSelectedRoute, coroutineScope, homeListState, linksScrollState, revalScrollState, circularsScrollState, examDatesScrollState, settingsScrollState, aboutScrollState)
                                        lastSelectedRoute = about.route
                                    },
                                shape = RoundedCornerShape(22.dp),
                                color = if (isAboutSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                tonalElevation = if (isAboutSelected) 4.dp else 12.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isAboutSelected) about.selectedIcon else about.icon,
                                        contentDescription = about.label,
                                        tint = if (isAboutSelected) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .graphicsLayer(scaleX = aboutScale, scaleY = aboutScale)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).padding(contentPadding)) {
                    SharedTransitionLayout {
                        NavHostContent(
                            sharedTransitionScope = this,
                            navController = navController,
                            context = context,
                            items = items,
                            homeListState = homeListState,
                            linksScrollState = linksScrollState,
                            revalScrollState = revalScrollState,
                            circularsScrollState = circularsScrollState,
                            examDatesScrollState = examDatesScrollState,
                            settingsScrollState = settingsScrollState,
                            aboutScrollState = aboutScrollState,
                            onNavigateToSettings = { navController.navigate("settings") },
                        )
                    }
                }
            }
        } else {
            // Portrait: content + bottom bar
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                    SharedTransitionLayout {
                        NavHostContent(
                            sharedTransitionScope = this,
                            navController = navController,
                            context = context,
                            items = items,
                            homeListState = homeListState,
                            linksScrollState = linksScrollState,
                            revalScrollState = revalScrollState,
                            circularsScrollState = circularsScrollState,
                            examDatesScrollState = examDatesScrollState,
                            settingsScrollState = settingsScrollState,
                            aboutScrollState = aboutScrollState,
                            onNavigateToSettings = { navController.navigate("settings") },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showNav,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomBar(
                        barItems = barItems,
                        aboutScreen = Screen.About,
                        currentDestination = currentDestination,
                        onItemClick = { screen ->
                            navigateToTab(navController, screen, lastSelectedRoute, coroutineScope, homeListState, linksScrollState, revalScrollState, circularsScrollState, examDatesScrollState, settingsScrollState, aboutScrollState)
                            lastSelectedRoute = screen.route
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NavHostContent(
    sharedTransitionScope: SharedTransitionScope,
    navController: androidx.navigation.NavHostController,
    context: android.content.Context,
    items: List<Screen>,
    homeListState: LazyListState,
    linksScrollState: LazyListState,
    revalScrollState: LazyListState,
    circularsScrollState: LazyListState,
    examDatesScrollState: LazyListState,
    settingsScrollState: androidx.compose.foundation.ScrollState,
    aboutScrollState: androidx.compose.foundation.ScrollState,
    onNavigateToSettings: () -> Unit,
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
                onSettingsClick = onNavigateToSettings,
                listState = homeListState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable(Screen.Links.route) {
            LinksScreen(onBackClick = {}, isTopLevel = true, scrollState = linksScrollState)
        }
        composable(Screen.Revaluation.route) {
            RevaluationScreen(listState = revalScrollState)
        }
        composable(Screen.Circulars.route) {
            CircularsScreen(listState = circularsScrollState)
        }
        composable(Screen.ExamDates.route) {
            ExamDatesScreen(listState = examDatesScrollState)
        }
        composable(Screen.Calculator.route) {
            CalculatorScreen()
        }
        composable(Screen.Vault.route) {
            VaultScreen(
                onViewPdf = { filePath, pdfTitle ->
                    val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
                    val encodedTitle = java.net.URLEncoder.encode(pdfTitle, "UTF-8")
                    navController.navigate("pdfViewer/$encodedPath/$encodedTitle")
                }
            )
        }
        composable("settings") {
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
                    onOpenBrowser = { url -> context.safeStartActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                    onViewInApp = { r -> navController.navigate("resultView/${r.id}") },
                    sharedTransitionScope = sharedTransitionScope,
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
        composable(
            route = "pdfViewer/{encodedPath}/{pdfTitle}",
            arguments = listOf(
                navArgument("encodedPath") { type = NavType.StringType },
                navArgument("pdfTitle") { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val fileUri = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("encodedPath") ?: "", "UTF-8")
            val pdfTitle = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("pdfTitle") ?: "", "UTF-8")
            PdfViewerScreen(
                fileUri = fileUri,
                title = pdfTitle,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomBar(
    barItems: List<Screen>,
    aboutScreen: Screen,
    currentDestination: androidx.navigation.NavDestination?,
    onItemClick: (Screen) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                barItems.forEach { screen ->
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
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .clickable { onItemClick(screen) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.icon,
                                contentDescription = screen.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                            )
                        }
                    }
                }
            }
        }

        val isAboutSelected = currentDestination?.hierarchy?.any { it.route == aboutScreen.route } == true
        val aboutScale by animateFloatAsState(
            targetValue = if (isAboutSelected) 1.15f else 1f,
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
                    .size(68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onItemClick(aboutScreen) },
                shape = RoundedCornerShape(22.dp),
                color = if (isAboutSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                tonalElevation = if (isAboutSelected) 4.dp else 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAboutSelected) aboutScreen.selectedIcon else aboutScreen.icon,
                        contentDescription = aboutScreen.label,
                        tint = if (isAboutSelected) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer(scaleX = aboutScale, scaleY = aboutScale)
                    )
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
    circularsScrollState: LazyListState,
    examDatesScrollState: LazyListState,
    settingsScrollState: androidx.compose.foundation.ScrollState,
    aboutScrollState: androidx.compose.foundation.ScrollState
) {
    val route = screen.route
    if (lastSelectedRoute == route) {
        when (route) {
            Screen.Home.route -> coroutineScope.launch { homeListState.animateScrollToItem(0) }
            Screen.Links.route -> coroutineScope.launch { linksScrollState.animateScrollToItem(0) }
            Screen.Revaluation.route -> coroutineScope.launch { revalScrollState.animateScrollToItem(0) }
            Screen.Circulars.route -> coroutineScope.launch { circularsScrollState.animateScrollToItem(0) }
            Screen.ExamDates.route -> coroutineScope.launch { examDatesScrollState.animateScrollToItem(0) }
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
