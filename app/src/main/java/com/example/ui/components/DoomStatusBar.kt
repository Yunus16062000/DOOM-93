package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.HudSnapshot
import com.example.game.KeyColor
import com.example.game.WeaponType
import com.example.ui.theme.AshText
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.BunkerSlate
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.DoomGold
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KeyBlue
import com.example.ui.theme.KeyRed
import com.example.ui.theme.KeyYellow
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NukageGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlasmaCyan
import com.example.ui.theme.SteelBorder

@Composable
fun DoomStatusBar(
    hud: HudSnapshot,
    onSelectWeapon: (WeaponType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(GunmetalElevated, GunmetalSurface, BunkerSlate)
                )
            )
            .border(1.5.dp, SteelBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Weapon strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WeaponType.entries.forEach { weapon ->
                val owned = hud.ownedWeapons.contains(weapon)
                val active = hud.activeWeapon == weapon
                val bgColor = when {
                    active -> BloodCrimson
                    owned -> ObsidianVoid.copy(alpha = 0.75f)
                    else -> ObsidianVoid.copy(alpha = 0.35f)
                }
                val borderColor = if (active) DoomGold else if (owned) SteelBorder else SteelBorder.copy(alpha = 0.4f)
                val textColor = when {
                    active -> ChalkWhite
                    owned -> AshText
                    else -> MutedSteel.copy(alpha = 0.4f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(3.dp))
                        .clickable(enabled = owned) { onSelectWeapon(weapon) }
                        .testTag("weapon_slot_${weapon.slot}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${weapon.slot}:${weapon.titleTr}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Classic Doomguy status bar row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HudCounterBox(
                label = "CEPHANE",
                value = if (hud.currentAmmo < 0) "∞" else hud.currentAmmo.toString(),
                valueColor = if (hud.currentAmmo in 0..5) BloodCrimson else DoomGold,
                modifier = Modifier.weight(1f)
            )

            HudCounterBox(
                label = "SAĞLIK",
                value = "${hud.health}%",
                valueColor = when {
                    hud.health <= 25 -> BloodCrimson
                    hud.health <= 60 -> DoomGold
                    else -> ChalkWhite
                },
                modifier = Modifier.weight(1f)
            )

            // Animated Marine Portrait
            DoomguyPortrait(
                health = hud.health,
                mood = hud.faceMood,
                modifier = Modifier
                    .size(52.dp)
                    .border(2.dp, SteelBorder, RoundedCornerShape(3.dp))
                    .background(ObsidianVoid, RoundedCornerShape(3.dp))
                    .semantics { contentDescription = "Denizci portresi, sağlık %${hud.health}" }
            )

            HudCounterBox(
                label = "ZIRH",
                value = "${hud.armor}%",
                valueColor = if (hud.armor > 100) PlasmaCyan else NukageGreen,
                modifier = Modifier.weight(1f)
            )

            // Keycards column
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .background(ObsidianVoid, RoundedCornerShape(3.dp))
                    .border(1.dp, SteelBorder, RoundedCornerShape(3.dp))
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KeyIndicator(
                    active = hud.keys.contains(KeyColor.BLUE),
                    color = KeyBlue,
                    label = "M"
                )
                KeyIndicator(
                    active = hud.keys.contains(KeyColor.YELLOW),
                    color = KeyYellow,
                    label = "S"
                )
                KeyIndicator(
                    active = hud.keys.contains(KeyColor.RED),
                    color = KeyRed,
                    label = "K"
                )
            }
        }
    }
}

@Composable
private fun HudCounterBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(ObsidianVoid, RoundedCornerShape(3.dp))
            .border(1.dp, SteelBorder, RoundedCornerShape(3.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = valueColor,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = AshText,
            maxLines = 1
        )
    }
}

@Composable
private fun KeyIndicator(active: Boolean, color: Color, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(11.dp)
            .background(
                if (active) color else color.copy(alpha = 0.14f),
                RoundedCornerShape(2.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) ObsidianVoid else MutedSteel
        )
    }
}

@Composable
fun DoomguyPortrait(
    health: Int,
    mood: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val px = w / 16f
        val py = h / 16f

        fun block(gx: Int, gy: Int, gw: Int, gh: Int, col: Color) {
            drawRect(
                color = col,
                topLeft = Offset(gx * px, gy * py),
                size = Size(gw * px, gh * py)
            )
        }

        // Background glow behind head
        val bg = if (mood == 4 || health <= 20) Color(0xFF330A0A) else Color(0xFF1A171B)
        drawRect(color = bg)

        if (mood == 5 || health <= 0) {
            // Skull face when dead
            block(4, 2, 8, 9, Color(0xFFD6CEC8))
            block(5, 11, 6, 3, Color(0xFFD6CEC8))
            block(5, 5, 2, 2, Color(0xFF120606))
            block(9, 5, 2, 2, Color(0xFF120606))
            block(7, 8, 2, 2, Color(0xFF120606))
            block(6, 12, 1, 2, Color(0xFF120606))
            block(9, 12, 1, 2, Color(0xFF120606))
            return@Canvas
        }

        // Marine armor collar + neck
        block(3, 13, 10, 3, Color(0xFF3F6A3B))
        block(6, 12, 4, 2, Color(0xFFC29072))

        // Hair (classic flat-top brown)
        block(4, 2, 8, 3, Color(0xFF5A3924))
        block(3, 3, 1, 3, Color(0xFF472B1A))
        block(12, 3, 1, 3, Color(0xFF472B1A))

        // Face skin
        val skin = if (mood == 4) Color(0xFFE07A68) else Color(0xFFD4A182)
        block(4, 4, 8, 8, skin)
        block(5, 12, 6, 1, skin)

        // Progressive damage bruises & blood
        if (health < 75) {
            block(5, 5, 2, 1, Color(0xFFA63A2E))
        }
        if (health < 50) {
            block(9, 8, 2, 2, Color(0xFFB81D1D))
            block(4, 10, 2, 2, Color(0xFF8F1414))
        }
        if (health < 25) {
            block(6, 4, 2, 3, Color(0xFFD91E1E))
            block(10, 6, 2, 4, Color(0xFFD91E1E))
        }

        // Eyes (look left/right/center or wince)
        val eyeShift = when (mood) {
            1 -> -1
            2 -> 1
            else -> 0
        }
        // Eyebrows
        block(5, 6, 2, 1, Color(0xFF472B1A))
        block(9, 6, 2, 1, Color(0xFF472B1A))

        if (mood == 4) {
            // Pain wince eyes
            block(5, 7, 2, 1, Color(0xFF1A0D0A))
            block(9, 7, 2, 1, Color(0xFF1A0D0A))
        } else {
            block(5, 7, 2, 1, Color.White)
            block(9, 7, 2, 1, Color.White)
            block((5 + eyeShift).coerceIn(4, 6), 7, 1, 1, Color(0xFF1E3A5F))
            block((9 + eyeShift).coerceIn(8, 10), 7, 1, 1, Color(0xFF1E3A5F))
        }

        // Nose
        block(7, 8, 2, 2, Color(0xFFB37D60))

        // Mouth (grin on weapon pickup, grimace on pain, tough line normally)
        when (mood) {
            3 -> {
                block(5, 10, 6, 2, Color(0xFFF5F0EA))
            }
            4 -> {
                block(6, 10, 4, 2, Color(0xFF4A0E0E))
            }
            else -> {
                block(6, 11, 4, 1, Color(0xFF6B3E2E))
            }
        }
    }
}
