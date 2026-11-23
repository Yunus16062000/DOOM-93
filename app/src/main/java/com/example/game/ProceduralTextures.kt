package com.example.game

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generates crisp 1993-style procedural 64x64 wall textures and 32x32/48x48 sprite matrices.
 */
object ProceduralTextures {
    const val TEX_SIZE = 64
    const val SPRITE_SIZE = 48

    // Wall type IDs stored in map grid
    const val WALL_UAC_TECH = 1
    const val WALL_BROWN_BRICK = 2
    const val WALL_NUKAGE_PIPE = 3
    const val WALL_HELL_FLESH = 4
    const val WALL_DOOR = 5
    const val WALL_DOOR_BLUE = 6
    const val WALL_DOOR_RED = 7
    const val WALL_DOOR_YELLOW = 8
    const val WALL_EXIT = 9

    val wallTextures: Map<Int, IntArray> by lazy {
        mapOf(
            WALL_UAC_TECH to generateUacTechWall(),
            WALL_BROWN_BRICK to generateBrownBrickWall(),
            WALL_NUKAGE_PIPE to generateNukagePipeWall(),
            WALL_HELL_FLESH to generateHellFleshWall(),
            WALL_DOOR to generateDoorTexture(0xFF8C8C94.toInt(), 0xFFD99A26.toInt()),
            WALL_DOOR_BLUE to generateDoorTexture(0xFF425478.toInt(), 0xFF3B82F6.toInt()),
            WALL_DOOR_RED to generateDoorTexture(0xFF6B3030.toInt(), 0xFFEF4444.toInt()),
            WALL_DOOR_YELLOW to generateDoorTexture(0xFF6B6230.toInt(), 0xFFFACC15.toInt()),
            WALL_EXIT to generateExitSwitchWall()
        )
    }

    // Sprite keys: "enemy_<type>_<frame>" where frame 0=idle, 1=attack, 2=dead
    val enemySprites: Map<String, IntArray> by lazy { buildEnemySprites() }
    val pickupSprites: Map<PickupType, IntArray> by lazy { buildPickupSprites() }
    val barrelSprite: IntArray by lazy { buildBarrelSprite() }
    val fireballSprite: IntArray by lazy { buildOrbSprite(0xFFFF6B1A.toInt(), 0xFFFFE066.toInt()) }
    val plasmaSprite: IntArray by lazy { buildOrbSprite(0xFF1CB5FF.toInt(), 0xFFD8F7FF.toInt()) }

    private fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int =
        ((a and 0xFF) shl 24) or ((r.coerceIn(0, 255) and 0xFF) shl 16) or
            ((g.coerceIn(0, 255) and 0xFF) shl 8) or (b.coerceIn(0, 255) and 0xFF)

