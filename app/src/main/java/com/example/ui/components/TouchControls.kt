package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AshText
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.BloodDark
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.DoomGold
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.HellfireAmber
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBorder
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun VirtualThumbstick(
    onMoveVector: (forward: Float, strafeRight: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 95f

    Box(
        modifier = modifier
            .size(136.dp)
            .clip(CircleShape)
            .background(ObsidianVoid.copy(alpha = 0.55f))
            .border(2.dp, SteelBorder.copy(alpha = 0.85f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    fun updateFromPos(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadiusPx) {
                            Offset(delta.x / dist * maxRadiusPx, delta.y / dist * maxRadiusPx)
                        } else {
                            delta
                        }
                        knobOffset = clamped
                        val strafe = (clamped.x / maxRadiusPx).coerceIn(-1f, 1f)
                        val forward = (-clamped.y / maxRadiusPx).coerceIn(-1f, 1f)
                        onMoveVector(forward, strafe)
                    }
                    updateFromPos(down.position)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            knobOffset = Offset.Zero
                            onMoveVector(0f, 0f)
                            break
                        }
                        change.consume()
                        updateFromPos(change.position)
                    } while (true)
                }
            }
            .testTag("virtual_thumbstick"),
        contentAlignment = Alignment.Center
    ) {
        // Crosshair lines inside stick pad
        Box(
            modifier = Modifier
                .size(96.dp)
                .border(1.dp, SteelBorder.copy(alpha = 0.35f), CircleShape)
        )
        // Thumb knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GunmetalSurface, ObsidianVoid)
                    )
                )
                .border(2.dp, DoomGold.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "HAREKET",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = AshText
            )
        }
    }
}

@Composable
fun LookDragSurface(
    sensitivity: Float,
    onRotateDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(sensitivity) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val radians = dragAmount.x * 0.0055f * sensitivity
                    onRotateDelta(radians)
                }
            }
            .testTag("look_drag_surface")
    )
}

@Composable
fun ActionButtonsPad(
    hasInteractPrompt: Boolean,
    onFireChanged: (Boolean) -> Unit,
    onUsePressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // USE / DOOR / EXIT button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (hasInteractPrompt) HellfireAmber.copy(alpha = 0.9f)
                        else GunmetalSurface.copy(alpha = 0.85f)
                    )
                    .border(
                        2.dp,
                        if (hasInteractPrompt) DoomGold else SteelBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            onUsePressed()
                        }
                    }
                    .testTag("use_action_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DoorFront,
                    contentDescription = "Kapıyı aç veya şalteri kullan",
                    tint = if (hasInteractPrompt) ObsidianVoid else ChalkWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "KULLAN",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = if (hasInteractPrompt) DoomGold else AshText
            )
        }

        // FIRE button (holdable for continuous fire!)
        var isFiring by remember { mutableStateOf(false) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            if (isFiring) listOf(HellfireAmber, BloodCrimson)
                            else listOf(BloodCrimson, BloodDark)
                        )
                    )
                    .border(
                        2.5.dp,
                        if (isFiring) ChalkWhite else DoomGold.copy(alpha = 0.85f),
                        CircleShape
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            isFiring = true
                            onFireChanged(true)
                            do {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id }
                                if (ch == null || !ch.pressed) {
                                    isFiring = false
                                    onFireChanged(false)
                                    break
                                }
                                ch.consume()
                            } while (true)
                        }
                    }
                    .testTag("fire_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Ateş et",
                    tint = ChalkWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "ATEŞ",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = ChalkWhite
            )
        }
    }
}
