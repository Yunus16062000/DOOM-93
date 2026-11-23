package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameSettings
import com.example.game.Difficulty
import com.example.game.LevelRepository
import com.example.ui.theme.AshText
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.BloodDark
import com.example.ui.theme.BunkerSlate
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.DoomGold
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.HellfireAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NukageGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SteelBorder

@Composable
fun TitleScreen(
    settings: GameSettings,
    initialDifficultyId: Int,
    onStartGame: (levelId: Int, difficultyId: Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedLevelId by remember { mutableIntStateOf(1) }
    var selectedDifficulty by remember { mutableIntStateOf(initialDifficultyId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        // Atmospheric generated retro Mars Hellgate key art
        Image(
            painter = painterResource(id = R.drawable.img_doom_title_bg),
            contentDescription = "Mars Cehennemi kapak görseli",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.42f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ObsidianVoid.copy(alpha = 0.72f),
                            ObsidianVoid.copy(alpha = 0.55f),
                            ObsidianVoid.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 620.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with Settings button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BloodDark.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, BloodCrimson, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "EPISODE 1 • MARS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = DoomGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .background(GunmetalSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .border(1.dp, SteelBorder, RoundedCornerShape(8.dp))
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Ayarlar",
                        tint = ChalkWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Iconic DOOM 93 Header
            Text(
                text = "DOOM 93",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                letterSpacing = 4.sp,
                color = BloodCrimson
            )
            Text(
                text = "DIZ BOYU CEHENNEM • 2.5D RAYCASTER",
                style = MaterialTheme.typography.labelLarge,
                color = DoomGold,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Episode Levels + Difficulty picker
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                item {
                    Text(
                        text = "ZORLUK SEVİYESİ",
                        style = MaterialTheme.typography.labelLarge,
                        color = AshText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    DifficultySelectorRow(
                        selectedId = selectedDifficulty,
                        onSelect = { selectedDifficulty = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BÖLÜM SEÇİMİ",
                        style = MaterialTheme.typography.labelLarge,
                        color = AshText
                    )
                }

                items(LevelRepository.episodeLevels) { meta ->
                    val isSelected = selectedLevelId == meta.id
                    val bestTime = settings.bestTimesSec[meta.id]
                    val bestKills = settings.bestKillsPct[meta.id]

                    LevelSelectCard(
                        meta = meta,
                        isSelected = isSelected,
                        bestTimeSec = bestTime,
                        bestKillsPct = bestKills,
                        onClick = { selectedLevelId = meta.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Play CTA
            Button(
                onClick = { onStartGame(selectedLevelId, selectedDifficulty) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BloodCrimson,
                    contentColor = ChalkWhite
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .border(1.5.dp, DoomGold, RoundedCornerShape(8.dp))
                    .testTag("start_game_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OYUNA BAŞLA • E1M$selectedLevelId",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun DifficultySelectorRow(
    selectedId: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val rows = Difficulty.entries.chunked(2)
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { diff ->
                    val active = diff.id == selectedId
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (active) BloodDark.copy(alpha = 0.85f)
                                else GunmetalSurface.copy(alpha = 0.82f)
                            )
                            .border(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) HellfireAmber else SteelBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelect(diff.id) }
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                            .testTag("difficulty_${diff.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (diff.id >= 2) Icons.Filled.Whatshot else Icons.Filled.Shield,
                                contentDescription = null,
                                tint = if (active) DoomGold else AshText,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = diff.titleTr,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 13.sp,
                                color = if (active) ChalkWhite else AshText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = diff.subtitleTr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = if (active) DoomGold else AshText.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelSelectCard(
    meta: LevelRepository.LevelMeta,
    isSelected: Boolean,
    bestTimeSec: Int?,
    bestKillsPct: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) GunmetalElevated.copy(alpha = 0.94f)
                else BunkerSlate.copy(alpha = 0.88f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) DoomGold else SteelBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("level_card_${meta.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Level thumbnail preview
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 62.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, SteelBorder, RoundedCornerShape(4.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_e1m1_hangar),
                contentDescription = "${meta.code} önizleme",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(ObsidianVoid.copy(alpha = 0.82f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = meta.code,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = if (isSelected) DoomGold else ChalkWhite
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meta.titleTr,
                fontFamily = OrbitronFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) DoomGold else ChalkWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta.subtitleTr,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = AshText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (bestTimeSec != null || bestKillsPct != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bestTimeSec != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = NukageGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = formatSeconds(bestTimeSec),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = NukageGreen
                            )
                        }
                    }
                    if (bestKillsPct != null) {
                        Text(
                            text = "KATLİAM %$bestKillsPct",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = HellfireAmber
                        )
                    }
                }
            }
        }
    }
}

fun formatSeconds(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
