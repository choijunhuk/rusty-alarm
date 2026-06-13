package com.example.rustyalarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private const val COLS = 10
private const val ROWS = 16

/**
 * 1-line Tetris. The first time the player clears any number of rows the
 * [onLineCleared] callback fires; the parent screen turns that into "dismiss
 * the alarm".
 *
 * Controls:
 *  ← / → : move left / right
 *  ⟳ : rotate
 *  ↓ : soft drop (1 cell). Hold cycle handled at parent level via repeated taps.
 */
@Composable
fun TetrisChallenge(
    onLineCleared: () -> Unit,
) {
    var board by remember { mutableStateOf(IntArray(COLS * ROWS)) }
    var piece by remember { mutableStateOf(spawnPiece()) }
    var pos by remember { mutableStateOf(((COLS / 2 - 2)) to 0) }
    var gameOver by remember { mutableStateOf(false) }
    var totalLines by remember { mutableIntStateOf(0) }

    // Auto-drop tick (gentler — 800ms per cell)
    LaunchedEffect(gameOver) {
        while (!gameOver) {
            delay(800)
            val (c, r) = pos
            if (canPlace(board, piece, c, r + 1)) {
                pos = c to (r + 1)
            } else {
                val merged = merge(board, piece, c, r)
                val (cleared, newBoard) = clearLines(merged)
                if (cleared > 0) {
                    totalLines += cleared
                    onLineCleared()
                    return@LaunchedEffect
                }
                board = newBoard
                piece = spawnPiece()
                pos = (COLS / 2 - 2) to 0
                if (!canPlace(board, piece, pos.first, pos.second)) {
                    gameOver = true
                }
            }
        }
    }

    val (pCol, pRow) = pos

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "한 줄 만들면 알람이 꺼져요",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "← → 이동 · ⟳ 회전 · ↓ 한 칸 내리기",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        // ── Game board ──
        val boardBg = MaterialTheme.colorScheme.background
        val boardEdge = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(COLS.toFloat() / ROWS)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            boardBg.copy(alpha = 0.85f),
                            boardBg,
                        )
                    )
                )
                .border(1.dp, boardEdge.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width / COLS
                val ch = size.height / ROWS

                // Grid lines (subtle)
                val gridColor = boardEdge.copy(alpha = 0.08f)
                for (c in 1 until COLS) {
                    drawLine(
                        gridColor,
                        Offset(c * cw, 0f), Offset(c * cw, size.height),
                        strokeWidth = 1f,
                    )
                }
                for (r in 1 until ROWS) {
                    drawLine(
                        gridColor,
                        Offset(0f, r * ch), Offset(size.width, r * ch),
                        strokeWidth = 1f,
                    )
                }

                // Locked board
                for (r in 0 until ROWS) {
                    for (c in 0 until COLS) {
                        val v = board[r * COLS + c]
                        if (v != 0) drawCell(c, r, cw, ch, COLORS[v])
                    }
                }

                // Current piece
                for ((dc, dr) in piece) {
                    val c = pCol + dc
                    val r = pRow + dr
                    if (r in 0 until ROWS && c in 0 until COLS) {
                        drawCell(c, r, cw, ch, COLORS[piece.colorIndex])
                    }
                }
            }
        }

        // ── Controls ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CtrlButton(Icons.Default.ChevronLeft, "왼쪽") {
                val (c, r) = pos
                if (canPlace(board, piece, c - 1, r)) pos = (c - 1) to r
            }
            CtrlButton(Icons.Default.Refresh, "회전") {
                val (c, r) = pos
                val rot = piece.rotated()
                val origCx = piece.cells.map { it.first }.average()
                val origCy = piece.cells.map { it.second }.average()
                val newCx = rot.cells.map { it.first }.average()
                val newCy = rot.cells.map { it.second }.average()
                val nc = c + (origCx - newCx).roundToInt()
                val nr = r + (origCy - newCy).roundToInt()
                if (canPlace(board, rot, nc, nr)) {
                    piece = rot
                    pos = nc to nr
                }
            }
            CtrlButton(Icons.Default.ChevronRight, "오른쪽") {
                val (c, r) = pos
                if (canPlace(board, piece, c + 1, r)) pos = (c + 1) to r
            }
            CtrlButton(Icons.Default.ArrowDownward, "내리기", primary = true) {
                val (c, r) = pos
                if (canPlace(board, piece, c, r + 1)) pos = c to (r + 1)
            }
        }

        if (gameOver) {
            Text(
                "게임 오버 — 화면을 다시 열어 재시작하세요",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (totalLines == 0) {
            Text(
                "줄을 채우면 사라져요 — 한 줄만 해도 통과!",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun CtrlButton(
    icon: ImageVector,
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (primary) {
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            }
        } else {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
            }
        }
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCell(
    c: Int, r: Int, cw: Float, ch: Float, color: Color,
) {
    drawRect(
        color = color,
        topLeft = Offset(c * cw + 1.5f, r * ch + 1.5f),
        size = Size(cw - 3f, ch - 3f),
    )
}

// ── Tetromino model ──────────────────────────────────────

private data class Piece(
    val cells: List<Pair<Int, Int>>,
    val colorIndex: Int,
) : Iterable<Pair<Int, Int>> by cells {
    fun rotated(): Piece {
        val r = cells.map { (x, y) -> -y to x }
        val minX = r.minOf { it.first }
        val minY = r.minOf { it.second }
        return Piece(r.map { (x, y) -> (x - minX) to (y - minY) }, colorIndex)
    }
}

private val SHAPES = listOf(
    listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),   // I
    listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),   // O
    listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),   // T
    listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),   // S
    listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),   // Z
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2),   // L
    listOf(1 to 0, 1 to 1, 0 to 2, 1 to 2),   // J
)

private val COLORS = listOf(
    Color.Transparent,                 // 0 = empty
    Color(0xFF00BCD4),                 // I cyan
    Color(0xFFFFEB3B),                 // O yellow
    Color(0xFF9C27B0),                 // T purple
    Color(0xFF4CAF50),                 // S green
    Color(0xFFF44336),                 // Z red
    Color(0xFFFF9800),                 // L orange
    Color(0xFF3F51B5),                 // J indigo
)

private fun spawnPiece(): Piece {
    val idx = Random.nextInt(SHAPES.size)
    return Piece(SHAPES[idx], idx + 1)
}

private fun canPlace(board: IntArray, piece: Piece, baseCol: Int, baseRow: Int): Boolean {
    for ((dc, dr) in piece) {
        val c = baseCol + dc
        val r = baseRow + dr
        if (c < 0 || c >= COLS || r >= ROWS) return false
        if (r >= 0 && board[r * COLS + c] != 0) return false
    }
    return true
}

private fun merge(board: IntArray, piece: Piece, baseCol: Int, baseRow: Int): IntArray {
    val out = board.copyOf()
    for ((dc, dr) in piece) {
        val c = baseCol + dc
        val r = baseRow + dr
        if (r in 0 until ROWS && c in 0 until COLS) {
            out[r * COLS + c] = piece.colorIndex
        }
    }
    return out
}

private fun clearLines(board: IntArray): Pair<Int, IntArray> {
    val rows = (0 until ROWS).map { r ->
        IntArray(COLS) { c -> board[r * COLS + c] }
    }
    val kept = rows.filter { row -> row.any { it == 0 } }
    val cleared = ROWS - kept.size
    val padded = List(cleared) { IntArray(COLS) } + kept
    val out = IntArray(COLS * ROWS)
    padded.forEachIndexed { r, row -> row.copyInto(out, r * COLS) }
    return cleared to out
}
