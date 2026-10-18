package com.seed.mazemind

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var menuLayout: LinearLayout
    private lateinit var tvLevel: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvHighScoreMenu: TextView
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        menuLayout = findViewById(R.id.menuLayout)
        tvLevel = findViewById(R.id.tvLevel)
        tvScore = findViewById(R.id.tvScore)
        tvHighScoreMenu = findViewById(R.id.tvHighScoreMenu)

        prefs = getSharedPreferences("MazeMindPrefs", Context.MODE_PRIVATE)
        updateHighScoreDisplay()

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            menuLayout.visibility = View.GONE
            // Восстанавливаем сохраненный уровень и счет при старте
            val savedLevel = prefs.getInt("saved_level", 1)
            val savedScore = prefs.getInt("saved_score", 0)
            gameView.loadGame(savedLevel, savedScore)
        }

        findViewById<Button>(R.id.btnUp).setOnClickListener { gameView.movePlayer(0, -1) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { gameView.movePlayer(0, 1) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { gameView.movePlayer(-1, 0) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { gameView.movePlayer(1, 0) }

        gameView.onGameStateChanged = { level, score ->
            tvLevel.text = "Уровень: $level"
            tvScore.text = "Счёт: $score"
            checkAndUpdateHighScore(score)
            // Сохраняем текущий игровой прогресс в память устройства
            prefs.edit()
                .putInt("saved_level", level)
                .putInt("saved_score", score)
                .apply()
        }

        gameView.onGameOverToMenu = {
            menuLayout.visibility = View.VISIBLE
            updateHighScoreDisplay()
        }
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
