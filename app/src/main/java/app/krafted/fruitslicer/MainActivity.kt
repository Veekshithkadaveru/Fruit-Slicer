package app.krafted.fruitslicer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.krafted.fruitslicer.ui.GameOverScreen
import app.krafted.fruitslicer.ui.GameScreen
import app.krafted.fruitslicer.ui.HomeScreen
import app.krafted.fruitslicer.ui.SplashScreen
import app.krafted.fruitslicer.ui.theme.FruitSlicerTheme
import app.krafted.fruitslicer.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FruitSlicerTheme {
                FruitSlicerApp()
            }
        }
    }
}

@Composable
fun FruitSlicerApp() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                viewModel = gameViewModel,
                onPlay = {
                    gameViewModel.resetGame()
                    navController.navigate("game")
                }
            )
        }
        composable("game") {
            GameScreen(
                viewModel = gameViewModel,
                onGameOver = {
                    navController.navigate("game_over") {
                        popUpTo("game") { inclusive = true }
                    }
                }
            )
        }
        composable("game_over") {
            GameOverScreen(
                viewModel = gameViewModel,
                onPlayAgain = {
                    gameViewModel.resetGame()
                    navController.navigate("game") {
                        popUpTo("home")
                    }
                },
                onHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
