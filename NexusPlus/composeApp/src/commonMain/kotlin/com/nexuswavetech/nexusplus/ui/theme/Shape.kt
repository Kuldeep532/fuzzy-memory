package com.nexuswavetech.nexusplus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Nexus Plus — Material 3 shape token system.
 *
 * All shape decisions live here so the entire UI can be re-themed
 * by changing a single file.  Token names map to the M3 shape scale:
 * None → ExtraSmall → Small → Medium → Large → ExtraLarge → Full.
 */
val NexusShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // chips, badges, tooltips
    small      = RoundedCornerShape(8.dp),    // text fields, small cards
    medium     = RoundedCornerShape(16.dp),   // cards, dialogs, bottom sheets
    large      = RoundedCornerShape(24.dp),   // modals, feature sheets
    extraLarge = RoundedCornerShape(32.dp),   // full-bleed hero surfaces
)

// Convenience tokens for one-off usages
val ShapeButton    = RoundedCornerShape(16.dp)
val ShapeCard      = RoundedCornerShape(16.dp)
val ShapeChip      = RoundedCornerShape(50)
val ShapeBottomBar = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val ShapeHero      = RoundedCornerShape(28.dp)
