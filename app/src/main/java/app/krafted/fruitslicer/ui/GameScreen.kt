package app.krafted.fruitslicer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.krafted.fruitslicer.game.SlicerGameView
import app.krafted.fruitslicer.viewmodel.GameUiState
import app.krafted.fruitslicer.viewmodel.GameViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onGameOver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val difficultyConfig by viewModel.difficultyConfig.collectAsState()

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.isGameOver }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                kotlinx.coroutines.delay(350)
                onGameOver()
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                SlicerGameView(ctx).apply {
                    onFruitSliced = { type, _, _ -> viewModel.onFruitSliced(type.points) }
                    onFruitMissed = { viewModel.onFruitMissed() }
                    onBombSwiped = { viewModel.onBombSwiped() }
                }
            },
            update = { view ->
                view.setRound(uiState.round)
                view.applyDifficulty(
                    speedMultiplier = difficultyConfig.speedMultiplier,
                    bombWeight = difficultyConfig.bombWeight,
                    maxOnScreen = difficultyConfig.maxOnScreen,
                    spawnIntervalMs = difficultyConfig.spawnIntervalMs
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        GameHud(
            state = uiState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        AnimatedVisibility(
            visible = uiState.isGameOver,
            enter = fadeIn(animationSpec = tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
            )
        }
    }
}

@Composable
fun GameHud(state: GameUiState, modifier: Modifier = Modifier) {
    val maxLives = 3
    val shakeAnimatables = remember { List(maxLives) { Animatable(0f) } }
    val prevLives = remember { mutableIntStateOf(state.lives) }
    val density = LocalDensity.current

    LaunchedEffect(state.lives) {
        val lost = prevLives.intValue - state.lives
        if (lost > 0) {
            val idx = state.lives.coerceIn(0, maxLives - 1)
            val anim = shakeAnimatables[idx]
            val shakePx = with(density) { 8.dp.toPx() }
            val spec = spring<Float>(dampingRatio = 0.3f, stiffness = 1200f)
            anim.snapTo(0f)
            anim.animateTo(-shakePx, spec)
            anim.animateTo(shakePx * 0.75f, spec)
            anim.animateTo(-shakePx * 0.5f, spec)
            anim.animateTo(shakePx * 0.3f, spec)
            anim.animateTo(-shakePx * 0.15f, spec)
            anim.animateTo(0f, spec)
        }
        prevLives.intValue = state.lives
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${state.score}",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Row {
            repeat(maxLives) { index ->
                val offsetX = shakeAnimatables[index].value
                Text(
                    text = if (index < state.lives) "❤️" else "🖤",
                    fontSize = 24.sp,
                    modifier = Modifier.graphicsLayer { translationX = offsetX }
                )
            }
        }
    }
}
