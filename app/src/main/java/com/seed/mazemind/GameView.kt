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
import java.util.LinkedList
import java.util.Queue
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
    private var hintsLeft = 3

    private lateinit var maze: Array<IntArray>
    private lateinit var coins: Array<BooleanArray>
    private var playerX = 1
    private var playerY = 1
    private var exitX = 1
    private var exitY = 1

    private val wallPaint = Paint()
    private val pathPaint = Paint()
    private val playerPaint = Paint().apply { isAntiAlias = true }
    private val exitPaint = Paint().apply { isAntiAlias = true }
    private val coinPaint = Paint().apply { color = 0xFFFFD700.toInt(); isAntiAlias = true }
    private val hintPaint = Paint().apply { color = 0xFF00FFCC.toInt(); isAntiAlias = true }

    var onGameStateChanged: ((Int, Int, Int, Int, Int) -> Unit)? = null
    var onGameOverToMenu: (() -> Unit)? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val minSwipeDistance = 50f
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 60)

    private var hintPath: List<Pair<Int, Int>> = emptyList()
    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed, hintsLeft)
            handler.postDelayed(this, 1000)
        }
    }

    init {
        setThemeIndex(0)
    }

    fun setThemeIndex(index: Int) {
        when (index) {
            1 -> { // Киберпанк
                wallPaint.color = 0xFF2D006B.toInt()
                pathPaint.color = 0xFF0D001A.toInt()
                playerPaint.color = 0xFFFF007F.toInt()
                exitPaint.color = 0xFF00F0FF.toInt()
            }
            2 -> { // Лес / Природа
                wallPaint.color = 0xFF1E3A1E.toInt()
                pathPaint.color = 0xFF0B1A0B.toInt()
                playerPaint.color = 0xFF81C784.toInt()
                exitPaint.color = 0xFFFFB74D.toInt()
            }
            else -> { // Неон
                wallPaint.color = 0xFF3A506B.toInt()
                pathPaint.color = 0xFF1C2541.toInt()
                playerPaint.color = 0xFF4CC9F0.toInt()
                exitPaint.color = 0xFF4BB543.toInt()
            }
        }
        invalidate()
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
        hintsLeft = 3
        hintPath = emptyList()
        generateLevel()
        invalidate()
        startTimer()
        onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed, hintsLeft)
    }

    fun loadGame(savedLevel: Int, savedScore: Int, savedDiff: Int, savedHints: Int) {
        difficulty = savedDiff
        level = savedLevel
        score = savedScore
        secondsElapsed = 0
        hintsLeft = savedHints
        hintPath = emptyList()
        generateLevel()
        invalidate()
        startTimer()
        onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed, hintsLeft)
    }

    fun showHint() {
        if (!::maze.isInitialized || hintsLeft <= 0) return

        val visited = Array(rows) { BooleanArray(cols) }
        val parent = HashMap<Pair<Int, Int>, Pair<Int, Int>>()
        val queue: Queue<Pair<Int, Int>> = LinkedList()

        val start = Pair(playerX, playerY)
        val end = Pair(exitX, exitY)

        queue.add(start)
        visited[start.second][start.first] = true
        var found = false

        val directions = listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))

        while (queue.isNotEmpty()) {
            val current = queue.poll()!!
            if (current == end) {
                found = true
                break
            }

            for (d in directions) {
                val nx = current.first + d.first
                val ny = current.second + d.second
                if (nx in 0 until cols && ny in 0 until rows && maze[ny][nx] == 0 && !visited[ny][nx]) {
                    visited[ny][nx] = true
                    parent[Pair(nx, ny)] = current
                    queue.add(Pair(nx, ny))
                }
            }
        }

        if (found) {
            hintsLeft--
            val path = mutableListOf<Pair<Int, Int>>()
            var curr: Pair<Int, Int>? = end
            while (curr != null && curr != start) {
                path.add(curr)
                curr = parent[curr]
            }
            hintPath = path.reversed()
            onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed, hintsLeft)
            invalidate()

            handler.removeCallbacksAndMessages("HINT")
            handler.postAtTime({
                hintPath = emptyList()
                invalidate()
            }, "HINT", android.os.SystemClock.uptimeMillis() + 2500)
        }
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
        coins = Array(rows) { BooleanArray(cols) }

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

        for (r in 1 until rows - 1) {
            for (c in 1 until cols - 1) {
                if (maze[r][c] == 0 && !(r == 1 && c == 1) && !(r == exitY && c == exitX)) {
                    if ((1..100).random() < 12) {
                        coins[r][c] = true
                    }
                }
            }
        }
    }

    fun movePlayer(dx: Int, dy: Int) {
        val newX = playerX + dx
        val newY = playerY + dy
        if (newY in 0 until rows && newX in 0 until cols && maze[newY][newX] == 0) {
            playerX = newX
            playerY = newY
            score += 5 * difficulty
            hintPath = emptyList()

            if (coins[playerY][playerX]) {
                coins[playerY][playerX] = false
                score += 50 * difficulty
                if (soundEnabled) {
                    try { toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 60) } catch (e: Exception) {}
                }
            } else if (soundEnabled) {
                try { toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 30) } catch (e: Exception) {}
            }

            if (playerX == exitX && playerY == exitY) {
                val speedBonus = max(10, (300 - secondsElapsed * 3)) * difficulty
                score += (100 * level * difficulty) + speedBonus

                if (soundEnabled) {
                    try { toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180) } catch (e: Exception) {}
                }
                level++
                hintsLeft = 3 // <--- Восстанавливаем 3 подсказки при переходе на новый уровень!
                secondsElapsed = 0
                generateLevel()
            }
            onGameStateChanged?.invoke(level, score, difficulty, secondsElapsed, hintsLeft)
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

                if (coins[r][c]) {
                    canvas.drawCircle(
                        (c + 0.5f) * cellWidth,
                        (r + 0.5f) * cellHeight,
                        cellWidth.coerceAtMost(cellHeight) * 0.2f,
                        coinPaint
                    )
                }
            }
        }

        for (p in hintPath) {
            canvas.drawCircle(
                (p.first + 0.5f) * cellWidth,
                (p.second + 0.5f) * cellHeight,
                cellWidth.coerceAtMost(cellHeight) * 0.18f,
                hintPaint
            )
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
