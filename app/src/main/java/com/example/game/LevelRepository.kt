package com.example.game

/**
 * 3 Handcrafted Episode 1 levels inspired by 1993 DOOM:
 * Legend:
 *  '1' = UAC Tech Wall, '2' = Brown Brick, '3' = Nukage Pipe Wall, '4' = Hell Flesh Wall
 *  'D' = Standard Door, 'b' = Blue Key Door, 'r' = Red Key Door, 'y' = Yellow Key Door
 *  'X' = Level Exit Switch Wall
 *  '.' = Open Floor, '~' = Toxic Nukage Floor, 'S' = Secret Sector Floor
 *  'P' = Player Spawn
 *  Enemies: 'z' = Zombieman, 's' = Shotgunner, 'i' = Imp, 'p' = Pinky, 'c' = Cacodemon, 'B' = Baron
 *  Pickups: 'h' = Stimpack, 'H' = Medikit, 'a' = Green Armor, 'A' = Megaarmor
 *           'm' = Bullets, 'q' = Shells, 'e' = Cells
 *           'G' = Shotgun, 'C' = Chaingun, 'L' = Plasma Rifle
 *           '1'=wall so keys are 'k'=Blue Key, 'K'=Red Key, 'Y'=Yellow Key
 *  'o' = Explosive Barrel
 */
object LevelRepository {

    data class LevelMeta(
        val id: Int,
        val code: String,
        val titleTr: String,
        val subtitleTr: String,
        val parTimeSec: Int
    )

    val episodeLevels = listOf(
        LevelMeta(1, "E1M1", "HANGAR", "Mars UAC İniş İstasyonu • İlk Temas", 75),
        LevelMeta(2, "E1M2", "NÜKLEER SANTRAL", "Mavi Kart • Zehirli Reaktör Labirenti", 120),
        LevelMeta(3, "E1M3", "CEHENNEM GEÇİDİ", "Kızıl Mabed • Cehennem Baronu Hesaplaşması", 150)
    )

    fun loadLevel(levelId: Int, difficulty: Difficulty): LevelDefinition {
        return when (levelId) {
            2 -> parseMap(
                id = 2,
                code = "E1M2",
                titleTr = "NÜKLEER SANTRAL",
                subtitleTr = "Mavi Kartı Bul ve Reaktörü Aş",
                spawnAngle = 0f,
                ascii = E1M2_MAP,
                difficulty = difficulty
            )
            3 -> parseMap(
                id = 3,
                code = "E1M3",
                titleTr = "CEHENNEM GEÇİDİ",
                subtitleTr = "Baron of Hell'i Yok Et ve Çıkışa Ulaş",
                spawnAngle = -Math.PI.toFloat() / 2f,
                ascii = E1M3_MAP,
                difficulty = difficulty
            )
            else -> parseMap(
                id = 1,
                code = "E1M1",
                titleTr = "HANGAR",
                subtitleTr = "UAC Mars Üssü • Bölüm 1",
                spawnAngle = -Math.PI.toFloat() / 2f,
                ascii = E1M1_MAP,
                difficulty = difficulty
            )
        }
    }

