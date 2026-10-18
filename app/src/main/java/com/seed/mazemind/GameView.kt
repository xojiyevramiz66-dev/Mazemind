package com.seed.mazemind

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Stack
import kotlin.math.abs

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var cols = 11
    private var rows = 15
    private var level = 1
    private var score = 0

    private lateinit var maze: Array<IntArray> // 1 - стена, 0 - проход
    private var playerX = 1
    private var playerY = 1
    private var exitX = 1
    private var exitY = 1

    private val wallPaint = Paint().apply { color = 0xFF3A506B.toInt() }
    private val pathPaint = Paint().apply { color = 0xFF1C2541.toInt() }
    private val playerPaint = Paint().apply { color = 0xFF4CC9F0.toInt(); isAntialias = true }
    private val exitPaint = Paint().apply { color = 0xFF4BB543.toInt(); isAntialias = true }

    var onGameStateChanged: ((Int, Int) -> Unit)? = null
    var onGameOverToMenu: (() -> Unit)? = null

    private var touchX = 0f
    private var touchY = 0f

    fun startNewGame() {
        level = 1
        score = 0
        generateLevel()
        invalidate()
        onGameStateChanged?.invoke(level, score)
    }

    private fun generateLevel() {
        cols = 9 + (level * 2).coerceAtMost(25)
        rows = 11 + (level * 2).coerceAtMost(35)
        if (cols % 2 == 0) cols++
        if (rows % 2 == 0) rows++

        maze = Array(rows) { IntArray(cols) { 1 } }

        // Построение лабиринта методом DFS с возвратом
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
            score += 10

            if (playerX == exitX && playerY == exitY) {
                level++
                score += 100 * level
                generateLevel()
            }
            onGameStateChanged?.invoke(level, score)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchX
                val dy = event.y - touchY
                if (abs(dx) > 50 || abs(dy) > 50) {
                    if (abs(dx) > abs(dy)) {
                        if (dx > 0) movePlayer(1, 0) else movePlayer(-1, 0)
                    } else {
                        if (dy > 0) movePlayer(0, 1) else movePlayer(0, -1)
                    }
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

        // Выход (Зелёный круг)
        canvas.drawCircle(
            (exitX + 0.5f) * cellWidth,
            (exitY + 0.5f) * cellHeight,
            cellWidth.coerceAtMost(cellHeight) * 0.35f,
            exitPaint
        )

        // Игрок (Голубой круг)
        canvas.drawCircle(
            (playerX + 0.5f) * cellWidth,
            (playerY + 0.5f) * cellHeight,
            cellWidth.coerceAtMost(cellHeight) * 0.4f,
            playerPaint
        )
    }
}
