package com.example.wordleclone

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

enum class GameState { PLAYING, WON, LOST }
enum class CharStatus { CORRECT, WRONG_POS, ABSENT, INITIAL }

data class CellData(val char: Char = ' ', val status: CharStatus = CharStatus.INITIAL)

class WordleViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = StatsRepository(application)
    val stats = repo.statsFlow

    // Game Data
    var board by mutableStateOf(List(6) { List(5) { CellData() } })
        private set
    var currentRow by mutableStateOf(0)
        private set
    var currentCol by mutableStateOf(0)
        private set
    var gameState by mutableStateOf(GameState.PLAYING)
        private set
    var targetWord by mutableStateOf("")
        private set
    var showStatsDialog by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Keyboard state
    var keyStates = mutableStateOf(mapOf<Char, CharStatus>())

    private var dictionary = setOf<String>()
    private var validSolutions = listOf<String>()
    private var startTime = 0L

    init {
        // FIXED: Launch coroutine on Main thread, switch to IO only for file loading
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                loadWords()
            }
            // Now we are back on Main thread to safely update state
            startNewGame()
        }
    }

    private fun loadWords() {
        try {
            val assetManager = getApplication<Application>().assets
            val inputStream = assetManager.open("words.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Filter 5 letter words
            val allWords = reader.readLines()
                .map { it.trim().uppercase(Locale.ROOT) }
                .filter { it.length == 5 && it.all { c -> c.isLetter() } }

            dictionary = allWords.toSet()
            validSolutions = allWords
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startNewGame() {
        if (validSolutions.isEmpty()) return
        targetWord = validSolutions.random()
        board = List(6) { List(5) { CellData() } }
        currentRow = 0
        currentCol = 0
        keyStates.value = mapOf()
        gameState = GameState.PLAYING
        startTime = System.currentTimeMillis()
        errorMessage = null
        println("Target Word: $targetWord")
    }

    fun onKeyInput(char: Char) {
        if (gameState != GameState.PLAYING) return

        if (char == '⌫') { // Backspace
            if (currentCol > 0) {
                currentCol--
                updateBoard(currentRow, currentCol, ' ')
            }
        } else if (char == '↵') { // Enter
            submitGuess()
        } else { // Letter
            if (currentCol < 5) {
                updateBoard(currentRow, currentCol, char)
                currentCol++
            }
        }
    }

    private fun updateBoard(row: Int, col: Int, char: Char) {
        val newRow = board[row].toMutableList()
        newRow[col] = newRow[col].copy(char = char)
        val newBoard = board.toMutableList()
        newBoard[row] = newRow
        board = newBoard
    }

    private fun submitGuess() {
        if (currentCol != 5) {
            showTemporaryError("Not enough letters")
            return
        }

        val guessWord = board[currentRow].map { it.char }.joinToString("")
        if (!dictionary.contains(guessWord)) {
            showTemporaryError("Not in word list")
            return
        }

        val newRowState = validateGuess(guessWord)
        val newBoard = board.toMutableList()
        newBoard[currentRow] = newRowState
        board = newBoard

        updateKeyboard(newRowState)

        if (guessWord == targetWord) {
            gameState = GameState.WON
            saveGameResult(true)
            showStatsDialog = true
        } else if (currentRow == 5) {
            gameState = GameState.LOST
            saveGameResult(false)
            showStatsDialog = true
        } else {
            currentRow++
            currentCol = 0
        }
    }

    private fun validateGuess(guess: String): List<CellData> {
        val result = MutableList(5) { CellData(guess[it], CharStatus.ABSENT) }
        val targetCharsFreq = targetWord.groupingBy { it }.eachCount().toMutableMap()

        // Pass 1: Greens
        guess.forEachIndexed { index, char ->
            if (char == targetWord[index]) {
                result[index] = CellData(char, CharStatus.CORRECT)
                targetCharsFreq[char] = targetCharsFreq[char]!! - 1
            }
        }

        // Pass 2: Yellows
        guess.forEachIndexed { index, char ->
            if (result[index].status != CharStatus.CORRECT) {
                if (targetCharsFreq.getOrDefault(char, 0) > 0) {
                    result[index] = CellData(char, CharStatus.WRONG_POS)
                    targetCharsFreq[char] = targetCharsFreq[char]!! - 1
                }
            }
        }
        return result
    }

    private fun updateKeyboard(rowResult: List<CellData>) {
        val currentKeys = keyStates.value.toMutableMap()
        rowResult.forEach { cell ->
            val currentStatus = currentKeys[cell.char] ?: CharStatus.INITIAL
            if (cell.status == CharStatus.CORRECT) {
                currentKeys[cell.char] = CharStatus.CORRECT
            } else if (cell.status == CharStatus.WRONG_POS && currentStatus != CharStatus.CORRECT) {
                currentKeys[cell.char] = CharStatus.WRONG_POS
            } else if (cell.status == CharStatus.ABSENT && currentStatus == CharStatus.INITIAL) {
                currentKeys[cell.char] = CharStatus.ABSENT
            }
        }
        keyStates.value = currentKeys
    }

    private fun saveGameResult(won: Boolean) {
        val timeTaken = (System.currentTimeMillis() - startTime) / 1000
        viewModelScope.launch {
            repo.updateStats(won, currentRow + 1, timeTaken)
        }
    }

    private fun showTemporaryError(msg: String) {
        errorMessage = msg
        viewModelScope.launch {
            delay(2000)
            errorMessage = null
        }
    }
}