package com.seed.mazemind

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var menuLayout: LinearLayout
    private lateinit var tvLevel: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvHighScoreMenu: TextView
    private lateinit var btnSoundToggle: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        menuLayout = findViewById(R.id.menuLayout)
        tvLevel = findViewById(R.id.tvLevel)
        tvScore = findViewById(R.id.tvScore)
        tvHighScoreMenu = findViewById(R.id.tvHighScoreMenu)
        btnSoundToggle = findViewById(R.id.btnSoundToggle)

        prefs = getSharedPreferences("MazeMindPrefs", Context.MODE_PRIVATE)
        updateHighScoreDisplay()
        updateSoundButtonLabel()

        // Корректный перехват системной кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (menuLayout.visibility == View.GONE) {
                    menuLayout.visibility = View.VISIBLE
                    updateHighScoreDisplay()
                } else {
                    finish()
                }
            }
        })

        // Кнопка включения/выключения звука
        btnSoundToggle.setOnClickListener {
            val currentSound = prefs.getBoolean("sound_enabled", true)
            val newSound = !currentSound
            prefs.edit().putBoolean("sound_enabled", newSound).apply()
            updateSoundButtonLabel()
            gameView.setSoundEnabled(newSound)
        }

        // Продолжить сохраненную игру
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            menuLayout.visibility = View.GONE
            val savedLevel = prefs.getInt("saved_level", 1)
            val savedScore = prefs.getInt("saved_score", 0)
            val savedDiff = prefs.getInt("saved_difficulty", 2)
            val soundEnabled = prefs.getBoolean("sound_enabled", true)
            
            gameView.setSoundEnabled(soundEnabled)
            gameView.loadGame(savedLevel, savedScore, savedDiff)
        }

        findViewById<Button>(R.id.btnEasy).setOnClickListener { startGameWithDifficulty(1) }
        findViewById<Button>(R.id.btnMedium).setOnClickListener { startGameWithDifficulty(2) }
        findViewById<Button>(R.id.btnHard).setOnClickListener { startGameWithDifficulty(3) }

        findViewById<Button>(R.id.btnUp).setOnClickListener { gameView.movePlayer(0, -1) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { gameView.movePlayer(0, 1) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { gameView.movePlayer(-1, 0) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { gameView.movePlayer(1, 0) }

        gameView.onGameStateChanged = { level, score, diff ->
            tvLevel.text = "Уровень: $level"
            tvScore.text = "Счёт: $score"
            checkAndUpdateHighScore(score)
            prefs.edit()
                .putInt("saved_level", level)
                .putInt("saved_score", score)
                .putInt("saved_difficulty", diff)
                .apply()
        }

        gameView.onGameOverToMenu = {
            menuLayout.visibility = View.VISIBLE
            updateHighScoreDisplay()
        }
    }

    private fun startGameWithDifficulty(difficulty: Int) {
        menuLayout.visibility = View.GONE
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        prefs.edit()
            .putInt("saved_level", 1)
            .putInt("saved_score", 0)
            .putInt("saved_difficulty", difficulty)
            .apply()
        gameView.setSoundEnabled(soundEnabled)
        gameView.startNewGame(difficulty)
    }

    private fun updateSoundButtonLabel() {
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        btnSoundToggle.text = if (soundEnabled) "Звук: Вкл" else "Звук: Выкл"
    }

    private fun checkAndUpdateHighScore(score: Int) {
        val high = prefs.getInt("high_score", 0)
        if (score > high) {
            prefs.edit().putInt("high_score", score).apply()
        }
    }

    private fun updateHighScoreDisplay() {
        val high = prefs.getInt("high_score", 0)
        tvHighScoreMenu.text = "Рекорд: $high"
    }
}
