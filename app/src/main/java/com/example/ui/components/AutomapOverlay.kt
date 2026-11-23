package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.game.AutomapSnapshot
import com.example.game.ProceduralTextures
import com.example.game.packCell
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.DoomGold
import com.example.ui.theme.KeyBlue
import com.example.ui.theme.KeyRed
import com.example.ui.theme.KeyYellow
import com.example.ui.theme.NukageGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBorder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AutomapView(
    automap: AutomapSnapshot,
    showEnemies: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(ObsidianVoid.copy(alpha = 0.86f), RoundedCornerShape(6.dp))
            .border(1.5.dp, SteelBorder, RoundedCornerShape(6.dp))
            .padding(6.dp)
            .semantics { contentDescription = "Bölüm haritası" }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cellSize = min(
                size.width / automap.gridWidth,
                size.height / automap.gridHeight
            )
            val offsetX = (size.width - cellSize * automap.gridWidth) / 2f
            val offsetY = (size.height - cellSize * automap.gridHeight) / 2f

            for (gy in 0 until automap.gridHeight) {
                for (gx in 0 until automap.gridWidth) {
                    val idx = gy * automap.gridWidth + gx
                    if (!automap.explored[idx]) continue
                    val wall = automap.wallCells[idx]
                    if (wall <= 0) continue

                    val color = when (wall) {
                        ProceduralTextures.WALL_EXIT -> Color.White
                        ProceduralTextures.WALL_DOOR -> DoomGold
                        ProceduralTextures.WALL_DOOR_BLUE -> KeyBlue
                        ProceduralTextures.WALL_DOOR_RED -> KeyRed
                        ProceduralTextures.WALL_DOOR_YELLOW -> KeyYellow
                        else -> NukageGreen.copy(alpha = 0.85f)
                    }

                    val openAmt = automap.doorOpenAmounts[packCell(gx, gy)] ?: 0f
                    if (openAmt > 0.7f) {
                        drawRect(
                            color = color.copy(alpha = 0.35f),
                            topLeft = Offset(offsetX + gx * cellSize, offsetY + gy * cellSize),
                            size = Size(cellSize, cellSize),
                            style = Stroke(width = 1.5f)
                        )
                    } else {
                        drawRect(
                            color = color,
                            topLeft = Offset(offsetX + gx * cellSize, offsetY + gy * cellSize),
                            size = Size(cellSize * 0.92f, cellSize * 0.92f)
                        )
                    }
                }
            }

            if (showEnemies) {
                for ((ex, ey) in automap.enemyPositions) {
                    val gx = ex.toInt().coerceIn(0, automap.gridWidth - 1)
                    val gy = ey.toInt().coerceIn(0, automap.gridHeight - 1)
                    if (automap.explored[gy * automap.gridWidth + gx]) {
                        drawCircle(
                            color = BloodCrimson,
                            radius = (cellSize * 0.3f).coerceAtLeast(2.5f),
                            center = Offset(offsetX + ex * cellSize, offsetY + ey * cellSize)
                        )
                    }
                }
            }

            // Player directional arrow
            val px = offsetX + automap.playerX * cellSize
            val py = offsetY + automap.playerY * cellSize
            val dirX = cos(automap.playerAngle)
            val dirY = sin(automap.playerAngle)
            val perpX = -dirY
            val perpY = dirX
            val arrowLen = (cellSize * 0.55f).coerceAtLeast(5f)

            val path = Path().apply {
                moveTo(px + dirX * arrowLen, py + dirY * arrowLen)
                lineTo(px - dirX * arrowLen * 0.6f + perpX * arrowLen * 0.5f, py - dirY * arrowLen * 0.6f + perpY * arrowLen * 0.5f)
                lineTo(px - dirX * arrowLen * 0.6f - perpX * arrowLen * 0.5f, py - dirY * arrowLen * 0.6f - perpY * arrowLen * 0.5f)
                close()
            }
            drawPath(path, color = Color.White)
        }
    }
}
