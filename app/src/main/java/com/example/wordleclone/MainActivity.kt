package com.example.wordleclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh // Placeholder for sun/moon if needed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordleclone.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1. Detect system theme initially
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            // 2. Force the theme based on our variable, not just the system
            MaterialTheme(
                colorScheme = if (isDarkTheme) com.example.wordleclone.ui.theme.DarkColorScheme else com.example.wordleclone.ui.theme.LightColorScheme
            ) {
                // 3. Pass the state and the toggle function down
                WordleApp(isDark = isDarkTheme, onToggleTheme = { isDarkTheme = !isDarkTheme })
            }
        }
    }
}

@Composable
fun WordleApp(
    viewModel: WordleViewModel = viewModel(),
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val board = viewModel.board
    val keyStates by viewModel.keyStates
    val showStats = viewModel.showStatsDialog
    val stats by viewModel.stats.collectAsState(initial = GameStats())
    val error = viewModel.errorMessage
    val gameState = viewModel.gameState

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HEADER ROW
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                // Title centered
                Text(
                    text = "WORDLE",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Theme Toggle Button (Right side)
                Button(
                    onClick = onToggleTheme,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = if (isDark) "☀\uFE0F" else "🌙", // Sun or Moon emoji
                        fontSize = 24.sp
                    )
                }
            }

            // GRID
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                board.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { cell ->
                            WordleCell(cell, isDark)
                        }
                    }
                }
            }

            // KEYBOARD
            Keyboard(keyStates, isDark) { viewModel.onKeyInput(it) }
        }

        // Error Toast
        if (error != null) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(bottom = 200.dp),
                color = Color.Black,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(text = error, color = Color.White, modifier = Modifier.padding(16.dp))
            }
        }

        // Stats Dialog
        if (showStats) {
            StatsDialog(stats, gameState, viewModel.targetWord) {
                viewModel.showStatsDialog = false
                viewModel.startNewGame()
            }
        }
    }
}

@Composable
fun WordleCell(cell: CellData, isDark: Boolean) {
    // Colors need to adapt manually if we are overriding system theme
    val bgColor by animateColorAsState(
        targetValue = when (cell.status) {
            CharStatus.CORRECT -> Green
            CharStatus.WRONG_POS -> Yellow
            CharStatus.ABSENT -> if (isDark) Color(0xFF3A3A3C) else DarkGray
            CharStatus.INITIAL -> Color.Transparent
        }
    )

    val borderColor = if (cell.status == CharStatus.INITIAL && cell.char != ' ') {
        if (isDark) Color(0xFF565758) else Color(0xFF878A8C)
    } else {
        Color.Transparent
    }

    val finalBorder = if (cell.status == CharStatus.INITIAL && cell.char == ' ')
        BorderStroke(2.dp, if(isDark) Color(0xFF3A3A3C) else LightGray)
    else BorderStroke(2.dp, borderColor)

    Box(
        modifier = Modifier
            .size(60.dp)
            .background(bgColor)
            .border(finalBorder),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.char.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = if (cell.status != CharStatus.INITIAL) Color.White else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun Keyboard(keyStates: Map<Char, CharStatus>, isDark: Boolean, onKey: (Char) -> Unit) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEachIndexed { i, rowStr ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (i == 2) KeyButton("ENTER", 50.dp, if(isDark) Color(0xFF818384) else Color(0xFFD3D6DA)) { onKey('↵') }

                rowStr.forEach { char ->
                    val status = keyStates[char] ?: CharStatus.INITIAL
                    val color = when(status) {
                        CharStatus.CORRECT -> Green
                        CharStatus.WRONG_POS -> Yellow
                        CharStatus.ABSENT -> if (isDark) Color(0xFF3A3A3C) else DarkGray
                        CharStatus.INITIAL -> if (isDark) Color(0xFF818384) else LightGray // Key Default
                    }
                    KeyButton(char.toString(), 32.dp, color) { onKey(char) }
                }

                if (i == 2) KeyButton("⌫", 50.dp, if(isDark) Color(0xFF818384) else Color(0xFFD3D6DA)) { onKey('⌫') }
            }
        }
    }
}

// ... Keep KeyButton and StatsDialog exactly the same as before ...
@Composable
fun KeyButton(text: String, width: androidx.compose.ui.unit.Dp, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .height(50.dp)
            .background(color, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatsDialog(stats: GameStats, gameState: GameState, targetWord: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (gameState == GameState.WON) "YOU WON!" else "GAME OVER", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Answer: $targetWord", modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.onSurface)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBox("Played", stats.gamesPlayed.toString())
                    StatBox("Win %", if(stats.gamesPlayed>0) "${(stats.totalWins * 100 / stats.gamesPlayed)}%" else "0%")
                    StatBox("Streak", stats.currentStreak.toString())
                    StatBox("Max", stats.maxStreak.toString())
                }
                Spacer(modifier = Modifier.height(16.dp))

                val avgTime = if(stats.totalWins > 0) stats.totalTimeSeconds / stats.totalWins else 0
                Text("Avg Time: ${avgTime}s | Best Time: ${if(stats.bestTimeSeconds == Long.MAX_VALUE) "-" else stats.bestTimeSeconds}s", color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                    Text("PLAY AGAIN", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}