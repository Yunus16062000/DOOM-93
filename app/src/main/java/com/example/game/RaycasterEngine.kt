package com.example.game

import android.graphics.Bitmap
import com.example.audio.RetroSoundEngine
import com.example.audio.SfxType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 60 FPS Software 2.5D DDA Raycaster + DOOM 1 gameplay simulation engine.
 */
class RaycasterEngine(
    private val level: LevelDefinition,
    private val difficulty: Difficulty,
    private val soundEngine: RetroSoundEngine,
    private val onVibrate: (Long) -> Unit,
    private val onLevelComplete: (HudSnapshot) -> Unit
) {
    companion object {
        const val RENDER_WIDTH = 320
        const val RENDER_HEIGHT = 200
        private const val PLAYER_RADIUS = 0.24f
        private const val MOVE_SPEED = 4.2f
        private const val FOV_PLANE = 0.66f
    }

    val frameBitmap: Bitmap = Bitmap.createBitmap(RENDER_WIDTH, RENDER_HEIGHT, Bitmap.Config.ARGB_8888)
    private val pixels = IntArray(RENDER_WIDTH * RENDER_HEIGHT)
    private val zBuffer = FloatArray(RENDER_WIDTH)

    // Player state
    private var playerX = level.spawnX
    private var playerY = level.spawnY
    private var playerAngle = level.spawnAngle
    private var health = 100
    private var armor = 0
    private var activeWeapon = WeaponType.PISTOL
    private val ownedWeapons = mutableSetOf(WeaponType.FIST, WeaponType.PISTOL)
    private var bullets = (50 * difficulty.ammoMultiplier).toInt()
    private var shells = 0
    private var cells = 0
    private val ownedKeys = mutableSetOf<KeyColor>()

    // Weapon & visual feedback state
    private var weaponCooldown = 0f
    private var muzzleFlashTimer = 0f
    private var weaponRecoil = 0f
    private var bobPhase = 0f
    private var bobAmount = 0f
    private var damageFlash = 0f
    private var pickupFlash = 0f
    private var screenShake = 0f
    private var nukageDamageTimer = 0f

    // Face animation state
    private var faceLookTimer = 1.5f
    private var faceMood = 0
    private var faceOverrideTimer = 0f

    // Status message
    private var statusMessage = "${level.code}: ${level.titleTr}"
    private var statusMessageTimer = 3.0f

    // Level entities (mutable copies)
    private val doors = level.doors.map { it.copy() }.associateBy { packCell(it.gx, it.gy) }
    private val enemies = level.enemies.map { it.copy() }.toMutableList()
    private val pickups = level.pickups.map { it.copy() }.toMutableList()
    private val barrels = level.barrels.map { it.copy() }.toMutableList()
    private val projectiles = mutableListOf<ProjectileEntity>()
    private val particles = mutableListOf<ParticleEntity>()

    // Explored grid for automap
    private val explored = BooleanArray(level.width * level.height)
    private val discoveredSecrets = mutableSetOf<Long>()
    private val totalKills = enemies.size
    private val totalItems = pickups.count { it.type.isItemCounted }
    private val totalSecrets = level.secretTiles.size.coerceAtLeast(1)
    private var killsCount = 0
    private var itemsCount = 0

    private var elapsedTimeSec = 0f
    private var phase = GamePhase.PLAYING
    private var nextProjectileId = 1
    private var fpsCounter = 60
    private var fpsAccum = 0f
    private var fpsFrames = 0
    private val rng = Random(System.currentTimeMillis())

    // Input state
    var moveForwardInput: Float = 0f
    var strafeRightInput: Float = 0f
    var fireHeld: Boolean = false

    init {
        revealAroundPlayer()
    }

    fun rotatePlayer(deltaRadians: Float) {
        if (phase != GamePhase.PLAYING) return
        playerAngle += deltaRadians
    }

    fun setPaused(paused: Boolean) {
        if (phase == GamePhase.DEAD || phase == GamePhase.LEVEL_COMPLETE) return
        phase = if (paused) GamePhase.PAUSED else GamePhase.PLAYING
    }

    fun selectWeapon(weapon: WeaponType) {
        if (phase != GamePhase.PLAYING) return
        if (ownedWeapons.contains(weapon)) {
            activeWeapon = weapon
            weaponCooldown = 0.12f
            soundEngine.play(SfxType.SWITCH, 1.15f)
        }
    }

    fun triggerUseAction() {
        if (phase != GamePhase.PLAYING) return
        val dirX = cos(playerAngle)
        val dirY = sin(playerAngle)

        // Raycheck up to 1.85 units ahead for Exit switch or Door
        for (step in 1..14) {
            val dist = step * 0.13f
            val tx = (playerX + dirX * dist).toInt()
            val ty = (playerY + dirY * dist).toInt()
            if (tx !in 0 until level.width || ty !in 0 until level.height) break
            val cell = level.grid[ty][tx]
            if (cell == ProceduralTextures.WALL_EXIT) {
                soundEngine.play(SfxType.SWITCH)
                phase = GamePhase.LEVEL_COMPLETE
                val snap = buildHudSnapshot()
                onLevelComplete(snap)
                return
            }
            val door = doors[packCell(tx, ty)]
            if (door != null) {
                tryOpenDoor(door)
                return
            }
            if (cell > 0) break
        }
    }

    private fun tryOpenDoor(door: DoorState) {
        val req = door.requiredKey
        if (req != null && !ownedKeys.contains(req)) {
            val keyName = when (req) {
                KeyColor.BLUE -> "MAVİ KART GEREKLİ!"
                KeyColor.RED -> "KIRMIZI KART GEREKLİ!"
                KeyColor.YELLOW -> "SARI KART GEREKLİ!"
            }
            showMessage(keyName, 2.0f)
            soundEngine.play(SfxType.SWITCH, 0.7f)
            return
        }
        if (!door.opening && door.openAmount < 0.9f) {
            door.opening = true
            door.holdTimer = 4.5f
            soundEngine.play(SfxType.DOOR)
        }
    }

    fun step(dtRaw: Float): HudSnapshot {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)

        fpsAccum += dt
        fpsFrames++
        if (fpsAccum >= 0.5f) {
            fpsCounter = (fpsFrames / fpsAccum).toInt().coerceIn(1, 120)
            fpsAccum = 0f
            fpsFrames = 0
        }

        if (phase == GamePhase.PLAYING) {
            elapsedTimeSec += dt
            updatePlayer(dt)
            updateDoors(dt)
            updateWeapons(dt)
            updateProjectiles(dt)
            updateEnemies(dt)
            updateParticles(dt)
            updateTimers(dt)
            revealAroundPlayer()
        }

        renderFrame()
        return buildHudSnapshot()
    }

    private fun updatePlayer(dt: Float) {
        val cosA = cos(playerAngle)
        val sinA = sin(playerAngle)
        val rightX = -sinA
        val rightY = cosA

        var vx = cosA * moveForwardInput + rightX * strafeRightInput
        var vy = sinA * moveForwardInput + rightY * strafeRightInput
        val mag = sqrt(vx * vx + vy * vy)
        if (mag > 1f) {
            vx /= mag
            vy /= mag
        }

        if (mag > 0.05f) {
            bobPhase += dt * 11f
            bobAmount = (bobAmount + dt * 5f).coerceAtMost(1f)
            val dx = vx * MOVE_SPEED * dt
            val dy = vy * MOVE_SPEED * dt
            moveWithCollision(dx, dy)
        } else {
            bobAmount = (bobAmount - dt * 5f).coerceAtLeast(0f)
        }

        // Check if standing on a secret tile
        val cellKey = packCell(playerX.toInt(), playerY.toInt())
        if (level.secretTiles.contains(cellKey) && discoveredSecrets.add(cellKey)) {
            showMessage("GİZLİ BÖLGE BULUNDU!", 2.5f)
            soundEngine.play(SfxType.KEYCARD, 1.1f)
        }

        // Toxic Nukage damage
        if (level.nukageTiles.contains(cellKey)) {
            nukageDamageTimer += dt
            if (nukageDamageTimer >= 0.65f) {
                nukageDamageTimer = 0f
                applyPlayerDamage(5, isHazard = true)
            }
        } else {
            nukageDamageTimer = 0.35f
        }

        // Check item pickups
        for (pickup in pickups) {
            if (pickup.collected) continue
            val dx = pickup.x - playerX
            val dy = pickup.y - playerY
            if (dx * dx + dy * dy < 0.42f) {
                collectPickup(pickup)
            }
        }
    }

    private fun moveWithCollision(dx: Float, dy: Float) {
        val newX = playerX + dx
        if (!isBlockedForPlayer(newX, playerY, PLAYER_RADIUS)) {
            playerX = newX
        } else {
            autoBumpDoor(newX, playerY)
        }

        val newY = playerY + dy
        if (!isBlockedForPlayer(playerX, newY, PLAYER_RADIUS)) {
            playerY = newY
        } else {
            autoBumpDoor(playerX, newY)
        }
    }

    private fun autoBumpDoor(targetX: Float, targetY: Float) {
        val gx = targetX.toInt()
        val gy = targetY.toInt()
        val door = doors[packCell(gx, gy)]
        if (door != null && door.requiredKey == null && !door.opening) {
            tryOpenDoor(door)
        }
    }

    private fun isBlockedForPlayer(x: Float, y: Float, r: Float): Boolean {
        val minGx = floor((x - r).toDouble()).toInt()
        val maxGx = floor((x + r).toDouble()).toInt()
        val minGy = floor((y - r).toDouble()).toInt()
        val maxGy = floor((y + r).toDouble()).toInt()

        for (gy in minGy..maxGy) {
            for (gx in minGx..maxGx) {
                if (gx !in 0 until level.width || gy !in 0 until level.height) return true
                val wall = level.grid[gy][gx]
                if (wall > 0) {
                    val door = doors[packCell(gx, gy)]
                    if (door == null || door.openAmount < 0.78f) {
                        return true
                    }
                }
            }
        }

        for (barrel in barrels) {
            if (!barrel.exploded) {
                val bx = barrel.x - x
                val by = barrel.y - y
                if (bx * bx + by * by < (r + 0.26f) * (r + 0.26f)) return true
            }
        }
        return false
    }

    private fun collectPickup(p: PickupEntity) {
        var picked = false
        var msg = ""
        var isKey = false

        when (p.type) {
            PickupType.STIMPACK -> if (health < 100) {
                health = (health + 12).coerceAtMost(100)
                picked = true; msg = "+12 SAĞLIK"
            }
            PickupType.MEDIKIT -> if (health < 100) {
                health = (health + 30).coerceAtMost(100)
                picked = true; msg = "+30 İLK YARDIM ÇANTASI"
            }
            PickupType.GREEN_ARMOR -> if (armor < 100) {
                armor = (armor + 50).coerceAtMost(100)
                picked = true; msg = "+50 YEŞİL ZIRH"
            }
            PickupType.MEGA_ARMOR -> if (armor < 200) {
                armor = 200
                picked = true; msg = "MEGA ZIRH (%200)!"
            }
            PickupType.AMMO_CLIP -> if (bullets < 200) {
                bullets = (bullets + 14).coerceAtMost(200)
                picked = true; msg = "+14 MERMİ"
            }
            PickupType.SHELLS -> if (shells < 60) {
                shells = (shells + 6).coerceAtMost(60)
                picked = true; msg = "+6 FİŞEK"
            }
            PickupType.CELLS -> if (cells < 150) {
                cells = (cells + 25).coerceAtMost(150)
                picked = true; msg = "+25 PLAZMA ENERJİSİ"
            }
            PickupType.SHOTGUN_WEAPON -> {
                val newlyOwned = ownedWeapons.add(WeaponType.SHOTGUN)
                shells = (shells + 8).coerceAtMost(60)
                if (newlyOwned) activeWeapon = WeaponType.SHOTGUN
                picked = true; msg = "POMPALI TÜFEK ALINDI!"
                faceMood = 3; faceOverrideTimer = 1.4f
            }
            PickupType.CHAINGUN_WEAPON -> {
                val newlyOwned = ownedWeapons.add(WeaponType.CHAINGUN)
                bullets = (bullets + 25).coerceAtMost(200)
                if (newlyOwned) activeWeapon = WeaponType.CHAINGUN
                picked = true; msg = "MAKİNELİ TÜFEK ALINDI!"
                faceMood = 3; faceOverrideTimer = 1.4f
            }
            PickupType.PLASMA_WEAPON -> {
                val newlyOwned = ownedWeapons.add(WeaponType.PLASMA)
                cells = (cells + 40).coerceAtMost(150)
                if (newlyOwned) activeWeapon = WeaponType.PLASMA
                picked = true; msg = "PLAZMA TÜFEĞİ ALINDI!"
                faceMood = 3; faceOverrideTimer = 1.6f
            }
            PickupType.BLUE_KEY -> {
                ownedKeys.add(KeyColor.BLUE)
                picked = true; isKey = true; msg = "MAVİ KART ALINDI!"
            }
            PickupType.RED_KEY -> {
                ownedKeys.add(KeyColor.RED)
                picked = true; isKey = true; msg = "KIRMIZI KART ALINDI!"
            }
            PickupType.YELLOW_KEY -> {
                ownedKeys.add(KeyColor.YELLOW)
                picked = true; isKey = true; msg = "SARI KART ALINDI!"
            }
        }

        if (picked) {
            p.collected = true
            if (p.type.isItemCounted) itemsCount++
            pickupFlash = 0.45f
            showMessage(msg, 2.0f)
            soundEngine.play(if (isKey) SfxType.KEYCARD else SfxType.PICKUP)
        }
    }

    private fun updateDoors(dt: Float) {
        for (door in doors.values) {
            if (door.opening) {
                door.openAmount = (door.openAmount + dt * 1.6f).coerceAtMost(1f)
                if (door.openAmount >= 1f) {
                    door.holdTimer -= dt
                    if (door.holdTimer <= 0f) {
                        // Only close if player and enemies aren't inside the doorway
                        val pxCell = playerX.toInt()
                        val pyCell = playerY.toInt()
                        val occupiedByEnemy = enemies.any {
                            it.alive && it.x.toInt() == door.gx && it.y.toInt() == door.gy
                        }
                        if ((pxCell != door.gx || pyCell != door.gy) && !occupiedByEnemy) {
                            door.opening = false
                        } else {
                            door.holdTimer = 1.0f
                        }
                    }
                }
            } else if (door.openAmount > 0f) {
                door.openAmount = (door.openAmount - dt * 1.4f).coerceAtLeast(0f)
            }
        }
    }

    private fun updateWeapons(dt: Float) {
        weaponCooldown = (weaponCooldown - dt).coerceAtLeast(0f)
        muzzleFlashTimer = (muzzleFlashTimer - dt).coerceAtLeast(0f)
        weaponRecoil = (weaponRecoil - dt * 5.5f).coerceAtLeast(0f)

        if (!fireHeld || weaponCooldown > 0f) return

        // Check ammo
        val hasAmmo = when (activeWeapon.ammoType) {
            AmmoType.NONE -> true
            AmmoType.BULLETS -> bullets >= activeWeapon.ammoPerShot
            AmmoType.SHELLS -> shells >= activeWeapon.ammoPerShot
            AmmoType.CELLS -> cells >= activeWeapon.ammoPerShot
        }

        if (!hasAmmo) {
            autoSwitchBestWeapon()
            return
        }

        when (activeWeapon.ammoType) {
            AmmoType.BULLETS -> bullets -= activeWeapon.ammoPerShot
            AmmoType.SHELLS -> shells -= activeWeapon.ammoPerShot
            AmmoType.CELLS -> cells -= activeWeapon.ammoPerShot
            AmmoType.NONE -> {}
        }

        weaponCooldown = activeWeapon.cooldownSec
        weaponRecoil = 1f
        if (activeWeapon != WeaponType.FIST) {
            muzzleFlashTimer = 0.08f
        }

        // Alert nearby enemies on gunshot!
        if (activeWeapon != WeaponType.FIST) {
            for (e in enemies) {
                if (!e.alive || e.alert) continue
                val d2 = (e.x - playerX) * (e.x - playerX) + (e.y - playerY) * (e.y - playerY)
                if (d2 < 85f) e.alert = true
            }
        }

        val sfx = when (activeWeapon) {
            WeaponType.FIST -> SfxType.FIST
            WeaponType.PISTOL -> SfxType.PISTOL
            WeaponType.SHOTGUN -> SfxType.SHOTGUN
            WeaponType.CHAINGUN -> SfxType.CHAINGUN
            WeaponType.PLASMA -> SfxType.PLASMA
        }
        soundEngine.play(sfx, rng.nextFloat() * 0.1f + 0.95f)
        if (activeWeapon == WeaponType.SHOTGUN) onVibrate(28L)

        if (activeWeapon.isProjectile) {
            val spread = (rng.nextFloat() - 0.5f) * activeWeapon.spreadRad
            val ang = playerAngle + spread
            val speed = 13.5f
            projectiles += ProjectileEntity(
                id = nextProjectileId++,
                x = playerX + cos(ang) * 0.35f,
                y = playerY + sin(ang) * 0.35f,
                vx = cos(ang) * speed,
                vy = sin(ang) * speed,
                damage = activeWeapon.damage,
                fromPlayer = true,
                isPlasma = true
            )
        } else {
            // Hitscan pellets (with classic DOOM horizontal cone check)
            repeat(activeWeapon.pellets) {
                val spread = (rng.nextFloat() - 0.5f) * activeWeapon.spreadRad
                fireHitscanRay(playerAngle + spread, activeWeapon.range, activeWeapon.damage)
            }
        }
    }

    private fun autoSwitchBestWeapon() {
        val order = listOf(
            WeaponType.SHOTGUN,
            WeaponType.CHAINGUN,
            WeaponType.PLASMA,
            WeaponType.PISTOL,
            WeaponType.FIST
        )
        for (w in order) {
            if (!ownedWeapons.contains(w)) continue
            val ok = when (w.ammoType) {
                AmmoType.NONE -> true
                AmmoType.BULLETS -> bullets > 0
                AmmoType.SHELLS -> shells > 0
                AmmoType.CELLS -> cells > 0
            }
            if (ok) {
                activeWeapon = w
                showMessage("CEPHANE BİTTİ -> ${w.titleTr}", 1.5f)
                return
            }
        }
    }

    private fun fireHitscanRay(angle: Float, maxRange: Float, damage: Int) {
        val dx = cos(angle)
        val dy = sin(angle)
        val step = 0.08f
        var dist = 0.15f

        while (dist <= maxRange) {
            val rx = playerX + dx * dist
            val ry = playerY + dy * dist
            val gx = rx.toInt()
            val gy = ry.toInt()

            if (gx !in 0 until level.width || gy !in 0 until level.height) break
            val wall = level.grid[gy][gx]
            if (wall > 0) {
                val door = doors[packCell(gx, gy)]
                if (door == null || door.openAmount < 0.78f) {
                    spawnParticles(rx - dx * 0.08f, ry - dy * 0.08f, 0.5f, 0xFFFFD166.toInt(), 3)
                    return
                }
            }

            // Check explosive barrels
            for (b in barrels) {
                if (b.exploded) continue
                val bdx = b.x - rx
                val bdy = b.y - ry
                if (bdx * bdx + bdy * bdy < 0.14f) {
                    damageBarrel(b, damage)
                    return
                }
            }

            // Check enemies
            for (e in enemies) {
                if (!e.alive) continue
                val edx = e.x - rx
                val edy = e.y - ry
                val rad = e.type.radius + 0.10f
                if (edx * edx + edy * edy < rad * rad) {
                    damageEnemy(e, damage)
                    return
                }
            }
            dist += step
        }
    }

    private fun damageEnemy(e: EnemyEntity, damage: Int) {
        if (!e.alive) return
        e.hp -= damage
        e.alert = true
        e.painTimer = 0.14f
        spawnParticles(e.x, e.y, 0.55f, 0xFFD92626.toInt(), 6)

        if (e.hp <= 0) {
            e.alive = false
            e.deathTimer = 0.35f
            killsCount++
            soundEngine.play(SfxType.ENEMY_DEATH, rng.nextFloat() * 0.15f + 0.9f)
            // Classic DOOM drops!
            if (e.type == EnemyType.ZOMBIEMAN) {
                pickups += PickupEntity(pickups.size + 1000, PickupType.AMMO_CLIP, e.x, e.y)
            } else if (e.type == EnemyType.SHOTGUNNER) {
                pickups += PickupEntity(pickups.size + 1000, PickupType.SHELLS, e.x, e.y)
            }
            if (e.type == EnemyType.BARON) {
                showMessage("CEHENNEM BARONU YOK EDİLDİ!", 3.0f)
            }
        } else {
            soundEngine.play(SfxType.ENEMY_HIT, rng.nextFloat() * 0.15f + 0.92f)
        }
    }

    private fun damageBarrel(b: BarrelEntity, damage: Int) {
        if (b.exploded) return
        b.hp -= damage
        spawnParticles(b.x, b.y, 0.45f, 0xFF52F542.toInt(), 4)
        if (b.hp <= 0) {
            explodeBarrel(b)
        }
    }

    private fun explodeBarrel(b: BarrelEntity) {
        if (b.exploded) return
        b.exploded = true
        screenShake = 0.35f
        soundEngine.play(SfxType.EXPLOSION)
        onVibrate(60L)
        spawnParticles(b.x, b.y, 0.5f, 0xFFFF7A1A.toInt(), 18)

        val blastRadius = 2.8f
        // Damage player
        val pd = sqrt((playerX - b.x) * (playerX - b.x) + (playerY - b.y) * (playerY - b.y))
        if (pd < blastRadius) {
            val dmg = ((1f - pd / blastRadius) * 45f).toInt().coerceAtLeast(8)
            applyPlayerDamage(dmg)
        }
        // Damage enemies
        for (e in enemies) {
            if (!e.alive) continue
            val ed = sqrt((e.x - b.x) * (e.x - b.x) + (e.y - b.y) * (e.y - b.y))
            if (ed < blastRadius) {
                val dmg = ((1f - ed / blastRadius) * 110f).toInt().coerceAtLeast(20)
                damageEnemy(e, dmg)
            }
        }
        // Chain reaction!
        for (other in barrels) {
            if (!other.exploded) {
                val bd = sqrt((other.x - b.x) * (other.x - b.x) + (other.y - b.y) * (other.y - b.y))
                if (bd < blastRadius) {
                    explodeBarrel(other)
                }
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val iter = projectiles.iterator()
        while (iter.hasNext()) {
            val pr = iter.next()
            if (!pr.alive) {
                iter.remove()
                continue
            }
            pr.x += pr.vx * dt
            pr.y += pr.vy * dt

            val gx = pr.x.toInt()
            val gy = pr.y.toInt()
            if (gx !in 0 until level.width || gy !in 0 until level.height) {
                pr.alive = false
                continue
            }

            val wall = level.grid[gy][gx]
            if (wall > 0) {
                val door = doors[packCell(gx, gy)]
                if (door == null || door.openAmount < 0.78f) {
                    val col = if (pr.isPlasma) 0xFF33D4FF.toInt() else 0xFFFF7A1A.toInt()
                    spawnParticles(pr.x, pr.y, 0.5f, col, 6)
                    pr.alive = false
                    continue
                }
            }

            if (pr.fromPlayer) {
                var hit = false
                for (b in barrels) {
                    if (!b.exploded && distSq(pr.x, pr.y, b.x, b.y) < 0.16f) {
                        damageBarrel(b, pr.damage)
                        hit = true
                        break
                    }
                }
                if (!hit) {
                    for (e in enemies) {
                        if (!e.alive) continue
                        val rad = e.type.radius + 0.14f
                        if (distSq(pr.x, pr.y, e.x, e.y) < rad * rad) {
                            damageEnemy(e, pr.damage)
                            spawnParticles(pr.x, pr.y, 0.5f, 0xFF33D4FF.toInt(), 5)
                            hit = true
                            break
                        }
                    }
                }
                if (hit) pr.alive = false
            } else {
                if (distSq(pr.x, pr.y, playerX, playerY) < (PLAYER_RADIUS + 0.16f) * (PLAYER_RADIUS + 0.16f)) {
                    applyPlayerDamage((pr.damage * difficulty.damageScale).toInt().coerceAtLeast(4))
                    pr.alive = false
                }
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        for (e in enemies) {
            if (!e.alive) {
                if (e.deathTimer > 0f) e.deathTimer = (e.deathTimer - dt).coerceAtLeast(0f)
                continue
            }
            e.painTimer = (e.painTimer - dt).coerceAtLeast(0f)
            e.animTimer += dt

            val dx = playerX - e.x
            val dy = playerY - e.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
            val canSee = dist < 15f && hasLineOfSight(e.x, e.y, playerX, playerY)

            if (!e.alert) {
                if (canSee && dist < 11.5f) {
                    e.alert = true
                } else {
                    continue
                }
            }

            // Movement toward player when outside preferred standoff range
            val desiredMinDist = if (e.type.isRanged) 2.6f else 0.72f
            if (e.painTimer <= 0f && dist > desiredMinDist) {
                val spd = e.type.speed * difficulty.enemySpeedScale * dt
                val nx = dx / dist
                val ny = dy / dist
                val stepX = e.x + nx * spd
                val stepY = e.y + ny * spd
                if (!isBlockedForEnemy(stepX, e.y, e.type.radius, e.id)) e.x = stepX
                if (!isBlockedForEnemy(e.x, stepY, e.type.radius, e.id)) e.y = stepY
            }

            // Attack cooldown
            e.attackTimer -= dt
            if (e.attackTimer <= 0f && canSee && dist <= e.type.attackRange) {
                e.attackTimer = e.type.attackCooldownSec * (0.85f + rng.nextFloat() * 0.3f)
                if (e.type.isProjectileAttacker) {
                    val isPlasma = e.type == EnemyType.CACODEMON || e.type == EnemyType.BARON
                    val pSpd = if (e.type == EnemyType.BARON) 8.2f else 6.8f
                    val nx = dx / dist
                    val ny = dy / dist
                    projectiles += ProjectileEntity(
                        id = nextProjectileId++,
                        x = e.x + nx * 0.38f,
                        y = e.y + ny * 0.38f,
                        vx = nx * pSpd,
                        vy = ny * pSpd,
                        damage = e.type.damage,
                        fromPlayer = false,
                        isPlasma = isPlasma
                    )
                    soundEngine.play(if (isPlasma) SfxType.PLASMA else SfxType.PISTOL, 0.72f)
                } else if (e.type.isRanged) {
                    // Hitscan zombie/sergeant with distance accuracy falloff
                    soundEngine.play(
                        if (e.type == EnemyType.SHOTGUNNER) SfxType.SHOTGUN else SfxType.PISTOL,
                        0.85f
                    )
                    val hitChance = (1f - dist / 14f).coerceIn(0.25f, 0.85f)
                    if (rng.nextFloat() < hitChance) {
                        val dmg = (e.type.damage * difficulty.damageScale * (0.65f + rng.nextFloat() * 0.5f))
                            .toInt().coerceAtLeast(3)
                        applyPlayerDamage(dmg)
                    }
                } else {
                    // Melee bite (Pinky Demon)
                    soundEngine.play(SfxType.FIST, 0.75f)
                    applyPlayerDamage((e.type.damage * difficulty.damageScale).toInt().coerceAtLeast(6))
                }
            }
        }
    }

    private fun isBlockedForEnemy(x: Float, y: Float, r: Float, selfId: Int): Boolean {
        val minGx = floor((x - r).toDouble()).toInt()
        val maxGx = floor((x + r).toDouble()).toInt()
        val minGy = floor((y - r).toDouble()).toInt()
        val maxGy = floor((y + r).toDouble()).toInt()
        for (gy in minGy..maxGy) {
            for (gx in minGx..maxGx) {
                if (gx !in 0 until level.width || gy !in 0 until level.height) return true
                val wall = level.grid[gy][gx]
                if (wall > 0) {
                    val door = doors[packCell(gx, gy)]
                    if (door == null || door.openAmount < 0.78f) return true
                }
            }
        }
        for (other in enemies) {
            if (other.id == selfId || !other.alive) continue
            val minD = r + other.type.radius
            if (distSq(x, y, other.x, other.y) < minD * minD) return true
        }
        if (distSq(x, y, playerX, playerY) < (r + PLAYER_RADIUS) * (r + PLAYER_RADIUS)) return true
        return false
    }

    private fun hasLineOfSight(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val dx = x1 - x0
        val dy = y1 - y0
        val dist = sqrt(dx * dx + dy * dy)
        val steps = (dist / 0.22f).toInt().coerceAtLeast(1)
        val sx = dx / steps
        val sy = dy / steps
        var cx = x0
        var cy = y0
        for (i in 1 until steps) {
            cx += sx
            cy += sy
            val gx = cx.toInt()
            val gy = cy.toInt()
            if (gx !in 0 until level.width || gy !in 0 until level.height) return false
            val wall = level.grid[gy][gx]
            if (wall > 0) {
                val door = doors[packCell(gx, gy)]
                if (door == null || door.openAmount < 0.72f) return false
            }
        }
        return true
    }

    private fun applyPlayerDamage(rawDmg: Int, isHazard: Boolean = false) {
        if (phase != GamePhase.PLAYING) return
        val armorAbsorb = if (armor > 0) (rawDmg * 0.45f).toInt().coerceAtMost(armor) else 0
        armor -= armorAbsorb
        val hpDmg = (rawDmg - armorAbsorb).coerceAtLeast(1)
        health = (health - hpDmg).coerceAtLeast(0)
        damageFlash = (damageFlash + 0.55f).coerceAtMost(0.95f)
        if (!isHazard) {
            screenShake = 0.16f
            onVibrate(40L)
        }
        faceMood = 4
        faceOverrideTimer = 0.55f
        soundEngine.play(SfxType.HURT, rng.nextFloat() * 0.12f + 0.94f)

        if (health <= 0) {
            health = 0
            phase = GamePhase.DEAD
            faceMood = 5
            onVibrate(120L)
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val pt = iter.next()
            pt.life -= dt
            if (pt.life <= 0f) {
                iter.remove()
            } else {
                pt.x += pt.vx * dt
                pt.y += pt.vy * dt
                pt.z += pt.vz * dt
                pt.vz -= 2.8f * dt
            }
        }
    }

    private fun updateTimers(dt: Float) {
        damageFlash = (damageFlash - dt * 1.9f).coerceAtLeast(0f)
        pickupFlash = (pickupFlash - dt * 2.2f).coerceAtLeast(0f)
        screenShake = (screenShake - dt * 1.8f).coerceAtLeast(0f)
        if (statusMessageTimer > 0f) {
            statusMessageTimer -= dt
            if (statusMessageTimer <= 0f) statusMessage = ""
        }
        if (faceOverrideTimer > 0f) {
            faceOverrideTimer -= dt
            if (faceOverrideTimer <= 0f) faceMood = 0
        } else {
            faceLookTimer -= dt
            if (faceLookTimer <= 0f) {
                faceLookTimer = 1.1f + rng.nextFloat() * 1.4f
                faceMood = rng.nextInt(3) // 0, 1, 2
            }
        }
    }

    private fun spawnParticles(x: Float, y: Float, z: Float, color: Int, count: Int) {
        repeat(count) {
            particles += ParticleEntity(
                x = x,
                y = y,
                z = z,
                vx = (rng.nextFloat() - 0.5f) * 2.2f,
                vy = (rng.nextFloat() - 0.5f) * 2.2f,
                vz = rng.nextFloat() * 1.6f,
                color = color,
                life = 0.25f + rng.nextFloat() * 0.25f
            )
        }
    }

    private fun revealAroundPlayer() {
        val px = playerX.toInt()
        val py = playerY.toInt()
        for (dy in -4..4) {
            for (dx in -4..4) {
                val gx = px + dx
                val gy = py + dy
                if (gx in 0 until level.width && gy in 0 until level.height) {
                    if (dx * dx + dy * dy <= 20) {
                        explored[gy * level.width + gx] = true
                    }
                }
            }
        }
    }

    private fun showMessage(msg: String, duration: Float) {
        statusMessage = msg
        statusMessageTimer = duration
    }

    private fun renderFrame() {
        val dirX = cos(playerAngle)
        val dirY = sin(playerAngle)
        val planeX = -dirY * FOV_PLANE
        val planeY = dirX * FOV_PLANE

        val bobYOffset = (sin(bobPhase) * 3.5f * bobAmount).toInt()
        val shakeX = if (screenShake > 0.01f) ((rng.nextFloat() - 0.5f) * 8f * screenShake).toInt() else 0
        val shakeY = if (screenShake > 0.01f) ((rng.nextFloat() - 0.5f) * 8f * screenShake).toInt() else 0
        val horizon = RENDER_HEIGHT / 2 + bobYOffset + shakeY
        val muzzleBoost = if (muzzleFlashTimer > 0f) 0.28f else 0f

        // 1. Ceiling & Floor scanlines (with Nukage & Secret floor casting!)
        val isHell = level.id == 3
        val ceilR = if (isHell) 46 else 24
        val ceilG = if (isHell) 14 else 22
        val ceilB = if (isHell) 16 else 28

        for (y in 0 until RENDER_HEIGHT) {
            val rowOffset = y * RENDER_WIDTH
            if (y < horizon) {
                val distFactor = ((horizon - y).toFloat() / RENDER_HEIGHT * 2f).coerceIn(0.15f, 1f)
                val col = packArgb(
                    (ceilR * distFactor).toInt(),
                    (ceilG * distFactor).toInt(),
                    (ceilB * distFactor).toInt()
                )
                pixels.fill(col, rowOffset, rowOffset + RENDER_WIDTH)
            } else {
                val p = (y - horizon).coerceAtLeast(1)
                val rowDistance = (0.5f * RENDER_HEIGHT) / p
                val fog = (1f / (1f + rowDistance * 0.17f) + muzzleBoost).coerceIn(0.14f, 1f)

                val rayDirX0 = dirX - planeX
                val rayDirY0 = dirY - planeY
                val rayDirX1 = dirX + planeX
                val rayDirY1 = dirY + planeY

                val floorStepX = rowDistance * (rayDirX1 - rayDirX0) / RENDER_WIDTH
                val floorStepY = rowDistance * (rayDirY1 - rayDirY0) / RENDER_WIDTH
                var floorX = playerX + rowDistance * rayDirX0
                var floorY = playerY + rowDistance * rayDirY0

                for (x in 0 until RENDER_WIDTH) {
                    val cellX = floorX.toInt()
                    val cellY = floorY.toInt()
                    val tx = ((floorX - cellX) * 64f).toInt() and 63
                    val ty = ((floorY - cellY) * 64f).toInt() and 63
                    val key = packCell(cellX, cellY)
                    val checker = ((tx shr 4) + (ty shr 4)) and 1

                    val baseR: Int
                    val baseG: Int
                    val baseB: Int
                    if (level.nukageTiles.contains(key)) {
                        // Bubbling glowing toxic nukage floor
                        val glow = if (((tx + ty) % 13) < 3) 235 else 175
                        baseR = 32; baseG = glow; baseB = 42
                    } else if (level.secretTiles.contains(key)) {
                        baseR = if (checker == 0) 140 else 110
                        baseG = if (checker == 0) 105 else 80
                        baseB = 34
                    } else if (isHell) {
                        baseR = if (checker == 0) 68 else 54
                        baseG = if (checker == 0) 32 else 24
                        baseB = if (checker == 0) 30 else 22
                    } else {
                        baseR = if (checker == 0) 68 else 56
                        baseG = if (checker == 0) 68 else 56
                        baseB = if (checker == 0) 74 else 62
                    }

                    pixels[rowOffset + x] = packArgb(
                        (baseR * fog).toInt(),
                        (baseG * fog).toInt(),
                        (baseB * fog).toInt()
                    )
                    floorX += floorStepX
                    floorY += floorStepY
                }
            }
        }

        // 2. DDA Wall Raycasting (including sliding doors!)
        for (x in 0 until RENDER_WIDTH) {
            val cameraX = 2f * (x + shakeX) / RENDER_WIDTH - 1f
            val rayDirX = dirX + planeX * cameraX
            val rayDirY = dirY + planeY * cameraX

            var mapX = playerX.toInt()
            var mapY = playerY.toInt()

            val deltaDistX = if (rayDirX == 0f) 1e30f else abs(1f / rayDirX)
            val deltaDistY = if (rayDirY == 0f) 1e30f else abs(1f / rayDirY)

            val stepX: Int
            val stepY: Int
            var sideDistX: Float
            var sideDistY: Float

            if (rayDirX < 0f) {
                stepX = -1
                sideDistX = (playerX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1f - playerX) * deltaDistX
            }

            if (rayDirY < 0f) {
                stepY = -1
                sideDistY = (playerY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1f - playerY) * deltaDistY
            }

            var hitWall = 0
            var side = 0
            var perpWallDist = 1f
            var hitDoorOffset = 0f

            var steps = 0
            while (steps++ < 48) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }
                if (mapX !in 0 until level.width || mapY !in 0 until level.height) {
                    hitWall = 1
                    perpWallDist = 16f
                    break
                }

                val cell = level.grid[mapY][mapX]
                if (cell > 0) {
                    val door = doors[packCell(mapX, mapY)]
                    if (door != null) {
                        val rawDist = if (side == 0) sideDistX - deltaDistX else sideDistY - deltaDistY
                        val wallX = if (side == 0) {
                            playerY + rawDist * rayDirY
                        } else {
                            playerX + rawDist * rayDirX
                        }
                        val frac = wallX - floor(wallX)
                        if (frac >= door.openAmount) {
                            hitWall = cell
                            perpWallDist = rawDist.coerceAtLeast(0.08f)
                            hitDoorOffset = door.openAmount
                            break
                        }
                    } else {
                        hitWall = cell
                        perpWallDist = if (side == 0) sideDistX - deltaDistX else sideDistY - deltaDistY
                        perpWallDist = perpWallDist.coerceAtLeast(0.08f)
                        break
                    }
                }
            }

            zBuffer[x] = perpWallDist
            if (hitWall <= 0) continue

            val lineHeight = (RENDER_HEIGHT / perpWallDist).toInt()
            val drawStart = (-lineHeight / 2 + horizon).coerceAtLeast(0)
            val drawEnd = (lineHeight / 2 + horizon).coerceAtMost(RENDER_HEIGHT - 1)

            var wallX = if (side == 0) {
                playerY + perpWallDist * rayDirY
            } else {
                playerX + perpWallDist * rayDirX
            }
            wallX -= floor(wallX)
            wallX = (wallX - hitDoorOffset).coerceIn(0f, 0.999f)

            var texX = (wallX * ProceduralTextures.TEX_SIZE).toInt() and 63
            if ((side == 0 && rayDirX > 0f) || (side == 1 && rayDirY < 0f)) {
                texX = 63 - texX
            }

            val texture = ProceduralTextures.wallTextures[hitWall]
                ?: ProceduralTextures.wallTextures.getValue(ProceduralTextures.WALL_UAC_TECH)

            val sideShade = if (side == 1) 0.72f else 1.0f
            val fog = (1f / (1f + perpWallDist * 0.14f) + muzzleBoost).coerceIn(0.15f, 1.0f) * sideShade

            val step = ProceduralTextures.TEX_SIZE.toFloat() / lineHeight.coerceAtLeast(1)
            var texPos = (drawStart - horizon + lineHeight / 2f) * step

            for (y in drawStart..drawEnd) {
                val texY = texPos.toInt() and 63
                texPos += step
                val texColor = texture[(texY shl 6) + texX]
                pixels[y * RENDER_WIDTH + x] = shadeColor(texColor, fog)
            }
        }

        // 3. Z-Buffered Sprites (Pickups, Barrels, Enemies, Projectiles, Particles)
        renderSprites(dirX, dirY, planeX, planeY, horizon, muzzleBoost)

        // 4. First-person DOOM weapon viewmodel
        renderWeaponViewmodel(bobYOffset)

        // 5. Screen tint overlays (Damage crimson flash / Pickup gold flash)
        if (damageFlash > 0.02f || pickupFlash > 0.02f) {
            applyScreenFlash()
        }

        frameBitmap.setPixels(pixels, 0, RENDER_WIDTH, 0, 0, RENDER_WIDTH, RENDER_HEIGHT)
    }

    private data class DrawableSprite(
        val x: Float,
        val y: Float,
        val distSq: Float,
        val spriteData: IntArray,
        val scale: Float,
        val vShift: Float,
        val flashWhite: Boolean = false
    )

    private fun renderSprites(
        dirX: Float,
        dirY: Float,
        planeX: Float,
        planeY: Float,
        horizon: Int,
        muzzleBoost: Float
    ) {
        val list = ArrayList<DrawableSprite>(enemies.size + pickups.size + barrels.size + projectiles.size)

        for (p in pickups) {
            if (p.collected) continue
            val spr = ProceduralTextures.pickupSprites[p.type] ?: continue
            list += DrawableSprite(p.x, p.y, distSq(playerX, playerY, p.x, p.y), spr, 0.72f, 10f)
        }

        for (b in barrels) {
            if (b.exploded) continue
            list += DrawableSprite(
                b.x, b.y, distSq(playerX, playerY, b.x, b.y),
                ProceduralTextures.barrelSprite, 0.85f, 6f
            )
        }

        for (e in enemies) {
            val key = if (!e.alive) {
                "${e.type.name}_2"
            } else if (e.attackTimer > e.type.attackCooldownSec - 0.22f || ((e.animTimer * 4f).toInt() % 2 == 1 && e.alert)) {
                "${e.type.name}_1"
            } else {
                "${e.type.name}_0"
            }
            val spr = ProceduralTextures.enemySprites[key] ?: continue
            val vShift = if (e.type == EnemyType.CACODEMON && e.alive) -10f else 4f
            list += DrawableSprite(
                e.x, e.y, distSq(playerX, playerY, e.x, e.y),
                spr, e.type.spriteScale, vShift, e.painTimer > 0f
            )
        }

        for (pr in projectiles) {
            if (!pr.alive) continue
            val spr = if (pr.isPlasma) ProceduralTextures.plasmaSprite else ProceduralTextures.fireballSprite
            list += DrawableSprite(pr.x, pr.y, distSq(playerX, playerY, pr.x, pr.y), spr, 0.45f, 0f)
        }

        // Back-to-front painter sort
        list.sortByDescending { it.distSq }

        val invDet = 1.0f / (planeX * dirY - dirX * planeY)
        val sz = ProceduralTextures.SPRITE_SIZE

        for (spr in list) {
            val sx = spr.x - playerX
            val sy = spr.y - playerY
            val transformX = invDet * (dirY * sx - dirX * sy)
            val transformY = invDet * (-planeY * sx + planeX * sy)
            if (transformY <= 0.18f) continue

            val screenX = ((RENDER_WIDTH / 2f) * (1f + transformX / transformY)).toInt()
            val spriteHeight = abs((RENDER_HEIGHT / transformY) * spr.scale).toInt().coerceAtMost(RENDER_HEIGHT * 3)
            val spriteWidth = spriteHeight
            val vMoveScreen = (spr.vShift / transformY).toInt()

            val drawStartY = (-spriteHeight / 2 + horizon + vMoveScreen).coerceAtLeast(0)
            val drawEndY = (spriteHeight / 2 + horizon + vMoveScreen).coerceAtMost(RENDER_HEIGHT - 1)
            val drawStartX = (-spriteWidth / 2 + screenX).coerceAtLeast(0)
            val drawEndX = (spriteWidth / 2 + screenX).coerceAtMost(RENDER_WIDTH - 1)

            val fog = (1f / (1f + transformY * 0.13f) + muzzleBoost).coerceIn(0.2f, 1f)

            for (stripe in drawStartX..drawEndX) {
                if (transformY >= zBuffer[stripe]) continue
                val texX = ((stripe - (-spriteWidth / 2 + screenX)) * sz / spriteWidth.coerceAtLeast(1))
                    .coerceIn(0, sz - 1)

                for (y in drawStartY..drawEndY) {
                    val d = y - vMoveScreen - horizon + spriteHeight / 2
                    val texY = (d * sz / spriteHeight.coerceAtLeast(1)).coerceIn(0, sz - 1)
                    val col = spr.spriteData[texY * sz + texX]
                    if ((col ushr 24) > 128) {
                        pixels[y * RENDER_WIDTH + stripe] = if (spr.flashWhite) {
                            0xFFFFCCCC.toInt()
                        } else {
                            shadeColor(col, fog)
                        }
                    }
                }
            }
        }

        // Render tiny blood/spark particles
        for (pt in particles) {
            val sx = pt.x - playerX
            val sy = pt.y - playerY
            val tx = invDet * (dirY * sx - dirX * sy)
            val ty = invDet * (-planeY * sx + planeX * sy)
            if (ty <= 0.25f) continue
            val px = ((RENDER_WIDTH / 2f) * (1f + tx / ty)).toInt()
            val py = (horizon + ((0.5f - pt.z) * RENDER_HEIGHT / ty)).toInt()
            if (px in 1 until RENDER_WIDTH - 1 && py in 1 until RENDER_HEIGHT - 1 && ty < zBuffer[px]) {
                pixels[py * RENDER_WIDTH + px] = pt.color
                pixels[py * RENDER_WIDTH + px + 1] = pt.color
                pixels[(py + 1) * RENDER_WIDTH + px] = pt.color
            }
        }
    }

    private fun renderWeaponViewmodel(bobYOffset: Int) {
        val bobX = (cos(bobPhase * 0.5f) * 8f * bobAmount).toInt()
        val recoilY = (weaponRecoil * 18f).toInt()
        val cx = RENDER_WIDTH / 2 + bobX
        val baseY = RENDER_HEIGHT - 1 + max(0, bobYOffset) + recoilY

        fun fillRect(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
            val sx = x0.coerceIn(0, RENDER_WIDTH - 1)
            val ex = x1.coerceIn(0, RENDER_WIDTH - 1)
            val sy = y0.coerceIn(0, RENDER_HEIGHT - 1)
            val ey = y1.coerceIn(0, RENDER_HEIGHT - 1)
            for (y in sy..ey) {
                val row = y * RENDER_WIDTH
                for (x in sx..ex) pixels[row + x] = color
            }
        }

        // Muzzle flash behind gun barrel
        if (muzzleFlashTimer > 0f && activeWeapon != WeaponType.FIST) {
            val flashCol = if (activeWeapon == WeaponType.PLASMA) 0xFF5CE1FF.toInt() else 0xFFFFD13B.toInt()
            val coreCol = 0xFFFFFFFF.toInt()
            val fy = baseY - 64
            fillRect(cx - 16, fy - 12, cx + 16, fy + 10, flashCol)
            fillRect(cx - 8, fy - 18, cx + 8, fy + 14, coreCol)
        }

        when (activeWeapon) {
            WeaponType.FIST -> {
                val punchPush = (weaponRecoil * 24f).toInt()
                val fy = baseY - 38 - punchPush
                // Forearm + brass knuckles
                fillRect(cx + 12, fy + 16, cx + 44, RENDER_HEIGHT - 1, 0xFF8F654D.toInt())
                fillRect(cx + 4, fy, cx + 46, fy + 22, 0xFFA8795E.toInt())
                fillRect(cx + 2, fy - 4, cx + 42, fy + 4, 0xFFD4A72C.toInt())
            }
            WeaponType.PISTOL -> {
                // Grip + steel slide
                fillRect(cx - 10, baseY - 36, cx + 10, RENDER_HEIGHT - 1, 0xFF2B2B30.toInt())
                fillRect(cx - 8, baseY - 58, cx + 8, baseY - 26, 0xFF52525C.toInt())
                fillRect(cx - 5, baseY - 62, cx + 5, baseY - 34, 0xFF7B7B88.toInt())
                fillRect(cx - 2, baseY - 62, cx + 2, baseY - 58, 0xFF141418.toInt())
            }
            WeaponType.SHOTGUN -> {
                // Twin-tone classic combat shotgun
                fillRect(cx - 13, baseY - 40, cx + 13, RENDER_HEIGHT - 1, 0xFF5C341A.toInt())
                fillRect(cx - 9, baseY - 64, cx + 9, baseY - 32, 0xFF3E3E46.toInt())
                fillRect(cx - 6, baseY - 72, cx + 6, baseY - 48, 0xFF6E6E7A.toInt())
                fillRect(cx - 3, baseY - 74, cx + 3, baseY - 66, 0xFF18181B.toInt())
            }
            WeaponType.CHAINGUN -> {
                // Wide rotary barrels!
                val spin = if (fireHeld && bullets > 0) ((elapsedTimeSec * 25f).toInt() % 3) * 3 else 0
                fillRect(cx - 22, baseY - 42, cx + 22, RENDER_HEIGHT - 1, 0xFF2E2E34.toInt())
                fillRect(cx - 18, baseY - 66, cx + 18, baseY - 36, 0xFF585864.toInt())
                for (bx in -14..14 step 7) {
                    fillRect(cx + bx + spin % 3 - 2, baseY - 74, cx + bx + spin % 3 + 2, baseY - 42, 0xFF8C8C9A.toInt())
                }
                fillRect(cx - 16, baseY - 54, cx + 16, baseY - 48, 0xFFD99A26.toInt())
            }
            WeaponType.PLASMA -> {
                // Futuristic UAC plasma rifle with glowing cyan core
                fillRect(cx - 18, baseY - 44, cx + 18, RENDER_HEIGHT - 1, 0xFF334155.toInt())
                fillRect(cx - 13, baseY - 66, cx + 13, baseY - 36, 0xFF475569.toInt())
                fillRect(cx - 7, baseY - 72, cx + 7, baseY - 44, 0xFF0EA5E9.toInt())
                fillRect(cx - 3, baseY - 70, cx + 3, baseY - 48, 0xFFBAE6FD.toInt())
            }
        }
    }

    private fun applyScreenFlash() {
        val redAmt = damageFlash.coerceIn(0f, 0.7f)
        val goldAmt = pickupFlash.coerceIn(0f, 0.35f)
        for (i in pixels.indices) {
            val c = pixels[i]
            var r = (c ushr 16) and 0xFF
            var g = (c ushr 8) and 0xFF
            var b = c and 0xFF
            if (redAmt > 0.01f) {
                r = (r + (235 - r) * redAmt).toInt()
                g = (g * (1f - redAmt * 0.75f)).toInt()
                b = (b * (1f - redAmt * 0.75f)).toInt()
            }
            if (goldAmt > 0.01f) {
                r = (r + (255 - r) * goldAmt).toInt()
                g = (g + (215 - g) * goldAmt).toInt()
            }
            pixels[i] = packArgb(r, g, b)
        }
    }

    private fun shadeColor(color: Int, factor: Float): Int {
        val r = (((color ushr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = (((color ushr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun packArgb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or ((r.coerceIn(0, 255)) shl 16) or ((g.coerceIn(0, 255)) shl 8) or (b.coerceIn(0, 255))

    private fun distSq(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        return dx * dx + dy * dy
    }

    private fun findInteractPrompt(): String? {
        val dirX = cos(playerAngle)
        val dirY = sin(playerAngle)
        for (step in 1..13) {
            val dist = step * 0.13f
            val tx = (playerX + dirX * dist).toInt()
            val ty = (playerY + dirY * dist).toInt()
            if (tx !in 0 until level.width || ty !in 0 until level.height) break
            val cell = level.grid[ty][tx]
            if (cell == ProceduralTextures.WALL_EXIT) {
                return "ÇIKIŞ ŞALTERİNE BAS [KULLAN]"
            }
            val door = doors[packCell(tx, ty)]
            if (door != null && door.openAmount < 0.7f) {
                return when (door.requiredKey) {
                    KeyColor.BLUE -> if (ownedKeys.contains(KeyColor.BLUE)) "MAVİ KAPIYI AÇ [KULLAN]" else "KİLİTLİ: MAVİ KART GEREKLİ"
                    KeyColor.RED -> if (ownedKeys.contains(KeyColor.RED)) "KIRMIZI KAPIYI AÇ [KULLAN]" else "KİLİTLİ: KIRMIZI KART GEREKLİ"
                    KeyColor.YELLOW -> if (ownedKeys.contains(KeyColor.YELLOW)) "SARI KAPIYI AÇ [KULLAN]" else "KİLİTLİ: SARI KART GEREKLİ"
                    null -> "KAPIYI AÇ [KULLAN]"
                }
            }
            if (cell > 0) break
        }
        return null
    }

    private fun buildHudSnapshot(): HudSnapshot {
        val flatGrid = IntArray(level.width * level.height)
        for (y in 0 until level.height) {
            for (x in 0 until level.width) {
                flatGrid[y * level.width + x] = level.grid[y][x]
            }
        }
        val doorAmounts = doors.mapValues { it.value.openAmount }
        val activeEnemies = enemies.filter { it.alive }.map { it.x to it.y }

        return HudSnapshot(
            levelId = level.id,
            levelCode = level.code,
            levelTitle = level.titleTr,
            phase = phase,
            health = health,
            armor = armor,
            activeWeapon = activeWeapon,
            ownedWeapons = ownedWeapons.toSet(),
            bullets = bullets,
            shells = shells,
            cells = cells,
            keys = ownedKeys.toSet(),
            kills = killsCount,
            totalKills = totalKills,
            items = itemsCount,
            totalItems = totalItems,
            secrets = discoveredSecrets.size,
            totalSecrets = totalSecrets,
            elapsedSec = elapsedTimeSec.toInt(),
            fps = fpsCounter,
            faceMood = faceMood,
            damageFlash = damageFlash,
            pickupFlash = pickupFlash,
            statusMessage = statusMessage,
            interactPrompt = if (phase == GamePhase.PLAYING) findInteractPrompt() else null,
            automap = AutomapSnapshot(
                gridWidth = level.width,
                gridHeight = level.height,
                wallCells = flatGrid,
                explored = explored.copyOf(),
                doorOpenAmounts = doorAmounts,
                playerX = playerX,
                playerY = playerY,
                playerAngle = playerAngle,
                enemyPositions = activeEnemies
            )
        )
    }
}
