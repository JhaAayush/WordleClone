package com.example.wordleclone

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "game_stats")

data class GameStats(
    val gamesPlayed: Int = 0,
    val winDistribution: List<Int> = List(6) { 0 }, // Wins for 1 guess, 2 guesses...
    val maxStreak: Int = 0,
    val currentStreak: Int = 0,
    val bestTimeSeconds: Long = Long.MAX_VALUE,
    val totalTimeSeconds: Long = 0,
    val totalWins: Int = 0
)

class StatsRepository(private val context: Context) {
    private val GAMES_PLAYED = intPreferencesKey("games_played")
    private val WINS_1 = intPreferencesKey("wins_1")
    private val WINS_2 = intPreferencesKey("wins_2")
    private val WINS_3 = intPreferencesKey("wins_3")
    private val WINS_4 = intPreferencesKey("wins_4")
    private val WINS_5 = intPreferencesKey("wins_5")
    private val WINS_6 = intPreferencesKey("wins_6")
    private val MAX_STREAK = intPreferencesKey("max_streak")
    private val CUR_STREAK = intPreferencesKey("cur_streak")
    private val BEST_TIME = longPreferencesKey("best_time")
    private val TOTAL_TIME = longPreferencesKey("total_time")
    private val TOTAL_WINS = intPreferencesKey("total_wins")

    val statsFlow: Flow<GameStats> = context.dataStore.data.map { pref ->
        val dist = listOf(
            pref[WINS_1] ?: 0, pref[WINS_2] ?: 0, pref[WINS_3] ?: 0,
            pref[WINS_4] ?: 0, pref[WINS_5] ?: 0, pref[WINS_6] ?: 0
        )
        GameStats(
            gamesPlayed = pref[GAMES_PLAYED] ?: 0,
            winDistribution = dist,
            maxStreak = pref[MAX_STREAK] ?: 0,
            currentStreak = pref[CUR_STREAK] ?: 0,
            bestTimeSeconds = pref[BEST_TIME] ?: Long.MAX_VALUE,
            totalTimeSeconds = pref[TOTAL_TIME] ?: 0L,
            totalWins = pref[TOTAL_WINS] ?: 0
        )
    }

    suspend fun updateStats(won: Boolean, guesses: Int, timeSeconds: Long) {
        context.dataStore.edit { pref ->
            pref[GAMES_PLAYED] = (pref[GAMES_PLAYED] ?: 0) + 1

            if (won) {
                val winKey = when(guesses) {
                    1 -> WINS_1; 2 -> WINS_2; 3 -> WINS_3; 4 -> WINS_4; 5 -> WINS_5; else -> WINS_6
                }
                pref[winKey] = (pref[winKey] ?: 0) + 1
                pref[TOTAL_WINS] = (pref[TOTAL_WINS] ?: 0) + 1

                // Streaks
                val newCurStreak = (pref[CUR_STREAK] ?: 0) + 1
                pref[CUR_STREAK] = newCurStreak
                pref[MAX_STREAK] = maxOf(pref[MAX_STREAK] ?: 0, newCurStreak)

                // Time
                pref[TOTAL_TIME] = (pref[TOTAL_TIME] ?: 0L) + timeSeconds
                pref[BEST_TIME] = minOf(pref[BEST_TIME] ?: Long.MAX_VALUE, timeSeconds)
            } else {
                pref[CUR_STREAK] = 0
            }
        }
    }
}