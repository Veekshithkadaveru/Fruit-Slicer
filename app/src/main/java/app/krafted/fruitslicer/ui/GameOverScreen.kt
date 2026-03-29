package app.krafted.fruitslicer.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.krafted.fruitslicer.R
import app.krafted.fruitslicer.viewmodel.GameViewModel

private val FruitGold = Color(0xFFFFD36E)
private val FruitRed = Color(0xFFFF5A6C)
private val FruitPurple = Color(0xFF7946B8)
private val FruitPurpleDeep = Color(0xFF1D1028)
private val FruitGreen = Color(0xFF7CFF9E)

@Composable
fun GameOverScreen(
    viewModel: GameViewModel,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = true) { }

    val infiniteTransition = rememberInfiniteTransition(label = "gameOverBackground")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridSlide"
    )

    val headerColor = if (uiState.isNewHighScore) FruitGold else FruitRed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF140B1A),
                        FruitPurpleDeep,
                        Color(0xFF09060D)
                    )
                )
            )
    ) {
        Image(
            painter = painterResource(R.drawable.bg_main),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.18f }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.12f }
                .drawWithContent {
                    drawContent()
                    val gridSize = 56f
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
                            color = FruitRed,
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StatusChip(
                text = if (uiState.isNewHighScore) "NEW HIGH SCORE" else "RUN COMPLETE",
                color = if (uiState.isNewHighScore) FruitGold else FruitRed
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "GAME OVER",
                style = TextStyle(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = headerColor,
                    shadow = Shadow(
                        color = headerColor.copy(alpha = 0.65f),
                        blurRadius = 20f
                    )
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (uiState.isNewHighScore) {
                    "A sharper run, a higher mark."
                } else {
                    "Reset, reload, and beat your best."
                },
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = FruitPurple.copy(alpha = 0.3f),
                        blurRadius = 10f
                    )
                ),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1D1322).copy(alpha = 0.94f),
                                Color(0xFF120C16).copy(alpha = 0.88f)
                            )
                        )
                    )
                    .border(1.5.dp, FruitPurple.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniAsset(R.drawable.fruit_orange, FruitGold)
                        Text(
                            text = "FINAL SCORE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FruitGold.copy(alpha = 0.82f),
                            letterSpacing = 2.sp
                        )
                        MiniAsset(R.drawable.fruit_pineapple, FruitRed)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = uiState.score.toString(),
                        style = TextStyle(
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            brush = Brush.verticalGradient(
                                listOf(Color.White, FruitGold, Color(0xFFFFAD52))
                            ),
                            shadow = Shadow(
                                color = FruitGold.copy(alpha = 0.35f),
                                blurRadius = 18f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, FruitPurple, Color.Transparent)
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Best",
                            value = uiState.highScore.toString(),
                            valueColor = if (uiState.isNewHighScore) FruitGold else Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Round",
                            value = uiState.round.toString(),
                            valueColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeonButton(
                    text = "PLAY AGAIN",
                    onClick = onPlayAgain,
                    color = FruitGreen,
                    modifier = Modifier.weight(1f)
                )
                NeonButton(
                    text = "HOME",
                    onClick = onHome,
                    color = FruitRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun MiniAsset(drawableRes: Int, tintColor: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(tintColor.copy(alpha = 0.12f))
            .border(1.dp, tintColor.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, FruitPurple.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.62f),
            letterSpacing = 1.1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
    }
}

@Composable
private fun NeonButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .border(2.dp, color, RoundedCornerShape(10.dp))
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = color,
                shadow = Shadow(
                    color = color.copy(alpha = 0.75f),
                    blurRadius = 14f
                )
            )
        )
    }
}
