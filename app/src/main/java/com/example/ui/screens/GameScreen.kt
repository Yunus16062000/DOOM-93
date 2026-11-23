package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEngine
import com.example.data.GameSettings
import com.example.game.Difficulty
import com.example.game.GamePhase
import com.example.game.HudSnapshot
import com.example.game.LevelRepository
import com.example.game.RaycasterEngine
import com.example.ui.components.ActionButtonsPad
import com.example.ui.components.AutomapView
import com.example.ui.components.DoomStatusBar
import com.example.ui.components.LookDragSurface
import com.example.ui.components.VirtualThumbstick
import com.example.ui.theme.AshText
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.BloodDark
import com.example.ui.theme.BunkerSlate
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.DoomGold
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.HellfireAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NukageGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SteelBorder
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    levelId: Int,
    difficultyId: Int,
    settings: GameSettings,
    soundEngine: RetroSoundEngine,
    onRecordLevelClear: (levelId: Int, elapsedSec: Int, killsPct: Int) -> Unit,
    onNextLevel: (nextLevelId: Int) -> Unit,
    onExitToMenu: () -> Unit
) {
    val context = LocalContext.current
    var restartNonce by remember { mutableIntStateOf(0) }
    var fullAutomapOpen by remember { mutableStateOf(false) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    LaunchedEffect(settings.sfxVolume) {
        soundEngine.setVolume(settings.sfxVolume)
    }

    val engine = remember(levelId, difficultyId, restartNonce) {
        val diff = Difficulty.fromId(difficultyId)
        val level = LevelRepository.loadLevel(levelId, diff)
        RaycasterEngine(
            level = level,
            difficulty = diff,
            soundEngine = soundEngine,
            onVibrate = { ms ->
                if (settings.vibrationEnabled && vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(ms)
                    }
                }
            },
            onLevelComplete = { snap ->
                val killsPct = if (snap.totalKills > 0) (snap.kills * 100f / snap.totalKills).roundToInt() else 100
                onRecordLevelClear(snap.levelId, snap.elapsedSec, killsPct)
            }
        )
    }

    var hud by remember(engine) { mutableStateOf(engine.step(0.016f)) }
    var frameTick by remember(engine) { mutableLongStateOf(0L) }

    LaunchedEffect(engine) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNanos = nowNanos
            hud = engine.step(dt)
            frameTick = nowNanos
        }
    }

    BackHandler {
        when {
            fullAutomapOpen -> fullAutomapOpen = false
            hud.phase == GamePhase.PLAYING -> engine.setPaused(true)
            hud.phase == GamePhase.PAUSED -> engine.setPaused(false)
            else -> onExitToMenu()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        val isLandscape = maxWidth >= maxHeight

        if (isLandscape) {
            LandscapeGameLayout(
                engine = engine,
                hud = hud,
                frameTick = frameTick,
                settings = settings,
                fullAutomapOpen = fullAutomapOpen,
                onToggleAutomap = { fullAutomapOpen = !fullAutomapOpen },
                onPause = { engine.setPaused(true) }
            )
        } else {
            PortraitGameLayout(
                engine = engine,
                hud = hud,
                frameTick = frameTick,
                settings = settings,
                fullAutomapOpen = fullAutomapOpen,
                onToggleAutomap = { fullAutomapOpen = !fullAutomapOpen },
                onPause = { engine.setPaused(true) }
            )
        }

        // Phase Overlays (Paused / Dead / Level Complete)
        when (hud.phase) {
            GamePhase.PAUSED -> PauseOverlay(
                levelCode = hud.levelCode,
                levelTitle = hud.levelTitle,
                onResume = { engine.setPaused(false) },
                onRestart = {
                    fullAutomapOpen = false
                    restartNonce++
                },
                onExit = onExitToMenu
            )
            GamePhase.DEAD -> DeathOverlay(
                hud = hud,
                onRestart = {
                    fullAutomapOpen = false
                    restartNonce++
                },
                onExit = onExitToMenu
            )
            GamePhase.LEVEL_COMPLETE -> LevelCompleteOverlay(
                hud = hud,
                hasNextLevel = levelId < 3,
                onNextLevel = { onNextLevel(levelId + 1) },
                onRestart = {
                    fullAutomapOpen = false
                    restartNonce++
                },
                onExit = onExitToMenu
            )
            GamePhase.PLAYING -> {}
        }
    }
}