    private fun parseMap(
        id: Int,
        code: String,
        titleTr: String,
        subtitleTr: String,
        spawnAngle: Float,
        ascii: List<String>,
        difficulty: Difficulty
    ): LevelDefinition {
        val h = ascii.size
        val w = ascii.maxOf { it.length }
        val grid = Array(h) { IntArray(w) }
        val nukage = mutableSetOf<Long>()
        val secrets = mutableSetOf<Long>()
        val doors = mutableListOf<DoorState>()
        val enemies = mutableListOf<EnemyEntity>()
        val pickups = mutableListOf<PickupEntity>()
        val barrels = mutableListOf<BarrelEntity>()
        var spawnX = 2.5f
        var spawnY = 2.5f
        var enemyId = 1
        var pickupId = 1
        var barrelId = 1

        for (y in 0 until h) {
            val row = ascii[y]
            for (x in 0 until w) {
                val ch = if (x < row.length) row[x] else '1'
                val cx = x + 0.5f
                val cy = y + 0.5f
                when (ch) {
                    '1' -> grid[y][x] = ProceduralTextures.WALL_UAC_TECH
                    '2' -> grid[y][x] = ProceduralTextures.WALL_BROWN_BRICK
                    '3' -> grid[y][x] = ProceduralTextures.WALL_NUKAGE_PIPE
                    '4' -> grid[y][x] = ProceduralTextures.WALL_HELL_FLESH
                    'X' -> grid[y][x] = ProceduralTextures.WALL_EXIT
                    'D' -> {
                        grid[y][x] = ProceduralTextures.WALL_DOOR
                        doors += DoorState(x, y, null)
                    }
                    'b' -> {
                        grid[y][x] = ProceduralTextures.WALL_DOOR_BLUE
                        doors += DoorState(x, y, KeyColor.BLUE)
                    }
                    'r' -> {
                        grid[y][x] = ProceduralTextures.WALL_DOOR_RED
                        doors += DoorState(x, y, KeyColor.RED)
                    }
                    'y' -> {
                        grid[y][x] = ProceduralTextures.WALL_DOOR_YELLOW
                        doors += DoorState(x, y, KeyColor.YELLOW)
                    }
                    '~' -> nukage += packCell(x, y)
                    'S' -> secrets += packCell(x, y)
                    'P' -> {
                        spawnX = cx
                        spawnY = cy
                    }
                    'z' -> enemies += EnemyEntity(enemyId++, EnemyType.ZOMBIEMAN, cx, cy)
                    's' -> enemies += EnemyEntity(enemyId++, EnemyType.SHOTGUNNER, cx, cy)
                    'i' -> enemies += EnemyEntity(enemyId++, EnemyType.IMP, cx, cy)
                    'p' -> enemies += EnemyEntity(enemyId++, EnemyType.PINKY, cx, cy)
                    'c' -> enemies += EnemyEntity(enemyId++, EnemyType.CACODEMON, cx, cy)
                    'B' -> enemies += EnemyEntity(enemyId++, EnemyType.BARON, cx, cy)
                    'h' -> pickups += PickupEntity(pickupId++, PickupType.STIMPACK, cx, cy)
                    'H' -> pickups += PickupEntity(pickupId++, PickupType.MEDIKIT, cx, cy)
                    'a' -> pickups += PickupEntity(pickupId++, PickupType.GREEN_ARMOR, cx, cy)
                    'A' -> pickups += PickupEntity(pickupId++, PickupType.MEGA_ARMOR, cx, cy)
                    'm' -> pickups += PickupEntity(pickupId++, PickupType.AMMO_CLIP, cx, cy)
                    'q' -> pickups += PickupEntity(pickupId++, PickupType.SHELLS, cx, cy)
                    'e' -> pickups += PickupEntity(pickupId++, PickupType.CELLS, cx, cy)
                    'G' -> pickups += PickupEntity(pickupId++, PickupType.SHOTGUN_WEAPON, cx, cy)
                    'C' -> pickups += PickupEntity(pickupId++, PickupType.CHAINGUN_WEAPON, cx, cy)
                    'L' -> pickups += PickupEntity(pickupId++, PickupType.PLASMA_WEAPON, cx, cy)
                    'k' -> pickups += PickupEntity(pickupId++, PickupType.BLUE_KEY, cx, cy)
                    'K' -> pickups += PickupEntity(pickupId++, PickupType.RED_KEY, cx, cy)
                    'Y' -> pickups += PickupEntity(pickupId++, PickupType.YELLOW_KEY, cx, cy)
                    'o' -> barrels += BarrelEntity(barrelId++, cx, cy)
                }
            }
        }

        // Extra difficulty tuning: on Hard/Nightmare upgrade some Zombiemen to Shotgunners/Imps
        val tunedEnemies = if (difficulty.id >= Difficulty.HARD.id) {
            enemies.mapIndexed { idx, e ->
                if (e.type == EnemyType.ZOMBIEMAN && idx % 2 == 0) {
                    e.copy(type = EnemyType.SHOTGUNNER, hp = EnemyType.SHOTGUNNER.maxHp)
                } else if (difficulty == Difficulty.NIGHTMARE && e.type == EnemyType.IMP && idx % 3 == 0) {
                    e.copy(type = EnemyType.PINKY, hp = EnemyType.PINKY.maxHp)
                } else {
                    e
                }
            }
        } else {
            enemies
        }

        return LevelDefinition(
            id = id,
            code = code,
            titleTr = titleTr,
            subtitleTr = subtitleTr,
            grid = grid,
            nukageTiles = nukage,
            secretTiles = secrets,
            spawnX = spawnX,
            spawnY = spawnY,
            spawnAngle = spawnAngle,
            doors = doors,
            enemies = tunedEnemies,
            pickups = pickups,
            barrels = barrels
        )
    }

