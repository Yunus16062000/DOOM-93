package com.example.game

enum class Difficulty(
    val id: Int,
    val titleTr: String,
    val subtitleTr: String,
    val damageScale: Float,
    val enemySpeedScale: Float,
    val ammoMultiplier: Float
) {
    EASY(0, "Çaylak Denizci", "Az hasar • Bol cephane", 0.6f, 0.85f, 1.5f),
    NORMAL(1, "Klasik Mars", "Orijinal DOOM 1 dengesi", 1.0f, 1.0f, 1.0f),
    HARD(2, "Ultra-Şiddet", "Acımasız düşmanlar • Yüksek hız", 1.35f, 1.2f, 1.0f),
    NIGHTMARE(3, "Kabus!", "Maksimum adrenalin • Hayatta kal", 1.65f, 1.35f, 1.25f);

    companion object {
        fun fromId(id: Int): Difficulty = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

enum class AmmoType {
    NONE,
    BULLETS,
    SHELLS,
    CELLS
}

enum class WeaponType(
    val slot: Int,
    val titleTr: String,
    val ammoType: AmmoType,
    val ammoPerShot: Int,
    val damage: Int,
    val cooldownSec: Float,
    val range: Float,
    val pellets: Int,
    val spreadRad: Float,
    val isProjectile: Boolean = false
) {
    FIST(1, "MUŞTA", AmmoType.NONE, 0, 32, 0.36f, 1.45f, 1, 0f),
    PISTOL(2, "TABANCA", AmmoType.BULLETS, 1, 18, 0.28f, 18f, 1, 0.02f),
    SHOTGUN(3, "POMPALI", AmmoType.SHELLS, 1, 14, 0.72f, 14f, 7, 0.14f),
    CHAINGUN(4, "MAKİNELİ", AmmoType.BULLETS, 1, 15, 0.105f, 18f, 1, 0.055f),
    PLASMA(5, "PLAZMA", AmmoType.CELLS, 1, 44, 0.15f, 22f, 1, 0.015f, isProjectile = true)
}

enum class KeyColor {
    BLUE,
    RED,
    YELLOW
}

enum class EnemyType(
    val maxHp: Int,
    val speed: Float,
    val radius: Float,
    val attackRange: Float,
    val attackCooldownSec: Float,
    val damage: Int,
    val isRanged: Boolean,
    val isProjectileAttacker: Boolean,
    val spriteScale: Float,
    val titleTr: String
) {
    ZOMBIEMAN(35, 1.45f, 0.30f, 10f, 1.35f, 9, true, false, 0.92f, "Zombi Asker"),
    SHOTGUNNER(55, 1.55f, 0.30f, 9f, 1.55f, 16, true, false, 0.95f, "Pompalı Çavuş"),
    IMP(75, 1.75f, 0.32f, 11f, 1.45f, 14, true, true, 0.95f, "İblis Imp"),
    PINKY(120, 2.55f, 0.36f, 1.15f, 0.9f, 20, false, false, 1.02f, "Pembe Şeytan"),
    CACODEMON(210, 1.45f, 0.42f, 13f, 1.65f, 22, true, true, 1.08f, "Cacodemon"),
    BARON(480, 1.55f, 0.46f, 13f, 1.25f, 30, true, true, 1.22f, "Cehennem Baronu")
}

enum class PickupType(val isItemCounted: Boolean) {
    STIMPACK(true),
    MEDIKIT(true),
    GREEN_ARMOR(true),
    MEGA_ARMOR(true),
    AMMO_CLIP(true),
    SHELLS(true),
    CELLS(true),
    SHOTGUN_WEAPON(false),
    CHAINGUN_WEAPON(false),
    PLASMA_WEAPON(false),
    BLUE_KEY(true),
    RED_KEY(true),
    YELLOW_KEY(true)
}

data class EnemyEntity(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var hp: Int = type.maxHp,
    var alive: Boolean = true,
    var alert: Boolean = false,
    var attackTimer: Float = 0.6f,
    var painTimer: Float = 0f,
    var animTimer: Float = 0f,
    var deathTimer: Float = 0f
)

data class PickupEntity(
    val id: Int,
    val type: PickupType,
    val x: Float,
    val y: Float,
    var collected: Boolean = false
)

data class BarrelEntity(
    val id: Int,
    val x: Float,
    val y: Float,
    var hp: Int = 25,
    var exploded: Boolean = false
)

data class ProjectileEntity(
    val id: Int,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int,
    val fromPlayer: Boolean,
    val isPlasma: Boolean,
    var alive: Boolean = true
)

data class ParticleEntity(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val color: Int,
    var life: Float
)

data class DoorState(
    val gx: Int,
    val gy: Int,
    val requiredKey: KeyColor? = null,
    var openAmount: Float = 0f, // 0f closed .. 1f fully open
    var opening: Boolean = false,
    var holdTimer: Float = 0f
)

enum class GamePhase {
    PLAYING,
    PAUSED,
    DEAD,
    LEVEL_COMPLETE
}

data class LevelDefinition(
    val id: Int,
    val code: String,
    val titleTr: String,
    val subtitleTr: String,
    val grid: Array<IntArray>,
    val nukageTiles: Set<Long>,
    val secretTiles: Set<Long>,
    val spawnX: Float,
    val spawnY: Float,
    val spawnAngle: Float,
    val doors: List<DoorState>,
    val enemies: List<EnemyEntity>,
    val pickups: List<PickupEntity>,
    val barrels: List<BarrelEntity>
) {
    val width: Int get() = grid[0].size
    val height: Int get() = grid.size
}

data class AutomapSnapshot(
    val gridWidth: Int,
    val gridHeight: Int,
    val wallCells: IntArray,
    val explored: BooleanArray,
    val doorOpenAmounts: Map<Long, Float>,
    val playerX: Float,
    val playerY: Float,
    val playerAngle: Float,
    val enemyPositions: List<Pair<Float, Float>>
)

data class HudSnapshot(
    val levelId: Int = 1,
    val levelCode: String = "E1M1",
    val levelTitle: String = "HANGAR",
    val phase: GamePhase = GamePhase.PLAYING,
    val health: Int = 100,
    val armor: Int = 0,
    val activeWeapon: WeaponType = WeaponType.PISTOL,
    val ownedWeapons: Set<WeaponType> = setOf(WeaponType.FIST, WeaponType.PISTOL),
    val bullets: Int = 50,
    val shells: Int = 0,
    val cells: Int = 0,
    val keys: Set<KeyColor> = emptySet(),
    val kills: Int = 0,
    val totalKills: Int = 0,
    val items: Int = 0,
    val totalItems: Int = 0,
    val secrets: Int = 0,
    val totalSecrets: Int = 0,
    val elapsedSec: Int = 0,
    val fps: Int = 60,
    val faceMood: Int = 0, // 0=normal, 1=lookLeft, 2=lookRight, 3=grin, 4=pain, 5=dead
    val damageFlash: Float = 0f,
    val pickupFlash: Float = 0f,
    val statusMessage: String = "",
    val interactPrompt: String? = null,
    val automap: AutomapSnapshot? = null
) {
    val currentAmmo: Int
        get() = when (activeWeapon.ammoType) {
            AmmoType.NONE -> -1
            AmmoType.BULLETS -> bullets
            AmmoType.SHELLS -> shells
            AmmoType.CELLS -> cells
        }
}

fun packCell(gx: Int, gy: Int): Long = (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)