@Composable
private fun PortraitGameLayout(
    engine: RaycasterEngine,
    hud: HudSnapshot,
    frameTick: Long,
    settings: GameSettings,
    fullAutomapOpen: Boolean,
    onToggleAutomap: () -> Unit,
    onPause: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top HUD Strip
        TopGameStrip(
            hud = hud,
            showFps = settings.showFps,
            fullAutomapOpen = fullAutomapOpen,
            onToggleAutomap = onToggleAutomap,
            onPause = onPause
        )

        // Framed 16:10 3D Raycaster Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, SteelBorder, RoundedCornerShape(6.dp))
        ) {
            ViewportContent(
                engine = engine,
                hud = hud,
                frameTick = frameTick,
                settings = settings,
                fullAutomapOpen = fullAutomapOpen
            )
        }

        // Control Deck in the ergonomic lower third (never obscures 3D view!)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Also allow dragging anywhere in the middle empty control area to look around
            LookDragSurface(
                sensitivity = settings.lookSensitivity,
                onRotateDelta = { engine.rotatePlayer(it) },
                modifier = Modifier.fillMaxSize()
            )

            VirtualThumbstick(
                onMoveVector = { fwd, strafe ->
                    engine.moveForwardInput = fwd
                    engine.strafeRightInput = strafe
                },
                modifier = Modifier.align(Alignment.CenterStart)
            )

            ActionButtonsPad(
                hasInteractPrompt = hud.interactPrompt != null,
                onFireChanged = { engine.fireHeld = it },
                onUsePressed = { engine.triggerUseAction() },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Classic Doom Status Bar
        DoomStatusBar(
            hud = hud,
            onSelectWeapon = { engine.selectWeapon(it) }
        )
    }
}

@Composable
private fun LandscapeGameLayout(
    engine: RaycasterEngine,
    hud: HudSnapshot,
    frameTick: Long,
    settings: GameSettings,
    fullAutomapOpen: Boolean,
    onToggleAutomap: () -> Unit,
    onPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Centered crisp 16:10 viewport
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(16f / 10f)
                .align(Alignment.Center)
        ) {
            ViewportContent(
                engine = engine,
                hud = hud,
                frameTick = frameTick,
                settings = settings,
                fullAutomapOpen = fullAutomapOpen
            )
        }

        // Floating top bar
        Box(modifier = Modifier.statusBarsPadding().align(Alignment.TopCenter)) {
            TopGameStrip(
                hud = hud,
                showFps = settings.showFps,
                fullAutomapOpen = fullAutomapOpen,
                onToggleAutomap = onToggleAutomap,
                onPause = onPause
            )
        }

        // Floating thumbstick bottom-left
        VirtualThumbstick(
            onMoveVector = { fwd, strafe ->
                engine.moveForwardInput = fwd
                engine.strafeRightInput = strafe
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 14.dp)
        )

        // Compact status bar centered bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 420.dp)
                .padding(bottom = 4.dp)
        ) {
            DoomStatusBar(
                hud = hud,
                onSelectWeapon = { engine.selectWeapon(it) }
            )
        }

        // Floating fire/use bottom-right
        ActionButtonsPad(
            hasInteractPrompt = hud.interactPrompt != null,
            onFireChanged = { engine.fireHeld = it },
            onUsePressed = { engine.triggerUseAction() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 14.dp)
        )
    }
}

@Composable
private fun TopGameStrip(
    hud: HudSnapshot,
    showFps: Boolean,
    fullAutomapOpen: Boolean,
    onToggleAutomap: () -> Unit,
    onPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "${hud.levelCode}: ${hud.levelTitle}",
                fontFamily = OrbitronFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DoomGold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LEŞ ${hud.kills}/${hud.totalKills}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = AshText
                )
                Text(
                    text = formatSeconds(hud.elapsedSec),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = NukageGreen
                )
                if (showFps) {
                    Text(
                        text = "${hud.fps} FPS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = HellfireAmber
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onToggleAutomap,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (fullAutomapOpen) NukageGreen.copy(alpha = 0.25f) else GunmetalSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (fullAutomapOpen) NukageGreen else SteelBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .testTag("automap_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = "Haritayı aç/kapat",
                    tint = if (fullAutomapOpen) NukageGreen else ChalkWhite
                )
            }

            IconButton(
                onClick = onPause,
                modifier = Modifier
                    .size(48.dp)
                    .background(GunmetalSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, SteelBorder, RoundedCornerShape(8.dp))
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Oyunu duraklat",
                    tint = ChalkWhite
                )
            }
        }
    }
}

