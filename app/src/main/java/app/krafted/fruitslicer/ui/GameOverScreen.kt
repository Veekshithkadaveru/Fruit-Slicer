package app.krafted.fruitslicer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.krafted.fruitslicer.viewmodel.GameViewModel

private val Gold = Color(0xFFFFD700)

@Composable
fun GameOverScreen(
    viewModel: GameViewModel,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            color = Color(0xFFE94560),
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Score",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp
        )
        Text(
            text = "${uiState.score}",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Best",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp
        )
        Text(
            text = "${uiState.highScore}",
            color = if (uiState.isNewHighScore) Gold else Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        if (uiState.isNewHighScore) {
            Text(
                text = "NEW HIGH SCORE!",
                color = Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560))
            ) {
                Text("PLAY AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onHome,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF533483))
            ) {
                Text("HOME", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