    private fun generateUacTechWall(): IntArray {
        val px = IntArray(TEX_SIZE * TEX_SIZE)
        val rng = Random(101)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = rng.nextInt(-8, 9)
                var r = 92 + n
                var g = 96 + n
                var b = 104 + n

                // Horizontal seam & top/bottom trim
                if (y < 4 || y >= 60 || y == 31 || y == 32) {
                    r -= 28; g -= 28; b -= 28
                }
                // Vertical panel seams
                if (x % 32 == 0 || x % 32 == 31) {
                    r -= 24; g -= 24; b -= 24
                }
                // Hazard yellow-black stripe near bottom
                if (y in 52..57) {
                    val stripe = ((x + y) / 6) % 2 == 0
                    if (stripe) {
                        r = 188; g = 140; b = 28
                    } else {
                        r = 28; g = 28; b = 30
                    }
                }
                // Rivets in corners
                if ((x % 32 == 5 || x % 32 == 26) && (y == 8 || y == 27 || y == 36 || y == 48)) {
                    r += 34; g += 34; b += 34
                }
                // UAC red indicator light
                if (x in 13..18 && y in 14..18) {
                    r = 215; g = 42; b = 38
                }
                px[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return px
    }

    private fun generateBrownBrickWall(): IntArray {
        val px = IntArray(TEX_SIZE * TEX_SIZE)
        val rng = Random(202)
        for (y in 0 until TEX_SIZE) {
            val row = y / 16
            val xOff = if (row % 2 == 0) 0 else 16
            for (x in 0 until TEX_SIZE) {
                val n = rng.nextInt(-10, 11)
                val bx = (x + xOff) % 32
                val by = y % 16
                var r = 108 + n
                var g = 76 + n
                var b = 54 + n
                if (by == 0 || by == 15 || bx == 0) {
                    r = 44; g = 32; b = 24
                } else if (by == 1 || bx == 1) {
                    r += 18; g += 14; b += 10
                }
                // Grime near bottom
                if (y > 48) {
                    val f = (y - 48) * 2
                    r -= f; g -= f / 2; b -= f
                }
                px[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return px
    }

    private fun generateNukagePipeWall(): IntArray {
        val px = IntArray(TEX_SIZE * TEX_SIZE)
        val rng = Random(303)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = rng.nextInt(-6, 7)
                var r = 64 + n
                var g = 74 + n
                var b = 68 + n
                // Vertical ribbed steel pipes
                val pipe = (x % 16)
                if (pipe in 3..12) {
                    val shade = 18 - abs(pipe - 7) * 4
                    r += shade; g += shade + 6; b += shade
                }
                // Glowing toxic slime band & drips
                val dripLen = 28 + ((x * 7) % 19)
                if (y in 44..63 || (x % 9 == 2 && y in 26..dripLen)) {
                    r = 32 + n; g = 185 + n * 2; b = 42 + n
                    if (y in 46..50) g += 40
                }
                px[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return px
    }

    private fun generateHellFleshWall(): IntArray {
        val px = IntArray(TEX_SIZE * TEX_SIZE)
        val rng = Random(404)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = rng.nextInt(-12, 13)
                var r = 115 + n
                var g = 24 + n / 2
                var b = 22 + n / 2
                // Glowing magma / blood veins
                val vein = abs((x - 32) + ((y * 3) % 17 - 8)) < 3 ||
                    abs((y - 32) + ((x * 5) % 15 - 7)) < 2
                if (vein) {
                    r = 215; g = 68; b = 20
                }
                // Dark obsidian border stones
                if (x < 4 || x > 59 || y < 4 || y > 59) {
                    r = 38; g = 24; b = 28
                }
                px[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return px
    }

    private fun generateDoorTexture(baseColor: Int, accentColor: Int): IntArray {
        val px = IntArray(TEX_SIZE * TEX_SIZE)
        val br = (baseColor ushr 16) and 0xFF
        val bg = (baseColor ushr 8) and 0xFF
        val bb = baseColor and 0xFF
        val ar = (accentColor ushr 16) and 0xFF
        val ag = (accentColor ushr 8) and 0xFF
        val ab = accentColor and 0xFF
        val rng = Random(505)

        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = rng.nextInt(-6, 7)
                var r = br + n
                var g = bg + n
                var b = bb + n

                // Heavy steel frame
                if (x < 6 || x >= 58 || y < 5 || y >= 59) {
                    r = 46; g = 46; b = 52
                }
                // Key color lock bars on left & right frame
                if ((x in 6..10 || x in 53..57) && y in 10..53) {
                    r = ar; g = ag; b = ab
                }
                // Center vertical seam & lock emblem
                if (x in 30..33) {
                    r -= 36; g -= 36; b -= 36
                }
                if (x in 24..39 && y in 26..37) {
                    r = ar; g = ag; b = ab
                    if (x in 27..36 && y in 29..34) {
                        r = 25; g = 25; b = 28
                    }
                }
                // Horizontal reinforcement ribs
                if (y == 16 || y == 47) {
                    r -= 24; g -= 24; b -= 24
                }
                px[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return px
    }

    private fun generateExitSwitchWall(): IntArray {
        val px = generateUacTechWall()
        for (y in 12..44) {
            for (x in 18..45) {
                px[y * TEX_SIZE + x] = argb(34, 34, 38)
            }
        }
        // Red "EXIT" header bar
        for (y in 14..21) {
            for (x in 21..42) {
                px[y * TEX_SIZE + x] = argb(220, 32, 32)
            }
        }
        // Glowing lever / green switch button
        for (y in 26..39) {
            for (x in 25..38) {
                px[y * TEX_SIZE + x] = if (y in 29..36 && x in 28..35) {
                    argb(72, 245, 84)
                } else {
                    argb(120, 120, 130)
                }
            }
        }
        return px
    }

    private fun buildEnemySprites(): Map<String, IntArray> {
        val map = mutableMapOf<String, IntArray>()
        EnemyType.entries.forEach { type ->
            map["${type.name}_0"] = drawEnemyFrame(type, attack = false, dead = false)
            map["${type.name}_1"] = drawEnemyFrame(type, attack = true, dead = false)
            map["${type.name}_2"] = drawEnemyFrame(type, attack = false, dead = true)
        }
        return map
    }

    private fun drawEnemyFrame(type: EnemyType, attack: Boolean, dead: Boolean): IntArray {
        val px = IntArray(SPRITE_SIZE * SPRITE_SIZE)
        fun plot(x: Int, y: Int, color: Int) {
            if (x in 0 until SPRITE_SIZE && y in 0 until SPRITE_SIZE) {
                px[y * SPRITE_SIZE + x] = color
            }
        }

        fun rect(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
            for (y in y0..y1) for (x in x0..x1) plot(x, y, color)
        }

        fun circle(cx: Int, cy: Int, rad: Int, color: Int) {
            for (y in (cy - rad)..(cy + rad)) {
                for (x in (cx - rad)..(cx + rad)) {
                    if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= rad * rad) plot(x, y, color)
                }
            }
        }

        if (dead) {
            // Collapsed crimson/ash corpse puddle near ground
            val bodyCol = when (type) {
                EnemyType.CACODEMON -> 0xFF9C1B1B.toInt()
                EnemyType.PINKY -> 0xFFAA4A64.toInt()
                else -> 0xFF7D1616.toInt()
            }
            rect(10, 38, 37, 45, 0xFF520808.toInt())
            rect(13, 35, 34, 42, bodyCol)
            rect(17, 36, 22, 39, 0xFFB8A89A.toInt())
            return px
        }

        val skin: Int
        val armor: Int
        val eye = if (attack) 0xFFFFFF44.toInt() else 0xFFFF2222.toInt()

        when (type) {
            EnemyType.ZOMBIEMAN -> {
                skin = 0xFF9E8A72.toInt(); armor = 0xFF4F7A44.toInt()
            }
            EnemyType.SHOTGUNNER -> {
                skin = 0xFF968070.toInt(); armor = 0xFF8C2424.toInt()
            }
            EnemyType.IMP -> {
                skin = 0xFF8C4828.toInt(); armor = 0xFF6B321C.toInt()
            }
            EnemyType.PINKY -> {
                skin = 0xFFD46A84.toInt(); armor = 0xFFAA445C.toInt()
            }
            EnemyType.CACODEMON -> {
                // Floating spiked red sphere with one massive green eye
                circle(24, 21, 15, 0xFFB81C1C.toInt())
                circle(24, 21, 12, 0xFFD92B2B.toInt())
                // Horns
                rect(12, 5, 16, 11, 0xFF423A36.toInt())
                rect(32, 5, 36, 11, 0xFF423A36.toInt())
                // Cyclops Eye
                circle(24, 18, 5, 0xFF191919.toInt())
                circle(24, 18, 4, if (attack) 0xFF7BFF4D.toInt() else 0xFF33D433.toInt())
                // Toothy grin / fireball mouth
                rect(15, 25, 33, 30, 0xFF1A0505.toInt())
                for (tx in 16..32 step 3) rect(tx, 25, tx + 1, 27, 0xFFEDE8DC.toInt())
                if (attack) circle(24, 28, 3, 0xFF33E6FF.toInt())
                return px
            }
            EnemyType.BARON -> {
                skin = 0xFFB83D32.toInt(); armor = 0xFF734C38.toInt()
            }
        }

        // Humanoid / Demon silhouette
        val isLarge = type == EnemyType.BARON || type == EnemyType.PINKY
        val wBonus = if (isLarge) 3 else 0

        // Legs
        rect(16 - wBonus, 30, 21, 45, armor)
        rect(27, 30, 32 + wBonus, 45, armor)
        rect(15 - wBonus, 42, 22, 46, 0xFF2B2220.toInt())
        rect(26, 42, 33 + wBonus, 46, 0xFF2B2220.toInt())

        // Torso
        rect(14 - wBonus, 16, 34 + wBonus, 31, armor)
        rect(17, 18, 31, 28, skin)

        // Head + Horns for demons
        rect(18, 7, 30, 17, skin)
        if (type == EnemyType.IMP || type == EnemyType.BARON || type == EnemyType.PINKY) {
            rect(15, 4, 18, 9, 0xFFE3D5B8.toInt())
            rect(30, 4, 33, 9, 0xFFE3D5B8.toInt())
        }
        // Glowing Eyes
        rect(20, 10, 22, 12, eye)
        rect(26, 10, 28, 12, eye)

        // Arms & Weapon / Fireball
        if (attack) {
            rect(9 - wBonus, 17, 14 - wBonus, 26, skin)
            rect(34 + wBonus, 17, 40 + wBonus, 24, skin)
            if (type == EnemyType.ZOMBIEMAN || type == EnemyType.SHOTGUNNER) {
                rect(22, 20, 42, 24, 0xFF38383E.toInt())
                circle(42, 22, 3, 0xFFFFD633.toInt())
            } else if (type == EnemyType.IMP) {
                circle(39, 21, 4, 0xFFFF7A1A.toInt())
            } else if (type == EnemyType.BARON) {
                circle(39, 21, 5, 0xFF42FF55.toInt())
            }
        } else {
            rect(10 - wBonus, 17, 14 - wBonus, 29, skin)
            rect(34 + wBonus, 17, 38 + wBonus, 29, skin)
            if (type == EnemyType.ZOMBIEMAN || type == EnemyType.SHOTGUNNER) {
                rect(24, 22, 39, 25, 0xFF2D2D32.toInt())
            }
        }
        return px
    }

    private fun buildBarrelSprite(): IntArray {
        val px = IntArray(SPRITE_SIZE * SPRITE_SIZE)
        for (y in 18..45) {
            for (x in 15..32) {
                val edge = x == 15 || x == 32 || y == 18 || y == 45
                val rim = y == 19 || y == 20 || y == 43 || y == 44 || y == 31
                val slime = y in 21..24 && x in 20..27
                px[y * SPRITE_SIZE + x] = when {
                    edge -> 0xFF222822.toInt()
                    rim -> 0xFF6B736B.toInt()
                    slime -> 0xFF52F542.toInt()
                    else -> if (x < 22) 0xFF3B7A3B.toInt() else 0xFF2A582A.toInt()
                }
            }
        }
        return px
    }

    private fun buildPickupSprites(): Map<PickupType, IntArray> {
        val map = mutableMapOf<PickupType, IntArray>()
        PickupType.entries.forEach { type ->
            val px = IntArray(SPRITE_SIZE * SPRITE_SIZE)
            fun rect(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
                for (y in y0..y1) for (x in x0..x1) {
                    if (x in 0 until SPRITE_SIZE && y in 0 until SPRITE_SIZE) {
                        px[y * SPRITE_SIZE + x] = color
                    }
                }
            }
            when (type) {
                PickupType.STIMPACK, PickupType.MEDIKIT -> {
                    val pad = if (type == PickupType.MEDIKIT) 0 else 3
                    rect(13 + pad, 27 + pad, 34 - pad, 44, 0xFFEAE6E1.toInt())
                    rect(21, 31 + pad / 2, 26, 41 - pad / 2, 0xFFD92626.toInt())
                    rect(17 + pad / 2, 34 + pad / 2, 30 - pad / 2, 38 - pad / 2, 0xFFD92626.toInt())
                }
                PickupType.GREEN_ARMOR, PickupType.MEGA_ARMOR -> {
                    val col = if (type == PickupType.GREEN_ARMOR) 0xFF34A853.toInt() else 0xFF2563EB.toInt()
                    rect(13, 24, 34, 43, col)
                    rect(17, 24, 30, 29, 0xFF1E1E24.toInt())
                    rect(20, 31, 27, 39, 0xFFE2E8F0.toInt())
                }
                PickupType.AMMO_CLIP -> {
                    rect(17, 29, 30, 44, 0xFF52525B.toInt())
                    rect(19, 25, 28, 30, 0xFFF59E0B.toInt())
                }
                PickupType.SHELLS -> {
                    rect(14, 32, 33, 44, 0xFF991B1B.toInt())
                    rect(16, 29, 20, 33, 0xFFFBBF24.toInt())
                    rect(22, 29, 26, 33, 0xFFFBBF24.toInt())
                    rect(28, 29, 32, 33, 0xFFFBBF24.toInt())
                }
                PickupType.CELLS -> {
                    rect(15, 27, 32, 44, 0xFF1E3A5F.toInt())
                    rect(18, 24, 29, 41, 0xFF33D4FF.toInt())
                }
                PickupType.SHOTGUN_WEAPON -> {
                    rect(8, 32, 40, 36, 0xFF3F3F46.toInt())
                    rect(10, 36, 18, 41, 0xFF78350F.toInt())
                    rect(22, 36, 32, 39, 0xFF92400E.toInt())
                }
                PickupType.CHAINGUN_WEAPON -> {
                    rect(10, 28, 38, 40, 0xFF52525B.toInt())
                    rect(24, 30, 42, 38, 0xFF94A3B8.toInt())
                    rect(14, 35, 24, 43, 0xFFF59E0B.toInt())
                }
                PickupType.PLASMA_WEAPON -> {
                    rect(10, 29, 39, 40, 0xFF334155.toInt())
                    rect(16, 31, 35, 37, 0xFF38BDF8.toInt())
                }
                PickupType.BLUE_KEY, PickupType.RED_KEY, PickupType.YELLOW_KEY -> {
                    val col = when (type) {
                        PickupType.BLUE_KEY -> 0xFF3B82F6.toInt()
                        PickupType.RED_KEY -> 0xFFEF4444.toInt()
                        else -> 0xFFFACC15.toInt()
                    }
                    rect(15, 26, 32, 43, col)
                    rect(17, 28, 30, 34, 0xFF111827.toInt())
                    rect(20, 37, 27, 40, 0xFFFFFFFF.toInt())
                }
            }
            map[type] = px
        }
        return map
    }

    private fun buildOrbSprite(outerColor: Int, coreColor: Int): IntArray {
        val px = IntArray(SPRITE_SIZE * SPRITE_SIZE)
        val cx = SPRITE_SIZE / 2
        val cy = SPRITE_SIZE / 2
        for (y in 0 until SPRITE_SIZE) {
            for (x in 0 until SPRITE_SIZE) {
                val dx = (x - cx).toFloat()
                val dy = (y - cy).toFloat()
                val d = sqrt(dx * dx + dy * dy)
                if (d < 6f) {
                    px[y * SPRITE_SIZE + x] = coreColor
                } else if (d < 11f) {
                    px[y * SPRITE_SIZE + x] = outerColor
                }
            }
        }
        return px
    }
}
