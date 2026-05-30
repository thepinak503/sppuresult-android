package pinak.sppunotify.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.*
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import pinak.sppunotify.R
import pinak.sppunotify.ui.screens.*

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : Screen("home", R.string.nav_results, Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List)
    object Revaluation : Screen("reval", R.string.nav_reval, Icons.Outlined.Refresh, Icons.Filled.Refresh)
    object Circulars : Screen("circulars", R.string.nav_circulars, Icons.Outlined.Description, Icons.Default.Description)
    object ExamDates : Screen("exam_dates", R.string.nav_exam_dates, Icons.Outlined.Event, Icons.Filled.Event)
    object Calculator : Screen("calculator", R.string.nav_calculator, Icons.Outlined.Calculate, Icons.Filled.Calculate)
    object Vault : Screen("vault", R.string.nav_vault, Icons.Outlined.FolderZip, Icons.Filled.FolderZip)
    object Links : Screen("links", R.string.nav_links, Icons.Outlined.Public, Icons.Filled.Public)
    object Settings : Screen("settings", R.string.settings_title, Icons.Outlined.Settings, Icons.Filled.Settings)
    object About : Screen("about", R.string.nav_about, Icons.Outlined.Info, Icons.Filled.Info)
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val items = listOf(
        Screen.Home,
        Screen.Revaluation,
        Screen.Circulars,
        Screen.ExamDates,
        Screen.Calculator,
        Screen.Vault,
        Screen.Links,
        Screen.Settings,
        Screen.About
    )
    
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showNav,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                // Gradient header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "SPPU Result Notify",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "v1.2.0",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationDrawerItem(
                            label = { Text(text = stringResource(screen.labelRes), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            selected = selected,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navigateToTab(navController, screen, lastSelectedRoute, coroutineScope, homeListState, linksScrollState, revalScrollState, circularsScrollState, examDatesScrollState, settingsScrollState, aboutScrollState)
                                lastSelectedRoute = screen.route
                            },
                            icon = { Icon(if (selected) screen.selectedIcon else screen.icon, contentDescription = null) },
                            shape = RoundedCornerShape(20.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    // 🔧 Quick Tools section
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Text(
                        "Quick Tools",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Sync Dashboard", fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navController.navigate("syncDashboard")
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Sync Dashboard") },
                        shape = RoundedCornerShape(20.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Notification History", fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navController.navigate("notificationHistory")
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "Notification History") },
                        shape = RoundedCornerShape(20.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    onMenuClick = { coroutineScope.launch { drawerState.open() } }
                )
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
    onMenuClick: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize(),
        // Tab → Tab: slide direction based on index, fast spring
        enterTransition = {
            val initialRoute = initialState.destination.route ?: ""
            val targetRoute  = targetState.destination.route ?: ""
            val tabRoutes    = items.map { it.route }
            if (initialRoute in tabRoutes && targetRoute in tabRoutes) {
                val dir = if (tabRoutes.indexOf(targetRoute) > tabRoutes.indexOf(initialRoute)) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { (it * 0.15f * dir).toInt() },
                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                ) + fadeIn(animationSpec = tween(300))
            } else {
                // Detail push: scale in + fade
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                ) + fadeIn(animationSpec = tween(400))
            }
        },
        exitTransition = {
            val initialRoute = initialState.destination.route ?: ""
            val targetRoute  = targetState.destination.route ?: ""
            val tabRoutes    = items.map { it.route }
            if (initialRoute in tabRoutes && targetRoute in tabRoutes) {
                val dir = if (tabRoutes.indexOf(targetRoute) > tabRoutes.indexOf(initialRoute)) -1 else 1
                slideOutHorizontally(
                    targetOffsetX = { (it * 0.15f * dir).toInt() },
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeOut(animationSpec = tween(300))
            } else {
                // Slide slightly back + fade out
                slideOutVertically(
                    targetOffsetY = { -(it * 0.04f).toInt() },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        },
        // Pop back: scale out + reverse push
        popEnterTransition = {
            slideInVertically(
                initialOffsetY = { -(it * 0.04f).toInt() },
                animationSpec = tween(400, easing = EaseOutQuart)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            scaleOut(
                targetScale = 0.92f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = hiltViewModel(),
                onResultClick = { res -> navController.navigate("details/${res.id}") },
                onMenuClick = onMenuClick,
                listState = homeListState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable(Screen.Links.route) {
            LinksScreen(onBackClick = onMenuClick, isTopLevel = true, scrollState = linksScrollState)
        }
        composable(Screen.Revaluation.route) {
            RevaluationScreen(listState = revalScrollState, onMenuClick = onMenuClick)
        }
        composable(Screen.Circulars.route) {
            CircularsScreen(listState = circularsScrollState, onMenuClick = onMenuClick)
        }
        composable(Screen.ExamDates.route) {
            ExamDatesScreen(listState = examDatesScrollState, onMenuClick = onMenuClick)
        }
        composable(Screen.Calculator.route) {
            CalculatorScreen(onMenuClick = onMenuClick)
        }
        composable(Screen.Vault.route) {
            VaultScreen(
                onMenuClick = onMenuClick,
                onViewPdf = { filePath, pdfTitle ->
                    val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
                    val encodedTitle = java.net.URLEncoder.encode(pdfTitle, "UTF-8")
                    navController.navigate("pdfViewer/$encodedPath/$encodedTitle")
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(scrollState = settingsScrollState, onMenuClick = onMenuClick)
        }
        composable(Screen.About.route) {
            AboutScreen(scrollState = aboutScrollState, onMenuClick = onMenuClick)
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
                    animatedVisibilityScope = this@composable,
                    viewModel = viewModel
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
        composable(
            route = "syncDashboard",
        ) {
            SyncDashboardScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = "notificationHistory",
        ) {
            NotificationHistoryScreen(onBackClick = { navController.popBackStack() })
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
