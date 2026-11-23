package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DoomDarkColorScheme = darkColorScheme(
    primary = BloodCrimson,
    onPrimary = ChalkWhite,
    primaryContainer = BloodDark,
    onPrimaryContainer = ChalkWhite,
    secondary = HellfireAmber,
    onSecondary = ObsidianVoid,
    secondaryContainer = GunmetalElevated,
    onSecondaryContainer = DoomGold,
    tertiary = NukageGreen,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = ChalkWhite,
    surface = BunkerSlate,
    onSurface = ChalkWhite,
    surfaceVariant = GunmetalSurface,
    onSurfaceVariant = AshText,
    outline = SteelBorder,
    error = BloodCrimson,
    onError = ChalkWhite
)

private val DoomShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

@Composable
fun Doom93Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DoomDarkColorScheme,
        typography = DoomTypography,
        shapes = DoomShapes,
        content = content
    )
}
