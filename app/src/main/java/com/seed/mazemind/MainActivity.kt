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
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvHighScoreMenu: TextView
    private lateinit var tvAchievements: TextView
    private lateinit var btnSoundToggle: Button
    private lateinit var btnThemeToggle: Button
    private lateinit var btnHint: Button
    private lateinit var prefs: SharedPreferences

    private val themeNames = arrayOf("Неон", "Киберпанк", "Лес")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        menuLayout = findViewById(R.id.menuLayout)
        tvLevel = findViewById(R.id.tvLevel)
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvHighScoreMenu = findViewById(R.id.tvHighScoreMenu)
        tvAchievements = findViewById(R.id.tvAchievements)
        btnSoundToggle = findViewById(R.id.btnSoundToggle)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        btnHint = findViewById(R.id.btnHint)

        prefs = getSharedPreferences("MazeMindPrefs", Context.MODE_PRIVATE)
        
        updateHighScoreDisplay()
        updateSoundButtonLabel()
        updateThemeButtonLabel()
        updateAchievementsDisplay()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (menuLayout.visibility == View.GONE) {
                    gameView.stopTimer()
                    menuLayout.visibility = View.VISIBLE
                    updateHighScoreDisplay()
                    updateAchievementsDisplay()
                } else {
                    finish()
                }
            }
        })

        btnSoundToggle.setOnClickListener {
            val currentSound = prefs.getBoolean("sound_enabled", true)
            val newSound = !currentSound
            prefs.edit().putBoolean("sound_enabled", newSound).apply()
            updateSoundButtonLabel()
            gameView.setSoundEnabled(newSound)
        }

        btnThemeToggle.setOnClickListener {
            var currentTheme = prefs.getInt("theme_index", 0)
            currentTheme = (currentTheme + 1) % themeNames.size
            prefs.edit().putInt("theme_index", currentTheme).apply()
            updateThemeButtonLabel()
            gameView.setThemeIndex(currentTheme)
        }

        btnHint.setOnClickListener {
            gameView.showHint()
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            menuLayout.visibility = View.GONE
            val savedLevel = prefs.getInt("saved_level", 1)
            val savedScore = prefs.getInt("saved_score", 0)
            val savedDiff = prefs.getInt("saved_difficulty", 2)
            val soundEnabled = prefs.getBoolean("sound_enabled", true)
            val themeIndex = prefs.getInt("theme_index", 0)
            
            gameView.setSoundEnabled(soundEnabled)
            gameView.setThemeIndex(themeIndex)
            gameView.loadGame(savedLevel, savedScore, savedDiff)
        }

        findViewById<Button>(R.id.btnEasy).setOnClickListener { startGameWithDifficulty(1) }
        findViewById<Button>(R.id.btnMedium).setOnClickListener { startGameWithDifficulty(2) }
        findViewById<Button>(R.id.btnHard).setOnClickListener { startGameWithDifficulty(3) }

        findViewById<Button>(R.id.btnUp).setOnClickListener { gameView.movePlayer(0, -1) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { gameView.movePlayer(0, 1) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { gameView.movePlayer(-1, 0) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { gameView.movePlayer(1, 0) }

        gameView.onGameStateChanged = { level, score, diff, seconds, hintsLeft ->
            tvLevel.text = "Уровень: $level"
            tvTimer.text = "⏱ ${seconds}с"
            tvScore.text = "Счёт: $score"
            btnHint.text = "💡 Подсказка ($hintsLeft/3)"
            
            checkAndUpdateHighScore(score)
            checkAchievements(level, score)

            prefs.edit()
                .putInt("saved_level", level)
                .putInt("saved_score", score)
                .putInt("saved_difficulty", diff)
                .apply()
        }

        gameView.onGameOverToMenu = {
            menuLayout.visibility = View.VISIBLE
            updateHighScoreDisplay()
            updateAchievementsDisplay()
        }
    }

    private fun startGameWithDifficulty(difficulty: Int) {
        menuLayout.visibility = View.GONE
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        val themeIndex = prefs.getInt("theme_index", 0)
        
        prefs.edit()
            .putInt("saved_level", 1)
            .putInt("saved_score", 0)
            .putInt("saved_difficulty", difficulty)
            .apply()
            
        gameView.setSoundEnabled(soundEnabled)
        gameView.setThemeIndex(themeIndex)
        gameView.startNewGame(difficulty)
    }

    private fun updateSoundButtonLabel() {
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        btnSoundToggle.text = if (soundEnabled) "Звук: Вкл" else "Звук: Выкл"
    }

    private fun updateThemeButtonLabel() {
        val themeIndex = prefs.getInt("theme_index", 0)
        btnThemeToggle.text = "Тема: ${themeNames[themeIndex]}"
    }

    private fun checkAndUpdateHighScore(score: Int) {
        val high = prefs.getInt("high_score", 0)
        if (score > high) {
            prefs.edit().putInt("high_score", score).apply()
        }
    }

    private fun checkAchievements(level: Int, score: Int) {
        if (level >= 5) prefs.edit().putBoolean("ach_lvl5", true).apply()
        if (score >= 1000) prefs.edit().putBoolean("ach_score1000", true).apply()
    }

    private fun updateHighScoreDisplay() {
        val high = prefs.getInt("high_score", 0)
        tvHighScoreMenu.text = "Рекорд: $high"
    }

    private fun updateAchievementsDisplay() {
        val achLvl5 = if (prefs.getBoolean("ach_lvl5", false)) "✅ Дойти до 5 уровня\n" else "🔒 Дойти до 5 уровня\n"
        val achScore = if (prefs.getBoolean("ach_score1000", false)) "✅ Набрать 1000 очков" else "🔒 Набрать 1000 очков"
        tvAchievements.text = "🏆 Достижения:\n$achLvl5$achScore"
    }
}
