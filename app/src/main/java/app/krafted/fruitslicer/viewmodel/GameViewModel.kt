package app.krafted.fruitslicer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.krafted.fruitslicer.data.AppDatabase
import app.krafted.fruitslicer.data.ScoreEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val score: Int = 0,
    val lives: Int = 3,
    val round: Int = 1,
    val isGameOver: Boolean = false,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false
)

data class DifficultyConfig(
    val maxOnScreen: Int,
    val speedMultiplier: Float,
    val bombWeight: Int,
    val spawnIntervalMs: Long
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val scoreDao = AppDatabase.getDatabase(application).scoreDao()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _difficultyConfig = MutableStateFlow(difficultyForRound(1))
    val difficultyConfig: StateFlow<DifficultyConfig> = _difficultyConfig.asStateFlow()

    private var roundTimerJob: Job? = null

    init {
        scoreDao.getHighestScore().onEach { best ->
            _uiState.update { it.copy(highScore = best ?: 0) }
        }.launchIn(viewModelScope)

        startRoundTimer()
    }

    fun onFruitSliced(points: Int) {
        if (_uiState.value.isGameOver) return
        _uiState.update { it.copy(score = it.score + points) }
    }

    fun onFruitMissed() {
        if (_uiState.value.isGameOver) return
        loseLife()
    }

    fun onBombSwiped() {
        if (_uiState.value.isGameOver) return
        triggerGameOver()
    }

    fun resetGame() {
        roundTimerJob?.cancel()
        _uiState.update {
            GameUiState(highScore = it.highScore)
        }
        _difficultyConfig.value = difficultyForRound(1)
        startRoundTimer()
    }

    private fun loseLife() {
        val newLives = (_uiState.value.lives - 1).coerceAtLeast(0)
        _uiState.update { it.copy(lives = newLives) }
        if (newLives <= 0) triggerGameOver()
    }

    private fun triggerGameOver() {
        roundTimerJob?.cancel()
        val state = _uiState.value
        val isNew = state.score > state.highScore
        _uiState.update { it.copy(
            isGameOver = true,
            isNewHighScore = isNew,
            highScore = if (isNew) state.score else it.highScore
        ) }
        if (isNew) {
            viewModelScope.launch {
                scoreDao.insert(ScoreEntity(score = state.score))
            }
        }
    }

    private fun startRoundTimer() {
        roundTimerJob?.cancel()
        roundTimerJob = viewModelScope.launch {
            while (!_uiState.value.isGameOver) {
                delay(15_000L)
                if (!_uiState.value.isGameOver) advanceRound()
            }
        }
    }

    private fun advanceRound() {
        val nextRound = _uiState.value.round + 1
        _uiState.update { it.copy(round = nextRound) }
        _difficultyConfig.value = difficultyForRound(nextRound)
    }

    private fun difficultyForRound(round: Int): DifficultyConfig = when {
        round <= 1 -> DifficultyConfig(
            maxOnScreen = 2,
            speedMultiplier = 1.0f,
            bombWeight = 0,
            spawnIntervalMs = 1200
        )

        round == 2 -> DifficultyConfig(
            maxOnScreen = 3,
            speedMultiplier = 1.0f,
            bombWeight = 0,
            spawnIntervalMs = 1100
        )

        round == 3 -> DifficultyConfig(
            maxOnScreen = 3,
            speedMultiplier = 1.3f,
            bombWeight = 4,
            spawnIntervalMs = 1000
        )

        round == 4 -> DifficultyConfig(
            maxOnScreen = 4,
            speedMultiplier = 1.3f,
            bombWeight = 6,
            spawnIntervalMs = 900
        )

        round == 5 -> DifficultyConfig(
            maxOnScreen = 4,
            speedMultiplier = 1.6f,
            bombWeight = 8,
            spawnIntervalMs = 800
        )

        else -> DifficultyConfig(
            maxOnScreen = 5,
            speedMultiplier = 1.6f,
            bombWeight = 12,
            spawnIntervalMs = 700
        )
    }
}
