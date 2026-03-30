package app.krafted.fruitslicer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.krafted.fruitslicer.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val SplashDarkBg = Color(0xFF07040B)
private val SplashPurple = Color(0xFF6B2FA0)
private val SplashGold = Color(0xFFFFD36E)
private val SplashCyan = Color(0xFF5CF5E8)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.4f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val glowRadius = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        glowRadius.animateTo(
            targetValue = 1f,
            animationSpec = tween(1800, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay(150)
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay(150)
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(700)
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay(1100)
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay(2800)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashInfinite")

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val particleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleRotation"
    )

    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SplashPurple.copy(alpha = 0.35f),
                            SplashDarkBg
                        ),
                        radius = 1400f
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glowRadius.value * 0.6f }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val particleCount = 8
            val orbitRadius = size.minDimension * 0.35f

            for (i in 0 until particleCount) {
                val angle = Math.toRadians(
                    (particleRotation + (i * 360f / particleCount)).toDouble()
                )
                val wobble = sin(particleRotation * 0.03 + i) * 20f
                val px = cx + (orbitRadius + wobble.toFloat()) * cos(angle).toFloat()
                val py = cy + (orbitRadius + wobble.toFloat()) * sin(angle).toFloat()
                val particleAlpha = (0.3f + 0.5f * sin(particleRotation * 0.05 + i * 0.7).toFloat())
                    .coerceIn(0f, 1f)

                val particleColor = if (i % 2 == 0) SplashGold else SplashCyan

                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha * 0.3f),
                    radius = 12f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha * 0.8f),
                    radius = 4f,
                    center = Offset(px, py)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        translationY = floatY
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SplashGold.copy(alpha = glowPulse * glowRadius.value * 0.5f),
                                    SplashPurple.copy(alpha = glowPulse * glowRadius.value * 0.2f),
                                    Color.Transparent
                                ),
                                radius = size.minDimension * 1.1f
                            )
                        )
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_logo),
                    contentDescription = "Fruit Slicer Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "FRUIT SLICER",
                style = TextStyle(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SplashGold.copy(alpha = 0.6f),
                            Color.White,
                            SplashGold,
                            Color.White,
                            SplashGold.copy(alpha = 0.6f)
                        ),
                        startX = shimmerOffset * 800f,
                        endX = (shimmerOffset + 0.5f) * 800f
                    ),
                    shadow = Shadow(
                        color = SplashGold.copy(alpha = 0.5f),
                        blurRadius = 20f
                    )
                ),
                modifier = Modifier
                    .graphicsLayer { alpha = titleAlpha.value }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SLICE  •  SCORE  •  CONQUER",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    color = SplashCyan.copy(alpha = 0.7f),
                    shadow = Shadow(
                        color = SplashCyan.copy(alpha = 0.4f),
                        blurRadius = 10f
                    )
                ),
                modifier = Modifier
                    .graphicsLayer { alpha = subtitleAlpha.value }
            )
        }
    }
}
