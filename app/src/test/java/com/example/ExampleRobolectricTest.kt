package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.Difficulty
import com.example.game.LevelRepository
import com.example.game.ProceduralTextures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Doom 93", appName)
  }

  @Test
  fun `all episode 1 levels have valid spawns, enemies, and exits`() {
    for (levelId in 1..3) {
      val level = LevelRepository.loadLevel(levelId, Difficulty.NORMAL)
      val sx = level.spawnX.toInt()
      val sy = level.spawnY.toInt()
      assertEquals("Spawn must be open floor in E1M$levelId", 0, level.grid[sy][sx])
      assertTrue("E1M$levelId must have enemies", level.enemies.isNotEmpty())
      assertTrue("E1M$levelId must have pickups", level.pickups.isNotEmpty())
      val hasExit = level.grid.any { row -> row.any { it == ProceduralTextures.WALL_EXIT } }
      assertTrue("E1M$levelId must have an exit switch", hasExit)
    }
  }
}
