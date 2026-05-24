package pinak.sppunotify.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

/**
 * Standard top app bar for all primary screens.
 * Uses CenterAlignedTopAppBar + ExtraBold title — consistent across the whole app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navIcon: ImageVector,
    navContentDescription: String = "Menu",
    onNavClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable () -> Unit = {},
) {
    AppTopBar(
        titleContent = {
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navIcon = navIcon,
        navContentDescription = navContentDescription,
        onNavClick = onNavClick,
        scrollBehavior = scrollBehavior,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    titleContent: @Composable () -> Unit,
    navIcon: ImageVector,
    navContentDescription: String = "Menu",
    onNavClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            AnimatedContent(
                targetState = titleContent,
                transitionSpec = {
                    fadeIn(tween(300)) + scaleIn(initialScale = 0.95f) togetherWith
                    fadeOut(tween(200)) + scaleOut(targetScale = 0.95f)
                },
                label = "TitleAnim"
            ) { content ->
                content()
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavClick) {
                Icon(navIcon, contentDescription = navContentDescription)
            }
        },
        actions = { actions() },
        scrollBehavior = scrollBehavior,
    )
}