@Composable
private fun ViewportContent(
    engine: RaycasterEngine,
    hud: HudSnapshot,
    frameTick: Long,
    settings: GameSettings,
    fullAutomapOpen: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Software Raycaster Bitmap Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read frameTick so Compose redraws every tick
            if (frameTick >= 0L) {
                val bmp = engine.frameBitmap.asImageBitmap()
                drawImage(
                    image = bmp,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bmp.width, bmp.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.None
                )
            }
        }

        // Touch Look surface right over the 3D view
        LookDragSurface(
            sensitivity = settings.lookSensitivity,
            onRotateDelta = { engine.rotatePlayer(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Classic retro Crosshair
        if (settings.showCrosshair && !fullAutomapOpen) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val arm = 8.dp.toPx()
                val gap = 3.dp.toPx()
                drawLine(NukageGreen.copy(alpha = 0.85f), Offset(cx - arm, cy), Offset(cx - gap, cy), strokeWidth = 2.5f)
                drawLine(NukageGreen.copy(alpha = 0.85f), Offset(cx + gap, cy), Offset(cx + arm, cy), strokeWidth = 2.5f)
                drawLine(NukageGreen.copy(alpha = 0.85f), Offset(cx, cy - arm), Offset(cx, cy - gap), strokeWidth = 2.5f)
                drawLine(NukageGreen.copy(alpha = 0.85f), Offset(cx, cy + gap), Offset(cx, cy + arm), strokeWidth = 2.5f)
            }
        }

        // Corner radar minimap
        val automap = hud.automap
        if (automap != null && settings.showMinimap && !fullAutomapOpen) {
            AutomapView(
                automap = automap,
                showEnemies = false,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(96.dp)
            )
        }

        // Fullscreen Automap overlay
        if (automap != null && fullAutomapOpen) {
            AutomapView(
                automap = automap,
                showEnemies = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }

        // Status Toast at top-left
        if (hud.statusMessage.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(ObsidianVoid.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                    .border(1.dp, DoomGold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = hud.statusMessage,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = DoomGold
                )
            }
        }

        // Interactive Door / Exit Prompt near bottom-center of viewport
        val prompt = hud.interactPrompt
        if (prompt != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(BloodDark.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .border(1.dp, DoomGold, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = prompt,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChalkWhite
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    levelCode: String,
    levelTitle: String,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(0.86f)
                .background(BunkerSlate, RoundedCornerShape(10.dp))
                .border(2.dp, DoomGold, RoundedCornerShape(10.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "OYUN DURAKLATILDI",
                fontFamily = OrbitronFontFamily,
                fontSize = 20.sp,
                color = DoomGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$levelCode • $levelTitle",
                style = MaterialTheme.typography.bodyMedium,
                color = AshText
            )

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("resume_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("DEVAM ET", fontFamily = OrbitronFontFamily, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_restart_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = ChalkWhite)
                Spacer(Modifier.width(6.dp))
                Text("BÖLÜMÜ YENİDEN BAŞLAT", color = ChalkWhite)
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_exit_button")
            ) {
                Text("ANA MENÜYE DÖN", color = AshText)
            }
        }
    }
}

@Composable
private fun DeathOverlay(
    hud: HudSnapshot,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC5A0404)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(0.86f)
                .background(ObsidianVoid.copy(alpha = 0.94f), RoundedCornerShape(10.dp))
                .border(2.dp, BloodCrimson, RoundedCornerShape(10.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ÖLDÜN!",
                fontFamily = OrbitronFontFamily,
                fontSize = 34.sp,
                color = BloodCrimson
            )
            Text(
                text = "${hud.levelCode} haritasında iblislere yenik düştün.",
                style = MaterialTheme.typography.bodyLarge,
                color = AshText,
                textAlign = TextAlign.Center
            )

            TallyRow("KATLİAM", "${hud.kills} / ${hud.totalKills}")
            TallyRow("SÜRE", formatSeconds(hud.elapsedSec))

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("death_restart_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("YENİDEN DENE", fontFamily = OrbitronFontFamily, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("death_exit_button")
            ) {
                Text("ANA MENÜ", color = ChalkWhite)
            }
        }
    }
}

@Composable
private fun LevelCompleteOverlay(
    hud: HudSnapshot,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val killsPct = if (hud.totalKills > 0) (hud.kills * 100f / hud.totalKills).roundToInt() else 100
    val itemsPct = if (hud.totalItems > 0) (hud.items * 100f / hud.totalItems).roundToInt() else 100
    val secretsPct = if (hud.totalSecrets > 0) (hud.secrets * 100f / hud.totalSecrets).roundToInt() else 100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.88f)
                .background(BunkerSlate, RoundedCornerShape(10.dp))
                .border(2.dp, DoomGold, RoundedCornerShape(10.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (hasNextLevel) "BÖLÜM TAMAMLANDI!" else "EPISODE 1 ZAFERİ!",
                fontFamily = OrbitronFontFamily,
                fontSize = 21.sp,
                color = DoomGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${hud.levelCode} • ${hud.levelTitle}",
                style = MaterialTheme.typography.titleMedium,
                color = NukageGreen
            )

            Spacer(Modifier.height(4.dp))
            TallyRow("KATLİAM", "%$killsPct (${hud.kills}/${hud.totalKills})")
            TallyRow("EŞYALAR", "%$itemsPct (${hud.items}/${hud.totalItems})")
            TallyRow("GİZLİLER", "%$secretsPct (${hud.secrets}/${hud.totalSecrets})")
            TallyRow("SÜRE", formatSeconds(hud.elapsedSec))
            Spacer(Modifier.height(6.dp))

            if (hasNextLevel) {
                Button(
                    onClick = onNextLevel,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("next_level_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SONRAKİ BÖLÜM", fontFamily = OrbitronFontFamily, fontSize = 15.sp)
                }
            } else {
                Text(
                    text = "Cehennem Baronu'nu devirdin ve Mars üssünü kurtardın!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChalkWhite,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("victory_restart_button")
            ) {
                Text("TEKRAR OYNA", color = ChalkWhite)
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("victory_exit_button")
            ) {
                Text("ANA MENÜ", color = AshText)
            }
        }
    }
}

@Composable
private fun TallyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianVoid.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .border(1.dp, SteelBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = AshText
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DoomGold
        )
    }
}
