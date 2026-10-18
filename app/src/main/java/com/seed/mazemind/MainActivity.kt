package com.seed.mazemind

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var menuLayout: LinearLayout
    private lateinit var languageLayout: LinearLayout
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

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("MazeMindPrefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("selected_language", "") ?: ""
        if (langCode.isNotEmpty()) {
            val locale = Locale(langCode)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        menuLayout = findViewById(R.id.menuLayout)
        languageLayout = findViewById(R.id.languageLayout)
        tvLevel = findViewById(R.id.tvLevel)
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvHighScoreMenu = findViewById(R.id.tvHighScoreMenu)
        tvAchievements = findViewById(R.id.tvAchievements)
        btnSoundToggle = findViewById(R.id.btnSoundToggle)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        btnHint = findViewById(R.id.btnHint)

        prefs = getSharedPreferences("MazeMindPrefs", Context.MODE_PRIVATE)

        val isFirstRun = prefs.getBoolean("is_first_run", true)
        if (isFirstRun) {
            languageLayout.visibility = View.VISIBLE
            menuLayout.visibility = View.GONE
        } else {
            languageLayout.visibility = View.GONE
            menuLayout.visibility = View.VISIBLE
        }

        setupLanguageButtons()
        updateHighScoreDisplay()
        updateSoundButtonLabel()
        updateThemeButtonLabel()
        updateAchievementsDisplay()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (menuLayout.visibility == View.GONE && languageLayout.visibility == View.GONE) {
                    gameView.stopTimer()
                    menuLayout.visibility = View.VISIBLE
                    updateHighScoreDisplay()
                    updateAchievementsDisplay()
                } else {
                    finish()
                }
            }
        })

        findViewById<Button>(R.id.btnExit).setOnClickListener {
            finishAffinity()
        }

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
            val savedHints = prefs.getInt("saved_hints", 3) // Загрузка сохраненных подсказок
            val soundEnabled = prefs.getBoolean("sound_enabled", true)
            val themeIndex = prefs.getInt("theme_index", 0)
            
            gameView.setSoundEnabled(soundEnabled)
            gameView.setThemeIndex(themeIndex)
            gameView.loadGame(savedLevel, savedScore, savedDiff, savedHints)
        }

        findViewById<Button>(R.id.btnEasy).setOnClickListener { startGameWithDifficulty(1) }
        findViewById<Button>(R.id.btnMedium).setOnClickListener { startGameWithDifficulty(2) }
        findViewById<Button>(R.id.btnHard).setOnClickListener { startGameWithDifficulty(3) }

        findViewById<Button>(R.id.btnUp).setOnClickListener { gameView.movePlayer(0, -1) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { gameView.movePlayer(0, 1) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { gameView.movePlayer(-1, 0) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { gameView.movePlayer(1, 0) }

        gameView.onGameStateChanged = { level, score, diff, seconds, hintsLeft ->
            tvLevel.text = "${getString(R.string.level_prefix)}$level"
            tvTimer.text = "⏱ ${seconds}с"
            tvScore.text = "${getString(R.string.score_prefix)}$score"
            btnHint.text = "${getString(R.string.hint_btn)} ($hintsLeft/3)"
            
            checkAndUpdateHighScore(score)
            checkAchievements(level, score)

            // Сохраняем текущее количество оставшихся подсказок
            prefs.edit()
                .putInt("saved_level", level)
                .putInt("saved_score", score)
                .putInt("saved_difficulty", diff)
                .putInt("saved_hints", hintsLeft)
                .apply()
        }

        gameView.onGameOverToMenu = {
            menuLayout.visibility = View.VISIBLE
            updateHighScoreDisplay()
            updateAchievementsDisplay()
        }
    }

    private fun setupLanguageButtons() {
        findViewById<Button>(R.id.btnLangRu).setOnClickListener { setAppLanguage("ru") }
        findViewById<Button>(R.id.btnLangEn).setOnClickListener { setAppLanguage("en") }
    }

    private fun setAppLanguage(langCode: String) {
        prefs.edit()
            .putString("selected_language", langCode)
            .putBoolean("is_first_run", false)
            .apply()

        recreate()
    }

    private fun startGameWithDifficulty(difficulty: Int) {
        menuLayout.visibility = View.GONE
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        val themeIndex = prefs.getInt("theme_index", 0)
        
        prefs.edit()
            .putInt("saved_level", 1)
            .putInt("saved_score", 0)
            .putInt("saved_difficulty", difficulty)
            .putInt("saved_hints", 3) // Новая игра — сброс подсказок на 3
            .apply()
            
        gameView.setSoundEnabled(soundEnabled)
        gameView.setThemeIndex(themeIndex)
        gameView.startNewGame(difficulty)
    }

    private fun updateSoundButtonLabel() {
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        btnSoundToggle.text = if (soundEnabled) getString(R.string.sound_on) else getString(R.string.sound_off)
    }

    private fun updateThemeButtonLabel() {
        val themeIndex = prefs.getInt("theme_index", 0)
        val themeRes = when (themeIndex) {
            1 -> R.string.theme_cyber
            2 -> R.string.theme_forest
            else -> R.string.theme_neon
        }
        btnThemeToggle.text = getString(themeRes)
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
        tvHighScoreMenu.text = "${getString(R.string.high_score)}$high"
    }

    private fun updateAchievementsDisplay() {
        val achLvl5 = if (prefs.getBoolean("ach_lvl5", false)) "✅ " else "🔒 "
        val achScore = if (prefs.getBoolean("ach_score1000", false)) "✅ " else "🔒 "
        tvAchievements.text = "${getString(R.string.achievements)}\n${achLvl5}Lvl 5  |  ${achScore}1000 pts"
    }
}
