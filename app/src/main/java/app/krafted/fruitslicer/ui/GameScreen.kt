package app.krafted.fruitslicer.ui

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .collect { onGameOver() }
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
    }
}

@Composable
fun GameHud(state: GameUiState, modifier: Modifier = Modifier) {
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
        Text(
            text = "❤️".repeat(state.lives),
            fontSize = 24.sp
        )
    }
}
