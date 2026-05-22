package pinak.sppunotify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColorScheme = darkColorScheme(
    primary                = PrimaryDark,
    onPrimary              = OnPrimaryDark,
    primaryContainer       = PrimaryContainerDark,
    onPrimaryContainer     = OnPrimaryContainerDark,
    secondary              = SecondaryDark,
    onSecondary            = OnSecondaryDark,
    secondaryContainer     = SecondaryContainerDark,
    onSecondaryContainer   = OnSecondaryContainerDark,
    tertiary               = TertiaryDark,
    onTertiary             = OnTertiaryDark,
    tertiaryContainer      = TertiaryContainerDark,
    onTertiaryContainer    = OnTertiaryContainerDark,
    error                  = ErrorDark,
    onError                = OnErrorDark,
    errorContainer         = ErrorContainerDark,
    onErrorContainer       = OnErrorContainerDark,
    background             = BackgroundDark,
    onBackground           = OnBackgroundDark,
    surface                = SurfaceDark,
    onSurface              = OnSurfaceDark,
    surfaceVariant         = SurfaceVariantDark,
    onSurfaceVariant       = OnSurfaceVariantDark,
    surfaceContainerLow    = SurfaceContainerLowDark,
    surfaceContainer       = SurfaceContainerDark,
    surfaceContainerHigh   = SurfaceContainerHighDark,
    outline                = OutlineDark,
    outlineVariant         = OutlineVariantDark,
    scrim                  = ScrimDark,
)

private val LightColorScheme = lightColorScheme(
    primary                = PrimaryLight,
    onPrimary              = OnPrimaryLight,
    primaryContainer       = PrimaryContainerLight,
    onPrimaryContainer     = OnPrimaryContainerLight,
    secondary              = SecondaryLight,
    onSecondary            = OnSecondaryLight,
    secondaryContainer     = SecondaryContainerLight,
    onSecondaryContainer   = OnSecondaryContainerLight,
    tertiary               = TertiaryLight,
    onTertiary             = OnTertiaryLight,
    tertiaryContainer      = TertiaryContainerLight,
    onTertiaryContainer    = OnTertiaryContainerLight,
    error                  = ErrorLight,
    onError                = OnErrorLight,
    errorContainer         = ErrorContainerLight,
    onErrorContainer       = OnErrorContainerLight,
    background             = BackgroundLight,
    onBackground           = OnBackgroundLight,
    surface                = SurfaceLight,
    onSurface              = OnSurfaceLight,
    surfaceVariant         = SurfaceVariantLight,
    onSurfaceVariant       = OnSurfaceVariantLight,
    surfaceContainerLow    = SurfaceContainerLowLight,
    surfaceContainer       = SurfaceContainerLight,
    surfaceContainerHigh   = SurfaceContainerHighLight,
    outline                = OutlineLight,
    outlineVariant         = OutlineVariantLight,
    scrim                  = ScrimLight,
)

@Composable
fun SPPUResultWatchTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) {
                // Keep our richer dark surface tones on top of dynamic primary
                dynamicDarkColorScheme(context).copy(
                    background           = BackgroundDark,
                    surface              = SurfaceDark,
                    onBackground         = OnBackgroundDark,
                    onSurface            = OnSurfaceDark,
                    surfaceVariant       = SurfaceVariantDark,
                    onSurfaceVariant     = OnSurfaceVariantDark,
                    surfaceContainerLow  = SurfaceContainerLowDark,
                    surfaceContainer     = SurfaceContainerDark,
                    surfaceContainerHigh = SurfaceContainerHighDark,
                    outline              = OutlineDark,
                    outlineVariant       = OutlineVariantDark,
                )
            } else {
                dynamicLightColorScheme(context).copy(
                    surfaceContainerLow  = SurfaceContainerLowLight,
                    surfaceContainer     = SurfaceContainerLight,
                    surfaceContainerHigh = SurfaceContainerHighLight,
                )
            }
        }
        isDark -> DarkColorScheme
        else   -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.statusBarColor     = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars     = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
