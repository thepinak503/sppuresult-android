package pinak.sppunotify.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pinak.sppunotify.R
import pinak.sppunotify.ui.theme.SppuAccent
import pinak.sppunotify.ui.theme.SppuBlue

@Composable
fun BrandSplashScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    var logoScale by remember { mutableStateOf(0.6f) }
    var logoAlpha by remember { mutableStateOf(0f) }
    var textAlpha by remember { mutableStateOf(0f) }
    var textOffsetY by remember { mutableStateOf(40.dp) }
    var loaderAlpha by remember { mutableStateOf(0f) }
    var progress by remember { mutableStateOf(0f) }

    // Animations
    val animatedScale by animateFloatAsState(
        targetValue = logoScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )
    val animatedLogoAlpha by animateFloatAsState(
        targetValue = logoAlpha,
        animationSpec = tween(durationMillis = 800),
        label = "LogoAlpha"
    )
    val animatedTextAlpha by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = tween(durationMillis = 1000),
        label = "TextAlpha"
    )
    val animatedTextOffsetY by animateDpAsState(
        targetValue = textOffsetY,
        animationSpec = tween(durationMillis = 1000),
        label = "TextOffsetY"
    )
    val animatedLoaderAlpha by animateFloatAsState(
        targetValue = loaderAlpha,
        animationSpec = tween(durationMillis = 800),
        label = "LoaderAlpha"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1600, easing = LinearOutSlowInEasing),
        label = "Progress"
    )

    // Pulsing background glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "GlowTransition")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Trigger animations in sequence
    LaunchedEffect(Unit) {
        logoScale = 1.0f
        logoAlpha = 1.0f

        delay(400)
        textAlpha = 1.0f
        textOffsetY = 0.dp

        delay(300)
        loaderAlpha = 1.0f
        progress = 1.0f

        delay(1200)
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F263F),
                        Color(0xFF080D11)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative soft mesh background highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = SppuBlue.copy(alpha = 0.15f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.1f, size.height * 0.2f)
            )
            drawCircle(
                color = Color(0xFF138D75).copy(alpha = 0.08f), // Secondary teal/emerald soft glow
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .alpha(animatedLogoAlpha)
            ) {
                // Pulsing glow aura
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(glowScale)
                        .alpha(glowAlpha)
                        .background(SppuBlue.copy(alpha = 0.5f), CircleShape)
                        .blur(24.dp)
                )

                // Main Logo Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(animatedScale)
                        .background(Color(0xFF0E1E2B), CircleShape)
                        .border(1.5.dp, SppuAccent.copy(alpha = 0.8f), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "SPPU Result Watch Logo",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Title Block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = animatedTextOffsetY)
                    .alpha(animatedTextAlpha)
            ) {
                Text(
                    text = "SPPU RESULT WATCH",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Real-time updates, smart insights",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Loading Progress Bar
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .alpha(animatedLoaderAlpha),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .width(160.dp)
                        .clip(CircleShape),
                    color = SppuAccent,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}
