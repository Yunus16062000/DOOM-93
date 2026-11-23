package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.GameViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TitleScreen
import com.example.ui.theme.Doom93Theme

object DoomRoutes {
    const val TITLE = "title"
    const val SETTINGS = "settings"
    const val GAME = "game/{levelId}/{difficultyId}"

    fun gameRoute(levelId: Int, difficultyId: Int): String = "game/$levelId/$difficultyId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            Doom93Theme {
                val vm: GameViewModel = viewModel()
                val settings by vm.settings.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = DoomRoutes.TITLE
                ) {
                    composable(DoomRoutes.TITLE) {
                        TitleScreen(
                            settings = settings,
                            initialDifficultyId = vm.selectedDifficultyId,
                            onStartGame = { levelId, diffId ->
                                vm.selectDifficulty(diffId)
                                navController.navigate(DoomRoutes.gameRoute(levelId, diffId))
                            },
                            onOpenSettings = {
                                navController.navigate(DoomRoutes.SETTINGS)
                            }
                        )
                    }

                    composable(DoomRoutes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onSensitivityChange = vm::updateSensitivity,
                            onSfxVolumeChange = vm::updateSfxVolume,
                            onToggleMinimap = vm::setShowMinimap,
                            onToggleCrosshair = vm::setShowCrosshair,
                            onToggleFps = vm::setShowFps,
                            onToggleVibration = vm::setVibrationEnabled,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = DoomRoutes.GAME,
                        arguments = listOf(
                            navArgument("levelId") { type = NavType.IntType },
                            navArgument("difficultyId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1
                        val difficultyId = backStackEntry.arguments?.getInt("difficultyId") ?: 1

                        GameScreen(
                            levelId = levelId,
                            difficultyId = difficultyId,
                            settings = settings,
                            soundEngine = vm.soundEngine,
                            onRecordLevelClear = vm::recordLevelClear,
                            onNextLevel = { nextId ->
                                navController.navigate(DoomRoutes.gameRoute(nextId, difficultyId)) {
                                    popUpTo(DoomRoutes.TITLE)
                                }
                            },
                            onExitToMenu = {
                                navController.popBackStack(DoomRoutes.TITLE, inclusive = false)
                            }
                        )
                    }
                }
            }
        }
    }
}
