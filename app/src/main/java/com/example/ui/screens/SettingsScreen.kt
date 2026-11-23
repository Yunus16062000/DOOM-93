package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSettings
import com.example.ui.theme.AshText
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.BunkerSlate
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.DoomGold
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NukageGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SteelBorder
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSensitivityChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleMinimap: (Boolean) -> Unit,
    onToggleCrosshair: (Boolean) -> Unit,
    onToggleFps: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .widthIn(max = 580.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(GunmetalSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, SteelBorder, RoundedCornerShape(8.dp))
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = ChalkWhite
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = "AYARLAR",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 22.sp,
                        color = DoomGold
                    )
                    Text(
                        text = "Kontrol, Ses ve HUD Tercihleri",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AshText
                    )
                }
            }

            // Sensitivity Slider
            SettingSliderCard(
                title = "DOKUNMATİK BAKIŞ HASSASİYETİ",
                valueLabel = "${(settings.lookSensitivity * 100).roundToInt()}%",
                value = settings.lookSensitivity,
                valueRange = 0.4f..2.2f,
                onValueChange = onSensitivityChange,
                tag = "sensitivity_slider"
            )

            // SFX Volume Slider
            SettingSliderCard(
                title = "SES EFEKTİ SEVİYESİ",
                valueLabel = "${(settings.sfxVolume * 100).roundToInt()}%",
                value = settings.sfxVolume,
                valueRange = 0f..1f,
                onValueChange = onSfxVolumeChange,
                tag = "sfx_volume_slider"
            )

            // Toggle Switches
            SettingSwitchCard(
                title = "KÖŞE MİNİ HARİTA (RADAR)",
                subtitle = "Ekran köşesinde canlı keşfedilmiş haritayı göster",
                checked = settings.showMinimap,
                onCheckedChange = onToggleMinimap,
                tag = "minimap_switch"
            )

            SettingSwitchCard(
                title = "NİŞANGAH (CROSSHAIR)",
                subtitle = "Ekran ortasında klasik hedef artısını göster",
                checked = settings.showCrosshair,
                onCheckedChange = onToggleCrosshair,
                tag = "crosshair_switch"
            )

            SettingSwitchCard(
                title = "TİTREŞİM GERİ BİLDİRİMİ",
                subtitle = "Pompalı atışı, patlama ve hasar anında titreşim",
                checked = settings.vibrationEnabled,
                onCheckedChange = onToggleVibration,
                tag = "vibration_switch"
            )

            SettingSwitchCard(
                title = "FPS SAYACI",
                subtitle = "Üst HUD çubuğunda saniyedeki kare hızını göster",
                checked = settings.showFps,
                onCheckedChange = onToggleFps,
                tag = "fps_switch"
            )
        }
    }
}

@Composable
private fun SettingSliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    tag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BunkerSlate, RoundedCornerShape(8.dp))
            .border(1.dp, SteelBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                color = ChalkWhite
            )
            Text(
                text = valueLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 14.sp,
                color = DoomGold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = DoomGold,
                activeTrackColor = BloodCrimson,
                inactiveTrackColor = GunmetalSurface
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun SettingSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BunkerSlate, RoundedCornerShape(8.dp))
            .border(1.dp, SteelBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                color = ChalkWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = AshText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = NukageGreen,
                uncheckedThumbColor = AshText,
                uncheckedTrackColor = GunmetalSurface
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
