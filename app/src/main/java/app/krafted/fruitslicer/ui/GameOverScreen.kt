package app.krafted.fruitslicer.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.krafted.fruitslicer.viewmodel.GameViewModel
import kotlinx.coroutines.delay

private val Gold = Color(0xFFFFD700)
private val RedPrimary = Color(0xFFE94560)
private val RedLight = Color(0xFFFF6B6B)
private val DarkBg = Color(0xFF0D0D1A)
private val DarkBg2 = Color(0xFF1A0A0E)

@Composable
fun GameOverScreen(
    viewModel: GameViewModel,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var titleVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        titleVisible = true
        delay(250)
        contentVisible = true
        delay(200)
        buttonsVisible = true
    }

    val titleScale by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "titleScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "contentAlpha"
    )
    val contentSlide by animateFloatAsState(
        targetValue = if (contentVisible) 0f else 40f,
        animationSpec = tween(500),
        label = "contentSlide"
    )
    val buttonsAlpha by animateFloatAsState(
        targetValue = if (buttonsVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "buttonsAlpha"
    )

    val stars = when {
        uiState.score >= 300 -> 3
        uiState.score >= 100 -> 2
        uiState.score >= 30 -> 1
        else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(DarkBg, DarkBg2, DarkBg))
            )
            .drawBehind {
                // Background slash decorations
                drawLine(
                    color = RedPrimary.copy(alpha = 0.06f),
                    start = Offset(0f, size.height * 0.05f),
                    end = Offset(size.width * 0.75f, size.height * 0.65f),
                    strokeWidth = 90f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = RedPrimary.copy(alpha = 0.04f),
                    start = Offset(size.width * 0.25f, 0f),
                    end = Offset(size.width, size.height * 0.75f),
                    strokeWidth = 60f,
                    cap = StrokeCap.Round
                )
                // Juice splatter blobs
                val splatters = listOf(
                    Offset(size.width * 0.08f, size.height * 0.12f) to 55f,
                    Offset(size.width * 0.88f, size.height * 0.08f) to 40f,
                    Offset(size.width * 0.15f, size.height * 0.82f) to 35f,
                    Offset(size.width * 0.82f, size.height * 0.78f) to 50f,
                    Offset(size.width * 0.50f, size.height * 0.04f) to 28f
                )
                splatters.forEach { (center, radius) ->
                    drawCircle(
                        color = RedPrimary.copy(alpha = 0.08f),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = RedPrimary.copy(alpha = 0.04f),
                        radius = radius * 1.9f,
                        center = center
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top slash accent
            SlashDivider(
                modifier = Modifier.graphicsLayer { scaleX = titleScale; alpha = titleScale }
            )

            Spacer(Modifier.height(16.dp))

            // GAME OVER title with gradient
            Text(
                text = "GAME OVER",
                modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale },
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(RedLight, RedPrimary, RedLight)
                    ),
                    fontSize = 50.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Round reached subtitle
            Text(
                text = "Round ${uiState.round} reached",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer { scaleX = titleScale; alpha = titleScale }
            )

            Spacer(Modifier.height(16.dp))

            // Bottom slash accent
            SlashDivider(
                modifier = Modifier.graphicsLayer { scaleX = titleScale; alpha = titleScale }
            )

            Spacer(Modifier.height(28.dp))

            // Star rating
            Row(
                modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    AnimatedStar(filled = index < stars, delayMs = index * 180)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Score / Best card
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = contentAlpha; translationY = contentSlide }
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.09f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                RedPrimary.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.08f),
                                RedPrimary.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                // NEW BEST badge
                if (uiState.isNewHighScore) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-16).dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Gold, Color(0xFFFFA500))
                                ),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "✦  NEW BEST  ✦",
                            color = Color(0xFF1A1A00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScorePill(label = "SCORE", value = "${uiState.score}", valueColor = Color.White)

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(72.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    ScorePill(
                        label = "BEST",
                        value = "${uiState.highScore}",
                        valueColor = if (uiState.isNewHighScore) Gold else Color.White
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Buttons
            Column(
                modifier = Modifier
                    .graphicsLayer { alpha = buttonsAlpha }
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GradientButton(
                    text = "PLAY AGAIN",
                    gradient = Brush.horizontalGradient(colors = listOf(RedLight, RedPrimary)),
                    onClick = onPlayAgain
                )
                OutlineButton(text = "HOME", onClick = onHome)
            }
        }
    }
}

@Composable
private fun AnimatedStar(filled: Boolean, delayMs: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh),
        label = "starScale"
    )
    Text(
        text = if (filled) "★" else "☆",
        fontSize = if (filled) 44.sp else 36.sp,
        color = if (filled) Gold else Color.White.copy(alpha = 0.22f),
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    )
}

@Composable
private fun ScorePill(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 48.sp
        )
    }
}

@Composable
private fun SlashDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(100.dp)
            .height(3.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        RedPrimary.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(2.dp)
            )
    )
}

@Composable
private fun GradientButton(text: String, gradient: Brush, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
        }
    }
}

@Composable
private fun OutlineButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }
    }
}
