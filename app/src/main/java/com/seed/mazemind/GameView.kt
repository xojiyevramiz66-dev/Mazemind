package com.seed.mazemind

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Stack
import kotlin.math.abs
import kotlin.math.max

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var cols = 11
    private var rows = 15
    private var level = 1
    private var score = 0
    private var difficulty = 2
    private var soundEnabled = true
    private var secondsElapsed = 0

    private lateinit var maze: Array<IntArray>
    private var playerX = 1
    private var playerY = 1
    private var exitX = 1
    private var exitY = 1

    private val wallPaint = Paint().apply { color = 0xFF3A506B.toInt() }
    private val pathPaint = Paint().apply { color = 0xFF1C2541.toInt() }
    private val playerPaint = Paint().apply { color = 0xFF4CC9F0.toInt(); isAntiAlias = true }
    private val exitPaint = Paint().apply { color = 0xFF4BB543.toInt(); isAntiAlias = true }

    // Лямбда принимает: уровень, счет, сложность, секунды таймера
    var onGameStateChanged: ((Int, Int, Int, Int) -> Unit)? = null
    var onGameOverToMenu: (() -> Unit)? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val minSwipeDistance = 50f
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 60)

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed)
            handler.postDelayed(this, 1000)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun startTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, 1000)
    }

    fun stopTimer() {
        handler.removeCallbacks(timerRunnable)
    }

    fun startNewGame(diff: Int) {
        difficulty = diff
        level = 1
        score = 0
        secondsElapsed = 0
        generateLevel()
        invalidate()
        startTimer()
        onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed)
    }

    fun loadGame(savedLevel: Int, savedScore: Int, savedDiff: Int) {
        difficulty = savedDiff
        level = savedLevel
        score = savedScore
        secondsElapsed = 0
        generateLevel()
        invalidate()
        startTimer()
        onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed)
    }

    private fun generateLevel() {
        val baseCols = when (difficulty) {
            1 -> 7
            3 -> 15
            else -> 11
        }
        val baseRows = when (difficulty) {
            1 -> 9
            3 -> 21
            else -> 15
        }

        cols = baseCols + ((level - 1) * 2).coerceAtMost(if (difficulty == 3) 20 else 10)
        rows = baseRows + ((level - 1) * 2).coerceAtMost(if (difficulty == 3) 30 else 14)
        if (cols % 2 == 0) cols++
        if (rows % 2 == 0) rows++

        maze = Array(rows) { IntArray(cols) { 1 } }

        val stack = Stack<Pair<Int, Int>>()
        val startX = 1
        val startY = 1
        maze[startY][startX] = 0
        stack.push(Pair(startX, startY))

        val directions = listOf(Pair(0, -2), Pair(0, 2), Pair(-2, 0), Pair(2, 0))

        while (stack.isNotEmpty()) {
            val (cx, cy) = stack.peek()
            val neighbors = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()

            for (d in directions) {
                val nx = cx + d.first
                val ny = cy + d.second
                if (nx in 1 until cols - 1 && ny in 1 until rows - 1 && maze[ny][nx] == 1) {
                    neighbors.add(Pair(Pair(nx, ny), Pair(cx + d.first / 2, cy + d.second / 2)))
                }
            }

            if (neighbors.isNotEmpty()) {
                val (next, wall) = neighbors.random()
                maze[wall.second][wall.first] = 0
                maze[next.second][next.first] = 0
                stack.push(next)
            } else {
                stack.pop()
            }
        }

        playerX = 1
        playerY = 1
        exitX = cols - 2
        exitY = rows - 2
        maze[exitY][exitX] = 0
    }

    fun movePlayer(dx: Int, dy: Int) {
        val newX = playerX + dx
        val newY = playerY + dy
        if (newY in 0 until rows && newX in 0 until cols && maze[newY][newX] == 0) {
            playerX = newX
            playerY = newY
            score += 10 * difficulty

            if (soundEnabled) {
                try {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
                } catch (e: Exception) {}
            }

            if (playerX == exitX && playerY == exitY) {
                // Бонус за скорость: чем меньше секунд потрачено, тем больше бонус
                val speedBonus = max(10, (300 - secondsElapsed * 3)) * difficulty
                score += (100 * level * difficulty) + speedBonus

                if (soundEnabled) {
                    try {
                        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                    } catch (e: Exception) {}
                }
                level++
                secondsElapsed = 0 // сбрасываем таймер для нового уровня
                generateLevel()
            }
            onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                if (abs(dx) > minSwipeDistance || abs(dy) > minSwipeDistance) {
                    if (abs(dx) > abs(dy)) {
                        if (dx > 0) movePlayer(1, 0) else movePlayer(-1, 0)
                    } else {
                        if (dy > 0) movePlayer(0, 1) else movePlayer(0, -1)
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::maze.isInitialized) return

        val cellWidth = width.toFloat() / cols
        val cellHeight = height.toFloat() / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val paint = if (maze[r][c] == 1) wallPaint else pathPaint
                canvas.drawRect(c * cellWidth, r * cellHeight, (c + 1) * cellWidth, (r + 1) * cellHeight, paint)
            }
        }

        canvas.drawCircle(
            (exitX + 0.5f) * cellWidth,
            (exitY + 0.5f) * cellHeight,
            cellWidth.coerceAtMost(cellHeight) * 0.35f,
            exitPaint
        )

        canvas.drawCircle(
            (playerX + 0.5f) * cellWidth,
            (playerY + 0.5f) * cellHeight,
            cellWidth.coerceAtMost(cellHeight) * 0.4f,
            playerPaint
        )
    }
}