    // E1M1: Hangar (22x22)
    private val E1M1_MAP = listOf(
        "1111111111111111111111",
        "1..a..1.....o....i..X1",
        "1..S..D..z.....s....11",
        "1..m..1.....q.......11",
        "111111111D11111111D111",
        "1.......1.....1......1",
        "1.i..o..1.~~~.1..s.H.1",
        "1.......D.~~~.D......1",
        "1..h....1.~~~.1..i...1",
        "11111D11111111111D1111",
        "2.......2.....2......2",
        "2..G.q..2..z..2..C.m.2",
        "2.......D.....D......2",
        "2..o.z..2..h..2..S.a.2",
        "222D2222222D2222222222",
        "1........1...........1",
        "1..z..o..1..m.....s..1",
        "1........D...........1",
        "1....h...1.....o.....1",
        "1....P...1..q.....H..1",
        "1111111111111111111111"
    )

    // E1M2: Nuclear Plant (24x22) - Features Blue Keycard Wing + Explosive Barrel Depot
    private val E1M2_MAP = listOf(
        "111111111111111111111111",
        "1.H...i...1..o..o..i..X1",
        "1....o....b.....s.....11",
        "1.m...p...1..q.....H..11",
        "1111D1111111111111111111",
        "3........3~~~~3........3",
        "3..z..s..3~~~~3..p..i..3",
        "3........D~~~~D........3",
        "3..o..h..3~~~~3..h..q..3",
        "333D33333333333333D33333",
        "1.....1..........1.....1",
        "1.P.G.D..z..o..s.D..k..1",
        "1...m.1..........1..a..1",
        "11111111D111111D11111111",
        "2.......2......2.......2",
        "2..L.e..2..i...2..C.m..2",
        "2..S.A..D......D.......2",
        "2.......2..p...2..o..s.2",
        "222222222222222222222222"
    )

    // E1M3: Hell's Gate (24x22) - Red + Yellow keys, Cacodemons, Baron of Hell Boss!
    private val E1M3_MAP = listOf(
        "444444444444444444444444",
        "4..A...4........4...H.X4",
        "4..S...4...B....r......4",
        "4..e...4........4..c...4",
        "444D444444y4444444444444",
        "4..........4...........4",
        "4..c...o...4...i...p...4",
        "4..........D...........4",
        "4..H...q...4...o...K...4",
        "44444D44444444444D444444",
        "2........2....2........2",
        "2..s..i..D....D..c..m..2",
        "2........2....2........2",
        "2..Y..e..2..h.2..C..q..2",
        "2222222D2222222222D22222",
        "1.........1............1",
        "1..G..a...1...z....s...1",
        "1.........D............1",
        "1....P....1...o....H...1",
        "1....m....1............1",
        "111111111111111111111111"
    )
}
