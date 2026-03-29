package app.krafted.fruitslicer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.krafted.fruitslicer.R
import app.krafted.fruitslicer.viewmodel.GameViewModel

private val FruitGold = Color(0xFFFFD36E)
private val FruitRed = Color(0xFFFF5A6C)
private val FruitPurple = Color(0xFF7946B8)
private val FruitPurpleDeep = Color(0xFF1D1028)
private val FruitGreen = Color(0xFF7CFF9E)
private val DarkBg = Color(0xFF09060D)

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    onPlay: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "homeAnimations")
    
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridSlide"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3D1A6E).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        Image(
            painter = painterResource(R.drawable.bg_main),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.25f },
            colorFilter = ColorFilter.tint(
                Color(0xFF2B164D), BlendMode.Color
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.15f }
                .drawWithContent {
                    drawContent()
                    val gridSize = 64f
                    for (x in 0..(size.width / gridSize).toInt() + 1) {
                        drawLine(
                            color = FruitPurple,
                            start = Offset(x * gridSize, 0f),
                            end = Offset(x * gridSize, size.height),
                            strokeWidth = 2f
                        )
                    }
                    for (y in 0..(size.height / gridSize).toInt() + 1) {
                        val yPos = (y * gridSize + bgOffset) % size.height
                        drawLine(
                            color = FruitGold.copy(alpha = 0.8f),
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 2f
                        )
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Text(
                text = "FRUIT SLICER",
                style = TextStyle(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White, FruitGold, Color(0xFFFFAD52))
                    ),
                    shadow = Shadow(
                        color = FruitGold.copy(alpha = 0.4f),
                        blurRadius = 16f
                    )
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = floatOffset }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    FruitGold.copy(alpha = glowAlpha * 0.7f),
                                    Color.Transparent
                                ),
                                radius = size.minDimension * 0.85f
                            )
                        )
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.banner),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (uiState.highScore > 0) {
                PremiumScoreBadge(score = uiState.highScore)
                Spacer(modifier = Modifier.height(42.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            PremiumPlayButton(onClick = onPlay, glowAlpha = glowAlpha)
        }
    }
}

@Composable
private fun PremiumScoreBadge(score: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF201330).copy(alpha = 0.85f),
                        Color(0xFF130A1F).copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(FruitGold.copy(alpha = 0.8f), FruitPurple.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = "★",
            fontSize = 20.sp,
            color = FruitGold,
            style = TextStyle(
                shadow = Shadow(color = FruitGold.copy(alpha = 0.7f), blurRadius = 12f)
            )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SUPERIOR RECORD",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = score.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = FruitGold,
                style = TextStyle(
                    shadow = Shadow(color = FruitGold.copy(alpha = 0.4f), blurRadius = 8f)
                )
            )
        }
    }
}

@Composable
private fun PremiumPlayButton(onClick: () -> Unit, glowAlpha: Float) {
    Box(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(FruitGreen.copy(alpha = 0.08f + (glowAlpha * 0.15f)))
            .border(2.5.dp, FruitGreen.copy(alpha = 0.8f + (glowAlpha * 0.4f)), RoundedCornerShape(18.dp))
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PLAY",
            style = TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                color = FruitGreen,
                shadow = Shadow(
                    color = FruitGreen.copy(alpha = 0.8f + glowAlpha),
                    blurRadius = 24f
                )
            )
        )
    }
}
